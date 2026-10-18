package com.scms.backend.training;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

public record CoachAssignmentWorkloadResponse(
	UUID coachAccountId,
	String fullName,
	String phone,
	String email,
	long activeMemberCount,
	@Schema(nullable = true) Integer maximumMemberCount,
	boolean availableForAssignment
) {
	static CoachAssignmentWorkloadResponse from(CoachAssignmentRepository.CoachAssignmentWorkloadView view) {
		return new CoachAssignmentWorkloadResponse(view.getCoachAccountId(), view.getFullName(), view.getPhone(),
			view.getEmail(), view.getActiveMemberCount(), null, true);
	}
}
