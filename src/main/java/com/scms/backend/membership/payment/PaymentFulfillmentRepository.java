package com.scms.backend.membership.payment;

import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.payment.PaymentFulfillmentService.CourseUnit;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

final class PaymentFulfillmentRepository {

	private final JdbcTemplate db;

	PaymentFulfillmentRepository(JdbcTemplate db) {
		this.db = db;
	}

	Map<String, Object> courseOrderForUpdate(UUID memberId, UUID orderId) {
		try {
			lockCourseTransferForPayment(orderId);
			UUID runId = db.queryForObject("select course_run_id from course_orders where id=? and member_account_id=?", UUID.class, orderId, memberId);
			db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
			db.queryForObject("select id from course_orders where id=? and member_account_id=? for update", UUID.class, orderId, memberId);
			db.queryForObject("select id from course_reservations where course_order_id=? for update", UUID.class, orderId);
			db.queryForList("select id from course_payments where course_order_id=? for update", orderId);
			return db.queryForMap(coursePaymentSql() + " and orders.id=? and orders.member_account_id=?", orderId, memberId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Course Order was not found");
		}
	}

	UUID memberIdForPayment(UUID paymentId) {
		try {
			return db.queryForObject("select o.member_account_id from payments p join membership_orders o on o.id=p.order_id where p.id=?", UUID.class, paymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Payment was not found");
		}
	}

	UUID memberIdForOrder(UUID orderId) {
		try {
			return db.queryForObject("select member_account_id from membership_orders where id=?", UUID.class, orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
	}

	void lockCourseTransferForPayment(UUID orderId) {
		var transfers = db.queryForList("select source_run_id,target_run_id from course_transfer_offers where difference_order_id=? for update", orderId);
		if (!transfers.isEmpty()) {
			var transfer = transfers.getFirst();
			for (UUID runId : List.of(uuid(transfer, "source_run_id"), uuid(transfer, "target_run_id")).stream().distinct().sorted().toList()) db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
		}
	}

	Map<String, Object> coursePaymentForUpdate(UUID coursePaymentId) {
		try {
			UUID paymentOrderId = db.queryForObject("select course_order_id from course_payments where id=?", UUID.class, coursePaymentId);
			lockCourseTransferForPayment(paymentOrderId);
			UUID runId = db.queryForObject("""
				select orders.course_run_id from course_payments payment
				join course_orders orders on orders.id=payment.course_order_id where payment.id=?
				""", UUID.class, coursePaymentId);
			db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
			UUID orderId = db.queryForObject("select course_order_id from course_payments where id=?", UUID.class, coursePaymentId);
			db.queryForObject("select id from course_orders where id=? for update", UUID.class, orderId);
			db.queryForObject("select id from course_reservations where course_order_id=? for update", UUID.class, orderId);
			db.queryForObject("select id from course_payments where id=? for update", UUID.class, coursePaymentId);
			return db.queryForMap(coursePaymentSql() + " and payment.id=?", coursePaymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Course Payment was not found");
		}
	}

	Map<String, Object> coursePayment(UUID coursePaymentId) {
		try {
			return db.queryForMap(coursePaymentSql() + " and payment.id=?", coursePaymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Course Payment was not found");
		}
	}

	String coursePaymentSql() {
		return """
			select payment.id payment_id,orders.id order_id,orders.order_reference,orders.actor_account_id,
				orders.member_account_id,orders.course_run_id,orders.tuition_vnd_snapshot,
				payment.status payment_status,coalesce(payment.amount,orders.tuition_vnd_snapshot) payment_amount,
				coalesce(payment.currency_code,orders.currency_code) payment_currency,
				payment.payment_reference bank_transfer_content,payment.bank_code_snapshot,
				payment.bank_account_number_snapshot,payment.bank_account_name_snapshot,
				payment.deadline_at expires_at,payment.method payment_method,payment.provider,
				payment.provider_transaction_id,payment.processed_by_account_id,payment.paid_at,
				orders.status order_status,reservation.id reservation_id,
				reservation.status reservation_status,reservation.expires_at reservation_expires_at,
				run.status run_status,run.total_units
			from course_orders orders
			join course_reservations reservation on reservation.course_order_id=orders.id
			join course_runs run on run.id=orders.course_run_id
			left join course_payments payment on payment.course_order_id=orders.id
			where 1=1
			""";
	}

	Map<String, Object> paymentForUpdate(UUID paymentId) {
		try {
			UUID orderId = db.queryForObject("select order_id from payments where id=?", UUID.class, paymentId);
			db.queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId);
			db.queryForObject("select id from payments where id=? for update", UUID.class, paymentId);
			return payment(paymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Payment was not found");
		}
	}

	Map<String, Object> payment(UUID paymentId) {
		try {
			String sql = """
				select p.id payment_id,p.order_id,p.method payment_method,p.status payment_status,
					p.amount payment_amount,p.currency_code payment_currency,p.bank_transfer_content,
					p.bank_code_snapshot,p.bank_account_number_snapshot,p.bank_account_name_snapshot,
					p.provider,p.provider_reference,
					p.provider_transaction_id,p.processed_by_account_id,p.paid_at,
					o.status order_status,o.fulfillment_mode,o.order_number,o.member_account_id,
					a.full_name member_name,m.member_code,o.offer_id,o.plan_code_snapshot,
					o.offer_name_snapshot,o.price_amount_snapshot,o.currency_code_snapshot,
					o.duration_days_snapshot,o.expires_at
				from payments p join membership_orders o on o.id=p.order_id
				join accounts a on a.id=o.member_account_id
				left join member_profiles m on m.account_id=a.id where p.id=?
				""";
			return db.queryForMap(sql, paymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Payment was not found");
		}
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return (UUID) row.get(key);
	}

	int fulfillCourseInsertCoursePayments(java.util.UUID paymentId, java.util.UUID orderId, java.util.UUID reservationId, java.util.UUID memberAccountId, java.util.UUID actorAccountId, java.lang.String orderReference, java.lang.Object paymentAmount, java.sql.Timestamp value8, java.util.UUID actorId, java.sql.Timestamp nowTimestamp, java.sql.Timestamp nowTimestamp2, java.sql.Timestamp nowTimestamp3) {
		return db.update("""
			insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,
				actor_account_id,payment_reference,amount,currency_code,status,deadline_at,method,
				processed_by_account_id,paid_at,created_at,updated_at)
			values(?,?,?,?,?,?,?,'VND','PAID',?,'CASH',?,?,?,?)
			""", paymentId, orderId, reservationId, memberAccountId, actorAccountId, orderReference, paymentAmount, value8, actorId, nowTimestamp, nowTimestamp2, nowTimestamp3);
	}

	int fulfillCourseUpdateCoursePayments(java.lang.String method, java.lang.Object value2, java.lang.String providerTransactionId, java.util.UUID actorId, java.sql.Timestamp nowTimestamp, java.util.UUID paymentId) {
		return db.update("""
			update course_payments set status='PAID',method=?,provider=?,provider_transaction_id=?,
				processed_by_account_id=?,paid_at=?,updated_at=current_timestamp where id=? and status='PENDING'
			""", method, value2, providerTransactionId, actorId, nowTimestamp, paymentId);
	}

	int fulfillCourseUpdateCourseOrders(java.util.UUID orderId) {
		return db.update("update course_orders set status='PAID',updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'", orderId);
	}

	int fulfillCourseUpdateCourseReservations(java.util.UUID reservationId) {
		return db.update("update course_reservations set status='ENROLLED',updated_at=current_timestamp where id=? and status='HELD'", reservationId);
	}

	int fulfillCourseInsertCourseEnrollments(java.util.UUID enrollmentId, java.util.UUID orderId, java.util.UUID reservationId, java.util.UUID paymentId, java.util.UUID memberAccountId, java.util.UUID actorAccountId, java.util.UUID actorId, java.util.UUID courseRunId, java.math.BigInteger paidAmount, int size, java.sql.Timestamp nowTimestamp, java.math.BigInteger paidAmount2) {
		return db.update("""
			insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,
				member_account_id,actor_account_id,processed_by_account_id,course_run_id,
				amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,credit_applied_vnd,cash_paid_vnd)
			values(?,?,?,?,?,?,?,?,?,?,?,0,?)
			""", enrollmentId, orderId, reservationId, paymentId, memberAccountId, actorAccountId, actorId, courseRunId, paidAmount, size, nowTimestamp, paidAmount2);
	}

	int fulfillCourseInsertCourseEnrollmentSessions(java.util.UUID randomUUID, java.util.UUID enrollmentId, java.util.UUID unitId, java.util.UUID courseRunId, int unitNumber, java.math.BigInteger allocation, java.lang.String disposition) {
		return db.update("""
				insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,
					unit_number,allocation_vnd,disposition) values(?,?,?,?,?,?,?)
			""", randomUUID, enrollmentId, unitId, courseRunId, unitNumber, allocation, disposition);
	}

	int fulfillLockedUpdatePayments(java.lang.String provider, java.lang.String providerTransactionId, java.lang.String evidence, java.util.UUID actorId, java.sql.Timestamp nowTimestamp, java.util.UUID paymentId) {
		return db.update("""
			update payments set status='PAID', provider=?, provider_transaction_id=?, evidence=?,
				processed_by_account_id=?, paid_at=?, updated_at=current_timestamp
			where id=? and status='PENDING'
			""", provider, providerTransactionId, evidence, actorId, nowTimestamp, paymentId);
	}

	int fulfillLockedUpdateMembershipOrders(java.sql.Timestamp nowTimestamp, java.util.UUID orderId) {
		return db.update("update membership_orders set status='PAID',paid_at=?,expires_at=null,updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'", nowTimestamp, orderId);
	}

	int createArtifactsInsertMemberships(java.util.UUID membershipId, java.util.UUID memberId, java.util.UUID orderId, java.util.UUID offerId, java.lang.Object planCodeSnapshot, java.lang.Object offerNameSnapshot, java.lang.Object priceAmountSnapshot, java.lang.Object currencyCodeSnapshot, java.lang.Object durationDaysSnapshot, java.lang.Object value10, java.lang.Object value11, java.lang.Object value12) {
		return db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,
				offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				status,starts_at,ends_at)
			values(?,?,?,?,?,?,?,?,?,?,?,?)
			""", membershipId, memberId, orderId, offerId, planCodeSnapshot, offerNameSnapshot, priceAmountSnapshot, currencyCodeSnapshot, durationDaysSnapshot, value10, value11, value12);
	}

	int createArtifactsInsertReceipts(java.util.UUID receiptId, java.lang.String value2, java.util.UUID paymentId, java.util.UUID orderId, java.util.UUID memberId, java.lang.Object paymentAmount, java.lang.Object paymentCurrency, java.lang.Object paymentMethod) {
		return db.update("""
			insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,
				currency_code_snapshot,payment_method_snapshot)
			values(?,?,?,?,?,?,?,?)
			""", receiptId, value2, paymentId, orderId, memberId, paymentAmount, paymentCurrency, paymentMethod);
	}

	java.util.List<com.scms.backend.membership.payment.PaymentFulfillmentService.CourseUnit> courseUnitsFindCourseSessions(java.util.UUID courseRunId) {
		return db.query("""
			select coalesce(replacement.id, original.id) id,original.unit_number,
				coalesce(replacement.start_at, original.start_at) start_at
			from course_sessions original
			left join course_sessions replacement on replacement.id=original.replacement_session_id
			where original.course_run_id=? and original.is_makeup=false
			  and not exists (select 1 from course_sessions replaced_by
				where replaced_by.replacement_session_id=original.id)
			order by original.unit_number,original.id
			""", (result, index) -> new CourseUnit(result.getObject("id", UUID.class), result.getInt("unit_number"), result.getTimestamp("start_at").toInstant()), courseRunId);
	}

	java.util.Map<java.lang.String, java.lang.Object> courseEnrollmentFindCourseEnrollments(java.util.UUID orderId, java.util.UUID memberId) {
		return db.queryForMap("""
			select id,course_order_id,member_account_id,course_run_id,amount_paid_vnd_snapshot,
				total_units_snapshot,enrolled_at from course_enrollments where course_order_id=? and member_account_id=?
			""", orderId, memberId);
	}

	java.util.Map<java.lang.String, java.lang.Object> fulfillCashFindMembershipOrders(java.util.UUID paymentId, java.util.UUID orderId) {
		return db.queryForMap("""
			select ?::uuid payment_id,o.id order_id,'CASH' payment_method,'PENDING' payment_status,
				o.order_number,a.full_name member_name,m.member_code,null::text provider_reference,
				o.price_amount_snapshot payment_amount,o.currency_code_snapshot payment_currency,
				o.status order_status,o.fulfillment_mode,o.member_account_id,o.offer_id,o.plan_code_snapshot,
				o.offer_name_snapshot,o.price_amount_snapshot,o.currency_code_snapshot,
				o.duration_days_snapshot
			from membership_orders o join accounts a on a.id=o.member_account_id
			join member_profiles m on m.account_id=a.id where o.id=? for update of o
			""", paymentId, orderId);
	}

	int fulfillCashInsertPayments(java.util.UUID paymentId, java.util.UUID orderId, java.lang.Object paymentAmount, java.util.UUID actorId, java.sql.Timestamp nowTimestamp) {
		return db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at)
			values(?,?,'CASH','PAID',?,'VND',?,?)
			""", paymentId, orderId, paymentAmount, actorId, nowTimestamp);
	}

	int fulfillCashUpdateMembershipOrders(java.sql.Timestamp nowTimestamp, java.util.UUID orderId) {
		return db.update("update membership_orders set status='PAID',paid_at=?,expires_at=null,updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'", nowTimestamp, orderId);
	}

	java.lang.Integer ensureMembershipCanFulfillFindMemberships(java.util.UUID memberAccountId) {
		return db.queryForObject("select count(*) from memberships where member_account_id=? and status='ACTIVE'", Integer.class, memberAccountId);
	}

	java.util.Map<java.lang.String, java.lang.Object> resultFindMemberships(java.util.UUID orderId) {
		return db.queryForMap("""
			select m.id membership_id,r.id receipt_id,o.fulfillment_mode from memberships m
			join receipts r on r.order_id=m.order_id join membership_orders o on o.id=m.order_id
			where m.order_id=?
			""", orderId);
	}
}
