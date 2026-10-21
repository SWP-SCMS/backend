package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ActiveAccountJwtValidatorTests {
	@Test
	void rejectsAccessTokenIssuedBeforeAnAuthenticationVersionChange() {
		AccountRepository accounts=mock(AccountRepository.class);UUID id=UUID.randomUUID();
		ActiveAccountJwtValidator validator=new ActiveAccountJwtValidator(accounts);
		Jwt jwt=Jwt.withTokenValue("token").header("alg","none").subject(id.toString())
			.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claim("authVersion",0L).build();
		when(accounts.existsByIdAndStatusAndAuthVersion(id,AccountStatus.ACTIVE,0L)).thenReturn(false);

		assertThat(validator.validate(jwt).hasErrors()).isTrue();
		verify(accounts).existsByIdAndStatusAndAuthVersion(id,AccountStatus.ACTIVE,0L);
	}

	@Test
	void acceptsCurrentAuthenticationVersion() {
		AccountRepository accounts=mock(AccountRepository.class);UUID id=UUID.randomUUID();
		ActiveAccountJwtValidator validator=new ActiveAccountJwtValidator(accounts);
		Jwt jwt=Jwt.withTokenValue("token").header("alg","none").subject(id.toString())
			.issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claim("authVersion",2L).build();
		when(accounts.existsByIdAndStatusAndAuthVersion(id,AccountStatus.ACTIVE,2L)).thenReturn(true);

		assertThat(validator.validate(jwt).hasErrors()).isFalse();
	}
}
