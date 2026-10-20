package com.scms.backend.scheduling;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select discipline from Discipline discipline where discipline.id = :id")
	java.util.Optional<Discipline> findByIdForUpdate(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("""
		select discipline
		from ClassSession session
		join session.sportClass sportClass
		join sportClass.discipline discipline
		where session.id = :sessionId
		""")
	java.util.Optional<Discipline> findBySessionIdForUpdate(@Param("sessionId") UUID sessionId);

	@Query("""
		select distinct discipline
		from Discipline discipline
		left join fetch discipline.requiredBenefitCodes
		order by discipline.name, discipline.id
		""")
	List<Discipline> findAllWithRequiredBenefitCodes();

	@Query(value = """
		select cs.id as "sessionId", sc.id as "classId", sc.name as "className",
			cs.start_time as "startTime", cs.end_time as "endTime"
		from class_sessions cs
		join sport_classes sc on sc.id = cs.sport_class_id
		where sc.discipline_id = :disciplineId
		  and cs.status in ('SCHEDULED', 'IN_PROGRESS')
		  and cs.end_time > current_timestamp
		order by cs.start_time, cs.id
		""", nativeQuery = true)
	List<BlockingSessionView> findBlockingSessions(@Param("disciplineId") UUID disciplineId);

	interface BlockingSessionView {
		UUID getSessionId();
		UUID getClassId();
		String getClassName();
		Instant getStartTime();
		Instant getEndTime();
	}
}
