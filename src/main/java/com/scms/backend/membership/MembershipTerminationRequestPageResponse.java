package com.scms.backend.membership;

import java.util.List;

public record MembershipTerminationRequestPageResponse(List<MembershipTerminationRequestResponse> content,
		int page, int size, long totalElements, int totalPages) {
	static MembershipTerminationRequestPageResponse from(org.springframework.data.domain.Page<MembershipTerminationRequest> page) {
		return new MembershipTerminationRequestPageResponse(page.getContent().stream()
			.map(MembershipTerminationRequestResponse::from).toList(), page.getNumber(), page.getSize(),
			page.getTotalElements(), page.getTotalPages());
	}
	static MembershipTerminationRequestPageResponse from(org.springframework.data.domain.Page<MembershipTerminationRequest> page,
			java.util.function.Function<MembershipTerminationRequest, MembershipTerminationRequestResponse> mapper) {
		return new MembershipTerminationRequestPageResponse(page.getContent().stream().map(mapper).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
	static MembershipTerminationRequestPageResponse empty() {
		return new MembershipTerminationRequestPageResponse(List.of(), 0, 20, 0, 0);
	}
}
