package com.scms.backend.auth;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationService {

	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenService jwtTokenService;
	private final RefreshTokenService refreshTokenService;
	private final String dummyPasswordHash;

	AuthenticationService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
			JwtTokenService jwtTokenService, RefreshTokenService refreshTokenService) {
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenService = jwtTokenService;
		this.refreshTokenService = refreshTokenService;
		this.dummyPasswordHash = passwordEncoder.encode("dummy-password-never-used");
	}

	@Transactional
	AuthSession login(LoginRequest request) {
		Optional<Account> candidate = findActiveAccount(request.identifier());
		String passwordHash = candidate.map(Account::getPasswordHash).orElse(dummyPasswordHash);
		boolean passwordMatches = passwordEncoder.matches(request.password(), passwordHash);
		if (candidate.isEmpty() || !passwordMatches) {
			throw new InvalidCredentialsException();
		}
		Account account = candidate.orElseThrow();
		return issueSession(account, refreshTokenService.issue(account).value());
	}

	@Transactional
	AuthSession refresh(String rawRefreshToken) {
		RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
		return issueSession(rotated.account(), rotated.value());
	}

	@Transactional
	void logout(String rawRefreshToken) {
		refreshTokenService.revokeIfPresent(rawRefreshToken);
	}

	@Transactional
	void changePassword(UUID accountId, ChangePasswordRequest request, String rawRefreshToken) {
		request.validate();
		Account account = accountRepository.findById(accountId)
			.filter(candidate -> candidate.getStatus() == AccountStatus.ACTIVE)
			.orElseThrow(InvalidAuthenticatedAccountException::new);
		String currentPasswordHash = account.getPasswordHash();
		if (!passwordEncoder.matches(request.currentPassword(), currentPasswordHash)) {
			throw new CurrentPasswordIncorrectException();
		}
		if (passwordEncoder.matches(request.newPassword(), currentPasswordHash)) {
			throw new NewPasswordSameAsCurrentException();
		}

		account.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshTokenService.revokeIfPresent(rawRefreshToken, accountId);
	}

	private AuthSession issueSession(Account account, String refreshToken) {
		JwtTokenService.AccessToken accessToken = jwtTokenService.issue(account);
		AuthResponse response = new AuthResponse(account.getId(), account.getFullName(), account.getRole(),
			accessToken.value(), "Bearer", accessToken.expiresInSeconds());
		return new AuthSession(response, refreshToken);
	}

	private Optional<Account> findActiveAccount(String identifier) {
		if (identifier.contains("@")) {
			return accountRepository.findByEmailIgnoreCaseAndStatus(identifier.toLowerCase(Locale.ROOT),
				AccountStatus.ACTIVE);
		}
		return accountRepository.findByPhoneAndStatus(identifier, AccountStatus.ACTIVE);
	}
}
