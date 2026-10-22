package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MembershipFulfillmentModeTests {

	private final AccountRepository accounts = mock(AccountRepository.class);
	private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
	private final MembershipOfferRepository offers = mock(MembershipOfferRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private final MembershipOrderRepository orders = mock(MembershipOrderRepository.class);
	private final MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
	private UUID memberId;
	private UUID offerId;
	private MembershipOffer offer;

	@BeforeEach
	void setUp() {
		memberId = UUID.randomUUID();
		offerId = UUID.randomUUID();
		Account account = mock(Account.class);
		when(account.getRole()).thenReturn(AccountRole.MEMBER);
		when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
		MemberProfile member = mock(MemberProfile.class);
		when(member.getAccountId()).thenReturn(memberId);
		when(member.getAccount()).thenReturn(account);
		when(member.getMemberCode()).thenReturn("MB-100001");
		when(profiles.findById(memberId)).thenReturn(Optional.of(member));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(account));
		MembershipPlan plan = mock(MembershipPlan.class);
		when(plan.getPlanCode()).thenReturn("BASIC");
		offer = mock(MembershipOffer.class);
		when(offer.getId()).thenReturn(offerId);
		when(offer.getPlan()).thenReturn(plan);
		when(offer.getName()).thenReturn("Offer");
		when(offer.getPriceAmount()).thenReturn(java.math.BigInteger.valueOf(1000));
		when(offer.getCurrencyCode()).thenReturn("VND");
		when(offer.getDurationDays()).thenReturn(30);
		when(offers.findSellablePlanCodeForUpdate(offerId)).thenReturn(Optional.of("BASIC"));
		when(offers.findByIdAndStatusAndActivePlan(offerId, MembershipOfferStatus.ACTIVE))
			.thenReturn(Optional.of(offer));
		when(orders.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void activeMemberGetsReservedOrder() throws Exception {
		when(memberships.existsByMemberAccountIdAndStatus(memberId, MembershipStatus.ACTIVE)).thenReturn(true);

		MembershipOrderService service = service();
		MembershipOrderResponse response = service.createForMember(memberId, new MembershipOrderCreateRequest(offerId));

		MembershipOrder order = capturedOrder();
		assertThat(enumValue(order, "getFulfillmentMode")).isEqualTo("RESERVED");
		assertThat(enumValue(response, "fulfillmentMode")).isEqualTo("RESERVED");
	}

	@Test
	void reservedMembershipRejectsRenewalWithStableConflictCode() {
		when(memberships.existsByMemberAccountIdAndStatus(memberId, MembershipStatus.RESERVED)).thenReturn(true);

		assertThatThrownBy(() -> service().createForMember(memberId, new MembershipOrderCreateRequest(offerId)))
			.isInstanceOf(RuntimeException.class)
			.satisfies(exception -> assertThat(exception.getClass().getSimpleName())
				.isEqualTo("MembershipRenewalAlreadyExistsException"));
	}

	@Test
	void paidResultCarriesFulfillmentMode() throws Exception {
		UUID paymentId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		JdbcTemplateFixture fixture = new JdbcTemplateFixture(paymentId, orderId);
		PaymentFulfillmentService service = fixture.service();

		PaymentResultResponse response = service.result(Map.of(
			"payment_id", paymentId, "order_id", orderId, "payment_status", "PAID",
			"payment_method", "BANK_TRANSFER", "paid_at", java.sql.Timestamp.from(java.time.Instant.now())));

		assertThat(enumValue(response, "fulfillmentMode")).isEqualTo("RESERVED");
	}

	private MembershipOrderService service() {
		return new MembershipOrderService(accounts, profiles, offers, memberships, orders, lifecycle);
	}

	private MembershipOrder capturedOrder() {
		ArgumentCaptor<MembershipOrder> captor = ArgumentCaptor.forClass(MembershipOrder.class);
		verify(orders).saveAndFlush(captor.capture());
		return captor.getValue();
	}

	private String enumValue(Object target, String accessor) throws Exception {
		Method method = target.getClass().getMethod(accessor);
		Object value = method.invoke(target);
		return value == null ? null : value.toString();
	}

	private static final class JdbcTemplateFixture {
		private final org.springframework.jdbc.core.JdbcTemplate db = mock(org.springframework.jdbc.core.JdbcTemplate.class);
		private final UUID paymentId;
		private final UUID orderId;

		private JdbcTemplateFixture(UUID paymentId, UUID orderId) {
			this.paymentId = paymentId;
			this.orderId = orderId;
			when(db.queryForMap(any(String.class), eq(orderId))).thenReturn(Map.of(
				"membership_id", UUID.randomUUID(), "receipt_id", UUID.randomUUID(),
				"fulfillment_mode", "RESERVED"));
		}

		private PaymentFulfillmentService service() {
			return new PaymentFulfillmentService(db, mock(MembershipLifecycleService.class),
				mock(com.scms.backend.audit.AuditEventRepository.class),
				mock(com.scms.backend.notification.NotificationWriter.class), java.time.Clock.systemUTC());
		}
	}
}
