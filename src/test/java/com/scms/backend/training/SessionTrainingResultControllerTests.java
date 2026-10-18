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
import java.util.Map;
import java.util.Optional;
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

@WebMvcTest(controllers = SessionTrainingResultController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class SessionTrainingResultControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean SessionTrainingResultService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID COACH_ID = UUID.randomUUID();
	private static final UUID SESSION_ID = UUID.randomUUID();
	private static final UUID MEMBER_ID = UUID.randomUUID();
	private static final Instant ACTIVITY_AT = Instant.parse("2026-10-14T07:00:00Z");

	@Test
	void getReturnsLinkedResultOr204() throws Exception {
		when(service.get(COACH_ID, SESSION_ID, MEMBER_ID))
				.thenReturn(Optional.of(response(UUID.randomUUID(), "Completed")));
		mockMvc.perform(get("/coach/class-sessions/{sessionId}/members/{memberId}/training-results",
				SESSION_ID, MEMBER_ID).with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.classSessionId").value(SESSION_ID.toString()));

		when(service.get(COACH_ID, SESSION_ID, MEMBER_ID)).thenReturn(Optional.empty());
		mockMvc.perform(get("/coach/class-sessions/{sessionId}/members/{memberId}/training-results",
				SESSION_ID, MEMBER_ID).with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isNoContent());
	}

	@Test
	void createReturns201ForCoach() throws Exception {
		when(service.create(eq(COACH_ID), eq(SESSION_ID), eq(MEMBER_ID), any()))
			.thenReturn(response(UUID.randomUUID(), "Completed"));

		mockMvc.perform(post("/coach/class-sessions/{sessionId}/members/{memberId}/training-results",
				SESSION_ID, MEMBER_ID)
			.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
			.contentType(MediaType.APPLICATION_JSON).content(requestJson("Completed")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memberAccountId").value(MEMBER_ID.toString()))
			.andExpect(jsonPath("$.classSessionId").value(SESSION_ID.toString()))
			.andExpect(jsonPath("$.content.notes").value("Completed"));
	}

	@Test
	void updateReturns200ForCoach() throws Exception {
		UUID resultId = UUID.randomUUID();
		when(service.update(eq(COACH_ID), eq(SESSION_ID), eq(MEMBER_ID), eq(resultId), any()))
			.thenReturn(response(resultId, "Updated"));

		mockMvc.perform(put("/coach/class-sessions/{sessionId}/members/{memberId}/training-results/{resultId}",
				SESSION_ID, MEMBER_ID, resultId)
			.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
			.contentType(MediaType.APPLICATION_JSON).content(requestJson("Updated")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(resultId.toString()))
			.andExpect(jsonPath("$.content.notes").value("Updated"));
	}

	@Test
	void createRejectsMissingActivityAt() throws Exception {
		mockMvc.perform(post("/coach/class-sessions/{sessionId}/members/{memberId}/training-results",
				SESSION_ID, MEMBER_ID)
			.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
			.contentType(MediaType.APPLICATION_JSON).content("{\"content\":{}}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void endpointsRejectMemberRole() throws Exception {
		mockMvc.perform(post("/coach/class-sessions/{sessionId}/members/{memberId}/training-results",
				SESSION_ID, MEMBER_ID)
			.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
			.contentType(MediaType.APPLICATION_JSON).content(requestJson("Denied")))
			.andExpect(status().isForbidden());
	}

	private SessionTrainingResultResponse response(UUID id, String notes) {
		return new SessionTrainingResultResponse(id, MEMBER_ID, SESSION_ID, ACTIVITY_AT,
			Map.of("notes", notes), COACH_ID, ACTIVITY_AT, ACTIVITY_AT);
	}

	private String requestJson(String notes) {
		return """
			{"activityAt":"%s","content":{"notes":"%s"}}
			""".formatted(ACTIVITY_AT, notes);
	}
}
