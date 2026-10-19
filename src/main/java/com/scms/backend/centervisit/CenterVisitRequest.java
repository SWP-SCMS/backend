package com.scms.backend.centervisit;

public record CenterVisitRequest(String memberId, String phone, Boolean identityVerified) {

	CenterVisitRequest(String memberId, String phone) {
		this(memberId, phone, null);
	}
}
