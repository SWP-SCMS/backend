package com.scms.backend.training;

import java.util.List;

public record MemberTrainingFeedbackPageResponse(
	List<MemberTrainingFeedbackResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
