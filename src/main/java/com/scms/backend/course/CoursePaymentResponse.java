package com.scms.backend.course;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CoursePaymentResponse(
	@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID paymentId,
	@Schema(format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED) UUID orderId,
	@Schema(minimum = "1", maximum = "9999999999999999999", requiredMode = Schema.RequiredMode.REQUIRED)
	BigInteger amount,
	@Schema(allowableValues = "VND", requiredMode = Schema.RequiredMode.REQUIRED) String currencyCode,
	@Schema(allowableValues = { "SEPAY", "CASH" }, requiredMode = Schema.RequiredMode.REQUIRED) String method,
	@Schema(type = "string", nullable = true, allowableValues = "SEPAY",
		requiredMode = Schema.RequiredMode.REQUIRED) String provider,
	@Schema(pattern = "^COR-[0-9A-F]{32}$", requiredMode = Schema.RequiredMode.REQUIRED) String paymentReference,
	@Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String bankCode,
	@Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String bankAccountNumber,
	@Schema(nullable = true, requiredMode = Schema.RequiredMode.REQUIRED) String bankAccountName,
	@Schema(nullable = true, format = "uri", requiredMode = Schema.RequiredMode.REQUIRED) String qrUrl,
	@Schema(format = "date-time", requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
	@Schema(allowableValues = { "PENDING", "PAID", "FAILED" }, requiredMode = Schema.RequiredMode.REQUIRED)
	String status) {

	public static String qrUrl(String bankCode, String account, String accountName, BigInteger amount,
			String reference) {
		if (bankCode == null || account == null || accountName == null) return null;
		return "https://qr.sepay.vn/img?bank=" + enc(bankCode) + "&acc=" + enc(account)
			+ "&template=compact&amount=" + amount + "&des=" + enc(reference)
			+ "&accountName=" + enc(accountName);
	}

	private static String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
