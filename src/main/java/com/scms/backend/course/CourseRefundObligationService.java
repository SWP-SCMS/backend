package com.scms.backend.course;

import static com.scms.backend.course.CourseRefundApiModels.RefundObligation;
import static com.scms.backend.course.CourseRefundApiModels.RefundObligationPage;
import static com.scms.backend.course.CourseRefundApiModels.MemberRefundObligation;
import static com.scms.backend.course.CourseRefundApiModels.MemberRefundObligationPage;

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
		active(managerId, AccountRole.MANAGER, false);
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
		active(managerId, AccountRole.MANAGER, false);
		return find(obligationId);
	}

	@Transactional(readOnly = true)
	MemberRefundObligationPage listForMember(UUID memberId, int page, int size) {
		active(memberId, AccountRole.MEMBER, false);
		page(page, size);
		long total = db.queryForObject("select count(*) from course_refund_obligations where member_account_id=?",
			Long.class, memberId);
		List<MemberRefundObligation> items = db.query(select() + " where member_account_id=? "
			+ "order by created_at desc,id desc limit ? offset ?", this::map, memberId, size, (long) page * size)
			.stream().map(this::member).toList();
		return new MemberRefundObligationPage(items, page, size, total, (int) ((total + size - 1) / size));
	}

	@Transactional(readOnly = true)
	MemberRefundObligation detailForMember(UUID memberId, UUID obligationId) {
		active(memberId, AccountRole.MEMBER, false);
		return db.query(select() + " where id=? and member_account_id=?", this::map, obligationId, memberId)
			.stream().findFirst().map(this::member).orElseThrow(CourseRefundException::notFound);
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

	@Transactional
	RefundObligation recordDisbursement(UUID receptionistId, UUID obligationId, String idempotencyKey,
			CourseRefundApiModels.RefundDisbursement request) {
		String key = key(idempotencyKey);
		DisbursementInput input = disbursement(request);
		DecisionContext context = lock(receptionistId, AccountRole.RECEPTIONIST, obligationId);
		RefundObligation obligation = context.obligation();
		if (key.equals(context.disbursementKey())) {
			if (receptionistId.equals(obligation.disbursedByAccountId()) && matches(obligation, input)) return obligation;
			throw idempotencyConflict();
		}
		UUID reused = db.query("select id from course_refund_obligations where disbursed_by_account_id=? "
			+ "and disbursement_idempotency_key=?", (rs, row) -> rs.getObject(1, UUID.class), receptionistId, key)
			.stream().findFirst().orElse(null);
		if (reused != null) throw idempotencyConflict();
		if (!"APPROVED".equals(obligation.status())) {
			if (obligation.disbursedAt() != null) throw CourseRefundException.conflict("COURSE_REFUND_ALREADY_DISBURSED",
				"A disbursement was already recorded for this refund obligation.");
			throw transition();
		}
		version(obligation, input.expectedVersion());
		if (!input.amount().equals(obligation.approvedAmountVnd())) {
			throw CourseRefundException.conflict("COURSE_REFUND_AMOUNT_MISMATCH",
				"The disbursement amount must equal the approved refund amount.");
		}
		Instant now = clock.instant();
		int changed = db.update("""
			update course_refund_obligations set status='DISBURSEMENT_RECORDED',disbursed_amount_vnd=?,
			 disbursed_by_account_id=?,disbursed_at=?,disbursement_method=?,disbursement_reference=?,
			 evidence_reference=?,disbursement_idempotency_key=?,version=version+1,updated_at=?
			where id=? and status='APPROVED' and version=?
			""", input.amount(), receptionistId, Timestamp.from(now), input.method(), input.reference(),
			input.evidence(), key, Timestamp.from(now), obligationId, input.expectedVersion());
		if (changed != 1) throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), receptionistId, "COURSE_REFUND_DISBURSED",
			"COURSE_REFUND_OBLIGATION", obligationId, null, Map.of("status", "APPROVED"),
			Map.of("status", "DISBURSEMENT_RECORDED", "amountVnd", input.amount().toString(),
				"method", input.method())));
		notifyMember(obligation, "COURSE_REFUND_DISBURSED", Map.of("amountVnd", input.amount().toString(),
			"method", input.method()));
		for (Account manager : accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			notifications.write("course-refund-verification-ready:" + obligationId + ":" + manager.getId(), manager.getId(),
				"COURSE_REFUND_VERIFICATION_READY", "COURSE_REFUND_OBLIGATION", obligationId,
				Map.of("amountVnd", input.amount().toString(), "source", obligation.source()));
		}
		return find(obligationId);
	}

	@Transactional
	RefundObligation verify(UUID managerId, UUID obligationId, CourseRefundApiModels.RefundVerification request) {
		VerificationInput input = verification(request);
		DecisionContext context = lock(managerId, AccountRole.MANAGER, obligationId);
		RefundObligation obligation = context.obligation();
		if ("COMPLETED".equals(obligation.status())) {
			if (managerId.equals(obligation.verifiedByAccountId())
					&& input.amount().equals(obligation.verifiedAmountVnd())
					&& input.reference().equals(obligation.disbursementReference())
					&& input.evidence().equals(obligation.evidenceReference())
					&& input.reason().equals(obligation.verificationReason())) return obligation;
			throw CourseRefundException.conflict("COURSE_REFUND_EVIDENCE_MISMATCH",
				"The verification replay does not match the completed verification.");
		}
		if (!Set.of("DISBURSEMENT_RECORDED", "PENDING_VERIFICATION").contains(obligation.status())) throw transition();
		version(obligation, input.expectedVersion());
		if (!input.amount().equals(obligation.approvedAmountVnd())
				|| !input.amount().equals(obligation.disbursedAmountVnd())
				|| !input.reference().equals(obligation.disbursementReference())
				|| !input.evidence().equals(obligation.evidenceReference())) {
			throw CourseRefundException.conflict("COURSE_REFUND_EVIDENCE_MISMATCH",
				"The verification evidence does not match the recorded disbursement.");
		}
		Instant now = clock.instant();
		long version = input.expectedVersion();
		if ("DISBURSEMENT_RECORDED".equals(obligation.status())) {
			int queued = db.update("update course_refund_obligations set status='PENDING_VERIFICATION',"
				+ "version=version+1,updated_at=? where id=? and status='DISBURSEMENT_RECORDED' and version=?",
				Timestamp.from(now), obligationId, version);
			if (queued != 1) throw changed();
			version++;
		}
		int changed = db.update("""
			update course_refund_obligations set status='COMPLETED',verified_amount_vnd=?,
			 verified_by_account_id=?,verification_reason=?,verified_at=?,version=version+1,updated_at=?
			where id=? and status='PENDING_VERIFICATION' and version=?
			""", input.amount(), managerId, input.reason(), Timestamp.from(now), Timestamp.from(now),
			obligationId, version);
		if (changed != 1) throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_VERIFIED",
			"COURSE_REFUND_OBLIGATION", obligationId, input.reason(),
			Map.of("status", obligation.status()), Map.of("status", "COMPLETED", "amountVnd", input.amount().toString())));
		notifyMember(obligation, "COURSE_REFUND_COMPLETED", Map.of("amountVnd", input.amount().toString()));
		return find(obligationId);
	}

	@Transactional
	RefundObligation fail(UUID managerId, UUID obligationId, CourseRefundApiModels.RefundTerminal request) {
		return terminal(managerId, obligationId, request, CourseRefundStatus.FAILED);
	}

	@Transactional
	RefundObligation returned(UUID managerId, UUID obligationId, CourseRefundApiModels.RefundTerminal request) {
		return terminal(managerId, obligationId, request, CourseRefundStatus.RETURNED);
	}

	private RefundObligation terminal(UUID managerId, UUID obligationId, CourseRefundApiModels.RefundTerminal request,
			CourseRefundStatus target) {
		Long expectedVersion = request == null ? null : request.expectedVersion();
		String normalizedReason = reason(request == null ? null : request.reason());
		DecisionContext context = lock(managerId, AccountRole.MANAGER, obligationId);
		RefundObligation obligation = context.obligation();
		if (target.name().equals(obligation.status()) && managerId.equals(obligation.terminalByAccountId())
				&& normalizedReason.equals(obligation.terminalReason())) return obligation;
		if (!CourseRefundStateMachine.canTransition(CourseRefundStatus.valueOf(obligation.status()), target)) throw transition();
		version(obligation, expectedVersion);
		Instant now = clock.instant();
		int changed = db.update("update course_refund_obligations set status=?,terminal_by_account_id=?,"
			+ "terminal_reason=?,terminal_at=?,version=version+1,updated_at=? where id=? and status=? and version=?",
			target.name(), managerId, normalizedReason, Timestamp.from(now), Timestamp.from(now), obligationId,
			obligation.status(), expectedVersion);
		if (changed != 1) throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_" + target.name(),
			"COURSE_REFUND_OBLIGATION", obligationId, normalizedReason, Map.of("status", obligation.status()),
			Map.of("status", target.name())));
		notifyMember(obligation, "COURSE_REFUND_" + target.name(), Map.of("status", target.name()));
		return find(obligationId);
	}

	private DecisionContext lockForDecision(UUID managerId, UUID obligationId) {
		return lock(managerId, AccountRole.MANAGER, obligationId);
	}

	private DecisionContext lock(UUID actorId, AccountRole role, UUID obligationId) {
		active(actorId, role, true);
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
		String disbursementKey = db.queryForObject("select disbursement_idempotency_key "
			+ "from course_refund_obligations where id=?", String.class, obligationId);
		return new DecisionContext(obligation, all, amountPaid, disbursementKey);
	}

	private void active(UUID accountId, AccountRole role, boolean lock) {
		if (!lock) {
			if (!accounts.existsByIdAndRoleAndStatus(accountId, role, AccountStatus.ACTIVE))
				throw new InvalidAuthenticatedAccountException();
			return;
		}
		Account account = accounts.findByIdForUpdate(accountId).orElseThrow(InvalidAuthenticatedAccountException::new);
		if (account.getRole() != role || account.getStatus() != AccountStatus.ACTIVE)
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
	private void page(int page, int size) {
		if (page < 0 || size < 1 || size > 100) throw CourseRefundException.invalid("Invalid page or size.");
	}
	private String key(String value) {
		if (value == null || value.isBlank() || value.trim().length() > 100)
			throw CourseRefundException.invalid("Idempotency-Key is required and must be at most 100 characters.");
		return value.trim();
	}
	private DisbursementInput disbursement(CourseRefundApiModels.RefundDisbursement request) {
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0
				|| !vnd(request.amountVnd()) || blank(request.method(), 32) || blank(request.reference(), 200)
				|| blank(request.evidenceReference(), 500)) {
			throw CourseRefundException.invalid("Valid version, VND amount, method, reference, and evidence are required.");
		}
		String method = request.method().trim().toUpperCase(java.util.Locale.ROOT);
		if (!Set.of("BANK_TRANSFER", "CASH").contains(method))
			throw CourseRefundException.invalid("Disbursement method must be BANK_TRANSFER or CASH.");
		return new DisbursementInput(request.expectedVersion(), request.amountVnd(), method,
			request.reference().trim(), request.evidenceReference().trim());
	}
	private VerificationInput verification(CourseRefundApiModels.RefundVerification request) {
		if (request == null || request.expectedVersion() == null || request.expectedVersion() < 0
				|| !vnd(request.amountVnd()) || blank(request.disbursementReference(), 200)
				|| blank(request.evidenceReference(), 500)) {
			throw CourseRefundException.invalid("Valid version, amount, disbursement reference, and evidence are required.");
		}
		return new VerificationInput(request.expectedVersion(), request.amountVnd(),
			request.disbursementReference().trim(), request.evidenceReference().trim(), reason(request.reason()));
	}
	private boolean vnd(BigInteger value) { return value != null && value.signum() >= 0 && value.toString().length() <= 19; }
	private boolean blank(String value, int maximum) {
		return value == null || value.isBlank() || value.trim().length() > maximum;
	}
	private boolean matches(RefundObligation obligation, DisbursementInput input) {
		return input.amount().equals(obligation.disbursedAmountVnd())
			&& input.method().equals(obligation.disbursementMethod())
			&& input.reference().equals(obligation.disbursementReference())
			&& input.evidence().equals(obligation.evidenceReference());
	}
	private CourseRefundException idempotencyConflict() {
		return CourseRefundException.conflict("COURSE_REFUND_IDEMPOTENCY_CONFLICT",
			"Idempotency-Key was already used with another disbursement request.");
	}
	private CourseRefundException transition() {
		return CourseRefundException.conflict("COURSE_REFUND_INVALID_TRANSITION",
			"The refund obligation is not in a valid state for this action.");
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
			rs.getObject("disbursed_by_account_id", UUID.class), instant(rs.getTimestamp("disbursed_at")),
			rs.getString("disbursement_method"), rs.getString("disbursement_reference"),
			rs.getString("evidence_reference"), rs.getObject("verified_by_account_id", UUID.class),
			rs.getString("verification_reason"), instant(rs.getTimestamp("verified_at")),
			rs.getObject("terminal_by_account_id", UUID.class), rs.getString("terminal_reason"),
			instant(rs.getTimestamp("terminal_at")), rs.getTimestamp("created_at").toInstant(),
			rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
	}
	private MemberRefundObligation member(RefundObligation value) {
		return new MemberRefundObligation(value.id(), value.enrollmentId(), value.courseRunId(), value.source(),
			value.status(), value.grossObligationVnd(), value.approvedAmountVnd(), value.disbursedAmountVnd(),
			value.verifiedAmountVnd(), value.currencyCode(), value.createdAt(), value.updatedAt(), value.version());
	}
	private void notifyMember(RefundObligation obligation, String type, Map<String, Object> payload) {
		notifications.write(type.toLowerCase(java.util.Locale.ROOT).replace('_', '-') + ":" + obligation.id(),
			obligation.memberAccountId(), type, "COURSE_REFUND_OBLIGATION", obligation.id(), payload);
	}
	private static BigInteger amount(BigDecimal value) { return value == null ? null : value.toBigIntegerExact(); }
	private static Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }
	private record DecisionContext(RefundObligation obligation, List<RefundObligation> all, BigInteger amountPaid,
		String disbursementKey) { }
	private record DisbursementInput(long expectedVersion, BigInteger amount, String method, String reference,
		String evidence) { }
	private record VerificationInput(long expectedVersion, BigInteger amount, String reference, String evidence,
		String reason) { }
}
