package com.scms.backend.course.schedulechange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.course.Course;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.enrollment.CourseEnrollmentService;
import com.scms.backend.notification.NotificationWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CourseScheduleChangeServiceTests {

	private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

	private final JdbcTemplate db = mock(JdbcTemplate.class);

	private final NotificationWriter notifications = mock(NotificationWriter.class);

	private final AccountRepository accounts = mock(AccountRepository.class);

	private final CourseScheduleChangeService service = new CourseScheduleChangeService(db, notifications, accounts, Clock.fixed(NOW, ZoneOffset.UTC));

	private final UUID sessionId = UUID.randomUUID();

	private final UUID enrollmentSessionId = UUID.randomUUID();

	private final UUID memberId = UUID.randomUUID();

	private final UUID responseId = UUID.randomUUID();

	@Test
	void transferChoiceIsRejectedBeforeReadingOrWritingScheduleChanges() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		assertThatThrownBy(() -> service.respond(memberId, responseId, "REQUEST_TRANSFER"))
			.isInstanceOfSatisfying(CourseException.class, error -> assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_INVALID"));
		verifyNoInteractions(db, notifications);
	}

	@Test
	void nullChoiceProducesStableValidationError() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		assertThatThrownBy(() -> service.respond(memberId, responseId, null)).isInstanceOfSatisfying(CourseException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_INVALID"));
	}

	@Test
	void nullRequestBodyProducesStableValidationError() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		CourseEnrollmentService enrollmentService = new CourseEnrollmentService(db, accounts, mock(com.scms.backend.audit.AuditEventRepository.class), Clock.fixed(NOW, ZoneOffset.UTC), service);
		assertThatThrownBy(() -> enrollmentService.respondToScheduleChange(memberId, responseId, null)).isInstanceOfSatisfying(CourseException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_INVALID"));
	}

	@Test
	void memberCannotReadOrRespondToAnotherMembersRecord() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		when(db.query(any(String.class), any(RowMapper.class), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class), eq(responseId), eq(memberId))).thenReturn(List.of());
		assertThatThrownBy(() -> service.respond(memberId, responseId, "CONTINUE")).isInstanceOfSatisfying(CourseException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_NOT_FOUND"));
	}

	@Test
	void sameChoiceReplayIsIdempotentAndDifferentChoiceConflicts() {
		com.scms.backend.account.Account member = mock(com.scms.backend.account.Account.class);
		when(member.getRole()).thenReturn(com.scms.backend.account.AccountRole.MEMBER);
		when(member.getStatus()).thenReturn(com.scms.backend.account.AccountStatus.ACTIVE);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(java.util.Optional.of(member));
		when(db.queryForList(contains("select e.course_run_id"), eq(UUID.class), eq(responseId), eq(memberId))).thenReturn(List.of(UUID.randomUUID()));
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		var existing = new com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange(responseId, enrollmentSessionId, NOW.minusSeconds(7200), NOW.minusSeconds(3600), NOW.plusSeconds(3600), NOW.plusSeconds(7200), NOW.minusSeconds(3600), NOW.plusSeconds(1800), "RESPONDED", "CONTINUE", NOW);
		when(db.query(any(String.class), any(RowMapper.class), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class), eq(responseId), eq(memberId))).thenReturn(List.of(existing));
		org.assertj.core.api.Assertions.assertThat(service.respond(memberId, responseId, "CONTINUE")).isEqualTo(existing);
		assertThatThrownBy(() -> service.respond(memberId, responseId, "REQUEST_WITHDRAWAL")).isInstanceOfSatisfying(CourseException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_CONFLICT"));
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void responseAtDeadlineIsClosed() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		var pending = new com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange(responseId, enrollmentSessionId, NOW.minusSeconds(7200), NOW.minusSeconds(3600), NOW.plusSeconds(3600), NOW.plusSeconds(7200), NOW.minusSeconds(3600), NOW, "PENDING", null, null);
		when(db.query(any(String.class), any(RowMapper.class), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class), eq(responseId), eq(memberId))).thenReturn(List.of(pending));
		assertThatThrownBy(() -> service.respond(memberId, responseId, "CONTINUE")).isInstanceOfSatisfying(CourseException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_CLOSED"));
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void minorTimeChangeNotifiesEnrolledMemberWithoutCreatingResponse() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class))).thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));
		Instant oldStart = Instant.parse("2035-01-01T02:00:00Z");
		service.notifyReschedule(session(), oldStart, oldStart.plusSeconds(3600), oldStart.plusSeconds(1800), oldStart.plusSeconds(5400));
		verify(db, never()).update(contains("insert into course_schedule_change_responses"), any(Object[].class));
		verify(notifications).write(any(String.class), eq(memberId), eq("COURSE_SCHEDULE_CHANGED"), eq("COURSE_SESSION"), eq(sessionId), any(Map.class));
	}

	@Test
	void materialTimeChangeCreatesResponseRecordAndIncludesDeadlineInNotification() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class))).thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));
		Instant oldStart = Instant.parse("2035-01-01T02:00:00Z");
		service.notifyReschedule(session(), oldStart, oldStart.plusSeconds(3600), oldStart.plusSeconds(3601), oldStart.plusSeconds(7201));
		verify(db).update(contains("insert into course_schedule_change_responses"), any(Object[].class));
		verify(notifications).write(any(String.class), eq(memberId), eq("COURSE_SCHEDULE_CHANGED"), eq("COURSE_SESSION"), eq(sessionId), org.mockito.ArgumentMatchers.argThat(payload -> payload.get("responseDeadline").equals("2030-01-04T00:00:00Z") && payload.get("recordId") != null));
	}

	@Test
	void delayedLaterUnitDoesNotGrantOpeningDelayRefundCause() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class))).thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));
		List<Object[]> inserts = new ArrayList<>();
		org.mockito.Mockito.doAnswer(call -> {
			inserts.add((Object[]) call.getRawArguments()[1]);
			return 1;
		}).when(db).update(contains("insert into course_schedule_change_responses"), any(Object[].class));
		Instant oldStart = Instant.parse("2035-01-01T02:00:00Z");
		service.notifyReschedule(session(), oldStart, oldStart.plusSeconds(3600), oldStart.plusSeconds(8 * 86400), oldStart.plusSeconds(8 * 86400 + 3600));
		assertThat(inserts).singleElement().satisfies(values -> assertThat(values[8]).isEqualTo("CENTER_SESSION_RESCHEDULE"));
	}

	@Test
	void conflictRefundCauseOnlyAppliesToAffectedEnrollment() {
		UUID otherEnrollment = UUID.randomUUID(), otherMember = UUID.randomUUID();
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class))).thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId), Map.of("enrollment_session_id", otherEnrollment, "member_account_id", otherMember)));
		when(db.queryForObject(any(String.class), eq(Boolean.class), any(Object[].class))).thenReturn(true);
		when(db.queryForList(contains("select distinct current_es.id"), eq(UUID.class), any(Object[].class))).thenReturn(List.of(enrollmentSessionId));
		List<Object[]> inserts = new ArrayList<>();
		org.mockito.Mockito.doAnswer(call -> {
			inserts.add((Object[]) call.getRawArguments()[1]);
			return 1;
		}).when(db).update(contains("insert into course_schedule_change_responses"), any(Object[].class));
		Instant oldStart = Instant.parse("2035-01-01T02:00:00Z");
		service.notifyReschedule(session(), oldStart, oldStart.plusSeconds(3600), oldStart.plusSeconds(7200), oldStart.plusSeconds(10800));
		assertThat(inserts).hasSize(2);
		assertThat(inserts.get(0)[8]).isEqualTo("MEMBER_SCHEDULE_CONFLICT");
		assertThat(inserts.get(1)[8]).isEqualTo("CENTER_SESSION_RESCHEDULE");
	}

	@Test
	void materialChangeWithDeadlineAtOrBeforeNowIsRejectedBeforeWrites() {
		when(db.queryForList(contains("disposition='ENROLLED'"), any(Object[].class))).thenReturn(List.of(Map.of("enrollment_session_id", enrollmentSessionId, "member_account_id", memberId)));
		assertThatThrownBy(() -> service.notifyReschedule(session(), NOW, NOW.plusSeconds(3600), NOW.plusSeconds(3601), NOW.plusSeconds(7201))).isInstanceOfSatisfying(CourseException.class, error -> org.assertj.core.api.Assertions.assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_DEADLINE_UNAVAILABLE"));
		verify(db, never()).update(any(String.class), any(Object[].class));
		verifyNoInteractions(notifications);
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "CONTINUE", "REQUEST_WITHDRAWAL" })
	void oneDecisionUpdatesTheWholeEventAndCreatesAtMostOneWithdrawal(String choice) {
		var audits = mock(com.scms.backend.audit.AuditEventRepository.class);
		var withdrawals = mock(com.scms.backend.course.withdrawal.CourseWithdrawalService.class);
		var batchService = new CourseScheduleChangeService(db, notifications, accounts, Clock.fixed(NOW, ZoneOffset.UTC), withdrawals, audits);
		UUID event = UUID.randomUUID(), siblingId = UUID.randomUUID();
		var first = batchRow(responseId, event, NOW.plusSeconds(3600), null, com.scms.backend.course.CourseWithdrawalCause.START_DELAY_OVER_7_DAYS);
		var sibling = batchRow(siblingId, event, NOW.plusSeconds(3600), null, com.scms.backend.course.CourseWithdrawalCause.CENTER_SESSION_RESCHEDULE);
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		var member = mock(com.scms.backend.account.Account.class);
		when(member.getRole()).thenReturn(com.scms.backend.account.AccountRole.MEMBER);
		when(member.getStatus()).thenReturn(com.scms.backend.account.AccountStatus.ACTIVE);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(java.util.Optional.of(member));
		when(db.queryForList(contains("select e.course_run_id"), eq(UUID.class), eq(responseId), eq(memberId))).thenReturn(List.of(UUID.randomUUID()));
		when(db.query(any(String.class), any(RowMapper.class), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class), any(UUID.class), eq(memberId)))
			.thenReturn(List.of(first, sibling), List.of(batchRow(responseId, event, NOW.plusSeconds(3600), choice)));
		batchService.respond(memberId, responseId, choice);
		verify(db).update(contains("where status='PENDING' and id in"), eq(choice), eq(java.sql.Timestamp.from(NOW)), eq(responseId));
		verify(audits).save(any(com.scms.backend.audit.AuditEvent.class));
		if (choice.equals("REQUEST_WITHDRAWAL")) verify(withdrawals).createForScheduleChange(eq(memberId), eq(memberId), eq(siblingId));
		else verifyNoInteractions(withdrawals);
	}

	@Test
	void earliestDeadlineClosesTheWholeEventBeforeAnyWrite() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, com.scms.backend.account.AccountRole.MEMBER, com.scms.backend.account.AccountStatus.ACTIVE)).thenReturn(true);
		UUID event = UUID.randomUUID();
		when(db.query(any(String.class), any(RowMapper.class), any(java.sql.Timestamp.class), any(java.sql.Timestamp.class), eq(responseId), eq(memberId)))
			.thenReturn(List.of(batchRow(responseId, event, NOW.plusSeconds(3600), null), batchRow(UUID.randomUUID(), event, NOW, null)));
		assertThatThrownBy(() -> service.respond(memberId, responseId, "CONTINUE"))
			.isInstanceOfSatisfying(CourseException.class, error -> assertThat(error.getCode()).isEqualTo("COURSE_SCHEDULE_CHANGE_RESPONSE_CLOSED"));
		verify(db, never()).update(any(String.class), any(Object[].class));
	}

	private CourseScheduleChangeApiModels.ScheduleChange batchRow(UUID id, UUID event, Instant deadline, String response) {
		return batchRow(id, event, deadline, response, com.scms.backend.course.CourseWithdrawalCause.VOLUNTARY);
	}

	private CourseScheduleChangeApiModels.ScheduleChange batchRow(UUID id, UUID event, Instant deadline, String response, com.scms.backend.course.CourseWithdrawalCause cause) {
		return new CourseScheduleChangeApiModels.ScheduleChange(id, enrollmentSessionId, NOW.plusSeconds(7200), NOW.plusSeconds(10800),
			NOW.plusSeconds(14400), NOW.plusSeconds(18000), NOW, deadline,
			response == null ? "PENDING" : "RESPONDED", response, response == null ? null : NOW, event,
			cause, null, true, "ELIGIBLE_WITH_REFUND", java.math.BigInteger.TEN, java.math.BigInteger.valueOf(8));
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
