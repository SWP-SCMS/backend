package com.scms.backend.audit;

import java.util.List;

public record AuditHistoryPageResponse(List<AuditHistoryResponse> content, int page, int size, long totalElements,
		int totalPages) {
}
