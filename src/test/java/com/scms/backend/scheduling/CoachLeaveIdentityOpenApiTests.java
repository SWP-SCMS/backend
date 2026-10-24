package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CoachLeaveIdentityOpenApiTests {

	@Test
	void coachLeaveResponseDocumentsCoachIdentity() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/mf-008-coach-leave.yaml")) {
			assertThat(input).isNotNull();
			JsonNode response = new ObjectMapper(new YAMLFactory()).readTree(input)
				.at("/components/schemas/CoachLeaveResponse");
			assertThat(response.path("required").toString())
				.contains("coachId", "coachFullName", "coachPhone");
			assertThat(response.path("properties").has("coachFullName")).isTrue();
			assertThat(response.path("properties").has("coachPhone")).isTrue();
		}
	}
}
