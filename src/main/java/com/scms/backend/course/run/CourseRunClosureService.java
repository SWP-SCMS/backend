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
	}

	List<UUID> findDueRunIds(Instant now, UUID afterId, int limit) {
		return db.queryForList("""
			select id from course_runs
			where status='OPEN_FOR_REGISTRATION' and registration_closes_at<=?
			  and (?::uuid is null or id>?::uuid)
			order by id limit ?
			""", UUID.class, Timestamp.from(now), afterId, afterId, limit);
	}

	@Transactional
	boolean closeAtRegistration(UUID runId, Instant now) {
		CourseRun run = runs.findByIdForUpdate(runId).orElse(null);
		if (run == null || run.getStatus() != CourseRunStatus.OPEN_FOR_REGISTRATION || run.getRegistrationClosesAt().isAfter(now))
			return false;
		List<UUID> cancellationRecipients = affectedMembers(runId);
		expiry.expireDueForRun(runId, now);
		int enrollmentCount = db.queryForObject("select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')", Integer.class, runId);
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
		List<UUID> orderIds = db.queryForList("select id from course_orders where course_run_id=? order by id", UUID.class, runId);
		for (UUID orderId : orderIds) db.queryForObject("select id from course_orders where id=? for update", UUID.class, orderId);
		for (UUID reservationId : db.queryForList("select id from course_reservations where course_run_id=? order by id", UUID.class, runId)) db.queryForObject("select id from course_reservations where id=? for update", UUID.class, reservationId);
		for (UUID paymentId : db.queryForList("""
			select payment.id from course_payments payment join course_orders orders
			  on orders.id=payment.course_order_id where orders.course_run_id=? order by payment.id
			""", UUID.class, runId)) db.queryForObject("select id from course_payments where id=? for update", UUID.class, paymentId);
		if (cancellationRecipients == null)
			cancellationRecipients = affectedMembers(runId);
		db.update("update course_reservations set status='EXPIRED',updated_at=? " + "where course_run_id=? and status='HELD'", Timestamp.from(now), runId);
		db.update("update course_orders set status='EXPIRED',updated_at=? " + "where course_run_id=? and status='PENDING_PAYMENT'", Timestamp.from(now), runId);
		db.update("update course_payments set status='FAILED',updated_at=? " + "where course_order_id in (select id from course_orders where course_run_id=?) and status='PENDING'", Timestamp.from(now), runId);
		db.update("update course_transfer_holds set status='RELEASED',released_at=? where target_run_id=? and status='HELD'", Timestamp.from(now), runId);
		db.update("update course_transfer_requests set status='EXPIRED' where status='OFFERED' and id in " + "(select transfer_request_id from course_transfer_offers where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT'))", runId);
		db.update("update course_transfer_offers set status='CANCELLED' where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT')", runId);
		cancelPendingWithdrawals(actorId, runId, now);
		CourseUnitRefunds.cancelled(db, runId, actorId, now);
		// Both manager cancellation and minimum-enrollment closure hold the run lock.
		// Terminal learning/refund history is preserved; cancelled slots stop blocking resources.
		db.update("""
			update course_sessions set status='CANCELLED',version=version+1,updated_at=?
			where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED','CANCELLED_PENDING_MAKEUP')
			""", Timestamp.from(now), runId);
		waitlists.cancelForRun(actorId, runId);
		for (UUID memberId : cancellationRecipients) {
			notifications.write("course-run:" + runId + ":" + memberId + ":course_run_cancelled", memberId, "COURSE_RUN_CANCELLED", "COURSE_RUN", runId, Map.of("courseRunId", runId.toString()));
		}
	}

	private void cancelPendingWithdrawals(UUID actorId, UUID runId, Instant now) {
		for (Map<String, Object> request : db.queryForList("""
			update course_withdrawal_requests set status='CANCELLED',cancelled_at=?,cancelled_by_account_id=?,
			 cancellation_reason='COURSE_RUN_CANCELLED' where course_run_id=? and status='PENDING'
			returning id,course_enrollment_id,member_account_id
			""", Timestamp.from(now), actorId, runId)) {
			UUID requestId = (UUID) request.get("id"), memberId = (UUID) request.get("member_account_id");
			db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", request.get("course_enrollment_id"));
			audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST", requestId, "COURSE_RUN_CANCELLED", Map.of("status", "PENDING"), Map.of("status", "CANCELLED")));
			notifications.write("course-withdrawal-cancelled:" + requestId, memberId, "COURSE_WITHDRAWAL_CANCELLED", "COURSE_WITHDRAWAL_REQUEST", requestId, Map.of("reason", "COURSE_RUN_CANCELLED"));
		}
	}

	public void cancelEffects(UUID actorId, CourseRun run) {
		cancelEffects(actorId, run, clock.instant());
	}

	private List<UUID> affectedMembers(UUID runId) {
		return db.queryForList("select distinct member_account_id from course_orders " + "where course_run_id=? and status in ('PENDING_PAYMENT','PAID') order by member_account_id", UUID.class, runId);
	}

	private void notifyEnrolledMembers(UUID runId, String type) {
		for (UUID memberId : db.queryForList("select member_account_id from course_enrollments " + "where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by id", UUID.class, runId)) {
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
}
