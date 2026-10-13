package com.scms.backend.training;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CoachAssignmentAssignRequest(
	@NotNull(message = "coachAccountId must not be null")
	UUID coachAccountId,

	@Size(max = 1000, message = "reason must be at most 1000 characters")
	String reason
) {
}
