package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private JwtTokenService jwtTokenService;

	@Mock
	private RefreshTokenService refreshTokenService;

	private AuthenticationService authenticationService;

	@BeforeEach
	void setUp() {
		when(passwordEncoder.encode("dummy-password-never-used")).thenReturn("dummy-hash");
		authenticationService = new AuthenticationService(accountRepository, passwordEncoder, jwtTokenService,
			refreshTokenService);
	}

	@Test
	void unknownIdentifierStillPerformsOnePasswordComparisonAgainstDummyHash() {
		when(accountRepository.findByEmailIgnoreCaseAndStatus("missing@example.test", AccountStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> authenticationService.login(
			new LoginRequest(" Missing@Example.Test ", "candidate-password")))
			.isInstanceOf(InvalidCredentialsException.class)
			.hasMessage("Invalid credentials");

		verify(passwordEncoder).matches("candidate-password", "dummy-hash");
	}

	@Test
	void overlongUtf8PasswordIsRejectedBeforeBcrypt() {
		assertThatThrownBy(() -> authenticationService.login(
			new LoginRequest("member@example.test", "á".repeat(37))))
			.isInstanceOf(InvalidCredentialsException.class);

		verify(passwordEncoder, never()).matches(anyString(), anyString());
	}

	@Test
	void overlongUtf8CurrentPasswordReturnsValidationErrorBeforeBcrypt() {
		assertThatThrownBy(() -> authenticationService.changePassword(UUID.randomUUID(),
			new ChangePasswordRequest("á".repeat(37), "valid-new-password"), null))
			.isInstanceOf(ChangePasswordValidationException.class)
			.hasMessage("must be at most 72 UTF-8 bytes")
			.extracting("field").isEqualTo("currentPassword");

		verify(accountRepository, never()).findByIdForUpdate(any());
		verify(passwordEncoder, never()).matches(anyString(), anyString());
	}
}
