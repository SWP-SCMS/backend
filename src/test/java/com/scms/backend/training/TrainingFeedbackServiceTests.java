package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockingDetails;
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
class TrainingFeedbackServiceTests {

	@Mock TrainingFeedbackRepository feedback;
	@Mock TrainingPlanVersionRepository planVersions;
	@Mock TrainingResultRepository results;
	@Mock CoachAssignmentRepository assignments;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;

	private final UUID coachId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID planVersionId = UUID.randomUUID();
	private final UUID resultId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-14T08:00:00Z");
	private TrainingFeedbackService service;

	@BeforeEach
	void setUp() {
		service = new TrainingFeedbackService(feedback, planVersions, results, assignments, accounts, audits,
				Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void createForPlanVersionPersistsAssignmentAndAudit() {
		CoachAssignment assignment = permitCurrentCoach();
		when(planVersions.findByIdAndMemberAccountId(planVersionId, memberId))
				.thenReturn(Optional.of(planVersion()));
		when(feedback.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		TrainingFeedbackResponse response = service.create(coachId, memberId, planRequest("Good progress"));

		assertThat(response.coachAssignmentId()).isEqualTo(assignment.getId());
		assertThat(response.trainingPlanVersionId()).isEqualTo(planVersionId);
		assertThat(response.trainingResultId()).isNull();
		assertThat(response.content()).isEqualTo("Good progress");
		assertBenefitRequested("TRAINING_FEEDBACK");
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_FEEDBACK_CREATED");
		assertThat(audit.getValue().getAfterData())
				.containsEntry("trainingPlanVersionId", planVersionId.toString());
	}

	private void assertBenefitRequested(String benefitCode) {
		assertThat(mockingDetails(assignments).getInvocations()).anySatisfy(invocation -> {
			assertThat(invocation.getMethod().getName()).isEqualTo("hasActiveMembershipBenefit");
			assertThat(invocation.getArguments()).containsExactly(memberId, benefitCode, now);
		});
	}

	@Test
	void createForPersonalResultAcceptsOwnedResult() {
		permitCurrentCoach();
		when(results.findPersonalByIdAndMemberAccountId(resultId, memberId))
				.thenReturn(Optional.of(personalResult()));
		when(feedback.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		TrainingFeedbackResponse response = service.create(coachId, memberId, resultRequest("Keep form"));

		assertThat(response.trainingPlanVersionId()).isNull();
		assertThat(response.trainingResultId()).isEqualTo(resultId);
	}

	@Test
	void createRejectsSessionOrAnotherMembersResult() {
		permitCurrentCoach();
		when(results.findPersonalByIdAndMemberAccountId(resultId, memberId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, memberId, resultRequest("Denied")))
				.isInstanceOfSatisfying(TrainingFeedbackException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("TRAINING_RESULT_NOT_FOUND"));
		verify(feedback, never()).saveAndFlush(any());
	}

	@Test
	void createRejectsCoachOutsideCurrentAssignment() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActiveMembershipBenefit(memberId, "TRAINING_FEEDBACK", now)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, memberId, planRequest("Denied")))
				.isInstanceOfSatisfying(TrainingFeedbackException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("COACH_ASSIGNMENT_NOT_FOUND"));
		verify(planVersions, never()).findByIdAndMemberAccountId(any(), any());
	}

	@Test
	void createRejectsExpiredPlusAndEndsAssignment() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActiveMembershipBenefit(memberId, "TRAINING_FEEDBACK", now)).thenReturn(false);

		assertThatThrownBy(() -> service.create(coachId, memberId, planRequest("Denied")))
				.isInstanceOfSatisfying(TrainingFeedbackException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));
		verify(assignments).endActiveAssignmentIfPersonalCoachingExpired(memberId, now);
		verify(feedback, never()).saveAndFlush(any());
	}

	@Test
	void updatePreservesOriginalAssignmentAndAuditsBeforeAndAfter() {
		CoachAssignment assignment = permitCurrentCoach();
		when(results.findPersonalByIdAndMemberAccountId(resultId, memberId))
				.thenReturn(Optional.of(personalResult()));
		UUID feedbackId = UUID.randomUUID();
		TrainingFeedback existing = new TrainingFeedback(feedbackId, memberId, assignment.getId(),
				planVersionId, null, "Before", coachId, now.minusSeconds(3600));
		when(feedback.findPersonalByIdAndMemberAccountIdForUpdate(feedbackId, memberId))
				.thenReturn(Optional.of(existing));
		when(feedback.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		TrainingFeedbackResponse response = service.update(coachId, memberId, feedbackId,
				resultRequest("After"));

		assertThat(response.coachAssignmentId()).isEqualTo(assignment.getId());
		assertThat(response.trainingPlanVersionId()).isNull();
		assertThat(response.trainingResultId()).isEqualTo(resultId);
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("TRAINING_FEEDBACK_UPDATED");
		assertThat(audit.getValue().getBeforeData()).containsEntry("content", "Before");
		assertThat(audit.getValue().getAfterData()).containsEntry("content", "After");
	}

	private CoachAssignment permitCurrentCoach() {
		CoachAssignment assignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, UUID.randomUUID(),
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(7200), now.minusSeconds(7200));
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActiveMembershipBenefit(memberId, "TRAINING_FEEDBACK", now)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
		return assignment;
	}

	private PersonalTrainingFeedbackRequest planRequest(String content) {
		return new PersonalTrainingFeedbackRequest(planVersionId, null, content);
	}

	private PersonalTrainingFeedbackRequest resultRequest(String content) {
		return new PersonalTrainingFeedbackRequest(null, resultId, content);
	}

	private TrainingPlanVersion planVersion() {
		return new TrainingPlanVersion(planVersionId, UUID.randomUUID(), 1, Map.of(), coachId, now);
	}

	private TrainingResult personalResult() {
		return new TrainingResult(resultId, memberId, UUID.randomUUID(), planVersionId,
				now.minusSeconds(600), Map.of("sets", 3), coachId, now.minusSeconds(600));
	}
}
