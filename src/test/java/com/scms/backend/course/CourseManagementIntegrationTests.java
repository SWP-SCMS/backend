package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CourseManagementIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService service;
	@Autowired CourseRepository courses;
	@Autowired CourseRunRepository runs;
	@Autowired CourseSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	private UUID managerId;
	private UUID disciplineId;
	private UUID coachId;

	@BeforeEach
	void setUp() {
		managerId = account(AccountRole.MANAGER, "Course manager").getId();
		coachId = account(AccountRole.COACH, "Course coach").getId();
		disciplineId = UUID.randomUUID();
		jdbc.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')",
			disciplineId, "Course discipline " + disciplineId);
	}

	@Test
	void rejectsStaleUpdatesAndDuplicateCourseAndRunCodes() {
		CourseResponse course = createPublishedCourse("CAT-101");
		assertThatThrownBy(() -> service.patchCourse(managerId, course.id(),
			patch("{\"expectedVersion\":%d,\"name\":\"Stale\"}".formatted(course.version() - 1))))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_CHANGED"));
		assertThatThrownBy(() -> service.createCourse(managerId,
			new CourseCreateRequest("CAT-101", "Duplicate", null, disciplineId, null, 2)))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_CODE_CONFLICT"));

		CourseRunCreateRequest run = runRequest(course.id(), "CAT-RUN-1");
		service.createRun(managerId, run);
		assertThatThrownBy(() -> service.createRun(managerId, run))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_RUN_CODE_CONFLICT"));
	}

	@Test
	void sameValueCoursePatchLeavesStoredVersionAndAuditCountUnchanged() {
		CourseResponse created = service.createCourse(managerId,
			new CourseCreateRequest("NOOP-101", "No-op course", "Description", disciplineId,
				"https://example.test/noop.png", 2));
		long auditCount = auditCount("COURSE_UPDATED", created.id());

		CourseResponse response = service.patchCourse(managerId, created.id(), patch("""
			{"expectedVersion":%d,"name":" No-op course ","description":" Description ",
			 "imageUrl":" https://example.test/noop.png ","defaultMinimumEnrollment":2}
			""".formatted(created.version())));
		Course stored = courses.findById(created.id()).orElseThrow();

		assertThat(response.version()).isEqualTo(created.version());
		assertThat(stored.getVersion()).isEqualTo(created.version());
		assertThat(auditCount("COURSE_UPDATED", created.id())).isEqualTo(auditCount);
	}

	@Test
	void explicitNullCoursePatchClearsStoredNullableValues() {
		CourseResponse created = service.createCourse(managerId,
			new CourseCreateRequest("CLEAR-101", "Clear course", "Description", disciplineId,
				"https://example.test/clear.png", 2));

		service.patchCourse(managerId, created.id(), patch("""
			{"expectedVersion":%d,"description":null,"imageUrl":null,"defaultMinimumEnrollment":null}
			""".formatted(created.version())));
		Course stored = courses.findById(created.id()).orElseThrow();

		assertThat(stored.getDescription()).isNull();
		assertThat(stored.getImageUrl()).isNull();
		assertThat(stored.getDefaultMinimumEnrollment()).isNull();
	}

	@Test
	void sharedSessionConflictUsesCourseSessionLocksAndOverlapChecks() {
		CourseResponse course = createPublishedCourse("LOCK-101");
		CourseRunResponse run = service.createRun(managerId, runRequest(course.id(), "LOCK-RUN-1"));
		UUID roomId = UUID.randomUUID();
		jdbc.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,20,'EXCLUSIVE','ACTIVE')",
			roomId, "Course room " + roomId);
		Instant start = Instant.parse("2030-02-01T10:00:00Z");
		service.createSession(managerId, run.id(),
			new CourseSessionCreateRequest(1, roomId, coachId, start, start.plusSeconds(3600), BigInteger.TEN));

		assertThatThrownBy(() -> service.createSession(managerId, run.id(),
			new CourseSessionCreateRequest(2, roomId, coachId, start.plusSeconds(60),
				start.plusSeconds(3660), BigInteger.TEN)))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_SESSION_RESOURCE_CONFLICT"));
	}

	@Test
	void auditFailureRollsBackCourseCreationAtomically() {
		long before = courses.count();
		String constraint = "chk_test_reject_course_audit";
		jdbc.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_CREATED') not valid");
		try {
			assertThatThrownBy(() -> service.createCourse(managerId,
				new CourseCreateRequest("ROLLBACK-101", "Rollback", null, disciplineId, null, 2)))
				.rootCause().hasMessageContaining(constraint);
			assertThat(courses.count()).isEqualTo(before);
			assertThat(jdbc.queryForObject("select count(*) from audit_events where action='COURSE_CREATED'"
				+ " and after_data->>'code'='ROLLBACK-101'", Long.class)).isZero();
		}
		finally {
			jdbc.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	@Test
	void auditFailureRollsBackCourseUpdateAtomically() {
		CourseResponse created = service.createCourse(managerId,
			new CourseCreateRequest("UPDATE-ROLLBACK-101", "Original", null, disciplineId, null, 2));
		String constraint = "chk_test_reject_course_update_audit";
		jdbc.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_UPDATED') not valid");
		try {
			assertThatThrownBy(() -> service.patchCourse(managerId, created.id(),
				patch("{\"expectedVersion\":%d,\"name\":\"Changed\"}".formatted(created.version()))))
				.rootCause().hasMessageContaining(constraint);
			Course stored = courses.findById(created.id()).orElseThrow();
			assertThat(stored.getName()).isEqualTo("Original");
			assertThat(stored.getVersion()).isEqualTo(created.version());
			assertThat(auditCount("COURSE_UPDATED", created.id())).isZero();
		}
		finally {
			jdbc.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	@Test
	void auditFailureRollsBackCourseRunMutationAndVersion() {
		CourseResponse course = createPublishedCourse("RUN-ROLLBACK-101");
		CourseRunResponse created = service.createRun(managerId, runRequest(course.id(), "RUN-ROLLBACK-1"));
		String constraint = "chk_test_reject_course_run_update_audit";
		jdbc.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_RUN_UPDATED') not valid");
		try {
			assertThatThrownBy(() -> service.patchRun(managerId, created.id(),
				new CourseRunPatchRequest(created.version(), 4)))
				.rootCause().hasMessageContaining(constraint);
			CourseRun stored = runs.findDetailedById(created.id()).orElseThrow();
			assertThat(stored.getMinimumEnrollment()).isEqualTo(created.minimumEnrollment());
			assertThat(stored.getVersion()).isEqualTo(created.version());
			assertThat(auditCount("COURSE_RUN_UPDATED", created.id())).isZero();
		}
		finally {
			jdbc.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	@Test
	void auditFailureRollsBackCourseSessionMutationAndVersion() {
		CourseResponse course = createPublishedCourse("SESSION-ROLLBACK-101");
		CourseRunResponse run = service.createRun(managerId, runRequest(course.id(), "SESSION-ROLLBACK-1"));
		UUID roomId = UUID.randomUUID();
		jdbc.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,20,'EXCLUSIVE','ACTIVE')",
			roomId, "Rollback room " + roomId);
		Instant originalStart = Instant.parse("2030-02-01T10:00:00Z");
		CourseSessionResponse created = service.createSession(managerId, run.id(),
			new CourseSessionCreateRequest(1, roomId, coachId, originalStart,
				originalStart.plusSeconds(3600), BigInteger.TEN));
		Instant changedStart = originalStart.plusSeconds(7200);
		String constraint = "chk_test_reject_course_session_update_audit";
		jdbc.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_SESSION_RESCHEDULED') not valid");
		try {
			assertThatThrownBy(() -> service.patchSession(managerId, run.id(), created.id(),
				new CourseSessionPatchRequest(created.version(), null, null, changedStart,
					changedStart.plusSeconds(3600))))
				.rootCause().hasMessageContaining(constraint);
			CourseSession stored = sessions.findByIdAndCourseRunId(created.id(), run.id()).orElseThrow();
			assertThat(stored.getStartAt()).isEqualTo(created.startAt());
			assertThat(stored.getEndAt()).isEqualTo(created.endAt());
			assertThat(stored.getVersion()).isEqualTo(created.version());
			assertThat(auditCount("COURSE_SESSION_RESCHEDULED", created.id())).isZero();
		}
		finally {
			jdbc.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	private CourseResponse createPublishedCourse(String code) {
		CourseResponse created = service.createCourse(managerId,
			new CourseCreateRequest(code, "Course " + code, null, disciplineId, null, 2));
		return service.publishCourse(managerId, created.id(), new VersionRequest(created.version()));
	}

	private CourseRunCreateRequest runRequest(UUID courseId, String code) {
		return new CourseRunCreateRequest(code, courseId, coachId, BigInteger.valueOf(1_000_000), 20, 2,
			Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-15T00:00:00Z"),
			Instant.parse("2030-02-01T00:00:00Z"), Instant.parse("2030-03-01T00:00:00Z"), 10, true);
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private CoursePatchRequest patch(String json) {
		try {
			return new ObjectMapper().readValue(json, CoursePatchRequest.class);
		}
		catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}

	private long auditCount(String action, UUID targetId) {
		return jdbc.queryForObject("select count(*) from audit_events where action=? and target_id=?",
			Long.class, action, targetId);
	}
}
