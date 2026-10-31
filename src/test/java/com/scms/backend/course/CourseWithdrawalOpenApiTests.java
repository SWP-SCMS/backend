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
	}
}
