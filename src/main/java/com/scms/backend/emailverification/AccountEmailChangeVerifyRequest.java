package com.scms.backend.emailverification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountEmailChangeVerifyRequest(
		@NotBlank @Pattern(regexp = "^[0-9]{6}$") String otp) {
}
