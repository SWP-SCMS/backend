package com.scms.backend.scheduling;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplineCreateRequest(
		@NotBlank @Size(max = 150) String name,
		String description) {
}
