package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class ClassSessionAvailabilityOpenApiTests {

	@Test
	void documentsAvailableCoachEndpointAndResponse() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/us23-manager-scheduling-changes.yaml")) {
			assertThat(input).isNotNull();
			JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(input);
			JsonNode operation = document.at(
				"/paths/~1manager~1class-sessions~1{id}~1available-coaches/get");
			assertThat(operation.isMissingNode()).isFalse();
			assertThat(operation.at("/responses/200/content/application~1json/schema/items/$ref").asText())
				.isEqualTo("#/components/schemas/AvailableCoachResponse");
			JsonNode schema = document.at("/components/schemas/AvailableCoachResponse");
			assertThat(schema.path("required").toString())
				.contains("coachAccountId", "fullName", "phone");
			JsonNode discovery = document.at("/paths/~1manager~1coaches~1available/get");
			assertThat(discovery.isMissingNode()).isFalse();
			assertThat(discovery.at("/responses/200/content/application~1json/schema/items/$ref").asText())
				.isEqualTo("#/components/schemas/AvailableCoachResponse");
		}
	}
}
