package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class PaymentServiceReconciliationQueueTests {

	private static final Instant NOW = Instant.parse("2026-10-04T12:00:00Z");
	private final UUID actor = UUID.randomUUID();
	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final PaymentService service = new PaymentService(db, accounts, mock(AuditEventRepository.class),
		mock(PaymentFulfillmentService.class), Clock.fixed(NOW, ZoneOffset.UTC));

	@BeforeEach
	void allowManager() {
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
	}

	@Test
	@SuppressWarnings("unchecked")
	void returnsPendingPaymentsWithStablePagination() throws Exception {
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		Instant createdAt = NOW.minusSeconds(120);
		Instant expiresAt = NOW.plusSeconds(3600);
		when(db.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(1L);

		ArgumentCaptor<RowMapper<ReconciliationQueueItem>> mapperCaptor = ArgumentCaptor.forClass(RowMapper.class);
		when(db.query(anyString(), mapperCaptor.capture(), any(Object[].class))).thenAnswer(invocation -> {
			ResultSet rs = mock(ResultSet.class);
			when(rs.getObject("payment_id", UUID.class)).thenReturn(paymentId);
			when(rs.getObject("order_id", UUID.class)).thenReturn(orderId);
			when(rs.getString("order_number")).thenReturn("ORD-001");
			when(rs.getObject("member_account_id", UUID.class)).thenReturn(memberId);
			when(rs.getString("member_name")).thenReturn("Nguyen Van A");
			when(rs.getBigDecimal("amount")).thenReturn(new java.math.BigDecimal("12000"));
			when(rs.getString("currency_code")).thenReturn("VND");
			when(rs.getString("transfer_content")).thenReturn("SCMS ABC123");
			when(rs.getTimestamp("created_at")).thenReturn(Timestamp.from(createdAt));
			when(rs.getTimestamp("expires_at")).thenReturn(Timestamp.from(expiresAt));
			return List.of(mapperCaptor.getValue().mapRow(rs, 0));
		});

		ReconciliationQueuePageResponse response = service.reconciliationQueue(actor,
			ReconciliationQueueStatus.PENDING, "ORD-001", 0, 20);

		assertThat(response.totalElements()).isEqualTo(1);
		assertThat(response.totalPages()).isEqualTo(1);
		assertThat(response.content()).containsExactly(new ReconciliationQueueItem(paymentId, orderId,
			"ORD-001", memberId, "Nguyen Van A", BigInteger.valueOf(12000), "VND",
			"SCMS ABC123", createdAt, expiresAt));
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		verify(db).query(sql.capture(), any(RowMapper.class), any(Object[].class));
		assertThat(sql.getValue())
			.contains("p.status='PENDING'", "o.status='PENDING_PAYMENT'")
			.contains("o.expires_at is null or o.expires_at > ?")
			.contains("order by p.created_at desc, p.id desc")
			.contains("limit ? offset ?");
	}
}
