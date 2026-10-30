package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseCatalogOpenApiTests {

	@Test
	void staticContractDocumentsOnlyThePublicReadCatalog() throws Exception {
		InputStream source = getClass().getResourceAsStream("/openapi/course-catalog.yaml");
		assertThat(source).as("course catalog OpenAPI contract").isNotNull();
		JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(source);

		assertThat(document.path("openapi").asText()).isEqualTo("3.1.0");
		assertThat(document.path("paths").properties().stream().map(java.util.Map.Entry::getKey).toList())
			.containsExactlyInAnyOrder("/courses", "/courses/{courseId}", "/course-runs", "/course-runs/{runId}");
		for (String path : Set.of("~1courses", "~1courses~1{courseId}", "~1course-runs", "~1course-runs~1{runId}")) {
			assertThat(document.at("/paths/" + path + "/get/security").isArray()).isTrue();
			assertThat(document.at("/paths/" + path + "/get/security").isEmpty()).isTrue();
		}
		assertThat(document.at("/components/schemas/RegistrationStatus/enum").toString())
			.isEqualTo("[\"UPCOMING\",\"OPEN\",\"CLOSED\"]");
		assertThat(document.at("/components/schemas/CatalogCourseRun/properties/status/enum").toString())
			.isEqualTo("[\"OPEN_FOR_REGISTRATION\"]");
		assertThat(document.at("/components/schemas/CatalogCourseRunDetail/allOf/1/properties/sessions/items/$ref").asText())
			.isEqualTo("#/components/schemas/CatalogCourseSession");
		assertThat(document.at("/components/schemas/CatalogCourseRun/properties/capacityRemaining").isMissingNode())
			.isTrue();
		assertThat(document.at("/components/schemas/CatalogCourseSession/properties/startAt/format").asText())
			.isEqualTo("date-time");
		assertThat(document.at("/components/schemas/CatalogProblem/required").toString())
			.contains("title", "status", "detail", "instance", "code");
	}
}
