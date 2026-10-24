package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachLeaveResponse(UUID id, UUID coachId, String coachFullName, String coachPhone,
		Instant startTime, Instant endTime, String reason,
		CoachLeaveStatus status, UUID reviewedBy, String reviewNote, Instant reviewedAt, Instant createdAt,
		List<UUID> affectedSessionIds) {
	static CoachLeaveResponse from(CoachLeaveRequest request, List<UUID> affectedSessionIds) {
		return new CoachLeaveResponse(request.getId(), request.getCoach().getId(),
			request.getCoach().getFullName(), request.getCoach().getPhone(), request.getStartTime(),
			request.getEndTime(), request.getReason(), request.getStatus(), request.getReviewedBy(),
			request.getReviewNote(), request.getReviewedAt(), request.getCreatedAt(),
			List.copyOf(affectedSessionIds));
	}
}

