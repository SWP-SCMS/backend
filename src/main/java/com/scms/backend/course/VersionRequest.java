package com.scms.backend.course;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VersionRequest(@NotNull @PositiveOrZero Long expectedVersion, @jakarta.validation.constraints.Size(min = 5, max = 1000) String reason) {

	public VersionRequest(Long expectedVersion) {
		this(expectedVersion, null);
	}
}
