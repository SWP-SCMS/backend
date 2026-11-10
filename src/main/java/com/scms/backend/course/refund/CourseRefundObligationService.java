package com.scms.backend.course.refund;

import static com.scms.backend.course.refund.CourseRefundApiModels.MemberRefundObligation;
import static com.scms.backend.course.refund.CourseRefundApiModels.MemberRefundObligationPage;
import static com.scms.backend.course.refund.CourseRefundApiModels.ReceptionCourseRefund;
import static com.scms.backend.course.refund.CourseRefundApiModels.ReceptionCourseRefundPage;
import static com.scms.backend.course.refund.CourseRefundApiModels.RefundObligation;
import static com.scms.backend.course.refund.CourseRefundApiModels.RefundObligationPage;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseRefundException;
import com.scms.backend.course.CourseRefundSource;
import com.scms.backend.course.CourseRefundStatus;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.notification.NotificationWriter;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseRefundObligationService {

	private static final Set<String> COMMITTED = Set.of("APPROVED", "DISBURSEMENT_RECORDED", "PENDING_VERIFICATION", "COMPLETED");

	private static final Set<String> IMMEDIATE_DISBURSEMENT_SOURCES = Set.of(CourseRefundSource.COURSE_RUN_CANCELLED.name(), CourseRefundSource.PAYMENT_EXCEPTION.name(), CourseRefundSource.TRANSFER_CREDIT_EXCESS.name());

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final Clock clock;

	CourseRefundObligationService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
		this.courseRefundObligationRepository = new CourseRefundObligationRepository(this.db);
	}

	@Transactional(readOnly = true)
	RefundObligationPage list(UUID managerId, int page, int size, String status) {
		active(managerId, AccountRole.MANAGER, false);
		if (page < 0 || size < 1 || size > 100)
			throw CourseRefundException.invalid("Invalid page or size.");
		String normalizedStatus = status(status);
		long total = courseRefundObligationRepository.listFindCourseRefundObligations(normalizedStatus, normalizedStatus);
		List<RefundObligation> items = courseRefundObligationRepository.listFindRows(normalizedStatus, normalizedStatus, size, (long) page * size);
		return new RefundObligationPage(items, page, size, total, (int) ((total + size - 1) / size));
	}

	@Transactional(readOnly = true)
	RefundObligation detail(UUID managerId, UUID obligationId) {
		active(managerId, AccountRole.MANAGER, false);
		return find(obligationId);
	}

	@Transactional(readOnly = true)
	public MemberRefundObligationPage memberHistoryForReceptionist(UUID actorId, UUID memberId, int page, int size) {
		active(actorId, AccountRole.RECEPTIONIST, false);
		return listForMember(memberId, page, size);
	}

	@Transactional(readOnly = true)
	public MemberRefundObligation memberDetailForReceptionist(UUID actorId, UUID memberId, UUID obligationId) {
		active(actorId, AccountRole.RECEPTIONIST, false);
		return detailForMember(memberId, obligationId);
	}

	@Transactional(readOnly = true)
	MemberRefundObligationPage listForMember(UUID memberId, int page, int size) {
		active(memberId, AccountRole.MEMBER, false);
		page(page, size);
		long total = courseRefundObligationRepository.listForMemberFindCourseRefundObligations(memberId);
		List<MemberRefundObligation> items = courseRefundObligationRepository.listForMemberFindRows(memberId, size, (long) page * size).stream().map(this::member).toList();
		return new MemberRefundObligationPage(items, page, size, total, (int) ((total + size - 1) / size));
	}

	@Transactional(readOnly = true)
	MemberRefundObligation detailForMember(UUID memberId, UUID obligationId) {
		active(memberId, AccountRole.MEMBER, false);
		return courseRefundObligationRepository.detailForMemberFindRows(obligationId, memberId).stream().findFirst().map(this::member).orElseThrow(CourseRefundException::notFound);
	}

	@Transactional(readOnly = true)
	ReceptionCourseRefundPage listForReceptionist(UUID receptionistId, int page, int size, Boolean eligible) {
		active(receptionistId, AccountRole.RECEPTIONIST, false);
		page(page, size);
		return receptionRefundPage(page, size, eligible);
	}

	@Transactional(readOnly = true)
	ReceptionCourseRefund detailForReceptionist(UUID receptionistId, UUID refundId) {
		active(receptionistId, AccountRole.RECEPTIONIST, false);
		return courseRefundObligationRepository.detailForReceptionistFindRows(refundId).stream().findFirst().orElseThrow(CourseRefundException::notFound);
	}

	@Transactional
	public RefundObligation approve(UUID managerId, UUID obligationId, Long expectedVersion, String reason) {
		String normalizedReason = reason(reason);
		DecisionContext context = lockForDecision(managerId, obligationId);
		RefundObligation obligation = context.obligation();
		if ("APPROVED".equals(obligation.status()) && managerId.equals(obligation.approvedByAccountId()) && normalizedReason.equals(obligation.approvalReason()))
			return obligation;
		if (!CourseRefundStateMachine.canTransition(CourseRefundStatus.valueOf(obligation.status()), CourseRefundStatus.APPROVED))
			throw alreadyDecided();
		version(obligation, expectedVersion);
		BigInteger committed = context.all().stream().filter(item -> !item.id().equals(obligationId)).filter(item -> COMMITTED.contains(item.status())).map(RefundObligation::approvedAmountVnd).filter(java.util.Objects::nonNull).reduce(BigInteger.ZERO, BigInteger::add);
		BigInteger available = context.amountPaid().subtract(committed).max(BigInteger.ZERO);
		if (obligation.grossObligationVnd().compareTo(available) > 0) {
			throw CourseRefundException.conflict("COURSE_REFUND_AMOUNT_EXCEEDS_AVAILABLE", "The refund would exceed the Enrollment paid amount after prior committed refunds.");
		}
		Instant now = clock.instant();
		int changed = courseRefundObligationRepository.approveUpdateCourseRefundObligations(managerId, normalizedReason, Timestamp.from(now), Timestamp.from(now), obligationId, expectedVersion);
		if (changed != 1)
			throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_APPROVED", "COURSE_REFUND_OBLIGATION", obligationId, normalizedReason, Map.of("status", "PENDING_REVIEW"), Map.of("status", "APPROVED", "amountVnd", obligation.grossObligationVnd().toString(), "source", obligation.source())));
		notifications.write("course-refund-approved:" + obligationId, obligation.memberAccountId(), "COURSE_REFUND_APPROVED", "COURSE_REFUND_OBLIGATION", obligationId, Map.of("amountVnd", obligation.grossObligationVnd().toString(), "source", obligation.source()));
		return find(obligationId);
	}

	@Transactional
	RefundObligation reject(UUID managerId, UUID obligationId, Long expectedVersion, String reason) {
		String normalizedReason = reason(reason);
		DecisionContext context = lockForDecision(managerId, obligationId);
		RefundObligation obligation = context.obligation();
		if ("REJECTED".equals(obligation.status()) && managerId.equals(obligation.rejectedByAccountId()) && normalizedReason.equals(obligation.rejectionReason()))
			return obligation;
		if (!CourseRefundStateMachine.canTransition(CourseRefundStatus.valueOf(obligation.status()), CourseRefundStatus.REJECTED))
			throw alreadyDecided();
		version(obligation, expectedVersion);
		Instant now = clock.instant();
		int changed = courseRefundObligationRepository.rejectUpdateCourseRefundObligations(managerId, normalizedReason, Timestamp.from(now), Timestamp.from(now), obligationId, expectedVersion);
		if (changed != 1)
			throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_REJECTED", "COURSE_REFUND_OBLIGATION", obligationId, normalizedReason, Map.of("status", "PENDING_REVIEW"), Map.of("status", "REJECTED", "source", obligation.source())));
		notifications.write("course-refund-rejected:" + obligationId, obligation.memberAccountId(), "COURSE_REFUND_REJECTED", "COURSE_REFUND_OBLIGATION", obligationId, Map.of("source", obligation.source()));
		return find(obligationId);
	}

	@Transactional
	public RefundObligation recordDisbursement(UUID receptionistId, UUID obligationId, String idempotencyKey, CourseRefundApiModels.RefundDisbursement request) {
		String key = key(idempotencyKey);
		DisbursementInput input = disbursement(request);
		DecisionContext context = lock(receptionistId, AccountRole.RECEPTIONIST, obligationId);
		RefundObligation obligation = context.obligation();
		if (key.equals(context.disbursementKey())) {
			if (receptionistId.equals(obligation.disbursedByAccountId()) && matches(obligation, input))
				return obligation;
			throw idempotencyConflict();
		}
		UUID reused = courseRefundObligationRepository.recordDisbursementFindCourseRefundObligations(receptionistId, key).stream().findFirst().orElse(null);
		if (reused != null)
			throw idempotencyConflict();
		if (!"APPROVED".equals(obligation.status())) {
			if (obligation.disbursedAt() != null)
				throw CourseRefundException.conflict("COURSE_REFUND_ALREADY_DISBURSED", "A disbursement was already recorded for this refund obligation.");
			throw transition();
		}
		if (!IMMEDIATE_DISBURSEMENT_SOURCES.contains(obligation.source()) && !CourseRunStatus.COMPLETED.name().equals(context.courseRunStatus()) && !(CourseRefundSource.REDUCED_SESSION_COUNT.name().equals(obligation.source()) && CourseRunStatus.CANCELLED.name().equals(context.courseRunStatus()))) {
			throw CourseRefundException.conflict("COURSE_REFUND_DISBURSEMENT_NOT_ELIGIBLE", "The Course Run must be completed before this refund can be disbursed.");
		}
		version(obligation, input.expectedVersion());
		if (!input.amount().equals(obligation.approvedAmountVnd())) {
			throw CourseRefundException.conflict("COURSE_REFUND_AMOUNT_MISMATCH", "The disbursement amount must equal the approved refund amount.");
		}
		Instant now = clock.instant();
		int changed = courseRefundObligationRepository.recordDisbursementUpdateCourseRefundObligations(input.amount(), receptionistId, Timestamp.from(now), input.method(), input.reference(), input.evidence(), key, Timestamp.from(now), obligationId, input.expectedVersion());
		if (changed != 1)
			throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), receptionistId, "COURSE_REFUND_DISBURSED", "COURSE_REFUND_OBLIGATION", obligationId, null, Map.of("status", "APPROVED"), Map.of("status", "DISBURSEMENT_RECORDED", "amountVnd", input.amount().toString(), "method", input.method())));
		notifyMember(obligation, "COURSE_REFUND_DISBURSED", Map.of("amountVnd", input.amount().toString(), "method", input.method()));
		for (Account manager : accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			notifications.write("course-refund-verification-ready:" + obligationId + ":" + manager.getId(), manager.getId(), "COURSE_REFUND_VERIFICATION_READY", "COURSE_REFUND_OBLIGATION", obligationId, Map.of("amountVnd", input.amount().toString(), "source", obligation.source()));
		}
		return find(obligationId);
	}

	@Transactional
	public RefundObligation verify(UUID managerId, UUID obligationId, CourseRefundApiModels.RefundVerification request) {
		VerificationInput input = verification(request);
		DecisionContext context = lock(managerId, AccountRole.MANAGER, obligationId);
		RefundObligation obligation = context.obligation();
		if ("COMPLETED".equals(obligation.status())) {
			if (managerId.equals(obligation.verifiedByAccountId()) && input.amount().equals(obligation.verifiedAmountVnd()) && input.reference().equals(obligation.disbursementReference()) && input.evidence().equals(obligation.evidenceReference()) && input.reason().equals(obligation.verificationReason()))
				return obligation;
			throw CourseRefundException.conflict("COURSE_REFUND_EVIDENCE_MISMATCH", "The verification replay does not match the completed verification.");
		}
		if (!Set.of("DISBURSEMENT_RECORDED", "PENDING_VERIFICATION").contains(obligation.status()))
			throw transition();
		version(obligation, input.expectedVersion());
		if (!input.amount().equals(obligation.approvedAmountVnd()) || !input.amount().equals(obligation.disbursedAmountVnd()) || !input.reference().equals(obligation.disbursementReference()) || !input.evidence().equals(obligation.evidenceReference())) {
			throw CourseRefundException.conflict("COURSE_REFUND_EVIDENCE_MISMATCH", "The verification evidence does not match the recorded disbursement.");
		}
		Instant now = clock.instant();
		long version = input.expectedVersion();
		if ("DISBURSEMENT_RECORDED".equals(obligation.status())) {
			int queued = courseRefundObligationRepository.verifyUpdateCourseRefundObligations(Timestamp.from(now), obligationId, version);
			if (queued != 1)
				throw changed();
			version++;
		}
		int changed = courseRefundObligationRepository.completeVerifiedRefund(input.amount(), managerId, input.reason(), Timestamp.from(now), Timestamp.from(now), obligationId, version);
		if (changed != 1)
			throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_VERIFIED", "COURSE_REFUND_OBLIGATION", obligationId, input.reason(), Map.of("status", obligation.status()), Map.of("status", "COMPLETED", "amountVnd", input.amount().toString())));
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

	private RefundObligation terminal(UUID managerId, UUID obligationId, CourseRefundApiModels.RefundTerminal request, CourseRefundStatus target) {
		Long expectedVersion = request == null ? null : request.expectedVersion();
		String normalizedReason = reason(request == null ? null : request.reason());
		DecisionContext context = lock(managerId, AccountRole.MANAGER, obligationId);
		RefundObligation obligation = context.obligation();
		if (target.name().equals(obligation.status()) && managerId.equals(obligation.terminalByAccountId()) && normalizedReason.equals(obligation.terminalReason()))
			return obligation;
		if (!CourseRefundStateMachine.canTransition(CourseRefundStatus.valueOf(obligation.status()), target))
			throw transition();
		version(obligation, expectedVersion);
		Instant now = clock.instant();
		int changed = courseRefundObligationRepository.terminalUpdateCourseRefundObligations(target.name(), managerId, normalizedReason, Timestamp.from(now), Timestamp.from(now), obligationId, obligation.status(), expectedVersion);
		if (changed != 1)
			throw changed();
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_REFUND_" + target.name(), "COURSE_REFUND_OBLIGATION", obligationId, normalizedReason, Map.of("status", obligation.status()), Map.of("status", target.name())));
		notifyMember(obligation, "COURSE_REFUND_" + target.name(), Map.of("status", target.name()));
		return find(obligationId);
	}

	private DecisionContext lockForDecision(UUID managerId, UUID obligationId) {
		return lock(managerId, AccountRole.MANAGER, obligationId);
	}

	private DecisionContext lock(UUID actorId, AccountRole role, UUID obligationId) {
		active(actorId, role, true);
		Map<String, Object> visible = courseRefundObligationRepository.lockFindCourseRefundObligations(obligationId).stream().findFirst().orElseThrow(CourseRefundException::notFound);
		UUID runId = (UUID) visible.get("course_run_id");
		UUID enrollmentId = (UUID) visible.get("course_enrollment_id");
		String courseRunStatus = courseRefundObligationRepository.lockFindCourseRuns(runId);
		BigInteger amountPaid = courseRefundObligationRepository.lockFindCourseEnrollments(enrollmentId, runId).toBigIntegerExact();
		List<RefundObligation> all = courseRefundObligationRepository.lockFindOf(enrollmentId);
		RefundObligation obligation = all.stream().filter(item -> item.id().equals(obligationId)).findFirst().orElseThrow(CourseRefundException::notFound);
		String disbursementKey = courseRefundObligationRepository.findDisbursementIdempotencyKey(obligationId);
		return new DecisionContext(obligation, all, amountPaid, disbursementKey, courseRunStatus);
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
		return CourseRefundValidation.reason(value);
	}

	private String status(String value) {
		return CourseRefundValidation.status(value);
	}

	private void page(int page, int size) {
		CourseRefundValidation.page(page, size);
	}

	private String key(String value) {
		return CourseRefundValidation.key(value);
	}

	private DisbursementInput disbursement(CourseRefundApiModels.RefundDisbursement request) {
		return CourseRefundValidation.disbursement(request);
	}

	private VerificationInput verification(CourseRefundApiModels.RefundVerification request) {
		return CourseRefundValidation.verification(request);
	}

	private boolean matches(RefundObligation obligation, DisbursementInput input) {
		return CourseRefundValidation.matches(obligation, input);
	}

	private CourseRefundException idempotencyConflict() {
		return CourseRefundValidation.idempotencyConflict();
	}

	private CourseRefundException transition() {
		return CourseRefundValidation.transition();
	}

	private void version(RefundObligation obligation, Long expectedVersion) {
		CourseRefundValidation.version(obligation, expectedVersion);
	}

	private CourseRefundException changed() {
		return CourseRefundValidation.changed();
	}

	private CourseRefundException alreadyDecided() {
		return CourseRefundValidation.alreadyDecided();
	}

	private RefundObligation find(UUID id) {
		return courseRefundObligationRepository.find(id);
	}

	private RefundObligation map(ResultSet rs, int row) throws SQLException {
		return courseRefundObligationRepository.map(rs, row);
	}

	private MemberRefundObligation member(RefundObligation value) {
		return new MemberRefundObligation(value.id(), value.enrollmentId(), value.courseRunId(), value.source(), value.status(), value.grossObligationVnd(), value.approvedAmountVnd(), value.disbursedAmountVnd(), value.verifiedAmountVnd(), value.currencyCode(), value.createdAt(), value.updatedAt(), value.version());
	}

	private void notifyMember(RefundObligation obligation, String type, Map<String, Object> payload) {
		notifications.write(type.toLowerCase(java.util.Locale.ROOT).replace('_', '-') + ":" + obligation.id(), obligation.memberAccountId(), type, "COURSE_REFUND_OBLIGATION", obligation.id(), payload);
	}

	private static BigInteger amount(BigDecimal value) {
		return CourseRefundObligationRepository.amount(value);
	}

	private static Instant instant(Timestamp value) {
		return CourseRefundObligationRepository.instant(value);
	}

	private record DecisionContext(RefundObligation obligation, List<RefundObligation> all, BigInteger amountPaid, String disbursementKey, String courseRunStatus) {
	}

	record DisbursementInput(long expectedVersion, BigInteger amount, String method, String reference, String evidence) {
	}

	record VerificationInput(long expectedVersion, BigInteger amount, String reference, String evidence, String reason) {
	}

	private final CourseRefundObligationRepository courseRefundObligationRepository;

	private ReceptionCourseRefundPage receptionRefundPage(int page, int size, Boolean eligible) {
		return courseRefundObligationRepository.receptionRefundPage(page, size, eligible);
	}
}
