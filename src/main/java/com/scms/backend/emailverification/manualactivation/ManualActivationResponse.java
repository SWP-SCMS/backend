package com.scms.backend.emailverification.manualactivation;

import com.scms.backend.account.AccountRole;
import com.scms.backend.emailverification.ManualActivationReasonCode;
import com.scms.backend.emailverification.ManualActivationStatus;
import com.scms.backend.emailverification.ManualVerificationMethod;
import java.time.Instant;
import java.util.UUID;

public record ManualActivationResponse(UUID requestId, UUID accountId, String memberId, String maskedEmail, UUID requestedByAccountId, String requestedByName, AccountRole requestedByRole, ManualActivationReasonCode reasonCode, String reasonDetail, ManualVerificationMethod verificationMethod, String evidenceNote, ManualActivationStatus status, UUID reviewedByAccountId, String decisionNote, Instant requestedAt, Instant reviewedAt, Instant expiresAt) {
}
