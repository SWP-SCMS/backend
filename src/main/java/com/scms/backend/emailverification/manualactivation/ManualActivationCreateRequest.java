package com.scms.backend.emailverification.manualactivation;

import com.scms.backend.emailverification.ManualActivationReasonCode;
import com.scms.backend.emailverification.ManualVerificationMethod;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ManualActivationCreateRequest(@NotBlank String memberId, @NotNull ManualActivationReasonCode reasonCode, @NotBlank @Size(max = 1000) String reasonDetail, @NotNull ManualVerificationMethod verificationMethod, @NotBlank @Size(max = 2000) String evidenceNote, @AssertTrue(message = "must confirm in-person identity check") boolean identityChecked) {
}
