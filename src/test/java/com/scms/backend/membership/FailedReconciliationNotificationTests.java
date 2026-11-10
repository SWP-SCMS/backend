package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.membership.payment.PaymentActionRequest;
import com.scms.backend.membership.payment.PaymentFulfillmentService;
import com.scms.backend.membership.payment.PaymentService;
import com.scms.backend.notification.NotificationWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FailedReconciliationNotificationTests {

	@Test
	void failedDecisionNotifiesMemberAndTerminalRetryDoesNotNotifyAgain() {
		UUID actor = UUID.randomUUID(), paymentId = UUID.randomUUID();
		var accounts = mock(AccountRepository.class);
		var fulfillment = mock(PaymentFulfillmentService.class);
		var now = Instant.parse("2026-11-05T00:00:00Z");
		var payment = new HashMap<String, Object>();
		payment.put("order_id", UUID.randomUUID());
		payment.put("payment_status", "PENDING");
		payment.put("expires_at", now.minusSeconds(1));
		when(accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(fulfillment.payment(paymentId)).thenReturn(payment);
		when(fulfillment.paymentForUpdate(paymentId)).thenReturn(payment);
		var service = new PaymentService(mock(JdbcTemplate.class), accounts, mock(AuditEventRepository.class), fulfillment, mock(MemberProfileRepository.class), mock(MembershipLifecycleService.class), Clock.fixed(now, ZoneOffset.UTC));
		var request = new PaymentActionRequest(PaymentActionRequest.ReconciliationStatus.FAILED, null, null, null, "bank statement", "not received");
		assertThat(service.reconcile(actor, paymentId, request).status()).isEqualTo("FAILED");
		payment.put("payment_status", "FAILED");
		assertThatThrownBy(() -> service.reconcile(actor, paymentId, request)).isInstanceOf(PaymentException.class);
		verify(fulfillment, times(1)).notifyFailedReconciliation(paymentId, payment);
	}

	@Test
	void notificationUsesStableKeyAndMemberRecipientWithoutStaffEvidence() {
		var writer = mock(NotificationWriter.class);
		var service = new PaymentFulfillmentService(mock(JdbcTemplate.class), mock(MembershipLifecycleService.class), mock(AuditEventRepository.class), writer, Clock.systemUTC());
		UUID payment = UUID.randomUUID(), member = UUID.randomUUID(), order = UUID.randomUUID();
		service.notifyFailedReconciliation(payment, Map.of("member_account_id", member, "order_id", order));
		verify(writer).write("PAYMENT_FAILED:" + payment, member, "PAYMENT_FAILED", "PAYMENT", payment, Map.of("orderId", order.toString(), "status", "FAILED"));
	}
}
