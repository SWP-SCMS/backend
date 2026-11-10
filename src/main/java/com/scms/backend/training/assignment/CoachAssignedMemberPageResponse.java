package com.scms.backend.training.assignment;

import java.util.List;

/**
 * Paged result of assigned members for a Coach.
 */
public record CoachAssignedMemberPageResponse(List<CoachAssignedMemberSummaryResponse> content, int page, int size, long totalElements, int totalPages) {
}
