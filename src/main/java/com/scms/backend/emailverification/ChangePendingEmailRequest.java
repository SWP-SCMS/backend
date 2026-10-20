package com.scms.backend.emailverification;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record ChangePendingEmailRequest(@NotBlank @Email String newEmail) { }
