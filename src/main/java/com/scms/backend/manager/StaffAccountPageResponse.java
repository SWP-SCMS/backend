package com.scms.backend.manager;

import java.util.List;

public record StaffAccountPageResponse(List<StaffAccountResponse> content, int page, int size, long totalElements,
		int totalPages) {
}
