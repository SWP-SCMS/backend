package com.scms.backend.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.GlobalExceptionHandler;
import com.scms.backend.common.error.SecurityProblemDetailHandler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

@WebMvcTest(controllers = ProfileImageUploadController.class,
		excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc
@Import({ com.scms.backend.config.SecurityConfiguration.class, SecurityProblemDetailHandler.class,
		GlobalExceptionHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class ProfileImageUploadControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfileImageUploadService service;

	@MockitoBean
	private ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void memberCanUploadMultipartImageAndReceivesUpdatedProfile() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(service.uploadForMember(eq(accountId), any())).thenReturn(response(accountId));

		mockMvc.perform(multipart("/members/me/profile/image")
			.file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2 }))
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString()).claim("role", "MEMBER"))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(accountId.toString()))
			.andExpect(jsonPath("$.profileImageUrl").value("/profile-images/new.png"));

		verify(service).uploadForMember(eq(accountId), any());
	}

	@Test
	void memberRoleCannotUploadThroughReceptionEndpoint() throws Exception {
		mockMvc.perform(multipart("/reception/members/MB-123/profile/image")
			.file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1 }))
			.with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "MEMBER"))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden());
}

	@Test
	void receptionistCanUploadForMember() throws Exception {
		UUID receptionistId = UUID.randomUUID();
		when(service.uploadForReceptionist(eq(receptionistId), eq("MB-123"), any()))
			.thenReturn(response(UUID.randomUUID()));

		mockMvc.perform(multipart("/reception/members/MB-123/profile/image")
			.file(new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[] { 1, 2 }))
			.with(jwt().jwt(jwt -> jwt.subject(receptionistId.toString()).claim("role", "RECEPTIONIST"))
				.authorities(new SimpleGrantedAuthority("ROLE_RECEPTIONIST"))))
			.andExpect(status().isOk());

		verify(service).uploadForReceptionist(eq(receptionistId), eq("MB-123"), any());
	}

	@Test
	void missingFilePartIsRejected() throws Exception {
		UUID accountId = UUID.randomUUID();

		mockMvc.perform(multipart("/members/me/profile/image")
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString()).claim("role", "MEMBER"))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isBadRequest());
	}

	private MemberProfileResponse response(UUID accountId) {
		return new MemberProfileResponse(accountId, "MB-123", "Member", "0901234567", "member@example.com",
			LocalDate.of(2000, 1, 1), AccountRole.MEMBER, AccountStatus.ACTIVE, "/profile-images/new.png", null,
			null, null);
	}
}
