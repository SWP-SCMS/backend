package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class CourseCatalogServiceTests {

	private static final Instant NOW = Instant.parse("2026-11-15T00:00:00Z");
	private final CourseRepository courses = mock(CourseRepository.class);
	private final CourseRunRepository runs = mock(CourseRunRepository.class);
	private final CourseSessionRepository sessions = mock(CourseSessionRepository.class);
	private final PageRequest page = PageRequest.of(0, 20);
	private CourseCatalogService service;

	@BeforeEach
	void setUp() {
		service = new CourseCatalogService(courses, runs, sessions, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void courseReadsQueryOnlyPublishedRows() {
		Course course = publishedCourse();
		when(courses.findCatalogByStatus(CourseStatus.PUBLISHED, page))
			.thenReturn(new PageImpl<>(List.of(course), page, 1));
		when(courses.findCatalogByIdAndStatus(course.getId(), CourseStatus.PUBLISHED))
			.thenReturn(Optional.of(course));

		CatalogCoursePageResponse listed = service.listCourses(page);
		CatalogCourseResponse detailed = service.getCourse(course.getId());

		assertThat(listed.content()).singleElement().isEqualTo(detailed);
		assertThat(detailed.code()).isEqualTo("SWIM-101");
		verify(courses).findCatalogByStatus(CourseStatus.PUBLISHED, page);
		verify(courses).findCatalogByIdAndStatus(course.getId(), CourseStatus.PUBLISHED);
	}

	@Test
	void hiddenCourseUsesTheSameNotFoundContractAsMissingCourse() {
		UUID courseId = UUID.randomUUID();
		when(courses.findCatalogByIdAndStatus(courseId, CourseStatus.PUBLISHED)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getCourse(courseId))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_NOT_FOUND"));
	}

	@Test
	void runReadsRequireOpenRegistrationAndPublishedParent() {
		CourseRun run = openRun(NOW, NOW.plusSeconds(3600));
		when(runs.findCatalogByStatusAndCourseStatus(CourseRunStatus.OPEN_FOR_REGISTRATION,
			CourseStatus.PUBLISHED, page)).thenReturn(new PageImpl<>(List.of(run), page, 1));

		CatalogCourseRunPageResponse response = service.listRuns(page);

		assertThat(response.content()).singleElement().satisfies(row -> {
			assertThat(row.status()).isEqualTo(CourseRunStatus.OPEN_FOR_REGISTRATION);
			assertThat(row.registrationStatus()).isEqualTo(RegistrationStatus.OPEN);
		});
		verify(runs).findCatalogByStatusAndCourseStatus(CourseRunStatus.OPEN_FOR_REGISTRATION,
			CourseStatus.PUBLISHED, page);
	}

	@Test
	void registrationStatusUsesHalfOpenWindowBoundaries() {
		List<CourseRun> visibleRuns = List.of(
			openRun(NOW.plusSeconds(1), NOW.plusSeconds(3600)),
			openRun(NOW, NOW.plusSeconds(3600)),
			openRun(NOW.minusSeconds(3600), NOW));
		when(runs.findCatalogByStatusAndCourseStatus(CourseRunStatus.OPEN_FOR_REGISTRATION,
			CourseStatus.PUBLISHED, page)).thenReturn(new PageImpl<>(visibleRuns, page, visibleRuns.size()));

		assertThat(service.listRuns(page).content()).extracting(CatalogCourseRunResponse::registrationStatus)
			.containsExactly(RegistrationStatus.UPCOMING, RegistrationStatus.OPEN, RegistrationStatus.CLOSED);
	}

	@Test
	void hiddenOrCrossCourseRunDoesNotLoadSessions() {
		UUID runId = UUID.randomUUID();
		when(runs.findCatalogByIdAndStatusAndCourseStatus(runId, CourseRunStatus.OPEN_FOR_REGISTRATION,
			CourseStatus.PUBLISHED)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getRun(runId))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_RUN_NOT_FOUND"));
		verifyNoInteractions(sessions);
	}

	@Test
	void runDetailPreservesRepositoryScheduleOrderAndPresentationFields() {
		CourseRun run = openRun(NOW.minusSeconds(3600), NOW.plusSeconds(3600));
		Room room = mock(Room.class);
		Account coach = run.getPrimaryCoach();
		when(room.getId()).thenReturn(UUID.randomUUID());
		when(room.getName()).thenReturn("Pool A");
		CourseSession first = new CourseSession(UUID.randomUUID(), run, 1, room, coach,
			NOW.plusSeconds(7200), NOW.plusSeconds(10800), BigInteger.valueOf(300000));
		CourseSession second = new CourseSession(UUID.randomUUID(), run, 2, room, coach,
			NOW.plusSeconds(14400), NOW.plusSeconds(18000), BigInteger.valueOf(350000));
		when(runs.findCatalogByIdAndStatusAndCourseStatus(run.getId(), CourseRunStatus.OPEN_FOR_REGISTRATION,
			CourseStatus.PUBLISHED)).thenReturn(Optional.of(run));
		when(sessions.findByCourseRunIdOrderByStartAtAscIdAsc(run.getId())).thenReturn(List.of(first, second));

		CatalogCourseRunDetailResponse response = service.getRun(run.getId());

		assertThat(response.sessions()).extracting(CatalogCourseSessionResponse::id)
			.containsExactly(first.getId(), second.getId());
		assertThat(response.sessions().getFirst()).satisfies(session -> {
			assertThat(session.unitNumber()).isEqualTo(1);
			assertThat(session.roomName()).isEqualTo("Pool A");
			assertThat(session.coachName()).isEqualTo("Coach One");
			assertThat(session.startAt()).isEqualTo(first.getStartAt());
			assertThat(session.originalStartAt()).isEqualTo(first.getOriginalStartAt());
			assertThat(session.allocationVnd()).isEqualTo(BigInteger.valueOf(300000));
			assertThat(session.status()).isEqualTo(CourseSessionStatus.SCHEDULED);
		});
	}

	private Course publishedCourse() {
		Discipline discipline = mock(Discipline.class);
		when(discipline.getId()).thenReturn(UUID.randomUUID());
		when(discipline.getName()).thenReturn("Swimming");
		Course course = new Course(UUID.randomUUID(), "SWIM-101", "Beginner Swim", "Learn safely",
			discipline, "https://example.test/swim.jpg", 3);
		course.publish();
		return course;
	}

	private CourseRun openRun(Instant registrationOpensAt, Instant registrationClosesAt) {
		Course course = publishedCourse();
		Account coach = mock(Account.class);
		when(coach.getId()).thenReturn(UUID.randomUUID());
		when(coach.getFullName()).thenReturn("Coach One");
		Instant plannedStart = registrationClosesAt.plusSeconds(3600);
		CourseRun run = new CourseRun(UUID.randomUUID(), "RUN-" + UUID.randomUUID(), course, coach,
			BigInteger.valueOf(1500000), 20, 3, registrationOpensAt, registrationClosesAt,
			plannedStart, plannedStart.plusSeconds(86400), 8, true);
		run.openForRegistration("POLICY-1");
		return run;
	}
}
