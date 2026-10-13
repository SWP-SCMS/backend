package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class TrainingPlanOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us42OpenApiDocumentsCreateContract() throws Exception {
		try (InputStream source = getClass().getResourceAsStream(
				"/openapi/us42-personal-coach-create-training-plan.yaml")) {
			assertThat(source).isNotNull();
			String yaml = new String(source.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(yaml).doesNotContain("nullable: true");
			Map<String, Object> document = new Yaml().load(yaml);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			Map<String, Object> path = (Map<String, Object>) paths.get("/coach/members/{memberId}/training-plans");
			Map<String, Object> operation = (Map<String, Object>) path.get("post");
			Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
			assertThat(responses.keySet()).containsAll(List.of("201", "400", "401", "403", "404", "409"));
			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			assertThat(schemas).containsKeys("CreateTrainingPlanRequest", "TrainingPlanResponse", "ProblemDetail");
		}
	}
}
