package com.scms.backend.scheduling;

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
import com.scms.backend.course.CourseSessionService;
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
class CourseSharedAvailabilityIntegrationTests {
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired AccountRepository accounts;
	@Autowired DisciplineRepository disciplines;
	@Autowired SportClassRepository classes;
	@Autowired RoomRepository rooms;
	@Autowired CourseSessionService courseSessions;
	@Autowired ClassSessionService gymSessions;
	@Autowired RoomService roomService;
	@Autowired RoomSuspensionService roomSuspensions;
	@Autowired JdbcTemplate jdbc;

	@Test
	void courseAndGymBlockEachOtherByRoomAndCoachButAllowAdjacentAndCompletedWindows() {
		Account manager = account(AccountRole.MANAGER, "Course Manager");
		Account courseCoach = account(AccountRole.COACH, "Course Coach");
		Account gymCoach = account(AccountRole.COACH, "Gym Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Course shared resource",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Shared resource class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Course shared room", 20,
			RoomStatus.ACTIVE, RoomUsageMode.SHARED));
		Room secondRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Course coach room", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		UUID runId = insertRun(discipline.getId(), courseCoach.getId());
		Instant start = Instant.parse("2035-01-10T02:00:00Z"), end = start.plusSeconds(3600);
		var courseSession = courseSessions.create(runId, 1, courseCoach.getId(), room.getId(), start, end,
			BigInteger.valueOf(100_000));

		assertThat(roomService.availability(manager.getId(), start, end, 1, null).stream()
			.filter(candidate -> candidate.roomId().equals(room.getId())).findFirst().orElseThrow().available()).isFalse();
		assertThatThrownBy(() -> gymSessions.create(manager.getId(), new ClassSessionCreateRequest(
			sportClass.getId(), gymCoach.getId(), room.getId(), start, end, 10)))
			.isInstanceOf(RecurringScheduleException.class);

		gymSessions.create(manager.getId(), new ClassSessionCreateRequest(sportClass.getId(), gymCoach.getId(),
			room.getId(), end, end.plusSeconds(3600), 10));
		jdbc.update("update course_sessions set status='COMPLETED' where id=?", courseSession.getId());
		gymSessions.create(manager.getId(), new ClassSessionCreateRequest(sportClass.getId(), gymCoach.getId(),
			room.getId(), start, end, 10));
		assertThatThrownBy(() -> courseSessions.create(runId, 2, courseCoach.getId(), room.getId(), start, end,
			BigInteger.valueOf(100_000))).isInstanceOf(com.scms.backend.course.CourseException.class);

		Instant coachStart = end.plusSeconds(7200), coachEnd = coachStart.plusSeconds(3600);
		gymSessions.create(manager.getId(), new ClassSessionCreateRequest(sportClass.getId(), courseCoach.getId(),
			secondRoom.getId(), coachStart, coachEnd, 10));
		assertThatThrownBy(() -> courseSessions.create(runId, 3, courseCoach.getId(), room.getId(), coachStart,
			coachEnd, BigInteger.valueOf(100_000))).isInstanceOf(com.scms.backend.course.CourseException.class);
	}

	@Test
	void ongoingCourseSessionBlocksRoomDeactivationAndAppearsInPermanentImpact() {
		Account manager = account(AccountRole.MANAGER, "Ongoing Manager");
		Account coach = account(AccountRole.COACH, "Ongoing Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Ongoing discipline",
			null, DisciplineStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Ongoing room", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		UUID runId = insertRun(discipline.getId(), coach.getId());
		var session = courseSessions.create(runId, 1, coach.getId(), room.getId(),
			Instant.parse("2035-01-10T02:00:00Z"), Instant.parse("2035-01-10T03:00:00Z"), BigInteger.TEN);
		Instant now = Instant.now();
		jdbc.update("update course_sessions set start_at=?, end_at=? where id=?", now.minusSeconds(1800),
			now.plusSeconds(1800), session.getId());

		assertThatThrownBy(() -> roomService.update(manager.getId(), room.getId(),
			new RoomPatchRequest(null, null, RoomStatus.INACTIVE))).isInstanceOf(RoomException.class);
		RoomImpactPreviewResponse preview = roomSuspensions.previewImpact(manager.getId(), room.getId(),
			new RoomImpactPreviewRequest(null, null, true));
		assertThat(preview.courseSessionIds()).contains(session.getId());
	}

	private UUID insertRun(UUID disciplineId, UUID coachId) {
		UUID courseId = UUID.randomUUID(), runId = UUID.randomUUID();
		jdbc.update("""
			insert into courses(id, code, name, discipline_id, status)
			values (?, ?, 'Shared Course', ?, 'DRAFT')
			""", courseId, "COURSE-" + courseId, disciplineId);
		jdbc.update("""
			insert into course_runs(id, run_code, course_id, primary_coach_account_id, tuition_vnd,
			 capacity, minimum_enrollment, registration_opens_at, registration_closes_at,
			 planned_start_at, planned_end_at, total_units, named_coach_commitment, status)
			values (?, ?, ?, ?, 100000, 10, 1, ?, ?, ?, ?, 3, false, 'DRAFT')
			""", runId, "RUN-" + runId, courseId, coachId, Instant.parse("2034-12-01T00:00:00Z"),
			Instant.parse("2035-01-01T00:00:00Z"), Instant.parse("2035-01-10T00:00:00Z"),
			Instant.parse("2035-01-11T00:00:00Z"));
		return runId;
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
