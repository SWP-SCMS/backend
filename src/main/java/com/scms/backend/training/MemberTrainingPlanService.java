package com.scms.backend.training;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberTrainingPlanService {

	private final TrainingPlanRepository plans;
	private final TrainingPlanVersionRepository versions;

	public MemberTrainingPlanService(TrainingPlanRepository plans, TrainingPlanVersionRepository versions) {
		this.plans = plans;
		this.versions = versions;
	}

	@Transactional(readOnly = true)
	public List<MemberTrainingPlanResponse> viewOwnPlans(UUID memberId) {
		List<TrainingPlan> memberPlans = plans.findByMemberAccountIdOrderByCreatedAtDesc(memberId);
		if (memberPlans.isEmpty()) {
			return List.of();
		}
		Map<UUID, List<TrainingPlanVersion>> historyByPlan = versions
				.findHistoryByTrainingPlanIds(memberPlans.stream().map(TrainingPlan::getId).toList())
				.stream().collect(Collectors.groupingBy(TrainingPlanVersion::getTrainingPlanId));
		return memberPlans.stream()
				.map(plan -> MemberTrainingPlanResponse.from(plan,
					historyByPlan.getOrDefault(plan.getId(), List.of())))
				.toList();
	}
}
