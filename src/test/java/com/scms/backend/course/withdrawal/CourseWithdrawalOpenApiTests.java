package com.scms.backend.course.withdrawal;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseWithdrawalOpenApiTests {

	@Test
	void contractSeparatesNullableRefundProgressFromWithdrawalDecision() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-withdrawal.yaml")) {
			var properties = new ObjectMapper(new YAMLFactory()).readTree(input).at("/components/schemas/WithdrawalRequest/properties");
			assertThat(properties.path("refundObligationId").path("format").asText()).isEqualTo("uuid");
			assertThat(properties.path("refundStatus").path("enum").toString()).contains("DISBURSEMENT_RECORDED", "COMPLETED", "RETURNED", "null");
			assertThat(properties.path("status").path("enum").toString()).doesNotContain("COMPLETED");
		}
	}

	@Test
	void contractExposesOwnedMemberReceptionAndManagerDecisionSurfaces() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-withdrawal.yaml")) {
			assertThat(input).isNotNull();
			var paths = new ObjectMapper(new YAMLFactory()).readTree(input).path("paths");
			assertThat(paths.has("/members/me/course-enrollments/{enrollmentId}/withdrawal-requests")).isTrue();
			assertThat(paths.has("/members/me/course-withdrawal-requests/{requestId}")).isTrue();
			assertThat(paths.has("/reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests")).isTrue();
			assertThat(paths.has("/manager/course-withdrawal-requests/{requestId}/approve")).isTrue();
			assertThat(paths.has("/manager/course-withdrawal-requests/{requestId}/reject")).isTrue();
			for (String path : java.util.List.of("/members/me/course-withdrawal-requests/{requestId}/cancel", "/reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel")) {
				assertThat(paths.path(path).path("post").path("responses").has("409")).isTrue();
				assertThat(paths.path(path).path("post").path("responses").path("200").toString()).contains("WithdrawalRequest");
			}
		}
		try (var input = getClass().getResourceAsStream("/openapi/course-enrollment.yaml")) {
			var paths = new ObjectMapper(new YAMLFactory()).readTree(input).path("paths");
			assertThat(paths.has("/reception/members/{memberId}/course-enrollments")).isTrue();
			assertThat(paths.has("/reception/members/{memberId}/course-enrollments/{enrollmentId}")).isTrue();
		}
		try (var input = getClass().getResourceAsStream("/openapi/course-withdrawal.yaml")) {
			var schemas = new ObjectMapper(new YAMLFactory()).readTree(input).at("/components/schemas");
			assertThat(schemas.path("WithdrawalRequest").path("properties").fieldNames()).toIterable().contains("memberCode", "courseCode", "runCode", "totalUnits", "remainingUnits", "estimatedRefundAmount", "cause", "withdrawalEligible", "withdrawalReason", "refundBasisVnd", "refundAmountVnd", "scheduleChangeResponseId", "changeEventId", "snapshotCapturedAt", "snapshotSource", "snapshotWithdrawalEligible", "snapshotWithdrawalReason", "cancelledAt", "cancelledByAccountId", "cancellationReason", "canCancel", "learningSuspended");
			assertThat(schemas.at("/WithdrawalRequest/properties/cause/enum").toString()).contains("VOLUNTARY", "START_DELAY_OVER_7_DAYS", "MEMBER_SCHEDULE_CONFLICT", "REDUCED_UNIT_COUNT", "CENTER_SESSION_RESCHEDULE");
		}
	}
}
