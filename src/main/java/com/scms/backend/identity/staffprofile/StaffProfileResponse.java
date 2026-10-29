package com.scms.backend.identity.staffprofile;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record StaffProfileResponse(UUID accountId, String fullName, String phone, String email,
		LocalDate birthDate, AccountRole role, AccountStatus status,
		@Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String profileImageUrl,
		boolean pendingEmailChange,
		String pendingEmailMasked, Instant pendingEmailOtpExpiresAt, Instant pendingEmailResendAvailableAt) { }
