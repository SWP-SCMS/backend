package com.scms.backend.membership;

import java.util.List;

public record ReconciliationQueuePageResponse(List<ReconciliationQueueItem> content, int page, int size,
		long totalElements, int totalPages) {
}
