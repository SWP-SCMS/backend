package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CourseWaitlistModelsTests {

	@Test
	void invitationExpiryUsesEarliestBusinessDeadline() {
		Instant issued = Instant.parse("2026-11-01T00:00:00Z");
		Instant registrationClose = issued.plusSeconds(48 * 3600);
		Instant nextSession = issued.plusSeconds(12 * 3600);

		assertThat(CourseWaitlistService.invitationExpiresAt(issued, registrationClose, nextSession))
			.isEqualTo(nextSession);
	}

	@Test
	void responseKeepsWaitlistAndInvitationEvidence() {
		UUID id = UUID.randomUUID();
		UUID runId = UUID.randomUUID();
		Instant now = Instant.parse("2026-11-01T00:00:00Z");
		CourseWaitlistResponse response = new CourseWaitlistResponse(id, runId, "WAITING", 2, null, null, now);

		assertThat(response.id()).isEqualTo(id);
		assertThat(response.courseRunId()).isEqualTo(runId);
		assertThat(response.position()).isEqualTo(2);
	}
}
