package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class MemberCoachAssignmentOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void memberActiveAssignmentContractDocumentsSuccessAndNoContent() throws Exception {
		try (InputStream source = getClass().getResourceAsStream(
				"/openapi/member-active-coach-assignment.yaml")) {
			assertThat(source).isNotNull();
			Map<String, Object> document = new Yaml().load(source);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");
			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			Map<String, Object> path = (Map<String, Object>) paths.get("/members/me/coach-assignment");
			Map<String, Object> get = (Map<String, Object>) path.get("get");
			Map<String, Object> responses = (Map<String, Object>) get.get("responses");
			assertThat(responses).containsKeys("200", "204", "401", "403");

			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			Map<String, Object> response = (Map<String, Object>) schemas.get("MemberCoachAssignmentResponse");
			Map<String, Object> properties = (Map<String, Object>) response.get("properties");
			assertThat(properties).containsKeys("assignmentId", "coachAccountId", "coachFullName",
					"coachPhone", "coachEmail", "status", "startsAt");
		}
	}
}
