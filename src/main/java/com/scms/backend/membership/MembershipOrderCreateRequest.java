package com.scms.backend.membership;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;

public record MembershipOrderCreateRequest(@NotNull UUID offerId) {

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown membership order field: " + fieldName);
	}
}
