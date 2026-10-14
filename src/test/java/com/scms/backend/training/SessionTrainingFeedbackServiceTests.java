package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionTrainingFeedbackServiceTests {

	@Mock TrainingFeedbackRepository feedback;
	@Mock TrainingResultRepository results;
	@Mock ClassSessionRepository sessions;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;

	private final UUID coachId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID sessionId = UUID.randomUUID();
	private final UUID resultId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-15T08:00:00Z");
	private SessionTrainingFeedbackService service;

	@BeforeEach
	void setUp() {
		service = new SessionTrainingFeedbackService(feedback, results, sessions, accounts, audits,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void createPersistsSessionFeedbackWithoutPersonalAssignmentAndAuditsIt() {
		permitTeachingCoach();
		when(results.findSessionByIdAndMemberAccountId(resultId, sessionId, memberId))
			.thenReturn(Optional.of(sessionResult()));
		when(feedback.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		SessionTrainingFeedbackResponse response = service.create(coachId, sessionId, memberId,
			request("Good mobility"));

		assertThat(response.classSessionId()).isEqualTo(sessionId);
		assertThat(response.trainingResultId()).isEqualTo(resultId);
		ArgumentCaptor<TrainingFeedback> saved = ArgumentCaptor.forClass(TrainingFeedback.class);
		verify(feedback).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getCoachAssignmentId()).isNull();
		assertThat(saved.getValue().getTrainingPlanVersionId()).isNull();
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_FEEDBACK_CREATED");
		assertThat(audit.getValue().getAfterData()).containsEntry("classSessionId", sessionId.toString());
	}

	@Test
	void createRejectsCoachWhoDoesNotTeachSession() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, sessionId, memberId, request("Denied")))
			.isInstanceOfSatisfying(TrainingFeedbackException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("SESSION_NOT_FOUND"));
		verify(results, never()).findSessionByIdAndMemberAccountId(any(), any(), any());
	}

	@Test
	void createRejectsPersonalOrAnotherSessionResult() {
		permitTeachingCoach();
		when(results.findSessionByIdAndMemberAccountId(resultId, sessionId, memberId))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, sessionId, memberId, request("Denied")))
			.isInstanceOfSatisfying(TrainingFeedbackException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("TRAINING_RESULT_NOT_FOUND"));
		verify(feedback, never()).saveAndFlush(any());
	}

	@Test
	void updateKeepsSessionScopeAndAuditsBeforeAndAfter() {
		permitTeachingCoach();
		when(results.findSessionByIdAndMemberAccountId(resultId, sessionId, memberId))
			.thenReturn(Optional.of(sessionResult()));
		UUID feedbackId = UUID.randomUUID();
		TrainingFeedback existing = TrainingFeedback.forClassSessionResult(feedbackId, memberId,
			resultId, "Before", coachId, now.minusSeconds(3600));
		when(feedback.findSessionByIdAndMemberAccountIdForUpdate(feedbackId, sessionId, memberId))
			.thenReturn(Optional.of(existing));
		when(feedback.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		SessionTrainingFeedbackResponse response = service.update(coachId, sessionId, memberId,
			feedbackId, request("After"));

		assertThat(response.content()).isEqualTo("After");
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_FEEDBACK_UPDATED");
		assertThat(audit.getValue().getBeforeData()).containsEntry("content", "Before");
		assertThat(audit.getValue().getAfterData()).containsEntry("content", "After");
	}

	private void permitTeachingCoach() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId))
			.thenReturn(Optional.of(mock(ClassSession.class)));
	}

	private SessionTrainingFeedbackRequest request(String content) {
		return new SessionTrainingFeedbackRequest(resultId, content);
	}

	private TrainingResult sessionResult() {
		return TrainingResult.forClassSession(resultId, memberId, sessionId, now.minusSeconds(600),
			Map.of("repetitions", 10), coachId, now.minusSeconds(600));
	}
}
