package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseRefundOpenApiTests {
	@Test
	void contractExposesManagerReviewQueueDetailAndDecisions() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-refund-obligations.yaml")) {
			assertThat(input).isNotNull();
			var paths = new ObjectMapper(new YAMLFactory()).readTree(input).path("paths");
			assertThat(paths.has("/manager/course-refund-obligations")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/approve")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/reject")).isTrue();
		}
	}
}
