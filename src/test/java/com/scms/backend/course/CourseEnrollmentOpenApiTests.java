package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseEnrollmentOpenApiTests {
	@Test void staticContractKeepsMemberAndCoachBoundariesExplicit() throws Exception {
		var source = getClass().getResourceAsStream("/openapi/course-enrollment.yaml");
		assertThat(source).isNotNull();
		JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(source);
		assertThat(document.path("paths").properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("/members/me/course-enrollments", "/members/me/course-schedule-changes",
				"/members/me/course-schedule-changes/{responseId}/response", "/coach/course-sessions/{sessionId}/roster",
				"/coach/course-sessions/{sessionId}/roster/{enrollmentSessionId}/attendance");
		assertThat(document.at("/components/schemas/AttendanceUpdate/properties/status/enum").toString())
			.isEqualTo("[\"PRESENT\",\"ABSENT\"]");
		assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();
		assertThat(document.at("/components/schemas/RosterEntry/properties/memberId").isMissingNode()).isTrue();
		assertThat(document.at("/components/schemas/MemberEnrollment/properties/sessions/items/$ref").asText())
			.isEqualTo("#/components/schemas/MemberSession");
		assertThat(document.at("/paths/~1coach~1course-sessions~1{sessionId}~1roster~1{enrollmentSessionId}~1attendance/patch/responses/409/$ref").asText())
			.isEqualTo("#/components/responses/Conflict");
	}
}
