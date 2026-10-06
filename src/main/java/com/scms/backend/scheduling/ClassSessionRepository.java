package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>,
		JpaSpecificationExecutor<ClassSession> {

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from ClassSession session where session.id = :id")
	java.util.Optional<ClassSession> findByIdForUpdate(@Param("id") UUID id);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ClassSession session
		set session.status = com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS,
			session.updatedAt = :now, session.version = session.version + 1
		where session.status = com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED
		  and session.startTime <= :now
		""")
	int advanceScheduledToInProgress(@Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update ClassSession session
		set session.status = com.scms.backend.scheduling.ClassSessionStatus.COMPLETED,
			session.updatedAt = :now, session.version = session.version + 1
		where session.status = com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS
		  and session.endTime <= :now
		""")
	int advanceInProgressToCompleted(@Param("now") Instant now);

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and (session.room.id = :roomId or session.teachingCoach.id = :coachId)
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsOverlap(@Param("coachId") UUID coachId, @Param("roomId") UUID roomId,
			@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.id <> :sessionId
		  and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and (session.room.id = :roomId or session.teachingCoach.id = :coachId)
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsOverlapExcluding(@Param("sessionId") UUID sessionId, @Param("coachId") UUID coachId,
			@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime);
}
