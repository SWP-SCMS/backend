package com.scms.backend.course;

import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRunRepository extends JpaRepository<CourseRun, UUID> {

	boolean existsByRunCode(String runCode);

	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	Page<CourseRun> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	@Query("select run from CourseRun run where run.id = :id")
	java.util.Optional<CourseRun> findDetailedById(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	@Query("select run from CourseRun run where run.id = :id")
	java.util.Optional<CourseRun> findByIdForUpdate(@Param("id") UUID id);
}
