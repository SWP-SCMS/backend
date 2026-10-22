package com.scms.backend.identity.memberprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberProfileServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private MemberProfileRepository memberProfileRepository;

	@Mock
	private AccountIdentifierAvailability identifierAvailability;

	private MemberProfileService memberProfileService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
		memberProfileService = new MemberProfileService(accountRepository, memberProfileRepository,
			identifierAvailability, new MemberProfileDetailsValidator(clock));
	}

	@Test
	void directEmailChangeRequiresVerifiedFlow() {
		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, "Race Member",
			"0901234567", "old@example.com", LocalDate.of(2000, 1, 15), "bcrypt-hash");
		MemberProfile profile = new MemberProfile(account);
		MemberProfilePatchRequest request = new MemberProfilePatchRequest();
		request.setEmail("new@example.com");
		when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
		when(memberProfileRepository.findById(accountId)).thenReturn(Optional.of(profile));
		assertThatThrownBy(() -> memberProfileService.updateProfile(accountId, request))
			.isInstanceOfSatisfying(com.scms.backend.emailverification.EmailVerificationException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("EMAIL_CHANGE_VERIFICATION_REQUIRED"));
	}
}
