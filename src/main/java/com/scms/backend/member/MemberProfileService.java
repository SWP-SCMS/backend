package com.scms.backend.member;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

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

	private static final Pattern PHONE_PATTERN = Pattern.compile("0[0-9]{9}");
	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final Clock clock;

	MemberProfileService(AccountRepository accountRepository, MemberProfileRepository memberProfileRepository,
			AccountIdentifierAvailability identifierAvailability, Clock clock) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.identifierAvailability = identifierAvailability;
		this.clock = clock;
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
			? normalizeRequired(request.fullName(), "fullName", 200)
			: account.getFullName();
		String phone = request.hasPhone()
			? normalizePhone(request.phone(), "phone")
			: account.getPhone();
		String email = request.hasEmail()
			? normalizeEmail(request.email())
			: account.getEmail();
		LocalDate birthDate = request.hasBirthDate()
			? validateBirthDate(request.birthDate())
			: account.getBirthDate();
		String profileImageUrl = request.hasProfileImageUrl()
			? normalizeOptional(request.profileImageUrl(), "profileImageUrl", null)
			: profile.getProfileImageUrl();
		String fitnessGoal = request.hasFitnessGoal()
			? normalizeOptional(request.fitnessGoal(), "fitnessGoal", null)
			: profile.getFitnessGoal();
		String emergencyContactName = request.hasEmergencyContactName()
			? normalizeOptional(request.emergencyContactName(), "emergencyContactName", 200)
			: profile.getEmergencyContactName();
		String emergencyContactPhone = request.hasEmergencyContactPhone()
			? normalizeOptionalPhone(request.emergencyContactPhone())
			: profile.getEmergencyContactPhone();

		validateEmergencyContact(emergencyContactName, emergencyContactPhone);
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

	private String normalizeRequired(String value, String field, int maxLength) {
		if (value == null) {
			throw invalid(field, "must not be null");
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw invalid(field, "must not be blank");
		}
		if (normalized.length() > maxLength) {
			throw invalid(field, "must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	private String normalizePhone(String value, String field) {
		String normalized = normalizeRequired(value, field, 32);
		if (!PHONE_PATTERN.matcher(normalized).matches()) {
			throw invalid(field, "must be 10 digits starting with 0");
		}
		return normalized;
	}

	private String normalizeEmail(String value) {
		String normalized = normalizeRequired(value, "email", 320).toLowerCase(Locale.ROOT);
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			throw invalid("email", "must be a well-formed email address");
		}
		return normalized;
	}

	private LocalDate validateBirthDate(LocalDate value) {
		if (value == null) {
			throw invalid("birthDate", "must not be null");
		}
		if (value.isAfter(LocalDate.now(clock))) {
			throw invalid("birthDate", "must not be in the future");
		}
		return value;
	}

	private String normalizeOptional(String value, String field, Integer maxLength) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			throw invalid(field, "must be null or non-blank");
		}
		if (maxLength != null && normalized.length() > maxLength) {
			throw invalid(field, "must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	private String normalizeOptionalPhone(String value) {
		if (value == null) {
			return null;
		}
		return normalizePhone(value, "emergencyContactPhone");
	}

	private void validateEmergencyContact(String name, String phone) {
		if ((name == null) != (phone == null)) {
			throw invalid("emergencyContact", "name and phone must both be provided or both be null");
		}
	}

	private MemberProfileValidationException invalid(String field, String message) {
		return new MemberProfileValidationException(field, message);
	}

	private MemberProfileResponse toResponse(Account account, MemberProfile profile) {
		return new MemberProfileResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus(),
			profile.getProfileImageUrl(), profile.getFitnessGoal(), profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone());
	}
}
