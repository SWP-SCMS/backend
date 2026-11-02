package com.scms.backend.course;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CourseReceptionActionsController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class CourseReceptionActionsControllerTests {
	@Autowired MockMvc mvc;
	@MockitoBean CourseWaitlistService waitlists;
	@MockitoBean CourseScheduleChangeService changes;
	@MockitoBean CourseTransferService transfers;
	@MockitoBean CourseWithdrawalService withdrawals;
	@MockitoBean CourseRefundObligationService refunds;
	@MockitoBean ActiveAccountJwtValidator activeAccounts;

	@ParameterizedTest
	@ValueSource(strings = { "MEMBER", "COACH", "MANAGER", "ANONYMOUS" })
	void allProxyRoutesRequireReceptionist(String role) throws Exception {
		String base = "/reception/members/" + UUID.randomUUID(), id = UUID.randomUUID().toString();
		for (String route : List.of("GET /course-waitlists", "POST /course-runs/" + id + "/waitlist",
			"DELETE /course-runs/" + id + "/waitlist", "POST /course-waitlists/" + id + "/accept",
			"GET /course-schedule-changes", "POST /course-schedule-changes/" + id + "/response",
			"GET /course-transfer-offers", "POST /course-transfer-offers/" + id + "/accept",
			"GET /course-withdrawal-requests", "GET /course-withdrawal-requests/" + id,
			"GET /course-refund-obligations", "GET /course-refund-obligations/" + id)) {
			String[] parts = route.split(" ");
			var call = request(HttpMethod.valueOf(parts[0]), base + parts[1]).header("Idempotency-Key", "role-test")
				.contentType("application/json").content("{\"response\":\"CONTINUE\"}");
			if (!role.equals("ANONYMOUS")) call.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString())
				.claim("role", role)).authorities(new SimpleGrantedAuthority("ROLE_" + role)));
			mvc.perform(call).andExpect(role.equals("ANONYMOUS") ? status().isUnauthorized() : status().isForbidden());
		}
		verifyNoInteractions(waitlists, changes, transfers, withdrawals, refunds);
	}
}
