package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class MemberTrainingPlanOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us44OpenApiDocumentsOwnPlanVersionHistoryContract() throws Exception {
		try (InputStream source = getClass().getResourceAsStream(
				"/openapi/us44-member-view-training-plan.yaml")) {
			assertThat(source).isNotNull();
			String yaml = new String(source.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(yaml).doesNotContain("nullable: true");
			Map<String, Object> document = new Yaml().load(yaml);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			Map<String, Object> operation = (Map<String, Object>) ((Map<String, Object>) paths.get(
					"/members/me/training-plans")).get("get");
			Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
			assertThat(responses.keySet()).containsAll(List.of("200", "401", "403"));
			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			assertThat(schemas).containsKeys("MemberTrainingPlanResponse",
					"MemberTrainingPlanVersionResponse", "ProblemDetail");
		}
	}
}
