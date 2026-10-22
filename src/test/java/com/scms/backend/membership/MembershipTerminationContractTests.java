package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@WebMvcTest(controllers = MembershipTerminationController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class MembershipTerminationContractTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean MembershipTerminationService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void migrationDefinesOnePendingTerminationRequestPerMembership() throws Exception {
		String migration = Files.readString(Path.of("src/main/resources/db/migration/V31__membership_termination_requests.sql"));
		assertThat(migration).contains("create table membership_termination_requests");
		assertThat(migration).contains("unique index");
		assertThat(migration).contains("status in ('PENDING','APPROVED','EXECUTED','REJECTED','WITHDRAWN','OBSOLETE')");
	}

	@Test
	void memberCanReachOwnTerminationCollection() throws Exception {
		UUID memberId = UUID.randomUUID();
		when(service.listForMember(memberId, 0, 20)).thenReturn(MembershipTerminationRequestPageResponse.empty());

		mockMvc.perform(get("/api/v1/members/me/membership-termination-requests")
				.contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(memberId.toString()).claim("role", "MEMBER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk());
		verify(service).listForMember(memberId, 0, 20);
	}

	@Test
	void onlyManagerCanApproveOrReject() throws Exception {
		for (String role : new String[] { "MEMBER", "RECEPTIONIST", "COACH" }) {
			mockMvc.perform(post("/api/v1/manager/membership-termination-requests/{id}/approve", UUID.randomUUID())
					.contextPath("/api/v1")
					.contentType("application/json").content("{\"expectedVersion\":0}")
					.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", role))
						.authorities(new SimpleGrantedAuthority("ROLE_" + role))))
				.andExpect(status().isForbidden());
		}
	}

	@Test
	void invalidPaginationIsRejectedBeforeCallingService() throws Exception {
		UUID memberId = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/members/me/membership-termination-requests?page=-1").contextPath("/api/v1")
				.with(jwt().jwt(token -> token.subject(memberId.toString()).claim("role", "MEMBER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isBadRequest());
	}


	@Test
	void terminationModesAndStatusesMatchTheWorkflowContract() {
		assertThat(MembershipTerminationMode.values()).extracting(Enum::name)
			.containsExactly("IMMEDIATE", "END_OF_CYCLE");
		assertThat(MembershipTerminationRequestStatus.values()).extracting(Enum::name)
			.containsExactly("PENDING", "APPROVED", "EXECUTED", "REJECTED", "WITHDRAWN", "OBSOLETE");
	}

	@Test
	void databaseGeneratedTimestampsAreReadBackAfterInsert() throws NoSuchFieldException {
		assertThat(MembershipTerminationRequest.class.getDeclaredField("createdAt")
			.getAnnotation(Generated.class).event()).containsExactly(EventType.INSERT);
		assertThat(MembershipTerminationRequest.class.getDeclaredField("updatedAt")
			.getAnnotation(Generated.class).event()).containsExactly(EventType.INSERT);
	}
}
