package com.scms.backend.emailverification;
import java.time.Instant;
import java.util.UUID;
public record ManualActivationResponse(UUID requestId,UUID accountId,String memberId,String maskedEmail,
	UUID requestedByAccountId,ManualActivationReasonCode reasonCode,String reasonDetail,
	ManualVerificationMethod verificationMethod,String evidenceNote,ManualActivationStatus status,
	UUID reviewedByAccountId,String decisionNote,Instant requestedAt,Instant reviewedAt,Instant expiresAt) { }
