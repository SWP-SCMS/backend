package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record AssignmentCandidateResponse(
	UUID memberAccountId,
	String memberCode,
	String fullName,
	String phone,
	String email,
	Instant membershipEndsAt,
	@Schema(allowableValues = { "UNASSIGNED", "ASSIGNED" }) AssignmentCandidateStatus assignmentStatus
) {
	static AssignmentCandidateResponse from(CoachAssignmentRepository.AssignmentCandidateView view) {
		return new AssignmentCandidateResponse(view.getMemberAccountId(), view.getMemberCode(), view.getFullName(),
			view.getPhone(), view.getEmail(), view.getMembershipEndsAt(),
			AssignmentCandidateStatus.valueOf(view.getAssignmentStatus()));
	}
}
