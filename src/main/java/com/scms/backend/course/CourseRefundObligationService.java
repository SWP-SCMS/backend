package com.scms.backend.course;

import static com.scms.backend.course.CourseRefundApiModels.RefundObligation;
import static com.scms.backend.course.CourseRefundApiModels.RefundObligationPage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CourseRefundObligationService {
	private static final Set<String> COMMITTED = Set.of("APPROVED", "DISBURSEMENT_RECORDED",
		"PENDING_VERIFICATION", "COMPLETED");
	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	CourseRefundObligationService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.db = db; this.accounts = accounts; this.audits = audits; this.notifications = notifications;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	RefundObligationPage list(UUID managerId, int page, int size, String status) {
		active(managerId, false);
		if (page < 0 || size < 1 || size > 100) throw CourseRefundException.invalid("Invalid page or size.");
		String normalizedStatus = status(status);
		long total = db.queryForObject("select count(*) from course_refund_obligations where "
			+ "(cast(? as varchar) is null or status=?)", Long.class, normalizedStatus, normalizedStatus);
		List<RefundObligation> items = db.query(select() + " where (cast(? as varchar) is null or status=?) "
			+ "order by case when status='PENDING_REVIEW' then 0 else 1 end,created_at,id limit ? offset ?",
			this::map, normalizedStatus, normalizedStatus, size, (long) page * size);
		return new RefundObligationPage(items, page, size, total, (int) ((total + size - 1) / size));
	}

	@Transactional(readOnly = true)
	RefundObligation detail(UUID managerId, UUID obligationId) {
		active(managerId, false);
		return find(obligationId);
	}

	@Transactional
	RefundObligation approve(UUID managerId, UUID obligationId, Long expectedVersion, String reason) {
		String normalizedReason = reason(reason);
		DecisionContext context = lockForDecision(managerId, obligationId);
		RefundObligation obligation = context.obligation();
		if ("APPROVED".equals(obligation.status()) && managerId.equals(obligation.approvedByAccountId())
				&& normalizedReason.equals(obligation.approvalReason())) return obligation;
		if (!CourseRefundStateMachine.canTransition(CourseRefundStatus.valueOf(obligation.status()),
				CourseRefundStatus.APPROVED)) throw alreadyDecided();
		version(obligation, expectedVersion);

		BigInteger committed = context.all().stream().filter(item -> !item.id().equals(obligationId))
			.filter(item -> COMMITTED.contains(item.status())).map(RefundObligation::approvedAmountVnd)
			.filter(java.util.Objects::nonNull).reduce(BigInteger.ZERO, BigInteger::add);
		BigInteger available = context.amountPaid().subtract(committed).max(BigInteger.ZERO);
		if (obligation.grossObligationVnd().compareTo(available) > 0) {
			throw CourseRefundException.conflict("COURSE_REFUND_AMOUNT_EXCEEDS_AVAILABLE",
				"The refund would exceed the Enrollment paid amount after prior committed refunds.");
		}
		Instant now = clock.instant();
		int changed = db.update("""
			update course_refund_obligations set status='APPROVED',approved_amount_vnd=amount_vnd,
			 approved_by_account_id=?,approval_reason=?,approved_at=?,version=version+1,updated_at=?
			where id=? and status='PENDING_REVIEW' and version=?
			""", managerId, normalizedReason, Timestamp.from(now), Timestamp.from(now), obligationId, expectedVersion);
		if (changed != 1) throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_APPROVED",
			"COURSE_REFUND_OBLIGATION", obligationId, normalizedReason, Map.of("status", "PENDING_REVIEW"),
			Map.of("status", "APPROVED", "amountVnd", obligation.grossObligationVnd().toString(),
				"source", obligation.source())));
		notifications.write("course-refund-approved:" + obligationId, obligation.memberAccountId(),
			"COURSE_REFUND_APPROVED", "COURSE_REFUND_OBLIGATION", obligationId,
			Map.of("amountVnd", obligation.grossObligationVnd().toString(), "source", obligation.source()));
		return find(obligationId);
	}

	@Transactional
	RefundObligation reject(UUID managerId, UUID obligationId, Long expectedVersion, String reason) {
		String normalizedReason = reason(reason);
		DecisionContext context = lockForDecision(managerId, obligationId);
		RefundObligation obligation = context.obligation();
		if ("REJECTED".equals(obligation.status()) && managerId.equals(obligation.rejectedByAccountId())
				&& normalizedReason.equals(obligation.rejectionReason())) return obligation;
		if (!CourseRefundStateMachine.canTransition(CourseRefundStatus.valueOf(obligation.status()),
				CourseRefundStatus.REJECTED)) throw alreadyDecided();
		version(obligation, expectedVersion);
		Instant now = clock.instant();
		int changed = db.update("""
			update course_refund_obligations set status='REJECTED',rejected_by_account_id=?,rejection_reason=?,
			 rejected_at=?,version=version+1,updated_at=? where id=? and status='PENDING_REVIEW' and version=?
			""", managerId, normalizedReason, Timestamp.from(now), Timestamp.from(now), obligationId, expectedVersion);
		if (changed != 1) throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_REJECTED",
			"COURSE_REFUND_OBLIGATION", obligationId, normalizedReason, Map.of("status", "PENDING_REVIEW"),
			Map.of("status", "REJECTED", "source", obligation.source())));
		notifications.write("course-refund-rejected:" + obligationId, obligation.memberAccountId(),
			"COURSE_REFUND_REJECTED", "COURSE_REFUND_OBLIGATION", obligationId,
			Map.of("source", obligation.source()));
		return find(obligationId);
	}

	private DecisionContext lockForDecision(UUID managerId, UUID obligationId) {
		active(managerId, true);
		Map<String, Object> visible = db.queryForList("select course_enrollment_id,course_run_id "
			+ "from course_refund_obligations where id=?", obligationId).stream().findFirst()
			.orElseThrow(CourseRefundException::notFound);
		UUID runId = (UUID) visible.get("course_run_id");
		UUID enrollmentId = (UUID) visible.get("course_enrollment_id");
		db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
		BigInteger amountPaid = db.queryForObject("select amount_paid_vnd_snapshot from course_enrollments "
			+ "where id=? and course_run_id=? for update", BigDecimal.class, enrollmentId, runId).toBigIntegerExact();
		List<RefundObligation> all = db.query(select() + " where course_enrollment_id=? order by id for update",
			this::map, enrollmentId);
		RefundObligation obligation = all.stream().filter(item -> item.id().equals(obligationId)).findFirst()
			.orElseThrow(CourseRefundException::notFound);
		return new DecisionContext(obligation, all, amountPaid);
	}

	private void active(UUID managerId, boolean lock) {
		if (!lock) {
			if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
				throw new InvalidAuthenticatedAccountException();
			return;
		}
		Account account = accounts.findByIdForUpdate(managerId).orElseThrow(InvalidAuthenticatedAccountException::new);
		if (account.getRole() != AccountRole.MANAGER || account.getStatus() != AccountStatus.ACTIVE)
			throw new InvalidAuthenticatedAccountException();
	}

	private String reason(String value) {
		if (value == null || value.isBlank() || value.trim().length() > 1000)
			throw CourseRefundException.invalid("A non-blank reason of at most 1000 characters is required.");
		return value.trim();
	}
	private String status(String value) {
		if (value == null || value.isBlank()) return null;
		try { return CourseRefundStatus.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)).name(); }
		catch (IllegalArgumentException exception) { throw CourseRefundException.invalid("Unknown refund status."); }
	}
	private void version(RefundObligation obligation, Long expectedVersion) {
		if (expectedVersion == null || expectedVersion < 0) throw CourseRefundException.invalid("expectedVersion is required.");
		if (obligation.version() != expectedVersion) throw changed();
	}
	private CourseRefundException changed() {
		return CourseRefundException.conflict("COURSE_REFUND_CHANGED", "The refund obligation changed. Refresh and retry.");
	}
	private CourseRefundException alreadyDecided() {
		return CourseRefundException.conflict("COURSE_REFUND_ALREADY_DECIDED",
			"The refund obligation already has another decision.");
	}
	private RefundObligation find(UUID id) {
		return db.query(select() + " where id=?", this::map, id).stream().findFirst()
			.orElseThrow(CourseRefundException::notFound);
	}
	private String select() { return "select * from course_refund_obligations"; }
	private RefundObligation map(ResultSet rs, int row) throws SQLException {
		return new RefundObligation(rs.getObject("id", UUID.class), rs.getObject("course_enrollment_id", UUID.class),
			rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class),
			rs.getObject("course_order_id", UUID.class), rs.getObject("course_payment_id", UUID.class),
			rs.getString("source_type"), rs.getString("status"), amount(rs.getBigDecimal("amount_vnd")),
			amount(rs.getBigDecimal("approved_amount_vnd")), amount(rs.getBigDecimal("disbursed_amount_vnd")),
			amount(rs.getBigDecimal("verified_amount_vnd")), rs.getString("currency_code").trim(),
			rs.getObject("requester_account_id", UUID.class), rs.getString("request_reason"),
			rs.getObject("approved_by_account_id", UUID.class), rs.getString("approval_reason"),
			instant(rs.getTimestamp("approved_at")), rs.getObject("rejected_by_account_id", UUID.class),
			rs.getString("rejection_reason"), instant(rs.getTimestamp("rejected_at")),
			rs.getString("evidence_reference"), rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
	}
	private static BigInteger amount(BigDecimal value) { return value == null ? null : value.toBigIntegerExact(); }
	private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
	private record DecisionContext(RefundObligation obligation, List<RefundObligation> all, BigInteger amountPaid) { }
}
