package com.scms.backend.scheduling;

import java.util.UUID;

public record SportClassCreateRequest(UUID disciplineId, String name, SportClassType classType,
		String description) {
}
