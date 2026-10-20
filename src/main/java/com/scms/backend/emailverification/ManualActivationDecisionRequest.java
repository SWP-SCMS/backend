package com.scms.backend.emailverification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ManualActivationDecisionRequest(@NotBlank @Size(max=1000) String decisionNote) { }
