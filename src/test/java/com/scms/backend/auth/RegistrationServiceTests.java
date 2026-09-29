package com.scms.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTests {

	@Mock
	private AccountRepository accountRepository;

	@Mock
	private MemberProfileRepository memberProfileRepository;

	@Mock
	private AuditEventRepository auditEventRepository;

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AccountIdentifierAvailability identifierAvailability;

	private RegistrationService registrationService;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
		registrationService = new RegistrationService(accountRepository, memberProfileRepository,
			auditEventRepository, notificationRepository, passwordEncoder, clock, identifierAvailability);
	}

	@Test
	void databaseEmailConstraintRaceIsTranslatedToConflictAndStopsRemainingWrites() {
		RegistrationRequest request = new RegistrationRequest("Race Member", "0901234567", "race@example.com",
			"Password123", LocalDate.of(2000, 1, 15), null, null);
		when(passwordEncoder.encode(request.password())).thenReturn("bcrypt-hash");
		DataIntegrityViolationException databaseFailure = new DataIntegrityViolationException(
			"duplicate key value violates unique constraint uq_accounts_current_email");
		when(accountRepository.saveAndFlush(any())).thenThrow(databaseFailure);
		when(identifierAvailability.fromDatabase(databaseFailure)).thenReturn(DuplicateAccountException.email());

		assertThatThrownBy(() -> registrationService.register(request))
			.isInstanceOfSatisfying(DuplicateAccountException.class,
				exception -> org.assertj.core.api.Assertions.assertThat(exception.getCode())
					.isEqualTo("EMAIL_ALREADY_EXISTS"));

		verify(memberProfileRepository, never()).saveAndFlush(any());
		verify(auditEventRepository, never()).save(any());
		verify(notificationRepository, never()).save(any());
	}

	@Test
	void registrationRequestStringRepresentationRedactsPassword() {
		RegistrationRequest request = new RegistrationRequest("Member", "0901234567", "member@example.com",
			"secret-password", LocalDate.of(2000, 1, 15), null, null);

		assertThat(request.toString()).contains("password=[REDACTED]").doesNotContain("secret-password");
	}
}
