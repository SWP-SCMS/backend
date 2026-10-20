package com.scms.backend.scheduling;

import java.util.List;

public record DisciplinePatchRequest(String name, String description, DisciplineStatus status,
		List<String> requiredBenefitCodes) {

	public DisciplinePatchRequest(String name, String description, DisciplineStatus status) {
		this(name, description, status, null);
	}
}
