package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class MembershipTerminationServiceTests {
	private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private final MembershipTerminationRequestRepository requests = mock(MembershipTerminationRequestRepository.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final NotificationWriter notifications = mock(NotificationWriter.class);
	private final MembershipTerminationExecutionService execution = mock(MembershipTerminationExecutionService.class);
	private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
	private final MembershipTerminationService service = new MembershipTerminationService(accounts, profiles,
		memberships, requests, audits, notifications, execution, clock);
	private final UUID memberId = UUID.randomUUID();
	private final UUID membershipId = UUID.randomUUID();

	@BeforeEach
	void activeMemberIsAuthorized() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
	}

	@Test
	void duplicateOpenRequestIsRejectedBeforeCreatingAnotherRequest() {
		Account member = new Account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member", "0900000001",
			"member@example.test", java.time.LocalDate.of(1990, 1, 1), "hash");
		Membership membership = new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(), "PLUS",
			"Plus", BigInteger.ONE, "VND", 30, MembershipStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(60));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE))
			.thenReturn(Optional.of(membership));
		when(requests.existsByMembershipIdAndStatusIn(eq(membershipId), any())).thenReturn(true);

		assertThatThrownBy(() -> service.createForMember(memberId,
			new MembershipTerminationCreateRequest(MembershipTerminationMode.IMMEDIATE, "reason")))
			.isInstanceOf(MembershipTerminationException.class)
			.extracting("code").isEqualTo("MEMBERSHIP_TERMINATION_ALREADY_PENDING");
		verify(requests, never()).saveAndFlush(any());
	}

	@Test
	void memberTerminationNotifiesActiveManagersButNotTheMemberActor() {
		Account member = new Account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member", "0900000001",
			"member@example.test", java.time.LocalDate.of(1990, 1, 1), "hash");
		Membership membership = new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(), "PLUS",
			"Plus", BigInteger.ONE, "VND", 30, MembershipStatus.ACTIVE, NOW.minusSeconds(60), NOW.plusSeconds(60));
		UUID managerId = UUID.randomUUID();
		Account manager = mock(Account.class);
		when(manager.getId()).thenReturn(managerId);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(member));
		when(memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)).thenReturn(Optional.of(membership));
		when(requests.existsByMembershipIdAndStatusIn(eq(membershipId), any())).thenReturn(false);
		when(accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(List.of(manager));

		service.createForMember(memberId, new MembershipTerminationCreateRequest(MembershipTerminationMode.END_OF_CYCLE, "reason"));

		verify(notifications, never()).write(any(), eq(memberId), eq("MEMBERSHIP_TERMINATION_REQUESTED"), any(), any(), any());
		verify(notifications).write(any(), eq(managerId), eq("MEMBERSHIP_TERMINATION_REVIEW_REQUIRED"),
			eq("MEMBERSHIP_TERMINATION_REQUEST"), any(), any());
	}

	@Test
	void staleManagerDecisionIsRejectedWithoutTouchingMembership() {
		UUID managerId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		MembershipTerminationRequest request = mock(MembershipTerminationRequest.class);
		when(request.getVersion()).thenReturn(4L);
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

		assertThatThrownBy(() -> service.approve(managerId, requestId, new MembershipTerminationDecisionRequest(3L)))
			.isInstanceOf(MembershipTerminationException.class)
			.extracting("code").isEqualTo("MEMBERSHIP_TERMINATION_CHANGED");
		verify(memberships, never()).findByIdForUpdate(any());
	}

	@Test
	void entityAllowsOnlyPendingToApprovalAndWithdrawal() {
		MembershipTerminationRequest request = new MembershipTerminationRequest(UUID.randomUUID(), membershipId, memberId,
			memberId, MembershipTerminationMode.END_OF_CYCLE, "reason");
		request.approve(UUID.randomUUID(), NOW.plusSeconds(60), NOW);
		assertThat(request.getStatus()).isEqualTo(MembershipTerminationRequestStatus.APPROVED);
		assertThatThrownBy(() -> request.withdraw(NOW)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void immediateApprovalDelegatesExecutionToTheLifecycleCoordinator() {
		UUID managerId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		MembershipTerminationRequest request = mock(MembershipTerminationRequest.class);
		Membership membership = mock(Membership.class);
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(request.getVersion()).thenReturn(2L);
		when(request.getStatus()).thenReturn(MembershipTerminationRequestStatus.PENDING);
		when(request.getMode()).thenReturn(MembershipTerminationMode.IMMEDIATE);
		when(request.getMemberAccountId()).thenReturn(memberId);
		when(request.getId()).thenReturn(requestId);
		when(request.getMembershipId()).thenReturn(membershipId);
		when(request.getReason()).thenReturn("reason");
		when(membership.getStatus()).thenReturn(MembershipStatus.ACTIVE);
		when(memberships.findByIdForUpdate(membershipId)).thenReturn(Optional.of(membership));

		service.approve(managerId, requestId, new MembershipTerminationDecisionRequest(2L));

		verify(execution).executeImmediate(requestId, managerId, NOW);
	}

	@Test
	void managerTerminationListIncludesCurrentMemberIdentityUsingBatchLookups() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		MembershipTerminationRequest request = new MembershipTerminationRequest(UUID.randomUUID(), membershipId,
			memberId, memberId, MembershipTerminationMode.IMMEDIATE, "reason");
		MembershipTerminationRequest anotherRequest = new MembershipTerminationRequest(UUID.randomUUID(), membershipId,
			memberId, memberId, MembershipTerminationMode.END_OF_CYCLE, "another reason");
		when(requests.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(request, anotherRequest), PageRequest.of(0, 20), 2));
		Account member = mock(Account.class);
		when(member.getId()).thenReturn(memberId);
		when(member.getFullName()).thenReturn("Member One");
		when(accounts.findAllById(List.of(memberId))).thenReturn(List.of(member));
		MemberProfile profile = mock(MemberProfile.class);
		when(profile.getAccountId()).thenReturn(memberId);
		when(profile.getMemberCode()).thenReturn("MB-100001");
		when(profiles.findAllById(List.of(memberId))).thenReturn(List.of(profile));

		List<MembershipTerminationRequestResponse> items = service.listForManager(managerId, 0, 20).content();

		assertThat(items).hasSize(2).allSatisfy(item -> {
			assertThat(item.memberId()).isEqualTo("MB-100001");
			assertThat(item.memberFullName()).isEqualTo("Member One");
		});
		verify(accounts).findAllById(List.of(memberId));
		verify(profiles).findAllById(List.of(memberId));
	}
}
