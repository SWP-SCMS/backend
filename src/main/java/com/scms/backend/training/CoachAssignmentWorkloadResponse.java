package com.scms.backend.training;

import java.util.UUID;

public record CoachAssignmentWorkloadResponse(
	UUID coachAccountId,
	String fullName,
	String phone,
	String email,
	long activeMemberCount,
	int maximumMemberCount,
	boolean availableForAssignment
) {
	static CoachAssignmentWorkloadResponse from(CoachAssignmentRepository.CoachAssignmentWorkloadView view,
			int maximumMemberCount) {
		return new CoachAssignmentWorkloadResponse(view.getCoachAccountId(), view.getFullName(), view.getPhone(),
			view.getEmail(), view.getActiveMemberCount(), maximumMemberCount,
			view.getActiveMemberCount() < maximumMemberCount);
	}
}
