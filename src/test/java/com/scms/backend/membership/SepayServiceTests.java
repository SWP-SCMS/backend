package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class SepayServiceTests {
	@Mock JdbcTemplate db;
	@Mock AccountRepository accounts;
	@Mock PaymentFulfillmentService fulfillment;

	@Test
	void responseAndQrPreserveExactPersistedTransferContent() {
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Instant expiresAt = Instant.parse("2026-10-09T00:00:00Z");
		String persistedContent = "  SCMS-EXACT CONTENT  ";
		Map<String, Object> order = Map.of(
			"status", "PENDING_PAYMENT", "order_number", "ORD-123", "expires_at", Timestamp.from(expiresAt));
		Map<String, Object> payment = Map.of(
			"id", paymentId, "order_id", orderId, "amount", BigDecimal.valueOf(12000), "currency_code", "VND",
			"bank_transfer_content", persistedContent, "status", "PENDING");
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap("select * from membership_orders where id=? and member_account_id=? for update",
			orderId, actor)).thenReturn(order);
		when(db.queryForList(
			"select * from payments where order_id=? and status='PENDING' order by created_at limit 1", orderId))
			.thenReturn(List.of(payment));
		SepayService service = new SepayService(db, accounts, fulfillment,
			Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC),
			"MB", "123456789", "SCMS GYM", "hook-secret");

		SepayPaymentResponse response = service.create(actor, orderId);
		String encodedDescription = Arrays.stream(URI.create(response.qrUrl()).getRawQuery().split("&"))
			.filter(parameter -> parameter.startsWith("des="))
			.findFirst().orElseThrow().substring(4);

		assertThat(response.paymentReference()).isEqualTo(persistedContent);
		assertThat(response.transferContent()).isEqualTo(persistedContent);
		assertThat(URLDecoder.decode(encodedDescription, StandardCharsets.UTF_8)).isEqualTo(persistedContent);
	}
}
