package com.scms.backend.membership.payment;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class SepayRepository {

	java.util.UUID createForReceptionistFindAccounts(java.lang.Object value2) {
		return db.queryForObject("""
			select a.id from accounts a join member_profiles p on p.account_id=a.id
			where p.member_code=? and a.role='MEMBER' and a.status='ACTIVE'
			""", UUID.class, value2);
	}

	java.util.UUID findCourseRunId(java.util.UUID orderId, java.util.UUID memberId) {
		return db.queryForObject("select course_run_id from course_orders where id=? and member_account_id=?", UUID.class, orderId, memberId);
	}

	java.util.UUID lockCourseRun(java.util.UUID runId) {
		return db.queryForObject("select id from course_runs where id=? for update", UUID.class, runId);
	}

	java.util.UUID lockCourseOrder(java.util.UUID orderId, java.util.UUID memberId) {
		return db.queryForObject("select id from course_orders where id=? and member_account_id=? for update", UUID.class, orderId, memberId);
	}

	java.util.UUID lockCourseReservation(java.util.UUID orderId) {
		return db.queryForObject("select id from course_reservations where course_order_id=? for update", UUID.class, orderId);
	}

	java.util.Map<java.lang.String, java.lang.Object> findCoursePaymentContext(java.util.UUID orderId, java.util.UUID memberId) {
		return db.queryForMap("""
			select o.id order_id,o.order_reference,o.actor_account_id,o.tuition_vnd_snapshot,o.currency_code,
				o.status order_status,r.id reservation_id,r.status reservation_status,r.expires_at,
				r.course_run_id,run.status run_status
			from course_orders o join course_reservations r on r.course_order_id=o.id
			join course_runs run on run.id=r.course_run_id
			where o.id=? and o.member_account_id=?
			""", orderId, memberId);
	}

	java.util.List<java.util.Map<java.lang.String, java.lang.Object>> lockCoursePayments(java.util.UUID orderId) {
		return db.queryForList("select * from course_payments where course_order_id=? for update", orderId);
	}

	int createCoursePaymentInsertCoursePayments(java.util.UUID paymentId, java.util.UUID orderId, java.lang.Object reservationId, java.util.UUID memberId, java.lang.Object actorAccountId, java.lang.String reference, java.lang.Object tuitionVndSnapshot, java.sql.Timestamp expiresAtTimestamp, java.lang.String bankCode, java.lang.String bankAccount, java.lang.String bankAccountName, java.sql.Timestamp nowTimestamp, java.sql.Timestamp nowTimestamp2) {
		return db.update("""
			insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,
				payment_reference,amount,currency_code,status,deadline_at,bank_code_snapshot,
				bank_account_number_snapshot,bank_account_name_snapshot,method,provider,created_at,updated_at)
			values(?,?,?,?,?,?,?,'VND','PENDING',?,?,?,?,'SEPAY','SEPAY',?,?)
			""", paymentId, orderId, reservationId, memberId, actorAccountId, reference, tuitionVndSnapshot, expiresAtTimestamp, bankCode, bankAccount, bankAccountName, nowTimestamp, nowTimestamp2);
	}

	java.util.Map<java.lang.String, java.lang.Object> findCoursePayment(java.util.UUID paymentId) {
		return db.queryForMap("select * from course_payments where id=?", paymentId);
	}

	java.util.Map<java.lang.String, java.lang.Object> createPaymentFindMembershipOrders(java.util.UUID orderId, java.util.UUID memberId) {
		return db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update", orderId, memberId);
	}

	int createPaymentUpdateMembershipOrders(java.sql.Timestamp expiresAtTimestamp, java.util.UUID orderId) {
		return db.update("update membership_orders set expires_at=?,payment_method='BANK_TRANSFER',updated_at=current_timestamp where id=?", expiresAtTimestamp, orderId);
	}

	int createPaymentInsertPayments(java.util.UUID paymentId, java.util.UUID orderId, java.lang.Object priceAmountSnapshot, java.lang.String reference, java.lang.String bankCode, java.lang.String bankAccount, java.lang.String bankAccountName, java.lang.String reference2) {
		return db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,
				bank_code_snapshot,bank_account_number_snapshot,bank_account_name_snapshot,
				provider,provider_reference)
			values(?,?,'BANK_TRANSFER','PENDING',?,'VND',?,?,?,?,'SEPAY',?)
			""", paymentId, orderId, priceAmountSnapshot, reference, bankCode, bankAccount, bankAccountName, reference2);
	}

	java.util.Map<java.lang.String, java.lang.Object> createPaymentFindPayments(java.util.UUID paymentId) {
		return db.queryForMap("select * from payments where id=?", paymentId);
	}

	java.util.UUID findMembershipPaymentByReference(java.lang.String paymentReference) {
		return db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?", UUID.class, paymentReference);
	}

	java.util.UUID findMembershipPaymentByNormalizedReference(java.lang.String normalizedPaymentReference) {
		return db.queryForObject("select id from payments where provider='SEPAY' and replace(provider_reference, '-', '')=?", UUID.class, normalizedPaymentReference);
	}

	java.util.UUID findCoursePaymentByReference(java.lang.String paymentReference) {
		return db.queryForObject("select id from course_payments where payment_reference=?", UUID.class, paymentReference);
	}

	java.util.UUID findCoursePaymentByNormalizedReference(java.lang.String normalizedPaymentReference) {
		return db.queryForObject("select id from course_payments where replace(payment_reference, '-', '')=?", UUID.class, normalizedPaymentReference);
	}

	java.util.List<java.util.Map<java.lang.String, java.lang.Object>> pendingPaymentFindPayments(java.util.UUID orderId) {
		return db.queryForList("select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId);
	}

	private final JdbcTemplate db;

	private final String bankCode;

	private final String bankAccount;

	private final String bankAccountName;

	SepayRepository(JdbcTemplate db, String bankCode, String bankAccount, String bankAccountName) {
		this.db = db;
		this.bankCode = bankCode;
		this.bankAccount = bankAccount;
		this.bankAccountName = bankAccountName;
	}
}
