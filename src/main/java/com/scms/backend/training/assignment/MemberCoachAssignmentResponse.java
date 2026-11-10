package com.scms.backend.training.assignment;

import com.scms.backend.account.Account;
import com.scms.backend.training.CoachAssignment;
import com.scms.backend.training.CoachAssignmentStatus;
import java.time.Instant;
import java.util.UUID;

public record MemberCoachAssignmentResponse(UUID assignmentId, UUID coachAccountId, String coachFullName, String coachPhone, String coachEmail, CoachAssignmentStatus status, Instant startsAt) {

	static MemberCoachAssignmentResponse from(CoachAssignment assignment, Account coach) {
		return new MemberCoachAssignmentResponse(assignment.getId(), assignment.getCoachAccountId(), coach.getFullName(), coach.getPhone(), coach.getEmail(), assignment.getStatus(), assignment.getStartsAt());
	}
}
