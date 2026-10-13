package com.scms.backend.training;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {
}
