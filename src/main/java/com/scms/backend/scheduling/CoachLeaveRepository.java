package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CoachLeaveRepository extends JpaRepository<CoachLeaveRequest, UUID> {
	List<CoachLeaveRequest> findByCoach_IdOrderByCreatedAtDesc(UUID coachId);
	List<CoachLeaveRequest> findByStatusOrderByCreatedAtAsc(CoachLeaveStatus status);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select request from CoachLeaveRequest request where request.id = :id")
	java.util.Optional<CoachLeaveRequest> findByIdForUpdate(@Param("id") UUID id);

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

