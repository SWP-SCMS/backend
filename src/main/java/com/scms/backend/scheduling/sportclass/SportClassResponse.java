package com.scms.backend.scheduling.sportclass;

import com.scms.backend.scheduling.SportClass;
import com.scms.backend.scheduling.SportClassStatus;
import com.scms.backend.scheduling.SportClassType;
import java.util.UUID;

public record SportClassResponse(UUID id, UUID disciplineId, String disciplineName, String name, SportClassType classType, String description, SportClassStatus status) {

	static SportClassResponse from(SportClass sportClass) {
		return new SportClassResponse(sportClass.getId(), sportClass.getDiscipline().getId(), sportClass.getDiscipline().getName(), sportClass.getName(), sportClass.getClassType(), sportClass.getDescription(), sportClass.getStatus());
	}
}
