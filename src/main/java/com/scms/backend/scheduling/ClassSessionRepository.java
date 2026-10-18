package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>,
		JpaSpecificationExecutor<ClassSession> {

	java.util.Optional<ClassSession> findByIdAndTeachingCoach_Id(UUID id, UUID coachId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select session from ClassSession session
		where session.id = :id
		  and session.teachingCoach.id = :coachId
		  and session.sportClass.classType in (
			com.scms.backend.scheduling.SportClassType.GROUP,
			com.scms.backend.scheduling.SportClassType.YOGA)
		""")
	java.util.Optional<ClassSession> findOwnedGroupOrYogaByIdForRead(@Param("id") UUID id,
		@Param("coachId") UUID coachId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from ClassSession session where session.id = :id")
	java.util.Optional<ClassSession> findByIdForUpdate(@Param("id") UUID id);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select session from ClassSession session
		where session.id in :sessionIds
		order by session.id
		""")
	List<ClassSession> findAllByIdForUpdate(@Param("sessionIds") Collection<UUID> sessionIds);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select session from ClassSession session
		where session.status = com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED
		  and session.startTime > :now
		  and (:classId is null or session.sportClass.id = :classId)
		  and (:from is null or session.endTime > :from)
		  and (:to is null or session.startTime < :to)
		order by session.id
		""")
	List<ClassSession> findFutureScheduledForUpdate(@Param("classId") UUID classId,
		@Param("from") Instant from, @Param("to") Instant to, @Param("now") Instant now);

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

	@Query(value = """
		select class_session_id as "sessionId", count(*) as "bookedCount"
		from bookings
		where status = 'BOOKED' and class_session_id in (:sessionIds)
		group by class_session_id
		""", nativeQuery = true)
	List<BookingCountView> countBookedBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

	@Query("""
		select session from ClassSession session
		where session.id in :sessionIds
		  and session.status = com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED
		  and session.startTime > :now
		order by session.startTime, session.id
		""")
	List<ClassSession> findFutureScheduledByIds(@Param("sessionIds") Collection<UUID> sessionIds,
			@Param("now") Instant now);

	@Query("""
		select session from ClassSession session
		where session.room.id = :roomId
		  and ((:permanentDisable = true and (session.status = com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS
			or (session.status = com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED and session.startTime > :now)))
			or (:permanentDisable = false and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
			and session.startTime < :endTime and session.endTime > :startTime))
		order by session.id
		""")
	List<ClassSession> findRoomImpacts(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime, @Param("permanentDisable") boolean permanentDisable,
			@Param("now") Instant now);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select session from ClassSession session
		where session.room.id = :roomId
		  and ((:permanentDisable = true and (session.status = com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS
			or (session.status = com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED and session.startTime > :now)))
			or (:permanentDisable = false and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
			and session.startTime < :endTime and session.endTime > :startTime))
		order by session.id
		""")
	List<ClassSession> findRoomImpactsForUpdate(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime, @Param("permanentDisable") boolean permanentDisable,
			@Param("now") Instant now);

	interface BookingCountView {
		UUID getSessionId();
		long getBookedCount();
	}
}
