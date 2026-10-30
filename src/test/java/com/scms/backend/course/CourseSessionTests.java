package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CourseSessionTests {

	@Test
	void reschedulingKeepsTheOriginalScheduleSnapshot() {
		Instant originalStart = Instant.parse("2035-01-01T02:00:00Z");
		Instant originalEnd = Instant.parse("2035-01-01T03:00:00Z");
		CourseSession session = new CourseSession(UUID.randomUUID(), null, 1, null, null, originalStart,
			originalEnd, java.math.BigInteger.ONE);

		session.reschedule(Instant.parse("2035-01-02T02:00:00Z"), Instant.parse("2035-01-02T03:00:00Z"));

		assertThat(session.getStartAt()).isEqualTo(Instant.parse("2035-01-02T02:00:00Z"));
		assertThat(session.getOriginalStartAt()).isEqualTo(originalStart);
		assertThat(session.getOriginalEndAt()).isEqualTo(originalEnd);
	}
}
