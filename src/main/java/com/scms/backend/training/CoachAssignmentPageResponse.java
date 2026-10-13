package com.scms.backend.training;

import java.util.List;

public record CoachAssignmentPageResponse(
	List<CoachAssignmentResponse> content,
	int page,
	int size,
	long totalElements,
	int totalPages
) {
}
