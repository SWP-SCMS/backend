package com.scms.backend.emailverification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(@NotBlank @Email String email,
		@NotBlank @Pattern(regexp="^[0-9]{6}$", message="must be 6 digits") String otp) { }
