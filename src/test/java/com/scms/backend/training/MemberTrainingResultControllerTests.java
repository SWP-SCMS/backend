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

@WebMvcTest(controllers = MemberTrainingResultController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MemberTrainingResultControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean MemberTrainingResultService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void memberViewsHistoryScopedByJwtSubject() throws Exception {
		UUID memberId = UUID.randomUUID();
		UUID sessionId = UUID.randomUUID();
		Instant activityAt = Instant.parse("2026-10-14T07:00:00Z");
		when(service.viewOwnHistory(memberId)).thenReturn(List.of(new MemberTrainingResultResponse(
			UUID.randomUUID(), memberId, null, null, sessionId, activityAt, Map.of("notes", "Yoga"),
			UUID.randomUUID(), activityAt, activityAt)));

		mockMvc.perform(get("/members/me/training-results")
			.with(jwt().jwt(token -> token.subject(memberId.toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].memberAccountId").value(memberId.toString()))
			.andExpect(jsonPath("$[0].classSessionId").value(sessionId.toString()))
			.andExpect(jsonPath("$[0].content.notes").value("Yoga"));
	}

	@Test
	void endpointRejectsCoachRole() throws Exception {
		mockMvc.perform(get("/members/me/training-results")
			.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()))
				.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isForbidden());
	}
}
