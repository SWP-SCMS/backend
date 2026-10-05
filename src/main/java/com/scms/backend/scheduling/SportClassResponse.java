package com.scms.backend.scheduling;

import java.util.UUID;

public record SportClassResponse(UUID id, UUID disciplineId, String disciplineName, String name,
		SportClassType classType, String description, SportClassStatus status) {

	static SportClassResponse from(SportClass sportClass) {
		return new SportClassResponse(sportClass.getId(), sportClass.getDiscipline().getId(),
			sportClass.getDiscipline().getName(), sportClass.getName(), sportClass.getClassType(),
			sportClass.getDescription(), sportClass.getStatus());
	}
}
