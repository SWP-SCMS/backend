package com.scms.backend.membership;

import java.util.List;

public record MembershipFreezeRequestPageResponse(List<MembershipFreezeRequestResponse> content, int page, int size,
		long totalElements, int totalPages) {
	static MembershipFreezeRequestPageResponse from(org.springframework.data.domain.Page<MembershipFreezeRequest> page) {
		return fromResponses(page.map(MembershipFreezeRequestResponse::from));
	}
	static MembershipFreezeRequestPageResponse fromResponses(
			org.springframework.data.domain.Page<MembershipFreezeRequestResponse> page) {
		return new MembershipFreezeRequestPageResponse(page.getContent(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
	static MembershipFreezeRequestPageResponse empty() { return new MembershipFreezeRequestPageResponse(List.of(), 0, 20, 0, 0); }
}
