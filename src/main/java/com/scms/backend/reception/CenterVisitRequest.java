package com.scms.backend.reception;

public record CenterVisitRequest(String memberId, String phone, Boolean identityVerified) {

	CenterVisitRequest(String memberId, String phone) {
		this(memberId, phone, null);
	}
}
