package com.scms.backend.identity.staffprofile;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.member.MemberProfileDetailsValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffProfileService {
	private final AccountRepository accounts;
	private final AccountIdentifierAvailability identifiers;
	private final MemberProfileDetailsValidator validator;

	StaffProfileService(AccountRepository accounts, AccountIdentifierAvailability identifiers,
			MemberProfileDetailsValidator validator) {
		this.accounts = accounts;
		this.identifiers = identifiers;
		this.validator = validator;
	}

	@Transactional(readOnly = true)
	StaffProfileResponse getProfile(UUID accountId) {
		return toResponse(loadStaff(accountId));
	}

	@Transactional
	StaffProfileResponse updateProfile(UUID accountId, StaffProfilePatchRequest request) {
		Account account = loadStaff(accountId);
		String fullName = request.fullName() == null ? account.getFullName()
			: validator.normalizeRequired(request.fullName(), "fullName", 200);
		String phone = request.phone() == null ? account.getPhone()
			: validator.normalizePhone(request.phone(), "phone");
		String email = request.email() == null ? account.getEmail() : validator.normalizeEmail(request.email());
		LocalDate birthDate = request.birthDate() == null ? account.getBirthDate()
			: validator.validateBirthDate(request.birthDate());
		identifiers.ensureAvailableForUpdate(accountId, account.getEmail(), account.getPhone(), email, phone);
		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		accounts.flush();
		return toResponse(account);
	}

	private Account loadStaff(UUID accountId) {
		Account account = accounts.findById(accountId)
			.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists"));
		if (account.getRole() == AccountRole.MEMBER) {
			throw new IllegalStateException("Staff profile requested for a member account");
		}
		return account;
	}

	private StaffProfileResponse toResponse(Account account) {
		return new StaffProfileResponse(account.getId(), account.getFullName(), account.getPhone(), account.getEmail(),
			account.getBirthDate(), account.getRole(), account.getStatus());
	}
}
