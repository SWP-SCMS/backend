package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
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
import org.springframework.dao.DataIntegrityViolationException;

class MembershipOrderServiceTests {

	private final AccountRepository accountRepository = mock(AccountRepository.class);
	private final MemberProfileRepository memberProfileRepository = mock(MemberProfileRepository.class);
	private final MembershipOfferRepository offerRepository = mock(MembershipOfferRepository.class);
	private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
	private final MembershipOrderRepository orderRepository = mock(MembershipOrderRepository.class);
	private final MembershipOrderService service = new MembershipOrderService(accountRepository,
		memberProfileRepository, offerRepository, membershipRepository, orderRepository);

	private UUID memberAccountId;
	private UUID offerId;

	@BeforeEach
	void prepareValidCreation() {
		memberAccountId = UUID.randomUUID();
		offerId = UUID.randomUUID();
		Account account = mock(Account.class);
		when(account.getRole()).thenReturn(AccountRole.MEMBER);
		when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
		MemberProfile member = mock(MemberProfile.class);
		when(member.getAccountId()).thenReturn(memberAccountId);
		when(member.getAccount()).thenReturn(account);
		when(member.getMemberCode()).thenReturn("MB-100001");
		when(memberProfileRepository.findById(memberAccountId)).thenReturn(Optional.of(member));

		MembershipPlan plan = mock(MembershipPlan.class);
		when(plan.getPlanCode()).thenReturn(MembershipPlanCode.BASIC);
		MembershipOffer offer = mock(MembershipOffer.class);
		when(offer.getId()).thenReturn(offerId);
		when(offer.getPlan()).thenReturn(plan);
		when(offer.getName()).thenReturn("Race Offer");
		when(offer.getPriceAmount()).thenReturn(BigInteger.valueOf(500000));
		when(offer.getCurrencyCode()).thenReturn("VND");
		when(offer.getDurationDays()).thenReturn(30);
		when(offerRepository.findByIdAndStatus(offerId, MembershipOfferStatus.ACTIVE))
			.thenReturn(Optional.of(offer));
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
}
