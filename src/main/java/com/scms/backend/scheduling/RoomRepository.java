package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {

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
}
