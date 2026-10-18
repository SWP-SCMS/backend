package com.scms.backend.training;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MemberCoachAssignmentController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MemberCoachAssignmentControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean MemberCoachAssignmentService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private final UUID memberId = UUID.randomUUID();

	@Test
	void memberReadsOwnActiveAssignment() throws Exception {
		UUID assignmentId = UUID.randomUUID();
		UUID coachId = UUID.randomUUID();
		when(service.getActive(memberId)).thenReturn(Optional.of(new MemberCoachAssignmentResponse(
				assignmentId, coachId, "Coach Alpha", "0911111111", "coach@example.com",
				CoachAssignmentStatus.ACTIVE, Instant.parse("2026-10-18T03:00:00Z"))));

		mockMvc.perform(get("/members/me/coach-assignment")
				.with(jwt().jwt(builder -> builder.subject(memberId.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.assignmentId").value(assignmentId.toString()))
				.andExpect(jsonPath("$.coachAccountId").value(coachId.toString()))
				.andExpect(jsonPath("$.coachFullName").value("Coach Alpha"));
	}

	@Test
	void memberWithoutAssignmentGets204() throws Exception {
		when(service.getActive(memberId)).thenReturn(Optional.empty());
		mockMvc.perform(get("/members/me/coach-assignment")
				.with(jwt().jwt(builder -> builder.subject(memberId.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
				.andExpect(status().isNoContent());
	}

	@Test
	void coachCannotReadMemberSelfServiceRoute() throws Exception {
		mockMvc.perform(get("/members/me/coach-assignment")
				.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isForbidden());
	}
}
