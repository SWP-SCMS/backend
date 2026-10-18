package com.scms.backend.training;

import java.util.List;

public record TrainingPlanPageResponse(
	List<TrainingPlanResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
