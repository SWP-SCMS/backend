package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class StaffStatusServiceTests {

	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock JdbcTemplate jdbc;

	@Test
	void deactivatesCoachOnlyAfterOutstandingWorkIsHandled() {
		UUID managerId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		Account coach = account(coachId, AccountRole.COACH);
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findById(coachId)).thenReturn(java.util.Optional.of(coach));
		when(jdbc.queryForObject(any(String.class), eq(Boolean.class), any(), any())).thenReturn(false);
		when(jdbc.queryForObject(any(String.class), eq(Boolean.class), any())).thenReturn(false);

		StaffAccountResponse result = service().deactivate(managerId, coachId,
			new StaffStatusChangeRequest(AccountStatus.INACTIVE, "Left company"));

		assertThat(result.status()).isEqualTo(AccountStatus.INACTIVE);
		verify(audits).save(any());
	}

	@Test
	void protectsLastActiveManager() {
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		Account target = account(targetId, AccountRole.MANAGER);
		when(accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findById(targetId)).thenReturn(java.util.Optional.of(target));
		when(accounts.findActiveManagersForUpdate()).thenReturn(List.of(target));

		assertThatThrownBy(() -> service().deactivate(actorId, targetId,
			new StaffStatusChangeRequest(AccountStatus.INACTIVE, "Left company")))
			.isInstanceOf(StaffStatusConflictException.class)
			.hasMessageContaining("last active Manager");
	}

	private StaffStatusService service() {
		return new StaffStatusService(accounts, audits, jdbc,
			Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC));
	}

	private Account account(UUID id, AccountRole role) {
		return new Account(id, role, AccountStatus.ACTIVE, "Test", "0901234567", id + "@example.com",
			LocalDate.of(1990, 1, 1), "hash");
	}
}
