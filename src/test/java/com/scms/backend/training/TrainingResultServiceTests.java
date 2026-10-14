package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingResultServiceTests {

	@Mock TrainingResultRepository results;
	@Mock TrainingPlanVersionRepository planVersions;
	@Mock CoachAssignmentRepository assignments;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;

	private final UUID coachId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID planVersionId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-13T08:00:00Z");
	private TrainingResultService service;

	@BeforeEach
	void setUp() {
		service = new TrainingResultService(results, planVersions, assignments, accounts, audits,
				Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void createPersistsPersonalResultAndAuditForCurrentCoach() {
		CoachAssignment assignment = permitCurrentCoach();
		when(planVersions.findByIdAndMemberAccountId(planVersionId, memberId))
				.thenReturn(Optional.of(planVersion()));
		when(results.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		TrainingResultResponse response = service.create(coachId, memberId, request("Completed 3 sets"));

		assertThat(response.memberAccountId()).isEqualTo(memberId);
		assertThat(response.coachAssignmentId()).isEqualTo(assignment.getId());
		assertThat(response.trainingPlanVersionId()).isEqualTo(planVersionId);
		assertThat(response.activityAt()).isEqualTo(now.minusSeconds(600));
		assertThat(response.content()).containsEntry("notes", "Completed 3 sets");
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_RESULT_CREATED");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(coachId);
		assertThat(audit.getValue().getAfterData()).containsEntry("memberAccountId", memberId.toString());
	}

	@Test
	void createRejectsPlanVersionOwnedByAnotherMember() {
		permitCurrentCoach();
		when(planVersions.findByIdAndMemberAccountId(planVersionId, memberId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, memberId, request("Result")))
				.isInstanceOfSatisfying(TrainingResultException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("TRAINING_PLAN_VERSION_NOT_FOUND"));

		verify(results, never()).saveAndFlush(any());
		verify(audits, never()).save(any());
	}

	@Test
	void createRejectsCoachOutsideCurrentAssignment() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActivePlusMembership(memberId, now)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, memberId, request("Result")))
				.isInstanceOfSatisfying(TrainingResultException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("COACH_ASSIGNMENT_NOT_FOUND"));

		verify(planVersions, never()).findByIdAndMemberAccountId(any(), any());
	}

	@Test
	void createRejectsExpiredPlusAfterEndingAssignment() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActivePlusMembership(memberId, now)).thenReturn(false);

		assertThatThrownBy(() -> service.create(coachId, memberId, request("Result")))
				.isInstanceOfSatisfying(TrainingResultException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));

		verify(assignments).endActiveAssignmentIfPlusExpired(memberId, now);
		verify(results, never()).saveAndFlush(any());
	}

	@Test
	void updateRecordsBeforeAndAfterHistoryWithoutChangingAssignment() {
		CoachAssignment assignment = permitCurrentCoach();
		when(planVersions.findByIdAndMemberAccountId(planVersionId, memberId))
				.thenReturn(Optional.of(planVersion()));
		UUID resultId = UUID.randomUUID();
		TrainingResult result = new TrainingResult(resultId, memberId, assignment.getId(), planVersionId,
				now.minusSeconds(3600), Map.of("notes", "Before"), coachId, now.minusSeconds(3600));
		when(results.findPersonalByIdAndMemberAccountIdForUpdate(resultId, memberId))
				.thenReturn(Optional.of(result));
		when(results.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		TrainingResultResponse response = service.update(coachId, memberId, resultId, request("After"));

		assertThat(response.coachAssignmentId()).isEqualTo(assignment.getId());
		assertThat(response.content()).containsEntry("notes", "After");
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_RESULT_UPDATED");
		assertThat(audit.getValue().getBeforeData().get("content")).isEqualTo(Map.of("notes", "Before"));
		assertThat(audit.getValue().getAfterData().get("content")).isEqualTo(Map.of("notes", "After"));
	}

	@Test
	void updateCannotReachAnotherMembersOrSessionResult() {
		permitCurrentCoach();
		when(planVersions.findByIdAndMemberAccountId(planVersionId, memberId))
				.thenReturn(Optional.of(planVersion()));
		UUID resultId = UUID.randomUUID();
		when(results.findPersonalByIdAndMemberAccountIdForUpdate(resultId, memberId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(coachId, memberId, resultId, request("After")))
				.isInstanceOfSatisfying(TrainingResultException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("TRAINING_RESULT_NOT_FOUND"));
	}

	private CoachAssignment permitCurrentCoach() {
		CoachAssignment assignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, UUID.randomUUID(),
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(7200));
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActivePlusMembership(memberId, now)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
		return assignment;
	}

	private PersonalTrainingResultRequest request(String notes) {
		return new PersonalTrainingResultRequest(planVersionId, now.minusSeconds(600), Map.of("notes", notes));
	}

	private TrainingPlanVersion planVersion() {
		return new TrainingPlanVersion(planVersionId, UUID.randomUUID(), 1, Map.of(), coachId, now);
	}
}
