package com.scms.backend.scheduling;

import java.util.UUID;

public record DisciplineResponse(UUID id, String name, String description, DisciplineStatus status) {

	static DisciplineResponse from(Discipline discipline) {
		return new DisciplineResponse(discipline.getId(), discipline.getName(), discipline.getDescription(),
			discipline.getStatus());
	}
}
