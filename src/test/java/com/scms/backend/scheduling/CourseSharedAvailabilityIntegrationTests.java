package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.CourseSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
	@Autowired ClassSessionRepository classSessions;
	@Autowired RoomService roomService;
	@Autowired RoomSuspensionService roomSuspensions;
	@Autowired JdbcTemplate jdbc;
	@MockitoBean SharedResourceLockRendezvous lockRendezvous;

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

	@Test
	void concurrentCourseAndGymCreationSerializesOnSharedRoomWithoutDeadlock() throws Exception {
		Account manager = account(AccountRole.MANAGER, "Race Manager");
		Account courseCoach = account(AccountRole.COACH, "Race Course Coach");
		Account gymCoach = account(AccountRole.COACH, "Race Gym Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Race discipline",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Race class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room room = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Race room", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		UUID runId = insertRun(discipline.getId(), courseCoach.getId());
		Instant start = Instant.parse("2035-01-12T02:00:00Z"), end = start.plusSeconds(3600);
		CountDownLatch resourceLocked = new CountDownLatch(1), gymAtLock = new CountDownLatch(1),
			releaseCourse = new CountDownLatch(1);
		AtomicInteger coursePid = new AtomicInteger(), gymPid = new AtomicInteger();
		doAnswer(invocation -> {
			if (Thread.currentThread().getName().equals("gym-race")) {
				gymPid.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
				gymAtLock.countDown();
			}
			else if (Thread.currentThread().getName().equals("course-race")) {
				coursePid.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
			}
			return null;
		}).when(lockRendezvous).beforeLock();
		doAnswer(invocation -> {
			if (Thread.currentThread().getName().equals("course-race")) {
				resourceLocked.countDown();
				await(releaseCourse);
			}
			return null;
		}).when(lockRendezvous).afterLock();
		var executor = Executors.newFixedThreadPool(2);
		try {
			var course = executor.submit(() -> {
				Thread.currentThread().setName("course-race");
				return courseSessions.create(runId, 1, courseCoach.getId(), room.getId(), start, end,
					BigInteger.valueOf(100_000));
			});
			assertThat(resourceLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var gym = executor.submit(() -> {
				Thread.currentThread().setName("gym-race");
				try {
					gymSessions.create(manager.getId(), new ClassSessionCreateRequest(sportClass.getId(),
						gymCoach.getId(), room.getId(), start, end, 10));
					return null;
				}
				catch (Throwable failure) { return failure; }
			});
			assertThat(gymAtLock.await(10, TimeUnit.SECONDS)).isTrue();
			awaitDatabaseLock(gymPid.get(), coursePid.get());
			releaseCourse.countDown();

			assertThat(course.get(10, TimeUnit.SECONDS)).isNotNull();
			assertThat(gym.get(10, TimeUnit.SECONDS)).isInstanceOf(RecurringScheduleException.class);
		}
		finally {
			releaseCourse.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void roomlessPtSessionCanChangeCoach() {
		Account manager = account(AccountRole.MANAGER, "PT Manager");
		Account firstCoach = account(AccountRole.COACH, "PT Coach One");
		Account secondCoach = account(AccountRole.COACH, "PT Coach Two");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "PT discipline",
			null, DisciplineStatus.ACTIVE));
		SportClass pt = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline, "PT class",
			SportClassType.PT_1_1, null, SportClassStatus.ACTIVE));
		Instant start = Instant.parse("2035-01-14T02:00:00Z");
		ClassSession session = classSessions.saveAndFlush(new ClassSession(UUID.randomUUID(), pt, null, firstCoach,
			null, start, start.plusSeconds(3600), 1, manager.getId()));

		ClassSessionDetailResponse changed = gymSessions.updateAssignment(manager.getId(), session.getId(),
			new ClassSessionAssignmentRequest(secondCoach.getId(), null));

		assertThat(changed.coachId()).isEqualTo(secondCoach.getId());
		assertThat(changed.roomId()).isNull();
	}

	@Test
	void courseRescheduleRejectsGymConflictAndCancelledPendingMakeupReleasesResources() {
		Account manager = account(AccountRole.MANAGER, "Cross reschedule Manager");
		Account coach = account(AccountRole.COACH, "Cross reschedule Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Cross reschedule discipline",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Cross reschedule class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room firstRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Cross reschedule room 1", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		Room secondRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Cross reschedule room 2", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		UUID runId = insertRun(discipline.getId(), coach.getId());
		Instant originalStart = Instant.parse("2035-01-15T02:00:00Z");
		var course = courseSessions.create(runId, 1, coach.getId(), firstRoom.getId(), originalStart,
			originalStart.plusSeconds(3600), BigInteger.TEN);
		Instant gymStart = originalStart.plusSeconds(7200);
		gymSessions.create(manager.getId(), new ClassSessionCreateRequest(sportClass.getId(), coach.getId(),
			secondRoom.getId(), gymStart, gymStart.plusSeconds(3600), 10));

		assertThatThrownBy(() -> courseSessions.reschedule(course.getId(), firstRoom.getId(), coach.getId(),
			gymStart, gymStart.plusSeconds(3600))).isInstanceOf(com.scms.backend.course.CourseException.class);

		jdbc.update("update course_sessions set status='CANCELLED_PENDING_MAKEUP' where id=?", course.getId());
		assertThat(gymSessions.create(manager.getId(), new ClassSessionCreateRequest(sportClass.getId(), coach.getId(),
			firstRoom.getId(), originalStart, originalStart.plusSeconds(3600), 10))).isNotNull();
	}

	@Test
	void roomImpactRejectsCourseConflictWithoutPartialGymMutation() {
		Account manager = account(AccountRole.MANAGER, "Impact rollback Manager");
		Account coach = account(AccountRole.COACH, "Impact rollback Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Impact rollback discipline",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Impact rollback class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room source = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Impact rollback source", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		Room target = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Impact rollback target", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		Instant start = Instant.parse("2035-01-16T02:00:00Z"), end = start.plusSeconds(3600);
		ClassSession gym = classSessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			source, start, end, 10, manager.getId()));
		UUID runId = insertRun(discipline.getId(), coach.getId());
		courseSessions.create(runId, 1, coach.getId(), source.getId(), start, end, BigInteger.TEN);
		RoomImpactPreviewRequest request = new RoomImpactPreviewRequest(start.minusSeconds(60), end.plusSeconds(60),
			false, "Maintenance", List.of(), new RoomBulkResolutionRequest("MOVE", target.getId(), "Move"), List.of());

		assertThatThrownBy(() -> roomSuspensions.applyImpact(manager.getId(), source.getId(), request))
			.isInstanceOf(RoomException.class);
		assertThat(jdbc.queryForObject("select room_id from class_sessions where id=?", UUID.class, gym.getId()))
			.isEqualTo(source.getId());
		assertThat(jdbc.queryForObject("select count(*) from room_suspensions where room_id=?", Long.class,
			source.getId())).isZero();
	}

	@Test
	void roomImpactRescheduleAndCourseCreateSerializeOnCoach() throws Exception {
		Account manager = account(AccountRole.MANAGER, "Impact race Manager");
		Account coach = account(AccountRole.COACH, "Impact race Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Impact race discipline",
			null, DisciplineStatus.ACTIVE));
		SportClass sportClass = classes.saveAndFlush(new SportClass(UUID.randomUUID(), discipline,
			"Impact race class", SportClassType.GROUP, null, SportClassStatus.ACTIVE));
		Room source = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Impact race source", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		Room courseRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Impact race course", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		Instant originalStart = Instant.parse("2035-01-17T02:00:00Z"), originalEnd = originalStart.plusSeconds(3600);
		Instant newStart = originalStart.plusSeconds(7200), newEnd = newStart.plusSeconds(3600);
		ClassSession gym = classSessions.saveAndFlush(new ClassSession(UUID.randomUUID(), sportClass, null, coach,
			source, originalStart, originalEnd, 10, manager.getId()));
		UUID runId = insertRun(discipline.getId(), coach.getId());
		RoomImpactPreviewRequest request = new RoomImpactPreviewRequest(originalStart.minusSeconds(60),
			originalEnd.plusSeconds(60), false, "Maintenance", List.of(new RoomSessionResolutionRequest(gym.getId(),
				"RESCHEDULE", null, newStart, newEnd, "Move later")));
		CountDownLatch impactLocked = new CountDownLatch(1), courseAtLock = new CountDownLatch(1),
			releaseImpact = new CountDownLatch(1);
		AtomicInteger impactPid = new AtomicInteger(), coursePid = new AtomicInteger();
		doAnswer(invocation -> {
			if (Thread.currentThread().getName().equals("impact-race")) {
				impactPid.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
			}
			else if (Thread.currentThread().getName().equals("course-impact-race")) {
				coursePid.set(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
				courseAtLock.countDown();
			}
			return null;
		}).when(lockRendezvous).beforeLock();
		doAnswer(invocation -> {
			if (Thread.currentThread().getName().equals("impact-race")) {
				impactLocked.countDown();
				await(releaseImpact);
			}
			return null;
		}).when(lockRendezvous).afterLock();
		var executor = Executors.newFixedThreadPool(2);
		try {
			var impact = executor.submit(() -> {
				Thread.currentThread().setName("impact-race");
				return roomSuspensions.applyImpact(manager.getId(), source.getId(), request);
			});
			assertThat(impactLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var course = executor.submit(() -> {
				Thread.currentThread().setName("course-impact-race");
				try {
					courseSessions.create(runId, 1, coach.getId(), courseRoom.getId(), newStart, newEnd,
						BigInteger.TEN);
					return null;
				}
				catch (Throwable failure) { return failure; }
			});
			assertThat(courseAtLock.await(10, TimeUnit.SECONDS)).isTrue();
			awaitDatabaseLock(coursePid.get(), impactPid.get());
			releaseImpact.countDown();

			assertThat(impact.get(10, TimeUnit.SECONDS)).isNotNull();
			assertThat(course.get(10, TimeUnit.SECONDS)).isInstanceOf(com.scms.backend.course.CourseException.class);
		}
		finally {
			releaseImpact.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void courseRescheduleExcludesItselfAndRejectsAnotherCourseCoachConflict() {
		Account coach = account(AccountRole.COACH, "Reschedule Coach");
		Discipline discipline = disciplines.saveAndFlush(new Discipline(UUID.randomUUID(), "Reschedule discipline",
			null, DisciplineStatus.ACTIVE));
		Room firstRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Reschedule room 1", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		Room secondRoom = rooms.saveAndFlush(new Room(UUID.randomUUID(), "Reschedule room 2", 20,
			RoomStatus.ACTIVE, RoomUsageMode.EXCLUSIVE));
		UUID runId = insertRun(discipline.getId(), coach.getId());
		Instant firstStart = Instant.parse("2035-01-13T02:00:00Z");
		var first = courseSessions.create(runId, 1, coach.getId(), firstRoom.getId(), firstStart,
			firstStart.plusSeconds(3600), BigInteger.TEN);
		Instant shifted = firstStart.plusSeconds(1800);
		courseSessions.reschedule(first.getId(), firstRoom.getId(), coach.getId(), shifted,
			shifted.plusSeconds(3600));
		Instant secondStart = firstStart.plusSeconds(7200);
		courseSessions.create(runId, 2, coach.getId(), secondRoom.getId(), secondStart,
			secondStart.plusSeconds(3600), BigInteger.TEN);

		assertThatThrownBy(() -> courseSessions.reschedule(first.getId(), firstRoom.getId(), coach.getId(),
			secondStart, secondStart.plusSeconds(3600))).isInstanceOf(com.scms.backend.course.CourseException.class);
	}

	private static void await(CountDownLatch latch) {
		try { if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("latch timeout"); }
		catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new IllegalStateException(exception); }
	}

	private void awaitDatabaseLock(int waiterPid, int blockerPid) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			Long waiting = jdbc.queryForObject("""
				select count(*) from pg_stat_activity
				where pid=? and wait_event_type='Lock' and ?=any(pg_blocking_pids(pid))
				""", Long.class, waiterPid, blockerPid);
			if (waiting != null && waiting == 1) return;
			Thread.onSpinWait();
		} while (System.nanoTime() < deadline);
		throw new AssertionError("Timed out waiting for Gym to block on the Course resource lock");
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
			Instant.parse("2035-02-01T00:00:00Z"));
		return runId;
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
