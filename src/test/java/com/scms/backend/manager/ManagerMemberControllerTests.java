package com.scms.backend.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ManagerMemberController.class)
@AutoConfigureMockMvc
class ManagerMemberControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ManagerMemberService service;

	private final UUID managerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();

	@Test
	void exposesListDetailAndPatchRoutes() throws Exception {
		MemberAccountResponse member = response();
		when(service.search(eq(managerId), eq("member"), eq(AccountStatus.ACTIVE), any(Pageable.class)))
			.thenReturn(new MemberAccountPageResponse(List.of(member), 0, 20, 1, 1));
		when(service.get(managerId, memberId)).thenReturn(member);
		when(service.update(eq(managerId), eq(memberId), any())).thenReturn(member);

		mockMvc.perform(get("/api/v1/manager/members")
			.contextPath("/api/v1").with(managerJwt())
			.queryParam("query", "member").queryParam("status", "ACTIVE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].accountId").value(memberId.toString()));

		mockMvc.perform(get("/api/v1/manager/members/{id}", memberId)
			.contextPath("/api/v1").with(managerJwt()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(memberId.toString()));

		mockMvc.perform(patch("/api/v1/manager/members/{id}", memberId)
			.contextPath("/api/v1").with(managerJwt())
			.contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"Updated Member\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.fullName").value("Member One"));
	}

	@Test
	void listRejectsInvalidPagination() throws Exception {
		mockMvc.perform(get("/api/v1/manager/members")
			.contextPath("/api/v1").with(managerJwt()).queryParam("size", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private org.springframework.test.web.servlet.request.RequestPostProcessor managerJwt() {
		return jwt().jwt(builder -> builder.subject(managerId.toString()).claim("role", "MANAGER"));
	}

	private MemberAccountResponse response() {
		return new MemberAccountResponse(memberId, "MB-100001", "Member One", "0900000000",
			"member@example.com", LocalDate.of(2000, 1, 1), AccountRole.MEMBER, AccountStatus.ACTIVE,
			null, null, null, null);
	}
}
