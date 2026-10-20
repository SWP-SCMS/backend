package com.scms.backend.scheduling;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DisciplineCreateRequest(
		@NotBlank @Size(max = 150) String name,
		String description,
		List<String> requiredBenefitCodes) {

	public DisciplineCreateRequest(String name, String description) {
		this(name, description, null);
	}
}
