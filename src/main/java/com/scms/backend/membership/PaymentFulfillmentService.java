package com.scms.backend.membership;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentFulfillmentService {
	private static final Logger log = LoggerFactory.getLogger(PaymentFulfillmentService.class);
	private final JdbcTemplate db;
	private final MembershipLifecycleService lifecycle;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	PaymentFulfillmentService(JdbcTemplate db, MembershipLifecycleService lifecycle, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.db = db; this.lifecycle = lifecycle; this.audits = audits; this.notifications = notifications; this.clock = clock;
	}

	@Transactional
	PaymentResultResponse fulfill(UUID paymentId, UUID actorId, String providerTransactionId,
			String evidence, String reason) {
		lifecycle.reconcile(memberIdForPayment(paymentId), clock.instant());
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
		if (providerTransactionId == null || providerTransactionId.isBlank()) {
			throw PaymentException.validation("providerTransactionId is required");
		}
		String provider = string(row, "provider");
		if (provider == null) provider = "SEPAY";
		Integer duplicate = db.queryForObject(
			"select count(*) from payments where provider=? and provider_transaction_id=? and id<>?",
			Integer.class, provider, providerTransactionId, paymentId);
		if (duplicate != null && duplicate > 0) {
			throw PaymentException.conflict("Provider transaction was already used");
		}
		ensureMembershipCanFulfill(row);
		return fulfillLocked(row, paymentId, actorId, provider, providerTransactionId, evidence, reason, clock.instant());
	}

	@Transactional
	SepayWebhookResponse processSepayWebhook(UUID paymentId, SepayWebhookRequest request,
			String normalizedReference, String payloadSha256) {
		if (paymentId == null) return finishEvent(null, request, normalizedReference, payloadSha256,
			SepayWebhookOutcome.REFERENCE_MISMATCH, "Payment reference was not found", false);

		lifecycle.reconcile(memberIdForPayment(paymentId), clock.instant());
		Map<String, Object> row = paymentForUpdate(paymentId);
		String transactionId = request.id().toString();
		if ("PAID".equals(string(row, "payment_status"))
				&& transactionId.equals(string(row, "provider_transaction_id"))) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.ALREADY_PROCESSED, "Provider transaction was already applied", true);
		}
		if (!"PENDING_PAYMENT".equals(string(row, "order_status"))
				|| !"PENDING".equals(string(row, "payment_status"))
				|| !"in".equalsIgnoreCase(request.transferType())) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.PAYMENT_NOT_PAYABLE, "Payment or Order is not payable", false);
		}
		if (blank(row, "bank_code_snapshot") || blank(row, "bank_account_number_snapshot")
				|| blank(row, "bank_account_name_snapshot")) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.DESTINATION_SNAPSHOT_MISSING,
				"Payment destination snapshot is missing", false);
		}
		String expectedReference = normalizeReference(string(row, "bank_transfer_content"));
		if (expectedReference.isBlank() || normalizedReference.isBlank()
				|| !normalizedReference.equals(expectedReference)
				|| !normalizeReference(request.content()).contains(expectedReference)) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.REFERENCE_MISMATCH, "Transfer reference or content does not match", false);
		}
		if (!"VND".equalsIgnoreCase(string(row, "payment_currency"))
				|| !amount(row, "payment_amount").equals(request.transferAmount())) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.AMOUNT_MISMATCH, "Transfer amount or currency does not match", false);
		}
		if (request.accountNumber() == null
				|| !string(row, "bank_account_number_snapshot").equals(request.accountNumber().trim())) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.ACCOUNT_MISMATCH, "Destination account does not match", false);
		}
		Instant now = clock.instant();
		Instant expiresAt = instant(row, "expires_at");
		if (expiresAt == null || !now.isBefore(expiresAt)) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED, "Payment window is missing or expired", false);
		}
		try {
			ensureMembershipCanFulfill(row);
		} catch (PaymentException exception) {
			return finishEvent(row, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.PAYMENT_NOT_PAYABLE, "Membership cannot be fulfilled", false);
		}
		if (!insertFinalEvent(paymentId, request, normalizedReference, payloadSha256,
				SepayWebhookOutcome.FULFILLED, "Payment fulfilled automatically")) {
			return replayEvent(paymentId, request, payloadSha256);
		}
		PaymentResultResponse result = fulfillLocked(row, paymentId, null, "SEPAY", transactionId,
			"SePay webhook transaction " + transactionId, "SePay incoming transfer confirmed", now);
		return new SepayWebhookResponse(true, true, SepayWebhookOutcome.FULFILLED,
			result.paymentId(), result.orderId(), result.status());
	}

	@Transactional
	PaymentResultResponse fulfillCash(UUID paymentId, UUID orderId, UUID actorId, String evidence, String reason) {
		lifecycle.reconcile(memberIdForOrder(orderId), clock.instant());
		Map<String, Object> row;
		try {
			row = db.queryForMap("""
				select ?::uuid payment_id,o.id order_id,'CASH' payment_method,'PENDING' payment_status,
					o.order_number,a.full_name member_name,m.member_code,null::text provider_reference,
					o.price_amount_snapshot payment_amount,o.currency_code_snapshot payment_currency,
					o.status order_status,o.fulfillment_mode,o.member_account_id,o.offer_id,o.plan_code_snapshot,
					o.offer_name_snapshot,o.price_amount_snapshot,o.currency_code_snapshot,
					o.duration_days_snapshot
				from membership_orders o join accounts a on a.id=o.member_account_id
				join member_profiles m on m.account_id=a.id where o.id=? for update of o
				""", paymentId, orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
		if (!"PENDING_PAYMENT".equals(string(row, "order_status"))) throw PaymentException.conflict("Order is not payable");
		ensureMembershipCanFulfill(row);
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

	private UUID memberIdForPayment(UUID paymentId) {
		try {
			return db.queryForObject(
				"select o.member_account_id from payments p join membership_orders o on o.id=p.order_id where p.id=?",
				UUID.class, paymentId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Payment was not found");
		}
	}

	private UUID memberIdForOrder(UUID orderId) {
		try {
			return db.queryForObject("select member_account_id from membership_orders where id=?", UUID.class, orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
	}

	private void ensureMembershipCanFulfill(Map<String, Object> row) {
		Integer active = db.queryForObject("select count(*) from memberships where member_account_id=? and status='ACTIVE'",
			Integer.class, uuid(row, "member_account_id"));
		if (fulfillmentMode(row) == FulfillmentMode.IMMEDIATE && active != null && active > 0) {
			throw PaymentException.conflict("Member already has an active membership");
		}
	}

	private PaymentResultResponse fulfillLocked(Map<String, Object> row, UUID paymentId, UUID actorId,
			String provider, String providerTransactionId, String evidence, String reason, Instant now) {
		try {
			db.update("""
				update payments set status='PAID', provider=?, provider_transaction_id=?, evidence=?,
					processed_by_account_id=?, paid_at=?, updated_at=current_timestamp
				where id=? and status='PENDING'
				""", provider, providerTransactionId, evidence, actorId, Timestamp.from(now), paymentId);
			db.update("update membership_orders set status='PAID',paid_at=?,expires_at=null,updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'",
				Timestamp.from(now), uuid(row, "order_id"));
			return createArtifacts(row, paymentId, actorId, evidence, reason, now);
		} catch (DataIntegrityViolationException exception) {
			throw PaymentException.conflict("Payment or Order was already fulfilled");
		}
	}

	private SepayWebhookResponse finishEvent(Map<String, Object> row, SepayWebhookRequest request,
			String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome,
			String detail, boolean processed) {
		UUID paymentId = row == null ? null : uuid(row, "payment_id");
		if (!insertFinalEvent(paymentId, request, normalizedReference, payloadSha256, outcome, detail)) {
			return replayEvent(paymentId, request, payloadSha256);
		}
		return webhookResponse(row, processed, outcome);
	}

	private boolean insertFinalEvent(UUID paymentId, SepayWebhookRequest request, String normalizedReference,
			String payloadSha256, SepayWebhookOutcome outcome, String detail) {
		return db.update("""
			insert into payment_provider_events(id,provider,provider_transaction_id,payment_id,transfer_amount,
				payment_reference,account_number_last4,payload_sha256,outcome,detail)
			values(?,'SEPAY',?,?,?,?,?,?,?,?)
			on conflict (provider,provider_transaction_id) do nothing
			""", UUID.randomUUID(), request.id().toString(), paymentId, request.transferAmount(),
			normalizedReference, last4(request.accountNumber()), payloadSha256, outcome.name(), detail) == 1;
	}

	private SepayWebhookResponse replayEvent(UUID requestedPaymentId, SepayWebhookRequest request,
			String payloadSha256) {
		Map<String, Object> event = db.queryForMap("""
			select id,payment_id,payload_sha256,outcome from payment_provider_events
			where provider='SEPAY' and provider_transaction_id=?
			""", request.id().toString());
		UUID existingPaymentId = uuid(event, "payment_id");
		boolean samePayload = payloadSha256.equals(string(event, "payload_sha256"));
		boolean samePayment = Objects.equals(existingPaymentId, requestedPaymentId);
		if (!samePayload) {
			log.warn("Rejected duplicate SePay transaction with changed canonical payload; transactionId={}, eventId={}",
				request.id(), event.get("id"));
		}
		Map<String, Object> row = existingPaymentId == null ? null : payment(existingPaymentId);
		String storedOutcome = string(event, "outcome");
		boolean processed = samePayload && samePayment && row != null && "PAID".equals(string(row, "payment_status"))
			&& request.id().toString().equals(string(row, "provider_transaction_id"))
			&& (SepayWebhookOutcome.FULFILLED.name().equals(storedOutcome)
				|| SepayWebhookOutcome.ALREADY_PROCESSED.name().equals(storedOutcome));
		return webhookResponse(row, processed, SepayWebhookOutcome.ALREADY_PROCESSED);
	}

	private SepayWebhookResponse webhookResponse(Map<String, Object> row, boolean processed,
			SepayWebhookOutcome outcome) {
		return new SepayWebhookResponse(true, processed, outcome, row == null ? null : uuid(row, "payment_id"),
			row == null ? null : uuid(row, "order_id"), row == null ? null : string(row, "payment_status"));
	}

	private static boolean blank(Map<String, Object> row, String key) {
		return string(row, key) == null || string(row, key).isBlank();
	}
	private static String normalizeReference(String value) {
		return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
	}
	private static String last4(String value) {
		if (value == null || value.isBlank()) return null;
		String normalized = value.trim();
		return normalized.substring(Math.max(0, normalized.length() - 4));
	}

	private PaymentResultResponse createArtifacts(Map<String, Object> row, UUID paymentId, UUID actorId,
			String evidence, String reason, Instant now) {
		UUID memberId = uuid(row, "member_account_id");
		FulfillmentMode mode = fulfillmentMode(row);
		boolean immediate = mode == FulfillmentMode.IMMEDIATE;
		UUID membershipId = UUID.randomUUID();
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,
				offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				status,starts_at,ends_at)
			values(?,?,?,?,?,?,?,?,?,?,?,?)
			""", membershipId, memberId, uuid(row, "order_id"), uuid(row, "offer_id"),
			row.get("plan_code_snapshot"), row.get("offer_name_snapshot"), row.get("price_amount_snapshot"),
			row.get("currency_code_snapshot"), row.get("duration_days_snapshot"), immediate ? "ACTIVE" : "RESERVED",
			immediate ? Timestamp.from(now) : null,
			immediate ? Timestamp.from(now.plusSeconds(number(row, "duration_days_snapshot").longValue() * 86400)) : null);
		if (!immediate) lifecycle.reconcile(memberId, now);
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
			string(row, "payment_method"), membershipId, receiptId, now, mode,
			string(row, "order_number"), string(row, "member_code"), string(row, "member_name"),
			string(row, "provider_reference"));
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

	PaymentResultResponse result(Map<String, Object> row) {
		UUID orderId = uuid(row, "order_id");
		Map<String, Object> fulfillment;
		try {
			fulfillment = db.queryForMap("""
				select m.id membership_id,r.id receipt_id,o.fulfillment_mode from memberships m
				join receipts r on r.order_id=m.order_id join membership_orders o on o.id=m.order_id
				where m.order_id=?
				""", orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.conflict("Paid Payment has incomplete fulfillment data");
		}
		return new PaymentResultResponse(uuid(row, "payment_id"), orderId, "PAID", string(row, "payment_method"),
			uuid(fulfillment, "membership_id"), uuid(fulfillment, "receipt_id"), instant(row, "paid_at"),
			fulfillmentMode(fulfillment), string(row, "order_number"), string(row, "member_code"),
			string(row, "member_name"), string(row, "provider_reference"));
	}

	static UUID uuid(Map<String, Object> row, String key) { return (UUID) row.get(key); }
	static String string(Map<String, Object> row, String key) { Object value=row.get(key); return value == null ? null : value.toString().trim(); }
	static Number number(Map<String, Object> row, String key) { return (Number) row.get(key); }
	static FulfillmentMode fulfillmentMode(Map<String, Object> row) {
		String value = string(row, "fulfillment_mode");
		return value == null ? FulfillmentMode.IMMEDIATE : FulfillmentMode.valueOf(value);
	}
	static BigInteger amount(Map<String, Object> row, String key) { return new BigDecimal(row.get(key).toString()).toBigIntegerExact(); }
	static Instant instant(Map<String, Object> row, String key) {
		Object value=row.get(key); return value instanceof Timestamp timestamp ? timestamp.toInstant() : (Instant) value;
	}
}
