package com.scms.backend.training;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberTrainingPlanController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MemberTrainingPlanControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean MemberTrainingPlanService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID MEMBER_ID = UUID.randomUUID();

	@Test
	void viewOwnPlansReturnsPlanAndVersionHistoryForMember() throws Exception {
		MemberTrainingPlanVersionResponse version = new MemberTrainingPlanVersionResponse(UUID.randomUUID(), 1,
				Map.of("goal", "Build strength"), UUID.randomUUID(), Instant.parse("2026-10-13T06:00:00Z"));
		MemberTrainingPlanResponse plan = new MemberTrainingPlanResponse(UUID.randomUUID(), MEMBER_ID,
				UUID.randomUUID(), "Strength plan", TrainingPlanStatus.ACTIVE, UUID.randomUUID(),
				Instant.parse("2026-10-13T06:00:00Z"), Instant.parse("2026-10-13T06:00:00Z"), List.of(version));
		when(service.viewOwnPlans(MEMBER_ID)).thenReturn(List.of(plan));

		mockMvc.perform(get("/members/me/training-plans")
				.with(jwt().jwt(builder -> builder.subject(MEMBER_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].memberAccountId").value(MEMBER_ID.toString()))
			.andExpect(jsonPath("$[0].versions[0].versionNumber").value(1))
			.andExpect(jsonPath("$[0].versions[0].content.goal").value("Build strength"));
	}

	@Test
	void viewOwnPlansReturns401WhenUnauthenticated() throws Exception {
		mockMvc.perform(get("/members/me/training-plans"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void viewOwnPlansReturns403ForCoachRole() throws Exception {
		mockMvc.perform(get("/members/me/training-plans")
				.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isForbidden());
	}
}
