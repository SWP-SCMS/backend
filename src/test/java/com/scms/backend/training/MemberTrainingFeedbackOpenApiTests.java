package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class MemberTrainingFeedbackOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us50OpenApiDocumentsOwnHistoryContract() throws Exception {
		try (InputStream source = getClass().getResourceAsStream(
				"/openapi/us50-member-feedback-history.yaml")) {
			assertThat(source).isNotNull();
			String yaml = new String(source.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(yaml).contains("Historical Feedback remains visible", "type: [\"string\", \"null\"]");
			Map<String, Object> document = new Yaml().load(yaml);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			Map<String, Object> operation = (Map<String, Object>) ((Map<String, Object>) paths.get(
				"/members/me/feedback")).get("get");
			assertThat(operation.get("responses"))
				.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
				.containsKeys("200", "401", "403");
			Map<String, Object> schemas = (Map<String, Object>) ((Map<String, Object>) document.get("components"))
				.get("schemas");
			assertThat(schemas).containsKeys("MemberTrainingFeedbackResponse", "ProblemDetail");
			Map<String, Object> response = (Map<String, Object>) schemas.get("MemberTrainingFeedbackResponse");
			assertThat((Map<String, Object>) response.get("properties"))
				.containsKeys("coachName", "sessionName", "sessionStartTime", "className");
		}
	}
}
