package com.scms.backend.reception;

import java.util.List;

public record CenterVisitPageResponse(List<CenterVisitResponse> content, int page, int size,
		long totalElements, int totalPages) {
}
