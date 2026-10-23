package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
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

@WebMvcTest(controllers = MembershipFreezeController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MembershipFreezeContractTests {
	@Autowired MockMvc mockMvc;
	@MockitoBean MembershipFreezeService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void migrationDefinesTheInitialFreezeRequestWorkflowAndCashFeeLedger() throws Exception {
		String migration = Files.readString(Path.of("src/main/resources/db/migration/V36__membership_freeze_requests.sql"));
		assertThat(migration).contains("create table membership_freeze_requests")
			.contains("APPROVED_PENDING_PAYMENT").contains("SCHEDULED")
			.contains("create table membership_freeze_periods").contains("create table membership_freeze_fee_payments");
	}

	@Test
	void executionMigrationActivatesFreezeAndMakesTheBookingDatabaseAuthorityRejectFrozenMembers() throws Exception {
		String migration = Files.readString(Path.of("src/main/resources/db/migration/V37__membership_freeze_execution.sql"));
		assertThat(migration).contains("'ACTIVE'", "'COMPLETED'", "trg_bookings_active_membership_freeze", "MEMBERSHIP_FROZEN",
			"MEMBERSHIP_FREEZE", "chk_center_visits_checkout_source");
	}

	@Test
	void openApiIncludesEveryFreezeStatusReturnedAfterExecution() throws Exception {
		String contract = Files.readString(Path.of("src/main/resources/openapi/membership-freeze.yaml"));
		assertThat(contract).contains("ACTIVE", "COMPLETED", "OBSOLETE",
			"/members/me/membership-freeze-requests/{requestId}/end-early:",
			"/reception/membership-freeze-requests/{requestId}/end-early:");
	}

	@Test
	void memberAndReceptionistCanReachEarlyEndEndpointsWithExpectedVersion() throws Exception {
		UUID requestId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID receptionistId = UUID.randomUUID();
		MembershipFreezeDecisionRequest decision = new MembershipFreezeDecisionRequest(7L);
		mockMvc.perform(post("/api/v1/members/me/membership-freeze-requests/{requestId}/end-early", requestId).contextPath("/api/v1")
				.contentType("application/json").content("{\"expectedVersion\":7}")
				.with(jwt().jwt(token -> token.subject(memberId.toString()).claim("role", "MEMBER")).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/reception/membership-freeze-requests/{requestId}/end-early", requestId).contextPath("/api/v1")
				.contentType("application/json").content("{\"expectedVersion\":7}")
				.with(jwt().jwt(token -> token.subject(receptionistId.toString()).claim("role", "RECEPTIONIST")).authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
			.andExpect(status().isOk());
		verify(service).endEarlyForMember(memberId, requestId, decision);
		verify(service).endEarlyForReceptionist(receptionistId, requestId, decision);
	}

	@Test
	void onlyMemberCanCreateOwnRequestAndOnlyManagerCanApprove() throws Exception {
		for (String role : new String[] { "RECEPTIONIST", "COACH", "MANAGER" }) {
			mockMvc.perform(post("/api/v1/members/me/membership-freeze-requests").contextPath("/api/v1")
					.contentType("application/json").content("{\"startDate\":\"2026-11-01\",\"endDate\":\"2026-11-02\",\"reason\":\"travel\"}")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role)).authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
		for (String role : new String[] { "MEMBER", "RECEPTIONIST", "COACH" }) {
			mockMvc.perform(post("/api/v1/manager/membership-freeze-requests/{id}/approve", UUID.randomUUID()).contextPath("/api/v1")
					.contentType("application/json").content("{\"expectedVersion\":0}")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role)).authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void memberCanReachOwnCollection() throws Exception {
		UUID memberId = UUID.randomUUID();
		when(service.listForMember(memberId, 0, 20)).thenReturn(MembershipFreezeRequestPageResponse.empty());
		mockMvc.perform(get("/api/v1/members/me/membership-freeze-requests").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(memberId.toString()).claim("role", "MEMBER")).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk());
		verify(service).listForMember(memberId, 0, 20);
	}

	@Test
	void openApiDocumentsTheThreeInitialWorkflowStatesAndCashEndpoint() throws Exception {
		String openApi = Files.readString(Path.of("src/main/resources/openapi/membership-freeze.yaml"));
		assertThat(openApi).contains("APPROVED_PENDING_PAYMENT", "SCHEDULED", "/cash-payment");
	}
}
