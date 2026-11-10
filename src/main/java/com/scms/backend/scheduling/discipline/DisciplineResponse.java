package com.scms.backend.scheduling.discipline;

import com.scms.backend.scheduling.Discipline;
import com.scms.backend.scheduling.DisciplineStatus;
import java.util.List;
import java.util.UUID;

public record DisciplineResponse(UUID id, String name, String description, DisciplineStatus status, List<String> requiredBenefitCodes) {

	static DisciplineResponse from(Discipline discipline) {
		return new DisciplineResponse(discipline.getId(), discipline.getName(), discipline.getDescription(), discipline.getStatus(), discipline.getEffectiveRequiredBenefitCodes());
	}
}
