package com.scms.backend.course.run;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseRunClosureRepository {

	List<UUID> findDueRunIdsFindCourseRuns(Timestamp nowTimestamp, UUID afterId, UUID afterId2, int limit) {
		return db.queryForList("""
			select id from course_runs
			where status='OPEN_FOR_REGISTRATION' and registration_closes_at<=?
			  and (?::uuid is null or id>?::uuid)
			order by id limit ?
			""", UUID.class, nowTimestamp, afterId, afterId2, limit);
	}

	java.lang.Integer closeAtRegistrationFindCourseEnrollments(UUID runId) {
		return db.queryForObject("select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')", Integer.class, runId);
	}

	List<UUID> cancelEffectsFindCourseOrders(UUID runId) {
		return db.queryForList("select id from course_orders where course_run_id=? order by id", UUID.class, runId);
	}

	UUID lockOrderForClosure(UUID orderId) {
		return db.queryForObject("select id from course_orders where id=? for update", UUID.class, orderId);
	}

	List<UUID> cancelEffectsFindCourseReservations(UUID runId) {
		return db.queryForList("select id from course_reservations where course_run_id=? order by id", UUID.class, runId);
	}

	UUID lockReservationForClosure(UUID reservationId) {
		return db.queryForObject("select id from course_reservations where id=? for update", UUID.class, reservationId);
	}

	List<UUID> cancelEffectsFindCoursePayments(UUID runId) {
		return db.queryForList("""
			select payment.id from course_payments payment join course_orders orders
			  on orders.id=payment.course_order_id where orders.course_run_id=? order by payment.id
			""", UUID.class, runId);
	}

	UUID lockPaymentForClosure(UUID paymentId) {
		return db.queryForObject("select id from course_payments where id=? for update", UUID.class, paymentId);
	}

	int cancelEffectsUpdateCourseReservations(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_reservations set status='EXPIRED',updated_at=? " + "where course_run_id=? and status='HELD'", nowTimestamp, runId);
	}

	int cancelEffectsUpdateCourseOrders(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_orders set status='EXPIRED',updated_at=? " + "where course_run_id=? and status='PENDING_PAYMENT'", nowTimestamp, runId);
	}

	int cancelEffectsUpdateCoursePayments(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_payments set status='FAILED',updated_at=? " + "where course_order_id in (select id from course_orders where course_run_id=?) and status='PENDING'", nowTimestamp, runId);
	}

	int cancelEffectsUpdateCourseTransferHolds(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_transfer_holds set status='RELEASED',released_at=? where target_run_id=? and status='HELD'", nowTimestamp, runId);
	}

	int cancelEffectsUpdateCourseTransferRequests(UUID runId) {
		return db.update("update course_transfer_requests set status='EXPIRED' where status='OFFERED' and id in " + "(select transfer_request_id from course_transfer_offers where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT'))", runId);
	}

	int cancelEffectsUpdateCourseTransferOffers(UUID runId) {
		return db.update("update course_transfer_offers set status='CANCELLED' where target_run_id=? and status in ('OFFERED','PENDING_PAYMENT')", runId);
	}

	int cancelEffectsUpdateCourseSessions(Timestamp nowTimestamp, UUID runId) {
		return db.update("""
			update course_sessions set status='CANCELLED',version=version+1,updated_at=?
			where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED','CANCELLED_PENDING_MAKEUP')
			""", nowTimestamp, runId);
	}

	List<Map<java.lang.String, java.lang.Object>> cancelPendingWithdrawalsFindCourseWithdrawalRequests(Timestamp nowTimestamp, UUID actorId, UUID runId) {
		return db.queryForList("""
			update course_withdrawal_requests set status='CANCELLED',cancelled_at=?,cancelled_by_account_id=?,
			 cancellation_reason='COURSE_RUN_CANCELLED' where course_run_id=? and status='PENDING'
			returning id,course_enrollment_id,member_account_id
			""", nowTimestamp, actorId, runId);
	}

	int cancelPendingWithdrawalsUpdateCourseEnrollments(java.lang.Object courseEnrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", courseEnrollmentId);
	}

	List<UUID> affectedMembersFindCourseOrders(UUID runId) {
		return db.queryForList("select distinct member_account_id from course_orders " + "where course_run_id=? and status in ('PENDING_PAYMENT','PAID') order by member_account_id", UUID.class, runId);
	}

	List<UUID> notifyEnrolledMembersFindCourseEnrollments(UUID runId) {
		return db.queryForList("select member_account_id from course_enrollments " + "where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by id", UUID.class, runId);
	}

	private final JdbcTemplate db;

	CourseRunClosureRepository(JdbcTemplate db) {
		this.db = db;
	}
}
