package com.scms.backend.scheduling;

import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {

	@Query(value = """
		select room.id as "roomId", room.name as "name", room.usage_mode as "usageMode",
		       room.capacity as "capacity", coalesce(sum(session.capacity), 0) as "occupiedCapacity",
		       exists (
		           select 1 from room_suspensions suspension
		           where suspension.room_id = room.id
		             and suspension.start_time < :endTime and suspension.end_time > :startTime
		       ) as "suspended"
		from rooms room
		left join class_sessions session on session.room_id = room.id
		 and session.status in ('SCHEDULED', 'IN_PROGRESS')
		 and session.start_time < :endTime and session.end_time > :startTime
		 and session.id <> coalesce(cast(:excludeSessionId as uuid),
		     cast('00000000-0000-0000-0000-000000000000' as uuid))
		where room.status = 'ACTIVE'
		group by room.id, room.name, room.usage_mode, room.capacity
		order by room.name, room.id
		""", nativeQuery = true)
	java.util.List<RoomAvailabilityView> findAvailability(@Param("startTime") java.time.Instant startTime,
			@Param("endTime") java.time.Instant endTime, @Param("excludeSessionId") UUID excludeSessionId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.id = :id")
	java.util.Optional<Room> findByIdForUpdate(@Param("id") UUID id);

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);

	@Query(value = """
		select exists (
			select 1 from class_sessions
			where room_id = :roomId
			  and status in ('SCHEDULED', 'IN_PROGRESS')
			  and capacity > :capacity
		)
		""", nativeQuery = true)
	boolean existsActiveSessionOverCapacity(@Param("roomId") UUID roomId, @Param("capacity") int capacity);

	@Query(value = """
		select exists (
			select 1 from class_sessions anchor
			where anchor.room_id = :roomId
			  and anchor.status in ('SCHEDULED', 'IN_PROGRESS')
			  and (select coalesce(sum(other.capacity), 0) from class_sessions other
			       where other.room_id = anchor.room_id
			         and other.status in ('SCHEDULED', 'IN_PROGRESS')
			         and other.start_time < anchor.end_time and other.end_time > anchor.start_time) > :capacity
		)
		""", nativeQuery = true)
	boolean existsActiveLoadOverCapacity(@Param("roomId") UUID roomId, @Param("capacity") int capacity);

	@Query(value = """
		select exists (
			select 1 from class_sessions first_session
			join class_sessions second_session on second_session.room_id = first_session.room_id
			 and second_session.id > first_session.id
			 and second_session.status in ('SCHEDULED', 'IN_PROGRESS')
			 and second_session.start_time < first_session.end_time
			 and second_session.end_time > first_session.start_time
			where first_session.room_id = :roomId
			  and first_session.status in ('SCHEDULED', 'IN_PROGRESS')
		)
		""", nativeQuery = true)
	boolean existsActiveSessionOverlapPair(@Param("roomId") UUID roomId);

	@Query(value = """
		select cs.id from class_sessions cs
		where cs.room_id = :roomId
		  and (cs.status = 'IN_PROGRESS'
		       or (cs.status = 'SCHEDULED' and cs.start_time > current_timestamp))
		order by cs.start_time, cs.id
		""", nativeQuery = true)
	java.util.List<UUID> findBlockingSessionIds(@Param("roomId") UUID roomId);

	@Query(value = """
		select exists (
			select 1 from room_suspensions
			where room_id = :roomId
			  and start_time < :endTime and end_time > :startTime
		)
		""", nativeQuery = true)
	boolean existsSuspensionOverlap(@Param("roomId") UUID roomId, @Param("startTime") java.time.Instant startTime,
			@Param("endTime") java.time.Instant endTime);

	@Query(value = """
		select exists (
			select 1 from class_sessions
			where room_id = :roomId
			  and status in ('SCHEDULED', 'IN_PROGRESS')
			  and start_time < :endTime and end_time > :startTime
		)
		""", nativeQuery = true)
	boolean existsActiveSessionOverlap(@Param("roomId") UUID roomId,
			@Param("startTime") java.time.Instant startTime, @Param("endTime") java.time.Instant endTime);
}
