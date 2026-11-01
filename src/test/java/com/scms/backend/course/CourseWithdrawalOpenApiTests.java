package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseWithdrawalOpenApiTests {
	@Test
	void contractExposesOwnedMemberReceptionAndManagerDecisionSurfaces() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-withdrawal.yaml")) {
			assertThat(input).isNotNull();
			var paths = new ObjectMapper(new YAMLFactory()).readTree(input).path("paths");
			assertThat(paths.has("/members/me/course-enrollments/{enrollmentId}/withdrawal-requests")).isTrue();
			assertThat(paths.has("/members/me/course-withdrawal-requests/{requestId}")).isTrue();
			assertThat(paths.has("/reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests")).isTrue();
			assertThat(paths.has("/manager/course-withdrawal-requests/{requestId}/approve")).isTrue();
			assertThat(paths.has("/manager/course-withdrawal-requests/{requestId}/reject")).isTrue();
		}
		try (var input = getClass().getResourceAsStream("/openapi/course-enrollment.yaml")) {
			var paths = new ObjectMapper(new YAMLFactory()).readTree(input).path("paths");
			assertThat(paths.has("/reception/members/{memberId}/course-enrollments")).isTrue();
			assertThat(paths.has("/reception/members/{memberId}/course-enrollments/{enrollmentId}")).isTrue();
		}
		try (var input = getClass().getResourceAsStream("/openapi/course-withdrawal.yaml")) {
			var schemas = new ObjectMapper(new YAMLFactory()).readTree(input).at("/components/schemas");
			assertThat(schemas.path("WithdrawalRequest").path("properties").fieldNames()).toIterable()
				.contains("memberCode", "courseCode", "runCode", "totalUnits", "remainingUnits", "estimatedRefundAmount");
		}
	}
}
