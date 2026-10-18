package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomSuspensionRepository extends JpaRepository<RoomSuspension, UUID> {

	@Query("""
		select (count(suspension) > 0) from RoomSuspension suspension
		where suspension.room.id = :roomId
		  and suspension.startTime < :endTime and suspension.endTime > :startTime
		""")
	boolean existsOverlap(@Param("roomId") UUID roomId, @Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime);

	@Query("""
		select (count(suspension) > 0) from RoomSuspension suspension
		where suspension.room.id = :roomId and suspension.id <> :suspensionId
		  and suspension.startTime < :endTime and suspension.endTime > :startTime
		""")
	boolean existsOverlapExcluding(@Param("roomId") UUID roomId, @Param("suspensionId") UUID suspensionId,
			@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select suspension from RoomSuspension suspension where suspension.id = :id")
	java.util.Optional<RoomSuspension> findByIdForUpdate(@Param("id") UUID id);

	List<RoomSuspension> findByRoom_IdOrderByStartTimeAsc(UUID roomId);
}
