package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class CourseCatalogIntegrationTests {

	private static final UUID DISCIPLINE_ID = uuid(1);
	private static final UUID COACH_ID = uuid(2);
	private static final UUID ROOM_ID = uuid(3);
	private static final UUID PUBLISHED_ALPHA_ID = uuid(10);
	private static final UUID PUBLISHED_BETA_ID = uuid(11);
	private static final UUID HIDDEN_COURSE_ID = uuid(12);
	private static final UUID FIRST_VISIBLE_RUN_ID = uuid(20);
	private static final UUID SECOND_VISIBLE_RUN_ID = uuid(21);
	private static final UUID HIDDEN_PARENT_RUN_ID = uuid(22);
	private static final UUID CONFIRMED_RUN_ID = uuid(42);
	private static final UUID EARLIER_SESSION_ID = uuid(30);
	private static final UUID LATER_SESSION_ID = uuid(31);
	private static final Instant REGISTRATION_OPENS = Instant.parse("2030-01-01T00:00:00Z");
	private static final Instant REGISTRATION_CLOSES = Instant.parse("2030-01-15T00:00:00Z");
	private static final Instant PLANNED_START = Instant.parse("2030-02-01T00:00:00Z");
	private static final Instant PLANNED_END = Instant.parse("2030-03-01T00:00:00Z");
	private static final Instant SESSION_START = Instant.parse("2030-02-10T10:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseCatalogService service;
	@Autowired JdbcTemplate jdbc;
	@Autowired EntityManager entityManager;
	@Autowired EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void seedCatalog() {
		jdbc.update("insert into disciplines(id,name,status) values (?,?, 'ACTIVE')",
			DISCIPLINE_ID, "Catalog integration discipline");
		jdbc.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values (?, 'COACH', 'ACTIVE', ?, ?, ?, date '1990-01-01', '{noop}password')
			""", COACH_ID, "Catalog Coach", "0900000002", "catalog-coach@example.test");
		jdbc.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,20,'EXCLUSIVE','ACTIVE')",
			ROOM_ID, "Catalog room");
		course(PUBLISHED_ALPHA_ID, "CAT-ALPHA", "Alpha", "PUBLISHED");
		course(PUBLISHED_BETA_ID, "CAT-BETA", "Beta", "PUBLISHED");
		course(HIDDEN_COURSE_ID, "CAT-HIDDEN", "Hidden", "DRAFT");

		run(FIRST_VISIBLE_RUN_ID, PUBLISHED_ALPHA_ID, "CAT-RUN-OPEN-1", CourseRunStatus.OPEN_FOR_REGISTRATION,
			PLANNED_START);
		run(SECOND_VISIBLE_RUN_ID, PUBLISHED_BETA_ID, "CAT-RUN-OPEN-2", CourseRunStatus.OPEN_FOR_REGISTRATION,
			PLANNED_START.plusSeconds(3600));
		run(HIDDEN_PARENT_RUN_ID, HIDDEN_COURSE_ID, "CAT-RUN-HIDDEN-PARENT",
			CourseRunStatus.OPEN_FOR_REGISTRATION, PLANNED_START.plusSeconds(7200));
		int offset = 3;
		for (CourseRunStatus status : CourseRunStatus.values()) {
			if (status == CourseRunStatus.OPEN_FOR_REGISTRATION) continue;
			run(uuid(40 + status.ordinal()), PUBLISHED_ALPHA_ID, "CAT-RUN-" + status, status,
				PLANNED_START.plusSeconds(3600L * offset++));
		}

		session(LATER_SESSION_ID, 1);
		session(EARLIER_SESSION_ID, 2);
		entityManager.flush();
		entityManager.clear();
	}

	@Test
	void realPostgresFiltersCatalogAndOrdersSameTimeSessionsByIdWithFixedQueryCount() {
		CatalogCoursePageResponse courses = service.listCourses(PageRequest.of(0, 20,
			Sort.by("name").and(Sort.by("id"))));
		assertThat(courses.content()).extracting(CatalogCourseResponse::id)
			.containsExactly(PUBLISHED_ALPHA_ID, PUBLISHED_BETA_ID);
		assertThatThrownBy(() -> service.getCourse(HIDDEN_COURSE_ID))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_NOT_FOUND"));

		CatalogCourseRunPageResponse runs = service.listRuns(PageRequest.of(0, 20,
			Sort.by("plannedStartAt").and(Sort.by("id"))));
		assertThat(runs.content()).extracting(CatalogCourseRunResponse::id)
			.containsExactly(FIRST_VISIBLE_RUN_ID, SECOND_VISIBLE_RUN_ID);
		assertThatThrownBy(() -> service.getRun(HIDDEN_PARENT_RUN_ID))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_RUN_NOT_FOUND"));
		assertThatThrownBy(() -> service.getRun(CONFIRMED_RUN_ID))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_RUN_NOT_FOUND"));

		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		var statistics = sessionFactory.getStatistics();
		boolean wasEnabled = statistics.isStatisticsEnabled();
		statistics.setStatisticsEnabled(true);
		statistics.clear();
		entityManager.clear();
		try {
			CatalogCourseRunDetailResponse detail = service.getRun(FIRST_VISIBLE_RUN_ID);
			assertThat(detail.sessions()).extracting(CatalogCourseSessionResponse::id)
				.containsExactly(EARLIER_SESSION_ID, LATER_SESSION_ID);
			assertThat(statistics.getPrepareStatementCount())
				.as("one run graph query plus one ordered session graph query").isEqualTo(2);
		}
		finally {
			statistics.setStatisticsEnabled(wasEnabled);
		}
	}

	private void course(UUID id, String code, String name, String status) {
		jdbc.update("""
			insert into courses(id,code,name,discipline_id,status)
			values (?,?,?,?,?)
			""", id, code, name, DISCIPLINE_ID, status);
	}

	private void run(UUID id, UUID courseId, String code, CourseRunStatus status, Instant plannedStart) {
		String policy = status == CourseRunStatus.DRAFT || status == CourseRunStatus.CANCELLED ? null : "POLICY-1";
		jdbc.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
			 minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,planned_end_at,
			 total_units,named_coach_commitment,policy_version_snapshot,status)
			values (?,?,?,?,1000000,20,2,?,?,?,?,10,true,?,?)
			""", id, code, courseId, COACH_ID, timestamp(REGISTRATION_OPENS), timestamp(REGISTRATION_CLOSES),
			timestamp(plannedStart), timestamp(PLANNED_END.plusSeconds(86400)), policy, status.name());
	}

	private void session(UUID id, int unitNumber) {
		jdbc.update("""
			insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at,
			 original_start_at,original_end_at,allocation_vnd,status)
			values (?,?,?,?,?,?,?,?,?,100000,'SCHEDULED')
			""", id, FIRST_VISIBLE_RUN_ID, unitNumber, ROOM_ID, COACH_ID, timestamp(SESSION_START),
			timestamp(SESSION_START.plusSeconds(3600)), timestamp(SESSION_START),
			timestamp(SESSION_START.plusSeconds(3600)));
	}

	private static UUID uuid(int suffix) {
		return UUID.fromString("00000000-0000-0000-0000-%012d".formatted(suffix));
	}

	private static Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}
}
