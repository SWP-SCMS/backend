package com.scms.backend.emailverification;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record ManualActivationCreateRequest(@NotBlank String memberId,@NotNull ManualActivationReasonCode reasonCode,
	@NotBlank @Size(max=1000) String reasonDetail,@NotNull ManualVerificationMethod verificationMethod,
	@NotBlank @Size(max=2000) String evidenceNote,@AssertTrue(message="must confirm in-person identity check") boolean identityChecked) { }
