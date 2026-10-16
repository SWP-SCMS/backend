package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ReceptionClassSessionController.class)
class ReceptionClassSessionControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ClassSessionService service;

	@Test
	void passesSupportedFiltersPaginationAndFixedSortToService() throws Exception {
		UUID receptionistId = UUID.randomUUID();
		UUID classId = UUID.randomUUID();
		Instant from = Instant.parse("2030-01-01T00:00:00Z");
		Instant to = Instant.parse("2030-01-02T00:00:00Z");
		org.mockito.Mockito.when(service.receptionSchedule(eq(receptionistId), eq(from), eq(to), eq(classId),
			org.mockito.ArgumentMatchers.any(Pageable.class)))
			.thenReturn(new MemberClassSessionPageResponse(List.of(), 2, 5, 0, 0));

		mockMvc.perform(get("/api/v1/reception/class-sessions")
				.contextPath("/api/v1")
				.param("from", from.toString()).param("to", to.toString())
				.param("classId", classId.toString()).param("page", "2").param("size", "5")
				.with(jwt().jwt(token -> token.subject(receptionistId.toString())
					.claim("role", "RECEPTIONIST"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(2))
			.andExpect(jsonPath("$.size").value(5));

		ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
		verify(service).receptionSchedule(eq(receptionistId), eq(from), eq(to), eq(classId), pageable.capture());
		assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
		assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
		assertThat(pageable.getValue().getSort().getOrderFor("startTime").getDirection())
			.isEqualTo(Sort.Direction.ASC);
		assertThat(pageable.getValue().getSort().getOrderFor("id").getDirection())
			.isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void rejectsPaginationOutsideExistingLimitsBeforeCallingService() throws Exception {
		mockMvc.perform(get("/api/v1/reception/class-sessions")
				.contextPath("/api/v1").param("size", "101")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
					.claim("role", "RECEPTIONIST"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.page[0]")
				.value("page must be >= 0 and size must be between 1 and 100"));

		verifyNoInteractions(service);
	}
}
