package com.scms.backend.course.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseEnrollmentOpenApiTests {

	@Test
	void staticContractKeepsMemberAndCoachBoundariesExplicit() throws Exception {
		var source = getClass().getResourceAsStream("/openapi/course-enrollment.yaml");
		assertThat(source).isNotNull();
		JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(source);
		for (JsonNode reference : document.findValues("$ref")) {
			if (reference.asText().startsWith("#/"))
				assertThat(document.at(reference.asText().substring(1)).isMissingNode()).as(reference.asText()).isFalse();
		}
		assertThat(document.path("paths").properties().stream().map(java.util.Map.Entry::getKey).toList()).containsExactlyInAnyOrder("/members/me/course-enrollments", "/members/me/course-schedule-changes", "/reception/members/{memberId}/course-schedule-changes", "/reception/members/{memberId}/course-schedule-changes/{responseId}/response", "/reception/members/{memberId}/course-enrollments", "/reception/members/{memberId}/course-enrollments/{enrollmentId}", "/members/me/course-schedule-changes/{responseId}/response", "/coach/course-sessions/{sessionId}/roster", "/coach/course-sessions/{sessionId}/roster/{enrollmentSessionId}/attendance");
		assertThat(document.at("/components/schemas/AttendanceUpdate/properties/status/enum").toString()).isEqualTo("[\"PRESENT\",\"ABSENT\"]");
		assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();
		assertThat(document.at("/components/schemas/RosterEntry/properties/memberId").isMissingNode()).isTrue();
		assertThat(document.at("/components/schemas/MemberEnrollment/properties/sessions/items/$ref").asText()).isEqualTo("#/components/schemas/MemberSession");
		assertThat(document.at("/components/schemas/MemberEnrollment/properties/courseRunStatus").isMissingNode()).isFalse();
		assertThat(document.at("/components/schemas/MemberEnrollment/properties/payment/$ref").asText()).isEqualTo("#/components/schemas/MemberPaymentSummary");
		assertThat(document.at("/components/schemas/MemberSession/properties").properties().stream().map(java.util.Map.Entry::getKey).toList()).contains("sessionStatus", "isMakeup", "originalSessionId", "replacementSessionId");
		assertThat(document.at("/components/schemas/MemberSession/required").toString()).contains("sessionStatus", "isMakeup", "originalSessionId", "replacementSessionId");
		assertThat(document.at("/components/schemas/ScheduleChange/properties").fieldNames()).toIterable().contains("changeEventId", "cause", "withdrawalRequestId", "withdrawalEligible", "withdrawalReason", "refundBasisVnd", "refundAmountVnd");
		assertThat(document.at("/components/schemas/ScheduleChange/properties/cause/enum").toString()).contains("START_DELAY_OVER_7_DAYS", "MEMBER_SCHEDULE_CONFLICT", "REDUCED_UNIT_COUNT");
		assertThat(document.at("/components/schemas/ReceptionCourseEnrollment/properties/estimatedRefundAmount/description").asText()).contains("Null when withdrawalEligible is false");
		assertThat(document.at("/paths/~1coach~1course-sessions~1{sessionId}~1roster~1{enrollmentSessionId}~1attendance/patch/responses/409/$ref").asText()).isEqualTo("#/components/responses/Conflict");
	}
}
