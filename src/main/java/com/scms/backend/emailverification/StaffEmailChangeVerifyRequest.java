package com.scms.backend.emailverification;

import jakarta.validation.constraints.Pattern;

public record StaffEmailChangeVerifyRequest(@Pattern(regexp="^[0-9]{6}$") String otp) { }
