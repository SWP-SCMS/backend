package com.scms.backend.course.order;

import com.scms.backend.course.order.CourseOrderService.StoredOrder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseOrderRepository {

	long countMemberHeldReservations(UUID accountId, UUID runId, Timestamp nowTimestamp) {
		return count("""
			select count(*) from course_reservations
			where member_account_id=? and course_run_id=? and status='HELD' and expires_at>?
			""", accountId, runId, nowTimestamp);
	}

	long countMemberEnrollments(UUID accountId, UUID runId) {
		return count("select count(*) from course_enrollments where member_account_id=? and course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')", accountId, runId);
	}

	long countRunEnrollments(UUID runId) {
		return count("select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')", runId);
	}

	long countRunHeldReservations(UUID runId, Timestamp nowTimestamp) {
		return count("""
			select count(*) from course_reservations where course_run_id=? and status='HELD' and expires_at>?
			""", runId, nowTimestamp);
	}

	long createCountCourseWaitlists(UUID runId, Timestamp nowTimestamp) {
		return count("""
			select count(*) from course_waitlists
			where course_run_id=? and status='INVITED' and invitation_expires_at>?
			""", runId, nowTimestamp);
	}

	long createCountCourseTransferHolds(UUID runId, Timestamp nowTimestamp) {
		return count("""
			select count(*) from course_transfer_holds where target_run_id=? and status='HELD' and expires_at>?
			""", runId, nowTimestamp);
	}

	int createInsertCourseOrders(UUID orderId, java.lang.String reference, UUID accountId, UUID actorId, UUID runId, java.math.BigInteger tuitionVnd, java.lang.String key, Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,
				tuition_vnd_snapshot,currency_code,status,idempotency_key,created_at,updated_at)
			values (?,?,?,?,?,?,'VND','PENDING_PAYMENT',?,?,?)
			""", orderId, reference, accountId, actorId, runId, tuitionVnd, key, nowTimestamp, nowTimestamp2);
	}

	int createInsertCourseReservations(UUID reservationId, UUID orderId, UUID accountId, UUID runId, Timestamp expiresAtTimestamp, Timestamp nowTimestamp, Timestamp nowTimestamp2) {
		return db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,
				created_at,updated_at)
			values (?,?,?,?,'HELD',?,?,?)
			""", reservationId, orderId, accountId, runId, expiresAtTimestamp, nowTimestamp, nowTimestamp2);
	}

	java.lang.Boolean hasHeldCourseOverlapFindCourseEnrollments(UUID memberId, UUID runId, Timestamp value4, Timestamp value5, UUID memberId2, UUID runId2, Timestamp nowTimestamp, Timestamp value9, Timestamp value10) {
		return db.queryForObject("""
			select exists (
				select 1 from course_enrollments enrollment
				join course_enrollment_sessions enrolled_unit on enrolled_unit.course_enrollment_id=enrollment.id
				join course_sessions session on session.id=enrolled_unit.course_session_id
				where enrollment.member_account_id=? and enrollment.course_run_id<>?
				  and enrollment.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
				  and enrolled_unit.disposition='ENROLLED'
				  and session.status in ('SCHEDULED','MAKEUP_SCHEDULED')
				  and session.start_at<? and session.end_at>?
				union all
				select 1 from course_reservations reservation
				join course_sessions session on session.course_run_id=reservation.course_run_id
				where reservation.member_account_id=? and reservation.course_run_id<>?
				  and reservation.status='HELD' and reservation.expires_at>?
				  and session.status in ('SCHEDULED','MAKEUP_SCHEDULED')
				  and session.start_at<? and session.end_at>?
			)
			""", Boolean.class, memberId, runId, value4, value5, memberId2, runId2, nowTimestamp, value9, value10);
	}

	List<StoredOrder> findByKeyFindRows(UUID actorId, java.lang.String key) {
		return db.query(orderQuery() + " where orders.actor_account_id=? and orders.idempotency_key=?", this::map, actorId, key);
	}

	List<StoredOrder> findOrderFindRows(UUID orderId, UUID memberId) {
		return db.query(orderQuery() + " where orders.id=? and orders.member_account_id=?", this::map, orderId, memberId);
	}

	private long count(String sql, Object... arguments) {
		return db.queryForObject(sql, Long.class, arguments);
	}

	private StoredOrder map(ResultSet result, int row) throws SQLException {
		UUID orderId = result.getObject("id", UUID.class);
		UUID memberId = result.getObject("member_account_id", UUID.class);
		UUID runId = result.getObject("course_run_id", UUID.class);
		Instant expiresAt = result.getTimestamp("expires_at").toInstant();
		String reservationStatus = result.getString("reservation_status");
		UUID paymentId = result.getObject("payment_id", UUID.class);
		CoursePaymentResponse payment = paymentId == null ? null : new CoursePaymentResponse(paymentId, orderId, result.getBigDecimal("payment_amount").toBigIntegerExact(), result.getString("payment_currency").trim(), result.getString("payment_method"), result.getString("provider"), result.getString("payment_reference"), result.getString("bank_code_snapshot"), result.getString("bank_account_number_snapshot"), result.getString("bank_account_name_snapshot"), "PENDING".equals(result.getString("payment_status")) ? CoursePaymentResponse.qrUrl(result.getString("bank_code_snapshot"), result.getString("bank_account_number_snapshot"), result.getString("bank_account_name_snapshot"), result.getBigDecimal("payment_amount").toBigIntegerExact(), result.getString("payment_reference")) : null, result.getTimestamp("payment_deadline").toInstant(), result.getString("payment_status"));
		return new StoredOrder(new CourseOrderResponse(orderId, result.getString("order_reference"), memberId, runId, result.getBigDecimal("tuition_vnd_snapshot").toBigIntegerExact(), result.getString("currency_code").trim(), result.getString("order_status"), reservationStatus, expiresAt, result.getTimestamp("created_at").toInstant(), payment), result.getObject("reservation_id", UUID.class), memberId, runId, reservationStatus, expiresAt);
	}

	private String orderQuery() {
		return """
			select orders.id,orders.order_reference,orders.member_account_id,
				orders.course_run_id,orders.tuition_vnd_snapshot,orders.currency_code,orders.status order_status,
				reservation.id reservation_id,reservation.status reservation_status,reservation.expires_at,
				orders.created_at,payment.id payment_id,payment.amount payment_amount,
				payment.currency_code payment_currency,payment.method payment_method,payment.provider,
				payment.payment_reference,payment.bank_code_snapshot,payment.bank_account_number_snapshot,
				payment.bank_account_name_snapshot,payment.status payment_status,payment.deadline_at payment_deadline
			from course_orders orders
			join course_reservations reservation on reservation.course_order_id=orders.id
			left join course_payments payment on payment.course_order_id=orders.id
			""";
	}

	private final JdbcTemplate db;

	CourseOrderRepository(JdbcTemplate db) {
		this.db = db;
	}
}
