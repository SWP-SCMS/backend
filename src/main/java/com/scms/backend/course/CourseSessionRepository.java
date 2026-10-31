package com.scms.backend.course;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseSessionRepository extends JpaRepository<CourseSession, UUID> {

	@EntityGraph(attributePaths = { "courseRun", "courseRun.course", "room", "coach" })
	List<CourseSession> findByCourseRunIdOrderByStartAtAscIdAsc(UUID runId);

	@EntityGraph(attributePaths = { "courseRun", "courseRun.course", "room", "coach" })
	java.util.Optional<CourseSession> findByIdAndCourseRunId(UUID id, UUID runId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from CourseSession session where session.id = :id")
	java.util.Optional<CourseSession> findByIdForUpdate(@Param("id") UUID id);

	@Query("select session.room.id from CourseSession session where session.id = :id")
	java.util.Optional<UUID> findRoomIdById(@Param("id") UUID id);

	@Query("select session.coach.id from CourseSession session where session.id = :id")
	java.util.Optional<UUID> findCoachIdById(@Param("id") UUID id);

	@Query("select session.courseRun.id from CourseSession session where session.id = :id")
	java.util.Optional<UUID> findCourseRunIdById(@Param("id") UUID id);

	@Query("""
		select (count(session) > 0) from CourseSession session
		where session.status in (com.scms.backend.course.CourseSessionStatus.SCHEDULED,
			com.scms.backend.course.CourseSessionStatus.MAKEUP_SCHEDULED)
		  and (session.room.id = :roomId or session.coach.id = :coachId)
		  and session.startAt < :endAt and session.endAt > :startAt
		""")
	boolean existsResourceOverlap(@Param("coachId") UUID coachId, @Param("roomId") UUID roomId,
			@Param("startAt") Instant startAt, @Param("endAt") Instant endAt);

	@Query("""
		select (count(session) > 0) from CourseSession session
		where session.id <> :sessionId
		  and session.status in (com.scms.backend.course.CourseSessionStatus.SCHEDULED,
			com.scms.backend.course.CourseSessionStatus.MAKEUP_SCHEDULED)
		  and (session.room.id = :roomId or session.coach.id = :coachId)
		  and session.startAt < :endAt and session.endAt > :startAt
		""")
	boolean existsResourceOverlapExcluding(@Param("sessionId") UUID sessionId, @Param("coachId") UUID coachId,
			@Param("roomId") UUID roomId, @Param("startAt") Instant startAt, @Param("endAt") Instant endAt);

	@Query("""
		select session.id from CourseSession session
		where session.coach.id = :coachId
		  and session.status in (com.scms.backend.course.CourseSessionStatus.SCHEDULED,
			com.scms.backend.course.CourseSessionStatus.MAKEUP_SCHEDULED)
		  and session.startAt < :endAt and session.endAt > :startAt
		order by session.startAt, session.id
		""")
	List<UUID> findActiveOverlapIdsByCoach(@Param("coachId") UUID coachId,
			@Param("startAt") Instant startAt, @Param("endAt") Instant endAt);

	@Query(value = """
		select session.id from course_sessions session
		where session.room_id = :roomId
		  and session.status in ('SCHEDULED', 'MAKEUP_SCHEDULED')
		  and ((:permanentDisable = true and session.end_at > :now)
		    or (:permanentDisable = false and session.start_at < :endAt and session.end_at > :startAt))
		order by session.id
		""", nativeQuery = true)
	List<UUID> findRoomImpactIds(@Param("roomId") UUID roomId, @Param("startAt") Instant startAt,
			@Param("endAt") Instant endAt, @Param("permanentDisable") boolean permanentDisable,
			@Param("now") Instant now);

	@Query(value = """
		select session.id from course_sessions session
		where session.room_id = :roomId
		  and session.status in ('SCHEDULED', 'MAKEUP_SCHEDULED')
		  and session.end_at > current_timestamp
		order by session.start_at, session.id
		""", nativeQuery = true)
	List<UUID> findBlockingRoomSessionIds(@Param("roomId") UUID roomId);

	@Query(value = """
		select exists (select 1 from course_sessions session
		join course_runs run on run.id = session.course_run_id
		where session.room_id = :roomId
		  and session.status in ('SCHEDULED', 'MAKEUP_SCHEDULED')
		  and run.capacity > :capacity)
		""", nativeQuery = true)
	boolean existsRunOverRoomCapacity(@Param("roomId") UUID roomId, @Param("capacity") int capacity);
}
