package com.scms.backend.training.assignment;

import com.scms.backend.scheduling.session.PtAssignmentImpactConfirmation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CoachAssignmentAssignRequest(@NotNull(message = "coachAccountId must not be null") UUID coachAccountId, @Size(max = 1000, message = "reason must be at most 1000 characters") String reason, PtAssignmentImpactConfirmation impactConfirmation) {

	public CoachAssignmentAssignRequest(UUID coachAccountId, String reason) {
		this(coachAccountId, reason, null);
	}
}
