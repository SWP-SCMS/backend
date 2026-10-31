package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class CourseScheduleChangePolicyTests {
	private static final Instant OLD_START = Instant.parse("2035-01-01T02:00:00Z");

	@Test
	void exactThirtyMinuteChangesOnSameVietnamDateAreMinor() {
		assertThat(CourseScheduleChangePolicy.isMaterial(OLD_START,
			OLD_START.plusSeconds(3600), OLD_START.plusSeconds(1800), OLD_START.plusSeconds(5400))).isFalse();
	}

	@Test
	void oneSecondOverThirtyMinutesIsMaterial() {
		assertThat(CourseScheduleChangePolicy.isMaterial(OLD_START,
			OLD_START.plusSeconds(3600), OLD_START.plusSeconds(1801), OLD_START.plusSeconds(5400))).isTrue();
	}

	@Test
	void crossingVietnamCalendarDateIsMaterialEvenWithinThirtyMinutes() {
		Instant beforeMidnight = Instant.parse("2035-01-01T16:50:00Z");
		assertThat(CourseScheduleChangePolicy.isMaterial(beforeMidnight, beforeMidnight.plusSeconds(3600),
			beforeMidnight.plusSeconds(600), beforeMidnight.plusSeconds(4200))).isTrue();
	}

	@Test
	void deadlineIsTheEarlierOfNotificationWindowAndSessionLeadTime() {
		Instant sentAt = Instant.parse("2035-01-01T00:00:00Z");
		assertThat(CourseScheduleChangePolicy.deadline(sentAt, sentAt.plus(Duration.ofDays(5))))
			.isEqualTo(Instant.parse("2035-01-04T00:00:00Z"));
		assertThat(CourseScheduleChangePolicy.deadline(sentAt, sentAt.plus(Duration.ofDays(2))))
			.isEqualTo(Instant.parse("2035-01-02T00:00:00Z"));
	}
}
