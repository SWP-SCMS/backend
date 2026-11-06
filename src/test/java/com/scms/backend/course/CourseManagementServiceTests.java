package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineRepository;
import com.scms.backend.scheduling.DisciplineStatus;
import com.scms.backend.scheduling.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CourseManagementServiceTests {

	private final CourseRepository courses = mock(CourseRepository.class);
	private final CourseRunRepository runs = mock(CourseRunRepository.class);
	private final CourseSessionRepository sessions = mock(CourseSessionRepository.class);
	private final DisciplineRepository disciplines = mock(DisciplineRepository.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final CourseSessionService sessionService = mock(CourseSessionService.class);
	private final CourseRunClosureService runClosure = mock(CourseRunClosureService.class);
	private final CourseManagementService service = new CourseManagementService(courses, runs, sessions,
		disciplines, accounts, audits, sessionService, runClosure);
	private UUID managerId;

	@Test
	void deactivatingCourseCancelsOnlyUnstartedRunsAndPreservesExactStartBoundary() {
		Instant now = Instant.parse("2029-01-01T00:00:00Z");
		var cascadeService = new CourseManagementService(courses, runs, sessions, disciplines, accounts, audits,
			sessionService, runClosure, null, null, java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));
		Course course = run(0L, 1).getCourse();
		course.publish();
		ReflectionTestUtils.setField(course, "version", 0L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));
		var children = new java.util.ArrayList<CourseRun>();
		var states = new CourseRunStatus[]{CourseRunStatus.DRAFT, CourseRunStatus.OPEN_FOR_REGISTRATION,
			CourseRunStatus.CONFIRMED, CourseRunStatus.IN_PROGRESS, CourseRunStatus.COMPLETED, CourseRunStatus.CANCELLED,
			CourseRunStatus.CONFIRMED, CourseRunStatus.OPEN_FOR_REGISTRATION};
		for (int i = 0; i < states.length; i++) {
			CourseRun child = run(0L, 1);
			ReflectionTestUtils.setField(child, "course", course);
			ReflectionTestUtils.setField(child, "status", states[i]);
			ReflectionTestUtils.setField(child, "plannedStartAt", i == 6 ? now : i == 7 ? now.minusSeconds(1) : now.plusSeconds(60));
			when(runs.findByIdForUpdate(child.getId())).thenReturn(Optional.of(child));
			children.add(child);
		}
		when(runs.findIdsByCourseId(course.getId())).thenReturn(children.stream().map(CourseRun::getId).toList());
		cascadeService.deactivateCourse(managerId, course.getId(), new VersionRequest(0L));
		assertThat(course.getStatus()).isEqualTo(CourseStatus.INACTIVE);
		for (int i = 0; i < children.size(); i++) {
			CourseRun child = children.get(i);
			if (i < 3) {
				assertThat(child.getStatus()).isEqualTo(CourseRunStatus.CANCELLED);
				verify(runClosure).cancelEffects(managerId, child, now);
			} else {
				assertThat(child.getStatus()).isEqualTo(states[i]);
				verify(runClosure, never()).cancelEffects(managerId, child, now);
			}
		}
		cascadeService.publishCourse(managerId, course.getId(), new VersionRequest(0L));
		assertThat(children.subList(0, 3)).allMatch(r -> r.getStatus() == CourseRunStatus.CANCELLED);
	}

	@Test
	void archivingInactiveCourseCancelsRemainingFutureRuns() {
		CourseRun child = run(0L, 1);
		Course course = child.getCourse();
		course.publish(); course.deactivate();
		ReflectionTestUtils.setField(course, "version", 0L);
		ReflectionTestUtils.setField(child, "plannedStartAt", Instant.now().plusSeconds(86400));
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));
		when(runs.findIdsByCourseId(course.getId())).thenReturn(java.util.List.of(child.getId()));
		when(runs.findByIdForUpdate(child.getId())).thenReturn(Optional.of(child));
		service.archiveCourse(managerId, course.getId(), new VersionRequest(0L));
		assertThat(course.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
		assertThat(child.getStatus()).isEqualTo(CourseRunStatus.CANCELLED);
	}

	@BeforeEach
	void activeManager() {
		managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
	}

    @Test
    void refusesOpeningWithoutCompleteScheduleAndDoesNotWrite() {
        CourseRun run = run(1L, 3);
        run.getCourse().publish();
        when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
        when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId())).thenReturn(java.util.List.of());
        assertThatThrownBy(() -> service.openRun(managerId, run.getId(), new CourseRunOpenRequest(1L, "P1")))
            .isInstanceOf(CourseException.class);
        assertThat(run.getStatus()).isEqualTo(CourseRunStatus.DRAFT);
        verify(runs, never()).saveAndFlush(any());
        verifyNoInteractions(audits);
    }

    @Test
    void opensOnlyCompleteNonOverlappingNumberedSchedule() {
        CourseRun run = run(1L, 3);
        run.getCourse().publish();
        when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
        var values = new java.util.ArrayList<CourseSession>();
        for (int i = 1; i <= run.getTotalUnits(); i++) {
            Instant start = run.getPlannedStartAt().plusSeconds(i * 3600L);
            values.add(new CourseSession(UUID.randomUUID(), run, i, mock(Room.class), run.getPrimaryCoach(),
                start, start.plusSeconds(1800), BigInteger.TEN));
        }
        when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId())).thenReturn(values);
        service.openRun(managerId, run.getId(), new CourseRunOpenRequest(1L, "P1"));
        assertThat(run.getStatus()).isEqualTo(CourseRunStatus.OPEN_FOR_REGISTRATION);
    }

    @Test
    void refusesDuplicateUnitNumbersEvenWhenCountMatches() {
        CourseRun run = run(1L, 3);
        run.getCourse().publish();
        when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
        var values = new java.util.ArrayList<CourseSession>();
        for (int i = 1; i <= run.getTotalUnits(); i++) {
            Instant start = run.getPlannedStartAt().plusSeconds(i * 3600L);
            values.add(new CourseSession(UUID.randomUUID(), run, 1, mock(Room.class), run.getPrimaryCoach(),
                start, start.plusSeconds(1800), BigInteger.TEN));
        }
        when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId())).thenReturn(values);
        assertThatThrownBy(() -> service.openRun(managerId, run.getId(), new CourseRunOpenRequest(1L, "P1")))
            .isInstanceOf(CourseException.class);
        assertThat(run.getStatus()).isEqualTo(CourseRunStatus.DRAFT);
    }

	@Test
	void createsDraftCourseAndWritesSanitizedAudit() {
		UUID disciplineId = UUID.randomUUID();
		Discipline discipline = mock(Discipline.class);
		when(discipline.getId()).thenReturn(disciplineId);
		when(discipline.getName()).thenReturn("Swimming");
		when(discipline.getStatus()).thenReturn(DisciplineStatus.ACTIVE);
		when(disciplines.findById(disciplineId)).thenReturn(Optional.of(discipline));
		when(courses.saveAndFlush(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CourseResponse response = service.createCourse(managerId,
			new CourseCreateRequest(" SWIM-101 ", " Beginner Swim ", " Basics ", disciplineId, null, 3));

		assertThat(response.code()).isEqualTo("SWIM-101");
		assertThat(response.name()).isEqualTo("Beginner Swim");
		assertThat(response.status()).isEqualTo(CourseStatus.DRAFT);
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("COURSE_CREATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getAfterData()).containsEntry("code", "SWIM-101")
			.doesNotContainKeys("managerName", "managerEmail");
	}

	@Test
	void rejectsStaleCoursePatchBeforeChangingTheEntity() {
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", null,
			mock(Discipline.class), null, 3);
		ReflectionTestUtils.setField(course, "version", 2L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));

		assertThatThrownBy(() -> service.patchCourse(managerId, course.getId(),
			patch("{\"expectedVersion\":1,\"name\":\"Changed\"}")))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_CHANGED"));
		assertThat(course.getName()).isEqualTo("Beginner Swim");
	}

	@Test
	void normalizedNoOpCoursePatchDoesNotChangeTimestampVersionOrAudit() {
		Discipline discipline = discipline();
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", "Basics", discipline,
			"https://example.test/swim.png", 3);
		Instant updatedAt = Instant.parse("2026-10-30T00:00:00Z");
		ReflectionTestUtils.setField(course, "updatedAt", updatedAt);
		ReflectionTestUtils.setField(course, "version", 4L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));

		CourseResponse response = service.patchCourse(managerId, course.getId(), patch("""
			{"expectedVersion":4,"name":" Beginner Swim ","description":" Basics ",
			 "imageUrl":" https://example.test/swim.png ","defaultMinimumEnrollment":3}
			"""));

		assertThat(response.updatedAt()).isEqualTo(updatedAt);
		assertThat(response.version()).isEqualTo(4L);
		verify(courses, never()).saveAndFlush(any(Course.class));
		verifyNoInteractions(audits);
	}

	@Test
	void explicitNullCoursePatchClearsNullableCatalogFields() {
		Discipline discipline = discipline();
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", "Basics", discipline,
			"https://example.test/swim.png", 3);
		ReflectionTestUtils.setField(course, "version", 2L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));
		when(courses.saveAndFlush(course)).thenReturn(course);

		CourseResponse response = service.patchCourse(managerId, course.getId(), patch("""
			{"expectedVersion":2,"description":null,"imageUrl":null,"defaultMinimumEnrollment":null}
			"""));

		assertThat(response.description()).isNull();
		assertThat(response.imageUrl()).isNull();
		assertThat(response.defaultMinimumEnrollment()).isNull();
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAfterData())
			.containsEntry("description", null)
			.containsEntry("imageUrl", null)
			.containsEntry("defaultMinimumEnrollment", null);
	}

	@Test
	void staleRunPatchDoesNotMutateOrAudit() {
		CourseRun run = run(4L, 3);
		when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));

		assertThatThrownBy(() -> service.patchRun(managerId, run.getId(), new CourseRunPatchRequest(3L, 5)))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_RUN_CHANGED"));

		assertThat(run.getMinimumEnrollment()).isEqualTo(3);
		verify(runs, never()).saveAndFlush(any(CourseRun.class));
		verifyNoInteractions(audits);
	}

	@Test
	void runScheduleCannotAddUnsoldUnitsAfterOpeningOrStartBeforeRegistrationCloses() {
		CourseRun run = run(4L, 3);
		run.openForRegistration("POLICY");
		when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));

		assertThatThrownBy(() -> service.patchRunSchedule(managerId, run.getId(), new CourseRunScheduleRequest(4L,
			run.getPlannedStartAt(), run.getPlannedEndAt(), 11))).isInstanceOf(CourseException.class);
		assertThatThrownBy(() -> service.patchRunSchedule(managerId, run.getId(), new CourseRunScheduleRequest(4L,
			run.getRegistrationClosesAt().minusSeconds(1), run.getPlannedEndAt(), 10))).isInstanceOf(CourseException.class);
		assertThat(run.getTotalUnits()).isEqualTo(10);
		verify(runs, never()).saveAndFlush(any());
		verifyNoInteractions(audits);
	}

	@Test
	void runReductionCannotRemoveAnAlreadyStartedUnit() {
		CourseRun run = run(4L, 3);
		CourseSession started = new CourseSession(UUID.randomUUID(), run, 10, room("Room"), coach("Coach"),
			Instant.EPOCH, Instant.EPOCH.plusSeconds(3600), BigInteger.TEN);
		when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
		when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId())).thenReturn(java.util.List.of(started));

		assertThatThrownBy(() -> service.patchRunSchedule(managerId, run.getId(), new CourseRunScheduleRequest(4L,
			run.getPlannedStartAt(), run.getPlannedEndAt(), 9))).isInstanceOf(CourseException.class);
		assertThat(started.getStatus()).isEqualTo(CourseSessionStatus.SCHEDULED);
		verify(runs, never()).saveAndFlush(any());
		verifyNoInteractions(audits);
	}

	@Test
	void runPatchWritesBeforeAndAfterAudit() {
		CourseRun run = run(4L, 3);
		when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
		when(runs.saveAndFlush(run)).thenReturn(run);

		service.patchRun(managerId, run.getId(), new CourseRunPatchRequest(4L, 5));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("COURSE_RUN_UPDATED");
		assertThat(audit.getValue().getBeforeData()).containsEntry("minimumEnrollment", 3);
		assertThat(audit.getValue().getAfterData()).containsEntry("minimumEnrollment", 5);
	}

	@Test
	void cancellingRunClosesItsActiveWaitlistEntriesInTheSameWorkflow() {
		CourseRun run = run(2L, 3);
		when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
		when(runs.saveAndFlush(run)).thenReturn(run);

		service.cancelRun(managerId, run.getId(), new VersionRequest(2L));

		verify(runClosure).cancelEffects(managerId, run);
	}

	@Test
	void cancellingRunRecordsActorAndStableReasonInBeforeAndAfterAudit() {
		CourseRun run = run(2L, 3);
		when(runs.findByIdForUpdate(run.getId())).thenReturn(Optional.of(run));
		when(runs.saveAndFlush(run)).thenReturn(run);

		service.cancelRun(managerId, run.getId(), new VersionRequest(2L));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(managerId);
		assertThat(audit.getValue().getBeforeData()).containsEntry("status", "DRAFT");
		assertThat(audit.getValue().getAfterData()).containsEntry("status", "CANCELLED")
			.containsEntry("reasonCode", "MANAGER_CANCELLED");
	}

	@Test
	void sessionPatchWritesBeforeAndAfterAudit() {
		CourseRun run = run(1L, 3);
		Room originalRoom = room("Original room");
		Room changedRoom = room("Changed room");
		Account originalCoach = coach("Original coach");
		Account changedCoach = coach("Changed coach");
		Instant originalStart = Instant.parse("2030-02-01T10:00:00Z");
		Instant changedStart = originalStart.plusSeconds(7200);
		CourseSession current = new CourseSession(UUID.randomUUID(), run, 1, originalRoom, originalCoach,
			originalStart, originalStart.plusSeconds(3600), BigInteger.TEN);
		CourseSession changed = new CourseSession(current.getId(), run, 1, changedRoom, changedCoach,
			changedStart, changedStart.plusSeconds(3600), BigInteger.TEN);
		ReflectionTestUtils.setField(current, "version", 2L);
		ReflectionTestUtils.setField(changed, "version", 3L);
		when(sessions.findByIdAndCourseRunId(current.getId(), run.getId())).thenReturn(Optional.of(current));
		when(sessionService.reschedule(current.getId(), changedRoom.getId(), changedCoach.getId(), changedStart,
			changedStart.plusSeconds(3600), 2L)).thenReturn(changed);

		service.patchSession(managerId, run.getId(), current.getId(), new CourseSessionPatchRequest(2L,
			changedRoom.getId(), changedCoach.getId(), changedStart, changedStart.plusSeconds(3600)));

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("COURSE_SESSION_RESCHEDULED");
		assertThat(audit.getValue().getBeforeData()).containsEntry("roomId", originalRoom.getId());
		assertThat(audit.getValue().getAfterData()).containsEntry("roomId", changedRoom.getId());
	}

	@Test
	void publishUsesCourseLifecycleAndRejectsRepeatedTransition() {
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", null,
			mock(Discipline.class), null, 3);
		ReflectionTestUtils.setField(course, "version", 0L);
		when(courses.findByIdForUpdate(course.getId())).thenReturn(Optional.of(course));
		when(courses.saveAndFlush(course)).thenReturn(course);

		assertThat(service.publishCourse(managerId, course.getId(), new VersionRequest(0L)).status())
			.isEqualTo(CourseStatus.PUBLISHED);
		assertThatThrownBy(() -> service.publishCourse(managerId, course.getId(), new VersionRequest(0L)))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_INVALID_TRANSITION"));
	}

	private CoursePatchRequest patch(String json) {
		try {
			return new ObjectMapper().readValue(json, CoursePatchRequest.class);
		}
		catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}

	private Discipline discipline() {
		Discipline discipline = mock(Discipline.class);
		when(discipline.getId()).thenReturn(UUID.randomUUID());
		when(discipline.getName()).thenReturn("Swimming");
		return discipline;
	}

	private CourseRun run(long version, int minimumEnrollment) {
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", null, discipline(), null, 3);
		Account coach = coach("Course coach");
		CourseRun run = new CourseRun(UUID.randomUUID(), "SWIM-RUN-1", course, coach,
			BigInteger.valueOf(1_000_000), 20, minimumEnrollment,
			Instant.parse("2030-01-01T00:00:00Z"), Instant.parse("2030-01-15T00:00:00Z"),
			Instant.parse("2030-02-01T00:00:00Z"), Instant.parse("2030-03-01T00:00:00Z"), 10, true);
		ReflectionTestUtils.setField(run, "version", version);
		return run;
	}

	private Account coach(String name) {
		Account coach = mock(Account.class);
		when(coach.getId()).thenReturn(UUID.randomUUID());
		when(coach.getFullName()).thenReturn(name);
		return coach;
	}

	private Room room(String name) {
		Room room = mock(Room.class);
		when(room.getId()).thenReturn(UUID.randomUUID());
		when(room.getName()).thenReturn(name);
		return room;
	}
}
