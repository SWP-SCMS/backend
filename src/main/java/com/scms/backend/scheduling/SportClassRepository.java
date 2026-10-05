package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SportClassRepository extends JpaRepository<SportClass, UUID> {

	boolean existsByDisciplineIdAndName(UUID disciplineId, String name);

	boolean existsByDisciplineIdAndNameAndIdNot(UUID disciplineId, String name, UUID id);
}
