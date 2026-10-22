package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record CoachLeaveResponse(UUID id, UUID coachId, Instant startTime, Instant endTime, String reason,
		CoachLeaveStatus status, UUID reviewedBy, String reviewNote, Instant reviewedAt, Instant createdAt,
		List<UUID> affectedSessionIds) {
	static CoachLeaveResponse from(CoachLeaveRequest request, List<UUID> affectedSessionIds) {
		return new CoachLeaveResponse(request.getId(), request.getCoach().getId(), request.getStartTime(),
			request.getEndTime(), request.getReason(), request.getStatus(), request.getReviewedBy(),
			request.getReviewNote(), request.getReviewedAt(), request.getCreatedAt(), List.copyOf(affectedSessionIds));
	}
}

