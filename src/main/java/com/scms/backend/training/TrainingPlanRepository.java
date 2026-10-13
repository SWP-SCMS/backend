package com.scms.backend.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

	List<TrainingPlan> findByMemberAccountIdOrderByCreatedAtDesc(UUID memberAccountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from TrainingPlan p where p.id = :id and p.memberAccountId = :memberId")
	Optional<TrainingPlan> findByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("memberId") UUID memberId);
}
