package com.scms.backend.membership.payment;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.membership.FulfillmentMode;
import com.scms.backend.membership.MembershipLifecycleService;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.notification.NotificationWriter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

final class MembershipPaymentFulfillment {

	private final JdbcTemplate db;

	private final MembershipLifecycleService lifecycle;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	MembershipPaymentFulfillment(JdbcTemplate db, MembershipLifecycleService lifecycle, AuditEventRepository audits, NotificationWriter notifications) {
		this.db = db;
		this.lifecycle = lifecycle;
		this.audits = audits;
		this.notifications = notifications;
	}

	PaymentResultResponse fulfillLocked(Map<String, Object> row, UUID paymentId, UUID actorId, String provider, String providerTransactionId, String evidence, String reason, Instant now) {
		try {
			db.update("""
				update payments set status='PAID', provider=?, provider_transaction_id=?, evidence=?,
					processed_by_account_id=?, paid_at=?, updated_at=current_timestamp
				where id=? and status='PENDING'
				""", provider, providerTransactionId, evidence, actorId, Timestamp.from(now), paymentId);
			db.update("update membership_orders set status='PAID',paid_at=?,expires_at=null,updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'", Timestamp.from(now), uuid(row, "order_id"));
			return createArtifacts(row, paymentId, actorId, evidence, reason, now);
		} catch (DataIntegrityViolationException exception) {
			throw PaymentException.conflict("Payment or Order was already fulfilled");
		}
	}

	PaymentResultResponse createArtifacts(Map<String, Object> row, UUID paymentId, UUID actorId, String evidence, String reason, Instant now) {
		UUID memberId = uuid(row, "member_account_id");
		FulfillmentMode mode = fulfillmentMode(row);
		boolean immediate = mode == FulfillmentMode.IMMEDIATE;
		UUID membershipId = UUID.randomUUID();
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,
				offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				status,starts_at,ends_at)
			values(?,?,?,?,?,?,?,?,?,?,?,?)
			""", membershipId, memberId, uuid(row, "order_id"), uuid(row, "offer_id"), row.get("plan_code_snapshot"), row.get("offer_name_snapshot"), row.get("price_amount_snapshot"), row.get("currency_code_snapshot"), row.get("duration_days_snapshot"), immediate ? "ACTIVE" : "RESERVED", immediate ? Timestamp.from(now) : null, immediate ? Timestamp.from(now.plusSeconds(number(row, "duration_days_snapshot").longValue() * 86400)) : null);
		if (!immediate)
			lifecycle.reconcile(memberId, now);
		UUID receiptId = UUID.randomUUID();
		db.update("""
			insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,
				currency_code_snapshot,payment_method_snapshot)
			values(?,?,?,?,?,?,?,?)
			""", receiptId, "RC-" + receiptId, paymentId, uuid(row, "order_id"), memberId, row.get("payment_amount"), row.get("payment_currency"), row.get("payment_method"));
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "PAYMENT_PAID", "PAYMENT", paymentId, reason, Map.of("status", "PENDING"), Map.of("status", "PAID", "evidence", evidence == null ? "" : evidence)));
		notifications.write("PAYMENT_PAID:" + paymentId, memberId, "PAYMENT_PAID", "PAYMENT", paymentId, Map.of("orderId", uuid(row, "order_id").toString(), "status", "PAID"));
		return new PaymentResultResponse(paymentId, uuid(row, "order_id"), "PAID", string(row, "payment_method"), membershipId, receiptId, now, mode, string(row, "order_number"), string(row, "member_code"), string(row, "member_name"), string(row, "provider_reference"));
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return PaymentFulfillmentRepository.uuid(row, key);
	}

	static String string(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value == null ? null : value.toString().trim();
	}

	static Number number(Map<String, Object> row, String key) {
		return (Number) row.get(key);
	}

	static FulfillmentMode fulfillmentMode(Map<String, Object> row) {
		String value = string(row, "fulfillment_mode");
		return value == null ? FulfillmentMode.IMMEDIATE : FulfillmentMode.valueOf(value);
	}
}
