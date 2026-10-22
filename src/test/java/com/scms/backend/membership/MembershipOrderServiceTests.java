package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MembershipOrderServiceTests {

	private final AccountRepository accountRepository = mock(AccountRepository.class);
	private final MemberProfileRepository memberProfileRepository = mock(MemberProfileRepository.class);
	private final MembershipOfferRepository offerRepository = mock(MembershipOfferRepository.class);
	private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
	private final MembershipOrderRepository orderRepository = mock(MembershipOrderRepository.class);
	private final MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
	private final MembershipOrderService service = new MembershipOrderService(accountRepository,
		memberProfileRepository, offerRepository, membershipRepository, orderRepository, lifecycle);

	private UUID memberAccountId;
	private UUID offerId;
	private MembershipPlan plan;
	private MembershipOffer offer;
	private MemberProfile member;

	@BeforeEach
	void prepareValidCreation() {
		memberAccountId = UUID.randomUUID();
		offerId = UUID.randomUUID();
		Account account = mock(Account.class);
		when(account.getRole()).thenReturn(AccountRole.MEMBER);
		when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
		member = mock(MemberProfile.class);
		when(member.getAccountId()).thenReturn(memberAccountId);
		when(member.getAccount()).thenReturn(account);
		when(member.getMemberCode()).thenReturn("MB-100001");
		when(memberProfileRepository.findById(memberAccountId)).thenReturn(Optional.of(member));
		when(memberProfileRepository.findVisibleMemberByCode(eq("MB-100001"), eq(AccountRole.MEMBER), any()))
			.thenReturn(Optional.of(member));
		when(accountRepository.existsByIdAndRoleAndStatus(any(), eq(AccountRole.RECEPTIONIST),
			eq(AccountStatus.ACTIVE))).thenReturn(true);

		plan = mock(MembershipPlan.class);
		when(plan.getPlanCode()).thenReturn(MembershipPlanCode.BASIC.name());
		when(plan.getStatus()).thenReturn(MembershipPlanStatus.ACTIVE);
		offer = mock(MembershipOffer.class);
		when(offer.getId()).thenReturn(offerId);
		when(offer.getPlan()).thenReturn(plan);
		when(offer.getName()).thenReturn("Race Offer");
		when(offer.getPriceAmount()).thenReturn(BigInteger.valueOf(500000));
		when(offer.getCurrencyCode()).thenReturn("VND");
		when(offer.getDurationDays()).thenReturn(30);
		when(offerRepository.findByIdAndStatusAndActivePlan(offerId, MembershipOfferStatus.ACTIVE))
			.thenReturn(Optional.of(offer));
		when(offerRepository.findSellablePlanCodeForUpdate(offerId))
			.thenReturn(Optional.of(MembershipPlanCode.BASIC.name()));
	}

	@Test
	void translatesPendingMemberUniqueConstraintFromSaveAndFlush() {
		when(orderRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("insert failed",
			new RuntimeException("duplicate key violates unique constraint \""
				+ MembershipOrderConstraintDetector.PENDING_MEMBER_CONSTRAINT + "\"")));

		assertThatThrownBy(() -> service.createForMember(memberAccountId,
			new MembershipOrderCreateRequest(offerId)))
			.isInstanceOf(PendingMembershipOrderExistsException.class);
	}

	@Test
	void doesNotHideUnrelatedDatabaseIntegrityFailure() {
		DataIntegrityViolationException unrelated = new DataIntegrityViolationException("insert failed",
			new RuntimeException("duplicate key violates unique constraint \"membership_orders_pkey\""));
		when(orderRepository.saveAndFlush(any())).thenThrow(unrelated);

		assertThatThrownBy(() -> service.createForMember(memberAccountId,
			new MembershipOrderCreateRequest(offerId)))
			.isSameAs(unrelated);
	}

	@Test
	void activeMemberCreationUsesAvailableOfferAndLocksActivePlan() {
		when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		AtomicReference<MembershipOrderResponse> response = new AtomicReference<>();

		assertThatCode(() -> response.set(service.createForMember(memberAccountId,
			new MembershipOrderCreateRequest(offerId)))).doesNotThrowAnyException();

		assertThat(response.get().offerId()).isEqualTo(offerId);
		verify(offerRepository).findSellablePlanCodeForUpdate(offerId);
		verify(offerRepository).findByIdAndStatusAndActivePlan(offerId, MembershipOfferStatus.ACTIVE);
	}

	@Test
	void activeReceptionistCreationUsesAvailableOfferAndActivePlan() {
		when(orderRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		AtomicReference<MembershipOrderResponse> response = new AtomicReference<>();

		assertThatCode(() -> response.set(service.createForReceptionist(UUID.randomUUID(), "MB-100001",
			new MembershipOrderCreateRequest(offerId)))).doesNotThrowAnyException();

		assertThat(response.get().memberId()).isEqualTo("MB-100001");
		assertThat(response.get().offerId()).isEqualTo(offerId);
	}

	@Test
	void memberCreationRejectsUnavailableOffer() {
		when(offerRepository.findByIdAndStatusAndActivePlan(offerId, MembershipOfferStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertUnavailable(() -> service.createForMember(memberAccountId,
			new MembershipOrderCreateRequest(offerId)));
		verify(orderRepository, never()).saveAndFlush(any());
	}

	@Test
	void receptionistCreationRejectsUnavailableOffer() {
		when(offerRepository.findByIdAndStatusAndActivePlan(offerId, MembershipOfferStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertUnavailable(() -> service.createForReceptionist(UUID.randomUUID(), "MB-100001",
			new MembershipOrderCreateRequest(offerId)));
		verify(orderRepository, never()).saveAndFlush(any());
	}

	@Test
	void creationRejectsFreshInactiveStatusWhenManagedPlanIsStaleActive() {
		when(offerRepository.findSellablePlanCodeForUpdate(offerId)).thenReturn(Optional.empty());

		assertUnavailable(() -> service.createForMember(memberAccountId,
			new MembershipOrderCreateRequest(offerId)));
		verify(orderRepository, never()).saveAndFlush(any());
	}

	@Test
	void preservesDynamicPlanCodeInOrderSnapshot() {
		MembershipPlan plan = mock(MembershipPlan.class);
		when(plan.getPlanCode()).thenReturn("FAMILY");
		MembershipOffer offer = mock(MembershipOffer.class);
		when(offer.getId()).thenReturn(offerId);
		when(offer.getPlan()).thenReturn(plan);
		when(offer.getName()).thenReturn("Family Offer");
		when(offer.getPriceAmount()).thenReturn(BigInteger.valueOf(500000));
		when(offer.getCurrencyCode()).thenReturn("VND");
		when(offer.getDurationDays()).thenReturn(30);

		MembershipOrder order = new MembershipOrder(UUID.randomUUID(), "ORD-FAMILY", memberAccountId,
			memberAccountId, offer);

		assertThat(order.getPlanCodeSnapshot()).isEqualTo("FAMILY");
	}

	private void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action)
			.isInstanceOf(MembershipOfferUnavailableException.class);
	}
}
