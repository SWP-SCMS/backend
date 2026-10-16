package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class MemberTrainingPlanOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us44OpenApiDocumentsPlanAndVersionHistory() throws Exception {
		try (InputStream source = getClass().getResourceAsStream("/openapi/us44-member-view-training-plan.yaml")) {
			assertThat(source).isNotNull();
			Map<String, Object> document = new Yaml().load(
					new String(source.readAllBytes(), StandardCharsets.UTF_8));
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			assertThat(paths).containsKey("/members/me/training-plans");
			Map<String, Object> schemas = (Map<String, Object>) ((Map<String, Object>) document.get("components"))
				.get("schemas");
			assertThat(schemas).containsKeys("MemberTrainingPlanResponse", "MemberTrainingPlanVersionResponse");
		}
	}
}
