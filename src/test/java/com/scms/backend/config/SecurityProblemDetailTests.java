package com.scms.backend.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityProblemDetailTests.TestController.class,
	excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class,
	SecurityProblemDetailTests.TestController.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class SecurityProblemDetailTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void unauthenticatedRequestsUseTheApiProblemContract() throws Exception {
		mockMvc.perform(get("/protected"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Authentication required"))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.detail").value("Authentication is required to access this resource."))
			.andExpect(jsonPath("$.instance").value("/protected"))
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void forbiddenRequestsUseTheApiProblemContract() throws Exception {
		mockMvc.perform(get("/manager/members").with(user("member").roles("MEMBER")))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Access denied"))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.detail").value("You do not have permission to access this resource."))
			.andExpect(jsonPath("$.instance").value("/manager/members"))
			.andExpect(jsonPath("$.code").value("FORBIDDEN"))
			.andExpect(jsonPath("$.errors").doesNotExist());
	}

	@Test
	void membershipOffersArePublic() throws Exception {
		mockMvc.perform(get("/membership-offers"))
			.andExpect(status().isOk());
		mockMvc.perform(get("/membership-offers/{offerId}", "00000000-0000-0000-0000-000000000001"))
			.andExpect(status().isOk());
	}

	@Test
	void bookingViewsAreRoleScoped() throws Exception {
		mockMvc.perform(get("/members/me/bookings").with(user("receptionist").roles("RECEPTIONIST")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/reception/members/{memberId}/bookings", "00000000-0000-0000-0000-000000000001")
			.with(user("member").roles("MEMBER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void receptionClassSessionCatalogueIsReceptionistOnly() throws Exception {
		for (String role : new String[] { "MEMBER", "COACH", "MANAGER" }) {
			mockMvc.perform(get("/reception/class-sessions").with(user(role.toLowerCase()).roles(role)))
				.andExpect(status().isForbidden());
		}
		mockMvc.perform(get("/reception/class-sessions")
			.with(user("receptionist").roles("RECEPTIONIST")))
			.andExpect(status().isOk());
	}

	@Test
	void auditHistoryIsManagerOnly() throws Exception {
		mockMvc.perform(get("/manager/audit-events").with(user("member").roles("MEMBER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void managerDashboardIsManagerOnly() throws Exception {
		mockMvc.perform(get("/manager/dashboard").with(user("member").roles("MEMBER")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/manager/dashboard").with(user("manager").roles("MANAGER")))
			.andExpect(status().isOk());
	}

	@Test
	void staffMembershipHistoryRoutesAreRoleScoped() throws Exception {
		mockMvc.perform(get("/reception/members/MB-100001/memberships")
			.with(user("receptionist").roles("RECEPTIONIST")))
			.andExpect(status().isOk());
		mockMvc.perform(get("/manager/members/00000000-0000-0000-0000-000000000001/memberships")
			.with(user("manager").roles("MANAGER")))
			.andExpect(status().isOk());

		for (String role : new String[] { "MEMBER", "COACH", "MANAGER" }) {
			mockMvc.perform(get("/reception/members/MB-100001/memberships")
				.with(user(role.toLowerCase()).roles(role)))
				.andExpect(status().isForbidden());
		}
		for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
			mockMvc.perform(get("/manager/members/00000000-0000-0000-0000-000000000001/memberships")
				.with(user(role.toLowerCase()).roles(role)))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void assignmentDiscoveryRoutesAreManagerOnly() throws Exception {
		for (String path : new String[] {
			"/manager/coach-assignments/candidates",
			"/manager/coaches/assignment-workloads"
		}) {
			mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
			mockMvc.perform(get(path).with(user("manager").roles("MANAGER")))
				.andExpect(status().isOk());
			for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
				mockMvc.perform(get(path).with(user(role.toLowerCase()).roles(role)))
					.andExpect(status().isForbidden());
			}
		}
	}

	@Test
	void courseManagementRoutesAreManagerOnly() throws Exception {
		for (String path : new String[] { "/manager/courses", "/manager/course-runs" }) {
			mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
			mockMvc.perform(get(path).with(user("manager").roles("MANAGER")))
				.andExpect(status().isOk());
			for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
				mockMvc.perform(get(path).with(user(role.toLowerCase()).roles(role)))
					.andExpect(status().isForbidden());
			}
		}
	}

	@RestController
	static class TestController {
		@GetMapping({ "/protected", "/manager/members", "/manager/audit-events", "/manager/dashboard",
			"/membership-offers", "/reception/members/{memberCode}/memberships",
			"/manager/members/{memberAccountId}/memberships", "/manager/coach-assignments/candidates",
			"/manager/coaches/assignment-workloads", "/manager/courses", "/manager/course-runs" })
		void protectedResource() {
		}

		@GetMapping("/membership-offers/{offerId}")
		void publicOfferDetail() {
		}

		@GetMapping("/members/me/bookings")
		void memberBookings() {
		}

		@GetMapping("/reception/members/{memberId}/bookings")
		void receptionBookings() {
		}

		@GetMapping("/reception/class-sessions")
		void receptionClassSessions() {
		}
	}
}
