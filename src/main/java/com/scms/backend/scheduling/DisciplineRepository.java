package com.scms.backend.scheduling;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DisciplineRepository extends JpaRepository<Discipline, UUID> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);
}
