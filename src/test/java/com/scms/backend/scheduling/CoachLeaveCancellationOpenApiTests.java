package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CoachLeaveCancellationOpenApiTests {

	@Test
	void documentsCancellationEndpointAndStatus() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/mf-008-coach-leave.yaml")) {
			assertThat(input).isNotNull();
			JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(input);
			JsonNode operation = document.at(
				"/paths/~1coach~1leave-requests~1{requestId}~1cancel/patch");
			assertThat(operation.isMissingNode()).isFalse();
			assertThat(operation.at("/responses/200/content/application~1json/schema/$ref").asText())
				.isEqualTo("#/components/schemas/CoachLeaveResponse");
			assertThat(operation.path("responses").has("409")).isTrue();
			assertThat(document.at("/components/schemas/CoachLeaveStatus/enum").toString())
				.isEqualTo("[\"PENDING\",\"APPROVED\",\"REJECTED\",\"WITHDRAWN\",\"EXPIRED\",\"CANCELLED\"]");
		}
	}
}
