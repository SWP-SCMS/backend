package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CourseScheduleChangeServiceTests {
	private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final NotificationWriter notifications = mock(NotificationWriter.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final CourseScheduleChangeService service = new CourseScheduleChangeService(db, notifications, accounts,
		Clock.fixed(NOW, ZoneOffset.UTC));
	private final UUID sessionId = UUID.randomUUID();
	private final UUID enrollmentSessionId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID responseId = UUID.randomUUID();

	@Test
	void memberCannotReadOrRespondToAnotherMembersRecord() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER,
			com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		when(db.query(any(String.class), any(RowMapper.class), eq(responseId), eq(memberId))).thenReturn(List.of());

		assertThatThrownBy(() -> service.respond(memberId, responseId, "CONTINUE"))
			.isInstanceOfSatisfying(CourseException.class, error ->
				org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_NOT_FOUND"));
	}

	@Test
	void sameChoiceReplayIsIdempotentAndDifferentChoiceConflicts() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER,
			com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		var existing = new com.scms.backend.course.CourseScheduleChangeApiModels.ScheduleChange(responseId,
			enrollmentSessionId, NOW.minusSeconds(7200), NOW.minusSeconds(3600), NOW.plusSeconds(3600),
			NOW.plusSeconds(7200), NOW.minusSeconds(3600), NOW.plusSeconds(1800), "RESPONDED", "CONTINUE", NOW);
		when(db.query(any(String.class), any(RowMapper.class), eq(responseId), eq(memberId)))
			.thenReturn(List.of(existing));

		org.assertj.core.api.Assertions.assertThat(service.respond(memberId, responseId, "CONTINUE"))
			.isEqualTo(existing);
		assertThatThrownBy(() -> service.respond(memberId, responseId, "REQUEST_TRANSFER"))
			.isInstanceOfSatisfying(CourseException.class, error ->
				org.assertj.core.api.Assertions.assertThat(error.getCode())
					.isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_CONFLICT"));
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void responseAtDeadlineIsClosed() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER,
			com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		var pending = new com.scms.backend.course.CourseScheduleChangeApiModels.ScheduleChange(responseId,
			enrollmentSessionId, NOW.minusSeconds(7200), NOW.minusSeconds(3600), NOW.plusSeconds(3600),
			NOW.plusSeconds(7200), NOW.minusSeconds(3600), NOW, "PENDING", null, null);
		when(db.query(any(String.class), any(RowMapper.class), eq(responseId), eq(memberId)))
			.thenReturn(List.of(pending));

		assertThatThrownBy(() -> service.respond(memberId, responseId, "CONTINUE"))
			.isInstanceOfSatisfying(CourseException.class, error ->
				org.assertj.core.api.Assertions.assertThat(error.getCode())
					.isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_CLOSED"));
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void minorTimeChangeNotifiesEnrolledMemberWithoutCreatingResponse() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class)))
			.thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));
		Instant oldStart = Instant.parse("2035-01-01T02:00:00Z");

		service.notifyReschedule(session(), oldStart, oldStart.plusSeconds(3600), oldStart.plusSeconds(1800),
			oldStart.plusSeconds(5400));

		verify(db, never()).update(contains("insert into course_schedule_change_responses"), any(Object[].class));
		verify(notifications).write(any(String.class), eq(memberId), eq("COURSE_SCHEDULE_CHANGED"),
			eq("COURSE_SESSION"), eq(sessionId), any(Map.class));
	}

	@Test
	void materialTimeChangeCreatesResponseRecordAndIncludesDeadlineInNotification() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class)))
			.thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));
		Instant oldStart = Instant.parse("2035-01-01T02:00:00Z");

		service.notifyReschedule(session(), oldStart, oldStart.plusSeconds(3600), oldStart.plusSeconds(3601),
			oldStart.plusSeconds(7201));

		verify(db).update(contains("insert into course_schedule_change_responses"), any(Object[].class));
		verify(notifications).write(any(String.class), eq(memberId), eq("COURSE_SCHEDULE_CHANGED"),
			eq("COURSE_SESSION"), eq(sessionId), org.mockito.ArgumentMatchers.argThat(payload ->
			payload.get("responseDeadline").equals("2030-01-04T00:00:00Z") && payload.get("recordId") != null));
	}

	@Test
	void materialChangeWithDeadlineAtOrBeforeNowIsRejectedBeforeWrites() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class)))
			.thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));

		assertThatThrownBy(() -> service.notifyReschedule(session(), NOW, NOW.plusSeconds(3600),
			NOW.plusSeconds(3601), NOW.plusSeconds(7201)))
			.isInstanceOfSatisfying(CourseException.class, error ->
				org.assertj.core.api.Assertions.assertThat(error.getCode())
					.isEqualTo("COURSE_SCHEDULE_CHANGE_DEADLINE_UNAVAILABLE"));

		verify(db, never()).update(any(String.class), any(Object[].class));
		verifyNoInteractions(notifications);
	}

	private CourseSession session() {
		CourseSession session = mock(CourseSession.class);
		CourseRun run = mock(CourseRun.class);
		Course course = mock(Course.class);
		when(session.getId()).thenReturn(sessionId);
		when(session.getVersion()).thenReturn(4L);
		when(session.getUnitNumber()).thenReturn(3);
		when(session.getCourseRun()).thenReturn(run);
		when(run.getCourse()).thenReturn(course);
		when(run.getRunCode()).thenReturn("RUN-1");
		when(course.getCode()).thenReturn("COURSE-1");
		return session;
	}
}
