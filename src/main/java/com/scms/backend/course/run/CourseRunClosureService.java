package com.scms.backend.course.run;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.order.CourseReservationExpiryService;
import com.scms.backend.course.refund.CourseUnitRefunds;
import com.scms.backend.course.waitlist.CourseWaitlistService;
import com.scms.backend.notification.NotificationWriter;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseRunClosureService {

	private final JdbcTemplate db;

	private final CourseRunRepository runs;

	private final CourseReservationExpiryService expiry;

	private final CourseWaitlistService waitlists;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final Clock clock;

	CourseRunClosureService(JdbcTemplate db, CourseRunRepository runs, CourseReservationExpiryService expiry, CourseWaitlistService waitlists, AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.runs = runs;
		this.expiry = expiry;
		this.waitlists = waitlists;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
		this.courseRunClosureRepository = new CourseRunClosureRepository(this.db);
	}

	List<UUID> findDueRunIds(Instant now, UUID afterId, int limit) {
		return courseRunClosureRepository.findDueRunIdsFindCourseRuns(Timestamp.from(now), afterId, afterId, limit);
	}

	@Transactional
	boolean closeAtRegistration(UUID runId, Instant now) {
		CourseRun run = runs.findByIdForUpdate(runId).orElse(null);
		if (run == null || run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || run.getRegistrationClosesAt().isAfter(now))
			return false;
		List<UUID> cancellationRecipients = affectedMembers(runId);
		expiry.expireDueForRun(runId, now);
		int enrollmentCount = courseRunClosureRepository.closeAtRegistrationFindCourseEnrollments(runId);
		Map<String, Object> before = snapshot(run);
		if (enrollmentCount >= run.getMinimumEnrollment()) {
			run.confirm();
			runs.saveAndFlush(run);
			waitlists.cancelForRun(null, runId);
			Map<String, Object> after = snapshot(run);
			after.put("reasonCode", "MINIMUM_ENROLLMENT_MET");
			after.put("activeEnrollmentCount", enrollmentCount);
			audits.save(new AuditEvent(UUID.randomUUID(), null, "COURSE_RUN_CONFIRMED", "COURSE_RUN", runId, null, before, after));
			notifyEnrolledMembers(runId, "COURSE_RUN_CONFIRMED");
		} else {
			run.cancel();
			runs.saveAndFlush(run);
			cancelEffects(null, run, now, cancellationRecipients);
			Map<String, Object> after = snapshot(run);
			after.put("reasonCode", "MINIMUM_ENROLLMENT_NOT_MET");
			after.put("activeEnrollmentCount", enrollmentCount);
			audits.save(new AuditEvent(UUID.randomUUID(), null, "COURSE_RUN_CANCELLED", "COURSE_RUN", runId, null, before, after));
		}
		return true;
	}

	public void cancelEffects(UUID actorId, CourseRun run, Instant now) {
		cancelEffects(actorId, run, now, null);
	}

	private void cancelEffects(UUID actorId, CourseRun run, Instant now, List<UUID> cancellationRecipients) {
		UUID runId = run.getId();
		List<UUID> orderIds = courseRunClosureRepository.cancelEffectsFindCourseOrders(runId);
		for (UUID orderId : orderIds) courseRunClosureRepository.lockOrderForClosure(orderId);
		for (UUID reservationId : courseRunClosureRepository.cancelEffectsFindCourseReservations(runId)) courseRunClosureRepository.lockReservationForClosure(reservationId);
		for (UUID paymentId : courseRunClosureRepository.cancelEffectsFindCoursePayments(runId)) courseRunClosureRepository.lockPaymentForClosure(paymentId);
		if (cancellationRecipients == null)
			cancellationRecipients = affectedMembers(runId);
		courseRunClosureRepository.cancelEffectsUpdateCourseReservations(Timestamp.from(now), runId);
		courseRunClosureRepository.cancelEffectsUpdateCourseOrders(Timestamp.from(now), runId);
		courseRunClosureRepository.cancelEffectsUpdateCoursePayments(Timestamp.from(now), runId);
		courseRunClosureRepository.cancelEffectsUpdateCourseTransferHolds(Timestamp.from(now), runId);
		courseRunClosureRepository.cancelEffectsUpdateCourseTransferRequests(runId);
		courseRunClosureRepository.cancelEffectsUpdateCourseTransferOffers(runId);
		cancelPendingWithdrawals(actorId, runId, now);
		CourseUnitRefunds.cancelled(db, runId, actorId, now);
		// Both manager cancellation and minimum-enrollment closure hold the run lock.
		// Terminal learning/refund history is preserved; cancelled slots stop blocking resources.
		courseRunClosureRepository.cancelEffectsUpdateCourseSessions(Timestamp.from(now), runId);
		waitlists.cancelForRun(actorId, runId);
		for (UUID memberId : cancellationRecipients) {
			notifications.write("course-run:" + runId + ":" + memberId + ":course_run_cancelled", memberId, "COURSE_RUN_CANCELLED", "COURSE_RUN", runId, Map.of("courseRunId", runId.toString()));
		}
	}

	private void cancelPendingWithdrawals(UUID actorId, UUID runId, Instant now) {
		for (Map<String, Object> request : courseRunClosureRepository.cancelPendingWithdrawalsFindCourseWithdrawalRequests(Timestamp.from(now), actorId, runId)) {
			UUID requestId = (UUID) request.get("id"), memberId = (UUID) request.get("member_account_id");
			courseRunClosureRepository.cancelPendingWithdrawalsUpdateCourseEnrollments(request.get("course_enrollment_id"));
			audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST", requestId, "COURSE_RUN_CANCELLED", Map.of("status", "PENDING"), Map.of("status", "CANCELLED")));
			notifications.write("course-withdrawal-cancelled:" + requestId, memberId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("reason", "COURSE_RUN_CANCELLED"));
		}
	}

	public void cancelEffects(UUID actorId, CourseRun run) {
		cancelEffects(actorId, run, clock.instant());
	}

	private List<UUID> affectedMembers(UUID runId) {
		return courseRunClosureRepository.affectedMembersFindCourseOrders(runId);
	}

	private void notifyEnrolledMembers(UUID runId, String type) {
		for (UUID memberId : courseRunClosureRepository.notifyEnrolledMembersFindCourseEnrollments(runId)) {
			notifications.write("course-run:" + runId + ":" + memberId + ":" + type.toLowerCase(), memberId, type, "COURSE_RUN", runId, Map.of("courseRunId", runId.toString()));
		}
	}

	private Map<String, Object> snapshot(CourseRun run) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("runCode", run.getRunCode());
		data.put("minimumEnrollment", run.getMinimumEnrollment());
		data.put("status", run.getStatus().name());
		return data;
	}

	private final CourseRunClosureRepository courseRunClosureRepository;
}
