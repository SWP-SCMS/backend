package com.scms.backend.member;

import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberProfileService {

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final MemberProfileDetailsValidator validator;

	MemberProfileService(AccountRepository accountRepository, MemberProfileRepository memberProfileRepository,
			AccountIdentifierAvailability identifierAvailability, MemberProfileDetailsValidator validator) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.identifierAvailability = identifierAvailability;
		this.validator = validator;
	}

	@Transactional(readOnly = true)
	MemberProfileResponse getProfile(UUID accountId) {
		return toResponse(loadMemberAccount(accountId), loadProfile(accountId));
	}

	@Transactional
	MemberProfileResponse updateProfile(UUID accountId, MemberProfilePatchRequest request) {
		Account account = loadMemberAccount(accountId);
		MemberProfile profile = loadProfile(accountId);

		String fullName = request.hasFullName()
			? validator.normalizeRequired(request.fullName(), "fullName", 200)
			: account.getFullName();
		String phone = request.hasPhone()
			? validator.normalizePhone(request.phone(), "phone")
			: account.getPhone();
		String email = request.hasEmail()
			? validator.normalizeEmail(request.email())
			: account.getEmail();
		LocalDate birthDate = request.hasBirthDate()
			? validator.validateBirthDate(request.birthDate())
			: account.getBirthDate();
		String profileImageUrl = request.hasProfileImageUrl()
			? validator.normalizeOptional(request.profileImageUrl(), "profileImageUrl", null)
			: profile.getProfileImageUrl();
		String fitnessGoal = request.hasFitnessGoal()
			? validator.normalizeOptional(request.fitnessGoal(), "fitnessGoal", null)
			: profile.getFitnessGoal();
		String emergencyContactName = request.hasEmergencyContactName()
			? validator.normalizeOptional(request.emergencyContactName(), "emergencyContactName", 200)
			: profile.getEmergencyContactName();
		String emergencyContactPhone = request.hasEmergencyContactPhone()
			? validator.normalizeOptionalPhone(request.emergencyContactPhone())
			: profile.getEmergencyContactPhone();

		validator.validateEmergencyContact(emergencyContactName, emergencyContactPhone);
		identifierAvailability.ensureAvailableForUpdate(accountId, account.getEmail(), account.getPhone(), email,
			phone);

		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		profile.updateDetails(profileImageUrl, fitnessGoal, emergencyContactName, emergencyContactPhone);
		try {
			accountRepository.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}
		return toResponse(account, profile);
	}

	private Account loadMemberAccount(UUID accountId) {
		Account account = accountRepository.findById(accountId)
			.orElseThrow(() -> new IllegalStateException("Authenticated account no longer exists"));
		if (account.getRole() != AccountRole.MEMBER) {
			throw new IllegalStateException("Member profile requested for a non-member account");
		}
		return account;
	}

	private MemberProfile loadProfile(UUID accountId) {
		return memberProfileRepository.findById(accountId)
			.orElseThrow(() -> new IllegalStateException("Member profile is missing"));
	}

	private MemberProfileResponse toResponse(Account account, MemberProfile profile) {
		return new MemberProfileResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus(),
			profile.getProfileImageUrl(), profile.getFitnessGoal(), profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone());
	}
}
