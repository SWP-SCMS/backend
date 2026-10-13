package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTrainingPlanServiceTests {

	@Mock TrainingPlanRepository plans;
	@Mock TrainingPlanVersionRepository versions;

	private final UUID memberId = UUID.randomUUID();
	private MemberTrainingPlanService service;

	@BeforeEach
	void setUp() {
		service = new MemberTrainingPlanService(plans, versions);
	}

	@Test
	void viewOwnPlansReturnsEveryVersionInHistoryOrder() {
		UUID planId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-10-13T06:00:00Z");
		TrainingPlan plan = new TrainingPlan(planId, memberId, UUID.randomUUID(), "Strength plan",
				coachId, createdAt);
		TrainingPlanVersion first = new TrainingPlanVersion(UUID.randomUUID(), planId, 1,
				Map.of("goal", "Foundation"), coachId, createdAt);
		TrainingPlanVersion second = new TrainingPlanVersion(UUID.randomUUID(), planId, 2,
				Map.of("goal", "Progressive overload"), coachId, createdAt.plusSeconds(3600));
		when(plans.findByMemberAccountIdOrderByCreatedAtDesc(memberId)).thenReturn(List.of(plan));
		when(versions.findHistoryByTrainingPlanIds(List.of(planId))).thenReturn(List.of(first, second));

		List<MemberTrainingPlanResponse> result = service.viewOwnPlans(memberId);

		assertThat(result).singleElement().satisfies(response -> {
			assertThat(response.memberAccountId()).isEqualTo(memberId);
			assertThat(response.title()).isEqualTo("Strength plan");
			assertThat(response.versions()).extracting(MemberTrainingPlanVersionResponse::versionNumber)
					.containsExactly(1, 2);
			assertThat(response.versions().get(0).content()).containsEntry("goal", "Foundation");
			assertThat(response.versions().get(1).content()).containsEntry("goal", "Progressive overload");
		});
	}

	@Test
	void viewOwnPlansReturnsEmptyWithoutLoadingVersionHistory() {
		when(plans.findByMemberAccountIdOrderByCreatedAtDesc(memberId)).thenReturn(List.of());

		assertThat(service.viewOwnPlans(memberId)).isEmpty();

		verify(versions, never()).findHistoryByTrainingPlanIds(List.of());
	}
}
