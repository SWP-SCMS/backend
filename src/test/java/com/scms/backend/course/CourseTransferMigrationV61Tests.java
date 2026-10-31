package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class CourseTransferMigrationV61Tests {
	@Test
	void migrationAddsLifecycleRequestsOffersAndCapacityHolds() throws Exception {
		try (var input = getClass().getResourceAsStream("/db/migration/V61__course_run_transfers.sql")) {
			assertThat(input).isNotNull();
			String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(sql).contains("lifecycle_status", "course_transfer_requests", "course_transfer_offers",
				"course_transfer_holds", "TRANSFER_CREDIT_EXCESS", "TRANSFERRED");
			assertThat(sql).contains("uq_course_transfer_request_schedule_change",
				"uq_course_transfer_offer_acceptance_key", "uq_course_transfer_hold_offer");
		}
	}
}
