package com.scms.backend.scheduling;

import java.util.List;

public record ClassSessionPageResponse(List<ClassSessionResponse> content, int page, int size,
		long totalElements, int totalPages) {
}
