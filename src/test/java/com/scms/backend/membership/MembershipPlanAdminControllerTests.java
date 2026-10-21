package com.scms.backend.membership;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@WebMvcTest(controllers = MembershipPlanAdminController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MembershipPlanAdminControllerTests {
	@Autowired MockMvc mockMvc;
	@MockitoBean MembershipPlanAdminService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void managerCanReadBenefitCatalog() throws Exception {
		UUID managerId = UUID.randomUUID();
		when(service.catalog(managerId)).thenReturn(List.of(
			new MembershipBenefitResponse("A_ACCESS", "A", "A desc", MembershipBenefitStatus.ACTIVE,
				20, "ACCESS", "Access", 10)));

		mockMvc.perform(get("/api/v1/manager/membership-benefits").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("A_ACCESS"))
			.andExpect(jsonPath("$[0].displayOrder").value(20))
			.andExpect(jsonPath("$[0].groupCode").value("ACCESS"))
			.andExpect(jsonPath("$[0].groupDisplayName").value("Access"))
			.andExpect(jsonPath("$[0].groupDisplayOrder").value(10));
	}

	@Test
	void memberCoachAndReceptionistAreDenied() throws Exception {
		for (String role : List.of("MEMBER", "COACH", "RECEPTIONIST")) {
			mockMvc.perform(get("/api/v1/manager/membership-benefits").contextPath("/api/v1")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
		verifyNoInteractions(service);
	}

	@Test
	void missingCreateBodyUsesMembershipPlanValidationContract() throws Exception {
		UUID managerId = UUID.randomUUID();
		when(service.create(managerId, null))
			.thenThrow(new MembershipPlanValidationException("request", "request is required"));

		mockMvc.perform(post("/api/v1/manager/membership-plans").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_PLAN_VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.request[0]").value("request is required"));
	}

	@Test
	void nullPatchBodyUsesMembershipPlanValidationContract() throws Exception {
		UUID managerId = UUID.randomUUID();
		when(service.update(managerId, "BASIC", null))
			.thenThrow(new MembershipPlanValidationException("request", "at least one field is required"));

		mockMvc.perform(patch("/api/v1/manager/membership-plans/BASIC").contextPath("/api/v1")
				.contentType(APPLICATION_JSON).content("null")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MEMBERSHIP_PLAN_VALIDATION_ERROR"))
			.andExpect(jsonPath("$.errors.request[0]").value("at least one field is required"));
	}

	@Test
	void malformedJsonStillUsesMalformedRequestContract() throws Exception {
		UUID managerId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/manager/membership-plans").contextPath("/api/v1")
				.contentType(APPLICATION_JSON).content("{")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
		verifyNoInteractions(service);
	}
}
