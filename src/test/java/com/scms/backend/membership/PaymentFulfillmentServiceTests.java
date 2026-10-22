package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentFulfillmentServiceTests {

	@Test
	void paymentForUpdateLocksOrderThenPaymentBeforeReadingProjection() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		Map<String, Object> projection = Map.of(
			"payment_id", paymentId,
			"order_id", orderId,
			"payment_status", "PENDING",
			"order_status", "PENDING_PAYMENT");
		when(db.queryForObject("select order_id from payments where id=?", UUID.class, paymentId))
			.thenReturn(orderId);
		when(db.queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId))
			.thenReturn(orderId);
		when(db.queryForObject("select id from payments where id=? for update", UUID.class, paymentId))
			.thenReturn(paymentId);
		when(db.queryForMap(anyString(), eq(paymentId))).thenReturn(projection);
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, mock(MembershipLifecycleService.class), mock(AuditEventRepository.class),
			mock(NotificationWriter.class), Clock.systemUTC());

		assertThat(service.paymentForUpdate(paymentId)).isSameAs(projection);

		InOrder order = inOrder(db);
		order.verify(db).queryForObject("select order_id from payments where id=?", UUID.class, paymentId);
		order.verify(db).queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId);
		order.verify(db).queryForObject("select id from payments where id=? for update", UUID.class, paymentId);
		ArgumentCaptor<String> projectionSql = ArgumentCaptor.forClass(String.class);
		order.verify(db).queryForMap(projectionSql.capture(), eq(paymentId));
		assertThat(projectionSql.getValue()).contains("from payments p join membership_orders o")
			.doesNotContain("for update");
		verifyNoMoreInteractions(db);
	}

	@Test
	void paymentForUpdatePreservesPaymentNotFoundBehavior() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		UUID paymentId = UUID.randomUUID();
		when(db.queryForObject("select order_id from payments where id=?", UUID.class, paymentId))
			.thenThrow(new EmptyResultDataAccessException(1));
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, mock(MembershipLifecycleService.class), mock(AuditEventRepository.class),
			mock(NotificationWriter.class), Clock.systemUTC());

		assertThatThrownBy(() -> service.paymentForUpdate(paymentId))
			.isInstanceOf(PaymentException.class)
			.hasMessage("Payment was not found");
	}

	@Test
	void fulfillLocksMemberBeforeOrderAndPayment() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		when(db.queryForObject(
			"select o.member_account_id from payments p join membership_orders o on o.id=p.order_id where p.id=?",
			UUID.class, paymentId)).thenReturn(memberId);
		when(db.queryForObject("select order_id from payments where id=?", UUID.class, paymentId)).thenReturn(orderId);
		when(db.queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId))
			.thenReturn(orderId);
		when(db.queryForObject("select id from payments where id=? for update", UUID.class, paymentId))
			.thenReturn(paymentId);
		when(db.queryForMap(anyString(), eq(paymentId))).thenReturn(Map.of(
			"payment_status", "FAILED", "order_id", orderId, "member_account_id", memberId));
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, lifecycle,
			mock(AuditEventRepository.class), mock(NotificationWriter.class), Clock.systemUTC());

		assertThatThrownBy(() -> service.fulfill(paymentId, null, "TX-1", "evidence", "reason"))
			.isInstanceOf(PaymentException.class).hasMessage("Payment is terminal");

		InOrder order = inOrder(db, lifecycle);
		order.verify(db).queryForObject(
			"select o.member_account_id from payments p join membership_orders o on o.id=p.order_id where p.id=?",
			UUID.class, paymentId);
		order.verify(lifecycle).reconcile(eq(memberId), org.mockito.ArgumentMatchers.any());
		order.verify(db).queryForObject("select order_id from payments where id=?", UUID.class, paymentId);
		order.verify(db).queryForObject("select id from membership_orders where id=? for update", UUID.class, orderId);
		order.verify(db).queryForObject("select id from payments where id=? for update", UUID.class, paymentId);
	}

	@Test
	void fulfillCashLocksMemberBeforeOrder() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		when(db.queryForObject("select member_account_id from membership_orders where id=?", UUID.class, orderId))
			.thenReturn(memberId);
		when(db.queryForMap(anyString(), eq(paymentId), eq(orderId))).thenReturn(Map.of(
			"order_status", "PAID", "order_id", orderId, "member_account_id", memberId));
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, lifecycle,
			mock(AuditEventRepository.class), mock(NotificationWriter.class), Clock.systemUTC());

		assertThatThrownBy(() -> service.fulfillCash(paymentId, orderId, UUID.randomUUID(), "evidence", "reason"))
			.isInstanceOf(PaymentException.class).hasMessage("Order is not payable");

		InOrder order = inOrder(db, lifecycle);
		order.verify(db).queryForObject("select member_account_id from membership_orders where id=?", UUID.class, orderId);
		order.verify(lifecycle).reconcile(eq(memberId), org.mockito.ArgumentMatchers.any());
		order.verify(db).queryForMap(anyString(), eq(paymentId), eq(orderId));
	}

	@Test
	void fulfillCashCreatesReservedMembershipWithoutDates() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		Map<String, Object> row = new java.util.HashMap<>();
		row.put("order_id", orderId);
		row.put("payment_id", paymentId);
		row.put("payment_method", "CASH");
		row.put("payment_status", "PENDING");
		row.put("order_status", "PENDING_PAYMENT");
		row.put("fulfillment_mode", "RESERVED");
		row.put("member_account_id", memberId);
		row.put("offer_id", UUID.randomUUID());
		row.put("plan_code_snapshot", "BASIC");
		row.put("offer_name_snapshot", "Offer");
		row.put("price_amount_snapshot", java.math.BigDecimal.valueOf(1000));
		row.put("currency_code_snapshot", "VND");
		row.put("payment_amount", java.math.BigDecimal.valueOf(1000));
		row.put("payment_currency", "VND");
		row.put("duration_days_snapshot", 30);
		when(db.queryForObject("select member_account_id from membership_orders where id=?", UUID.class, orderId))
			.thenReturn(memberId);
		when(db.queryForMap(anyString(), eq(paymentId), eq(orderId))).thenReturn(row);
		when(db.queryForObject(anyString(), eq(Integer.class), eq(memberId))).thenReturn(1);
		when(db.update(anyString(), any(Object[].class))).thenReturn(1);
		PaymentFulfillmentService service = new PaymentFulfillmentService(db, mock(MembershipLifecycleService.class),
			mock(AuditEventRepository.class), mock(NotificationWriter.class), Clock.systemUTC());

		PaymentResultResponse response = service.fulfillCash(paymentId, orderId, UUID.randomUUID(), "evidence", "reason");

		assertThat(response.fulfillmentMode()).isEqualTo(FulfillmentMode.RESERVED);
		ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(db, org.mockito.Mockito.atLeastOnce()).update(sql.capture(), args.capture());
		int membershipInsert = sql.getAllValues().indexOf(sql.getAllValues().stream()
			.filter(value -> value.contains("insert into memberships")).findFirst().orElseThrow());
		String membershipSql = sql.getAllValues().get(membershipInsert);
		assertThat(membershipSql).contains("values(?,?,?,?,?,?,?,?,?,?,?,?)").doesNotContain("values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
		assertThat(args.getAllValues().get(membershipInsert)).hasSize(12).contains("RESERVED", null, null);
	}

	private Account member(UUID id) {
		return new Account(id, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member", "0900000000",
			id + "@example.test", LocalDate.of(1990, 1, 1), "hash");
	}
}
