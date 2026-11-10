package com.scms.backend.training.session;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookedSessionTrainingController.class)
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class BookedSessionTrainingControllerTests {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	BookedSessionTrainingService service;

	@MockitoBean
	ActiveAccountJwtValidator activeAccountJwtValidator;

	final UUID coach = UUID.randomUUID(), session = UUID.randomUUID(), member = UUID.randomUUID();

	String path(boolean course) {
		return "/coach/" + (course ? "course" : "pt") + "-sessions/" + session + "/members/" + member + "/training-summary";
	}

	@Test
	void coachReadsBothSessionTypesScopedToJwt() throws Exception {
		for (boolean course : new boolean[] { true, false }) {
			when(service.get(coach, session, member, course)).thenReturn(new BookedSessionTrainingService.Summary(null, null, true));
			mvc.perform(get(path(course)).with(jwt().jwt(token -> token.subject(coach.toString())).authorities(new SimpleGrantedAuthority("ROLE_COACH")))).andExpect(status().isOk()).andExpect(jsonPath("$.writable").value(true));
			verify(service).get(coach, session, member, course);
		}
	}

	@Test
	void coachSavesWithoutClientSuppliedActivityTime() throws Exception {
		for (boolean course : new boolean[] { true, false }) {
			mvc.perform(put(path(course)).with(jwt().jwt(token -> token.subject(coach.toString())).authorities(new SimpleGrantedAuthority("ROLE_COACH"))).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Done\",\"feedback\":\"Good\"}")).andExpect(status().isOk());
			verify(service).save(coach, session, member, course, null, "Done", "Good");
		}
	}

	@Test
	void rejectsWrongRoleAndBlankContentForBothTypes() throws Exception {
		for (boolean course : new boolean[] { true, false }) {
			mvc.perform(get(path(course)).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))).andExpect(status().isForbidden());
			mvc.perform(put(path(course)).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\"Done\",\"feedback\":\"Good\"}")).andExpect(status().isForbidden());
			mvc.perform(put(path(course)).with(jwt().jwt(token -> token.subject(coach.toString())).authorities(new SimpleGrantedAuthority("ROLE_COACH"))).contentType(MediaType.APPLICATION_JSON).content("{\"notes\":\" \",\"feedback\":\"Good\"}")).andExpect(status().isBadRequest());
		}
		verifyNoInteractions(service);
	}
}
