package com.scms.backend.course;

import static com.scms.backend.course.CourseWithdrawalApiModels.WithdrawalRequest;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
class CourseWithdrawalService {
	private static final Logger log = LoggerFactory.getLogger(CourseWithdrawalService.class);
	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final CourseWithdrawalWaitlistPromotion promotions;
	private final Clock clock;

	CourseWithdrawalService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits,
			NotificationWriter notifications, CourseWithdrawalWaitlistPromotion promotions, Clock clock) {
		this.db = db; this.accounts = accounts; this.audits = audits; this.notifications = notifications;
		this.promotions = promotions; this.clock = clock;
	}

	@Transactional
	WithdrawalRequest createForMember(UUID memberId, UUID enrollmentId, String reason) {
		return create(memberId, AccountRole.MEMBER, memberId, enrollmentId, reason);
	}

	@Transactional
	WithdrawalRequest createForReceptionist(UUID receptionistId, UUID memberId, UUID enrollmentId, String reason) {
		return create(receptionistId, AccountRole.RECEPTIONIST, memberId, enrollmentId, reason);
	}

	@Transactional
	WithdrawalRequest createForManager(UUID managerId, UUID memberId, UUID enrollmentId, String reason) {
		return create(managerId, AccountRole.MANAGER, memberId, enrollmentId, reason);
	}

	@Transactional
	WithdrawalRequest createForScheduleChange(UUID memberId, UUID responseId) {
		active(memberId, AccountRole.MEMBER, true);
		List<Map<String, Object>> response = db.queryForList("""
			select r.id response_id,r.response,r.status,r.cause_code,r.change_event_id,
				es.course_enrollment_id,e.course_run_id,e.member_account_id
			from course_schedule_change_responses r
			join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			where r.id=? and e.member_account_id=?
			""", responseId, memberId);
		if (response.isEmpty()) throw CourseException.responseNotFound();
		UUID existing = db.queryForList("select id from course_withdrawal_requests where schedule_change_response_id=?",
			responseId).stream().map(row -> uuid(row, "id")).findFirst().orElse(null);
		if (existing != null) return find(existing);
		Map<String, Object> change = response.getFirst();
		if (!"REQUEST_WITHDRAWAL".equals(text(change, "response")) || !"RESPONDED".equals(text(change, "status")))
			throw CourseException.responseConflict();
		UUID enrollmentId = uuid(change, "course_enrollment_id"), runId = uuid(change, "course_run_id");
		CourseWithdrawalCause cause = CourseWithdrawalCause.valueOf(text(change, "cause_code"));
		requireWithdrawableRun(lockRun(runId));
		Map<String, Object> enrollment = lockEnrollment(enrollmentId, memberId);
		if (!"ACTIVE".equals(text(enrollment, "lifecycle_status"))) {
			if ("WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status"))) {
				UUID pending = db.queryForList("select id from course_withdrawal_requests where course_enrollment_id=? and status='PENDING'",
					enrollmentId).stream().map(row -> uuid(row, "id")).findFirst().orElse(null);
				if (pending != null) return find(pending);
			}
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ENROLLMENT_INACTIVE",
				"Only an active Enrollment can be withdrawn.");
		}
		if (Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from course_transfer_requests where "
			+ "source_enrollment_id=? and status in ('PENDING','OFFERED'))", Boolean.class, enrollmentId)))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_TRANSFER_OPEN",
				"The Enrollment already has an open transfer request.");
		Instant now = clock.instant();
		RefundSnapshot snapshot = refundSnapshot(enrollmentId, number(enrollment, "total_units_snapshot"),
			amount(enrollment, "amount_paid_vnd_snapshot"), now);
		CourseWithdrawalPolicy.Decision decision = CourseWithdrawalPolicy.evaluate(cause, snapshot.remainingUnits(),
			snapshot.totalUnits(), snapshot.unusedAllocation());
		if (!decision.eligible()) throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED",
			"Withdrawal is no longer allowed after the final unit has started.");
		return insertRequest(memberId, AccountRole.MEMBER, memberId, enrollmentId, runId,
			"Withdrawal requested after a material schedule change", cause, responseId,
			uuid(change, "change_event_id"), snapshot, now);
	}

	@Transactional(readOnly = true)
	List<WithdrawalRequest> listForMember(UUID memberId) {
		active(memberId, AccountRole.MEMBER, false);
		return db.query(select() + " where w.member_account_id=? order by w.requested_at desc,w.id desc",
			this::map, memberId);
	}

	@Transactional(readOnly = true)
	WithdrawalRequest detailForMember(UUID memberId, UUID requestId) {
		active(memberId, AccountRole.MEMBER, false);
		return db.query(select() + " where w.id=? and w.member_account_id=?", this::map,
			requestId, memberId)
			.stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	@Transactional(readOnly = true)
	List<WithdrawalRequest> managerQueue(UUID managerId) {
		active(managerId, AccountRole.MANAGER, false);
		return db.query(select() + " order by case when w.status='PENDING' then 0 else 1 end,w.requested_at,w.id",
			this::map);
	}

	@Transactional(readOnly = true)
	WithdrawalRequest managerDetail(UUID managerId, UUID requestId) {
		active(managerId, AccountRole.MANAGER, false);
		return find(requestId);
	}

	@Transactional
	WithdrawalRequest approve(UUID managerId, UUID requestId, String reason) {
		active(managerId, AccountRole.MANAGER, true);
		String normalizedReason = reason(reason);
		WithdrawalRequest request = lockForDecision(requestId, null);
		if (!"PENDING".equals(request.status())) throw invalidTransition();
		if ("CANCELLED".equals(request.courseRunStatus()) || request.snapshotCapturedAt() == null) throw invalidTransition();
		Map<String, Object> enrollment = lockEnrollment(request.enrollmentId(), request.memberAccountId());
		if (!"WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status"))) throw invalidTransition();
		Instant now = clock.instant();
		BigInteger refund = request.refundAmountVnd();
		db.update("update course_enrollments set lifecycle_status='WITHDRAWN' where id=? and lifecycle_status='WITHDRAWAL_PENDING'",
			request.enrollmentId());
		db.update("update course_reservations set status='WITHDRAWN',updated_at=? where id=(select course_reservation_id "
			+ "from course_enrollments where id=?) and status='ENROLLED'", Timestamp.from(now), request.enrollmentId());
		if (db.update("""
			update course_withdrawal_requests set status='APPROVED',decision_reason=?,decided_by_account_id=?,decided_at=?
			where id=? and status='PENDING'
			""", normalizedReason, managerId, Timestamp.from(now), requestId) != 1) throw invalidTransition();
		db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,
			 requester_account_id,request_reason)
			values(?,?,?, ?,?,?,?) on conflict(course_enrollment_id,source_type) do nothing
			""", UUID.randomUUID(), request.enrollmentId(), request.courseRunId(),
			request.cause() == CourseWithdrawalCause.VOLUNTARY ? "MEMBER_WITHDRAWAL" : "MATERIAL_CHANGE", refund,
			request.requesterAccountId(), request.requestReason());
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_WITHDRAWAL_APPROVED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"),
			Map.of("status", "APPROVED", "remainingUnits", request.remainingUnits(),
				"totalUnits", request.totalUnits(), "refundBasisVnd", request.refundBasisVnd().toString(),
				"refundAmountVnd", refund.toString())));
		notifications.write("course-withdrawal-approved:" + requestId, request.memberAccountId(),
			"COURSE_WITHDRAWAL_APPROVED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("refundAmountVnd", refund.toString()));
		promoteAfterCommit(request.courseRunId(), now);
		return find(requestId);
	}

	@Transactional
	WithdrawalRequest reject(UUID managerId, UUID requestId, String reason) {
		active(managerId, AccountRole.MANAGER, true);
		String normalizedReason = reason(reason);
		WithdrawalRequest request = lockForDecision(requestId, null);
		if (!"PENDING".equals(request.status())) throw invalidTransition();
		Map<String, Object> enrollment = lockEnrollment(request.enrollmentId(), request.memberAccountId());
		if (!"WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status"))) throw invalidTransition();
		Instant now = clock.instant();
		db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'",
			request.enrollmentId());
		db.update("update course_withdrawal_requests set status='REJECTED',decision_reason=?,decided_by_account_id=?,decided_at=? where id=? and status='PENDING'",
			normalizedReason, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_WITHDRAWAL_REJECTED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"),
			Map.of("status", "REJECTED")));
		notifications.write("course-withdrawal-rejected:" + requestId, request.memberAccountId(),
			"COURSE_WITHDRAWAL_REJECTED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("reason", normalizedReason));
		return find(requestId);
	}

	@Transactional
	WithdrawalRequest cancelForMember(UUID memberId, UUID requestId, String reason) {
		return cancel(memberId, AccountRole.MEMBER, memberId, requestId, reason);
	}

	@Transactional
	WithdrawalRequest cancelForReceptionist(UUID receptionistId, UUID memberId, UUID requestId, String reason) {
		return cancel(receptionistId, AccountRole.RECEPTIONIST, memberId, requestId, reason);
	}

	private WithdrawalRequest cancel(UUID actorId, AccountRole role, UUID memberId, UUID requestId, String reason) {
		active(actorId, role, true);
		String normalizedReason = reason(reason);
		WithdrawalRequest request = lockForDecision(requestId, memberId);
		if ("CANCELLED".equals(request.status())) return request;
		if (!"PENDING".equals(request.status())) throw invalidTransition();
		lockEnrollment(request.enrollmentId(), memberId);
		if (db.update("update course_withdrawal_requests set status='CANCELLED',cancelled_at=?,cancelled_by_account_id=?,"
			+ "cancellation_reason=? where id=? and status='PENDING'", Timestamp.from(clock.instant()), actorId,
			normalizedReason, requestId) != 1) throw invalidTransition();
		db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'",
			request.enrollmentId());
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST",
			requestId, normalizedReason, Map.of("status", "PENDING"), Map.of("status", "CANCELLED")));
		notifications.write("course-withdrawal-cancelled:" + requestId, memberId, "COURSE_WITHDRAWAL_CANCELLED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("reason", normalizedReason));
		return find(requestId);
	}

	private WithdrawalRequest create(UUID requesterId, AccountRole requesterRole, UUID memberId, UUID enrollmentId,
			String reason) {
		String normalizedReason = reason(reason);
		active(requesterId, requesterRole, true);
		if (!requesterId.equals(memberId)) active(memberId, AccountRole.MEMBER, true);
		Map<String, Object> visible = db.queryForList("select course_run_id from course_enrollments where id=? and member_account_id=?",
			enrollmentId, memberId).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
		UUID runId = uuid(visible, "course_run_id");
		requireWithdrawableRun(lockRun(runId));
		Map<String, Object> enrollment = lockEnrollment(enrollmentId, memberId);
		if ("WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status")) && Boolean.TRUE.equals(db.queryForObject(
			"select exists(select 1 from course_withdrawal_requests where course_enrollment_id=? and status='PENDING')",
			Boolean.class, enrollmentId)))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ALREADY_PENDING",
				"The Enrollment already has a pending withdrawal request.");
		if (!"ACTIVE".equals(text(enrollment, "lifecycle_status")))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ENROLLMENT_INACTIVE",
				"Only an active Enrollment can be withdrawn.");
		if (Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from course_transfer_requests where "
			+ "source_enrollment_id=? and status in ('PENDING','OFFERED'))", Boolean.class, enrollmentId)))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_TRANSFER_OPEN",
				"The Enrollment already has an open transfer request.");
		Instant now = clock.instant();
		RefundSnapshot snapshot = refundSnapshot(enrollmentId, number(enrollment, "total_units_snapshot"),
			amount(enrollment, "amount_paid_vnd_snapshot"), now);
		if (!CourseWithdrawalPolicy.canWithdraw(snapshot.remainingUnits(), snapshot.totalUnits()))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED",
				"Withdrawal is no longer allowed after the final unit has started.");
		return insertRequest(requesterId, requesterRole, memberId, enrollmentId, runId, normalizedReason,
			CourseWithdrawalCause.VOLUNTARY, null, null, snapshot, now);
	}

	private WithdrawalRequest insertRequest(UUID requesterId, AccountRole requesterRole, UUID memberId,
			UUID enrollmentId, UUID runId, String normalizedReason, CourseWithdrawalCause cause,
			UUID responseId, UUID eventId, RefundSnapshot snapshot, Instant now) {
		CourseWithdrawalPolicy.Decision decision = CourseWithdrawalPolicy.evaluate(cause, snapshot.remainingUnits(),
			snapshot.totalUnits(), snapshot.unusedAllocation());
		if (!decision.eligible()) throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED",
			"Withdrawal is no longer allowed after the final unit has started.");
		UUID requestId = UUID.randomUUID();
		db.update("""
			insert into course_withdrawal_requests(id,course_enrollment_id,course_run_id,member_account_id,
			 requester_account_id,request_reason,requested_at,cause_code,schedule_change_response_id,change_event_id,
			 snapshot_captured_at,total_units_snapshot,remaining_units_snapshot,refund_basis_vnd,refund_amount_vnd,
			 withdrawal_eligible_snapshot,withdrawal_reason_snapshot)
			values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			""", requestId, enrollmentId, runId, memberId, requesterId, normalizedReason, Timestamp.from(now),
			cause.name(), responseId, eventId, Timestamp.from(now), snapshot.totalUnits(), snapshot.remainingUnits(),
			decision.refundBasisVnd(), decision.refundAmountVnd(), decision.eligible(), decision.reason());
		db.update("update course_enrollments set lifecycle_status='WITHDRAWAL_PENDING' where id=? and lifecycle_status='ACTIVE'",
			enrollmentId);
		Map<String, Object> captured = new LinkedHashMap<>(Map.of("status", "PENDING", "enrollmentId", enrollmentId.toString(),
			"courseRunId", runId.toString(), "cause", cause.name(), "remainingUnits", snapshot.remainingUnits(),
			"totalUnits", snapshot.totalUnits(), "refundBasisVnd", decision.refundBasisVnd().toString(),
			"refundAmountVnd", decision.refundAmountVnd().toString()));
		if (responseId != null) captured.put("scheduleChangeResponseId", responseId.toString());
		audits.save(new AuditEvent(UUID.randomUUID(), requesterId, "COURSE_WITHDRAWAL_REQUESTED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of(), captured));
		if (!requesterId.equals(memberId)) notifications.write("course-withdrawal-requested:" + requestId,
			memberId, "COURSE_WITHDRAWAL_REQUESTED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("requesterRole", requesterRole.name()));
		for (UUID managerId : db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE' order by id",
			UUID.class)) notifications.write("course-withdrawal-review:" + requestId + ":" + managerId, managerId,
			"COURSE_WITHDRAWAL_REVIEW_REQUIRED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("courseRunId", runId.toString(), "cause", cause.name()));
		return find(requestId);
	}

	private RefundSnapshot refundSnapshot(UUID enrollmentId, int totalUnits, BigInteger amountPaid, Instant now) {
		List<Map<String, Object>> units = db.queryForList("""
			select s.start_at,es.allocation_vnd from course_enrollment_sessions es
			join course_sessions s on s.id=es.course_session_id
			where es.course_enrollment_id=? order by es.unit_number for update of es
			""", enrollmentId);
		int remaining = 0;
		BigInteger unused = BigInteger.ZERO;
		for (Map<String, Object> unit : units) if (instant(unit, "start_at").isAfter(now)) {
			remaining++;
			unused = unused.add(amount(unit, "allocation_vnd"));
		}
		return new RefundSnapshot(totalUnits, remaining, unused.min(amountPaid));
	}

	private void promoteAfterCommit(UUID runId, Instant now) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override public void afterCommit() {
				try { promotions.promote(runId, now); }
				catch (RuntimeException exception) {
					log.error("Course waitlist promotion failed after withdrawal for run {}", runId, exception);
				}
			}
		});
	}

	private void active(UUID id, AccountRole role, boolean lock) {
		if (!lock) {
			if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE))
				throw new InvalidAuthenticatedAccountException();
			return;
		}
		Account account = accounts.findByIdForUpdate(id).orElseThrow(InvalidAuthenticatedAccountException::new);
		if (account.getRole() != role || account.getStatus() != AccountStatus.ACTIVE)
			throw new InvalidAuthenticatedAccountException();
	}

	private String lockRun(UUID runId) {
		return db.queryForObject("select status from course_runs where id=? for update", String.class, runId);
	}
	private void requireWithdrawableRun(String status) {
		if (!List.of("OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS").contains(status))
			throw CourseWithdrawalException.conflict("COURSE_RUN_NOT_WITHDRAWABLE", "The Course Run no longer accepts withdrawal requests.");
	}

	private WithdrawalRequest lockForDecision(UUID requestId, UUID memberId) {
		WithdrawalRequest visible = find(requestId);
		if (memberId != null && !memberId.equals(visible.memberAccountId())) throw CourseWithdrawalException.notFound();
		active(visible.memberAccountId(), AccountRole.MEMBER, true);
		lockRun(visible.courseRunId());
		return lock(requestId);
	}

	private Map<String, Object> lockEnrollment(UUID enrollmentId, UUID memberId) {
		return db.queryForList("select id,course_run_id,lifecycle_status,total_units_snapshot,amount_paid_vnd_snapshot from course_enrollments "
			+ "where id=? and member_account_id=? for update", enrollmentId, memberId).stream().findFirst()
			.orElseThrow(CourseWithdrawalException::notFound);
	}
	private String reason(String reason) {
		if (reason == null || reason.isBlank() || reason.trim().length() > 1000)
			throw CourseWithdrawalException.invalid("A non-blank reason of at most 1000 characters is required.");
		return reason.trim();
	}
	private CourseWithdrawalException invalidTransition() {
		return CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_INVALID_TRANSITION",
			"The withdrawal request is no longer pending.");
	}
	private WithdrawalRequest lock(UUID id) {
		return db.query(select() + " where w.id=? for update of w", this::map, id).stream().findFirst()
			.orElseThrow(CourseWithdrawalException::notFound);
	}
	private WithdrawalRequest find(UUID id) {
		return db.query(select() + " where w.id=?", this::map, id).stream().findFirst()
			.orElseThrow(CourseWithdrawalException::notFound);
	}
	private String select() { return "select w.*,a.id member_id,mp.member_code,a.full_name member_name,c.id course_id,"
		+ "c.code course_code,c.name course_name,r.run_code,r.status course_run_status,e.lifecycle_status "
		+ "from course_withdrawal_requests w join course_enrollments e on e.id=w.course_enrollment_id "
		+ "join accounts a on a.id=e.member_account_id join member_profiles mp on mp.account_id=a.id "
		+ "join course_runs r on r.id=e.course_run_id join courses c on c.id=r.course_id"; }
	private WithdrawalRequest map(ResultSet rs, int row) throws SQLException {
		Instant capturedAt = instant(rs.getTimestamp("snapshot_captured_at"));
		boolean pending = "PENDING".equals(rs.getString("status"));
		boolean eligible = pending && capturedAt != null && !"CANCELLED".equals(rs.getString("course_run_status"));
		BigInteger refund = amount(rs.getBigDecimal("refund_amount_vnd"));
		String source = capturedAt != null ? "REQUEST_TIME" : refund != null ? "LEGACY_APPROVAL" : "UNAVAILABLE";
		String eligibilityReason = eligible ? rs.getString("withdrawal_reason_snapshot")
			: capturedAt == null ? "LEGACY_SNAPSHOT_MISSING" : "REQUEST_NOT_PENDING";
		return new WithdrawalRequest(rs.getObject("id", UUID.class), rs.getObject("course_enrollment_id", UUID.class),
			rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class),
			rs.getObject("requester_account_id", UUID.class), rs.getString("status"), rs.getString("request_reason"),
			rs.getString("decision_reason"), CourseWithdrawalCause.valueOf(rs.getString("cause_code")), eligible,
			eligibilityReason, rs.getTimestamp("requested_at").toInstant(), rs.getObject("decided_by_account_id", UUID.class),
			instant(rs.getTimestamp("decided_at")), rs.getObject("total_units_snapshot", Integer.class),
			rs.getObject("remaining_units_snapshot", Integer.class), amount(rs.getBigDecimal("refund_basis_vnd")), refund,
			rs.getObject("member_id", UUID.class), rs.getString("member_code"), rs.getString("member_name"),
			rs.getObject("course_id", UUID.class), rs.getString("course_code"), rs.getString("course_name"),
			rs.getString("run_code"), rs.getString("course_run_status"), eligible ? refund : null,
			rs.getObject("schedule_change_response_id", UUID.class), rs.getObject("change_event_id", UUID.class),
			capturedAt, source, rs.getObject("withdrawal_eligible_snapshot", Boolean.class),
			rs.getString("withdrawal_reason_snapshot"), instant(rs.getTimestamp("cancelled_at")),
			rs.getObject("cancelled_by_account_id", UUID.class), rs.getString("cancellation_reason"), pending,
			"WITHDRAWAL_PENDING".equals(rs.getString("lifecycle_status")));
	}
	private record RefundSnapshot(int totalUnits, int remainingUnits, BigInteger unusedAllocation) { }
	private static UUID uuid(Map<String, Object> row, String key) { return (UUID) row.get(key); }
	private static String text(Map<String, Object> row, String key) { return row.get(key) == null ? null : row.get(key).toString(); }
	private static int number(Map<String, Object> row, String key) { return ((Number) row.get(key)).intValue(); }
	private static BigInteger amount(Map<String, Object> row, String key) { return new BigDecimal(row.get(key).toString()).toBigIntegerExact(); }
	private static BigInteger amount(BigDecimal value) { return value == null ? null : value.toBigIntegerExact(); }
	private static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key); return value instanceof Instant instant ? instant : ((Timestamp) value).toInstant();
	}
	private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
