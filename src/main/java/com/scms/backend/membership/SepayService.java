package com.scms.backend.membership;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
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
	private final String bankDestinationSha256;
	private final String webhookApiKey;

	@Autowired
	SepayService(JdbcTemplate db, AccountRepository accounts, PaymentFulfillmentService fulfillment, Clock clock,
			@Value("${sepay.bank-code:}") String bankCode,
			@Value("${sepay.bank-account:}") String bankAccount,
			@Value("${sepay.bank-account-name:}") String bankAccountName,
			@Value("${sepay.bank-destination-sha256:}") String bankDestinationSha256,
			@Value("${sepay.webhook-api-key:}") String webhookApiKey) {
		this.db = db;
		this.accounts = accounts;
		this.fulfillment = fulfillment;
		this.clock = clock;
		this.bankCode = bankCode.trim();
		this.bankAccount = bankAccount.trim();
		this.bankAccountName = bankAccountName.trim();
		this.bankDestinationSha256 = bankDestinationSha256.trim();
		this.webhookApiKey = webhookApiKey;
	}

	SepayService(JdbcTemplate db, AccountRepository accounts, PaymentFulfillmentService fulfillment, Clock clock,
			String bankCode, String bankAccount, String bankAccountName, String webhookApiKey) {
		this(db, accounts, fulfillment, clock, bankCode, bankAccount, bankAccountName, "", webhookApiKey);
	}

	@Transactional
	SepayPaymentResponse create(UUID actor, UUID orderId) {
		if (!accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		return createPayment(actor, actor, orderId);
	}

	@Transactional
	SepayPaymentResponse createForReceptionist(UUID actor, String memberCode, UUID orderId) {
		if (!accounts.existsByIdAndRoleAndStatus(actor, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		UUID memberId;
		try {
			memberId = db.queryForObject("""
				select a.id from accounts a join member_profiles p on p.account_id=a.id
				where p.member_code=? and a.role='MEMBER' and a.status='ACTIVE'
				""", UUID.class, memberCode == null ? null : memberCode.trim());
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Active Member was not found");
		}
		return createPayment(actor, memberId, orderId);
	}

	private SepayPaymentResponse createPayment(UUID actor, UUID memberId, UUID orderId) {
		Map<String, Object> order;
		try {
			order = db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
				orderId, memberId);
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
			if (missingSnapshot(payment, "bank_code_snapshot")
					|| missingSnapshot(payment, "bank_account_number_snapshot")
					|| missingSnapshot(payment, "bank_account_name_snapshot"))
				throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_DESTINATION_SNAPSHOT_MISSING",
					"Payment destination snapshot is missing");
		}
		return response(order, payment, expiresAt);
	}

	@Transactional
	SepayWebhookResponse webhook(String suppliedApiKey, SepayWebhookRequest request) {
		if (webhookApiKey.isBlank() || suppliedApiKey == null || !MessageDigest.isEqual(
				suppliedApiKey.getBytes(StandardCharsets.UTF_8), webhookApiKey.getBytes(StandardCharsets.UTF_8))) {
			throw PaymentException.unauthorized("Invalid SePay webhook credentials");
		}
		if (request == null || request.id() == null || request.content() == null || request.content().isBlank()
				|| request.transferAmount() == null || request.transferType() == null || request.transferType().isBlank())
			throw PaymentException.validation("SePay webhook body is malformed");
		String paymentReference = request.code() == null || request.code().isBlank()
			? request.content().trim() : request.code().trim();
		String normalizedPaymentReference = normalizeReference(paymentReference);
		UUID paymentId = null;
		try {
			paymentId = db.queryForObject("select id from payments where provider='SEPAY' and provider_reference=?",
				UUID.class, paymentReference);
		} catch (EmptyResultDataAccessException exception) {
			try {
				paymentId = db.queryForObject(
					"select id from payments where provider='SEPAY' and replace(provider_reference, '-', '')=?",
					UUID.class, normalizedPaymentReference);
			} catch (EmptyResultDataAccessException ignored) { }
		}
		return fulfillment.processSepayWebhook(paymentId, request, normalizedPaymentReference, payloadSha256(request));
	}

	private static String normalizeReference(String value) {
		return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
	}
	private static String payloadSha256(SepayWebhookRequest request) {
		String canonical = String.join("|", request.id().toString(), normalizeReference(request.code()),
			normalizeReference(request.content()), request.transferAmount().toString(),
			request.transferType().trim().toUpperCase(Locale.ROOT), normalized(request.referenceCode()),
			normalized(request.transactionDate()), normalized(request.accountNumber()));
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(canonical.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
	private static String normalized(String value) {
		return value == null ? "" : value.trim();
	}
	private static boolean missingSnapshot(Map<String, Object> payment, String key) {
		Object value = payment.get(key);
		return value == null || value.toString().isBlank();
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
		byte[] expected;
		try {
			expected = HexFormat.of().parseHex(bankDestinationSha256);
		} catch (IllegalArgumentException exception) {
			throw invalidBankDestinationConfiguration();
		}
		try {
			String canonical = String.join("|", bankCode, bankAccount, bankAccountName).toUpperCase(Locale.ROOT);
			byte[] actual = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
			if (!MessageDigest.isEqual(actual, expected)) throw invalidBankDestinationConfiguration();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
	private PaymentException invalidBankDestinationConfiguration() {
		return new PaymentException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_DESTINATION_CONFIGURATION_INVALID",
			"SePay bank destination configuration is invalid");
	}
	private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
	private Instant instant(Object value) {
		if (value == null) return null;
		return value instanceof Timestamp timestamp ? timestamp.toInstant() : (Instant) value;
	}
}
