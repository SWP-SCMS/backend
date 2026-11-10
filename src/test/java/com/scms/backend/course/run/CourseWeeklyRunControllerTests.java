package com.scms.backend.course.run;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.scms.backend.auth.ActiveAccountJwtValidator;
import com.scms.backend.common.error.SecurityProblemDetailHandler;
import com.scms.backend.config.SecurityConfiguration;
import java.util.UUID;
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

@WebMvcTest(controllers = CourseWeeklyRunController.class)
@AutoConfigureMockMvc
@Import({ SecurityConfiguration.class, SecurityProblemDetailHandler.class })
@TestPropertySource(properties = { "scms.cors.allowed-origins=http://localhost:5173", "scms.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes-long" })
class CourseWeeklyRunControllerTests {

	@Autowired
	MockMvc mvc;

	@MockitoBean
	CourseWeeklyRunService service;

	@MockitoBean
	ActiveAccountJwtValidator validator;

	private String schedule = """
		{"openingDate":"2030-12-30","weekdays":[1,3,5],"startTime":"18:00","endTime":"19:00","totalUnits":4}
		""";

	@Test
	void managerPreviewReturnsComputedDates() throws Exception {
		when(service.preview(any(), any())).thenAnswer(i -> CourseWeeklySchedule.generate(i.getArgument(1)));
		mvc.perform(post("/manager/course-runs/preview-schedule").with(jwt().jwt(j -> j.subject(UUID.randomUUID().toString())).authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))).contentType(MediaType.APPLICATION_JSON).content(schedule)).andExpect(status().isOk()).andExpect(jsonPath("$.sessions.length()").value(4)).andExpect(jsonPath("$.plannedEndAt").value("2031-01-06T12:00:00Z"));
	}

	@Test
	void memberCannotCreateOrPreview() throws Exception {
		for (var path : new String[] { "preview-schedule", "with-schedule" }) mvc.perform(post("/manager/course-runs/" + path).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MEMBER"))).contentType(MediaType.APPLICATION_JSON).content(schedule)).andExpect(status().isForbidden());
		verifyNoInteractions(service);
	}

	@Test
	void invalidNestedScheduleNeverReachesCreateService() throws Exception {
		String body = """
			{"runCode":"WEEKLY","courseId":"%s","primaryCoachId":"%s","roomId":"%s","tuitionVnd":1000,
			 "capacity":10,"minimumEnrollment":1,"registrationOpensAt":"2030-01-01T00:00:00Z",
			 "registrationClosesAt":"2030-12-01T00:00:00Z","schedule":%s}
			""".formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), schedule.replace("[1,3,5]", "[]"));
		mvc.perform(post("/manager/course-runs/with-schedule").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MANAGER"))).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		verifyNoInteractions(service);
	}
}
