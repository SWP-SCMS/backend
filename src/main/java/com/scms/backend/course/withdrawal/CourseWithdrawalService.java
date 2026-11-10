package com.scms.backend.course.withdrawal;

import static com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.CourseWithdrawalException;
import com.scms.backend.notification.NotificationWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CourseWithdrawalService {

	private static final Logger log = LoggerFactory.getLogger(CourseWithdrawalService.class);

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final CourseWithdrawalWaitlistPromotion promotions;

	private final Clock clock;

	CourseWithdrawalService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, NotificationWriter notifications, CourseWithdrawalWaitlistPromotion promotions, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.promotions = promotions;
		this.clock = clock;
		this.courseWithdrawalRepository = new CourseWithdrawalRepository(this.db);
		this.courseWithdrawalSnapshot = new CourseWithdrawalSnapshot(this.db);
	}

	@Transactional
	public WithdrawalRequest createForMember(UUID memberId, UUID enrollmentId, String reason) {
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
	public WithdrawalRequest createForScheduleChange(UUID actorId, UUID memberId, UUID responseId) {
		AccountRole actorRole = actorId.equals(memberId) ? AccountRole.MEMBER : AccountRole.RECEPTIONIST;
		active(actorId, actorRole, true);
		if (!actorId.equals(memberId))
			active(memberId, AccountRole.MEMBER, true);
		List<Map<String, Object>> response = courseWithdrawalRepository.createForScheduleChangeFindCourseScheduleChangeResponses(responseId, memberId);
		if (response.isEmpty())
			throw CourseException.responseNotFound();
		UUID existing = courseWithdrawalRepository.createForScheduleChangeFindCourseWithdrawalRequests(responseId).stream().map(row -> uuid(row, "id")).findFirst().orElse(null);
		if (existing != null)
			return find(existing);
		Map<String, Object> change = response.getFirst();
		if (!"REQUEST_WITHDRAWAL".equals(text(change, "response")) || !"RESPONDED".equals(text(change, "status")))
			throw CourseException.responseConflict();
		UUID enrollmentId = uuid(change, "course_enrollment_id"), runId = uuid(change, "course_run_id");
		CourseWithdrawalCause cause = CourseWithdrawalCause.valueOf(text(change, "cause_code"));
		requireWithdrawableRun(lockRun(runId));
		Map<String, Object> enrollment = lockEnrollment(enrollmentId, memberId);
		if (!"ACTIVE".equals(text(enrollment, "lifecycle_status"))) {
			if ("WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status"))) {
				UUID pending = courseWithdrawalRepository.findPendingWithdrawalForEnrollment(enrollmentId).stream().map(row -> uuid(row, "id")).findFirst().orElse(null);
				if (pending != null)
					return find(pending);
			}
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ENROLLMENT_INACTIVE", "Only an active Enrollment can be withdrawn.");
		}
		if (Boolean.TRUE.equals(courseWithdrawalRepository.createForScheduleChangeFindCourseTransferRequests(enrollmentId)))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_TRANSFER_OPEN", "The Enrollment already has an open transfer request.");
		Instant now = clock.instant();
		RefundSnapshot snapshot = refundSnapshot(enrollmentId, number(enrollment, "total_units_snapshot"), amount(enrollment, "amount_paid_vnd_snapshot"), now);
		CourseWithdrawalPolicy.Decision decision = CourseWithdrawalPolicy.evaluate(cause, snapshot.remainingUnits(), snapshot.totalUnits(), snapshot.unusedAllocation());
		if (!decision.eligible())
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED", "Withdrawal is no longer allowed after the final unit has started.");
		return insertRequest(actorId, actorRole, memberId, enrollmentId, runId, "Withdrawal requested after a material schedule change", cause, responseId, uuid(change, "change_event_id"), snapshot, now);
	}

	@Transactional(readOnly = true)
	public List<WithdrawalRequest> listForReceptionist(UUID actorId, UUID memberId) {
		active(actorId, AccountRole.RECEPTIONIST, false);
		return listForMember(memberId);
	}

	@Transactional(readOnly = true)
	public WithdrawalRequest detailForReceptionist(UUID actorId, UUID memberId, UUID requestId) {
		active(actorId, AccountRole.RECEPTIONIST, false);
		return detailForMember(memberId, requestId);
	}

	@Transactional(readOnly = true)
	List<WithdrawalRequest> listForMember(UUID memberId) {
		active(memberId, AccountRole.MEMBER, false);
		return courseWithdrawalRepository.listForMemberFindRows(memberId);
	}

	@Transactional(readOnly = true)
	public WithdrawalRequest detailForMember(UUID memberId, UUID requestId) {
		active(memberId, AccountRole.MEMBER, false);
		return courseWithdrawalRepository.detailForMemberFindRows(requestId, memberId).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	@Transactional(readOnly = true)
	List<WithdrawalRequest> managerQueue(UUID managerId) {
		active(managerId, AccountRole.MANAGER, false);
		return courseWithdrawalRepository.managerQueueFindRows();
	}

	@Transactional(readOnly = true)
	WithdrawalRequest managerDetail(UUID managerId, UUID requestId) {
		active(managerId, AccountRole.MANAGER, false);
		return find(requestId);
	}

	@Transactional
	public WithdrawalRequest approve(UUID managerId, UUID requestId, String reason) {
		active(managerId, AccountRole.MANAGER, true);
		String normalizedReason = reason(reason);
		WithdrawalRequest request = lockForDecision(requestId, null);
		if (!"PENDING".equals(request.status()))
			throw invalidTransition();
		if ("CANCELLED".equals(request.courseRunStatus()) || request.snapshotCapturedAt() == null)
			throw invalidTransition();
		Map<String, Object> enrollment = lockEnrollment(request.enrollmentId(), request.memberAccountId());
		if (!"WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status")))
			throw invalidTransition();
		Instant now = clock.instant();
		BigInteger refund = request.refundAmountVnd();
		courseWithdrawalRepository.approveUpdateCourseEnrollments(request.enrollmentId());
		courseWithdrawalRepository.approveUpdateCourseReservations(Timestamp.from(now), request.enrollmentId());
		if (courseWithdrawalRepository.approveUpdateCourseWithdrawalRequests(normalizedReason, managerId, Timestamp.from(now), requestId) != 1)
			throw invalidTransition();
		courseWithdrawalRepository.approveInsertCourseRefundObligations(UUID.randomUUID(), request.enrollmentId(), request.courseRunId(), request.cause() == CourseWithdrawalCause.VOLUNTARY ? "MEMBER_WITHDRAWAL" : "MATERIAL_CHANGE", refund, request.requesterAccountId(), request.requestReason());
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_WITHDRAWAL_APPROVED", "COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"), Map.of("status", "APPROVED", "remainingUnits", request.remainingUnits(), "totalUnits", request.totalUnits(), "refundBasisVnd", request.refundBasisVnd().toString(), "refundAmountVnd", refund.toString())));
		notifications.write("course-withdrawal-approved:" + requestId, request.memberAccountId(), "COURSE_WITHDRAWAL_APPROVED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("refundAmountVnd", refund.toString()));
		promoteAfterCommit(request.courseRunId(), now);
		return find(requestId);
	}

	@Transactional
	WithdrawalRequest reject(UUID managerId, UUID requestId, String reason) {
		active(managerId, AccountRole.MANAGER, true);
		String normalizedReason = reason(reason);
		WithdrawalRequest request = lockForDecision(requestId, null);
		if (!"PENDING".equals(request.status()))
			throw invalidTransition();
		Map<String, Object> enrollment = lockEnrollment(request.enrollmentId(), request.memberAccountId());
		if (!"WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status")))
			throw invalidTransition();
		Instant now = clock.instant();
		courseWithdrawalRepository.rejectUpdateCourseEnrollments(request.enrollmentId());
		courseWithdrawalRepository.rejectUpdateCourseWithdrawalRequests(normalizedReason, managerId, Timestamp.from(now), requestId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "COURSE_WITHDRAWAL_REJECTED", "COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"), Map.of("status", "REJECTED")));
		notifications.write("course-withdrawal-rejected:" + requestId, request.memberAccountId(), "COURSE_WITHDRAWAL_REJECTED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("reason", normalizedReason));
		return find(requestId);
	}

	@Transactional
	public WithdrawalRequest cancelForMember(UUID memberId, UUID requestId, String reason) {
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
		if ("CANCELLED".equals(request.status()))
			return request;
		if (!"PENDING".equals(request.status()))
			throw invalidTransition();
		lockEnrollment(request.enrollmentId(), memberId);
		if (courseWithdrawalRepository.cancelUpdateCourseWithdrawalRequests(Timestamp.from(clock.instant()), actorId, normalizedReason, requestId) != 1)
			throw invalidTransition();
		courseWithdrawalRepository.cancelUpdateCourseEnrollments(request.enrollmentId());
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of("status", "PENDING"), Map.of("status", "CANCELLED")));
		notifications.write("course-withdrawal-cancelled:" + requestId, memberId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("reason", normalizedReason));
		return find(requestId);
	}

	private WithdrawalRequest create(UUID requesterId, AccountRole requesterRole, UUID memberId, UUID enrollmentId, String reason) {
		String normalizedReason = reason(reason);
		active(requesterId, requesterRole, true);
		if (!requesterId.equals(memberId))
			active(memberId, AccountRole.MEMBER, true);
		Map<String, Object> visible = courseWithdrawalRepository.createFindCourseEnrollments(enrollmentId, memberId).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
		UUID runId = uuid(visible, "course_run_id");
		requireWithdrawableRun(lockRun(runId));
		Map<String, Object> enrollment = lockEnrollment(enrollmentId, memberId);
		if ("WITHDRAWAL_PENDING".equals(text(enrollment, "lifecycle_status")) && Boolean.TRUE.equals(courseWithdrawalRepository.createFindCourseWithdrawalRequests(enrollmentId)))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ALREADY_PENDING", "The Enrollment already has a pending withdrawal request.");
		if (!"ACTIVE".equals(text(enrollment, "lifecycle_status")))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_ENROLLMENT_INACTIVE", "Only an active Enrollment can be withdrawn.");
		if (Boolean.TRUE.equals(courseWithdrawalRepository.createFindCourseTransferRequests(enrollmentId)))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_TRANSFER_OPEN", "The Enrollment already has an open transfer request.");
		Instant now = clock.instant();
		RefundSnapshot snapshot = refundSnapshot(enrollmentId, number(enrollment, "total_units_snapshot"), amount(enrollment, "amount_paid_vnd_snapshot"), now);
		if (!CourseWithdrawalPolicy.canWithdraw(snapshot.remainingUnits(), snapshot.totalUnits()))
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED", "Withdrawal is no longer allowed after the final unit has started.");
		return insertRequest(requesterId, requesterRole, memberId, enrollmentId, runId, normalizedReason, CourseWithdrawalCause.VOLUNTARY, null, null, snapshot, now);
	}

	private WithdrawalRequest insertRequest(UUID requesterId, AccountRole requesterRole, UUID memberId, UUID enrollmentId, UUID runId, String normalizedReason, CourseWithdrawalCause cause, UUID responseId, UUID eventId, RefundSnapshot snapshot, Instant now) {
		CourseWithdrawalPolicy.Decision decision = CourseWithdrawalPolicy.evaluate(cause, snapshot.remainingUnits(), snapshot.totalUnits(), snapshot.unusedAllocation());
		if (!decision.eligible())
			throw CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_WINDOW_CLOSED", "Withdrawal is no longer allowed after the final unit has started.");
		UUID requestId = UUID.randomUUID();
		courseWithdrawalRepository.insertRequestInsertCourseWithdrawalRequests(requestId, enrollmentId, runId, memberId, requesterId, normalizedReason, Timestamp.from(now), cause.name(), responseId, eventId, Timestamp.from(now), snapshot.totalUnits(), snapshot.remainingUnits(), decision.refundBasisVnd(), decision.refundAmountVnd(), decision.eligible(), decision.reason());
		courseWithdrawalRepository.insertRequestUpdateCourseEnrollments(enrollmentId);
		Map<String, Object> captured = new LinkedHashMap<>(Map.of("status", "PENDING", "enrollmentId", enrollmentId.toString(), "courseRunId", runId.toString(), "cause", cause.name(), "remainingUnits", snapshot.remainingUnits(), "totalUnits", snapshot.totalUnits(), "refundBasisVnd", decision.refundBasisVnd().toString(), "refundAmountVnd", decision.refundAmountVnd().toString()));
		if (responseId != null)
			captured.put("scheduleChangeResponseId", responseId.toString());
		audits.save(new AuditEvent(UUID.randomUUID(), requesterId, "COURSE_WITHDRAWAL_REQUESTED", "COURSE_WITHDRAWAL_REQUEST", requestId, normalizedReason, Map.of(), captured));
		if (!requesterId.equals(memberId))
			notifications.write("course-withdrawal-requested:" + requestId, memberId, "COURSE_WITHDRAWAL_REQUESTED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("requesterRole", requesterRole.name(), "actorAccountId", requesterId.toString(), "memberAccountId", memberId.toString()));
		for (UUID managerId : courseWithdrawalRepository.insertRequestFindAccounts()) notifications.write("course-withdrawal-review:" + requestId + ":" + managerId, managerId, "COURSE_WITHDRAWAL_REVIEW_REQUIRED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("courseRunId", runId.toString(), "cause", cause.name()));
		return find(requestId);
	}

	private RefundSnapshot refundSnapshot(UUID enrollmentId, int totalUnits, BigInteger amountPaid, Instant now) {
		return courseWithdrawalSnapshot.refundSnapshot(enrollmentId, totalUnits, amountPaid, now);
	}

	private void promoteAfterCommit(UUID runId, Instant now) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

			@Override
			public void afterCommit() {
				try {
					promotions.promote(runId, now);
				} catch (RuntimeException exception) {
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
		return courseWithdrawalRepository.lockRun(runId);
	}

	private void requireWithdrawableRun(String status) {
		if (!List.of("OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS").contains(status))
			throw CourseWithdrawalException.conflict("COURSE_RUN_NOT_WITHDRAWABLE", "The Course Run no longer accepts withdrawal requests.");
	}

	private WithdrawalRequest lockForDecision(UUID requestId, UUID memberId) {
		WithdrawalRequest visible = find(requestId);
		if (memberId != null && !memberId.equals(visible.memberAccountId()))
			throw CourseWithdrawalException.notFound();
		active(visible.memberAccountId(), AccountRole.MEMBER, true);
		lockRun(visible.courseRunId());
		return lock(requestId);
	}

	private Map<String, Object> lockEnrollment(UUID enrollmentId, UUID memberId) {
		return courseWithdrawalRepository.lockEnrollment(enrollmentId, memberId);
	}

	private String reason(String reason) {
		if (reason == null || reason.isBlank() || reason.trim().length() > 1000)
			throw CourseWithdrawalException.invalid("A non-blank reason of at most 1000 characters is required.");
		return reason.trim();
	}

	private CourseWithdrawalException invalidTransition() {
		return CourseWithdrawalException.conflict("COURSE_WITHDRAWAL_INVALID_TRANSITION", "The withdrawal request is no longer pending.");
	}

	private WithdrawalRequest lock(UUID id) {
		return courseWithdrawalRepository.lock(id);
	}

	private WithdrawalRequest find(UUID id) {
		return courseWithdrawalRepository.find(id);
	}

	private WithdrawalRequest map(ResultSet rs, int row) throws SQLException {
		return courseWithdrawalRepository.map(rs, row);
	}

	record RefundSnapshot(int totalUnits, int remainingUnits, BigInteger unusedAllocation) {
	}

	private static UUID uuid(Map<String, Object> row, String key) {
		return (UUID) row.get(key);
	}

	private static String text(Map<String, Object> row, String key) {
		return row.get(key) == null ? null : row.get(key).toString();
	}

	private static int number(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).intValue();
	}

	private static BigInteger amount(Map<String, Object> row, String key) {
		return CourseWithdrawalSnapshot.amount(row, key);
	}

	private static BigInteger amount(BigDecimal value) {
		return CourseWithdrawalSnapshot.amount(value);
	}

	private static Instant instant(Map<String, Object> row, String key) {
		return CourseWithdrawalSnapshot.instant(row, key);
	}

	private static Instant instant(Timestamp value) {
		return CourseWithdrawalSnapshot.instant(value);
	}

	private final CourseWithdrawalRepository courseWithdrawalRepository;

	private final CourseWithdrawalSnapshot courseWithdrawalSnapshot;
}
