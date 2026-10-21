package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class MembershipBenefitGroupOpenApiTests {

	@Test
	void staticContractsExposeBenefitGroupMetadata() throws Exception {
		for (String resource : List.of("us12-membership-offer-list.yaml", "us13-membership-offer-detail.yaml",
				"api-01-staff-membership-history.yaml")) {
			try (InputStream stream = getClass().getResourceAsStream("/openapi/" + resource)) {
				assertThat(stream).as(resource).isNotNull();
				JsonNode schema = new ObjectMapper(new YAMLFactory()).readTree(stream)
					.at("/components/schemas/MembershipBenefitResponse");
				assertThat(StreamSupport.stream(schema.path("required").spliterator(), false)
					.map(JsonNode::asText).toList())
					.contains("displayOrder", "groupCode", "groupDisplayName", "groupDisplayOrder");
				assertThat(schema.at("/properties/displayOrder/minimum").asInt()).isZero();
				assertThat(schema.at("/properties/groupCode/nullable").asBoolean()).isTrue();
				assertThat(schema.at("/properties/groupDisplayOrder/nullable").asBoolean()).isTrue();
			}
		}
	}
}
