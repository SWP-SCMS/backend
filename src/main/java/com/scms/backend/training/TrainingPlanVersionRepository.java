package com.scms.backend.training;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingPlanVersionRepository extends JpaRepository<TrainingPlanVersion, UUID> {

	Optional<TrainingPlanVersion> findTopByTrainingPlanIdOrderByVersionNumberDesc(UUID trainingPlanId);

	List<TrainingPlanVersion> findByTrainingPlanIdOrderByVersionNumberDesc(UUID trainingPlanId);

	@Query("select v from TrainingPlanVersion v where v.trainingPlanId in :planIds "
			+ "order by v.trainingPlanId, v.versionNumber")
	List<TrainingPlanVersion> findHistoryByTrainingPlanIds(@Param("planIds") Collection<UUID> planIds);

	@Query(value = """
		select v.*
		from training_plan_versions v
		join training_plans p on p.id = v.training_plan_id
		where v.id = :versionId and p.member_account_id = :memberId
		""", nativeQuery = true)
	Optional<TrainingPlanVersion> findByIdAndMemberAccountId(@Param("versionId") UUID versionId,
			@Param("memberId") UUID memberId);
}
