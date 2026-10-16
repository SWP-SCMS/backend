package com.scms.backend.support;

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

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InitialManagerProvisionerTests {

	@Mock AccountRepository accounts;
	@Mock AccountIdentifierAvailability identifiers;
	@Mock AuditEventRepository audits;
	@Mock PasswordEncoder encoder;
	@Mock JdbcTemplate db;

	private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC);

	@Test
	void disabledProvisioningDoesNothing() {
		provisioner(properties(false)).run();

		verify(db, never()).execute(any(String.class));
		verify(accounts, never()).saveAndFlush(any());
	}

	@Test
	void existingManagerMakesProvisioningIdempotent() {
		when(accounts.existsByRole(AccountRole.MANAGER)).thenReturn(true);

		provisioner(properties(true)).run();

		verify(db).execute(any(String.class));
		verify(accounts, never()).saveAndFlush(any());
		verify(audits, never()).save(any());
	}

	@Test
	void provisionsOneActiveManagerWithHashedPasswordAndSafeAudit() {
		when(accounts.existsByRole(AccountRole.MANAGER)).thenReturn(false);
		when(encoder.encode("a-strong-bootstrap-password")).thenReturn("bcrypt-hash");

		provisioner(properties(true)).run();

		ArgumentCaptor<Account> account = ArgumentCaptor.forClass(Account.class);
		verify(accounts).saveAndFlush(account.capture());
		assertThat(account.getValue().getRole()).isEqualTo(AccountRole.MANAGER);
		assertThat(account.getValue().getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(account.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
		verify(encoder).encode("a-strong-bootstrap-password");

		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getActorAccountId()).isNull();
		assertThat(audit.getValue().getAction()).isEqualTo("INITIAL_MANAGER_PROVISIONED");
		assertThat(audit.getValue().getAfterData())
			.containsEntry("role", "MANAGER")
			.containsEntry("status", "ACTIVE")
			.doesNotContainKeys("password", "passwordHash", "credential");
		assertThat(audit.getValue().getAfterData().values())
			.doesNotContain("a-strong-bootstrap-password", "bcrypt-hash");
	}

	@Test
	void enabledProvisioningRejectsMissingOrWeakCredentialsBeforeDatabaseAccess() {
		InitialManagerProperties invalid = new InitialManagerProperties(true, "Manager", "0901234567",
			"manager@example.com", LocalDate.of(1990, 1, 1), "short");

		assertThatThrownBy(() -> provisioner(invalid).run())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("INITIAL_MANAGER_PASSWORD");
		verify(db, never()).execute(any(String.class));
	}

	@Test
	void enabledProvisioningRejectsPasswordOver72Utf8BytesBeforeDatabaseAccess() {
		InitialManagerProperties invalid = new InitialManagerProperties(true, "Manager", "0901234567",
			"manager@example.com", LocalDate.of(1990, 1, 1), "🔒".repeat(19));

		assertThatThrownBy(() -> provisioner(invalid).run())
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("INITIAL_MANAGER_PASSWORD");
		verify(db, never()).execute(any(String.class));
	}

	private InitialManagerProvisioner provisioner(InitialManagerProperties properties) {
		return new InitialManagerProvisioner(properties, accounts, identifiers, audits, encoder, db, clock);
	}

	private InitialManagerProperties properties(boolean enabled) {
		return new InitialManagerProperties(enabled, " Initial Manager ", "0901234567", "MANAGER@EXAMPLE.COM",
			LocalDate.of(1990, 1, 1), "a-strong-bootstrap-password");
	}
}
