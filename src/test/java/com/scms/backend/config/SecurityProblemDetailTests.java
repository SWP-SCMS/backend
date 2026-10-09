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

	@RestController
	static class TestController {
		@GetMapping({ "/protected", "/manager/members", "/membership-offers" })
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
	}
}
