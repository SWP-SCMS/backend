package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.Account;

public record CoachAssignmentResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAccountId,
	String coachFullName,
	String coachPhone,
	String coachEmail,
	UUID assignedByAccountId,
	CoachAssignmentStatus status,
	Instant startsAt,
	Instant endsAt,
	String endReason,
	Instant createdAt
) {
	public static CoachAssignmentResponse from(CoachAssignment assignment, Account coach) {
		return new CoachAssignmentResponse(
			assignment.getId(),
			assignment.getMemberAccountId(),
			assignment.getCoachAccountId(),
			coach != null ? coach.getFullName() : null,
			coach != null ? coach.getPhone() : null,
			coach != null ? coach.getEmail() : null,
			assignment.getAssignedByAccountId(),
			assignment.getStatus(),
			assignment.getStartsAt(),
			assignment.getEndsAt(),
			assignment.getEndReason(),
			assignment.getCreatedAt()
		);
	}
}
