package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface ClassSessionRepository extends JpaRepository<ClassSession, UUID>,
		JpaSpecificationExecutor<ClassSession> {

	@EntityGraph(attributePaths = {"sportClass", "sportClass.discipline", "teachingCoach", "room"})
	List<ClassSession> findByIdIn(Collection<UUID> ids);

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

	@Query("""
		select session from ClassSession session
		where session.id = :id
		  and session.teachingCoach.id = :coachId
		  and session.sportClass.classType in (
			com.scms.backend.scheduling.SportClassType.GROUP,
			com.scms.backend.scheduling.SportClassType.YOGA)
		""")
	java.util.Optional<ClassSession> findOwnedGroupOrYogaById(@Param("id") UUID id,
		@Param("coachId") UUID coachId);

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select session from ClassSession session where session.id = :id")
	java.util.Optional<ClassSession> findByIdForUpdate(@Param("id") UUID id);

	@Query(value = """
		select session.id
		from class_sessions session
		where session.status = 'SCHEDULED' and session.start_time > :now
		  and (cast(:cursor as uuid) is null or session.id > cast(:cursor as uuid))
		  and exists (
			select 1 from session_waitlists entry
			where entry.class_session_id = session.id and entry.status = 'WAITING'
		  )
		  and (select count(*) from bookings booking
			where booking.class_session_id = session.id and booking.status = 'BOOKED') < session.capacity
		order by session.id
		""", nativeQuery = true)
	List<UUID> findWaitlistPromotionCandidateIds(@Param("now") Instant now, @Param("cursor") UUID cursor,
		Pageable pageable);

	@Query("select session.room.id from ClassSession session where session.id = :id")
	java.util.Optional<UUID> findRoomIdById(@Param("id") UUID id);

	@Query("select session.teachingCoach.id from ClassSession session where session.id = :id")
	java.util.Optional<UUID> findCoachIdById(@Param("id") UUID id);

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

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.teachingCoach.id = :coachId
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsCoachOverlap(@Param("coachId") UUID coachId, @Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.id <> :sessionId
		  and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.teachingCoach.id = :coachId
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsCoachOverlapExcluding(@Param("sessionId") UUID sessionId, @Param("coachId") UUID coachId,
		@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.room.id = :roomId
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsRoomOverlap(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);

	@Query("""
		select (count(session) > 0) from ClassSession session
		where session.id <> :sessionId
		  and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.room.id = :roomId
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	boolean existsRoomOverlapExcluding(@Param("sessionId") UUID sessionId, @Param("roomId") UUID roomId,
		@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query("""
		select coalesce(sum(session.capacity), 0) from ClassSession session
		where session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.room.id = :roomId
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	long sumOverlappingCapacity(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);

	@Query("""
		select coalesce(sum(session.capacity), 0) from ClassSession session
		where session.id <> :sessionId
		  and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.room.id = :roomId
		  and session.startTime < :endTime and session.endTime > :startTime
		""")
	long sumOverlappingCapacityExcluding(@Param("sessionId") UUID sessionId, @Param("roomId") UUID roomId,
		@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query("""
		select session.id from ClassSession session
		where session.id <> :sessionId
		  and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and (session.room.id = :roomId or session.teachingCoach.id = :coachId)
		  and session.startTime < :endTime and session.endTime > :startTime
		order by session.startTime, session.id
		""")
	java.util.Optional<UUID> findConflictIdExcluding(@Param("sessionId") UUID sessionId,
			@Param("coachId") UUID coachId, @Param("roomId") UUID roomId,
			@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query(value = """
		select class_session_id as "sessionId", count(*) as "bookedCount"
		from bookings
		where status = 'BOOKED' and class_session_id in (:sessionIds)
		group by class_session_id
		""", nativeQuery = true)
	List<BookingCountView> countBookedBySessionIds(@Param("sessionIds") Collection<UUID> sessionIds);

	@Query(value = """
		select session.id as "sessionId", required_benefit.benefit_code as "benefitCode"
		from class_sessions session
		join sport_classes sport_class on sport_class.id = session.sport_class_id
		join discipline_required_benefits required_benefit
		  on required_benefit.discipline_id = sport_class.discipline_id
		where session.id in (:sessionIds)
		order by session.id, required_benefit.benefit_code
		""", nativeQuery = true)
	List<SessionRequiredBenefitView> findRequiredBenefitsBySessionIds(
		@Param("sessionIds") Collection<UUID> sessionIds);

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
		select session.id from ClassSession session
		where session.teachingCoach.id = :coachId
		  and session.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
			com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
		  and session.startTime < :endTime and session.endTime > :startTime
		order by session.startTime, session.id
		""")
	List<UUID> findActiveOverlapIdsByCoach(@Param("coachId") UUID coachId,
		@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query("""
		select coach.id as coachAccountId, coach.fullName as fullName, coach.phone as phone
		from Account coach
		where coach.role = com.scms.backend.account.AccountRole.COACH
		  and coach.status = com.scms.backend.account.AccountStatus.ACTIVE
		  and not exists (
			select conflict.id from ClassSession conflict
			where conflict.id <> :sessionId
			  and conflict.teachingCoach.id = coach.id
			  and conflict.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
				com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
			  and conflict.startTime < :endTime and conflict.endTime > :startTime
		  )
		  and not exists (
			select courseConflict.id from com.scms.backend.course.CourseSession courseConflict
			where courseConflict.coach.id = coach.id
			  and courseConflict.status in (com.scms.backend.course.CourseSessionStatus.SCHEDULED,
				com.scms.backend.course.CourseSessionStatus.MAKEUP_SCHEDULED)
			  and courseConflict.startAt < :endTime and courseConflict.endAt > :startTime
		  )
		  and not exists (
			select leave.id from CoachLeaveRequest leave
			where leave.coach.id = coach.id
			  and leave.status = com.scms.backend.scheduling.CoachLeaveStatus.APPROVED
			  and leave.startTime < :endTime and leave.endTime > :startTime
		  )
		order by coach.fullName, coach.id
		""")
	List<AvailableCoachView> findAvailableCoaches(@Param("sessionId") UUID sessionId,
		@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Query("""
		select coach.id as coachAccountId, coach.fullName as fullName, coach.phone as phone
		from Account coach
		where coach.role = com.scms.backend.account.AccountRole.COACH
		  and coach.status = com.scms.backend.account.AccountStatus.ACTIVE
		  and not exists (
			select conflict.id from ClassSession conflict
			where conflict.teachingCoach.id = coach.id
			  and conflict.status in (com.scms.backend.scheduling.ClassSessionStatus.SCHEDULED,
				com.scms.backend.scheduling.ClassSessionStatus.IN_PROGRESS)
			  and conflict.startTime < :endTime and conflict.endTime > :startTime
		  )
		  and not exists (
			select courseConflict.id from com.scms.backend.course.CourseSession courseConflict
			where courseConflict.coach.id = coach.id
			  and courseConflict.status in (com.scms.backend.course.CourseSessionStatus.SCHEDULED,
				com.scms.backend.course.CourseSessionStatus.MAKEUP_SCHEDULED)
			  and courseConflict.startAt < :endTime and courseConflict.endAt > :startTime
		  )
		  and not exists (
			select leave.id from CoachLeaveRequest leave
			where leave.coach.id = coach.id
			  and leave.status = com.scms.backend.scheduling.CoachLeaveStatus.APPROVED
			  and leave.startTime < :endTime and leave.endTime > :startTime
		  )
		order by coach.fullName, coach.id
		""")
	List<AvailableCoachView> findAvailableCoachesForWindow(@Param("startTime") Instant startTime,
		@Param("endTime") Instant endTime);

	@Query(value = """
		select distinct booking.member_account_id
		from bookings booking
		where booking.class_session_id = :sessionId and booking.status = 'BOOKED'
		order by booking.member_account_id
		""", nativeQuery = true)
	List<UUID> findBookedMemberIds(@Param("sessionId") UUID sessionId);

	@Query(value = """
		select distinct target.member_account_id
		from bookings target
		where target.class_session_id = :sessionId and target.status = 'BOOKED'
		  and exists (
			select 1 from bookings other_booking
			join class_sessions other_session on other_session.id = other_booking.class_session_id
			where other_booking.member_account_id = target.member_account_id
			  and other_booking.status = 'BOOKED'
			  and other_booking.class_session_id <> :sessionId
			  and other_session.status in ('SCHEDULED', 'IN_PROGRESS')
			  and other_session.start_time < :endTime and other_session.end_time > :startTime
		  )
		order by target.member_account_id
		""", nativeQuery = true)
	List<UUID> findBookedMemberConflictIds(@Param("sessionId") UUID sessionId,
		@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

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

	@Query(value = """
		select distinct teaching_coach_account_id from class_sessions
		where room_id = :roomId
		  and ((:permanentDisable = true and (status = 'IN_PROGRESS'
			or (status = 'SCHEDULED' and start_time > :now)))
		  or (:permanentDisable = false and status in ('SCHEDULED', 'IN_PROGRESS')
			and start_time < :endTime and end_time > :startTime))
		order by teaching_coach_account_id
		""", nativeQuery = true)
	List<UUID> findRoomImpactCoachIds(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
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

	interface SessionRequiredBenefitView {
		UUID getSessionId();
		String getBenefitCode();
	}

	interface AvailableCoachView {
		UUID getCoachAccountId();
		String getFullName();
		String getPhone();
	}
}
