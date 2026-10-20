package com.scms.backend.membership;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigInteger;
import java.util.List;
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

@WebMvcTest(controllers = MembershipOfferAdminController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MembershipOfferAdminControllerTests {
	@Autowired MockMvc mockMvc;
	@MockitoBean MembershipOfferAdminService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void managerCanReadOfferDetail() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		when(service.get(managerId, offerId)).thenReturn(new MembershipOfferAdminDetailResponse(offerId, "ARCHIVED",
			"Archived", "Archived offer", "Description", BigInteger.valueOf(500000), "VND", 30,
			MembershipOfferStatus.INACTIVE, List.of("A_ACCESS"), List.of(
				new MembershipBenefitResponse("A_ACCESS", "A", "A desc", MembershipBenefitStatus.INACTIVE))));

		mockMvc.perform(get("/api/v1/manager/membership-offers/{offerId}", offerId).contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.planDisplayName").value("Archived"))
			.andExpect(jsonPath("$.benefitCodes[0]").value("A_ACCESS"));
	}

	@Test
	void nonManagersCannotReadOfferDetail() throws Exception {
		for (String role : List.of("MEMBER", "COACH", "RECEPTIONIST")) {
			mockMvc.perform(get("/api/v1/manager/membership-offers/{offerId}", UUID.randomUUID())
					.contextPath("/api/v1")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
		verifyNoInteractions(service);
	}
}
