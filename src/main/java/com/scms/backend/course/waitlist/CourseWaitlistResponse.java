package com.scms.backend.course.waitlist;

import java.time.Instant;
import java.util.UUID;

public record CourseWaitlistResponse(UUID id, UUID courseRunId, String courseName, String courseCode, String runCode, String status, Integer position, Instant invitedAt, Instant invitationExpiresAt, Instant createdAt) {
}
