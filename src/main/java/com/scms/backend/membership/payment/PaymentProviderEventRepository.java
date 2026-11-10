package com.scms.backend.membership.payment;

import com.scms.backend.membership.SepayWebhookOutcome;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PaymentProviderEventRepository {

	private final JdbcTemplate db;

	PaymentProviderEventRepository(JdbcTemplate db) {
		this.db = db;
	}

	boolean insertFinalEvent(UUID paymentId, SepayWebhookRequest request, String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome, String detail) {
		return insertFinalEvent(paymentId, null, request, normalizedReference, payloadSha256, outcome, detail);
	}

	boolean insertFinalEvent(UUID paymentId, UUID coursePaymentId, SepayWebhookRequest request, String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome, String detail) {
		return db.update("""
			insert into payment_provider_events(id,provider,provider_transaction_id,payment_id,course_payment_id,
				transfer_amount,payment_reference,account_number_last4,payload_sha256,outcome,detail)
			values(?,'SEPAY',?,?,?,?,?,?,?,?,?)
			on conflict (provider,provider_transaction_id) do nothing
			""", UUID.randomUUID(), request.id().toString(), paymentId, coursePaymentId, request.transferAmount(), normalizedReference, last4(request.accountNumber()), payloadSha256, outcome.name(), detail) == 1;
	}

	Map<String, Object> event(String transactionId) {
		List<Map<String, Object>> events = db.queryForList("""
			select id,payment_id,course_payment_id,payload_sha256,outcome from payment_provider_events
			where provider='SEPAY' and provider_transaction_id=?
			""", transactionId);
		return events.isEmpty() ? null : events.getFirst();
	}

	void lockProviderTransaction(String provider, String transactionId) {
		db.queryForObject("""
			select true from (select pg_advisory_xact_lock(hashtextextended(?, 0))) locked
			""", Boolean.class, provider + '\u001f' + transactionId);
	}

	boolean providerTransactionClaimed(String provider, String transactionId, UUID paymentId, UUID coursePaymentId) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists (
				select 1 from payment_provider_events
				where provider=? and provider_transaction_id=?
					and (payment_id is distinct from ?::uuid or course_payment_id is distinct from ?::uuid)
				union all
				select 1 from payments
				where provider=? and provider_transaction_id=? and id is distinct from ?::uuid
				union all
				select 1 from course_payments
				where provider=? and provider_transaction_id=? and id is distinct from ?::uuid
			)
			""", Boolean.class, provider, transactionId, paymentId, coursePaymentId, provider, transactionId, paymentId, provider, transactionId, coursePaymentId));
	}

	static String last4(String value) {
		if (value == null || value.isBlank())
			return null;
		String normalized = value.trim();
		return normalized.substring(Math.max(0, normalized.length() - 4));
	}
}
