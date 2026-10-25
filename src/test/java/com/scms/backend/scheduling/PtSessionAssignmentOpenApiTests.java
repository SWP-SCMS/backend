package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class PtSessionAssignmentOpenApiTests {

	@Test
	void documentsPtAssignmentConflictCollections() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/us23-manager-scheduling-changes.yaml")) {
			var document = new ObjectMapper(new YAMLFactory()).readTree(input);
			assertThat(document.at("/paths/~1manager~1class-sessions~1{id}~1assignment/patch").isMissingNode())
				.isFalse();
			assertThat(document.at("/components/schemas/SchedulingProblem/properties/affectedBookings/items/$ref")
				.asText()).isEqualTo("#/components/schemas/PtSessionAffectedBooking");
			assertThat(document.at("/components/schemas/SchedulingProblem/properties/affectedWaitlists/items/$ref")
				.asText()).isEqualTo("#/components/schemas/PtSessionAffectedWaitlist");
		}
	}
}
