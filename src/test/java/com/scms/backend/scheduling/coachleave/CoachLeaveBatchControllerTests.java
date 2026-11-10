package com.scms.backend.scheduling.coachleave;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoachLeaveBatchController.class)
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class CoachLeaveBatchControllerTests {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	CoachLeaveBatchService service;

	@MockitoBean
	ActiveAccountJwtValidator activeAccountJwtValidator;

	final UUID manager = UUID.randomUUID(), request = UUID.randomUUID(), session = UUID.randomUUID();

	String base() {
		return "/manager/coach-leave-requests/" + request;
	}

	@Test
	void managerCanSubmitBatchAndOtherRolesCannot() throws Exception {
		String body = "{\"sessions\":[{\"type\":\"CLASS\",\"id\":\"" + session + "\",\"expectedVersion\":2}],\"action\":\"cancel\",\"reason\":\"Leave\"}";
		when(service.resolve(eq(manager), eq(request), any())).thenReturn(new CoachLeaveBatchService.Result(1));
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(base() + "/batch").contentType("application/json").content(body).with(jwt().jwt(token -> token.subject(manager.toString())).authorities(new SimpleGrantedAuthority("ROLE_MANAGER")))).andExpect(status().isOk()).andExpect(jsonPath("$.processedCount").value(1));
		clearInvocations(service);
		for (String role : List.of("COACH", "MEMBER", "RECEPTIONIST")) for (String suffix : List.of("/batch", "/batch/available-coaches")) {
			mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(base() + suffix).contentType("application/json").content(body).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)))).andExpect(status().isForbidden());
		}
		verifyNoInteractions(service);
	}
}
