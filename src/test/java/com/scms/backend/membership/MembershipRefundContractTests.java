package com.scms.backend.membership;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

@WebMvcTest(controllers = MembershipRefundController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MembershipRefundContractTests {
	@Autowired MockMvc mockMvc;
	@MockitoBean MembershipRefundService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void eligibleSourceDiscoveryReturnsAnEmptyArrayAndRejectsOtherRoles() throws Exception {
		UUID receptionist = UUID.randomUUID();
		when(service.refundEligibleSources(receptionist, "MB-123456")).thenReturn(List.of());
		mockMvc.perform(get("/api/v1/reception/members/{memberCode}/refund-eligible-sources", "MB-123456")
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(receptionist.toString()).claim("role", "RECEPTIONIST"))
					.authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
			.andExpect(status().isOk())
			.andExpect(content().json("[]"));
		verify(service).refundEligibleSources(receptionist, "MB-123456");
		for (String role : new String[] { "MEMBER", "COACH", "MANAGER" }) {
			mockMvc.perform(get("/api/v1/reception/members/{memberCode}/refund-eligible-sources", "MB-123456")
					.contextPath("/api/v1")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void refundListsAreRestrictedToTheirStaffRoles() throws Exception {
		when(service.listForManager(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(20)))
			.thenReturn(new RefundRequestPageResponse(List.of(), 0, 20, 0, 0));
		when(service.listForReceptionist(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(0), org.mockito.ArgumentMatchers.eq(20)))
			.thenReturn(new RefundRequestPageResponse(List.of(), 0, 20, 0, 0));
		mockMvc.perform(get("/api/v1/manager/refund-requests").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/reception/refund-requests").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "RECEPTIONIST"))
					.authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
			.andExpect(status().isOk());
		for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
			mockMvc.perform(get("/api/v1/manager/refund-requests").contextPath("/api/v1")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
		for (String role : new String[] { "MEMBER", "COACH", "MANAGER" }) {
			mockMvc.perform(get("/api/v1/reception/refund-requests").contextPath("/api/v1")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void onlyManagerCanApproveAndOnlyReceptionistCanRecordCashDisbursement() throws Exception {
		UUID id = UUID.randomUUID();
		for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
			mockMvc.perform(post("/api/v1/manager/refund-requests/{id}/first-approval", id).contextPath("/api/v1")
					.contentType("application/json").content("{\"expectedVersion\":0,\"approvedAmount\":100}")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
		for (String role : new String[] { "MEMBER", "COACH", "MANAGER" }) {
			mockMvc.perform(post("/api/v1/reception/refund-requests/{id}/cash-disbursement", id).contextPath("/api/v1")
					.contentType("application/json").content("{\"expectedVersion\":0,\"evidence\":\"cash voucher\"}")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void creationAndFinalDecisionsRejectOtherStaffRoles() throws Exception {
		UUID id = UUID.randomUUID();
		for (String role : new String[] { "MEMBER", "COACH", "MANAGER" }) {
			mockMvc.perform(post("/api/v1/reception/members/{memberCode}/refund-requests", "MB-123456")
					.contextPath("/api/v1").contentType("application/json")
					.content("{\"paymentId\":\"" + id + "\",\"membershipId\":\"" + id
						+ "\",\"requestedAmount\":100,\"reason\":\"reason\",\"idempotencyKey\":\"key\"}")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
		for (String path : new String[] { "final-approval", "reject" }) {
			for (String role : new String[] { "MEMBER", "COACH", "RECEPTIONIST" }) {
				mockMvc.perform(post("/api/v1/manager/refund-requests/{id}/" + path, id).contextPath("/api/v1")
						.contentType("application/json").content("{\"expectedVersion\":0,\"reason\":\"reason\"}")
						.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
							.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
					.andExpect(status().isForbidden());
			}
		}
	}
}
