package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PaymentServiceMembershipHistoryTests {

	@Test
	@SuppressWarnings("unchecked")
	void returnsTypedCamelCaseMembershipHistory() throws Exception {
		UUID actor = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Instant startsAt = Instant.parse("2026-10-05T01:00:00Z");
		Instant endsAt = Instant.parse("2026-11-04T01:00:00Z");
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		PaymentService service = new PaymentService(db, accounts, mock(AuditEventRepository.class),
			mock(PaymentFulfillmentService.class), Clock.systemUTC());
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MEMBER, AccountStatus.ACTIVE))
			.thenReturn(true);

		ArgumentCaptor<RowMapper<Object>> mapper = ArgumentCaptor.forClass(RowMapper.class);
		when(db.query(anyString(), mapper.capture(), any(Object[].class))).thenAnswer(invocation -> {
			ResultSet rs = mock(ResultSet.class);
			when(rs.getObject("id", UUID.class)).thenReturn(membershipId);
			when(rs.getObject("order_id", UUID.class)).thenReturn(orderId);
			when(rs.getString("plan_code_snapshot")).thenReturn("PLUS");
			when(rs.getString("offer_name_snapshot")).thenReturn("PLUS 1");
			when(rs.getBigDecimal("price_amount_snapshot")).thenReturn(new BigDecimal("500000"));
			when(rs.getString("currency_code_snapshot")).thenReturn("VND");
			when(rs.getInt("duration_days_snapshot")).thenReturn(30);
			when(rs.getString("status")).thenReturn("ACTIVE");
			when(rs.getTimestamp("starts_at")).thenReturn(Timestamp.from(startsAt));
			when(rs.getTimestamp("ends_at")).thenReturn(Timestamp.from(endsAt));
			return List.of(mapper.getValue().mapRow(rs, 0));
		});

		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		JsonNode history = objectMapper.valueToTree(service.memberHistory(actor));
		assertThat(history.size()).isEqualTo(1);
		JsonNode item = history.get(0);

		assertThat(item.get("id").asText()).isEqualTo(membershipId.toString());
		assertThat(item.get("orderId").asText()).isEqualTo(orderId.toString());
		assertThat(item.get("planCode").asText()).isEqualTo("PLUS");
		assertThat(item.get("offerName").asText()).isEqualTo("PLUS 1");
		assertThat(item.get("priceAmount").asText()).isEqualTo("500000");
		assertThat(item.get("currencyCode").asText()).isEqualTo("VND");
		assertThat(item.get("durationDays").asInt()).isEqualTo(30);
		assertThat(item.get("status").asText()).isEqualTo("ACTIVE");
		assertThat(item.get("startsAt").asText()).isEqualTo(startsAt.toString());
		assertThat(item.get("endsAt").asText()).isEqualTo(endsAt.toString());
		assertThat(item.has("order_id")).isFalse();
		assertThat(item.has("offer_name_snapshot")).isFalse();
	}
}
