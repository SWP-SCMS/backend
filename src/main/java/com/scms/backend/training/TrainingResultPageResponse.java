package com.scms.backend.training;

import java.util.List;

public record TrainingResultPageResponse(
	List<TrainingResultResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
