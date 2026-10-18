package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class TrainingFeedbackOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us48OpenApiDocumentsCreateAndUpdateContracts() throws Exception {
		try (InputStream source = getClass().getResourceAsStream(
				"/openapi/us48-personal-coach-feedback.yaml")) {
			assertThat(source).isNotNull();
			String yaml = new String(source.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(yaml).doesNotContain("nullable: true");
			assertThat(yaml).contains("BR-FDB-03", "Group/Yoga Session Feedback is outside US48");
			assertThat(yaml).contains("TRAINING_FEEDBACK Benefit", "PLUS_MEMBERSHIP_REQUIRED");
			Map<String, Object> document = new Yaml().load(yaml);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			Map<String, Object> collection = (Map<String, Object>) paths.get(
					"/coach/members/{memberId}/feedback");
			Map<String, Object> item = (Map<String, Object>) paths.get(
					"/coach/members/{memberId}/feedback/{feedbackId}");
			assertThat(((Map<String, Object>) collection.get("post")).get("responses"))
					.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
					.containsKeys("201", "400", "401", "403", "404", "409");
			assertThat(((Map<String, Object>) item.get("put")).get("responses"))
					.asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
					.containsKeys("200", "400", "401", "403", "404", "409");
			Map<String, Object> schemas = (Map<String, Object>) ((Map<String, Object>) document.get("components"))
					.get("schemas");
			assertThat(schemas.keySet()).containsAll(List.of(
					"PersonalTrainingFeedbackRequest", "TrainingFeedbackResponse", "ProblemDetail"));
			assertThat((List<Object>) ((Map<String, Object>) schemas.get("PersonalTrainingFeedbackRequest"))
					.get("oneOf")).hasSize(2);
		}
	}
}
