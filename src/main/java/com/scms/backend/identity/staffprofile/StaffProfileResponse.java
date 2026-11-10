package com.scms.backend.identity.staffprofile;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record StaffProfileResponse(UUID accountId, String fullName, String phone, String email, LocalDate birthDate, AccountRole role, AccountStatus status, @Schema(types = { "string", "null" }, requiredMode = Schema.RequiredMode.REQUIRED) String profileImageUrl, boolean pendingEmailChange, String pendingEmailMasked, Instant pendingEmailOtpExpiresAt, Instant pendingEmailResendAvailableAt, String emergencyContactName, String emergencyContactPhone, String emergencyContactRelationship) {

	public StaffProfileResponse(UUID accountId, String fullName, String phone, String email, LocalDate birthDate, AccountRole role, AccountStatus status, String profileImageUrl, boolean pendingEmailChange, String pendingEmailMasked, Instant pendingEmailOtpExpiresAt, Instant pendingEmailResendAvailableAt) {
		this(accountId, fullName, phone, email, birthDate, role, status, profileImageUrl, pendingEmailChange, pendingEmailMasked, pendingEmailOtpExpiresAt, pendingEmailResendAvailableAt, null, null, null);
	}
}
