package com.scms.backend.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TrainingResultController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class TrainingResultControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean TrainingResultService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID COACH_ID = UUID.randomUUID();
	private static final UUID MEMBER_ID = UUID.randomUUID();
	private static final UUID PLAN_VERSION_ID = UUID.randomUUID();
	private static final Instant ACTIVITY_AT = Instant.parse("2026-10-13T07:00:00Z");

	@Test
	void listReturnsPersonalAndSessionResultsWithSessionLinkage() throws Exception {
		UUID sessionId = UUID.randomUUID();
		TrainingResultResponse row = new TrainingResultResponse(UUID.randomUUID(), MEMBER_ID, null, null,
				sessionId, ACTIVITY_AT, Map.of("notes", "Class result"), COACH_ID, ACTIVITY_AT, ACTIVITY_AT);
		when(service.list(eq(COACH_ID), eq(MEMBER_ID), any()))
				.thenReturn(new TrainingResultPageResponse(List.of(row), 0, 20, 1, 1));

		mockMvc.perform(get("/coach/members/{memberId}/training-results", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].classSessionId").value(sessionId.toString()))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void createReturns201ForCoach() throws Exception {
		TrainingResultResponse response = response(UUID.randomUUID(), "Completed");
		when(service.create(eq(COACH_ID), eq(MEMBER_ID), any())).thenReturn(response);

		mockMvc.perform(post("/coach/members/{memberId}/training-results", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("Completed")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberAccountId").value(MEMBER_ID.toString()))
				.andExpect(jsonPath("$.trainingPlanVersionId").value(PLAN_VERSION_ID.toString()))
				.andExpect(jsonPath("$.content.notes").value("Completed"));
	}

	@Test
	void updateReturns200ForCoach() throws Exception {
		UUID resultId = UUID.randomUUID();
		when(service.update(eq(COACH_ID), eq(MEMBER_ID), eq(resultId), any()))
				.thenReturn(response(resultId, "Updated"));

		mockMvc.perform(put("/coach/members/{memberId}/training-results/{resultId}", MEMBER_ID, resultId)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("Updated")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(resultId.toString()))
				.andExpect(jsonPath("$.content.notes").value("Updated"));
	}

	@Test
	void createRejectsMissingPlanVersion() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/training-results", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"activityAt\":\"2026-10-13T07:00:00Z\",\"content\":{}}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void endpointsRejectMemberRole() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/training-results", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("Denied")))
				.andExpect(status().isForbidden());
	}

	private TrainingResultResponse response(UUID id, String notes) {
		return new TrainingResultResponse(id, MEMBER_ID, UUID.randomUUID(), PLAN_VERSION_ID, null, ACTIVITY_AT,
				Map.of("notes", notes), COACH_ID, ACTIVITY_AT, ACTIVITY_AT);
	}

	private String requestJson(String notes) {
		return """
			{"trainingPlanVersionId":"%s","activityAt":"%s","content":{"notes":"%s"}}
			""".formatted(PLAN_VERSION_ID, ACTIVITY_AT, notes);
	}
}
