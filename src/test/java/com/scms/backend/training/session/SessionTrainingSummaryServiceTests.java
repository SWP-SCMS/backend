package com.scms.backend.training.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.session.ClassSessionRepository;
import com.scms.backend.training.TrainingFeedbackException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class SessionTrainingSummaryServiceTests {

	private final UUID coach = UUID.randomUUID(), sessionId = UUID.randomUUID(), member = UUID.randomUUID(), resultId = UUID.randomUUID();

	private final SessionTrainingResultService results = mock(SessionTrainingResultService.class);

	private final SessionTrainingFeedbackService feedback = mock(SessionTrainingFeedbackService.class);

	private final ClassSessionRepository sessions = mock(ClassSessionRepository.class);

	private final Instant at = Instant.parse("2026-01-01T10:00:00Z");

	private final RecordingTransactions transactions = new RecordingTransactions();

	private SessionTrainingSummaryService service() {
		var session = mock(ClassSession.class);
		when(session.getStartTime()).thenReturn(at);
		when(sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coach)).thenReturn(Optional.of(session));
		var proxy = new ProxyFactory(new SessionTrainingSummaryService(results, feedback, sessions));
		proxy.addAdvice(new TransactionInterceptor(transactions, new AnnotationTransactionAttributeSource()));
		return (SessionTrainingSummaryService) proxy.getProxy();
	}

	private SessionTrainingResultResponse result() {
		return new SessionTrainingResultResponse(resultId, member, sessionId, at, Map.of("notes", "Old", "repetitions", 10), coach, at, at);
	}

	@Test
	void createsBothAndLinksFeedbackToSavedResultInOneTransaction() {
		var service = service();
		when(results.get(coach, sessionId, member)).thenReturn(Optional.empty());
		when(feedback.get(coach, sessionId, member)).thenReturn(Optional.empty());
		when(results.create(eq(coach), eq(sessionId), eq(member), any())).thenReturn(result());
		service.save(coach, sessionId, member, " Done ", " Good ");
		verify(feedback).create(coach, sessionId, member, new SessionTrainingFeedbackRequest(resultId, "Good"));
		assertThat(transactions.commits).isEqualTo(1);
		assertThat(transactions.rollbacks).isZero();
	}

	@Test
	void retryUpdatesCanonicalRecordsAndPreservesExistingMetrics() {
		var service = service();
		UUID feedbackId = UUID.randomUUID();
		when(results.get(coach, sessionId, member)).thenReturn(Optional.of(result()));
		when(results.update(eq(coach), eq(sessionId), eq(member), eq(resultId), any())).thenReturn(result());
		when(feedback.get(coach, sessionId, member)).thenReturn(Optional.of(new SessionTrainingFeedbackResponse(feedbackId, member, sessionId, resultId, "Old", coach, at, at)));
		service.save(coach, sessionId, member, " New ", " Updated ");
		verify(results).update(coach, sessionId, member, resultId, new SessionTrainingResultRequest(at, Map.of("notes", "New", "repetitions", 10)));
		verify(feedback).update(coach, sessionId, member, feedbackId, new SessionTrainingFeedbackRequest(resultId, "Updated"));
		verify(results, never()).create(any(), any(), any(), any());
		assertThat(transactions.commits).isEqualTo(1);
	}

	@Test
	void feedbackFailureRollsBackCombinedTransaction() {
		var service = service();
		when(results.get(coach, sessionId, member)).thenReturn(Optional.empty());
		when(feedback.get(coach, sessionId, member)).thenReturn(Optional.empty());
		when(results.create(eq(coach), eq(sessionId), eq(member), any())).thenReturn(result());
		when(feedback.create(any(), any(), any(), any())).thenThrow(TrainingFeedbackException.plusMembershipRequired());
		assertThatThrownBy(() -> service.save(coach, sessionId, member, "Done", "Good")).isInstanceOf(TrainingFeedbackException.class);
		assertThat(transactions.rollbacks).isEqualTo(1);
		assertThat(transactions.commits).isZero();
	}

	static class RecordingTransactions extends AbstractPlatformTransactionManager {

		int commits, rollbacks;

		protected Object doGetTransaction() {
			return new Object();
		}

		protected void doBegin(Object transaction, TransactionDefinition definition) {
		}

		protected void doCommit(DefaultTransactionStatus status) {
			commits++;
		}

		protected void doRollback(DefaultTransactionStatus status) {
			rollbacks++;
		}
	}
}
