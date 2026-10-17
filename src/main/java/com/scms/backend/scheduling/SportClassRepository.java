package com.scms.backend.scheduling;

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
}
