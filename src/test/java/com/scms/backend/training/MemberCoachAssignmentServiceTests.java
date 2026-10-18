package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberCoachAssignmentServiceTests {

	@Mock CoachAssignmentRepository assignments;
	@Mock AccountRepository accounts;

	private final UUID memberId = UUID.randomUUID();
	private final UUID coachId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-18T03:00:00Z");
	private MemberCoachAssignmentService service;

	@BeforeEach
	void setUp() {
		service = new MemberCoachAssignmentService(assignments, accounts, Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void returnsCurrentAssignmentAfterExpiryReconciliation() {
		CoachAssignment assignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, UUID.randomUUID(),
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(3600), now.minusSeconds(3600));
		Account coach = new Account(coachId, AccountRole.COACH, AccountStatus.ACTIVE, "Coach Alpha",
				"0911111111", "coach@example.com", LocalDate.of(1990, 1, 1), "hash");
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByMemberAccountIdAndStatus(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.of(assignment));
		when(accounts.findById(coachId)).thenReturn(Optional.of(coach));

		MemberCoachAssignmentResponse response = service.getActive(memberId).orElseThrow();

		assertThat(response.assignmentId()).isEqualTo(assignment.getId());
		assertThat(response.coachFullName()).isEqualTo("Coach Alpha");
		assertThat(response.startsAt()).isEqualTo(assignment.getStartsAt());
		verify(assignments).endActiveAssignmentIfPlusExpired(memberId, now);
	}

	@Test
	void returnsEmptyWhenNoActiveAssignmentRemains() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByMemberAccountIdAndStatus(memberId, CoachAssignmentStatus.ACTIVE))
				.thenReturn(Optional.empty());

		assertThat(service.getActive(memberId)).isEmpty();
		verify(assignments).endActiveAssignmentIfPlusExpired(memberId, now);
	}

	@Test
	void rejectsNonActiveMemberActor() {
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
				.thenReturn(false);

		assertThatThrownBy(() -> service.getActive(memberId))
				.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}
}
