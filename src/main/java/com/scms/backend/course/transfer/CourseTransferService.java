package com.scms.backend.course.transfer;

import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer;
import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferRequest;
import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferTarget;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseTransferException;
import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.booking.BookingRepository;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseTransferService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final Clock clock;

	private final BookingRepository bookings;

	CourseTransferService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, NotificationWriter notifications, BookingRepository bookings, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.bookings = bookings;
		this.clock = clock;
		this.courseTransferRepository = new CourseTransferRepository(this.db);
		this.courseTransferCompletion = new CourseTransferCompletion(this.db, this.audits, this.notifications, this.courseTransferRepository);
	}

	@Transactional
	public TransferRequest requestFromScheduleChange(UUID actorId, UUID memberId, UUID responseId, Instant requestedAt) {
		active(actorId, actorId.equals(memberId) ? AccountRole.MEMBER : AccountRole.RECEPTIONIST);
		active(memberId, AccountRole.MEMBER);
		Map<String, Object> source = rows("""
			select e.id source_enrollment_id,e.course_run_id source_run_id,e.lifecycle_status
			from course_schedule_change_responses response
			join course_enrollment_sessions es on es.id=response.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			where response.id=? and e.member_account_id=?
			""", responseId, memberId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		TransferRequest existing = findRequestByResponse(responseId);
		if (existing != null)
			return existing;
		if (!"ACTIVE".equals(text(source, "lifecycle_status")))
			throw CourseTransferException.notFound();
		UUID requestId = UUID.randomUUID();
		try {
			db.update("""
				insert into course_transfer_requests(id,schedule_change_response_id,source_enrollment_id,source_run_id,
					member_account_id,requester_account_id,requested_at) values(?,?,?,?,?,?,?)
				""", requestId, responseId, uuid(source, "source_enrollment_id"), uuid(source, "source_run_id"), memberId, actorId, Timestamp.from(requestedAt));
		} catch (DataIntegrityViolationException exception) {
			return findRequestByResponse(responseId);
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_REQUESTED", "COURSE_TRANSFER_REQUEST", requestId, Map.of("sourceEnrollmentId", uuid(source, "source_enrollment_id").toString(), "sourceRunId", uuid(source, "source_run_id").toString())));
		for (UUID managerId : db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE'", UUID.class)) notifications.write("course-transfer-requested:" + requestId + ":" + managerId, managerId, "COURSE_TRANSFER_REQUESTED", "COURSE_TRANSFER_REQUEST", requestId, Map.of("requestId", requestId.toString(), "actorAccountId", actorId.toString(), "memberAccountId", memberId.toString()));
		return findRequest(requestId);
	}

	@Transactional(readOnly = true)
	List<TransferRequest> managerQueue(UUID managerId) {
		active(managerId, AccountRole.MANAGER);
		return db.query(requestSelect() + " order by tr.requested_at,tr.id", this::mapRequest);
	}

	@Transactional(readOnly = true)
	List<TransferTarget> eligibleTargets(UUID managerId, UUID requestId) {
		active(managerId, AccountRole.MANAGER);
		TransferRequest request = findRequest(requestId);
		if (!List.of("PENDING", "OFFERED").contains(request.status()))
			throw CourseTransferException.notFound();
		Instant now = clock.instant();
		return db.query("""
			select r.id,r.run_code,c.code course_code,c.name course_name,r.tuition_vnd,r.capacity,
				r.registration_closes_at,(select min(s.start_at) from course_sessions s where s.course_run_id=r.id
					and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at>?) next_session_at
			from course_runs r join courses c on c.id=r.course_id
			where r.id<>? and r.status='OPEN_FOR_REGISTRATION' and c.status='PUBLISHED'
			  and r.registration_opens_at<=? and r.registration_closes_at>?
			order by r.registration_closes_at,r.id
			""", (rs, n) -> target(rs, now), Timestamp.from(now), request.sourceRunId(), Timestamp.from(now), Timestamp.from(now)).stream().filter(target -> target.usedSeats() < target.capacity()).toList();
	}

	@Transactional
	TransferOffer approve(UUID managerId, UUID requestId, UUID targetRunId, String reason) {
		active(managerId, AccountRole.MANAGER);
		if (targetRunId == null)
			throw CourseTransferException.invalid("targetRunId is required.");
		String normalizedReason = reason(reason);
		TransferRequest request = lockRequest(requestId);
		if ("OFFERED".equals(request.status())) {
			TransferOffer existing = findOfferByRequest(requestId);
			if (existing != null && existing.targetRunId().equals(targetRunId))
				return existing;
			throw CourseTransferException.conflict("COURSE_TRANSFER_ALREADY_DECIDED", "The request already has another decision.");
		}
		if (!"PENDING".equals(request.status()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_ALREADY_DECIDED", "The request is no longer pending.");
		if (request.sourceRunId().equals(targetRunId))
			throw CourseTransferException.invalid("The target Run must differ from the source Run.");
		lockRuns(request.sourceRunId(), targetRunId);
		Instant now = clock.instant();
		expireDueForRun(targetRunId, now);
		Map<String, Object> target = rows("""
			select r.id,r.tuition_vnd,r.capacity,r.registration_opens_at,r.registration_closes_at,r.status,c.status course_status,
			 (select min(s.start_at) from course_sessions s where s.course_run_id=r.id
			  and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at>?) next_session_at
			from course_runs r join courses c on c.id=r.course_id where r.id=?
			""", Timestamp.from(now), targetRunId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		if (!"OPEN_FOR_REGISTRATION".equals(text(target, "status")) || !"PUBLISHED".equals(text(target, "course_status")) || now.isBefore(instant(target, "registration_opens_at")) || !now.isBefore(instant(target, "registration_closes_at")))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_UNAVAILABLE", "The target Run is not open for transfer.");
		if (!"ACTIVE".equals(db.queryForObject("select lifecycle_status from course_enrollments where id=? for update", String.class, request.sourceEnrollmentId())))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SOURCE_INACTIVE", "The source Enrollment is not active.");
		if (activeEnrollmentExists(request.memberAccountId(), targetRunId))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_DUPLICATE", "The Member already has an active target Enrollment.");
		if (usedSeats(targetRunId, now) >= number(target, "capacity"))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_FULL", "The target Run has no available seat.");
		if (hasScheduleConflict(request.memberAccountId(), request.sourceRunId(), targetRunId))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SCHEDULE_CONFLICT", "The target Run conflicts with the Member schedule.");
		for (Map<String, Object> session : rows("select start_at,end_at from course_sessions where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED')", targetRunId)) if (bookings.existsBookedOverlap(request.memberAccountId(), instant(session, "start_at"), instant(session, "end_at")))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SCHEDULE_CONFLICT", "The target Run conflicts with a booked ClassSession.");
		BigInteger credit = unusedCredit(request.sourceEnrollmentId(), now);
		BigInteger tuition = amount(target, "tuition_vnd");
		Instant expiresAt = CourseTransferPolicy.offerExpiresAt(now, instant(target, "registration_closes_at"), instant(target, "next_session_at"));
		if (!expiresAt.isAfter(now))
			throw CourseTransferException.conflict("COURSE_TRANSFER_DEADLINE_UNAVAILABLE", "No transfer acceptance window remains.");
		UUID offerId = UUID.randomUUID();
		db.update("""
			insert into course_transfer_offers(id,transfer_request_id,source_enrollment_id,source_run_id,target_run_id,
			 member_account_id,credit_vnd_snapshot,target_tuition_vnd_snapshot,difference_vnd_snapshot,expires_at,issued_at)
			 values(?,?,?,?,?,?,?,?,?,?,?)
			""", offerId, requestId, request.sourceEnrollmentId(), request.sourceRunId(), targetRunId, request.memberAccountId(), credit, tuition, tuition.subtract(credit), Timestamp.from(expiresAt), Timestamp.from(now));
		db.update("insert into course_transfer_holds(id,transfer_offer_id,target_run_id,member_account_id,expires_at,created_at) values(?,?,?,?,?,?)", UUID.randomUUID(), offerId, targetRunId, request.memberAccountId(), Timestamp.from(expiresAt), Timestamp.from(now));
		db.update("update course_transfer_requests set status='OFFERED',target_run_id=?,reason=?,decided_by_account_id=?,decided_at=? where id=?", targetRunId, normalizedReason, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_TRANSFER_APPROVED", "COURSE_TRANSFER_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"), Map.of("status", "OFFERED", "offerId", offerId.toString(), "targetRunId", targetRunId.toString(), "creditVnd", credit.toString(), "targetTuitionVnd", tuition.toString())));
		notifications.write("course-transfer-offered:" + offerId, request.memberAccountId(), "COURSE_TRANSFER_OFFERED", "COURSE_TRANSFER_OFFER", offerId, Map.of("sourceRunId", request.sourceRunId().toString(), "targetRunId", targetRunId.toString(), "creditVnd", credit.toString(), "targetTuitionVnd", tuition.toString(), "expiresAt", expiresAt.toString()));
		return findOffer(offerId);
	}

	@Transactional
	TransferRequest reject(UUID managerId, UUID requestId, String reason) {
		active(managerId, AccountRole.MANAGER);
		String normalized = reason(reason);
		TransferRequest request = lockRequest(requestId);
		if ("REJECTED".equals(request.status()) && normalized.equals(request.reason()))
			return request;
		if (!"PENDING".equals(request.status()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_ALREADY_DECIDED", "The request is no longer pending.");
		Instant now = clock.instant();
		db.update("update course_transfer_requests set status='REJECTED',reason=?,decided_by_account_id=?,decided_at=? where id=?", normalized, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_TRANSFER_REJECTED", "COURSE_TRANSFER_REQUEST", requestId, normalized, Map.of("status", "PENDING"), Map.of("status", "REJECTED")));
		notifications.write("course-transfer-rejected:" + requestId, request.memberAccountId(), "COURSE_TRANSFER_REJECTED", "COURSE_TRANSFER_REQUEST", requestId, Map.of("reason", normalized));
		return findRequest(requestId);
	}

	@Transactional(readOnly = true)
	public List<TransferOffer> offersForReceptionist(UUID actorId, UUID memberId) {
		active(actorId, AccountRole.RECEPTIONIST);
		return memberOffers(memberId);
	}

	@Transactional(readOnly = true)
	List<TransferOffer> memberOffers(UUID memberId) {
		active(memberId, AccountRole.MEMBER);
		return db.query(offerSelect() + " where transfer.member_account_id=? order by transfer.issued_at desc,transfer.id", this::mapOffer, memberId);
	}

	@Transactional(noRollbackFor = CourseTransferException.ExpiredOffer.class)
	public TransferOffer accept(UUID memberId, UUID offerId, String idempotencyKey) {
		return accept(memberId, memberId, offerId, idempotencyKey);
	}

	@Transactional(noRollbackFor = CourseTransferException.ExpiredOffer.class)
	public TransferOffer accept(UUID actorId, UUID memberId, UUID offerId, String idempotencyKey) {
		if (!actorId.equals(memberId))
			accounts.findByIdForUpdate(actorId).filter(account -> account.getRole() == AccountRole.RECEPTIONIST && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		accounts.findByIdForUpdate(memberId).filter(account -> account.getRole() == AccountRole.MEMBER && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		String key = key(idempotencyKey);
		TransferOffer offer = lockOffer(memberId, offerId);
		if (offer.acceptedAt() != null) {
			String existing = db.queryForObject("select acceptance_idempotency_key from course_transfer_offers where id=?", String.class, offerId);
			if (key.equals(existing))
				return offer;
			throw CourseTransferException.conflict("COURSE_TRANSFER_IDEMPOTENCY_CONFLICT", "The offer was accepted with another idempotency key.");
		}
		Instant now = clock.instant();
		lockRuns(offer.sourceRunId(), offer.targetRunId());
		if (!"OFFERED".equals(offer.status()) || !now.isBefore(offer.expiresAt())) {
			expireOffer(offer, now);
			throw CourseTransferException.expiredOffer();
		}
		BigInteger currentCredit = unusedCredit(offer.sourceEnrollmentId(), now);
		if (!currentCredit.equals(offer.creditVnd()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_OFFER_STALE", "The unused allocation changed after the offer was issued.");
		if (usedSeatsExcludingOffer(offer.targetRunId(), offer.id(), now) >= capacity(offer.targetRunId()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_FULL", "The target Run has no available seat.");
		if (offer.differenceVnd().signum() > 0) {
			UUID orderId = createDifferenceOrder(offer, actorId, now);
			db.update("update course_transfer_offers set status='PENDING_PAYMENT',acceptance_idempotency_key=?,difference_order_id=?,accepted_at=? where id=?", key, orderId, Timestamp.from(now), offerId);
			recordAcceptance(actorId, offer, orderId);
			return findOffer(offerId);
		}
		db.update("update course_transfer_offers set acceptance_idempotency_key=?,accepted_at=? where id=?", key, Timestamp.from(now), offerId);
		completeTransfer(findOffer(offerId), null, null, actorId, now);
		recordAcceptance(actorId, offer, null);
		return findOffer(offerId);
	}

	private void recordAcceptance(UUID actorId, TransferOffer offer, UUID orderId) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_ACCEPTED", "COURSE_TRANSFER_OFFER", offer.id(), Map.of("memberAccountId", offer.memberAccountId().toString(), "paymentRequired", orderId != null)));
	}

	@Transactional
	public CourseEnrollmentResponse completePaidDifference(UUID orderId, UUID paymentId, UUID actorId, Instant now) {
		TransferOffer offer = db.query(offerSelect() + " where transfer.difference_order_id=? for update of transfer", this::mapOffer, orderId).stream().findFirst().orElse(null);
		if (offer == null)
			return null;
		if ("COMPLETED".equals(offer.status()))
			return targetEnrollment(offer.id());
		if (!"PENDING_PAYMENT".equals(offer.status()) || !now.isBefore(offer.expiresAt()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_OFFER_EXPIRED", "The transfer offer is no longer payable.");
		lockRuns(offer.sourceRunId(), offer.targetRunId());
		return completeTransfer(offer, orderId, paymentId, actorId, now);
	}

	@Transactional
	int expireDue(Instant now) {
		List<TransferOffer> due = db.query(offerSelect() + " where transfer.status in ('OFFERED','PENDING_PAYMENT') and transfer.expires_at<=? order by transfer.id for update of transfer skip locked", this::mapOffer, Timestamp.from(now));
		for (TransferOffer offer : due) expireOffer(offer, now);
		return due.size();
	}

	private CourseEnrollmentResponse completeTransfer(TransferOffer offer, UUID orderId, UUID paymentId, UUID actorId, Instant now) {
		return courseTransferCompletion.completeTransfer(offer, orderId, paymentId, actorId, now);
	}

	private UUID createDifferenceOrder(TransferOffer offer, UUID actorId, Instant now) {
		UUID memberId = offer.memberAccountId();
		Instant expires = now.plusSeconds(900).isBefore(offer.expiresAt()) ? now.plusSeconds(900) : offer.expiresAt();
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase(Locale.ROOT);
		db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,tuition_vnd_snapshot,
			 currency_code,status,idempotency_key,created_at,updated_at) values(?,?,?,?,?,?,'VND','PENDING_PAYMENT',?,?,?)
			""", orderId, reference, memberId, actorId, offer.targetRunId(), offer.differenceVnd(), "transfer:" + offer.id(), Timestamp.from(now), Timestamp.from(now));
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,created_at,updated_at) values(?,?,?,?,'HELD',?,?,?)", reservationId, orderId, memberId, offer.targetRunId(), Timestamp.from(expires), Timestamp.from(now), Timestamp.from(now));
		db.update("update course_transfer_holds set status='RELEASED',expires_at=?,released_at=? where transfer_offer_id=? and status='HELD'", Timestamp.from(expires), Timestamp.from(now), offer.id());
		return orderId;
	}

	private void expireOffer(TransferOffer offer, Instant now) {
		if (!List.of("OFFERED", "PENDING_PAYMENT").contains(offer.status()))
			return;
		db.update("update course_transfer_offers set status='EXPIRED' where id=? and status in ('OFFERED','PENDING_PAYMENT')", offer.id());
		db.update("update course_transfer_holds set status='RELEASED',released_at=? where transfer_offer_id=? and status='HELD'", Timestamp.from(now), offer.id());
		db.update("update course_transfer_requests set status='EXPIRED' where id=? and status='OFFERED'", offer.requestId());
		if (offer.differenceOrderId() != null) {
			db.update("update course_reservations set status='EXPIRED',updated_at=? where course_order_id=? and status='HELD'", Timestamp.from(now), offer.differenceOrderId());
			db.update("update course_orders set status='EXPIRED',updated_at=? where id=? and status='PENDING_PAYMENT'", Timestamp.from(now), offer.differenceOrderId());
			db.update("update course_payments set status='FAILED',updated_at=? where course_order_id=? and status='PENDING'", Timestamp.from(now), offer.differenceOrderId());
		}
	}

	private void expireDueForRun(UUID runId, Instant now) {
		db.update("update course_transfer_holds set status='RELEASED',released_at=? where target_run_id=? and status='HELD' and expires_at<=?", Timestamp.from(now), runId, Timestamp.from(now));
		db.update("update course_transfer_requests set status='EXPIRED' where status='OFFERED' and id in " + "(select transfer_request_id from course_transfer_offers where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT') and expires_at<=?)", runId, Timestamp.from(now));
		db.update("update course_transfer_offers set status='EXPIRED' where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT') and expires_at<=?", runId, Timestamp.from(now));
	}

	private BigInteger unusedCredit(UUID enrollmentId, Instant now) {
		return courseTransferRepository.unusedCredit(enrollmentId, now);
	}

	private long usedSeats(UUID runId, Instant now) {
		return courseTransferRepository.usedSeats(runId, now);
	}

	private long usedSeatsExcludingOffer(UUID runId, UUID excludedOfferId, Instant now) {
		return courseTransferRepository.usedSeatsExcludingOffer(runId, excludedOfferId, now);
	}

	private boolean hasScheduleConflict(UUID memberId, UUID sourceRunId, UUID targetRunId) {
		return courseTransferRepository.hasScheduleConflict(memberId, sourceRunId, targetRunId);
	}

	private TransferTarget target(ResultSet rs, Instant now) throws SQLException {
		return courseTransferRepository.target(rs, now);
	}

	private void lockRuns(UUID first, UUID second) {
		courseTransferRepository.lockRuns(first, second);
	}

	private int capacity(UUID runId) {
		return courseTransferRepository.capacity(runId);
	}

	private boolean activeEnrollmentExists(UUID memberId, UUID runId) {
		return courseTransferRepository.activeEnrollmentExists(memberId, runId);
	}

	private void active(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private String reason(String value) {
		if (value == null || value.isBlank() || value.trim().length() > 500)
			throw CourseTransferException.invalid("A reason of at most 500 characters is required.");
		return value.trim();
	}

	private String key(String value) {
		if (value == null || value.isBlank() || value.trim().length() > 100)
			throw CourseTransferException.keyRequired();
		return value.trim();
	}

	private TransferRequest lockRequest(UUID id) {
		return courseTransferRepository.lockRequest(id);
	}

	private TransferRequest findRequest(UUID id) {
		return courseTransferRepository.findRequest(id);
	}

	private TransferRequest findRequestByResponse(UUID id) {
		return courseTransferRepository.findRequestByResponse(id);
	}

	private TransferOffer lockOffer(UUID memberId, UUID id) {
		return courseTransferRepository.lockOffer(memberId, id);
	}

	private TransferOffer findOffer(UUID id) {
		return courseTransferRepository.findOffer(id);
	}

	private TransferOffer findOfferByRequest(UUID id) {
		return courseTransferRepository.findOfferByRequest(id);
	}

	private CourseEnrollmentResponse targetEnrollment(UUID offerId) {
		return courseTransferRepository.targetEnrollment(offerId);
	}

	private String requestSelect() {
		return courseTransferRepository.requestSelect();
	}

	private String offerSelect() {
		return courseTransferRepository.offerSelect();
	}

	private TransferRequest mapRequest(ResultSet rs, int n) throws SQLException {
		return courseTransferRepository.mapRequest(rs, n);
	}

	private TransferOffer mapOffer(ResultSet rs, int n) throws SQLException {
		return courseTransferRepository.mapOffer(rs, n);
	}

	private List<Map<String, Object>> rows(String sql, Object... args) {
		return courseTransferCompletion.rows(sql, args);
	}

	private static UUID uuid(Map<String, Object> row, String key) {
		return CourseTransferCompletion.uuid(row, key);
	}

	private static String text(Map<String, Object> row, String key) {
		return row.get(key) == null ? null : row.get(key).toString();
	}

	private static int number(Map<String, Object> row, String key) {
		return CourseTransferCompletion.number(row, key);
	}

	private static BigInteger amount(Map<String, Object> row, String key) {
		return CourseTransferRepository.amount(row, key);
	}

	private static Instant instant(Map<String, Object> row, String key) {
		return CourseTransferCompletion.instant(row, key);
	}

	private final CourseTransferRepository courseTransferRepository;

	private final CourseTransferCompletion courseTransferCompletion;
}
