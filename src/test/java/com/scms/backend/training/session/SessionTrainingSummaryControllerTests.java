package com.scms.backend.training.session;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

@WebMvcTest(controllers = SessionTrainingSummaryController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class SessionTrainingSummaryControllerTests {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	SessionTrainingSummaryService service;

	@MockitoBean
	ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID COACH_ID = UUID.randomUUID();

	private static final UUID SESSION_ID = UUID.randomUUID();

	private static final UUID MEMBER_ID = UUID.randomUUID();

	private String url() {
		return "/coach/class-sessions/" + SESSION_ID + "/members/" + MEMBER_ID + "/training-summary";
	}

	@Test
	void coachCanSaveBothFields() throws Exception {
		mockMvc.perform(put(url()).with(jwt().jwt(b -> b.subject(COACH_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_COACH"))).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Done\",\"feedback\":\"Good\"}")).andExpect(status().isOk());
		org.mockito.Mockito.verify(service).save(COACH_ID, SESSION_ID, MEMBER_ID, "Done", "Good");
	}

	@Test
	void rejectsBlankInputWithoutCallingService() throws Exception {
		mockMvc.perform(put(url()).with(jwt().jwt(b -> b.subject(COACH_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_COACH"))).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Done\",\"feedback\":\"  \"}")).andExpect(status().isBadRequest());
		org.mockito.Mockito.verifyNoInteractions(service);
	}

	@Test
	void memberCannotWriteCoachSummary() throws Exception {
		mockMvc.perform(put(url()).with(jwt().jwt(b -> b.subject(MEMBER_ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Done\",\"feedback\":\"Good\"}")).andExpect(status().isForbidden());
		org.mockito.Mockito.verifyNoInteractions(service);
	}
}
