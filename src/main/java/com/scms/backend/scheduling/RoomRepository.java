package com.scms.backend.scheduling;

import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {

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
