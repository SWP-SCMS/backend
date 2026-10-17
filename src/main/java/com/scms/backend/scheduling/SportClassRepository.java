package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SportClassRepository extends JpaRepository<SportClass, UUID> {

	boolean existsByDisciplineIdAndName(UUID disciplineId, String name);

	boolean existsByDisciplineIdAndNameAndIdNot(UUID disciplineId, String name, UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select sportClass from SportClass sportClass where sportClass.discipline.id = :disciplineId "
		+ "order by sportClass.id")
	java.util.List<SportClass> findByDisciplineIdForUpdate(@Param("disciplineId") UUID disciplineId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select sportClass from SportClass sportClass where sportClass.id = :id")
	java.util.Optional<SportClass> findByIdForUpdate(@Param("id") UUID id);

	@Query(value = """
		select cs.id as "sessionId", sc.id as "classId", sc.name as "className",
			cs.start_time as "startTime", cs.end_time as "endTime"
		from class_sessions cs
		join sport_classes sc on sc.id = cs.sport_class_id
		where sc.id = :classId
		  and cs.status in ('SCHEDULED', 'IN_PROGRESS')
		  and cs.end_time > current_timestamp
		order by cs.start_time, cs.id
		""", nativeQuery = true)
	List<DisciplineRepository.BlockingSessionView> findBlockingSessions(@Param("classId") UUID classId);
}
