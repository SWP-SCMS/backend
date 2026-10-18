package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.manager.StaffAccountValidationException;
import com.scms.backend.membership.MembershipRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AssignmentDiscoveryServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");

	@Mock CoachAssignmentRepository assignments;
	@Mock MembershipRepository memberships;
	@Mock AccountRepository accounts;

	private AssignmentDiscoveryService service;
	private final UUID managerId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new AssignmentDiscoveryService(assignments, memberships, accounts,
			Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void candidatesReconcileBeforePagedProjectionAndMapTheResponse() {
		activeManager();
		var pageable = PageRequest.of(1, 2);
		var view = mock(CoachAssignmentRepository.AssignmentCandidateView.class);
		UUID memberId = UUID.randomUUID();
		Instant endsAt = NOW.plusSeconds(86400);
		when(view.getMemberAccountId()).thenReturn(memberId);
		when(view.getMemberCode()).thenReturn("MB-100001");
		when(view.getFullName()).thenReturn("Member One");
		when(view.getPhone()).thenReturn("0900000001");
		when(view.getEmail()).thenReturn("member@example.test");
		when(view.getMembershipEndsAt()).thenReturn(endsAt);
		when(view.getAssignmentStatus()).thenReturn("ASSIGNED");
		when(assignments.findAssignmentCandidates("member", "ASSIGNED", NOW, pageable))
			.thenReturn(new PageImpl<>(List.of(view), pageable, 3));

		AssignmentCandidatePageResponse result = service.candidates(
			managerId, "  member  ", AssignmentCandidateStatus.ASSIGNED, pageable);

		assertThat(result.page()).isEqualTo(1);
		assertThat(result.totalElements()).isEqualTo(3);
		assertThat(result.content()).singleElement().satisfies(candidate -> {
			assertThat(candidate.memberAccountId()).isEqualTo(memberId);
			assertThat(candidate.memberCode()).isEqualTo("MB-100001");
			assertThat(candidate.membershipEndsAt()).isEqualTo(endsAt);
			assertThat(candidate.assignmentStatus()).isEqualTo(AssignmentCandidateStatus.ASSIGNED);
		});
		InOrder order = inOrder(memberships, assignments);
		order.verify(memberships).expireDue(NOW);
		order.verify(assignments).endAllActiveAssignmentsWithExpiredPlus(NOW);
		order.verify(assignments).findAssignmentCandidates("member", "ASSIGNED", NOW, pageable);
	}

	@Test
	void workloadsReconcileBeforeGroupedProjectionAndReturnInformationalAvailability() {
		activeManager();
		var view = mock(CoachAssignmentRepository.CoachAssignmentWorkloadView.class);
		UUID coachId = UUID.randomUUID();
		when(view.getCoachAccountId()).thenReturn(coachId);
		when(view.getFullName()).thenReturn("Coach One");
		when(view.getPhone()).thenReturn("0900000002");
		when(view.getEmail()).thenReturn("coach@example.test");
		when(view.getActiveMemberCount()).thenReturn(7L);
		when(assignments.findAssignmentWorkloads()).thenReturn(List.of(view));

		List<CoachAssignmentWorkloadResponse> result = service.workloads(managerId);

		assertThat(result).singleElement().satisfies(workload -> {
			assertThat(workload.coachAccountId()).isEqualTo(coachId);
			assertThat(workload.activeMemberCount()).isEqualTo(7);
			assertThat(workload.maximumMemberCount()).isNull();
			assertThat(workload.availableForAssignment()).isTrue();
		});
		InOrder order = inOrder(memberships, assignments);
		order.verify(memberships).expireDue(NOW);
		order.verify(assignments).endAllActiveAssignmentsWithExpiredPlus(NOW);
		order.verify(assignments).findAssignmentWorkloads();
	}

	@Test
	void candidatesRejectOverlongQueryBeforeReconciliation() {
		activeManager();
		assertThatThrownBy(() -> service.candidates(managerId, "x".repeat(101),
			AssignmentCandidateStatus.ALL, PageRequest.of(0, 20)))
			.isInstanceOf(StaffAccountValidationException.class)
			.hasFieldOrPropertyWithValue("field", "query");
		verifyNoInteractions(memberships, assignments);
	}

	@Test
	void discoveryRejectsAnInactiveManager() {
		UUID inactiveManagerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(inactiveManagerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.workloads(inactiveManagerId))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
		verifyNoInteractions(memberships, assignments);
	}

	private void activeManager() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
	}
}
