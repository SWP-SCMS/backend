package com.scms.backend.course.run;

import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.course.CourseStatus;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRunRepository extends JpaRepository<CourseRun, UUID> {

	boolean existsByRunCode(String runCode);

	@Query("select run.id from CourseRun run where run.course.id = :courseId order by run.id")
	java.util.List<UUID> findIdsByCourseId(@Param("courseId") UUID courseId);

	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	Page<CourseRun> findAll(Pageable pageable);

	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	@Query("""
		select run from CourseRun run
		where run.status = :status and run.course.status = :courseStatus
		""")
	Page<CourseRun> findCatalogByStatusAndCourseStatus(@Param("status") CourseRunStatus status, @Param("courseStatus") CourseStatus courseStatus, Pageable pageable);

	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	@Query("""
		select run from CourseRun run
		where run.id = :id and run.status = :status and run.course.status = :courseStatus
		""")
	java.util.Optional<CourseRun> findCatalogByIdAndStatusAndCourseStatus(@Param("id") UUID id, @Param("status") CourseRunStatus status, @Param("courseStatus") CourseStatus courseStatus);

	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	@Query("select run from CourseRun run where run.id = :id")
	java.util.Optional<CourseRun> findDetailedById(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "course", "course.discipline", "primaryCoach" })
	@Query("select run from CourseRun run where run.id = :id")
	java.util.Optional<CourseRun> findByIdForUpdate(@Param("id") UUID id);
}
