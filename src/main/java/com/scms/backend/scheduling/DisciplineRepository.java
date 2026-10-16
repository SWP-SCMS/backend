package com.scms.backend.scheduling;

import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);

	@Query(value = """
		select cs.id from class_sessions cs
		join sport_classes sc on sc.id = cs.sport_class_id
		where sc.discipline_id = :disciplineId
		  and (cs.status = 'IN_PROGRESS'
		       or (cs.status = 'SCHEDULED' and cs.start_time > current_timestamp))
		order by cs.start_time, cs.id
		""", nativeQuery = true)
	List<UUID> findBlockingSessionIds(@Param("disciplineId") UUID disciplineId);
}
