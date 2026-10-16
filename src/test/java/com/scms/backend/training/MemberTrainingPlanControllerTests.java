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

	@Test
	void memberViewsOwnPlanAndVersions() throws Exception {
		UUID memberId = UUID.randomUUID();
		var version = new MemberTrainingPlanVersionResponse(UUID.randomUUID(), 1,
				Map.of("goal", "Strength"), UUID.randomUUID(), Instant.parse("2026-10-13T06:00:00Z"));
		var plan = new MemberTrainingPlanResponse(UUID.randomUUID(), memberId, UUID.randomUUID(), "Strength",
				TrainingPlanStatus.ACTIVE, UUID.randomUUID(), version.createdAt(), version.createdAt(), List.of(version));
		when(service.viewOwnPlans(memberId)).thenReturn(List.of(plan));

		mockMvc.perform(get("/members/me/training-plans")
				.with(jwt().jwt(token -> token.subject(memberId.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].memberAccountId").value(memberId.toString()))
			.andExpect(jsonPath("$[0].versions[0].versionNumber").value(1));
	}

	@Test
	void memberPlanEndpointRejectsCoachRole() throws Exception {
		mockMvc.perform(get("/members/me/training-plans")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isForbidden());
	}
}
