package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseTransferOpenApiTests {
	@Test
	void contractExposesManagerDecisionAndMemberAcceptanceOnly() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-transfer.yaml")) {
			assertThat(input).isNotNull();
			var paths = new ObjectMapper(new YAMLFactory()).readTree(input).path("paths");
			assertThat(paths.has("/manager/course-transfer-requests")).isTrue();
			assertThat(paths.has("/manager/course-transfer-requests/{requestId}/approve")).isTrue();
			assertThat(paths.has("/manager/course-transfer-requests/{requestId}/reject")).isTrue();
			assertThat(paths.has("/members/me/course-transfer-offers")).isTrue();
			assertThat(paths.has("/members/me/course-transfer-offers/{offerId}/accept")).isTrue();
			assertThat(paths.at("/~1members~1me~1course-transfer-offers~1{offerId}~1accept/post/parameters/1/name").asText())
				.isEqualTo("Idempotency-Key");
		}
	}
}
