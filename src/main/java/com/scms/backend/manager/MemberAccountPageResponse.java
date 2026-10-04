package com.scms.backend.manager;

import java.util.List;

public record MemberAccountPageResponse(List<MemberAccountResponse> content, int page, int size, long totalElements,
		int totalPages) {
}
