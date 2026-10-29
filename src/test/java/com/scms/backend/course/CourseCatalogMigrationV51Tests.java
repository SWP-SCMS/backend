package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CourseCatalogMigrationV51Tests {

	private static final Instant REGISTRATION_OPENS = Instant.parse("2026-11-01T00:00:00Z");
	private static final Instant REGISTRATION_CLOSES = Instant.parse("2026-11-15T00:00:00Z");
	private static final Instant PLANNED_START = Instant.parse("2026-11-20T00:00:00Z");
	private static final Instant PLANNED_END = Instant.parse("2027-01-20T00:00:00Z");

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() {
		flyway().clean();
	}

	@Test
	void freshSchemaCreatesCatalogAndEnforcesKeyConstraints() {
		flyway().migrate();
		JdbcTemplate db = db();
		Fixture fixture = insertValidCatalog(db);

		assertThat(db.queryForList("select table_name from information_schema.tables where table_schema='public'",
			String.class)).contains("courses", "course_runs", "course_sessions");
		assertThat(db.queryForObject("select count(*) from course_sessions", Integer.class)).isOne();

		assertThatThrownBy(() -> db.update("""
			insert into courses(id, code, name, discipline_id, default_minimum_enrollment, status)
			values (?, 'COURSE-BAD', 'Invalid course', ?, 0, 'DRAFT')
			""", UUID.randomUUID(), fixture.disciplineId()))
			.isInstanceOf(DataIntegrityViolationException.class);

		assertThatThrownBy(() -> insertRun(db, UUID.randomUUID(), "RUN-BAD-MIN", fixture.courseId(),
			fixture.coachId(), 10, 11, REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertRun(db, UUID.randomUUID(), "RUN-BAD-TIME", fixture.courseId(),
			fixture.coachId(), 10, 3, REGISTRATION_CLOSES, REGISTRATION_OPENS, PLANNED_START, PLANNED_END))
			.isInstanceOf(DataIntegrityViolationException.class);

		assertThatThrownBy(() -> db.update("""
			insert into course_sessions(id, course_run_id, unit_number, room_id, coach_account_id,
				start_at, end_at, original_start_at, original_end_at, allocation_vnd, status)
			values (?, ?, 1, ?, ?, ?, ?, ?, ?, 100000, 'SCHEDULED')
			""", UUID.randomUUID(), fixture.runId(), fixture.roomId(), fixture.coachId(),
			Timestamp.from(PLANNED_START.plusSeconds(7200)), Timestamp.from(PLANNED_START.plusSeconds(10800)),
			Timestamp.from(PLANNED_START.plusSeconds(7200)), Timestamp.from(PLANNED_START.plusSeconds(10800))))
			.isInstanceOf(DataIntegrityViolationException.class);

		assertThatThrownBy(() -> db.update("update course_sessions set replacement_session_id=id where id=?",
			fixture.sessionId())).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("update course_sessions set end_at=start_at where id=?",
			fixture.sessionId())).isInstanceOf(DataIntegrityViolationException.class);
	}

	private Fixture insertValidCatalog(JdbcTemplate db) {
		UUID disciplineId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		UUID runId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		db.update("insert into disciplines(id, name, status) values (?, ?, 'ACTIVE')",
			disciplineId, "Course discipline " + disciplineId);
		insertCoach(db, coachId);
		db.update("insert into rooms(id, name, capacity, status) values (?, ?, 20, 'ACTIVE')",
			roomId, "Course room " + roomId);
		db.update("""
			insert into courses(id, code, name, description, discipline_id, default_minimum_enrollment, status)
			values (?, 'COURSE-001', 'Course one', 'Foundation course', ?, 3, 'PUBLISHED')
			""", courseId, disciplineId);
		insertRun(db, runId, "RUN-001", courseId, coachId, 20, 3,
			REGISTRATION_OPENS, REGISTRATION_CLOSES, PLANNED_START, PLANNED_END);
		db.update("""
			insert into course_sessions(id, course_run_id, unit_number, room_id, coach_account_id,
				start_at, end_at, original_start_at, original_end_at, allocation_vnd, status)
			values (?, ?, 1, ?, ?, ?, ?, ?, ?, 100000, 'SCHEDULED')
			""", sessionId, runId, roomId, coachId, Timestamp.from(PLANNED_START),
			Timestamp.from(PLANNED_START.plusSeconds(3600)), Timestamp.from(PLANNED_START),
			Timestamp.from(PLANNED_START.plusSeconds(3600)));
		return new Fixture(disciplineId, coachId, roomId, courseId, runId, sessionId);
	}

	private void insertRun(JdbcTemplate db, UUID id, String code, UUID courseId, UUID coachId,
			int capacity, int minimumEnrollment, Instant registrationOpens, Instant registrationCloses,
			Instant plannedStart, Instant plannedEnd) {
		db.update("""
			insert into course_runs(id, run_code, course_id, primary_coach_account_id, tuition_vnd,
				capacity, minimum_enrollment, registration_opens_at, registration_closes_at,
				planned_start_at, planned_end_at, total_units, named_coach_commitment,
				policy_version_snapshot, status)
			values (?, ?, ?, ?, 5000000, ?, ?, ?, ?, ?, ?, 12, true, 'POLICY-2026-01', 'OPEN_FOR_REGISTRATION')
			""", id, code, courseId, coachId, capacity, minimumEnrollment, Timestamp.from(registrationOpens),
			Timestamp.from(registrationCloses), Timestamp.from(plannedStart), Timestamp.from(plannedEnd));
	}

	private void insertCoach(JdbcTemplate db, UUID coachId) {
		db.update("""
			insert into accounts(id, role, status, full_name, phone, email, birth_date, password_hash)
			values (?, 'COACH', 'ACTIVE', 'Course Coach', ?, ?, date '1990-01-01', 'unused')
			""", coachId, "09" + coachId.toString().substring(0, 8), coachId + "@example.test");
	}

	private Flyway flyway() {
		return Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false)
			.load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(
			postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	private record Fixture(UUID disciplineId, UUID coachId, UUID roomId, UUID courseId, UUID runId,
		UUID sessionId) {
	}
}
