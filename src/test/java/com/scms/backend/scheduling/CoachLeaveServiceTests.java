package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoachLeaveServiceTests {
	@Mock CoachLeaveRepository leaves;
	@Mock AccountRepository accounts;
	@Mock ClassSessionRepository sessions;
	@Mock AuditEventRepository audits;
	@Mock NotificationWriter notifications;
	@Mock CoachLeaveLifecycleService lifecycle;
	CoachLeaveService service;
	Instant now = Instant.parse("2026-10-22T01:00:00Z");

	@BeforeEach void setUp() {
		service = new CoachLeaveService(leaves, accounts, sessions, audits, notifications, lifecycle,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test void coachSubmitsNonOverlappingFutureLeave() {
		Account coach = account(AccountRole.COACH); Account manager = account(AccountRole.MANAGER);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		when(accounts.findByRoleAndStatusOrderById(AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(List.of(manager));
		when(leaves.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(sessions.findActiveOverlapIdsByCoach(any(), any(), any())).thenReturn(List.of());
		CoachLeaveResponse response = service.create(coach.getId(), new CoachLeaveCreateRequest(
			now.plusSeconds(3600), now.plusSeconds(7200), "Medical appointment"));
		assertThat(response.status()).isEqualTo(CoachLeaveStatus.PENDING);
		verify(audits).save(any());
		verify(notifications).write(any(), eq(manager.getId()), eq("COACH_LEAVE_REQUESTED"),
			eq("COACH_LEAVE_REQUEST"), eq(response.id()), any());
	}

	@Test void withdrawingPendingLeaveNotifiesActiveManagers() {
		Account coach = account(AccountRole.COACH); Account manager = account(AccountRole.MANAGER);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));
		when(accounts.findByRoleAndStatusOrderById(AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(List.of(manager));

		service.withdraw(coach.getId(), leave.getId());

		verify(notifications).write(any(), eq(manager.getId()), eq("COACH_LEAVE_WITHDRAWN"),
			eq("COACH_LEAVE_REQUEST"), eq(leave.getId()), any());
	}

	@Test void coachCancelsFutureApprovedLeaveAndNotifiesApprovingManager() {
		Account coach = account(AccountRole.COACH); Account manager = account(AccountRole.MANAGER);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		leave.approve(manager.getId(), "Approved", now.minusSeconds(60));
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));
		when(sessions.findActiveOverlapIdsByCoach(coach.getId(), leave.getStartTime(), leave.getEndTime()))
			.thenReturn(List.of());

		CoachLeaveResponse response = service.cancel(coach.getId(), leave.getId());

		assertThat(response.status()).isEqualTo(CoachLeaveStatus.CANCELLED);
		assertThat(response.reviewedBy()).isEqualTo(manager.getId());
		verify(audits).save(any());
		verify(notifications).write(any(), eq(manager.getId()), eq("COACH_LEAVE_CANCELLED"),
			eq("COACH_LEAVE_REQUEST"), eq(leave.getId()), any());
	}

	@Test void coachCannotCancelApprovedLeaveAfterItStarts() {
		Account coach = account(AccountRole.COACH); Account manager = account(AccountRole.MANAGER);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.minusSeconds(60),
			now.plusSeconds(3600), "Leave");
		leave.approve(manager.getId(), "Approved", now.minusSeconds(120));
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));

		assertThatThrownBy(() -> service.cancel(coach.getId(), leave.getId()))
			.isInstanceOfSatisfying(CoachLeaveException.class, error ->
				assertThat(error.getCode()).isEqualTo("COACH_LEAVE_ALREADY_STARTED"));
		assertThat(leave.getStatus()).isEqualTo(CoachLeaveStatus.APPROVED);
	}

	@Test void coachCannotCancelLeaveThatIsNotApproved() {
		Account coach = account(AccountRole.COACH);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));

		assertThatThrownBy(() -> service.cancel(coach.getId(), leave.getId()))
			.isInstanceOfSatisfying(CoachLeaveException.class, error ->
				assertThat(error.getCode()).isEqualTo("COACH_LEAVE_NOT_APPROVED"));
	}

	@Test void coachCannotCancelAnotherCoachsApprovedLeave() {
		Account coach = account(AccountRole.COACH); Account owner = account(AccountRole.COACH);
		Account manager = account(AccountRole.MANAGER);
		when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), owner, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		leave.approve(manager.getId(), "Approved", now.minusSeconds(60));
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));

		assertThatThrownBy(() -> service.cancel(coach.getId(), leave.getId()))
			.isInstanceOfSatisfying(CoachLeaveException.class, error ->
				assertThat(error.getCode()).isEqualTo("COACH_LEAVE_NOT_FOUND"));
	}

	@Test void overlappingOpenLeaveIsRejected() {
		Account coach = account(AccountRole.COACH); when(accounts.findById(coach.getId())).thenReturn(Optional.of(coach));
		when(leaves.existsOpenOverlap(any(), any(), any())).thenReturn(true);
		assertThatThrownBy(() -> service.create(coach.getId(), new CoachLeaveCreateRequest(
			now.plusSeconds(3600), now.plusSeconds(7200), "Leave")))
			.isInstanceOfSatisfying(CoachLeaveException.class,
				error -> assertThat(error.getCode()).isEqualTo("COACH_LEAVE_OVERLAP"));
	}

	@Test void managerCannotApproveWhileAffectedSessionsRemain() {
		Account manager = account(AccountRole.MANAGER); Account coach = account(AccountRole.COACH);
		when(accounts.findById(manager.getId())).thenReturn(Optional.of(manager));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));
		when(accounts.findByIdForUpdate(coach.getId())).thenReturn(Optional.of(coach));
		UUID affected = UUID.randomUUID();
		when(sessions.findActiveOverlapIdsByCoach(coach.getId(), leave.getStartTime(), leave.getEndTime()))
			.thenReturn(List.of(affected));
		assertThatThrownBy(() -> service.approve(manager.getId(), leave.getId(),
			new CoachLeaveReviewRequest("Approved")))
			.isInstanceOfSatisfying(CoachLeaveException.class, error -> {
				assertThat(error.getCode()).isEqualTo("COACH_LEAVE_HAS_AFFECTED_SESSIONS");
				assertThat(error.getAffectedSessionIds()).containsExactly(affected);
			});
		assertThat(leave.getStatus()).isEqualTo(CoachLeaveStatus.PENDING);
	}

	@Test void managerApprovesAfterAffectedSessionsAreResolved() {
		Account manager = account(AccountRole.MANAGER); Account coach = account(AccountRole.COACH);
		when(accounts.findById(manager.getId())).thenReturn(Optional.of(manager));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));
		when(accounts.findByIdForUpdate(coach.getId())).thenReturn(Optional.of(coach));
		when(sessions.findActiveOverlapIdsByCoach(coach.getId(), leave.getStartTime(), leave.getEndTime()))
			.thenReturn(List.of());
		CoachLeaveResponse response = service.approve(manager.getId(), leave.getId(),
			new CoachLeaveReviewRequest("Approved"));
		assertThat(response.status()).isEqualTo(CoachLeaveStatus.APPROVED);
		verify(notifications).write(any(), any(), any(), any(), any(), any());
	}

	@Test void managerCannotApproveAfterLeaveHasStarted() {
		Account manager = account(AccountRole.MANAGER); Account coach = account(AccountRole.COACH);
		when(accounts.findById(manager.getId())).thenReturn(Optional.of(manager));
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.minusSeconds(60),
			now.plusSeconds(3600), "Leave");
		when(lifecycle.expireIfStarted(leave.getId(), now)).thenAnswer(invocation -> {
			leave.expire(now);
			return true;
		});
		when(leaves.findByIdForUpdate(leave.getId())).thenReturn(Optional.of(leave));

		assertThatThrownBy(() -> service.approve(manager.getId(), leave.getId(), null))
			.isInstanceOfSatisfying(CoachLeaveException.class, error ->
				assertThat(error.getCode()).isEqualTo("COACH_LEAVE_ALREADY_STARTED"));
		assertThat(leave.getStatus()).isEqualTo(CoachLeaveStatus.EXPIRED);
		verify(notifications, org.mockito.Mockito.never()).write(any(), any(),
			eq("COACH_LEAVE_APPROVED"), any(), any(), any());
	}

	@Test void listingRequestsCatchesUpExpiredLeaveLifecycleFirst() {
		Account manager = account(AccountRole.MANAGER);
		when(accounts.findById(manager.getId())).thenReturn(Optional.of(manager));
		when(leaves.findAll()).thenReturn(List.of());

		service.managerList(manager.getId(), null);

		verify(lifecycle).catchUp(now);
	}

	@Test void managerListIncludesCoachIdentity() {
		Account manager = account(AccountRole.MANAGER);
		Account coach = account(AccountRole.COACH);
		CoachLeaveRequest leave = new CoachLeaveRequest(UUID.randomUUID(), coach, now.plusSeconds(3600),
			now.plusSeconds(7200), "Leave");
		when(accounts.findById(manager.getId())).thenReturn(Optional.of(manager));
		when(leaves.findAll()).thenReturn(List.of(leave));
		when(sessions.findActiveOverlapIdsByCoach(coach.getId(), leave.getStartTime(), leave.getEndTime()))
			.thenReturn(List.of());

		List<CoachLeaveResponse> result = service.managerList(manager.getId(), null);

		assertThat(result).singleElement().satisfies(response -> {
			assertThat(response.coachId()).isEqualTo(coach.getId());
			assertThat(response.coachFullName()).isEqualTo(coach.getFullName());
			assertThat(response.coachPhone()).isEqualTo(coach.getPhone());
		});
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return new Account(id, role, AccountStatus.ACTIVE, role.name(), "090" + id.toString().substring(0, 8),
			id + "@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
	}
}
