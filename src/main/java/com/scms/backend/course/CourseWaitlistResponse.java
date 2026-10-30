package com.scms.backend.course;

import java.time.Instant;
import java.util.UUID;

public record CourseWaitlistResponse(UUID id, UUID courseRunId, String status, Integer position,
		Instant invitedAt, Instant invitationExpiresAt, Instant createdAt) { }
