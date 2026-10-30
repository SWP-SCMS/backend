package com.scms.backend.course;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

final class CourseScheduleChangePolicy {
	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final Duration MATERIAL_DELTA = Duration.ofMinutes(30);

	private CourseScheduleChangePolicy() {}

	static boolean isMaterial(Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd) {
		return Duration.between(oldStart, newStart).abs().compareTo(MATERIAL_DELTA) > 0
			|| Duration.between(oldEnd, newEnd).abs().compareTo(MATERIAL_DELTA) > 0
			|| !oldStart.atZone(VIETNAM).toLocalDate().equals(newStart.atZone(VIETNAM).toLocalDate());
	}

	static Instant deadline(Instant notificationSentAt, Instant newStartAt) {
		return notificationSentAt.plus(Duration.ofHours(72)).isBefore(newStartAt.minus(Duration.ofHours(24)))
			? notificationSentAt.plus(Duration.ofHours(72)) : newStartAt.minus(Duration.ofHours(24));
	}
}
