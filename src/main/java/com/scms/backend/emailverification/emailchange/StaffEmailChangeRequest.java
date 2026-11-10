package com.scms.backend.emailverification.emailchange;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StaffEmailChangeRequest(@NotBlank @Email @Size(max = 320) String newEmail, @NotBlank @Size(max = 200) String currentPassword) {
}
