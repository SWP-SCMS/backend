package com.scms.backend.training.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.training.TrainingFeedback;
import com.scms.backend.training.TrainingPlanVersion;
import com.scms.backend.training.TrainingResult;
import com.scms.backend.training.TrainingResultException;
import com.scms.backend.training.assignment.CoachAssignmentRepository;
import com.scms.backend.training.feedback.TrainingFeedbackRepository;
import com.scms.backend.training.plan.TrainingPlanVersionRepository;
import com.scms.backend.training.result.TrainingResultRepository;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class BookedSessionTrainingServiceTests {

	@Mock
	JdbcTemplate db;

	@Mock
	AccountRepository accounts;

	@Mock
	TrainingResultRepository results;

	@Mock
	TrainingFeedbackRepository feedback;

	@Mock
	TrainingPlanVersionRepository plans;

	@Mock
	CoachAssignmentRepository assignments;

	@Mock
	AuditEventRepository audits;

	final UUID coach = UUID.randomUUID(), member = UUID.randomUUID(), session = UUID.randomUUID(), plan = UUID.randomUUID();

	final Instant now = Instant.parse("2035-01-01T10:00:00Z"), start = now.minusSeconds(7200);

	BookedSessionTrainingService service;

	@BeforeEach
	void setup() {
		service = new BookedSessionTrainingService(db, accounts, results, feedback, plans, assignments, audits, Clock.fixed(now, ZoneOffset.UTC));
	}

	@SuppressWarnings("unchecked")
	void session(String status, Instant end, boolean enrolled) throws Exception {
		when(accounts.existsByIdAndRoleAndStatus(coach, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		ResultSet row = mock(ResultSet.class);
		when(row.getTimestamp("starts_at")).thenReturn(Timestamp.from(start));
		when(row.getTimestamp("ends_at")).thenReturn(Timestamp.from(end));
		when(row.getString("status")).thenReturn(status);
		when(row.getString("run_status")).thenReturn("IN_PROGRESS");
		when(db.query(anyString(), any(RowMapper.class), eq(session), eq(coach))).thenAnswer(call -> List.of(((RowMapper<?>) call.getArgument(1)).mapRow(row, 0)));
		lenient().when(db.queryForList(anyString(), eq(UUID.class), eq(session), eq(member))).thenReturn(enrolled ? List.of(UUID.randomUUID()) : List.of());
	}

	void permitPt() {
		when(plans.findByIdAndMemberAccountId(plan, member)).thenReturn(Optional.of(mock(TrainingPlanVersion.class)));
		when(assignments.hasActiveMembershipBenefit(eq(member), anyString(), eq(start))).thenReturn(true);
	}

	void persist() {
		when(results.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
		when(feedback.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
	}

	@Test
	void ptUsesBookedSessionTimeAndLinksFeedbackToTheSameResult() throws Exception {
		session("COMPLETED", now.minusSeconds(3600), true);
		permitPt();
		persist();
		var saved = service.save(coach, session, member, false, plan, " Squat 40kg ", " Good form ");
		assertThat(saved.result().classSessionId()).isEqualTo(session);
		assertThat(saved.result().courseSessionId()).isNull();
		assertThat(saved.result().trainingPlanVersionId()).isEqualTo(plan);
		assertThat(saved.result().activityAt()).isEqualTo(start);
		assertThat(saved.result().content()).containsEntry("notes", "Squat 40kg");
		assertThat(saved.feedback().trainingResultId()).isEqualTo(saved.result().id());
		assertThat(saved.feedback().content()).isEqualTo("Good form");
	}

	@Test
	void courseUsesEnrollmentWithoutRequiringPersonalCoachingBenefits() throws Exception {
		session("COMPLETED", now.minusSeconds(3600), true);
		persist();
		var saved = service.save(coach, session, member, true, null, "Completed", "Progress");
		assertThat(saved.result().courseSessionId()).isEqualTo(session);
		assertThat(saved.result().classSessionId()).isNull();
		assertThat(saved.feedback().courseSessionId()).isEqualTo(session);
		verifyNoInteractions(assignments, plans);
	}

	@Test
	void retryUpdatesExistingPairAndPreservesOtherMeasurements() throws Exception {
		session("COMPLETED", now.minusSeconds(3600), true);
		permitPt();
		persist();
		TrainingResult result = TrainingResult.forClassSession(UUID.randomUUID(), member, session, start, Map.of("weight", 40, "notes", "Before"), coach, start);
		TrainingFeedback comment = TrainingFeedback.forClassSessionResult(UUID.randomUUID(), member, result.getId(), "Before", coach, start);
		when(results.findSessionByClassSessionIdAndMemberAccountId(session, member)).thenReturn(Optional.of(result));
		when(feedback.findCanonicalSessionFeedback(result.getId(), member)).thenReturn(Optional.of(comment));
		var saved = service.save(coach, session, member, false, plan, "After", "Updated");
		assertThat(saved.result().id()).isEqualTo(result.getId());
		assertThat(saved.feedback().id()).isEqualTo(comment.getId());
		assertThat(saved.result().content()).containsEntry("weight", 40).containsEntry("notes", "After");
	}

	@Test
	void rejectsUnfinishedSession() throws Exception {
		session("SCHEDULED", now.plusSeconds(1), true);
		assertThatThrownBy(() -> service.save(coach, session, member, false, plan, "x", "x")).isInstanceOfSatisfying(TrainingResultException.class, error -> assertThat(error.getCode()).isEqualTo("SESSION_TRAINING_NOT_AVAILABLE"));
		verifyNoInteractions(results, feedback);
	}

	@Test
	void rejectsCancelledSession() throws Exception {
		session("CANCELLED", now.minusSeconds(1), true);
		assertThatThrownBy(() -> service.save(coach, session, member, true, null, "x", "x")).isInstanceOf(TrainingResultException.class);
		verifyNoInteractions(results, feedback);
	}

	@Test
	void rejectsMemberOutsideRoster() throws Exception {
		session("COMPLETED", now.minusSeconds(1), false);
		assertThatThrownBy(() -> service.save(coach, session, member, true, null, "x", "x")).isInstanceOfSatisfying(TrainingResultException.class, error -> assertThat(error.getCode()).isEqualTo("SESSION_MEMBER_NOT_FOUND"));
		verifyNoInteractions(results, feedback);
	}

	@Test
	void rejectsAnotherCoachsSession() {
		when(accounts.existsByIdAndRoleAndStatus(coach, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		assertThatThrownBy(() -> service.save(coach, session, member, false, plan, "x", "x")).isInstanceOfSatisfying(TrainingResultException.class, error -> assertThat(error.getCode()).isEqualTo("SESSION_NOT_FOUND"));
		verifyNoInteractions(results, feedback);
	}

	@Test
	void rejectsPlanFromAnotherMember() throws Exception {
		session("COMPLETED", now.minusSeconds(1), true);
		assertThatThrownBy(() -> service.save(coach, session, member, false, plan, "x", "x")).isInstanceOfSatisfying(TrainingResultException.class, error -> assertThat(error.getCode()).isEqualTo("TRAINING_PLAN_VERSION_NOT_FOUND"));
		verifyNoInteractions(results, feedback);
	}
}
