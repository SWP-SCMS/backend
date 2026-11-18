package com.scms.backend.course.schedulechange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import com.scms.backend.account.Account;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseSessionStatus;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.run.CourseRunRescheduleService.PlannedSession;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.Room;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class CourseBatchScheduleNotificationsTests {

	private final Instant now = Instant.parse("2030-01-01T00:00:00Z");
	private final UUID runId = UUID.randomUUID(), memberId = UUID.randomUUID();
	private final CourseScheduleChangeRepository repository = mock(CourseScheduleChangeRepository.class);
	private final CourseScheduleChangeNotifications notifications = new CourseScheduleChangeNotifications(mock(JdbcTemplate.class), mock(NotificationWriter.class), Clock.fixed(now, ZoneOffset.UTC), repository);

	@Test
	void synchronizedWindowOnlyIncludesMovedSessionsInTheSharedResponse() {
		var first = session(1, true);
		var second = session(2, false);
		var third = session(3, true);
		members(first, second, third);
		var responses = notifications.prepareAtomicRunResponses(runId, CourseWithdrawalCause.VOLUNTARY, true, List.of(first, second, third), now);
		assertThat(responses).extracting(response -> response.sessionId()).containsExactly(first.session().getId(), third.session().getId());
		assertThat(responses).allSatisfy(response -> assertThat(response.responseId()).isNotNull());
		assertThat(responses).allSatisfy(response -> assertThat(response.cause()).isEqualTo(CourseWithdrawalCause.CENTER_SESSION_RESCHEDULE));
		assertThat(responses).extracting(response -> response.deadline()).containsOnly(responses.getFirst().deadline());
	}

	@Test
	void windowOnlyEditStillNotifiesTheWholeRun() {
		var first = session(1, false);
		var second = session(2, false);
		members(first, second);
		assertThat(notifications.prepareAtomicRunResponses(runId, CourseWithdrawalCause.START_DELAY_OVER_7_DAYS, true, List.of(first, second), now)).hasSize(2);
	}

	@Test
	void editingOnlyRunWindowKeepsVoluntaryCause() {
		var first = session(3, false);
		members(first);
		var responses = notifications.prepareAtomicRunResponses(runId, CourseWithdrawalCause.VOLUNTARY, true, List.of(first), now);
		assertThat(responses.getFirst().cause()).isEqualTo(CourseWithdrawalCause.VOLUNTARY);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.EnumSource(value = CourseWithdrawalCause.class, names = { "START_DELAY_OVER_7_DAYS", "REDUCED_UNIT_COUNT" })
	void specialRunCausesRetainTheirPolicyWhenSessionsAlsoMove(CourseWithdrawalCause cause) {
		var first = session(3, true);
		members(first);
		var responses = notifications.prepareAtomicRunResponses(runId, cause, true, List.of(first), now);
		assertThat(responses.getFirst().cause()).isEqualTo(cause);
	}

	@Test
	void makeupSessionMoveDoesNotGetTheNewRegularSessionCause() {
		var first = session(3, true);
		when(first.session().isMakeup()).thenReturn(true);
		members(first);
		var responses = notifications.prepareAtomicRunResponses(runId, CourseWithdrawalCause.VOLUNTARY, false, List.of(first), now);
		assertThat(responses.getFirst().cause()).isEqualTo(CourseWithdrawalCause.VOLUNTARY);
	}

	@Test
	void reductionStillNotifiesAllAffectedMembers() {
		var first = session(1, false);
		var second = session(2, true);
		members(first, second);
		assertThat(notifications.prepareAtomicRunResponses(runId, CourseWithdrawalCause.REDUCED_UNIT_COUNT, true, List.of(first, second), now)).hasSize(2);
	}

	@Test
	void resourceOnlyChangeKeepsExistingPendingScheduleConfirmation() {
		var unchanged = session(5, false);
		var newRoom = mock(Room.class);
		when(newRoom.getId()).thenReturn(UUID.randomUUID());
		var reassigned = new PlannedSession(unchanged.session(), unchanged.startAt(), unchanged.endAt(), unchanged.coach(), newRoom, false);
		members(reassigned);
		var responses = notifications.prepareAtomicRunResponses(runId, CourseWithdrawalCause.VOLUNTARY, false, List.of(reassigned), now);
		assertThat(responses).hasSize(1);
		assertThat(responses.getFirst().responseId()).isNull();
		var run = mock(com.scms.backend.course.CourseRun.class);
		var course = mock(com.scms.backend.course.Course.class);
		when(run.getId()).thenReturn(runId);
		when(run.getCourse()).thenReturn(course);
		when(run.getRunCode()).thenReturn("RUN");
		when(course.getCode()).thenReturn("COURSE");
		notifications.writeAtomicRunResponses(UUID.randomUUID(), run, responses, now);
		verify(repository, never()).writeAtomicRunResponsesUpdateCourseScheduleChangeResponses(any());
		verify(repository, never()).writeAtomicRunResponsesInsertCourseScheduleChangeResponses(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	private PlannedSession session(int day, boolean moved) {
		var session = mock(CourseSession.class);
		var coach = mock(Account.class);
		var room = mock(Room.class);
		when(coach.getId()).thenReturn(UUID.randomUUID());
		when(room.getId()).thenReturn(UUID.randomUUID());
		when(session.getId()).thenReturn(UUID.randomUUID());
		when(session.getCoach()).thenReturn(coach);
		when(session.getRoom()).thenReturn(room);
		when(session.getStatus()).thenReturn(CourseSessionStatus.SCHEDULED);
		Instant start = now.plusSeconds(day * 86400L + 3600);
		when(session.getStartAt()).thenReturn(start);
		when(session.getEndAt()).thenReturn(start.plusSeconds(3600));
		return new PlannedSession(session, start.plusSeconds(moved ? 7200 : 0), start.plusSeconds(moved ? 10800 : 3600), coach, room, false);
	}

	private void members(PlannedSession... sessions) {
		var rows = java.util.Arrays.stream(sessions)
			.map(item -> Map.<String, Object>of("course_session_id", item.session().getId(), "enrollment_session_id", UUID.randomUUID(), "member_account_id", memberId)).toList();
		when(repository.prepareAtomicRunResponsesFindCourseEnrollmentSessions(runId)).thenReturn(rows);
	}
}
