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
class TrainingPlanServiceTests {

	@Mock TrainingPlanRepository plans;
	@Mock TrainingPlanVersionRepository versions;
	@Mock CoachAssignmentRepository assignments;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;

	private final UUID coachId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-13T06:00:00Z");
	private TrainingPlanService service;

	@BeforeEach
	void setUp() {
		service = new TrainingPlanService(plans, versions, assignments, accounts, audits,
				Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void createPersistsPlanVersionOneAndAuditForCurrentPersonalCoach() {
		CoachAssignment assignment = activeAssignment();
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActivePlusMembership(memberId, now)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
		when(plans.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Map<String, Object> content = Map.of("goal", "Build strength", "sessionsPerWeek", 3);
		TrainingPlanResponse response = service.create(coachId, memberId,
				new CreateTrainingPlanRequest("  Strength foundation  ", content));

		assertThat(response.memberAccountId()).isEqualTo(memberId);
		assertThat(response.coachAssignmentId()).isEqualTo(assignment.getId());
		assertThat(response.title()).isEqualTo("Strength foundation");
		assertThat(response.status()).isEqualTo(TrainingPlanStatus.ACTIVE);
		assertThat(response.versionNumber()).isEqualTo(1);
		assertThat(response.content()).isEqualTo(content);
		assertThat(response.createdByAccountId()).isEqualTo(coachId);
		assertThat(response.createdAt()).isEqualTo(now);

		verify(assignments).endActiveAssignmentIfPlusExpired(memberId, now);
		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(auditCaptor.capture());
		assertThat(auditCaptor.getValue().getActorAccountId()).isEqualTo(coachId);
		assertThat(auditCaptor.getValue().getAction()).isEqualTo("TRAINING_PLAN_CREATED");
		assertThat(auditCaptor.getValue().getTargetType()).isEqualTo("TRAINING_PLAN");
		assertThat(auditCaptor.getValue().getTargetId()).isEqualTo(response.id());
		assertThat(auditCaptor.getValue().getAfterData()).containsEntry("versionNumber", 1);
	}

	@Test
	void createRejectsAccountThatIsNotAnActiveCoach() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> service.create(coachId, memberId, request()))
				.isInstanceOfSatisfying(TrainingPlanException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("ACTOR_NOT_ACTIVE_COACH"));

		verify(assignments, never()).endActiveAssignmentIfPlusExpired(any(), any());
		verify(plans, never()).saveAndFlush(any());
	}

	@Test
	void createRejectsMemberWithoutActivePlusMembershipAfterReconciliation() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActivePlusMembership(memberId, now)).thenReturn(false);

		assertThatThrownBy(() -> service.create(coachId, memberId, request()))
				.isInstanceOfSatisfying(TrainingPlanException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("PLUS_MEMBERSHIP_REQUIRED"));

		verify(assignments).endActiveAssignmentIfPlusExpired(memberId, now);
		verify(plans, never()).saveAndFlush(any());
	}

	@Test
	void createRejectsCoachOutsideCurrentAssignmentScope() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
		when(assignments.hasActivePlusMembership(memberId, now)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(coachId, memberId, request()))
				.isInstanceOfSatisfying(TrainingPlanException.class,
						exception -> assertThat(exception.getCode()).isEqualTo("COACH_ASSIGNMENT_NOT_FOUND"));

		verify(plans, never()).saveAndFlush(any());
	}

	private CreateTrainingPlanRequest request() {
		return new CreateTrainingPlanRequest("Strength plan", Map.of("goal", "Strength"));
	}

	private CoachAssignment activeAssignment() {
		return new CoachAssignment(UUID.randomUUID(), memberId, coachId, UUID.randomUUID(),
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(3600), now.minusSeconds(3600));
	}
}
