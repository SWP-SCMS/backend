package com.scms.backend.audit;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuditHistoryController.class)
class AuditHistoryControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean AuditHistoryService service;

	@Test
	void listsAuditHistoryWithAllSupportedFilters() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID actorId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();
		UUID eventId = UUID.randomUUID();
		Instant from = Instant.parse("2026-10-01T00:00:00Z");
		Instant to = Instant.parse("2026-10-02T00:00:00Z");
		AuditHistoryResponse event = new AuditHistoryResponse(eventId, actorId, "Reception Actor",
			AccountRole.RECEPTIONIST, "PAYMENT_PAID", "PAYMENT", targetId, "confirmed",
			Map.of("status", "PENDING"), Map.of("status", "PAID"), to.minusSeconds(1));
		UUID unresolvedActorId = UUID.randomUUID();
		AuditHistoryResponse unresolved = new AuditHistoryResponse(UUID.randomUUID(), unresolvedActorId, null, null,
			"ACCOUNT_CREATED", "ACCOUNT", targetId, null, null, Map.of(), to.minusSeconds(2));
		when(service.list(eq(managerId), eq(actorId), eq("PAYMENT_PAID"), eq("PAYMENT"), eq(targetId),
			eq(from), eq(to), any(Pageable.class)))
			.thenReturn(new AuditHistoryPageResponse(List.of(event, unresolved), 0, 20, 2, 1));

		mockMvc.perform(get("/api/v1/manager/audit-events").contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(managerId.toString())))
			.queryParam("actorId", actorId.toString())
			.queryParam("action", "PAYMENT_PAID")
			.queryParam("targetType", "PAYMENT")
			.queryParam("targetId", targetId.toString())
			.queryParam("from", from.toString())
			.queryParam("to", to.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].id").value(eventId.toString()))
			.andExpect(jsonPath("$.content[0].actorAccountId").value(actorId.toString()))
			.andExpect(jsonPath("$.content[0].actorName").value("Reception Actor"))
			.andExpect(jsonPath("$.content[0].actorRole").value("RECEPTIONIST"))
			.andExpect(jsonPath("$.content[0].beforeData.status").value("PENDING"))
			.andExpect(jsonPath("$.content[1].actorAccountId").value(unresolvedActorId.toString()))
			.andExpect(jsonPath("$.content[1].actorName").value(nullValue()))
			.andExpect(jsonPath("$.content[1].actorRole").value(nullValue()));

		verify(service).list(eq(managerId), eq(actorId), eq("PAYMENT_PAID"), eq("PAYMENT"), eq(targetId),
			eq(from), eq(to), any(Pageable.class));
	}

	@Test
	void rejectsInvalidPagination() throws Exception {
		mockMvc.perform(get("/api/v1/manager/audit-events").contextPath("/api/v1")
			.with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString())))
			.queryParam("page", "-1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}
}
