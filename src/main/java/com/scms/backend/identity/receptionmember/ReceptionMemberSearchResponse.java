package com.scms.backend.identity.receptionmember;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReceptionMemberSearchResponse(UUID accountId, String memberId, String fullName, String phone,
		String email, LocalDate birthDate, String profileImageUrl, AccountStatus status,
		@Schema(types = {"object", "null"}, requiredMode = Schema.RequiredMode.REQUIRED,
			schemaResolution = Schema.SchemaResolution.INLINE,
			description = "Current ACTIVE Membership for startsAt <= now < endsAt; frozen Memberships remain visible. Null when absent. Search does not reconcile lifecycle state.")
		ReceptionActiveMembershipResponse activeMembership) {
}
