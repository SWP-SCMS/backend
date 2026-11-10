package com.scms.backend.course.order;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseReservationExpiryRepository {

	List<UUID> findDueReservationIdsFindCourseReservations(Timestamp nowTimestamp, UUID afterId, UUID afterId2, int limit) {
		return db.queryForList("""
			select id from course_reservations
			where status='HELD' and expires_at<=? and (?::uuid is null or id>?::uuid)
			order by id limit ?
			""", UUID.class, nowTimestamp, afterId, afterId2, limit);
	}

	List<Map<java.lang.String, java.lang.Object>> expireReservationFindCourseReservations(UUID reservationId) {
		return db.queryForList("""
			select course_order_id,member_account_id,course_run_id
			from course_reservations where id=?
			""", reservationId);
	}

	UUID expireReservationFindCourseRuns(UUID runId) {
		return db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
	}

	UUID expireReservationFindCourseOrders(UUID orderId) {
		return db.queryForObject("select id from course_orders where id=? for update", UUID.class, orderId);
	}

	UUID lockReservationForExpiry(UUID reservationId) {
		return db.queryForObject("select id from course_reservations where id=? for update", UUID.class, reservationId);
	}

	int expireReservationUpdateCourseReservations(Timestamp nowTimestamp, UUID reservationId, Timestamp nowTimestamp2) {
		return db.update("""
			update course_reservations
			set status='EXPIRED',updated_at=?
			where id=? and status='HELD' and expires_at<=?
			""", nowTimestamp, reservationId, nowTimestamp2);
	}

	int expireReservationUpdateCourseOrders(Timestamp nowTimestamp, UUID orderId) {
		return db.update("""
			update course_orders set status='EXPIRED',updated_at=?
			where id=? and status='PENDING_PAYMENT'
			""", nowTimestamp, orderId);
	}

	List<UUID> expireReservationFindCoursePayments(UUID orderId) {
		return db.queryForList("select id from course_payments where course_order_id=? order by id", UUID.class, orderId);
	}

	UUID lockPaymentForExpiry(UUID paymentId) {
		return db.queryForObject("select id from course_payments where id=? for update", UUID.class, paymentId);
	}

	int expireReservationUpdateCoursePayments(Timestamp nowTimestamp, UUID orderId) {
		return db.update("update course_payments set status='FAILED',updated_at=? " + "where course_order_id=? and status='PENDING'", nowTimestamp, orderId);
	}

	List<UUID> expireDueForRunFindCourseReservations(UUID runId, Timestamp nowTimestamp) {
		return db.queryForList("""
			select id from course_reservations
			where course_run_id=? and status='HELD' and expires_at<=?
			order by id
			""", UUID.class, runId, nowTimestamp);
	}

	private final JdbcTemplate db;

	CourseReservationExpiryRepository(JdbcTemplate db) {
		this.db = db;
	}
}
