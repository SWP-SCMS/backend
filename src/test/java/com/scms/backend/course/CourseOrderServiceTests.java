package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class CourseOrderServiceTests {

	private static final Instant NOW = Instant.parse("2026-11-01T00:00:00Z");

	@Test
	void holdExpiresAfterFifteenMinutesWhenRegistrationClosesLater() {
		assertThat(CourseOrderService.holdExpiresAt(NOW, NOW.plusSeconds(3600)))
			.isEqualTo(NOW.plusSeconds(900));
	}

	@Test
	void holdExpiresAtRegistrationCloseWhenItComesFirst() {
		assertThat(CourseOrderService.holdExpiresAt(NOW, NOW.plusSeconds(300)))
			.isEqualTo(NOW.plusSeconds(300));
	}
}
