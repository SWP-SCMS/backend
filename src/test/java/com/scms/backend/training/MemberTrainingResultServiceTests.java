package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.scheduling.SportClassType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberTrainingResultServiceTests {

	@Mock TrainingResultRepository results;

	@Test
	void returnsOnlyRepositoryScopedPersonalAndSessionHistory() {
		UUID memberId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		UUID assignmentId = UUID.randomUUID();
		UUID planVersionId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-14T08:00:00Z");
		TrainingResult sessionResult = TrainingResult.forClassSession(UUID.randomUUID(), memberId, sessionId,
			now, Map.of("notes", "Yoga"), coachId, now);
		TrainingResult personalResult = new TrainingResult(UUID.randomUUID(), memberId, assignmentId,
			planVersionId, now.minusSeconds(3600), Map.of("notes", "Personal"), coachId,
			now.minusSeconds(3600));
		var sessionView = view(sessionResult, "Coach One", "PT 1-1", now, SportClassType.PT_1_1);
		var personalView = view(personalResult, "Coach One", null, null, null);
		when(results.findHistoryByMemberAccountId(memberId)).thenReturn(List.of(sessionView, personalView));

		List<MemberTrainingResultResponse> history = new MemberTrainingResultService(results)
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
		assertThat(history.get(1).coachName()).isEqualTo("Coach One");
		assertThat(history.get(1).sessionName()).isNull();
		verify(results).findHistoryByMemberAccountId(memberId);
	}

	@Test
	void returnsEmptyHistoryWithoutMembershipChecks() {
		UUID memberId = UUID.randomUUID();
		when(results.findHistoryByMemberAccountId(memberId)).thenReturn(List.of());

		assertThat(new MemberTrainingResultService(results).viewOwnHistory(memberId)).isEmpty();
	}

	private TrainingResultRepository.MemberTrainingResultHistoryView view(TrainingResult value,
			String coachName, String sessionName, Instant sessionStartTime, SportClassType classType) {
		var view = mock(TrainingResultRepository.MemberTrainingResultHistoryView.class);
		when(view.getResult()).thenReturn(value);
		when(view.getCoachName()).thenReturn(coachName);
		when(view.getSessionName()).thenReturn(sessionName);
		when(view.getSessionStartTime()).thenReturn(sessionStartTime);
		when(view.getClassType()).thenReturn(classType);
		return view;
	}
}
