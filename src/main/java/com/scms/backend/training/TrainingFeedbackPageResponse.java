package com.scms.backend.training;

import java.util.List;

public record TrainingFeedbackPageResponse(
	List<TrainingFeedbackResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
