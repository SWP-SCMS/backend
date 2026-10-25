package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class CoachAssignmentOpenApiTests {

	@Test
	@SuppressWarnings("unchecked")
	void us39OpenApiIsValidOpenApi31AndStrictlyAdheresToContract() {
		try (InputStream source = getClass().getResourceAsStream("/openapi/us39-coach-assignment.yaml")) {
			assertThat(source).isNotNull();
			byte[] bytes = source.readAllBytes();
			String rawYaml = new String(bytes, StandardCharsets.UTF_8);

			// OpenAPI 3.1 strictly forbids 'nullable: true'
			assertThat(rawYaml).doesNotContain("nullable: true");
			assertThat(rawYaml).doesNotContain("nullable:true");
			assertThat(rawYaml).contains("PERSONAL_COACHING Benefit", "PLUS_MEMBERSHIP_REQUIRED");

			Map<String, Object> document = new Yaml().load(new String(bytes, StandardCharsets.UTF_8));
			assertThat(document.get("openapi")).isEqualTo("3.1.0");

			Map<String, Object> paths = (Map<String, Object>) document.get("paths");
			assertThat(paths).containsKeys(
				"/manager/members/{memberId}/coach-assignment",
				"/manager/members/{memberId}/coach-assignment/impact",
				"/manager/members/{memberId}/coach-assignments",
				"/manager/members/{memberId}/coach-assignment/end"
			);

			Map<String, Object> components = (Map<String, Object>) document.get("components");
			assertThat(components).isNotNull();
			Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
			Map<String, Object> responses = (Map<String, Object>) components.get("responses");

			// 1. Verify Operations, Security and Responses
			assertOperationSecurityAndResponses(paths, "/manager/members/{memberId}/coach-assignment", "post",
					List.of("201", "400", "401", "403", "404", "409"));
			assertOperationSecurityAndResponses(paths, "/manager/members/{memberId}/coach-assignment", "get",
					List.of("200", "204", "401", "403", "404"));
			assertOperationSecurityAndResponses(paths, "/manager/members/{memberId}/coach-assignments", "get",
					List.of("200", "400", "401", "403", "404"));
			assertOperationSecurityAndResponses(paths, "/manager/members/{memberId}/coach-assignment/impact", "get",
					List.of("200", "401", "403", "404"));
			assertOperationSecurityAndResponses(paths, "/manager/members/{memberId}/coach-assignment/end", "patch",
					List.of("200", "400", "401", "403", "404", "409"));

			// 2. Verify Component Responses use application/problem+json and ProblemDetail
			for (String statusName : List.of("BadRequest", "Unauthorized", "Forbidden", "NotFound", "Conflict")) {
				Map<String, Object> resp = (Map<String, Object>) responses.get(statusName);
				assertThat(resp).as("Response " + statusName).isNotNull();
				Map<String, Object> content = (Map<String, Object>) resp.get("content");
				assertThat(content).containsKey("application/problem+json");
				Map<String, Object> mediaType = (Map<String, Object>) content.get("application/problem+json");
				Map<String, Object> schema = (Map<String, Object>) mediaType.get("schema");
				assertThat(schema.get("$ref")).isEqualTo("#/components/schemas/ProblemDetail");
			}

			// 3. Verify ProblemDetail schema and error codes
			Map<String, Object> problemDetail = (Map<String, Object>) schemas.get("ProblemDetail");
			assertThat((List<String>) problemDetail.get("required"))
					.contains("title", "status", "detail", "instance", "code");
			Map<String, Object> problemProperties = (Map<String, Object>) problemDetail.get("properties");
			Map<String, Object> codeProperty = (Map<String, Object>) problemProperties.get("code");
			List<String> errorCodes = (List<String>) codeProperty.get("enum");
			assertThat(errorCodes).contains(
				"VALIDATION_ERROR", "MALFORMED_REQUEST",
				"MEMBER_NOT_FOUND", "COACH_NOT_FOUND",
				"MEMBER_NOT_ACTIVE", "COACH_NOT_ACTIVE",
				"PLUS_MEMBERSHIP_REQUIRED", "COACH_ALREADY_ASSIGNED",
				"COACH_ASSIGNMENT_NOT_FOUND", "ACTOR_NOT_ACTIVE_MANAGER",
				"COACH_ASSIGNMENT_CONFLICT", "COACH_ASSIGNMENT_IMPACT_CONFIRMATION_REQUIRED",
				"COACH_ASSIGNMENT_IMPACT_CHANGED"
			);

			// 4. Verify CoachAssignmentResponse schema
			Map<String, Object> assignmentResp = (Map<String, Object>) schemas.get("CoachAssignmentResponse");
			assertThat((List<String>) assignmentResp.get("required")).contains(
				"id", "memberAccountId", "coachAccountId", "assignedByAccountId",
				"status", "startsAt", "createdAt"
			);
			Map<String, Object> respProps = (Map<String, Object>) assignmentResp.get("properties");
			assertThat(respProps).containsKeys(
				"id", "memberAccountId", "coachAccountId", "coachFullName", "coachPhone", "coachEmail",
				"assignedByAccountId", "status", "startsAt", "endsAt", "endReason", "createdAt",
				"processedImpact"
			);
			// Nullability: type must be list with string and null
			assertThat((List<String>) ((Map<String, Object>) respProps.get("coachFullName")).get("type"))
					.containsExactly("string", "null");
			assertThat((List<String>) ((Map<String, Object>) respProps.get("coachPhone")).get("type"))
					.containsExactly("string", "null");
			assertThat((List<String>) ((Map<String, Object>) respProps.get("coachEmail")).get("type"))
					.containsExactly("string", "null");
			assertThat((List<String>) ((Map<String, Object>) respProps.get("endsAt")).get("type"))
					.containsExactly("string", "null");
			assertThat((List<String>) ((Map<String, Object>) respProps.get("endReason")).get("type"))
					.containsExactly("string", "null");

			// 5. Verify CoachAssignmentPageResponse schema matches Java
			Map<String, Object> pageResp = (Map<String, Object>) schemas.get("CoachAssignmentPageResponse");
			assertThat((List<String>) pageResp.get("required")).containsExactlyInAnyOrder(
				"content", "page", "size", "totalElements", "totalPages"
			);
			Map<String, Object> pageProps = (Map<String, Object>) pageResp.get("properties");
			assertThat(pageProps).containsKey("content");

			// 6. Verify Request schemas
			Map<String, Object> assignReq = (Map<String, Object>) schemas.get("CoachAssignmentAssignRequest");
			assertThat((List<String>) assignReq.get("required")).containsExactly("coachAccountId");
			Map<String, Object> endReq = (Map<String, Object>) schemas.get("CoachAssignmentEndRequest");
			assertThat((List<String>) endReq.get("required")).containsExactly("reason");
			assertThat((Map<String, Object>) assignReq.get("properties")).containsKey("impactConfirmation");
			assertThat((Map<String, Object>) endReq.get("properties")).containsKey("impactConfirmation");
			assertThat(schemas).containsKeys("PtAssignmentImpactConfirmation", "PtAssignmentImpactItem",
				"PtAssignmentImpactResponse");
		}
		catch (Exception exception) {
			throw new AssertionError(exception);
		}
	}

	@SuppressWarnings("unchecked")
	private void assertOperationSecurityAndResponses(Map<String, Object> paths, String path, String method,
			List<String> expectedStatuses) {
		Map<String, Object> pathItem = (Map<String, Object>) paths.get(path);
		assertThat(pathItem).as("Path " + path).containsKey(method);
		Map<String, Object> operation = (Map<String, Object>) pathItem.get(method);

		// Security requirement
		List<Map<String, Object>> security = (List<Map<String, Object>>) operation.get("security");
		assertThat(security).as("Security on " + method + " " + path).isNotEmpty();
		assertThat(security.get(0)).containsKey("bearerAuth");

		// Responses
		Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
		assertThat(responses.keySet()).as("Responses on " + method + " " + path)
				.containsAll(expectedStatuses);
	}
}
