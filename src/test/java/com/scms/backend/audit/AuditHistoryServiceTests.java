package com.scms.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

class AuditHistoryServiceTests {

	@Test
	void prefersStoredActorSnapshotWithoutAccountLookup() {
		AuditEventRepository audits = mock(AuditEventRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(audits, accounts);
		UUID managerId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		AuditEvent event = new AuditEvent(UUID.randomUUID(), actorId, "ACCOUNT_UPDATED", "ACCOUNT",
			UUID.randomUUID(), Map.of());
		ReflectionTestUtils.setField(event, "actorNameSnapshot", "Original Actor");
		ReflectionTestUtils.setField(event, "actorRoleSnapshot", AccountRole.COACH);
		Pageable pageable = PageRequest.of(0, 20);
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(true);
		when(audits.findAll(any(Specification.class), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(event), pageable, 1));

		AuditHistoryResponse result = service.list(managerId, null, null, null, null, null, null, pageable)
			.content().getFirst();

		assertThat(result.actorName()).isEqualTo("Original Actor");
		assertThat(result.actorRole()).isEqualTo(AccountRole.COACH);
		verify(accounts, never()).findAllById(any());
	}

	@Test
	void bulkLoadsOnlyActorsWhoseSnapshotIsMissing() {
		AuditEventRepository audits = mock(AuditEventRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(audits, accounts);
		UUID managerId = UUID.randomUUID();
		UUID snapshottedActorId = UUID.randomUUID();
		UUID legacyActorId = UUID.randomUUID();
		AuditEvent snapshotted = new AuditEvent(UUID.randomUUID(), snapshottedActorId, "UPDATED", "ACCOUNT",
			UUID.randomUUID(), Map.of());
		ReflectionTestUtils.setField(snapshotted, "actorNameSnapshot", "Snapshot Actor");
		ReflectionTestUtils.setField(snapshotted, "actorRoleSnapshot", AccountRole.MANAGER);
		AuditEvent legacy = new AuditEvent(UUID.randomUUID(), legacyActorId, "UPDATED", "ACCOUNT",
			UUID.randomUUID(), Map.of());
		Account legacyActor = new Account(legacyActorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE,
			"Legacy Actor", "0901234568", "legacy@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
		Pageable pageable = PageRequest.of(0, 20);
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(true);
		when(audits.findAll(any(Specification.class), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(snapshotted, legacy), pageable, 2));
		when(accounts.findAllById(Set.of(legacyActorId))).thenReturn(List.of(legacyActor));

		AuditHistoryPageResponse result = service.list(managerId, null, null, null, null, null, null, pageable);

		assertThat(result.content()).extracting(AuditHistoryResponse::actorName)
			.containsExactly("Snapshot Actor", "Legacy Actor");
		verify(accounts).findAllById(Set.of(legacyActorId));
	}

	@Test
	void resolvesRepeatedActorWithOneBulkLookupAndPreservesPagination() {
		AuditEventRepository audits = mock(AuditEventRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(audits, accounts);
		UUID managerId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		AuditEvent first = new AuditEvent(UUID.randomUUID(), actorId, "PAYMENT_PAID", "PAYMENT", targetId,
			"confirmed", Map.of("status", "PENDING"), Map.of("status", "PAID"));
		AuditEvent second = new AuditEvent(UUID.randomUUID(), actorId, "PAYMENT_RECONCILED", "PAYMENT", targetId,
			"confirmed", Map.of("status", "PAID"), Map.of("status", "PAID"));
		Account actor = new Account(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE, "Reception Actor",
			"0901234567", "actor@example.test", LocalDate.of(1990, 1, 1), "{noop}password");
		Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(true);
		when(audits.findAll(any(Specification.class), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(first, second), pageable, 12));
		when(accounts.findAllById(Set.of(actorId))).thenReturn(List.of(actor));

		AuditHistoryPageResponse result = service.list(managerId, actorId, " payment_paid ", " payment ", targetId,
			Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-02T00:00:00Z"), pageable);

		assertThat(result.page()).isEqualTo(1);
		assertThat(result.totalElements()).isEqualTo(12);
		assertThat(result.content()).extracting(AuditHistoryResponse::actorName)
			.containsExactly("Reception Actor", "Reception Actor");
		assertThat(result.content()).extracting(AuditHistoryResponse::actorRole)
			.containsExactly(AccountRole.RECEPTIONIST, AccountRole.RECEPTIONIST);
		assertThat(result.content().getFirst().beforeData()).containsEntry("status", "PENDING");
		verify(audits).findAll(any(Specification.class), eq(pageable));
		verify(accounts).findAllById(Set.of(actorId));
		verify(accounts, never()).findById(any());
	}

	@Test
	void preservesUnresolvedActorIdWithNullIdentity() {
		AuditEventRepository audits = mock(AuditEventRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(audits, accounts);
		UUID managerId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		AuditEvent event = new AuditEvent(UUID.randomUUID(), actorId, "ACCOUNT_CREATED", "ACCOUNT",
			UUID.randomUUID(), Map.of());
		Pageable pageable = PageRequest.of(0, 20);
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(true);
		when(audits.findAll(any(Specification.class), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(event), pageable, 1));
		when(accounts.findAllById(Set.of(actorId))).thenReturn(List.of());

		AuditHistoryResponse result = service.list(managerId, null, null, null, null, null, null, pageable)
			.content().getFirst();

		assertThat(result.actorAccountId()).isEqualTo(actorId);
		assertThat(result.actorName()).isNull();
		assertThat(result.actorRole()).isNull();
	}

	@Test
	void rejectsInvalidTimeWindowAndOversizedFilters() {
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(mock(AuditEventRepository.class), accounts);
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(true);
		assertThatThrownBy(() -> service.list(managerId, null, null, null, null,
			Instant.parse("2026-10-02T00:00:00Z"), Instant.parse("2026-10-01T00:00:00Z"), PageRequest.of(0, 20)))
			.isInstanceOf(AuditHistoryValidationException.class)
			.hasMessage("must be after from");
		assertThatThrownBy(() -> service.list(managerId, null, "x".repeat(121), null, null, null, null,
			PageRequest.of(0, 20)))
			.isInstanceOf(AuditHistoryValidationException.class)
			.hasMessage("must be at most 120 characters");
	}

	@Test
	void requiresAnActiveManager() {
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(mock(AuditEventRepository.class), accounts);
		UUID managerId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(false);

		assertThatThrownBy(() -> service.list(managerId, null, null, null, null, null, null, PageRequest.of(0, 20)))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}
}
