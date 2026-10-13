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

	@Query("select v from TrainingPlanVersion v where v.trainingPlanId in :planIds "
			+ "order by v.trainingPlanId, v.versionNumber")
	List<TrainingPlanVersion> findHistoryByTrainingPlanIds(@Param("planIds") Collection<UUID> planIds);
}
