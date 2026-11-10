package com.scms.backend.membership.order;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MembershipOrderCreateRequest(@NotNull UUID offerId) {

	@JsonAnySetter
	public void rejectUnknownField(String fieldName, Object ignoredValue) {
		throw new IllegalArgumentException("Unknown membership order field: " + fieldName);
	}
}
