package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

class CourseCatalogRepositoryContractTests {

	@Test
	void courseCatalogQueriesFilterStatusAndFetchDiscipline() throws Exception {
		assertCatalogQuery(CourseRepository.class.getMethod("findCatalogByStatus", CourseStatus.class,
			Pageable.class), "course.status = :status", "discipline");
		assertCatalogQuery(CourseRepository.class.getMethod("findCatalogByIdAndStatus", UUID.class,
			CourseStatus.class), "course.id = :id", "discipline");
	}

	@Test
	void runCatalogQueriesFilterRunAndParentStatusAndFetchDisplayRelations() throws Exception {
		assertRunQuery(CourseRunRepository.class.getMethod("findCatalogByStatusAndCourseStatus",
			CourseRunStatus.class, CourseStatus.class, Pageable.class));
		assertRunQuery(CourseRunRepository.class.getMethod("findCatalogByIdAndStatusAndCourseStatus",
			UUID.class, CourseRunStatus.class, CourseStatus.class));
	}

	@Test
	void scheduleQueryHasDeterministicOrderAndFetchesDisplayRelations() throws Exception {
		Method method = CourseSessionRepository.class.getMethod("findByCourseRunIdOrderByStartAtAscIdAsc", UUID.class);
		assertThat(method.getAnnotation(EntityGraph.class).attributePaths())
			.contains("room", "coach");
	}

	private void assertCatalogQuery(Method method, String clause, String graphPath) {
		assertThat(method.getAnnotation(Query.class).value().toLowerCase()).contains(clause);
		assertThat(method.getAnnotation(EntityGraph.class).attributePaths()).contains(graphPath);
	}

	private void assertRunQuery(Method method) {
		String query = method.getAnnotation(Query.class).value().toLowerCase();
		assertThat(query).contains("run.status = :status", "run.course.status = :coursestatus");
		assertThat(method.getAnnotation(EntityGraph.class).attributePaths())
			.contains("course", "course.discipline", "primaryCoach");
	}
}
