package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
		when(results.findHistoryByMemberAccountId(memberId)).thenReturn(List.of(sessionResult, personalResult));

		List<MemberTrainingResultResponse> history = new MemberTrainingResultService(results)
			.viewOwnHistory(memberId);

		assertThat(history).hasSize(2);
		assertThat(history.get(0).classSessionId()).isEqualTo(sessionId);
		assertThat(history.get(0).coachAssignmentId()).isNull();
		assertThat(history.get(1).coachAssignmentId()).isEqualTo(assignmentId);
		assertThat(history.get(1).trainingPlanVersionId()).isEqualTo(planVersionId);
		assertThat(history.get(1).classSessionId()).isNull();
		verify(results).findHistoryByMemberAccountId(memberId);
	}

	@Test
	void returnsEmptyHistoryWithoutMembershipChecks() {
		UUID memberId = UUID.randomUUID();
		when(results.findHistoryByMemberAccountId(memberId)).thenReturn(List.of());

		assertThat(new MemberTrainingResultService(results).viewOwnHistory(memberId)).isEmpty();
	}
}
