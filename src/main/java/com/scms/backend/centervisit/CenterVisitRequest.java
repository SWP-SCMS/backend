package com.scms.backend.centervisit;

import java.util.UUID;

public record CenterVisitRequest(String memberId, String phone, Boolean identityVerified, UUID courseSessionId) {

	CenterVisitRequest(String memberId, String phone) {
		this(memberId, phone, null, null);
	}

	CenterVisitRequest(String memberId, String phone, Boolean identityVerified) {
		this(memberId, phone, identityVerified, null);
	}
}
