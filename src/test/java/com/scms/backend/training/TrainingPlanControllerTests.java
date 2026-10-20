package com.scms.backend.training;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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

@WebMvcTest(controllers = TrainingPlanController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = {
	"scms.cors.allowed-origins=http://localhost:5173",
	"scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long"
})
class TrainingPlanControllerTests {

	@Autowired MockMvc mockMvc;
	@MockitoBean TrainingPlanService service;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	private static final UUID COACH_ID = UUID.randomUUID();
	private static final UUID MEMBER_ID = UUID.randomUUID();

	@Test
	void listReturnsPaginatedCurrentVersionsForCoach() throws Exception {
		TrainingPlanResponse plan = new TrainingPlanResponse(UUID.randomUUID(), MEMBER_ID, UUID.randomUUID(),
				"Strength foundation", TrainingPlanStatus.ACTIVE, COACH_ID, Instant.parse("2026-10-13T06:00:00Z"),
				Instant.parse("2026-10-13T07:00:00Z"), UUID.randomUUID(), 2,
				Instant.parse("2026-10-13T07:00:00Z"), 1, Map.of("goal", "Build strength"));
		when(service.list(eq(COACH_ID), eq(MEMBER_ID), any()))
				.thenReturn(new TrainingPlanPageResponse(List.of(plan), 0, 20, 1, 1));

		mockMvc.perform(get("/coach/members/{memberId}/training-plans", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].versionNumber").value(2))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void createReturns201ForCoach() throws Exception {
		TrainingPlanResponse response = new TrainingPlanResponse(UUID.randomUUID(), MEMBER_ID, UUID.randomUUID(),
				"Strength foundation", TrainingPlanStatus.ACTIVE, COACH_ID, Instant.parse("2026-10-13T06:00:00Z"),
				Instant.parse("2026-10-13T06:00:00Z"), UUID.randomUUID(), 1,
				Instant.parse("2026-10-13T06:00:00Z"), 1, Map.of("goal", "Build strength"));
		when(service.create(eq(COACH_ID), eq(MEMBER_ID), any())).thenReturn(response);

		mockMvc.perform(post("/coach/members/{memberId}/training-plans", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"title":"Strength foundation","content":{"goal":"Build strength"}}
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberAccountId").value(MEMBER_ID.toString()))
				.andExpect(jsonPath("$.versionNumber").value(1))
				.andExpect(jsonPath("$.content.goal").value("Build strength"));
	}

	@Test
	void createReturns400ForBlankTitle() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/training-plans", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\" \",\"content\":{}}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void createReturns401WhenUnauthenticated() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/training-plans", MEMBER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Plan\",\"content\":{}}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void createReturns403ForMemberRole() throws Exception {
		mockMvc.perform(post("/coach/members/{memberId}/training-plans", MEMBER_ID)
				.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Plan\",\"content\":{}}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateReturns200WithNextVersionForCoach() throws Exception {
		UUID planId = UUID.randomUUID();
		TrainingPlanResponse response = new TrainingPlanResponse(planId, MEMBER_ID, UUID.randomUUID(),
				"Progressive strength", TrainingPlanStatus.ACTIVE, COACH_ID,
				Instant.parse("2026-10-13T06:00:00Z"), Instant.parse("2026-10-13T07:00:00Z"), UUID.randomUUID(), 2,
				Instant.parse("2026-10-13T07:00:00Z"), 1,
				Map.of("goal", "Progressive overload"));
		when(service.update(eq(COACH_ID), eq(MEMBER_ID), eq(planId), any())).thenReturn(response);

		mockMvc.perform(put("/coach/members/{memberId}/training-plans/{planId}", MEMBER_ID, planId)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Progressive strength\",\"content\":{\"goal\":\"Progressive overload\"},\"expectedVersionNumber\":1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(planId.toString()))
				.andExpect(jsonPath("$.versionNumber").value(2));
	}

	@Test
	void updateReturns403ForMemberRole() throws Exception {
		mockMvc.perform(put("/coach/members/{memberId}/training-plans/{planId}", MEMBER_ID, UUID.randomUUID())
				.with(jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_MEMBER")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Updated\",\"content\":{}}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void versionsReturnsCoachVisibleHistoryNewestFirst() throws Exception {
		UUID planId = UUID.randomUUID();
		when(service.versions(COACH_ID, MEMBER_ID, planId)).thenReturn(List.of(
				new TrainingPlanVersionResponse(UUID.randomUUID(), 2, Map.of("goal", "B"), 1, COACH_ID,
						Instant.parse("2026-10-13T07:00:00Z")),
				new TrainingPlanVersionResponse(UUID.randomUUID(), 1, Map.of("goal", "A"), 1, COACH_ID,
						Instant.parse("2026-10-13T06:00:00Z"))));

		mockMvc.perform(get("/coach/members/{memberId}/training-plans/{planId}/versions", MEMBER_ID, planId)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].versionNumber").value(2))
				.andExpect(jsonPath("$[1].versionNumber").value(1));
	}

	@Test
	void archiveReturnsArchivedPlan() throws Exception {
		UUID planId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-10-13T06:00:00Z");
		Instant archivedAt = Instant.parse("2026-10-13T08:00:00Z");
		when(service.archive(COACH_ID, MEMBER_ID, planId)).thenReturn(new TrainingPlanResponse(planId, MEMBER_ID,
				UUID.randomUUID(), "Finished", TrainingPlanStatus.ARCHIVED, COACH_ID, createdAt, archivedAt,
				UUID.randomUUID(), 2, Instant.parse("2026-10-13T07:00:00Z"), 1, Map.of("goal", "Done")));

		mockMvc.perform(post("/coach/members/{memberId}/training-plans/{planId}/archive", MEMBER_ID, planId)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ARCHIVED"))
				.andExpect(jsonPath("$.updatedAt").value(archivedAt.toString()));
	}

	@Test
	void staleUpdateReturnsConflictWithCurrentVersionNumber() throws Exception {
		UUID planId = UUID.randomUUID();
		doThrow(TrainingPlanException.versionConflict(4)).when(service)
				.update(eq(COACH_ID), eq(MEMBER_ID), eq(planId), any());

		mockMvc.perform(put("/coach/members/{memberId}/training-plans/{planId}", MEMBER_ID, planId)
				.with(jwt().jwt(builder -> builder.subject(COACH_ID.toString()))
						.authorities(new SimpleGrantedAuthority("ROLE_COACH")))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Stale\",\"content\":{\"goal\":\"Old\"},\"expectedVersionNumber\":3}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_VERSION_CONFLICT"))
				.andExpect(jsonPath("$.currentVersionNumber").value(4));
	}
}
