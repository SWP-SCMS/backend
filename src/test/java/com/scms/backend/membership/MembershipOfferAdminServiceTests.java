package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipOfferAdminServiceTests {

	@Mock MembershipOfferRepository offers;
	@Mock MembershipPlanRepository plans;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@InjectMocks MembershipOfferAdminService service;

	@Test
	void createWritesCompleteAuditEvent() {
		UUID managerId = activeManager();
		MembershipPlan plan = plan(MembershipPlanCode.PLUS);
		when(plans.findById(MembershipPlanCode.PLUS)).thenReturn(Optional.of(plan));
		when(offers.save(any(MembershipOffer.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MembershipOfferAdminResponse result = service.create(managerId, new MembershipOfferAdminRequest(
			MembershipPlanCode.PLUS, " Plus 30 ", " Full access ", BigInteger.valueOf(750000), 30, null));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits, times(1)).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("MEMBERSHIP_OFFER_CREATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getTargetType()).isEqualTo("MEMBERSHIP_OFFER");
		assertThat(audit.getValue().getTargetId()).isEqualTo(result.offerId());
		assertThat(audit.getValue().getBeforeData()).isNull();
		assertThat(audit.getValue().getAfterData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"planCode", "PLUS", "name", "Plus 30", "description", "Full access",
			"priceAmount", BigInteger.valueOf(750000), "currencyCode", "VND", "durationDays", 30,
			"status", "ACTIVE", "createdByAccountId", managerId.toString()));
	}

	@Test
	void updateWritesCompleteBeforeAndAfterAuditEvent() {
		UUID managerId = activeManager();
		UUID offerId = UUID.randomUUID();
		MembershipOffer offer = new MembershipOffer(offerId, plan(MembershipPlanCode.PLUS), "Plus 30", "Old",
			BigInteger.valueOf(750000), "VND", 30, MembershipOfferStatus.ACTIVE, managerId);
		when(offers.findById(offerId)).thenReturn(Optional.of(offer));

		service.update(managerId, offerId, new MembershipOfferPatchRequest(null, " Plus 60 ", " Updated ",
			BigInteger.valueOf(900000), 60, MembershipOfferStatus.INACTIVE));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits, times(1)).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("MEMBERSHIP_OFFER_UPDATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getTargetType()).isEqualTo("MEMBERSHIP_OFFER");
		assertThat(audit.getValue().getTargetId()).isEqualTo(offerId);
		assertThat(audit.getValue().getBeforeData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"planCode", "PLUS", "name", "Plus 30", "description", "Old",
			"priceAmount", BigInteger.valueOf(750000), "currencyCode", "VND", "durationDays", 30,
			"status", "ACTIVE", "createdByAccountId", managerId.toString()));
		assertThat(audit.getValue().getAfterData()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
			"planCode", "PLUS", "name", "Plus 60", "description", "Updated",
			"priceAmount", BigInteger.valueOf(900000), "currencyCode", "VND", "durationDays", 60,
			"status", "INACTIVE", "createdByAccountId", managerId.toString()));
	}

	@Test
	void normalizedNoOpPatchWritesNoAudit() {
		UUID managerId = activeManager();
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plan(MembershipPlanCode.BASIC), "Basic",
			"Access", BigInteger.valueOf(500000), "VND", 30, MembershipOfferStatus.ACTIVE, managerId);
		when(offers.findById(offer.getId())).thenReturn(Optional.of(offer));

		MembershipOfferAdminResponse result = service.update(managerId, offer.getId(),
			new MembershipOfferPatchRequest(null, " Basic ", " Access ", BigInteger.valueOf(500000), 30,
				MembershipOfferStatus.ACTIVE));

		assertThat(result).isEqualTo(MembershipOfferAdminResponse.from(offer));
		verify(audits, never()).save(any());
	}

	private UUID activeManager() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		return managerId;
	}

	private MembershipPlan plan(MembershipPlanCode code) {
		MembershipPlan plan = mock(MembershipPlan.class);
		when(plan.getPlanCode()).thenReturn(code);
		return plan;
	}
}
