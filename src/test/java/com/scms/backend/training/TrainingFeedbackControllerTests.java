package com.scms.backend.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

@WebMvcTest(controllers = TrainingFeedbackController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class TrainingFeedbackControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean TrainingFeedbackService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID COACH_ID = UUID.randomUUID();
	private static final UUID MEMBER_ID = UUID.randomUUID();
	private static final UUID PLAN_VERSION_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-14T08:00:00Z");

	@Test
	void createReturns201ForCoach() throws Exception {
		TrainingFeedbackResponse response = response(UUID.randomUUID(), "Good progress");
		when(service.create(eq(COACH_ID), eq(MEMBER_ID), any())).thenReturn(response);

		mockMvc.perform(post("/coach/members/{memberId}/feedback", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("Good progress")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberAccountId").value(MEMBER_ID.toString()))
				.andExpect(jsonPath("$.trainingPlanVersionId").value(PLAN_VERSION_ID.toString()))
				.andExpect(jsonPath("$.content").value("Good progress"));
	}

	@Test
	void updateReturns200ForCoach() throws Exception {
		UUID feedbackId = UUID.randomUUID();
		when(service.update(eq(COACH_ID), eq(MEMBER_ID), eq(feedbackId), any()))
				.thenReturn(response(feedbackId, "Updated"));

		mockMvc.perform(put("/coach/members/{memberId}/feedback/{feedbackId}", MEMBER_ID, feedbackId)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("Updated")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(feedbackId.toString()))
				.andExpect(jsonPath("$.content").value("Updated"));
	}

	@Test
	void createRejectsMissingOrAmbiguousContext() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/feedback", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"Missing context\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/coach/members/{memberId}/feedback", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"trainingPlanVersionId":"%s","trainingResultId":"%s","content":"Ambiguous"}
					""".formatted(PLAN_VERSION_ID, UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void endpointsRejectMemberRole() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/feedback", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestJson("Denied")))
				.andExpect(status().isForbidden());
	}

	private TrainingFeedbackResponse response(UUID id, String content) {
		return new TrainingFeedbackResponse(id, MEMBER_ID, UUID.randomUUID(), PLAN_VERSION_ID, null,
				content, COACH_ID, NOW, NOW);
	}

	private String requestJson(String content) {
		return """
			{"trainingPlanVersionId":"%s","content":"%s"}
			""".formatted(PLAN_VERSION_ID, content);
	}
}
