package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID> {

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and (session.room.id = :roomId or session.teachingCoach.id = :coachId)
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsOverlap(@Param("coachId") UUID coachId, @Param("roomId") UUID roomId,
			@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
