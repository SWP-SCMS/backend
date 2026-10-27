package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class RoomImpactAvailabilityOpenApiTests {

	@Test
	void documentsPerSessionAndBulkTargetRooms() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/us23-manager-scheduling-changes.yaml")) {
			assertThat(input).isNotNull();
			JsonNode document = new ObjectMapper(new YAMLFactory()).readTree(input);
			JsonNode preview = document.at("/components/schemas/RoomImpactPreviewResponse");
			assertThat(preview.path("required").toString()).contains("bulkAvailableTargetRooms");
			assertThat(preview.at("/properties/bulkAvailableTargetRooms/items/$ref").asText())
				.isEqualTo("#/components/schemas/RoomImpactTargetRoom");
			JsonNode session = document.at("/components/schemas/RoomImpactSession");
			assertThat(session.path("required").toString()).contains("availableTargetRooms");
			assertThat(session.at("/properties/availableTargetRooms/items/$ref").asText())
				.isEqualTo("#/components/schemas/RoomImpactTargetRoom");
			assertThat(document.at("/components/schemas/RoomImpactTargetRoom/required").toString())
				.contains("roomId", "roomName", "capacity");
		}
	}
}
