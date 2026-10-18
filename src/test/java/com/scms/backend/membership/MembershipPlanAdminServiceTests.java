package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipPlanAdminServiceTests {

	@Mock MembershipPlanRepository plans;
	@Mock MembershipBenefitRepository benefits;
	@Mock MembershipOrderRepository orders;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;

	@InjectMocks MembershipPlanAdminService service;

	@Test
	void activeManagerGetsBenefitsInCodeOrderIncludingInactive() {
		UUID managerId = activeManager();
		MembershipBenefit inactive = new MembershipBenefit("Z_ACCESS", "Z", "Z desc", MembershipBenefitStatus.INACTIVE);
		MembershipBenefit active = new MembershipBenefit("A_ACCESS", "A", "A desc", MembershipBenefitStatus.ACTIVE);
		when(benefits.findAllByOrderByCode()).thenReturn(List.of(active, inactive));

		assertThat(service.catalog(managerId)).extracting(MembershipBenefitResponse::code)
			.containsExactly("A_ACCESS", "Z_ACCESS");
	}

	@Test
	void listUsesBenefitFetchQueryAndRepresentsInactivePlan() {
		UUID managerId = activeManager();
		MembershipBenefit benefit = new MembershipBenefit("CENTER_ACCESS", "Center", "Access", MembershipBenefitStatus.ACTIVE);
		when(plans.findAllWithBenefits()).thenReturn(List.of(
			new MembershipPlan("OLD_PLAN", "Old", MembershipPlanStatus.INACTIVE, Set.of(benefit))));

		assertThat(service.list(managerId)).singleElement().satisfies(plan -> {
			assertThat(plan.code()).isEqualTo("OLD_PLAN");
			assertThat(plan.status()).isEqualTo(MembershipPlanStatus.INACTIVE);
		});
		verify(plans).findAllWithBenefits();
	}

	@Test
	void missingPlanReturnsNotFound() {
		UUID managerId = activeManager();
		when(plans.findByIdWithBenefits("MISSING")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(managerId, "MISSING"))
			.isInstanceOf(MembershipPlanNotFoundException.class);
	}

	@Test
	void createNormalizesCodeAndRequiresActiveBenefits() {
		UUID managerId = activeManager();
		MembershipBenefit benefit = new MembershipBenefit("CENTER_ACCESS", "Center", "Access", MembershipBenefitStatus.ACTIVE);
		when(plans.existsById("NEW_PLAN")).thenReturn(false);
		when(benefits.findAllByCodeIn(Set.of("CENTER_ACCESS"))).thenReturn(List.of(benefit));
		when(plans.saveAndFlush(any(MembershipPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));

		MembershipPlanResponse result = service.create(managerId,
			new MembershipPlanCreateRequest(" new_plan ", " New plan ", null, List.of("CENTER_ACCESS")));

		assertThat(result.code()).isEqualTo("NEW_PLAN");
		assertThat(result.displayName()).isEqualTo("New plan");
		assertThat(result.status()).isEqualTo(MembershipPlanStatus.ACTIVE);
		assertThat(result.benefitCodes()).containsExactly("CENTER_ACCESS");
		verify(audits).save(any());
	}

	@Test
	void createRejectsDuplicateCodeAndUnknownOrInactiveBenefits() {
		UUID managerId = activeManager();
		when(plans.existsById("BASIC")).thenReturn(true);
		assertThatThrownBy(() -> service.create(managerId,
			new MembershipPlanCreateRequest(" basic ", "Basic", null, List.of("CENTER_ACCESS"))))
			.isInstanceOf(MembershipPlanConflictException.class);

		when(plans.existsById("NEW_PLAN")).thenReturn(false);
		when(benefits.findAllByCodeIn(Set.of("MISSING"))).thenReturn(List.of());
		assertThatThrownBy(() -> service.create(managerId,
			new MembershipPlanCreateRequest("NEW_PLAN", "New", null, List.of("MISSING"))))
			.isInstanceOf(MembershipPlanValidationException.class);
	}

	@Test
	void lockedBenefitsRejectChangesButAcceptIdenticalInput() {
		UUID managerId = activeManager();
		MembershipBenefit benefit = new MembershipBenefit("CENTER_ACCESS", "Center", "Access", MembershipBenefitStatus.ACTIVE);
		MembershipPlan plan = new MembershipPlan("BASIC", "Basic", MembershipPlanStatus.ACTIVE, Set.of(benefit));
		when(plans.findByIdWithBenefits("BASIC")).thenReturn(Optional.of(plan));
		when(orders.existsByPlanCodeSnapshot("BASIC")).thenReturn(true);

		MembershipPlanResponse same = service.update(managerId, "BASIC",
			new MembershipPlanPatchRequest(null, null, List.of("CENTER_ACCESS")));
		assertThat(same.benefitCodes()).containsExactly("CENTER_ACCESS");
		verify(plans, never()).saveAndFlush(any());

		MembershipBenefit other = new MembershipBenefit("OTHER_ACCESS", "Other", "Other access", MembershipBenefitStatus.ACTIVE);
		when(benefits.findAllByCodeIn(Set.of("OTHER_ACCESS"))).thenReturn(List.of(other));
		assertThatThrownBy(() -> service.update(managerId, "BASIC",
			new MembershipPlanPatchRequest(null, null, List.of("OTHER_ACCESS"))))
			.isInstanceOf(MembershipPlanConflictException.class);
	}

	@Test
	void inactiveManagerIsRejected() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> service.catalog(managerId)).isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	private UUID activeManager() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		return managerId;
	}
}
