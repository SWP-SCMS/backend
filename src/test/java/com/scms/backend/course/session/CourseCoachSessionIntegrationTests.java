package com.scms.backend.course.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.VersionRequest;
import com.scms.backend.course.management.CourseCreateRequest;
import com.scms.backend.course.management.CourseManagementService;
import com.scms.backend.course.management.CourseResponse;
import com.scms.backend.course.run.CourseRunCreateRequest;
import com.scms.backend.course.run.CourseRunOpenRequest;
import com.scms.backend.course.run.CourseRunResponse;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CourseCoachSessionIntegrationTests {

	private static final Instant NOW = Instant.parse("2035-01-01T10:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	CourseCoachSessionService discovery;

	@Autowired
	CourseManagementService management;

	@Autowired
	AccountRepository accounts;

	@Autowired
	JdbcTemplate db;

	@MockitoBean
	Clock clock;

	private UUID managerId;

	private UUID coachId;

	private UUID otherCoachId;

	@BeforeEach
	void setUp() {
		when(clock.instant()).thenReturn(NOW);
		managerId = account(AccountRole.MANAGER).getId();
		coachId = account(AccountRole.COACH).getId();
		otherCoachId = account(AccountRole.COACH).getId();
	}

	@Test
	void assignedCoachGetsJoinedCourseSessionAndOtherCoachGetsHiddenNotFound() {
		UUID disciplineId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", disciplineId, "Discovery " + disciplineId);
		CourseResponse course = management.createCourse(managerId, new CourseCreateRequest("DISC-" + shortId(), "Discovery course", null, disciplineId, null, 1));
		course = management.publishCourse(managerId, course.id(), new VersionRequest(course.version()));
		CourseRunResponse run = management.createRun(managerId, new CourseRunCreateRequest("RUN-" + shortId(), course.id(), coachId, BigInteger.valueOf(100_000), 10, 1, NOW.minusSeconds(7200), NOW.minusSeconds(3600), NOW.minusSeconds(1800), NOW.plusSeconds(7200), 1, true));
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values(?,?,10,'EXCLUSIVE','ACTIVE')", roomId, "Discovery room " + roomId);
		CourseSessionResponse session = management.createSession(managerId, run.id(), new CourseSessionCreateRequest(1, roomId, coachId, NOW.minusSeconds(600), NOW.plusSeconds(600), BigInteger.TEN));
		when(clock.instant()).thenReturn(NOW.minusSeconds(7200));
		run = management.openRun(managerId, run.id(), new CourseRunOpenRequest(run.version(), "DISCOVERY-POLICY"));
		when(clock.instant()).thenReturn(NOW);
		db.update("update course_runs set status='IN_PROGRESS' where id=?", run.id());
		var page = discovery.list(coachId, NOW.minusSeconds(3600), NOW.plusSeconds(3600), "SCHEDULED", 0, 20, List.of("startsAt,asc"));
		assertThat(page.content()).extracting(CourseCoachSessionApiModels.CourseCoachSessionResponse::courseSessionId).containsExactly(session.id());
		assertThat(page.content().get(0).rosterCount()).isZero();
		assertThat(page.content().get(0).attendanceWritable()).isTrue();
		assertThat(page.content().get(0).attendanceWriteReason()).isEqualTo(CourseCoachSessionApiModels.AttendanceWriteReason.WRITABLE);
		assertThatThrownBy(() -> discovery.detail(otherCoachId, session.id())).isInstanceOfSatisfying(CourseException.class, error -> assertThat(error.getCode()).isEqualTo("COURSE_SESSION_NOT_FOUND"));
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " discovery test", "09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private static String shortId() {
		return UUID.randomUUID().toString().substring(0, 8);
	}
}
