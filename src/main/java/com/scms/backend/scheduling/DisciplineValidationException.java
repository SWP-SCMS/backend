package com.scms.backend.scheduling;

import com.scms.backend.scheduling.discipline.DisciplineSessionConflict;
import java.util.List;

public class DisciplineValidationException extends RuntimeException {

	private final String field;

	private final List<DisciplineSessionConflict> affectedSessions;

	public DisciplineValidationException(String field, String message) {
		this(field, message, List.of());
	}

	public DisciplineValidationException(String field, String message, List<DisciplineSessionConflict> affectedSessions) {
		super(message);
		this.field = field;
		this.affectedSessions = List.copyOf(affectedSessions);
	}

	public String getField() {
		return field;
	}

	public List<DisciplineSessionConflict> getAffectedSessions() {
		return affectedSessions;
	}
}
