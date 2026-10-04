package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.member.MemberProfileDetailsValidator;
import com.scms.backend.member.MemberProfilePatchRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ManagerMemberServiceTests {

	@Mock AccountRepository accounts;
	@Mock MemberProfileRepository profiles;
	@Mock AccountIdentifierAvailability identifiers;
	private ManagerMemberService service;
	private final UUID managerId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new ManagerMemberService(accounts, profiles, identifiers,
			new MemberProfileDetailsValidator(Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC)));
	}

	@Test
	void activeManagerCanListMembersWithStablePageMetadata() {
		MemberProfile member = member(AccountStatus.ACTIVE);
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(profiles.findAll(any(Specification.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(member), PageRequest.of(0, 20), 1));

		MemberAccountPageResponse result = service.search(managerId, " member ", AccountStatus.ACTIVE,
			PageRequest.of(0, 20));

		assertThat(result.totalElements()).isEqualTo(1);
		assertThat(result.content()).singleElement().satisfies(response -> {
			assertThat(response.accountId()).isEqualTo(member.getAccountId());
			assertThat(response.role()).isEqualTo(AccountRole.MEMBER);
			assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
		});
	}

	@Test
	void inactiveManagerCannotReadMembers() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(false);

		assertThatThrownBy(() -> service.get(managerId, UUID.randomUUID()))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	@Test
	void memberDetailRejectsNonMemberTarget() {
		UUID targetId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(profiles.findById(targetId)).thenReturn(Optional.of(profile(AccountRole.COACH, AccountStatus.ACTIVE)));

		assertThatThrownBy(() -> service.get(managerId, targetId))
			.isInstanceOf(MemberAccountNotFoundException.class);
	}

	@Test
	void patchUpdatesAllowedProfileFieldsAndPreservesIdentityAndStatus() {
		MemberProfile member = member(AccountStatus.SUSPENDED);
		UUID accountId = member.getAccountId();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(profiles.findById(accountId)).thenReturn(Optional.of(member));
		MemberProfilePatchRequest request = new MemberProfilePatchRequest();
		request.setFullName(" Updated Member ");
		request.setEmail(" UPDATED@EXAMPLE.COM ");
		request.setFitnessGoal(" Improve mobility ");

		MemberAccountResponse result = service.update(managerId, accountId, request);

		assertThat(result.fullName()).isEqualTo("Updated Member");
		assertThat(result.email()).isEqualTo("updated@example.com");
		assertThat(result.fitnessGoal()).isEqualTo("Improve mobility");
		assertThat(result.accountId()).isEqualTo(accountId);
		assertThat(result.role()).isEqualTo(AccountRole.MEMBER);
		assertThat(result.status()).isEqualTo(AccountStatus.SUSPENDED);
		verify(accounts).flush();
		verify(profiles).flush();
	}

	private MemberProfile member(AccountStatus status) {
		return profile(AccountRole.MEMBER, status);
	}

	private MemberProfile profile(AccountRole role, AccountStatus status) {
		Account account = new Account(UUID.randomUUID(), role, status, "Member One", "0900000000",
			"member@example.com", LocalDate.of(2000, 1, 1), "hash");
		return new MemberProfile(account, "https://cdn.example/member.png", "Build strength");
	}
}
