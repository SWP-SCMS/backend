package com.scms.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class AuditHistoryServiceTests {

	@Test
	void listsAuditEventsWithStableMappingAndPagination() {
		AuditEventRepository audits = mock(AuditEventRepository.class);
		AccountRepository accounts = mock(AccountRepository.class);
		AuditHistoryService service = new AuditHistoryService(audits, accounts);
		UUID managerId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		AuditEvent event = new AuditEvent(UUID.randomUUID(), actorId, "PAYMENT_PAID", "PAYMENT", targetId,
			"confirmed", Map.of("status", "PENDING"), Map.of("status", "PAID"));
		Pageable pageable = PageRequest.of(1, 10, Sort.by("createdAt").descending().and(Sort.by("id").descending()));
		when(accounts.existsByIdAndRoleAndStatus(eq(managerId), any(), any())).thenReturn(true);
		when(audits.findAll(any(Specification.class), eq(pageable)))
			.thenReturn(new PageImpl<>(List.of(event), pageable, 11));

		AuditHistoryPageResponse result = service.list(managerId, actorId, " payment_paid ", " payment ", targetId,
			Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-02T00:00:00Z"), pageable);

		assertThat(result.page()).isEqualTo(1);
		assertThat(result.totalElements()).isEqualTo(11);
		assertThat(result.content()).singleElement().satisfies(mapped -> {
			assertThat(mapped.id()).isEqualTo(event.getId());
			assertThat(mapped.beforeData()).containsEntry("status", "PENDING");
			assertThat(mapped.afterData()).containsEntry("status", "PAID");
		});
		verify(audits).findAll(any(Specification.class), eq(pageable));
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
