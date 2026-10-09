package com.scms.backend.membership;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentFulfillmentService {
	private final JdbcTemplate db;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	PaymentFulfillmentService(JdbcTemplate db, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.db = db; this.audits = audits; this.notifications = notifications; this.clock = clock;
	}

	@Transactional
	PaymentResultResponse fulfill(UUID paymentId, UUID actorId, String providerTransactionId,
			String evidence, String reason) {
		Map<String, Object> row = paymentForUpdate(paymentId);
		String status = string(row, "payment_status");
		if ("PAID".equals(status)) {
			String existing = string(row, "provider_transaction_id");
			if (providerTransactionId != null && existing != null && !providerTransactionId.equals(existing)) {
				throw PaymentException.conflict("Payment was already confirmed with another provider transaction");
			}
			return result(row);
		}
		if (!"PENDING".equals(status)) throw PaymentException.conflict("Payment is terminal");
		if (!"PENDING_PAYMENT".equals(string(row, "order_status"))) throw PaymentException.conflict("Order is not payable");
		ensureNoActiveMembership(row);

		Instant now = clock.instant();
		try {
			db.update("""
				update payments set status='PAID', provider_transaction_id=?, evidence=?,
					processed_by_account_id=?, paid_at=?, updated_at=current_timestamp
				where id=? and status='PENDING'
				""", providerTransactionId, evidence, actorId, Timestamp.from(now), paymentId);
			db.update("update membership_orders set status='PAID',paid_at=?,expires_at=null,updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'",
				Timestamp.from(now), uuid(row, "order_id"));
			return createArtifacts(row, paymentId, actorId, evidence, reason, now);
		} catch (DataIntegrityViolationException exception) {
			throw PaymentException.conflict("Payment or Order was already fulfilled");
		}
	}

	@Transactional
	PaymentResultResponse fulfillCash(UUID paymentId, UUID orderId, UUID actorId, String evidence, String reason) {
		Map<String, Object> row;
		try {
			row = db.queryForMap("""
				select ?::uuid payment_id,o.id order_id,'CASH' payment_method,'PENDING' payment_status,
					o.price_amount_snapshot payment_amount,o.currency_code_snapshot payment_currency,
					o.status order_status,o.member_account_id,o.offer_id,o.plan_code_snapshot,
					o.offer_name_snapshot,o.price_amount_snapshot,o.currency_code_snapshot,
					o.duration_days_snapshot
				from membership_orders o where o.id=? for update
				""", paymentId, orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
		if (!"PENDING_PAYMENT".equals(string(row, "order_status"))) throw PaymentException.conflict("Order is not payable");
		ensureNoActiveMembership(row);
		Instant now = clock.instant();
		try {
			db.update("""
				insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at)
				values(?,?,'CASH','PAID',?,'VND',?,?)
				""", paymentId, orderId, row.get("payment_amount"), actorId, Timestamp.from(now));
			db.update("update membership_orders set status='PAID',paid_at=?,expires_at=null,updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'",
				Timestamp.from(now), orderId);
			return createArtifacts(row, paymentId, actorId, evidence, reason, now);
		} catch (DataIntegrityViolationException exception) {
			throw PaymentException.conflict("Payment or Order was already fulfilled");
		}
	}

	private void ensureNoActiveMembership(Map<String, Object> row) {
		Integer active = db.queryForObject("select count(*) from memberships where member_account_id=? and status='ACTIVE'",
			Integer.class, uuid(row, "member_account_id"));
		if (active != null && active > 0) throw PaymentException.conflict("Member already has an active membership");
	}

	private PaymentResultResponse createArtifacts(Map<String, Object> row, UUID paymentId, UUID actorId,
			String evidence, String reason, Instant now) {
		UUID memberId = uuid(row, "member_account_id");
		UUID membershipId = UUID.randomUUID();
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,
				offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				status,starts_at,ends_at)
			values(?,?,?,?,?,?,?,?,?,'ACTIVE',?,?)
			""", membershipId, memberId, uuid(row, "order_id"), uuid(row, "offer_id"),
			row.get("plan_code_snapshot"), row.get("offer_name_snapshot"), row.get("price_amount_snapshot"),
			row.get("currency_code_snapshot"), row.get("duration_days_snapshot"), Timestamp.from(now),
			Timestamp.from(now.plusSeconds(number(row, "duration_days_snapshot").longValue() * 86400)));
		UUID receiptId = UUID.randomUUID();
		db.update("""
			insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,
				currency_code_snapshot,payment_method_snapshot)
			values(?,?,?,?,?,?,?,?)
			""", receiptId, "RC-" + receiptId, paymentId, uuid(row, "order_id"), memberId,
			row.get("payment_amount"), row.get("payment_currency"), row.get("payment_method"));
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "PAYMENT_PAID", "PAYMENT", paymentId,
			reason, Map.of("status", "PENDING"), Map.of("status", "PAID", "evidence", evidence == null ? "" : evidence)));
		notifications.write("PAYMENT_PAID:" + paymentId, memberId, "PAYMENT_PAID", "PAYMENT", paymentId,
			Map.of("orderId", uuid(row, "order_id").toString(), "status", "PAID"));
		return new PaymentResultResponse(paymentId, uuid(row, "order_id"), "PAID",
			string(row, "payment_method"), membershipId, receiptId, now);
	}

	Map<String, Object> paymentForUpdate(UUID paymentId) {
		return payment(paymentId, true);
	}

	Map<String, Object> payment(UUID paymentId) {
		return payment(paymentId, false);
	}

	private Map<String, Object> payment(UUID paymentId, boolean lock) {
		try {
			String sql = """
				select p.id payment_id,p.order_id,p.method payment_method,p.status payment_status,
					p.amount payment_amount,p.currency_code payment_currency,p.bank_transfer_content,
					p.bank_account_number_snapshot,p.provider,p.provider_reference,
					p.provider_transaction_id,p.processed_by_account_id,p.paid_at,
					o.status order_status,o.member_account_id,o.offer_id,o.plan_code_snapshot,
					o.offer_name_snapshot,o.price_amount_snapshot,o.currency_code_snapshot,
					o.duration_days_snapshot,o.expires_at
				from payments p join membership_orders o on o.id=p.order_id where p.id=?
				""" + (lock ? " for update" : "");
			return db.queryForMap(sql, paymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Payment was not found");
		}
	}

	PaymentResultResponse result(Map<String, Object> row) {
		UUID orderId = uuid(row, "order_id");
		Map<String, Object> fulfillment;
		try {
			fulfillment = db.queryForMap("""
				select m.id membership_id,r.id receipt_id from memberships m join receipts r on r.order_id=m.order_id
				where m.order_id=?
				""", orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.conflict("Paid Payment has incomplete fulfillment data");
		}
		return new PaymentResultResponse(uuid(row, "payment_id"), orderId, "PAID", string(row, "payment_method"),
			uuid(fulfillment, "membership_id"), uuid(fulfillment, "receipt_id"), instant(row, "paid_at"));
	}

	static UUID uuid(Map<String, Object> row, String key) { return (UUID) row.get(key); }
	static String string(Map<String, Object> row, String key) { Object value=row.get(key); return value == null ? null : value.toString().trim(); }
	static Number number(Map<String, Object> row, String key) { return (Number) row.get(key); }
	static BigInteger amount(Map<String, Object> row, String key) { return new BigDecimal(row.get(key).toString()).toBigIntegerExact(); }
	static Instant instant(Map<String, Object> row, String key) {
		Object value=row.get(key); return value instanceof Timestamp timestamp ? timestamp.toInstant() : (Instant) value;
	}
}
