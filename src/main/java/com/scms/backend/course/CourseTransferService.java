package com.scms.backend.course;

import static com.scms.backend.course.CourseTransferApiModels.TransferOffer;
import static com.scms.backend.course.CourseTransferApiModels.TransferRequest;
import static com.scms.backend.course.CourseTransferApiModels.TransferTarget;

import java.math.BigDecimal;
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

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.BookingRepository;
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

	CourseTransferService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits,
			NotificationWriter notifications, BookingRepository bookings, Clock clock) {
		this.db = db; this.accounts = accounts; this.audits = audits; this.notifications = notifications;
		this.bookings = bookings; this.clock = clock;
	}

	@Transactional
	TransferRequest requestFromScheduleChange(UUID memberId, UUID responseId, Instant requestedAt) {
		Map<String, Object> source = rows("""
			select e.id source_enrollment_id,e.course_run_id source_run_id
			from course_schedule_change_responses response
			join course_enrollment_sessions es on es.id=response.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			where response.id=? and e.member_account_id=? and e.lifecycle_status='ACTIVE'
			""", responseId, memberId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		TransferRequest existing = findRequestByResponse(responseId);
		if (existing != null) return existing;
		UUID requestId = UUID.randomUUID();
		try {
			db.update("""
				insert into course_transfer_requests(id,schedule_change_response_id,source_enrollment_id,source_run_id,
					member_account_id,requester_account_id,requested_at) values(?,?,?,?,?,?,?)
				""", requestId, responseId, uuid(source, "source_enrollment_id"), uuid(source, "source_run_id"),
				memberId, memberId, Timestamp.from(requestedAt));
		}
		catch (DataIntegrityViolationException exception) { return findRequestByResponse(responseId); }
		audits.save(new AuditEvent(UUID.randomUUID(), memberId, "COURSE_TRANSFER_REQUESTED", "COURSE_TRANSFER_REQUEST",
			requestId, Map.of("sourceEnrollmentId", uuid(source, "source_enrollment_id").toString(),
				"sourceRunId", uuid(source, "source_run_id").toString())));
		for (UUID managerId : db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE'", UUID.class))
			notifications.write("course-transfer-requested:" + requestId + ":" + managerId, managerId,
				"COURSE_TRANSFER_REQUESTED", "COURSE_TRANSFER_REQUEST", requestId,
				Map.of("requestId", requestId.toString()));
		return findRequest(requestId);
	}

	@Transactional(readOnly = true)
	List<TransferRequest> managerQueue(UUID managerId) {
		active(managerId, AccountRole.MANAGER);
		return db.query(requestSelect() + " order by requested_at,id", this::mapRequest);
	}

	@Transactional(readOnly = true)
	List<TransferTarget> eligibleTargets(UUID managerId, UUID requestId) {
		active(managerId, AccountRole.MANAGER);
		TransferRequest request = findRequest(requestId);
		if (!List.of("PENDING", "OFFERED").contains(request.status())) throw CourseTransferException.notFound();
		Instant now = clock.instant();
		return db.query("""
			select r.id,r.run_code,c.code course_code,c.name course_name,r.tuition_vnd,r.capacity,
				r.registration_closes_at,(select min(s.start_at) from course_sessions s where s.course_run_id=r.id
					and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at>?) next_session_at
			from course_runs r join courses c on c.id=r.course_id
			where r.id<>? and r.status='OPEN_FOR_REGISTRATION' and c.status='PUBLISHED'
			  and r.registration_opens_at<=? and r.registration_closes_at>?
			order by r.registration_closes_at,r.id
			""", (rs, n) -> target(rs, now), Timestamp.from(now), request.sourceRunId(),
			Timestamp.from(now), Timestamp.from(now)).stream().filter(target -> target.usedSeats() < target.capacity()).toList();
	}

	@Transactional
	TransferOffer approve(UUID managerId, UUID requestId, UUID targetRunId, String reason) {
		active(managerId, AccountRole.MANAGER);
		if (targetRunId == null) throw CourseTransferException.invalid("targetRunId is required.");
		String normalizedReason = reason(reason);
		TransferRequest request = lockRequest(requestId);
		if ("OFFERED".equals(request.status())) {
			TransferOffer existing = findOfferByRequest(requestId);
			if (existing != null && existing.targetRunId().equals(targetRunId)) return existing;
			throw CourseTransferException.conflict("COURSE_TRANSFER_ALREADY_DECIDED", "The request already has another decision.");
		}
		if (!"PENDING".equals(request.status())) throw CourseTransferException.conflict("COURSE_TRANSFER_ALREADY_DECIDED", "The request is no longer pending.");
		if (request.sourceRunId().equals(targetRunId)) throw CourseTransferException.invalid("The target Run must differ from the source Run.");
		lockRuns(request.sourceRunId(), targetRunId);
		Instant now = clock.instant();
		expireDueForRun(targetRunId, now);
		Map<String, Object> target = rows("""
			select r.id,r.tuition_vnd,r.capacity,r.registration_opens_at,r.registration_closes_at,r.status,c.status course_status,
			 (select min(s.start_at) from course_sessions s where s.course_run_id=r.id
			  and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.start_at>?) next_session_at
			from course_runs r join courses c on c.id=r.course_id where r.id=?
			""", Timestamp.from(now), targetRunId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		if (!"OPEN_FOR_REGISTRATION".equals(text(target, "status")) || !"PUBLISHED".equals(text(target, "course_status"))
				|| now.isBefore(instant(target, "registration_opens_at")) || !now.isBefore(instant(target, "registration_closes_at")))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_UNAVAILABLE", "The target Run is not open for transfer.");
		if (!"ACTIVE".equals(db.queryForObject("select lifecycle_status from course_enrollments where id=? for update",
			String.class, request.sourceEnrollmentId())))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SOURCE_INACTIVE", "The source Enrollment is not active.");
		if (activeEnrollmentExists(request.memberAccountId(), targetRunId))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_DUPLICATE", "The Member already has an active target Enrollment.");
		if (usedSeats(targetRunId, now) >= number(target, "capacity"))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_FULL", "The target Run has no available seat.");
		if (hasScheduleConflict(request.memberAccountId(), request.sourceRunId(), targetRunId))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SCHEDULE_CONFLICT", "The target Run conflicts with the Member schedule.");
		for (Map<String, Object> session : rows("select start_at,end_at from course_sessions where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED')", targetRunId))
			if (bookings.existsBookedOverlap(request.memberAccountId(), instant(session, "start_at"), instant(session, "end_at")))
				throw CourseTransferException.conflict("COURSE_TRANSFER_SCHEDULE_CONFLICT", "The target Run conflicts with a booked ClassSession.");
		BigInteger credit = unusedCredit(request.sourceEnrollmentId(), now);
		BigInteger tuition = amount(target, "tuition_vnd");
		Instant expiresAt = CourseTransferPolicy.offerExpiresAt(now, instant(target, "registration_closes_at"),
			instant(target, "next_session_at"));
		if (!expiresAt.isAfter(now)) throw CourseTransferException.conflict("COURSE_TRANSFER_DEADLINE_UNAVAILABLE", "No transfer acceptance window remains.");
		UUID offerId = UUID.randomUUID();
		db.update("""
			insert into course_transfer_offers(id,transfer_request_id,source_enrollment_id,source_run_id,target_run_id,
			 member_account_id,credit_vnd_snapshot,target_tuition_vnd_snapshot,difference_vnd_snapshot,expires_at,issued_at)
			 values(?,?,?,?,?,?,?,?,?,?,?)
			""", offerId, requestId, request.sourceEnrollmentId(), request.sourceRunId(), targetRunId,
			request.memberAccountId(), credit, tuition, tuition.subtract(credit), Timestamp.from(expiresAt), Timestamp.from(now));
		db.update("insert into course_transfer_holds(id,transfer_offer_id,target_run_id,member_account_id,expires_at,created_at) values(?,?,?,?,?,?)",
			UUID.randomUUID(), offerId, targetRunId, request.memberAccountId(), Timestamp.from(expiresAt), Timestamp.from(now));
		db.update("update course_transfer_requests set status='OFFERED',target_run_id=?,reason=?,decided_by_account_id=?,decided_at=? where id=?",
			targetRunId, normalizedReason, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_TRANSFER_APPROVED", "COURSE_TRANSFER_REQUEST",
			requestId, normalizedReason, Map.of("status", "PENDING"), Map.of("status", "OFFERED", "offerId", offerId.toString(),
				"targetRunId", targetRunId.toString(), "creditVnd", credit.toString(), "targetTuitionVnd", tuition.toString())));
		notifications.write("course-transfer-offered:" + offerId, request.memberAccountId(), "COURSE_TRANSFER_OFFERED",
			"COURSE_TRANSFER_OFFER", offerId, Map.of("sourceRunId", request.sourceRunId().toString(),
				"targetRunId", targetRunId.toString(), "creditVnd", credit.toString(), "targetTuitionVnd", tuition.toString(),
				"expiresAt", expiresAt.toString()));
		return findOffer(offerId);
	}

	@Transactional
	TransferRequest reject(UUID managerId, UUID requestId, String reason) {
		active(managerId, AccountRole.MANAGER);
		String normalized = reason(reason);
		TransferRequest request = lockRequest(requestId);
		if ("REJECTED".equals(request.status()) && normalized.equals(request.reason())) return request;
		if (!"PENDING".equals(request.status())) throw CourseTransferException.conflict("COURSE_TRANSFER_ALREADY_DECIDED", "The request is no longer pending.");
		Instant now = clock.instant();
		db.update("update course_transfer_requests set status='REJECTED',reason=?,decided_by_account_id=?,decided_at=? where id=?",
			normalized, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_TRANSFER_REJECTED", "COURSE_TRANSFER_REQUEST",
			requestId, normalized, Map.of("status", "PENDING"), Map.of("status", "REJECTED")));
		notifications.write("course-transfer-rejected:" + requestId, request.memberAccountId(), "COURSE_TRANSFER_REJECTED",
			"COURSE_TRANSFER_REQUEST", requestId, Map.of("reason", normalized));
		return findRequest(requestId);
	}

	@Transactional(readOnly = true)
	List<TransferOffer> memberOffers(UUID memberId) {
		active(memberId, AccountRole.MEMBER);
		return db.query(offerSelect() + " where member_account_id=? order by issued_at desc,id", this::mapOffer, memberId);
	}

	@Transactional(noRollbackFor = CourseTransferException.class)
	TransferOffer accept(UUID memberId, UUID offerId, String idempotencyKey) {
		active(memberId, AccountRole.MEMBER);
		String key = key(idempotencyKey);
		TransferOffer offer = lockOffer(memberId, offerId);
		if (offer.acceptedAt() != null) {
			String existing = db.queryForObject("select acceptance_idempotency_key from course_transfer_offers where id=?", String.class, offerId);
			if (key.equals(existing)) return offer;
			throw CourseTransferException.conflict("COURSE_TRANSFER_IDEMPOTENCY_CONFLICT", "The offer was accepted with another idempotency key.");
		}
		Instant now = clock.instant();
		lockRuns(offer.sourceRunId(), offer.targetRunId());
		if (!"OFFERED".equals(offer.status()) || !now.isBefore(offer.expiresAt())) {
			expireOffer(offer, now);
			throw CourseTransferException.conflict("COURSE_TRANSFER_OFFER_EXPIRED", "The transfer offer has expired.");
		}
		BigInteger currentCredit = unusedCredit(offer.sourceEnrollmentId(), now);
		if (!currentCredit.equals(offer.creditVnd()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_OFFER_STALE", "The unused allocation changed after the offer was issued.");
		if (usedSeatsExcludingOffer(offer.targetRunId(), offer.id(), now) >= capacity(offer.targetRunId()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_FULL", "The target Run has no available seat.");
		if (offer.differenceVnd().signum() > 0) {
			UUID orderId = createDifferenceOrder(offer, memberId, key, now);
			db.update("update course_transfer_offers set status='PENDING_PAYMENT',acceptance_idempotency_key=?,difference_order_id=?,accepted_at=? where id=?",
				key, orderId, Timestamp.from(now), offerId);
			return findOffer(offerId);
		}
		db.update("update course_transfer_offers set acceptance_idempotency_key=?,accepted_at=? where id=?",
			key, Timestamp.from(now), offerId);
		completeTransfer(findOffer(offerId), null, null, memberId, now);
		return findOffer(offerId);
	}

	@Transactional
	public CourseEnrollmentResponse completePaidDifference(UUID orderId, UUID paymentId, UUID actorId, Instant now) {
		TransferOffer offer = db.query(offerSelect() + " where difference_order_id=? for update", this::mapOffer, orderId)
			.stream().findFirst().orElse(null);
		if (offer == null) return null;
		if ("COMPLETED".equals(offer.status())) return targetEnrollment(offer.id());
		if (!"PENDING_PAYMENT".equals(offer.status()) || !now.isBefore(offer.expiresAt()))
			throw CourseTransferException.conflict("COURSE_TRANSFER_OFFER_EXPIRED", "The transfer offer is no longer payable.");
		lockRuns(offer.sourceRunId(), offer.targetRunId());
		return completeTransfer(offer, orderId, paymentId, actorId, now);
	}

	@Transactional
	int expireDue(Instant now) {
		List<TransferOffer> due = db.query(offerSelect() + " where status in ('OFFERED','PENDING_PAYMENT') and expires_at<=? order by id for update skip locked",
			this::mapOffer, Timestamp.from(now));
		for (TransferOffer offer : due) expireOffer(offer, now);
		return due.size();
	}

	private CourseEnrollmentResponse completeTransfer(TransferOffer offer, UUID orderId, UUID paymentId,
			UUID actorId, Instant now) {
		if (!"ACTIVE".equals(db.queryForObject("select lifecycle_status from course_enrollments where id=? for update",
			String.class, offer.sourceEnrollmentId())))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SOURCE_INACTIVE", "The source Enrollment is not active.");
		UUID reservationId = orderId == null ? null : db.queryForObject("select id from course_reservations where course_order_id=?", UUID.class, orderId);
		UUID enrollmentId = UUID.randomUUID();
		int totalUnits = db.queryForObject("select total_units from course_runs where id=?", Integer.class, offer.targetRunId());
		BigInteger appliedCredit = offer.creditVnd().min(offer.targetTuitionVnd());
		BigInteger cashPaid = offer.targetTuitionVnd().subtract(appliedCredit);
		db.update("""
			insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,member_account_id,
			 actor_account_id,processed_by_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,
			 lifecycle_status,source_transfer_offer_id,credit_applied_vnd,cash_paid_vnd)
			 values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			""", enrollmentId, orderId, reservationId, paymentId, offer.memberAccountId(), offer.memberAccountId(), actorId,
			offer.targetRunId(), offer.targetTuitionVnd(), totalUnits, Timestamp.from(now), "ACTIVE", offer.id(), appliedCredit, cashPaid);
		List<Map<String, Object>> units = rows("select id,unit_number,start_at from course_sessions where course_run_id=? and is_makeup=false order by unit_number,id", offer.targetRunId());
		if (units.size() != totalUnits) throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_SCHEDULE_INVALID", "The target schedule does not match its unit count.");
		BigInteger[] split = offer.targetTuitionVnd().divideAndRemainder(BigInteger.valueOf(totalUnits));
		for (int index = 0; index < units.size(); index++) {
			Map<String, Object> unit = units.get(index);
			BigInteger allocation = index == units.size() - 1 ? split[0].add(split[1]) : split[0];
			db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,unit_number,allocation_vnd,disposition) values(?,?,?,?,?,?,?)",
				UUID.randomUUID(), enrollmentId, uuid(unit, "id"), offer.targetRunId(), number(unit, "unit_number"), allocation,
				instant(unit, "start_at").isBefore(now) ? "NOT_ENROLLED" : "ENROLLED");
		}
		db.update("update course_enrollments set lifecycle_status='TRANSFERRED' where id=? and lifecycle_status='ACTIVE'", offer.sourceEnrollmentId());
		db.update("update course_reservations set status='TRANSFERRED',updated_at=? where id=(select course_reservation_id from course_enrollments where id=?) and status='ENROLLED'",
			Timestamp.from(now), offer.sourceEnrollmentId());
		db.update("update course_transfer_holds set status='CONSUMED',released_at=? where transfer_offer_id=? and status='HELD'", Timestamp.from(now), offer.id());
		db.update("update course_transfer_offers set status='COMPLETED',completed_at=? where id=?", Timestamp.from(now), offer.id());
		db.update("update course_transfer_requests set status='COMPLETED',completed_at=? where id=?", Timestamp.from(now), offer.requestId());
		if (offer.creditVnd().compareTo(offer.targetTuitionVnd()) > 0) db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,
			 requester_account_id,request_reason)
			values(?,?,?,'TRANSFER_CREDIT_EXCESS',?,?,'Unused Course transfer credit refund')
			on conflict(course_enrollment_id,source_type) do nothing
			""", UUID.randomUUID(), offer.sourceEnrollmentId(), offer.sourceRunId(),
			offer.creditVnd().subtract(offer.targetTuitionVnd()), actorId);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER",
			offer.id(), Map.of("sourceEnrollmentId", offer.sourceEnrollmentId().toString(), "targetEnrollmentId", enrollmentId.toString(),
				"creditVnd", appliedCredit.toString(), "cashPaidVnd", cashPaid.toString())));
		notifications.write("course-transfer-completed:" + offer.id(), offer.memberAccountId(), "COURSE_TRANSFER_COMPLETED",
			"COURSE_TRANSFER_OFFER", offer.id(), Map.of("targetEnrollmentId", enrollmentId.toString(),
				"targetRunId", offer.targetRunId().toString()));
		for (UUID managerId : db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE'", UUID.class))
			notifications.write("course-transfer-completed:" + offer.id() + ":" + managerId, managerId,
				"COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(),
				Map.of("memberAccountId", offer.memberAccountId().toString(), "targetRunId", offer.targetRunId().toString()));
		return new CourseEnrollmentResponse(enrollmentId, orderId, offer.memberAccountId(), offer.targetRunId(),
			offer.targetTuitionVnd(), totalUnits, now);
	}

	private UUID createDifferenceOrder(TransferOffer offer, UUID memberId, String key, Instant now) {
		Instant expires = now.plusSeconds(900).isBefore(offer.expiresAt()) ? now.plusSeconds(900) : offer.expiresAt();
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase(Locale.ROOT);
		db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,tuition_vnd_snapshot,
			 currency_code,status,idempotency_key,created_at,updated_at) values(?,?,?,?,?,?,'VND','PENDING_PAYMENT',?,?,?)
			""", orderId, reference, memberId, memberId, offer.targetRunId(), offer.differenceVnd(),
			"transfer:" + offer.id(), Timestamp.from(now), Timestamp.from(now));
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,created_at,updated_at) values(?,?,?,?,'HELD',?,?,?)",
			reservationId, orderId, memberId, offer.targetRunId(), Timestamp.from(expires), Timestamp.from(now), Timestamp.from(now));
		db.update("update course_transfer_holds set status='RELEASED',expires_at=?,released_at=? where transfer_offer_id=? and status='HELD'",
			Timestamp.from(expires), Timestamp.from(now), offer.id());
		return orderId;
	}

	private void expireOffer(TransferOffer offer, Instant now) {
		if (!List.of("OFFERED", "PENDING_PAYMENT").contains(offer.status())) return;
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
		db.update("update course_transfer_holds set status='RELEASED',released_at=? where target_run_id=? and status='HELD' and expires_at<=?",
			Timestamp.from(now), runId, Timestamp.from(now));
		db.update("update course_transfer_requests set status='EXPIRED' where status='OFFERED' and id in "
			+ "(select transfer_request_id from course_transfer_offers where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT') and expires_at<=?)",
			runId, Timestamp.from(now));
		db.update("update course_transfer_offers set status='EXPIRED' where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT') and expires_at<=?",
			runId, Timestamp.from(now));
	}

	private BigInteger unusedCredit(UUID enrollmentId, Instant now) {
		List<CourseTransferPolicy.UnitAllocation> units = db.query("""
			select s.start_at,es.allocation_vnd from course_enrollment_sessions es
			join course_sessions s on s.id=es.course_session_id where es.course_enrollment_id=?
			""", (rs, n) -> new CourseTransferPolicy.UnitAllocation(rs.getTimestamp(1).toInstant(), rs.getBigDecimal(2).toBigIntegerExact()), enrollmentId);
		return CourseTransferPolicy.unusedCredit(units, now);
	}

	private long usedSeats(UUID runId, Instant now) { return usedSeatsExcludingOffer(runId, null, now); }
	private long usedSeatsExcludingOffer(UUID runId, UUID excludedOfferId, Instant now) {
		return db.queryForObject("""
			select (select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))
			 +(select count(*) from course_reservations where course_run_id=? and status='HELD' and expires_at>?)
			 +(select count(*) from course_waitlists where course_run_id=? and status='INVITED' and invitation_expires_at>?)
			 +(select count(*) from course_transfer_holds where target_run_id=? and status='HELD' and expires_at>?
			   and (?::uuid is null or transfer_offer_id<>?))
			""", Long.class, runId, runId, Timestamp.from(now), runId, Timestamp.from(now), runId,
			Timestamp.from(now), excludedOfferId, excludedOfferId);
	}

	private boolean hasScheduleConflict(UUID memberId, UUID sourceRunId, UUID targetRunId) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists(select 1 from course_sessions target
			 join course_enrollment_sessions existing_session on existing_session.disposition='ENROLLED'
			 join course_enrollments existing on existing.id=existing_session.course_enrollment_id
			 join course_sessions existing_time on existing_time.id=existing_session.course_session_id
			 where target.course_run_id=? and target.status in ('SCHEDULED','MAKEUP_SCHEDULED')
			 and existing.member_account_id=? and existing.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') and existing.course_run_id<>?
			 and existing_time.start_at<target.end_at and existing_time.end_at>target.start_at)
			""", Boolean.class, targetRunId, memberId, sourceRunId));
	}

	private TransferTarget target(ResultSet rs, Instant now) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		Timestamp next = rs.getTimestamp("next_session_at");
		return new TransferTarget(id, rs.getString("run_code"), rs.getString("course_code"), rs.getString("course_name"),
			rs.getBigDecimal("tuition_vnd").toBigIntegerExact(), rs.getInt("capacity"), usedSeats(id, now),
			rs.getTimestamp("registration_closes_at").toInstant(), next == null ? null : next.toInstant());
	}

	private void lockRuns(UUID first, UUID second) {
		for (UUID id : List.of(first, second).stream().distinct().sorted().toList())
			db.queryForObject("select id from course_runs where id=? for update", UUID.class, id);
	}
	private int capacity(UUID runId) { return db.queryForObject("select capacity from course_runs where id=?", Integer.class, runId); }
	private boolean activeEnrollmentExists(UUID memberId, UUID runId) { return Boolean.TRUE.equals(db.queryForObject(
		"select exists(select 1 from course_enrollments where member_account_id=? and course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING'))", Boolean.class, memberId, runId)); }
	private void active(UUID id, AccountRole role) { if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException(); }
	private String reason(String value) { if (value == null || value.isBlank() || value.trim().length() > 500) throw CourseTransferException.invalid("A reason of at most 500 characters is required."); return value.trim(); }
	private String key(String value) { if (value == null || value.isBlank() || value.trim().length() > 100) throw CourseTransferException.keyRequired(); return value.trim(); }

	private TransferRequest lockRequest(UUID id) { return db.query(requestSelect() + " where id=? for update", this::mapRequest, id).stream().findFirst().orElseThrow(CourseTransferException::notFound); }
	private TransferRequest findRequest(UUID id) { return db.query(requestSelect() + " where id=?", this::mapRequest, id).stream().findFirst().orElseThrow(CourseTransferException::notFound); }
	private TransferRequest findRequestByResponse(UUID id) { return db.query(requestSelect() + " where schedule_change_response_id=?", this::mapRequest, id).stream().findFirst().orElse(null); }
	private TransferOffer lockOffer(UUID memberId, UUID id) { return db.query(offerSelect() + " where id=? and member_account_id=? for update", this::mapOffer, id, memberId).stream().findFirst().orElseThrow(CourseTransferException::notFound); }
	private TransferOffer findOffer(UUID id) { return db.query(offerSelect() + " where id=?", this::mapOffer, id).stream().findFirst().orElseThrow(CourseTransferException::notFound); }
	private TransferOffer findOfferByRequest(UUID id) { return db.query(offerSelect() + " where transfer_request_id=?", this::mapOffer, id).stream().findFirst().orElse(null); }
	private CourseEnrollmentResponse targetEnrollment(UUID offerId) {
		Map<String,Object> row=rows("select id,course_order_id,member_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at from course_enrollments where source_transfer_offer_id=?",offerId).stream().findFirst().orElseThrow(CourseTransferException::notFound);
		return new CourseEnrollmentResponse(uuid(row,"id"),uuid(row,"course_order_id"),uuid(row,"member_account_id"),uuid(row,"course_run_id"),amount(row,"amount_paid_vnd_snapshot"),number(row,"total_units_snapshot"),instant(row,"enrolled_at"));
	}
	private String requestSelect() { return "select * from course_transfer_requests"; }
	private String offerSelect() { return "select * from course_transfer_offers"; }
	private TransferRequest mapRequest(ResultSet rs,int n)throws SQLException{return new TransferRequest(rs.getObject("id",UUID.class),rs.getObject("schedule_change_response_id",UUID.class),rs.getObject("source_enrollment_id",UUID.class),rs.getObject("source_run_id",UUID.class),rs.getObject("member_account_id",UUID.class),rs.getString("status"),rs.getObject("target_run_id",UUID.class),rs.getString("reason"),rs.getTimestamp("requested_at").toInstant(),rs.getObject("decided_by_account_id",UUID.class),rs.getTimestamp("decided_at")==null?null:rs.getTimestamp("decided_at").toInstant());}
	private TransferOffer mapOffer(ResultSet rs,int n)throws SQLException{return new TransferOffer(rs.getObject("id",UUID.class),rs.getObject("transfer_request_id",UUID.class),rs.getObject("source_enrollment_id",UUID.class),rs.getObject("source_run_id",UUID.class),rs.getObject("target_run_id",UUID.class),rs.getObject("member_account_id",UUID.class),rs.getBigDecimal("credit_vnd_snapshot").toBigIntegerExact(),rs.getBigDecimal("target_tuition_vnd_snapshot").toBigIntegerExact(),rs.getBigDecimal("difference_vnd_snapshot").toBigIntegerExact(),rs.getTimestamp("expires_at").toInstant(),rs.getString("status"),rs.getObject("difference_order_id",UUID.class),rs.getTimestamp("accepted_at")==null?null:rs.getTimestamp("accepted_at").toInstant());}
	private List<Map<String,Object>> rows(String sql,Object...args){return db.queryForList(sql,args);}
	private static UUID uuid(Map<String,Object> row,String key){return (UUID)row.get(key);}
	private static String text(Map<String,Object> row,String key){return row.get(key)==null?null:row.get(key).toString();}
	private static int number(Map<String,Object> row,String key){return ((Number)row.get(key)).intValue();}
	private static BigInteger amount(Map<String,Object> row,String key){return new BigDecimal(row.get(key).toString()).toBigIntegerExact();}
	private static Instant instant(Map<String,Object> row,String key){Object value=row.get(key);return value==null?null:value instanceof Instant i?i:((Timestamp)value).toInstant();}
}
