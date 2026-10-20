package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class PaymentServiceLockingTests {

	@Test
	void cashLocksAndRevalidatesMemberBeforeMembershipChecks() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		PaymentFulfillmentService fulfillment = mock(PaymentFulfillmentService.class);
		PaymentService service = service(db, accounts, fulfillment);
		UUID actorId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		Account member = account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE);
		when(accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(argThat(sql -> sql != null && sql.contains("join member_profiles")), eq("MB-100001")))
			.thenReturn(Map.of("account_id", memberId));
		stubAvailableOffer(db, offerId);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
		when(db.queryForObject(
			"select count(*) from memberships where member_account_id=? and status='ACTIVE'", Long.class, memberId))
			.thenReturn(0L);
		when(db.queryForObject(
			"select count(*) from membership_orders where member_account_id=? and status='PENDING_PAYMENT'",
			Long.class, memberId)).thenReturn(0L);

		service.cash(actorId, "MB-100001", new CashPaymentRequest(offerId));

		verify(db).queryForMap(argThat(this::isAvailableOfferLockQuery), eq(offerId));
		InOrder order = inOrder(accounts, db);
		order.verify(accounts).findByIdForUpdate(memberId);
		order.verify(db).queryForObject(
			"select count(*) from memberships where member_account_id=? and status='ACTIVE'", Long.class, memberId);
		order.verify(db).queryForObject(
			"select count(*) from membership_orders where member_account_id=? and status='PENDING_PAYMENT'",
			Long.class, memberId);
	}

	@Test
	void cashRejectsMemberThatBecameSuspendedBeforeMembershipChecks() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		PaymentFulfillmentService fulfillment = mock(PaymentFulfillmentService.class);
		PaymentService service = service(db, accounts, fulfillment);
		UUID actorId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(argThat(sql -> sql != null && sql.contains("join member_profiles")), eq("MB-100001")))
			.thenReturn(Map.of("account_id", memberId));
		stubAvailableOffer(db, offerId);
		when(accounts.findByIdForUpdate(memberId))
			.thenReturn(Optional.of(account(memberId, AccountRole.MEMBER, AccountStatus.SUSPENDED)));

		assertThatThrownBy(() -> service.cash(actorId, "MB-100001", new CashPaymentRequest(offerId)))
			.isInstanceOf(PaymentException.class)
			.hasMessage("Active Member or Membership Offer was not found");
		verify(db, never()).queryForObject(
			"select count(*) from memberships where member_account_id=? and status='ACTIVE'", Long.class, memberId);
		verify(fulfillment, never()).fulfillCash(any(), any(), any(), any(), any());
	}

	@Test
	void cashMapsPendingOrderInsertRaceToConflict() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		PaymentFulfillmentService fulfillment = mock(PaymentFulfillmentService.class);
		PaymentService service = service(db, accounts, fulfillment);
		UUID actorId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(argThat(sql -> sql != null && sql.contains("join member_profiles")), eq("MB-100001")))
			.thenReturn(Map.of("account_id", memberId));
		stubAvailableOffer(db, offerId);
		when(accounts.findByIdForUpdate(memberId))
			.thenReturn(Optional.of(account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)));
		when(db.queryForObject(any(String.class), eq(Long.class), eq(memberId))).thenReturn(0L);
		doThrow(new DataIntegrityViolationException("uq_membership_orders_pending_member"))
			.when(db).update(argThat(sql -> sql.contains("insert into membership_orders")), any(Object[].class));

		assertThatThrownBy(() -> service.cash(actorId, "MB-100001", new CashPaymentRequest(offerId)))
			.isInstanceOfSatisfying(PaymentException.class, exception -> {
				assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(exception).hasMessage("Member already has a pending membership order");
			});
	}

	@Test
	void paidReconciliationUsesReadOnlyValidationBeforeFulfillmentLocks() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		PaymentFulfillmentService fulfillment = mock(PaymentFulfillmentService.class);
		PaymentService service = service(db, accounts, fulfillment);
		UUID actorId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		Map<String, Object> payment = Map.of(
			"payment_amount", BigDecimal.valueOf(12000),
			"bank_transfer_content", "SCMS-ORD-1");
		when(accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(fulfillment.payment(paymentId)).thenReturn(payment);

		service.reconcile(actorId, paymentId, new PaymentActionRequest(
			PaymentActionRequest.ReconciliationStatus.PAID, BigInteger.valueOf(12000), "SCMS-ORD-1", "TX-1",
			"bank statement", "manual confirmation"));

		verify(fulfillment).payment(paymentId);
		verify(fulfillment, never()).paymentForUpdate(paymentId);
		verify(fulfillment).fulfill(paymentId, actorId, "TX-1", "bank statement", "manual confirmation");
	}

	@Test
	void cashRejectsOfferWithoutAnActivePlanUsingOrderAvailabilityContract() {
		JdbcTemplate db = mock(JdbcTemplate.class);
		AccountRepository accounts = mock(AccountRepository.class);
		PaymentFulfillmentService fulfillment = mock(PaymentFulfillmentService.class);
		PaymentService service = service(db, accounts, fulfillment);
		UUID actorId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(argThat(sql -> sql != null && sql.contains("join member_profiles")), eq("MB-100001")))
			.thenReturn(Map.of("account_id", memberId));
		when(db.queryForMap(argThat(this::isAvailableOfferLockQuery), eq(offerId)))
			.thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

		assertThatThrownBy(() -> service.cash(actorId, "MB-100001", new CashPaymentRequest(offerId)))
			.isInstanceOf(MembershipOfferUnavailableException.class);
		verify(accounts, never()).findByIdForUpdate(memberId);
		verify(fulfillment, never()).fulfillCash(any(), any(), any(), any(), any());
	}

	private PaymentService service(JdbcTemplate db, AccountRepository accounts,
			PaymentFulfillmentService fulfillment) {
		return new PaymentService(db, accounts, mock(AuditEventRepository.class), fulfillment,
			mock(com.scms.backend.account.MemberProfileRepository.class), mock(MembershipRepository.class),
			Clock.systemUTC());
	}

	private Account account(UUID id, AccountRole role, AccountStatus status) {
		return new Account(id, role, status, "Member", "0900000000", id + "@example.test",
			LocalDate.of(1990, 1, 1), "hash");
	}

	private Map<String, Object> offer(UUID offerId) {
		return Map.of(
			"id", offerId,
			"name", "Offer",
			"plan_code", "BASIC",
			"price_amount", BigDecimal.valueOf(12000),
			"currency_code", "VND",
			"duration_days", 30);
	}

	private void stubAvailableOffer(JdbcTemplate db, UUID offerId) {
		when(db.queryForMap(argThat(this::isAvailableOfferLockQuery), eq(offerId)))
			.thenReturn(offer(offerId));
	}

	private boolean isAvailableOfferLockQuery(String sql) {
		return sql != null
			&& sql.contains("join membership_plans")
			&& sql.contains("offer.status='ACTIVE'")
			&& sql.contains("plan.status='ACTIVE'")
			&& sql.contains("for update of plan");
	}
}
