package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class AssignmentDiscoveryOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void staticContractDocumentsBothManagerDiscoveryReads() throws Exception {
		try (InputStream source = getClass().getResourceAsStream(
				"/openapi/api-02-api-03-assignment-discovery.yaml")) {
			assertThat(source).isNotNull();
			Map<String, Object> document = new Yaml().load(source);
			assertThat(document.get("openapi")).isEqualTo("3.1.0");

			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			assertThat(paths).containsOnlyKeys(
				"/manager/coach-assignments/candidates",
				"/manager/coaches/assignment-workloads");
			for (String path : paths.keySet()) {
				Map<String, Object> operation = (Map<String, Object>) ((Map<String, Object>) paths.get(path)).get("get");
				assertThat((List<Map<String, Object>>) operation.get("security")).singleElement()
					.satisfies(requirement -> assertThat(requirement).containsKey("bearerAuth"));
				assertThat((Map<String, Object>) operation.get("responses"))
					.containsKeys("200", "400", "401", "403");
			}

			Map<String, Object> components = (Map<String, Object>) document.get("components");
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			Map<String, Object> candidate = (Map<String, Object>) schemas.get("AssignmentCandidateResponse");
			assertThat((Map<String, Object>) candidate.get("properties")).containsOnlyKeys(
				"memberAccountId", "memberCode", "fullName", "phone", "email",
				"membershipEndsAt", "assignmentStatus");

			Map<String, Object> workload = (Map<String, Object>) schemas.get("CoachAssignmentWorkloadResponse");
			assertThat((Map<String, Object>) workload.get("properties")).containsOnlyKeys(
				"coachAccountId", "fullName", "phone", "email", "activeMemberCount",
				"maximumMemberCount", "availableForAssignment");
		}
	}
}
