package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.Account;

public record MemberCoachAssignmentResponse(
	UUID assignmentId,
	UUID coachAccountId,
	String coachFullName,
	String coachPhone,
	String coachEmail,
	CoachAssignmentStatus status,
	Instant startsAt
) {
	static MemberCoachAssignmentResponse from(CoachAssignment assignment, Account coach) {
		return new MemberCoachAssignmentResponse(assignment.getId(), assignment.getCoachAccountId(),
				coach.getFullName(), coach.getPhone(), coach.getEmail(), assignment.getStatus(),
				assignment.getStartsAt());
	}
}
