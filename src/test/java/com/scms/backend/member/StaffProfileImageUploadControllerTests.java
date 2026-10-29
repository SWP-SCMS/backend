package com.scms.backend.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.GlobalExceptionHandler;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.identity.staffprofile.StaffProfileResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProfileImageUploadController.class,
		excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@AutoConfigureMockMvc
@Import({ com.scms.backend.config.SecurityConfiguration.class, SecurityProblemDetailHandler.class,
	GlobalExceptionHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class StaffProfileImageUploadControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean ProfileImageUploadService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@ParameterizedTest
	@MethodSource("staffRoles")
	void staffCanUploadOwnProfileImage(AccountRole role) throws Exception {
		UUID accountId = UUID.randomUUID();
		when(service.uploadForStaff(eq(accountId), any())).thenReturn(response(accountId, role));

		mockMvc.perform(multipart("/staff/me/profile/image")
			.file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2 }))
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString()).claim("role", role.name()))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role.name()))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accountId").value(accountId.toString()))
			.andExpect(jsonPath("$.role").value(role.name()))
			.andExpect(jsonPath("$.profileImageUrl").value("/profile-images/staff.png"));

		verify(service).uploadForStaff(eq(accountId), any());
	}

	@Test
	void memberCannotUploadThroughStaffEndpoint() throws Exception {
		mockMvc.perform(multipart("/staff/me/profile/image")
			.file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1 }))
			.with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()).claim("role", "MEMBER"))
				.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden());
	}

	@Test
	void uploadResponseIncludesNullImageFieldForLegacyStaff() throws Exception {
		UUID accountId = UUID.randomUUID();
		StaffProfileResponse response = new StaffProfileResponse(accountId, "Staff", "0901234567",
			"staff@example.com", LocalDate.of(1990, 1, 1), AccountRole.COACH, AccountStatus.ACTIVE,
			null, false, null, null, null);
		when(service.uploadForStaff(eq(accountId), any())).thenReturn(response);

		mockMvc.perform(multipart("/staff/me/profile/image")
			.file(new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1 }))
			.with(jwt().jwt(jwt -> jwt.subject(accountId.toString()).claim("role", "COACH"))
				.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.profileImageUrl").value(nullValue()));
	}

	private StaffProfileResponse response(UUID accountId, AccountRole role) {
		return new StaffProfileResponse(accountId, "Staff", "0901234567", "staff@example.com",
			LocalDate.of(1990, 1, 1), role, AccountStatus.ACTIVE, "/profile-images/staff.png", false,
			null, null, null);
	}

	private static Stream<Arguments> staffRoles() {
		return Stream.of(AccountRole.MANAGER, AccountRole.RECEPTIONIST, AccountRole.COACH)
			.map(Arguments::of);
	}
}
