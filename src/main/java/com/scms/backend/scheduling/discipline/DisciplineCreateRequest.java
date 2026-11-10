package com.scms.backend.scheduling.discipline;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DisciplineCreateRequest(@NotBlank @Size(max = 150) String name, String description, List<String> requiredBenefitCodes) {

	public DisciplineCreateRequest(String name, String description) {
		this(name, description, null);
	}
}
