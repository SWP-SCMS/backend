package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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
			new CoursePatchRequest(course.version() - 1, "Stale", null, null, null)))
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
			+ " check (action <> 'COURSE_CREATED')");
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
}
