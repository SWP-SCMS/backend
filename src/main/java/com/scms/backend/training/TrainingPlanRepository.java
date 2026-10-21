package com.scms.backend.training;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

	Optional<TrainingPlan> findByIdAndMemberAccountId(UUID id, UUID memberAccountId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from TrainingPlan p where p.memberAccountId = :memberId and p.status = com.scms.backend.training.TrainingPlanStatus.ACTIVE")
	Optional<TrainingPlan> findActiveByMemberAccountIdForUpdate(@Param("memberId") UUID memberId);

	@Query("select p from TrainingPlan p where p.memberAccountId = :memberId "
			+ "order by p.createdAt desc, p.id desc")
	List<TrainingPlan> findByMemberAccountIdOrderByCreatedAtDesc(@Param("memberId") UUID memberId);

	@Query(value = "select p from TrainingPlan p where p.memberAccountId = :memberId",
			countQuery = "select count(p) from TrainingPlan p where p.memberAccountId = :memberId")
	Page<TrainingPlan> findByMemberAccountId(@Param("memberId") UUID memberId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from TrainingPlan p where p.id = :id and p.memberAccountId = :memberId")
	Optional<TrainingPlan> findByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("memberId") UUID memberId);
}
