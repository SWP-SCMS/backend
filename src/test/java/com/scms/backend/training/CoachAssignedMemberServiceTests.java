package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CoachAssignedMemberServiceTests {

	@Mock
	private CoachAssignmentRepository assignments;

	@Mock
	private AccountRepository accounts;

	@Mock
	private MemberProfileRepository profiles;

	@Mock
	private MemberProfile profile;

	private final Instant now = Instant.parse("2026-10-13T03:00:00Z");
	private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
	private final UUID coachId = UUID.randomUUID();
	private final UUID managerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final Pageable pageable = PageRequest.of(0, 20);

	private CoachAssignedMemberService service;

	@BeforeEach
	void setUp() {
		service = new CoachAssignedMemberService(assignments, accounts, profiles, clock);
	}

	@Test
	void listAssignedMembers_returnsOnlyCurrentCoachAssignmentsAfterExpiryReconciliation() {
		Account member = new Account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member One",
				"0901000001", "member@example.com", LocalDate.of(1995, 1, 1), "hash");
		CoachAssignment assignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(3600), now.minusSeconds(3600));

		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByCoachAccountIdAndStatusOrderByCreatedAtDesc(
				coachId, CoachAssignmentStatus.ACTIVE, pageable))
				.thenReturn(new PageImpl<>(List.of(assignment), pageable, 1));
		when(accounts.findAllById(List.of(memberId))).thenReturn(List.of(member));
		when(profiles.findAllById(List.of(memberId))).thenReturn(List.of(profile));
		when(profile.getAccountId()).thenReturn(memberId);
		when(profile.getMemberCode()).thenReturn("MBR-000001");
		when(profile.getProfileImageUrl()).thenReturn("https://cdn.example/member.png");
		when(profile.getFitnessGoal()).thenReturn("Build strength");

		CoachAssignedMemberPageResponse result = service.listAssignedMembers(coachId, pageable);

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content()).singleElement().satisfies(summary -> {
			assertThat(summary.memberAccountId()).isEqualTo(memberId);
			assertThat(summary.memberId()).isEqualTo("MBR-000001");
			assertThat(summary.fullName()).isEqualTo("Member One");
			assertThat(summary.assignmentId()).isEqualTo(assignment.getId());
			assertThat(summary.assignedSince()).isEqualTo(now.minusSeconds(3600));
		});

		InOrder lifecycleThenList = inOrder(assignments);
		lifecycleThenList.verify(assignments).endAllActiveAssignmentsWithExpiredPlus(now);
		lifecycleThenList.verify(assignments).findByCoachAccountIdAndStatusOrderByCreatedAtDesc(
				coachId, CoachAssignmentStatus.ACTIVE, pageable);
	}

	@Test
	void listAssignedMembers_returnsEmptyPageWhenCoachHasNoActiveAssignments() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByCoachAccountIdAndStatusOrderByCreatedAtDesc(
				coachId, CoachAssignmentStatus.ACTIVE, pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		CoachAssignedMemberPageResponse result = service.listAssignedMembers(coachId, pageable);

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		verify(accounts).findAllById(List.of());
		verify(profiles).findAllById(List.of());
	}

	@Test
	void listAssignedMembers_rejectsAccountThatIsNotAnActiveCoach() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(false);

		assertThatThrownBy(() -> service.listAssignedMembers(coachId, pageable))
				.isInstanceOf(InvalidAuthenticatedAccountException.class);

		verify(assignments, never()).endAllActiveAssignmentsWithExpiredPlus(now);
		verify(assignments, never()).findByCoachAccountIdAndStatusOrderByCreatedAtDesc(
				coachId, CoachAssignmentStatus.ACTIVE, pageable);
	}

	@Test
	void getMemberTrainingProfile_returnsProfileWithinActiveCoachAssignmentScope() {
		Account member = new Account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member One",
				"0901000001", "member@example.com", LocalDate.of(1995, 1, 1), "hash");
		CoachAssignment assignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(3600), now.minusSeconds(3600));

		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
		when(accounts.findById(memberId)).thenReturn(Optional.of(member));
		when(profiles.findById(memberId)).thenReturn(Optional.of(profile));
		when(profile.getMemberCode()).thenReturn("MBR-000001");
		when(profile.getProfileImageUrl()).thenReturn("https://cdn.example/member.png");
		when(profile.getFitnessGoal()).thenReturn("Build strength");
		when(profile.getEmergencyContactName()).thenReturn("Emergency Contact");
		when(profile.getEmergencyContactPhone()).thenReturn("0901999999");

		CoachMemberTrainingProfileResponse result = service.getMemberTrainingProfile(coachId, memberId);

		assertThat(result.memberAccountId()).isEqualTo(memberId);
		assertThat(result.memberId()).isEqualTo("MBR-000001");
		assertThat(result.fullName()).isEqualTo("Member One");
		assertThat(result.phone()).isEqualTo("0901000001");
		assertThat(result.email()).isEqualTo("member@example.com");
		assertThat(result.birthDate()).isEqualTo(LocalDate.of(1995, 1, 1));
		assertThat(result.fitnessGoal()).isEqualTo("Build strength");
		assertThat(result.emergencyContactName()).isEqualTo("Emergency Contact");
		assertThat(result.emergencyContactPhone()).isEqualTo("0901999999");
		assertThat(result.assignmentId()).isEqualTo(assignment.getId());
		assertThat(result.assignedSince()).isEqualTo(now.minusSeconds(3600));

		InOrder lifecycleThenOwnershipCheck = inOrder(assignments);
		lifecycleThenOwnershipCheck.verify(assignments).endActiveAssignmentIfPlusExpired(memberId, now);
		lifecycleThenOwnershipCheck.verify(assignments).findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE);
	}

	@Test
	void getMemberTrainingProfile_rejectsCoachOutsideActiveAssignmentScope() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getMemberTrainingProfile(coachId, memberId))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

		verify(accounts, never()).findById(memberId);
		verify(profiles, never()).findById(memberId);
	}

	@Test
	void getMemberTrainingProfile_rejectsAccountThatIsNotAnActiveCoachBeforeReadingMemberData() {
		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(false);

		assertThatThrownBy(() -> service.getMemberTrainingProfile(coachId, memberId))
				.isInstanceOf(InvalidAuthenticatedAccountException.class);

		verify(assignments, never()).endActiveAssignmentIfPlusExpired(memberId, now);
		verify(assignments, never()).findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE);
	}

	@Test
	void getMemberTrainingProfile_returnsNotFoundWhenMemberProfileIsMissing() {
		Account member = new Account(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Member One",
				"0901000001", "member@example.com", LocalDate.of(1995, 1, 1), "hash");
		CoachAssignment assignment = new CoachAssignment(UUID.randomUUID(), memberId, coachId, managerId,
				CoachAssignmentStatus.ACTIVE, now.minusSeconds(3600), now.minusSeconds(3600));

		when(accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE))
				.thenReturn(true);
		when(assignments.findByMemberAccountIdAndCoachAccountIdAndStatus(
				memberId, coachId, CoachAssignmentStatus.ACTIVE)).thenReturn(Optional.of(assignment));
		when(accounts.findById(memberId)).thenReturn(Optional.of(member));
		when(profiles.findById(memberId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getMemberTrainingProfile(coachId, memberId))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}
}
