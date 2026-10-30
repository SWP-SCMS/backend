package com.scms.backend.course;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {

	boolean existsByCode(String code);

	@EntityGraph(attributePaths = "discipline")
	Page<Course> findAll(Pageable pageable);

	@EntityGraph(attributePaths = "discipline")
	@Query("select course from Course course where course.id = :id")
	Optional<Course> findDetailedById(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = "discipline")
	@Query("select course from Course course where course.id = :id")
	Optional<Course> findByIdForUpdate(@Param("id") UUID id);
}
