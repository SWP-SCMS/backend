package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.notification.NotificationWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffAccountCreateServiceTests {

	@Mock AccountRepository accounts;
	@Mock AccountIdentifierAvailability identifiers;
	@Mock AuditEventRepository audits;
	@Mock NotificationWriter notifications;
	@Mock PasswordEncoder encoder;

	@Test
	void managerCreatesActiveStaffWithHashedPhonePasswordAndAudit() {
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(encoder.encode("0901234567")).thenReturn("bcrypt-hash");
		StaffAccountCreateService service = new StaffAccountCreateService(accounts, identifiers, audits, notifications,
			encoder, Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));

		StaffAccountResponse response = service.create(managerId,
			new StaffAccountCreateRequest(" Coach One ", "0901234567", "coach@example.com",
				LocalDate.of(1990, 1, 1), AccountRole.COACH));

		assertThat(response.role()).isEqualTo(AccountRole.COACH);
		assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
		verify(encoder).encode("0901234567");
		verify(accounts).saveAndFlush(any());
		verify(audits).save(any());
		verify(notifications).write(eq("ACCOUNT_CREATED:" + response.accountId()), eq(response.accountId()),
			eq("ACCOUNT_CREATED"), eq("ACCOUNT"), eq(response.accountId()), any());
	}
}
