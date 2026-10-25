package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.scms.backend.scheduling.SportClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTrainingFeedbackServiceTests {

	@Mock TrainingFeedbackRepository feedback;

	@Test
	void returnsOnlyRepositoryScopedPersonalAndSessionHistory() {
		UUID memberId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		UUID planVersionId = UUID.randomUUID();
		UUID resultId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-15T08:00:00Z");
		TrainingFeedback sessionFeedback = TrainingFeedback.forClassSessionResult(UUID.randomUUID(), memberId,
			resultId, "Session feedback", coachId, now);
		TrainingFeedback personalFeedback = new TrainingFeedback(UUID.randomUUID(), memberId, assignmentId,
			planVersionId, null, "Personal feedback", coachId, now.minusSeconds(3600));
		TrainingFeedbackRepository.MemberFeedbackHistoryView sessionView = view(sessionFeedback, sessionId,
			"Coach One", "PT 1-1", now, SportClassType.PT_1_1);
		TrainingFeedbackRepository.MemberFeedbackHistoryView personalView = view(personalFeedback, null,
			"Coach One", null, null, null);
		when(feedback.findHistoryByMemberAccountId(memberId)).thenReturn(List.of(sessionView, personalView));

		List<MemberTrainingFeedbackResponse> history = new MemberTrainingFeedbackService(feedback)
			.viewOwnHistory(memberId);

		assertThat(history).hasSize(2);
		assertThat(history.get(0).classSessionId()).isEqualTo(sessionId);
		assertThat(history.get(0).coachName()).isEqualTo("Coach One");
		assertThat(history.get(0).sessionName()).isEqualTo("PT 1-1");
		assertThat(history.get(0).sessionStartTime()).isEqualTo(now);
		assertThat(history.get(0).className()).isNull();
		assertThat(history.get(0).coachAssignmentId()).isNull();
		assertThat(history.get(1).coachAssignmentId()).isEqualTo(assignmentId);
		assertThat(history.get(1).trainingPlanVersionId()).isEqualTo(planVersionId);
		assertThat(history.get(1).classSessionId()).isNull();
		verify(feedback).findHistoryByMemberAccountId(memberId);
	}

	@Test
	void returnsEmptyHistoryWithoutMembershipChecks() {
		UUID memberId = UUID.randomUUID();
		when(feedback.findHistoryByMemberAccountId(memberId)).thenReturn(List.of());

		assertThat(new MemberTrainingFeedbackService(feedback).viewOwnHistory(memberId)).isEmpty();
	}

	private TrainingFeedbackRepository.MemberFeedbackHistoryView view(TrainingFeedback value,
			UUID classSessionId, String coachName, String sessionName, Instant sessionStartTime,
			SportClassType classType) {
		TrainingFeedbackRepository.MemberFeedbackHistoryView view =
			mock(TrainingFeedbackRepository.MemberFeedbackHistoryView.class);
		when(view.getFeedback()).thenReturn(value);
		when(view.getClassSessionId()).thenReturn(classSessionId);
		when(view.getCoachName()).thenReturn(coachName);
		when(view.getSessionName()).thenReturn(sessionName);
		when(view.getSessionStartTime()).thenReturn(sessionStartTime);
		when(view.getClassType()).thenReturn(classType);
		return view;
	}
}
