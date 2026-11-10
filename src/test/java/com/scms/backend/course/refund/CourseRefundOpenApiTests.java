package com.scms.backend.course.refund;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

class CourseRefundOpenApiTests {

	@Test
	void contractExposesReviewDisbursementVerificationAndMemberHistory() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-refund-obligations.yaml")) {
			assertThat(input).isNotNull();
			var document = new ObjectMapper(new YAMLFactory()).readTree(input);
			var paths = document.path("paths");
			assertThat(paths.has("/manager/course-refund-obligations")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/approve")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/reject")).isTrue();
			assertThat(paths.has("/reception/course-refund-obligations/{obligationId}/disburse")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/verify")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/fail")).isTrue();
			assertThat(paths.has("/manager/course-refund-obligations/{obligationId}/return")).isTrue();
			assertThat(paths.has("/members/me/course-refund-obligations")).isTrue();
			assertThat(paths.has("/members/me/course-refund-obligations/{obligationId}")).isTrue();
			assertThat(paths.has("/reception/course-refunds")).isTrue();
			assertThat(paths.has("/reception/course-refunds/{refundId}")).isTrue();
			var disburse = paths.path("/reception/course-refund-obligations/{obligationId}/disburse").path("post");
			assertThat(disburse.path("parameters").toString()).contains("IdempotencyKey");
			assertThat(disburse.at("/responses/409/description").asText()).contains("COURSE_REFUND_DISBURSEMENT_NOT_ELIGIBLE");
			assertThat(document.at("/components/parameters/IdempotencyKey/name").asText()).isEqualTo("Idempotency-Key");
		}
	}

	@Test
	void receptionQueueContractIsPaginatedFilterableAndRedacted() throws Exception {
		try (var input = getClass().getResourceAsStream("/openapi/course-refund-obligations.yaml")) {
			var document = new ObjectMapper(new YAMLFactory()).readTree(input);
			var list = document.at("/paths/~1reception~1course-refunds/get");
			assertThat(list.path("parameters").toString()).contains("page", "size", "eligible");
			assertThat(list.at("/responses/200/content/application~1json/schema/$ref").asText()).isEqualTo("#/components/schemas/ReceptionCourseRefundPage");
			assertThat(document.at("/paths/~1reception~1course-refunds~1{refundId}/get/responses/200/content/application~1json/schema/$ref").asText()).isEqualTo("#/components/schemas/ReceptionCourseRefund");
			var response = document.at("/components/schemas/ReceptionCourseRefund");
			assertThat(response.path("additionalProperties").asBoolean()).isFalse();
			assertThat(response.path("properties").properties().stream().map(java.util.Map.Entry::getKey).toList()).containsExactlyInAnyOrder("refundId", "memberId", "memberCode", "memberName", "courseId", "courseCode", "courseName", "courseRunId", "runCode", "courseRunStatus", "source", "amount", "approvedAt", "version", "disbursementEligible", "disbursementEligibilityReason").doesNotContain("email", "phone", "evidenceReference", "disbursementReference", "bankAccount");
			assertThat(response.at("/properties/disbursementEligibilityReason/enum").toString()).isEqualTo("[\"ELIGIBLE\",\"AWAITING_COURSE_COMPLETION\",\"COURSE_RUN_NOT_FOUND\"]");
		}
	}
}
