package com.scms.backend.training.result;

import java.util.List;

public record MemberTrainingResultPageResponse(List<MemberTrainingResultResponse> content, int page, int size, long totalElements, int totalPages) {
}
