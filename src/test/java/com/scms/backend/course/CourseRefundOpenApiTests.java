package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseRefundOpenApiTests {
	@Test
	void contractExposesReviewDisbursementVerificationAndMemberHistory() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-refund-obligations.yaml")) {
			assertThat(input).isNotNull();
			var document = new ObjectMapper(new YAMLFactory()).readTree(input);
			var paths = document.path("paths");
			assertThat(paths.has("/manager/course-refund-obligations")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/approve")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/reject")).isTrue();
			assertThat(paths.has("/reception/course-refund-obligations/{obligationId}/disburse")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/verify")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/fail")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/return")).isTrue();
			assertThat(paths.has("/members/me/course-refund-obligations")).isTrue();
			assertThat(paths.has("/members/me/course-refund-obligations/{obligationId}")).isTrue();
			var disburse = paths.path("/reception/course-refund-obligations/{obligationId}/disburse").path("post");
			assertThat(disburse.path("parameters").toString()).contains("IdempotencyKey");
			assertThat(document.at("/components/parameters/IdempotencyKey/name").asText())
				.isEqualTo("Idempotency-Key");
		}
	}
}
