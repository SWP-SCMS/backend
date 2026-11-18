package com.scms.backend.training.session;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import com.scms.backend.training.TrainingFeedback;
import com.scms.backend.training.TrainingResult;
import com.scms.backend.training.feedback.TrainingFeedbackRepository;
import com.scms.backend.training.result.TrainingResultRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MemberSessionTrainingController.class)
@Import({ MemberSessionTrainingService.class, SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class MemberSessionTrainingControllerTests {

	@Autowired MockMvc mvc;
	@MockitoBean TrainingResultRepository results;
	@MockitoBean TrainingFeedbackRepository feedback;
	@MockitoBean ActiveAccountJwtValidator activeAccountJwtValidator;

	final UUID member = UUID.randomUUID();
	final UUID session = UUID.randomUUID();
	final UUID coach = UUID.randomUUID();
	final Instant now = Instant.parse("2026-11-18T03:00:00Z");

	String path(boolean course) {
		return "/members/me/" + (course ? "course" : "class") + "-sessions/" + session + "/training-summary";
	}

	TrainingResult storedResult(boolean course) {
		var result = course ? TrainingResult.forCourseSession(UUID.randomUUID(), member, session, now, Map.of("notes", "Done"), coach, now)
				: TrainingResult.forClassSession(UUID.randomUUID(), member, session, now, Map.of("notes", "Done"), coach, now);
		if (course) when(results.findCourseSessionResult(session, member)).thenReturn(Optional.of(result));
		else when(results.findSessionByClassSessionIdAndMemberAccountId(session, member)).thenReturn(Optional.of(result));
		return result;
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void returnsBothFieldsForTheAuthenticatedMemberAndExactSession(boolean course) throws Exception {
		var result = storedResult(course);
		var comment = TrainingFeedback.forClassSessionResult(UUID.randomUUID(), member, result.getId(), "Line one\nLine two", coach, now);
		when(feedback.findCanonicalSessionFeedback(result.getId(), member)).thenReturn(Optional.of(comment));
		mvc.perform(get(path(course)).param("memberId", UUID.randomUUID().toString())
				.with(jwt().jwt(token -> token.subject(member.toString())).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.memberAccountId").value(member.toString()))
				.andExpect(jsonPath("$.result.content.notes").value("Done"))
				.andExpect(jsonPath("$.result." + (course ? "courseSessionId" : "classSessionId")).value(session.toString()))
				.andExpect(jsonPath("$.feedback.trainingResultId").value(result.getId().toString()))
				.andExpect(jsonPath("$.feedback.content").value("Line one\nLine two"));
		verify(feedback).findCanonicalSessionFeedback(result.getId(), member);
		verifyNoMoreInteractions(feedback);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void anotherMemberCannotReadTheStoredResultOrFeedback(boolean course) throws Exception {
		storedResult(course);
		UUID other = UUID.randomUUID();
		mvc.perform(get(path(course)).with(jwt().jwt(token -> token.subject(other.toString())).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
				.andExpect(status().isOk()).andExpect(content().json("{\"result\":null,\"feedback\":null}"));
		if (course) verify(results).findCourseSessionResult(session, other);
		else verify(results).findSessionByClassSessionIdAndMemberAccountId(session, other);
		verifyNoInteractions(feedback);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void resultRemainsVisibleBeforeFeedbackIsWritten(boolean course) throws Exception {
		storedResult(course);
		mvc.perform(get(path(course)).with(jwt().jwt(token -> token.subject(member.toString())).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.result.content.notes").value("Done"))
				.andExpect(jsonPath("$.feedback").doesNotExist());
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void rejectsAnonymousAndStaff(boolean course) throws Exception {
		mvc.perform(get(path(course))).andExpect(status().isUnauthorized());
		for (String role : new String[] { "COACH", "MANAGER", "RECEPTIONIST" }) {
			mvc.perform(get(path(course)).with(jwt().jwt(token -> token.subject(coach.toString())).authorities(new SimpleGrantedAuthority("ROLE_" + role))))
					.andExpect(status().isForbidden());
		}
		verifyNoInteractions(results, feedback);
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void validatesSessionIdentifiers(boolean course) throws Exception {
		mvc.perform(get(path(course).replace(session.toString(), "invalid"))
				.with(jwt().jwt(token -> token.subject(member.toString())).authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(results, feedback);
	}
}
