package com.scms.backend.training;

import java.util.List;
import java.util.Map;
import java.util.Set;

final class TrainingPlanContentValidator {

	private static final Set<String> ALLOWED_FIELDS = Set.of("goal", "schedule", "exercises", "notes");
	private static final int MAX_TEXT_LENGTH = 4000;
	private static final int MAX_EXERCISES = 100;

	private TrainingPlanContentValidator() {
	}

	static void validate(Map<String, Object> content) {
		for (String field : content.keySet()) {
			if (!ALLOWED_FIELDS.contains(field)) {
				throw TrainingPlanException.validationError("content." + field, "Unknown Training Plan content field.");
			}
		}
		validateText(content, "goal");
		validateText(content, "schedule");
		validateText(content, "notes");
		Object exercises = content.get("exercises");
		if (exercises != null) {
			if (!(exercises instanceof List<?> values) || values.size() > MAX_EXERCISES
					|| values.stream().anyMatch(value -> !(value instanceof String text)
							|| text.isBlank() || text.length() > 500)) {
				throw TrainingPlanException.validationError("content.exercises",
						"exercises must contain at most 100 non-blank strings of at most 500 characters.");
			}
		}
	}

	private static void validateText(Map<String, Object> content, String field) {
		Object value = content.get(field);
		if (value != null && (!(value instanceof String text) || text.length() > MAX_TEXT_LENGTH)) {
			throw TrainingPlanException.validationError("content." + field,
					field + " must be a string of at most 4000 characters.");
		}
	}
}
