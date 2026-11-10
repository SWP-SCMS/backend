package com.scms.backend.membership.payment;

import com.scms.backend.membership.PaymentException;
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
}
