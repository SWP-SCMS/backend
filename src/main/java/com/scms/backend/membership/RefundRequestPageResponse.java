package com.scms.backend.membership;

import java.util.List;

public record RefundRequestPageResponse(List<RefundRequestResponse> content, int page, int size,
		long totalElements, int totalPages) {
	static RefundRequestPageResponse from(org.springframework.data.domain.Page<RefundRequestResponse> page) {
		return new RefundRequestPageResponse(page.getContent(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
