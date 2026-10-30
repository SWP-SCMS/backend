package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoachLeaveRepository extends JpaRepository<CoachLeaveRequest, UUID> {
	@Override
	@EntityGraph(attributePaths = "coach")
	List<CoachLeaveRequest> findAll();

	@EntityGraph(attributePaths = "coach")
	List<CoachLeaveRequest> findByCoach_IdOrderByCreatedAtDesc(UUID coachId);

	@EntityGraph(attributePaths = "coach")
	List<CoachLeaveRequest> findByStatusOrderByCreatedAtAsc(CoachLeaveStatus status);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from CoachLeaveRequest request where request.id = :id")
	java.util.Optional<CoachLeaveRequest> findByIdForUpdate(@Param("id") UUID id);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select request from CoachLeaveRequest request
		where request.status = com.scms.backend.scheduling.CoachLeaveStatus.PENDING
		  and request.startTime <= :now
		order by request.id
		""")
	List<CoachLeaveRequest> findPendingStartedForUpdate(@Param("now") Instant now);

	@Query("""
		select (count(request) > 0) from CoachLeaveRequest request
		where request.coach.id = :coachId
		  and request.status in (com.scms.backend.scheduling.CoachLeaveStatus.PENDING,
			com.scms.backend.scheduling.CoachLeaveStatus.APPROVED)
		  and request.startTime < :endTime and request.endTime > :startTime
		""")
	boolean existsOpenOverlap(@Param("coachId") UUID coachId, @Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);

	@Query("""
		select (count(request) > 0) from CoachLeaveRequest request
		where request.coach.id = :coachId
		  and request.status = com.scms.backend.scheduling.CoachLeaveStatus.APPROVED
		  and request.startTime < :endTime and request.endTime > :startTime
		""")
	boolean existsApprovedOverlap(@Param("coachId") UUID coachId, @Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);
}

