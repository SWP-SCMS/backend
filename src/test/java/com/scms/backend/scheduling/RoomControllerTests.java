package com.scms.backend.scheduling;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RoomController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class RoomControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean RoomService roomService;
	@MockitoBean RoomSuspensionService suspensionService;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	@Test
	void managerExtendsSuspensionWithExpectedEndTimeContract() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suspensionId = UUID.randomUUID();
		Instant startTime = Instant.parse("2026-10-01T10:00:00Z");
		Instant expectedEndTime = Instant.parse("2026-10-31T10:00:00Z");
		Instant endTime = Instant.parse("2026-11-30T10:00:00Z");
		RoomSuspensionExtensionRequest request = new RoomSuspensionExtensionRequest(expectedEndTime, endTime,
			"Gia han bao tri", List.of());
		when(suspensionService.extend(managerId, roomId, suspensionId, request)).thenReturn(
			new RoomSuspensionResponse(suspensionId, roomId, startTime, endTime, "Gia han bao tri", managerId));

		mockMvc.perform(patch("/api/v1/manager/rooms/{roomId}/suspensions/{suspensionId}", roomId, suspensionId)
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "expectedEndTime": "2026-10-31T10:00:00Z",
					  "endTime": "2026-11-30T10:00:00Z",
					  "reason": "Gia han bao tri",
					  "resolutions": []
					}
					""")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(suspensionId.toString()))
			.andExpect(jsonPath("$.endTime").value(endTime.toString()))
			.andExpect(jsonPath("$.reason").value("Gia han bao tri"));

		verify(suspensionService).extend(managerId, roomId, suspensionId, request);
	}

	@Test
	void nonManagerCannotExtendSuspension() throws Exception {
		mockMvc.perform(patch("/api/v1/manager/rooms/{roomId}/suspensions/{suspensionId}",
				UUID.randomUUID(), UUID.randomUUID())
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")
				.with(jwt().jwt(token -> token.subject(UUID.randomUUID().toString()).claim("role", "MEMBER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
			.andExpect(status().isForbidden());

		verifyNoInteractions(suspensionService);
	}

	@Test
	void staleExpectedEndTimeReturnsRoomSuspensionChangedConflict() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suspensionId = UUID.randomUUID();
		Instant expectedEndTime = Instant.parse("2026-10-31T10:00:00Z");
		Instant endTime = Instant.parse("2026-11-30T10:00:00Z");
		RoomSuspensionExtensionRequest request = new RoomSuspensionExtensionRequest(expectedEndTime, endTime,
			"Gia han bao tri", List.of());
		when(suspensionService.extend(managerId, roomId, suspensionId, request))
			.thenThrow(RoomException.suspensionChanged());

		mockMvc.perform(patch("/api/v1/manager/rooms/{roomId}/suspensions/{suspensionId}", roomId, suspensionId)
				.contextPath("/api/v1")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"expectedEndTime":"2026-10-31T10:00:00Z","endTime":"2026-11-30T10:00:00Z",
					 "reason":"Gia han bao tri","resolutions":[]}
					""")
				.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
					.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ROOM_SUSPENSION_CHANGED"));
	}

	@Test
	void rejectsOmittedNullOrBlankRequiredExtensionFields() throws Exception {
		UUID managerId = UUID.randomUUID();
		UUID roomId = UUID.randomUUID();
		UUID suspensionId = UUID.randomUUID();
		List<Map.Entry<String, String>> invalidBodies = List.of(
			Map.entry("reason", """
				{"expectedEndTime":"2026-10-31T10:00:00Z","endTime":"2026-11-30T10:00:00Z",
				 "resolutions":[]}
				"""),
			Map.entry("reason", """
				{"expectedEndTime":"2026-10-31T10:00:00Z","endTime":"2026-11-30T10:00:00Z",
				 "reason":null,"resolutions":[]}
				"""),
			Map.entry("reason", """
				{"expectedEndTime":"2026-10-31T10:00:00Z","endTime":"2026-11-30T10:00:00Z",
				 "reason":"   ","resolutions":[]}
				"""),
			Map.entry("resolutions", """
				{"expectedEndTime":"2026-10-31T10:00:00Z","endTime":"2026-11-30T10:00:00Z",
				 "reason":"Gia han bao tri"}
				"""),
			Map.entry("resolutions", """
				{"expectedEndTime":"2026-10-31T10:00:00Z","endTime":"2026-11-30T10:00:00Z",
				 "reason":"Gia han bao tri","resolutions":null}
				"""));

		for (Map.Entry<String, String> invalid : invalidBodies) {
			mockMvc.perform(patch("/api/v1/manager/rooms/{roomId}/suspensions/{suspensionId}", roomId,
					suspensionId)
					.contextPath("/api/v1")
					.contentType(MediaType.APPLICATION_JSON)
					.content(invalid.getValue())
					.with(jwt().jwt(token -> token.subject(managerId.toString()).claim("role", "MANAGER"))
						.authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors." + invalid.getKey()).isArray());
		}

		verifyNoInteractions(suspensionService);
	}
}
