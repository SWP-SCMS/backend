package com.scms.backend.training.result;

import java.util.List;

public record TrainingResultPageResponse(List<TrainingResultResponse> content, int page, int size, long totalElements, int totalPages) {
}
