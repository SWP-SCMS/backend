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
import org.springframework.dao.DataIntegrityViolationException;
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

	@Transactional(readOnly = true)
	List<WithdrawalRequest> listForMember(UUID memberId) {
		active(memberId, AccountRole.MEMBER, false);
		return db.query(select() + " where member_account_id=? order by requested_at desc,id desc",
			this::map, memberId);
	}

	@Transactional(readOnly = true)
	WithdrawalRequest detailForMember(UUID memberId, UUID requestId) {
		active(memberId, AccountRole.MEMBER, false);
		return db.query(select() + " where id=? and member_account_id=?", this::map, requestId, memberId)
			.stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	@Transactional(readOnly = true)
	List<WithdrawalRequest> managerQueue(UUID managerId) {
		active(managerId, AccountRole.MANAGER, false);
		return db.query(select() + " order by case when status='PENDING' then 0 else 1 end,requested_at,id", this::map);
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
		WithdrawalRequest request = lock(requestId);
		if (!"PENDING".equals(request.status())) throw invalidTransition();
		active(request.memberAccountId(), AccountRole.MEMBER, true);
		lockRun(request.courseRunId());
		Map<String, Object> enrollment = lockEnrollment(request.enrollmentId(), request.memberAccountId());
		if (!"WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status"))) throw invalidTransition();
		Instant now = clock.instant();
		RefundSnapshot snapshot = refundSnapshot(request.enrollmentId(), number(enrollment, "total_units_snapshot"),
			amount(enrollment, "amount_paid_vnd_snapshot"), now);
		if (!CourseWithdrawalPolicy.canWithdraw(snapshot.remainingUnits(), snapshot.totalUnits()))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED",
				"Withdrawal is no longer allowed after the final unit has started.");
		BigInteger refund = CourseWithdrawalPolicy.refund(snapshot.remainingUnits(), snapshot.totalUnits(),
			snapshot.unusedAllocation());
		db.update("update course_enrollments set lifecycle_status='WITHDRAWN' where id=? and lifecycle_status='WITHDRAWAL_PENDING'",
			request.enrollmentId());
		db.update("update course_reservations set status='WITHDRAWN',updated_at=? where id=(select course_reservation_id "
			+ "from course_enrollments where id=?) and status='ENROLLED'", Timestamp.from(now), request.enrollmentId());
		db.update("""
			update course_withdrawal_requests set status='APPROVED',decision_reason=?,decided_by_account_id=?,decided_at=?,
			 total_units_snapshot=?,remaining_units_snapshot=?,refund_basis_vnd=?,refund_amount_vnd=? where id=?
			""", normalizedReason, managerId, Timestamp.from(now), snapshot.totalUnits(), snapshot.remainingUnits(),
			snapshot.unusedAllocation(), refund, requestId);
		db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd)
			values(?,?,?,'MEMBER_WITHDRAWAL',?) on conflict(course_enrollment_id,source_type) do nothing
			""", UUID.randomUUID(), request.enrollmentId(), request.courseRunId(), refund);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_WITHDRAWAL_APPROVED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"),
			Map.of("status", "APPROVED", "remainingUnits", snapshot.remainingUnits(),
				"totalUnits", snapshot.totalUnits(), "refundBasisVnd", snapshot.unusedAllocation().toString(),
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
		WithdrawalRequest request = lock(requestId);
		if (!"PENDING".equals(request.status())) throw invalidTransition();
		active(request.memberAccountId(), AccountRole.MEMBER, true);
		lockRun(request.courseRunId());
		Map<String, Object> enrollment = lockEnrollment(request.enrollmentId(), request.memberAccountId());
		if (!"WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status"))) throw invalidTransition();
		Instant now = clock.instant();
		db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'",
			request.enrollmentId());
		db.update("update course_withdrawal_requests set status='REJECTED',decision_reason=?,decided_by_account_id=?,decided_at=? where id=?",
			normalizedReason, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_WITHDRAWAL_REJECTED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"),
			Map.of("status", "REJECTED")));
		notifications.write("course-withdrawal-rejected:" + requestId, request.memberAccountId(),
			"COURSE_WITHDRAWAL_REJECTED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("reason", normalizedReason));
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
		lockRun(runId);
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
		UUID requestId = UUID.randomUUID();
		try {
			db.update("""
				insert into course_withdrawal_requests(id,course_enrollment_id,course_run_id,member_account_id,
				 requester_account_id,request_reason,requested_at) values(?,?,?,?,?,?,?)
				""", requestId, enrollmentId, runId, memberId, requesterId, normalizedReason, Timestamp.from(now));
		}
		catch (DataIntegrityViolationException exception) {
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ALREADY_PENDING",
				"The Enrollment already has a pending withdrawal request.");
		}
		db.update("update course_enrollments set lifecycle_status='WITHDRAWAL_PENDING' where id=? and lifecycle_status='ACTIVE'",
			enrollmentId);
		audits.save(new AuditEvent(UUID.randomUUID(), requesterId, "COURSE_WITHDRAWAL_REQUESTED",
			"COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of(),
			Map.of("status", "PENDING", "enrollmentId", enrollmentId.toString(), "courseRunId", runId.toString())));
		if (!requesterId.equals(memberId)) notifications.write("course-withdrawal-requested:" + requestId,
			memberId, "COURSE_WITHDRAWAL_REQUESTED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("requesterRole", requesterRole.name()));
		for (UUID managerId : db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE' order by id",
			UUID.class)) notifications.write("course-withdrawal-review:" + requestId + ":" + managerId, managerId,
			"COURSE_WITHDRAWAL_REVIEW_REQUIRED", "COURSE_WITHDRAWAL_REQUEST", requestId,
			Map.of("courseRunId", runId.toString()));
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

	private void lockRun(UUID runId) {
		db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
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
		return db.query(select() + " where id=? for update", this::map, id).stream().findFirst()
			.orElseThrow(CourseWithdrawalException::notFound);
	}
	private WithdrawalRequest find(UUID id) {
		return db.query(select() + " where id=?", this::map, id).stream().findFirst()
			.orElseThrow(CourseWithdrawalException::notFound);
	}
	private String select() { return "select * from course_withdrawal_requests"; }
	private WithdrawalRequest map(ResultSet rs, int row) throws SQLException {
		return new WithdrawalRequest(rs.getObject("id", UUID.class), rs.getObject("course_enrollment_id", UUID.class),
			rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class),
			rs.getObject("requester_account_id", UUID.class), rs.getString("status"), rs.getString("request_reason"),
			rs.getString("decision_reason"), rs.getTimestamp("requested_at").toInstant(),
			rs.getObject("decided_by_account_id", UUID.class), instant(rs.getTimestamp("decided_at")),
			rs.getObject("total_units_snapshot", Integer.class), rs.getObject("remaining_units_snapshot", Integer.class),
			amount(rs.getBigDecimal("refund_basis_vnd")), amount(rs.getBigDecimal("refund_amount_vnd")));
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
