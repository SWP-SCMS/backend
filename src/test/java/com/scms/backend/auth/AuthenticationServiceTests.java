package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
}
