package com.scms.backend.emailverification;
import java.time.Instant;
import java.util.UUID;
public record PendingVerificationAccountResponse(UUID accountId,String memberId,String fullName,String phone,
	String maskedEmail,Instant registrationExpiresAt) { }
