package com.scms.backend.scheduling.discipline;

import com.scms.backend.scheduling.DisciplineStatus;
import java.util.List;

public record DisciplinePatchRequest(String name, String description, DisciplineStatus status, List<String> requiredBenefitCodes) {

	public DisciplinePatchRequest(String name, String description, DisciplineStatus status) {
		this(name, description, status, null);
	}
}
