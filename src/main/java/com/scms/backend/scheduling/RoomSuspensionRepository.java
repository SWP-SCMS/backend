package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
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

	List<RoomSuspension> findByRoom_IdOrderByStartTimeAsc(UUID roomId);
}
