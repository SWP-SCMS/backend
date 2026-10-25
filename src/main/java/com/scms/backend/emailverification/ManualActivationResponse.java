package com.scms.backend.emailverification;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
public record ManualActivationResponse(UUID requestId,UUID accountId,String memberId,String maskedEmail,
	UUID requestedByAccountId,String requestedByName,AccountRole requestedByRole,
	ManualActivationReasonCode reasonCode,String reasonDetail,
	ManualVerificationMethod verificationMethod,String evidenceNote,ManualActivationStatus status,
	UUID reviewedByAccountId,String decisionNote,Instant requestedAt,Instant reviewedAt,Instant expiresAt) { }
