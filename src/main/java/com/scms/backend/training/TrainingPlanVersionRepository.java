package com.scms.backend.training;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPlanVersionRepository extends JpaRepository<TrainingPlanVersion, UUID> {

	Optional<TrainingPlanVersion> findTopByTrainingPlanIdOrderByVersionNumberDesc(UUID trainingPlanId);
}
