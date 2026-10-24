package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CoachLeaveExpiryOpenApiTests {

	@Test
	void documentsExpiredStatusAndApprovalConflict() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/mf-008-coach-leave.yaml")) {
			var document = new ObjectMapper(new YAMLFactory()).readTree(input);
			assertThat(document.at("/components/schemas/CoachLeaveStatus/enum").toString())
				.isEqualTo("[\"PENDING\",\"APPROVED\",\"REJECTED\",\"WITHDRAWN\",\"EXPIRED\"]");
			assertThat(document.at("/paths/~1manager~1coach-leave-requests~1{requestId}~1approve/patch/responses/409")
				.isMissingNode()).isFalse();
		}
	}
}
