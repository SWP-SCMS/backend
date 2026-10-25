package com.scms.backend.training;

import com.scms.backend.scheduling.PtAssignmentImpactConfirmation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachAssignmentEndRequest(
	@NotBlank(message = "reason must not be blank")
	@Size(max = 1000, message = "reason must be at most 1000 characters")
	String reason,

	PtAssignmentImpactConfirmation impactConfirmation
) {
	public CoachAssignmentEndRequest(String reason) {
		this(reason, null);
	}
}
