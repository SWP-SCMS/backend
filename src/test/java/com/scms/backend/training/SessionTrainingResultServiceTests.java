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
import com.scms.backend.scheduling.BookingRepository;
import com.scms.backend.scheduling.BookingStatus;
import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;

@ExtendWith(MockitoExtension.class)
class SessionTrainingResultServiceTests {

	@Mock TrainingResultRepository results;
	@Mock CoachAssignmentRepository assignments;
	@Mock ClassSessionRepository sessions;
	@Mock BookingRepository bookings;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;

	private final UUID coachId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID sessionId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-14T08:00:00Z");
	private SessionTrainingResultService service;

	@BeforeEach
	void setUp() {
		service = new SessionTrainingResultService(results, assignments, sessions, bookings, accounts, audits,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void createLookupLocksOnlyCanonicalSessionResult() throws NoSuchMethodException {
		Query query = TrainingResultRepository.class
			.getMethod("findSessionByClassSessionIdAndMemberAccountIdForUpdate", UUID.class, UUID.class)
			.getAnnotation(Query.class);

		assertThat(query.nativeQuery()).isTrue();
		assertThat(query.value()).contains("class_session_id = :sessionId", "member_account_id = :memberId",
			"coach_assignment_id is null", "legacy_duplicate = false", "for update");
	}

	@Test
	void createPersistsSessionResultWithoutPersonalAssignmentAndAuditsIt() {
		permitTeachingCoach();
		permitTrainingResultBenefit();
		when(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, sessionId,
			BookingStatus.BOOKED)).thenReturn(true);
		when(results.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		SessionTrainingResultResponse response = service.create(coachId, sessionId, memberId, request("Good form"));

		assertThat(response.memberAccountId()).isEqualTo(memberId);
		assertThat(response.classSessionId()).isEqualTo(sessionId);
		assertThat(response.content()).containsEntry("notes", "Good form");
		verify(assignments).hasActiveMembershipBenefit(memberId, "TRAINING_RESULT", now);
		ArgumentCaptor<TrainingResult> saved = ArgumentCaptor.forClass(TrainingResult.class);
		verify(results).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getCoachAssignmentId()).isNull();
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_RESULT_CREATED");
		assertThat(audit.getValue().getAfterData()).containsEntry("classSessionId", sessionId.toString());
	}

	@Test
	void createReturnsExistingSessionResultWithoutDuplicatingIt() {
		permitTeachingCoach();
		permitTrainingResultBenefit();
		when(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, sessionId,
			BookingStatus.BOOKED)).thenReturn(true);
		TrainingResult existing = TrainingResult.forClassSession(UUID.randomUUID(), memberId, sessionId,
			now.minusSeconds(600), Map.of("notes", "Already recorded"), coachId, now.minusSeconds(600));
		when(results.findSessionByClassSessionIdAndMemberAccountIdForUpdate(sessionId, memberId))
			.thenReturn(Optional.of(existing));

		SessionTrainingResultResponse response = service.create(coachId, sessionId, memberId, request("Retry"));

		assertThat(response.id()).isEqualTo(existing.getId());
		verify(results, never()).saveAndFlush(any());
		verify(audits, never()).save(any());
	}

	@Test
	void createRejectsCoachWhoDoesNotTeachGroupOrYogaSession() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, sessionId, memberId, request("Denied")))
			.isInstanceOfSatisfying(TrainingResultException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("SESSION_NOT_FOUND"));

		verify(bookings, never()).existsByMemberAccountIdAndClassSessionIdAndStatus(any(), any(), any());
		verify(results, never()).saveAndFlush(any());
	}

	@Test
	void createRejectsMemberWithoutBookedPlace() {
		permitTeachingCoach();
		when(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, sessionId,
			BookingStatus.BOOKED)).thenReturn(false);

		assertThatThrownBy(() -> service.create(coachId, sessionId, memberId, request("Denied")))
			.isInstanceOfSatisfying(TrainingResultException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("SESSION_MEMBER_NOT_FOUND"));

		verify(results, never()).saveAndFlush(any());
	}

	@Test
	void createRejectsMemberWithoutTrainingResultBenefit() {
		permitTeachingCoach();
		when(bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, sessionId,
			BookingStatus.BOOKED)).thenReturn(true);

		assertThatThrownBy(() -> service.create(coachId, sessionId, memberId, request("Denied")))
			.isInstanceOfSatisfying(TrainingResultException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));
		verify(assignments).hasActiveMembershipBenefit(memberId, "TRAINING_RESULT", now);
		verify(results, never()).saveAndFlush(any());
	}

	@Test
	void updatePreservesSessionOwnershipAndRecordsBeforeAndAfterHistory() {
		permitTeachingCoach();
		permitTrainingResultBenefit();
		UUID resultId = UUID.randomUUID();
		TrainingResult result = TrainingResult.forClassSession(resultId, memberId, sessionId,
			now.minusSeconds(3600), Map.of("notes", "Before"), coachId, now.minusSeconds(3600));
		when(results.findSessionByIdAndMemberAccountIdForUpdate(resultId, sessionId, memberId))
			.thenReturn(Optional.of(result));
		when(results.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		SessionTrainingResultResponse response = service.update(coachId, sessionId, memberId, resultId,
			request("After"));

		assertThat(response.content()).containsEntry("notes", "After");
		verify(assignments).hasActiveMembershipBenefit(memberId, "TRAINING_RESULT", now);
		assertThat(result.getCoachAssignmentId()).isNull();
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_RESULT_UPDATED");
		assertThat(audit.getValue().getBeforeData().get("content")).isEqualTo(Map.of("notes", "Before"));
		assertThat(audit.getValue().getAfterData().get("content")).isEqualTo(Map.of("notes", "After"));
	}

	private void permitTeachingCoach() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(sessions.findOwnedGroupOrYogaByIdForRead(sessionId, coachId))
			.thenReturn(Optional.of(mock(ClassSession.class)));
	}

	private void permitTrainingResultBenefit() {
		when(assignments.hasActiveMembershipBenefit(memberId, "TRAINING_RESULT", now)).thenReturn(true);
	}

	private SessionTrainingResultRequest request(String notes) {
		return new SessionTrainingResultRequest(now.minusSeconds(600), Map.of("notes", notes));
	}
}
