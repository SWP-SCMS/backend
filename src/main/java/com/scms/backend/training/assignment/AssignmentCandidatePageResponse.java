package com.scms.backend.training.assignment;

import java.util.List;

public record AssignmentCandidatePageResponse(List<AssignmentCandidateResponse> content, int page, int size, long totalElements, int totalPages) {
}
