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
		Map<String, Object> source = courseTransferRepository.requestFromScheduleChangeRowsCourseScheduleChangeResponses(responseId, memberId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		TransferRequest existing = findRequestByResponse(responseId);
		if (existing != null)
			return existing;
		if (!"ACTIVE".equals(text(source, "lifecycle_status")))
			throw CourseTransferException.notFound();
		UUID requestId = UUID.randomUUID();
		try {
			courseTransferRepository.requestFromScheduleChangeInsertCourseTransferRequests(requestId, responseId, uuid(source, "source_enrollment_id"), uuid(source, "source_run_id"), memberId, actorId, Timestamp.from(requestedAt));
		} catch (DataIntegrityViolationException exception) {
			return findRequestByResponse(responseId);
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_REQUESTED", "COURSE_TRANSFER_REQUEST", requestId, Map.of("sourceEnrollmentId", uuid(source, "source_enrollment_id").toString(), "sourceRunId", uuid(source, "source_run_id").toString())));
		for (UUID managerId : courseTransferRepository.requestFromScheduleChangeFindAccounts()) notifications.write("course-transfer-requested:" + requestId + ":" + managerId, managerId, "COURSE_TRANSFER_REQUESTED", "COURSE_TRANSFER_REQUEST", requestId, Map.of("requestId", requestId.toString(), "actorAccountId", actorId.toString(), "memberAccountId", memberId.toString()));
		return findRequest(requestId);
	}

	@Transactional(readOnly = true)
	List<TransferRequest> managerQueue(UUID managerId) {
		active(managerId, AccountRole.MANAGER);
		return courseTransferRepository.managerQueueFindRows();
	}

	@Transactional(readOnly = true)
	List<TransferTarget> eligibleTargets(UUID managerId, UUID requestId) {
		active(managerId, AccountRole.MANAGER);
		TransferRequest request = findRequest(requestId);
		if (!List.of("PENDING", "OFFERED").contains(request.status()))
			throw CourseTransferException.notFound();
		Instant now = clock.instant();
		return courseTransferRepository.eligibleTargetsFindCourseSessions(now, Timestamp.from(now), request.sourceRunId(), Timestamp.from(now), Timestamp.from(now)).stream().filter(target -> target.usedSeats() < target.capacity()).toList();
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
		Map<String, Object> target = courseTransferRepository.approveRowsCourseSessions(Timestamp.from(now), targetRunId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		if (!"OPEN_FOR_REGISTRATION".equals(text(target, "status")) || !"PUBLISHED".equals(text(target, "course_status")) || now.isBefore(instant(target, "registration_opens_at")) || !now.isBefore(instant(target, "registration_closes_at")))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_UNAVAILABLE", "The target Run is not open for transfer.");
		if (!"ACTIVE".equals(courseTransferRepository.approveFindCourseEnrollments(request.sourceEnrollmentId())))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SOURCE_INACTIVE", "The source Enrollment is not active.");
		if (activeEnrollmentExists(request.memberAccountId(), targetRunId))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_DUPLICATE", "The Member already has an active target Enrollment.");
		if (usedSeats(targetRunId, now) >= number(target, "capacity"))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_FULL", "The target Run has no available seat.");
		if (hasScheduleConflict(request.memberAccountId(), request.sourceRunId(), targetRunId))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SCHEDULE_CONFLICT", "The target Run conflicts with the Member schedule.");
		for (Map<String, Object> session : courseTransferRepository.findTargetRunSessionTimes(targetRunId)) if (bookings.existsBookedOverlap(request.memberAccountId(), instant(session, "start_at"), instant(session, "end_at")))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SCHEDULE_CONFLICT", "The target Run conflicts with a booked ClassSession.");
		BigInteger credit = unusedCredit(request.sourceEnrollmentId(), now);
		BigInteger tuition = amount(target, "tuition_vnd");
		Instant expiresAt = CourseTransferPolicy.offerExpiresAt(now, instant(target, "registration_closes_at"), instant(target, "next_session_at"));
		if (!expiresAt.isAfter(now))
			throw CourseTransferException.conflict("COURSE_TRANSFER_DEADLINE_UNAVAILABLE", "No transfer acceptance window remains.");
		UUID offerId = UUID.randomUUID();
		courseTransferRepository.approveInsertCourseTransferOffers(offerId, requestId, request.sourceEnrollmentId(), request.sourceRunId(), targetRunId, request.memberAccountId(), credit, tuition, tuition.subtract(credit), Timestamp.from(expiresAt), Timestamp.from(now));
		courseTransferRepository.approveInsertCourseTransferHolds(UUID.randomUUID(), offerId, targetRunId, request.memberAccountId(), Timestamp.from(expiresAt), Timestamp.from(now));
		courseTransferRepository.approveUpdateCourseTransferRequests(targetRunId, normalizedReason, managerId, Timestamp.from(now), requestId);
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
		courseTransferRepository.rejectUpdateCourseTransferRequests(normalized, managerId, Timestamp.from(now), requestId);
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
		return courseTransferRepository.memberOffersFindRows(memberId);
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
			String existing = courseTransferRepository.acceptFindCourseTransferOffers(offerId);
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
			courseTransferRepository.acceptUpdateCourseTransferOffers(key, orderId, Timestamp.from(now), offerId);
			recordAcceptance(actorId, offer, orderId);
			return findOffer(offerId);
		}
		courseTransferRepository.recordOfferAcceptance(key, Timestamp.from(now), offerId);
		completeTransfer(findOffer(offerId), null, null, actorId, now);
		recordAcceptance(actorId, offer, null);
		return findOffer(offerId);
	}

	private void recordAcceptance(UUID actorId, TransferOffer offer, UUID orderId) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_ACCEPTED", "COURSE_TRANSFER_OFFER", offer.id(), Map.of("memberAccountId", offer.memberAccountId().toString(), "paymentRequired", orderId != null)));
	}

	@Transactional
	public CourseEnrollmentResponse completePaidDifference(UUID orderId, UUID paymentId, UUID actorId, Instant now) {
		TransferOffer offer = courseTransferRepository.completePaidDifferenceFindOf(orderId).stream().findFirst().orElse(null);
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
		List<TransferOffer> due = courseTransferRepository.expireDueFindOf(Timestamp.from(now));
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
		courseTransferRepository.createDifferenceOrderInsertCourseOrders(orderId, reference, memberId, actorId, offer.targetRunId(), offer.differenceVnd(), "transfer:" + offer.id(), Timestamp.from(now), Timestamp.from(now));
		courseTransferRepository.createDifferenceOrderInsertCourseReservations(reservationId, orderId, memberId, offer.targetRunId(), Timestamp.from(expires), Timestamp.from(now), Timestamp.from(now));
		courseTransferRepository.createDifferenceOrderUpdateCourseTransferHolds(Timestamp.from(expires), Timestamp.from(now), offer.id());
		return orderId;
	}

	private void expireOffer(TransferOffer offer, Instant now) {
		if (!List.of("OFFERED", "PENDING_PAYMENT").contains(offer.status()))
			return;
		courseTransferRepository.expireOfferUpdateCourseTransferOffers(offer.id());
		courseTransferRepository.expireOfferUpdateCourseTransferHolds(Timestamp.from(now), offer.id());
		courseTransferRepository.expireOfferUpdateCourseTransferRequests(offer.requestId());
		if (offer.differenceOrderId() != null) {
			courseTransferRepository.expireOfferUpdateCourseReservations(Timestamp.from(now), offer.differenceOrderId());
			courseTransferRepository.expireOfferUpdateCourseOrders(Timestamp.from(now), offer.differenceOrderId());
			courseTransferRepository.expireOfferUpdateCoursePayments(Timestamp.from(now), offer.differenceOrderId());
		}
	}

	private void expireDueForRun(UUID runId, Instant now) {
		courseTransferRepository.expireDueForRunUpdateCourseTransferHolds(Timestamp.from(now), runId, Timestamp.from(now));
		courseTransferRepository.expireDueForRunUpdateCourseTransferRequests(runId, Timestamp.from(now));
		courseTransferRepository.expireDueForRunUpdateCourseTransferOffers(runId, Timestamp.from(now));
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
