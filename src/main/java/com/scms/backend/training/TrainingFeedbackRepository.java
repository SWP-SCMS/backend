package com.scms.backend.training;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingFeedbackRepository extends JpaRepository<TrainingFeedback, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select f from TrainingFeedback f
		where f.id = :id
		  and f.memberAccountId = :memberId
		  and f.coachAssignmentId is not null
		""")
	Optional<TrainingFeedback> findPersonalByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select f from TrainingFeedback f, TrainingResult r
		where f.id = :id
		  and f.memberAccountId = :memberId
		  and f.coachAssignmentId is null
		  and f.trainingPlanVersionId is null
		  and f.trainingResultId = r.id
		  and r.classSessionId = :sessionId
		  and r.coachAssignmentId is null
		""")
	Optional<TrainingFeedback> findSessionByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);
}
