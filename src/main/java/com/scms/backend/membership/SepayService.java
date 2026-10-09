package com.scms.backend.membership;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SepayService {
	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final PaymentFulfillmentService fulfillment;
	private final Clock clock;
	private final String bankCode;
	private final String bankAccount;
	private final String bankAccountName;
	private final String webhookApiKey;

	SepayService(JdbcTemplate db, AccountRepository accounts, PaymentFulfillmentService fulfillment, Clock clock,
			@Value("${sepay.bank-code:}") String bankCode,
			@Value("${sepay.bank-account:}") String bankAccount,
			@Value("${sepay.bank-account-name:}") String bankAccountName,
			@Value("${sepay.webhook-api-key:}") String webhookApiKey) {
		this.db = db;
		this.accounts = accounts;
		this.fulfillment = fulfillment;
		this.clock = clock;
		this.bankCode = bankCode.trim();
		this.bankAccount = bankAccount.trim();
		this.bankAccountName = bankAccountName.trim();
		this.webhookApiKey = webhookApiKey;
	}

	@Transactional
	SepayPaymentResponse create(UUID actor, UUID orderId) {
		if (!accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		Map<String, Object> order;
		try {
			order = db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
				orderId, actor);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
		if (!"PENDING_PAYMENT".equals(order.get("status"))) {
			throw PaymentException.conflict("Order is not payable");
		}

		Map<String, Object> payment = pendingPayment(orderId);
		Instant expiresAt;
		if (payment == null) {
			requireBankConfiguration();
			expiresAt = clock.instant().plus(24, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
			db.update("update membership_orders set expires_at=?,payment_method='BANK_TRANSFER',updated_at=current_timestamp where id=?",
				Timestamp.from(expiresAt), orderId);
			String reference = reference(order);
			UUID paymentId = UUID.randomUUID();
			db.update("""
				insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,
					bank_code_snapshot,bank_account_number_snapshot,bank_account_name_snapshot,
					provider,provider_reference)
				values(?,?,'BANK_TRANSFER','PENDING',?,'VND',?,?,?,?,'SEPAY',?)
				""", paymentId, orderId, order.get("price_amount_snapshot"), reference,
				bankCode, bankAccount, bankAccountName, reference);
			payment = db.queryForMap("select * from payments where id=?", paymentId);
		} else {
			expiresAt = instant(order.get("expires_at"));
			if (payment.get("bank_code_snapshot") == null
					&& payment.get("bank_account_number_snapshot") == null
					&& payment.get("bank_account_name_snapshot") == null) {
				requireBankConfiguration();
				int updated = db.update("""
					update payments set bank_code_snapshot=?,bank_account_number_snapshot=?,
						bank_account_name_snapshot=?,updated_at=current_timestamp
					where id=? and status='PENDING' and bank_code_snapshot is null
						and bank_account_number_snapshot is null and bank_account_name_snapshot is null
					""", bankCode, bankAccount, bankAccountName, payment.get("id"));
				if (updated != 1) throw PaymentException.conflict("Payment destination snapshot changed");
				payment.put("bank_code_snapshot", bankCode);
				payment.put("bank_account_number_snapshot", bankAccount);
				payment.put("bank_account_name_snapshot", bankAccountName);
			}
		}
		return response(order, payment, expiresAt);
	}

	@Transactional
	PaymentResultResponse webhook(String suppliedApiKey, SepayWebhookRequest request) {
		if (webhookApiKey.isBlank() || suppliedApiKey == null || !MessageDigest.isEqual(
				suppliedApiKey.getBytes(StandardCharsets.UTF_8), webhookApiKey.getBytes(StandardCharsets.UTF_8))) {
			throw PaymentException.unauthorized("Invalid SePay webhook credentials");
		}
		if (request == null) throw PaymentException.validation("SePay webhook body is required");
		if (!"in".equalsIgnoreCase(request.transferType())) {
			throw PaymentException.validation("SePay transfer must be incoming");
		}
		UUID paymentId;
		try {
			paymentId = db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
				UUID.class, request.code().trim());
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("SePay Payment was not found");
		}
		Map<String, Object> payment = fulfillment.paymentForUpdate(paymentId);
		String expectedReference = PaymentFulfillmentService.string(payment, "bank_transfer_content");
		if (!request.code().trim().equals(expectedReference)
				|| request.content() == null || !request.content().trim().contains(expectedReference)) {
			throw PaymentException.validation("SePay transfer content does not match Payment");
		}
		if (!PaymentFulfillmentService.amount(payment, "payment_amount").equals(request.transferAmount())) {
			throw PaymentException.validation("SePay transfer amount does not match Payment");
		}
		String expectedAccount = PaymentFulfillmentService.string(payment, "bank_account_number_snapshot");
		if (expectedAccount == null) expectedAccount = bankAccount;
		if (request.accountNumber() == null || !expectedAccount.equals(request.accountNumber().trim())) {
			throw PaymentException.validation("SePay bank account does not match configured account");
		}
		String providerTransactionId = String.valueOf(request.id());
		return fulfillment.fulfill(paymentId, null, providerTransactionId,
			"SePay webhook transaction " + providerTransactionId, "SePay incoming transfer confirmed");
	}

	private Map<String, Object> pendingPayment(UUID orderId) {
		var rows = db.queryForList("select * from payments where order_id=? and status='PENDING' order by created_at limit 1",
			orderId);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	private SepayPaymentResponse response(Map<String, Object> order, Map<String, Object> payment, Instant expiresAt) {
		String reference = (String) payment.get("bank_transfer_content");
		String snapshotBankCode = (String) payment.get("bank_code_snapshot");
		String snapshotBankAccount = (String) payment.get("bank_account_number_snapshot");
		String snapshotBankAccountName = (String) payment.get("bank_account_name_snapshot");
		String amount = new BigDecimal(payment.get("amount").toString()).toBigIntegerExact().toString();
		String qr = "https://qr.sepay.vn/img?bank=" + enc(snapshotBankCode) + "&acc=" + enc(snapshotBankAccount)
			+ "&template=compact&amount=" + amount + "&des=" + enc(reference)
			+ "&accountName=" + enc(snapshotBankAccountName);
		return new SepayPaymentResponse((UUID) payment.get("id"), (UUID) payment.get("order_id"),
			String.valueOf(order.get("order_number")), new BigDecimal(amount).toBigIntegerExact(),
			PaymentFulfillmentService.string(payment, "currency_code"), "SEPAY", reference, reference, snapshotBankCode,
			snapshotBankAccount, snapshotBankAccountName, qr, expiresAt, PaymentFulfillmentService.string(payment, "status"));
	}

	private String reference(Map<String, Object> order) { return "SCMS-" + order.get("order_number"); }
	private void requireBankConfiguration() {
		if (bankCode.isBlank() || bankAccount.isBlank() || bankAccountName.isBlank())
			throw PaymentException.validation("SePay bank configuration is missing");
	}
	private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
	private Instant instant(Object value) {
		if (value == null) return null;
		return value instanceof Timestamp timestamp ? timestamp.toInstant() : (Instant) value;
	}
}
