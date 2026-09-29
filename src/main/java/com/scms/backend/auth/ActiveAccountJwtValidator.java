package com.scms.backend.auth;

import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.stereotype.Component;

@Component
public class ActiveAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_TOKEN = new OAuth2Error(BearerTokenErrorCodes.INVALID_TOKEN);

	private final AccountRepository accountRepository;

	ActiveAccountJwtValidator(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt jwt) {
		UUID accountId;
		try {
			accountId = UUID.fromString(jwt.getSubject());
		}
		catch (IllegalArgumentException | NullPointerException exception) {
			return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
		}

		if (!accountRepository.existsByIdAndStatus(accountId, AccountStatus.ACTIVE)) {
			return OAuth2TokenValidatorResult.failure(INVALID_TOKEN);
		}
		return OAuth2TokenValidatorResult.success();
	}
}
