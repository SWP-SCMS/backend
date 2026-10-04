package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentServiceOrderCancellationTests {

	@Test
	void cancelsPendingOrderAndItsPendingPayment() {
		Instant now = Instant.parse("2026-10-04T15:00:00Z");
		UUID actor = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(anyString(), eq(orderId))).thenReturn(Map.of(
			"id", orderId,
			"status", "PENDING_PAYMENT"
		));
		when(db.update(anyString(), any(Object[].class))).thenReturn(1);
		PaymentService service = new PaymentService(db, accounts, mock(AuditEventRepository.class),
			mock(PaymentFulfillmentService.class), Clock.fixed(now, ZoneOffset.UTC));

		MembershipOrderCancellationResponse response = service.cancelOrder(actor, orderId,
			new MembershipOrderCancellationRequest(" Customer requested cancellation "));

		assertThat(response).isEqualTo(new MembershipOrderCancellationResponse(orderId, "EXPIRED", 1,
			now, "Customer requested cancellation"));
	}
}
