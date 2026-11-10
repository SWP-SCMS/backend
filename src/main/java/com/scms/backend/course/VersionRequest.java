package com.scms.backend.course;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record VersionRequest(@NotNull @PositiveOrZero Long expectedVersion, @jakarta.validation.constraints.Size(min = 5, max = 1000) String reason) {

	public VersionRequest(Long expectedVersion) {
		this(expectedVersion, null);
	}
}
