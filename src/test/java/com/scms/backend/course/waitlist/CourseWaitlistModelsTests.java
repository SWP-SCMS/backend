package com.scms.backend.course.waitlist;

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
		assertThat(CourseWaitlistService.invitationExpiresAt(issued, registrationClose, nextSession)).isEqualTo(nextSession);
	}

	@Test
	void responseKeepsWaitlistAndInvitationEvidence() {
		UUID id = UUID.randomUUID();
		UUID runId = UUID.randomUUID();
		Instant now = Instant.parse("2026-11-01T00:00:00Z");
		CourseWaitlistResponse response = new CourseWaitlistResponse(id, runId, "Yoga Basics", "YOGA-01", "YOGA-01-R1", "WAITING", 2, null, null, now);
		assertThat(response.id()).isEqualTo(id);
		assertThat(response.courseRunId()).isEqualTo(runId);
		assertThat(response.courseName()).isEqualTo("Yoga Basics");
		assertThat(response.courseCode()).isEqualTo("YOGA-01");
		assertThat(response.runCode()).isEqualTo("YOGA-01-R1");
		assertThat(response.position()).isEqualTo(2);
	}
}
