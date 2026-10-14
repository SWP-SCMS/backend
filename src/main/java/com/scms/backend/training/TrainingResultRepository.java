package com.scms.backend.training;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingResultRepository extends JpaRepository<TrainingResult, UUID> {

	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.coachAssignmentId is not null
		  and r.classSessionId is null
		""")
	Optional<TrainingResult> findPersonalByIdAndMemberAccountId(@Param("id") UUID id,
			@Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.coachAssignmentId is not null
		  and r.classSessionId is null
		""")
	Optional<TrainingResult> findPersonalByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("memberId") UUID memberId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select r from TrainingResult r
		where r.id = :id
		  and r.memberAccountId = :memberId
		  and r.classSessionId = :sessionId
		  and r.coachAssignmentId is null
		""")
	Optional<TrainingResult> findSessionByIdAndMemberAccountIdForUpdate(@Param("id") UUID id,
			@Param("sessionId") UUID sessionId, @Param("memberId") UUID memberId);
}
