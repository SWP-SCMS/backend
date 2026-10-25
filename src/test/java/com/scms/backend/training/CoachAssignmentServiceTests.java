package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.PtAssignmentImpactConfirmation;
import com.scms.backend.scheduling.PtAssignmentImpactResponse;
import com.scms.backend.scheduling.PtCoachAssignmentImpactService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CoachAssignmentServiceTests {

	@Mock
	private CoachAssignmentRepository assignments;

	@Mock
	private AccountRepository accounts;

	@Mock
	private AuditEventRepository audits;

	@Mock
	private NotificationWriter notifications;

	@Mock
	private PtCoachAssignmentImpactService impactService;

	private final Instant fixedNow = Instant.parse("2026-10-12T10:00:00Z");
	private final Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

	private CoachAssignmentService service;

	private UUID managerId;
	private UUID memberId;
	private UUID coachId;
	private UUID secondCoachId;

	private Account activeManager;
	private Account activeMember;
	private Account activeCoach;
	private Account activeSecondCoach;

	@BeforeEach
	void setUp() {
		service = new CoachAssignmentService(assignments, accounts, audits, notifications, clock, 20,
			impactService);

		managerId = UUID.randomUUID();
		memberId = UUID.randomUUID();
		coachId = UUID.randomUUID();
		secondCoachId = UUID.randomUUID();

		activeManager = new Account(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE, "Manager One",
				"0911000001", "manager@example.com", LocalDate.of(1985, 1, 1), "hashedPass");
		activeMember = new Account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member One",
				"0911000002", "member@example.com", LocalDate.of(1995, 2, 2), "hashedPass");
		activeCoach = new Account(coachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach Alpha",
				"0911000003", "coach.alpha@example.com", LocalDate.of(1990, 3, 3), "hashedPass");
		activeSecondCoach = new Account(secondCoachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach Beta",
				"0911000004", "coach.beta@example.com", LocalDate.of(1992, 4, 4), "hashedPass");
	}

	@Test
	void assign_whenManagerNotActive_throwsActorNotActiveManager() {
		Account inactiveManager = new Account(managerId, AccountRole.MANAGER, AccountStatus.INACTIVE, "Manager",
				"0911000001", "m@test.com", LocalDate.of(1985, 1, 1), "pass");
		when(accounts.findById(managerId)).thenReturn(Optional.of(inactiveManager));

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Initial assign");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "ACTOR_NOT_ACTIVE_MANAGER");
	}

	@Test
	void assign_whenMemberNotFound_throwsMemberNotFound() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.empty());

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Initial assign");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "MEMBER_NOT_FOUND");
	}

	@Test
	void assign_whenMemberSuspended_throwsMemberNotActive() {
		Account suspendedMember = new Account(memberId, AccountRole.MEMBER, AccountStatus.SUSPENDED, "Member",
				"0911000002", "mem@test.com", LocalDate.of(1995, 2, 2), "pass");
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(suspendedMember));

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Initial assign");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "MEMBER_NOT_ACTIVE");
	}

	@Test
	void assign_whenCoachNotFound_throwsCoachNotFound() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.empty());

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Initial assign");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "COACH_NOT_FOUND");
	}

	@Test
	void assign_whenCoachInactive_throwsCoachNotActive() {
		Account inactiveCoach = new Account(coachId, AccountRole.COACH, AccountStatus.INACTIVE, "Coach",
				"0911000003", "coach@test.com", LocalDate.of(1990, 3, 3), "pass");
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.of(inactiveCoach));

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Initial assign");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "COACH_NOT_ACTIVE");
	}

	@Test
	void assign_whenNoActivePlusMembership_throwsPlusMembershipRequired() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));
		when(assignments.hasActiveMembershipBenefit(memberId, "PERSONAL_COACHING", fixedNow)).thenReturn(false);

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Initial assign");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "PLUS_MEMBERSHIP_REQUIRED");
	}

	@Test
	void assign_whenMembershipFrozen_rejectsWithoutChangingExistingAssignments() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));
		when(assignments.hasActiveMembershipFreeze(memberId)).thenReturn(true);

		assertThatThrownBy(() -> service.assign(managerId, memberId, new CoachAssignmentAssignRequest(coachId, "new assignment")))
			.isInstanceOf(CoachAssignmentException.class).extracting("code").isEqualTo("MEMBERSHIP_FROZEN");
		verify(assignments, never()).findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE);
	}

	@Test
	void assign_whenNoExistingAssignment_createsActiveAssignmentAndAuditsAndNotifies() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));
		when(assignments.hasActiveMembershipBenefit(memberId, "PERSONAL_COACHING", fixedNow)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(assignments.saveAndFlush(any(CoachAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "First coaching program");

		CoachAssignmentResponse response = service.assign(managerId, memberId, request);

		assertThat(response).isNotNull();
		assertThat(response.memberAccountId()).isEqualTo(memberId);
		assertThat(response.coachAccountId()).isEqualTo(coachId);
		assertThat(response.coachFullName()).isEqualTo("Coach Alpha");
		assertThat(response.status()).isEqualTo(CoachAssignmentStatus.ACTIVE);
		assertBenefitRequested("PERSONAL_COACHING");
		assertThat(response.startsAt()).isEqualTo(fixedNow);
		assertThat(response.endsAt()).isNull();

		// Verify Audit event
		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(auditCaptor.capture());
		AuditEvent audit = auditCaptor.getValue();
		assertThat(audit.getAction()).isEqualTo("COACH_ASSIGNED");
		assertThat(audit.getTargetType()).isEqualTo("COACH_ASSIGNMENT");
		assertThat(audit.getReason()).isEqualTo("First coaching program");
		assertThat(audit.getBeforeData()).isNull();
		assertThat(audit.getAfterData()).containsEntry("status", "ACTIVE");

		// Verify Notifications (1 for Member, 1 for Coach)
		verify(notifications).write(eq("COACH_ASSIGNED:" + response.id() + ":MEMBER"), eq(memberId),
				eq("TRAINING_COACH_ASSIGNED"), eq("COACH_ASSIGNMENT"), eq(response.id()), any());
		verify(notifications).write(eq("COACH_ASSIGNED:" + response.id() + ":COACH"), eq(coachId),
				eq("TRAINING_COACH_ASSIGNED"), eq("COACH_ASSIGNMENT"), eq(response.id()), any());
	}

	@Test
	void assign_whenCoachIsAtCapacity_rejectsBeforeMutation() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));
		when(assignments.hasActiveMembershipBenefit(memberId, "PERSONAL_COACHING", fixedNow)).thenReturn(true);
		when(assignments.findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.empty());
		when(assignments.countByCoachAccountIdAndStatus(coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(20L);

		assertThatThrownBy(() -> service.assign(managerId, memberId,
				new CoachAssignmentAssignRequest(coachId, "At capacity")))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "COACH_CAPACITY_EXCEEDED");
		verify(assignments, never()).saveAndFlush(any());
	}

	private void assertBenefitRequested(String benefitCode) {
		assertThat(mockingDetails(assignments).getInvocations()).anySatisfy(invocation -> {
			assertThat(invocation.getMethod().getName()).isEqualTo("hasActiveMembershipBenefit");
			assertThat(invocation.getArguments()).containsExactly(memberId, benefitCode, fixedNow);
		});
	}

	@Test
	void assign_whenAlreadyAssignedToSameCoach_throwsAlreadyAssignedToThisCoach() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));
		when(assignments.hasActiveMembershipBenefit(memberId, "PERSONAL_COACHING", fixedNow)).thenReturn(true);

		CoachAssignment current = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, fixedNow.minusSeconds(3600), fixedNow.minusSeconds(3600));
		when(assignments.findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.of(current));

		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(coachId, "Reassign same");

		assertThatThrownBy(() -> service.assign(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "COACH_ALREADY_ASSIGNED");

		verify(assignments, never()).saveAndFlush(any());
	}

	@Test
	void assign_whenReassigningToDifferentCoach_endsPreviousAssignmentAndCreatesNewAndAuditsAndNotifiesAll() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(accounts.findById(secondCoachId)).thenReturn(Optional.of(activeSecondCoach));
		when(assignments.hasActiveMembershipBenefit(memberId, "PERSONAL_COACHING", fixedNow)).thenReturn(true);

		CoachAssignment current = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, fixedNow.minusSeconds(7200), fixedNow.minusSeconds(7200));
		when(assignments.findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.of(current));
		when(assignments.saveAndFlush(any(CoachAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PtAssignmentImpactConfirmation confirmation = new PtAssignmentImpactConfirmation(List.of(), List.of());
		PtAssignmentImpactResponse processed = PtAssignmentImpactResponse.empty();
		when(impactService.resolve(managerId, memberId, coachId, fixedNow, confirmation)).thenReturn(processed);
		CoachAssignmentAssignRequest request = new CoachAssignmentAssignRequest(secondCoachId, "Switching coach",
			confirmation);

		CoachAssignmentResponse response = service.assign(managerId, memberId, request);

		assertThat(response).isNotNull();
		assertThat(response.coachAccountId()).isEqualTo(secondCoachId);
		assertThat(response.status()).isEqualTo(CoachAssignmentStatus.ACTIVE);
		assertThat(response.processedImpact()).isSameAs(processed);
		verify(impactService).resolve(managerId, memberId, coachId, fixedNow, confirmation);

		// Old assignment must be ended
		assertThat(current.getStatus()).isEqualTo(CoachAssignmentStatus.ENDED);
		assertThat(current.getEndsAt()).isEqualTo(fixedNow);
		assertThat(current.getEndReason()).isEqualTo("Switching coach");

		// Audit verification
		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(auditCaptor.capture());
		AuditEvent audit = auditCaptor.getValue();
		assertThat(audit.getAction()).isEqualTo("COACH_REASSIGNED");
		assertThat(audit.getBeforeData()).containsEntry("coachAccountId", coachId.toString());
		assertThat(audit.getAfterData()).containsEntry("coachAccountId", secondCoachId.toString());

		// Notifications: Member, New Coach, Old Coach
		verify(notifications).write(eq("COACH_REASSIGNED:" + response.id() + ":MEMBER"), eq(memberId),
				eq("TRAINING_COACH_ASSIGNED"), eq("COACH_ASSIGNMENT"), eq(response.id()), any());
		verify(notifications).write(eq("COACH_REASSIGNED:" + response.id() + ":NEW_COACH"), eq(secondCoachId),
				eq("TRAINING_COACH_ASSIGNED"), eq("COACH_ASSIGNMENT"), eq(response.id()), any());
		verify(notifications).write(eq("COACH_ASSIGNMENT_ENDED:" + current.getId() + ":OLD_COACH"), eq(coachId),
				eq("TRAINING_COACH_ENDED"), eq("COACH_ASSIGNMENT"), eq(current.getId()), any());
	}

	@Test
	void getActive_whenAssignmentExists_returnsActiveResponse() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));

		CoachAssignment current = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, fixedNow, fixedNow);
		when(assignments.findByMemberAccountIdAndStatus(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.of(current));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));

		Optional<CoachAssignmentResponse> result = service.getActive(managerId, memberId);

		assertThat(result).isPresent();
		assertThat(result.get().id()).isEqualTo(current.getId());
		assertThat(result.get().coachFullName()).isEqualTo("Coach Alpha");
	}

	@Test
	void getActive_whenNoActiveAssignment_returnsEmpty() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(assignments.findByMemberAccountIdAndStatus(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.empty());

		Optional<CoachAssignmentResponse> result = service.getActive(managerId, memberId);

		assertThat(result).isEmpty();
	}

	@Test
	void listHistory_returnsPaginatedResponses() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));

		CoachAssignment a1 = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ENDED, fixedNow.minusSeconds(86400), fixedNow.minusSeconds(86400));
		a1.end(fixedNow.minusSeconds(43200), "Ended");
		CoachAssignment a2 = new CoachAssignment(UUID.randomUUID(), memberId, secondCoachId, managerId,
				CoachAssignmentStatus.ACTIVE, fixedNow.minusSeconds(43200), fixedNow.minusSeconds(43200));

		Pageable pageable = PageRequest.of(0, 10);
		when(assignments.findByMemberAccountIdOrderByCreatedAtDesc(memberId, pageable))
				.thenReturn(new PageImpl<>(List.of(a2, a1), pageable, 2));
		when(accounts.findAllById(any())).thenReturn(List.of(activeCoach, activeSecondCoach));

		CoachAssignmentPageResponse result = service.listHistory(managerId, memberId, pageable);

		assertThat(result).isNotNull();
		assertThat(result.content()).hasSize(2);
		assertThat(result.totalElements()).isEqualTo(2);
	}

	@Test
	void endAssignment_whenActiveAssignmentExists_endsAssignmentAndAuditsAndNotifies() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));

		CoachAssignment current = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, fixedNow.minusSeconds(3600), fixedNow.minusSeconds(3600));
		when(assignments.findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.of(current));
		when(assignments.saveAndFlush(any(CoachAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(accounts.findById(coachId)).thenReturn(Optional.of(activeCoach));

		CoachAssignmentEndRequest request = new CoachAssignmentEndRequest("Coach relocated");

		CoachAssignmentResponse response = service.endAssignment(managerId, memberId, request);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo(CoachAssignmentStatus.ENDED);
		assertThat(response.endsAt()).isEqualTo(fixedNow);
		assertThat(response.endReason()).isEqualTo("Coach relocated");

		// Audit
		ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(auditCaptor.capture());
		AuditEvent audit = auditCaptor.getValue();
		assertThat(audit.getAction()).isEqualTo("COACH_ASSIGNMENT_ENDED");
		assertThat(audit.getReason()).isEqualTo("Coach relocated");

		// Notifications (Member and Coach)
		verify(notifications).write(eq("COACH_ASSIGNMENT_ENDED:" + current.getId() + ":MEMBER"), eq(memberId),
				eq("TRAINING_COACH_ENDED"), eq("COACH_ASSIGNMENT"), eq(current.getId()), any());
		verify(notifications).write(eq("COACH_ASSIGNMENT_ENDED:" + current.getId() + ":COACH"), eq(coachId),
				eq("TRAINING_COACH_ENDED"), eq("COACH_ASSIGNMENT"), eq(current.getId()), any());
	}

	@Test
	void endAssignment_whenNoActiveAssignment_throwsActiveAssignmentNotFound() {
		when(accounts.findById(managerId)).thenReturn(Optional.of(activeManager));
		when(accounts.findById(memberId)).thenReturn(Optional.of(activeMember));
		when(assignments.findByMemberAccountIdAndStatusForUpdate(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.empty());

		CoachAssignmentEndRequest request = new CoachAssignmentEndRequest("Coach relocated");

		assertThatThrownBy(() -> service.endAssignment(managerId, memberId, request))
				.isInstanceOf(CoachAssignmentException.class)
				.hasFieldOrPropertyWithValue("code", "COACH_ASSIGNMENT_NOT_FOUND");
	}
}
