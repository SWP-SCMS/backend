package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseManagerOpenApiTests {

	@Test
	void staticContractDocumentsManagerCourseOperationsAndSecurity() throws Exception {
		InputStream source = getClass().getResourceAsStream("/openapi/course-manager.yaml");
		assertThat(source).as("course Manager OpenAPI contract").isNotNull();
		JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(source);
		assertThat(document.path("openapi").asText()).isEqualTo("3.1.0");
		assertThat(document.path("paths").properties().stream().map(java.util.Map.Entry::getKey).toList()).contains(
			"/manager/courses", "/manager/courses/{courseId}",
			"/manager/courses/{courseId}/publish", "/manager/courses/{courseId}/deactivate",
			"/manager/courses/{courseId}/archive", "/manager/course-runs",
			"/manager/course-runs/{runId}", "/manager/course-runs/{runId}/open-registration",
			"/manager/course-runs/{runId}/cancel", "/manager/course-runs/{runId}/sessions",
			"/manager/course-runs/{runId}/sessions/{sessionId}");
		assertThat(document.at("/security/0/bearerAuth").isArray()).isTrue();
		assertThat(document.at("/components/schemas/CourseStatus/enum").toString())
			.isEqualTo("[\"DRAFT\",\"PUBLISHED\",\"INACTIVE\",\"ARCHIVED\"]");
		assertThat(document.at("/components/schemas/CourseRunStatus/enum").toString())
			.contains("OPEN_FOR_REGISTRATION", "SETTLEMENT_PENDING", "CANCELLED");
		assertThat(document.at("/components/schemas/CourseCreateRequest/required").toString())
			.contains("code", "name", "disciplineId");
		assertThat(document.at("/components/schemas/CourseCreateRequest/properties/code/maxLength").asInt())
			.isEqualTo(50);
		assertThat(document.at("/components/schemas/CourseCreateRequest/properties/description/maxLength").asInt())
			.isEqualTo(2000);
		assertThat(document.at("/components/schemas/CourseCreateRequest/properties/imageUrl/maxLength").asInt())
			.isEqualTo(500);
		assertThat(document.at("/components/schemas/CoursePatchRequest/properties/description/maxLength").asInt())
			.isEqualTo(2000);
		assertThat(document.at("/components/schemas/CoursePatchRequest/properties/imageUrl/maxLength").asInt())
			.isEqualTo(500);
		assertThat(document.at("/paths/~1manager~1courses/post/responses/400/$ref").asText())
			.isEqualTo("#/components/responses/BadRequest");
		assertThat(document.at("/components/schemas/ApiProblem/required").toString())
			.contains("title", "status", "detail", "instance", "code");
		assertThat(document.at("/components/schemas/ApiProblem/properties/code/type").asText()).isEqualTo("string");
		assertThat(document.at("/components/responses/Unauthorized/description").asText())
			.contains("UNAUTHENTICATED", "INVALID_TOKEN");
		assertThat(document.at("/components/responses/Forbidden/description").asText()).contains("FORBIDDEN");
	}
}
