package com.scms.backend.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class MemberStatusServiceTests {

	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	@Mock NotificationWriter notifications;
	@Mock JdbcTemplate jdbc;
	private MemberStatusService service;
	private final UUID managerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		service = new MemberStatusService(accounts, audits, notifications, jdbc,
			Clock.fixed(Instant.parse("2026-10-01T04:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void suspendCancelsFutureBookingsAndWritesAuditAndNotification() {
		Account member = member(AccountStatus.ACTIVE);
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findById(memberId)).thenReturn(Optional.of(member));
		when(jdbc.update(anyString(), any(), any(), any(), any(), any())).thenReturn(2);

		MemberStatusChangeResponse response = service.changeStatus(managerId, memberId,
			new MemberStatusChangeRequest(AccountStatus.SUSPENDED, "Policy violation"));

		assertThat(response.status()).isEqualTo(AccountStatus.SUSPENDED);
		assertThat(response.cancelledFutureBookings()).isEqualTo(2);
		assertThat(member.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
		ArgumentCaptor<String> cancellationSql = ArgumentCaptor.forClass(String.class);
		verify(jdbc).update(cancellationSql.capture(), any(), any(), any(), any(), any());
		assertThat(cancellationSql.getValue()).contains("version = b.version + 1");
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getReason()).isEqualTo("Policy violation");
		assertThat(audit.getValue().getBeforeData()).containsEntry("status", "ACTIVE");
		verify(notifications).write(anyString(), eq(memberId), eq("MEMBER_SUSPENDED"), eq("ACCOUNT"), eq(memberId),
			any());
	}

	@Test
	void reactivateDoesNotChangeBookings() {
		Account member = member(AccountStatus.SUSPENDED);
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findById(memberId)).thenReturn(Optional.of(member));

		MemberStatusChangeResponse response = service.changeStatus(managerId, memberId,
			new MemberStatusChangeRequest(AccountStatus.ACTIVE, "Appeal accepted"));

		assertThat(response.cancelledFutureBookings()).isZero();
		assertThat(member.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		verify(jdbc, never()).update(anyString(), any(), any(), any(), any(), any());
	}

	@Test
	void rejectsNonManagerWrongTargetAndDuplicateTransition() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(false);
		assertThatThrownBy(() -> service.changeStatus(managerId, memberId,
			new MemberStatusChangeRequest(AccountStatus.SUSPENDED, "reason")))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);

		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findById(memberId)).thenReturn(Optional.of(member(AccountStatus.SUSPENDED)));
		assertThatThrownBy(() -> service.changeStatus(managerId, memberId,
			new MemberStatusChangeRequest(AccountStatus.SUSPENDED, "reason")))
			.isInstanceOf(MemberStatusConflictException.class);
	}

	private Account member(AccountStatus status) {
		return new Account(memberId, AccountRole.MEMBER, status, "Member", "0900000000", "member@test.local",
			LocalDate.of(2000, 1, 1), "hash");
	}
}
