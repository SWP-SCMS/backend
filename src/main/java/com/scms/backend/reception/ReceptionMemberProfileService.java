package com.scms.backend.reception;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.member.MemberProfileDetailsValidator;
import com.scms.backend.member.MemberProfilePatchRequest;
import com.scms.backend.member.MemberProfileResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceptionMemberProfileService {

	private static final Set<AccountStatus> VISIBLE_MEMBER_STATUSES =
		Set.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final MemberProfileDetailsValidator validator;

	ReceptionMemberProfileService(AccountRepository accountRepository,
			MemberProfileRepository memberProfileRepository,
			AccountIdentifierAvailability identifierAvailability,
			MemberProfileDetailsValidator validator) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.identifierAvailability = identifierAvailability;
		this.validator = validator;
	}

	@Transactional(readOnly = true)
	ReceptionMemberSearchResponse search(UUID receptionistAccountId, String memberId, String phone) {
		ensureActiveReceptionist(receptionistAccountId);
		if ((memberId == null) == (phone == null)) {
			throw new ReceptionMemberValidationException("criteria",
				"exactly one of memberId or phone must be provided");
		}

		MemberProfile profile;
		if (memberId != null) {
			String normalizedMemberId = validator.normalizeRequired(memberId, "memberId", 20);
			profile = findByMemberId(normalizedMemberId);
		}
		else {
			String normalizedPhone = validator.normalizePhone(phone, "phone");
			profile = memberProfileRepository.findVisibleMemberByPhone(normalizedPhone, AccountRole.MEMBER,
				VISIBLE_MEMBER_STATUSES).orElseThrow(ReceptionMemberNotFoundException::new);
		}
		return toSearchResponse(profile);
	}

	@Transactional(readOnly = true)
	MemberProfileResponse getProfile(UUID receptionistAccountId, String memberId) {
		ensureActiveReceptionist(receptionistAccountId);
		return toProfileResponse(findByMemberId(normalizeMemberId(memberId)));
	}

	@Transactional
	MemberProfileResponse updateProfile(UUID receptionistAccountId, String memberId,
			MemberProfilePatchRequest request) {
		ensureActiveReceptionist(receptionistAccountId);
		MemberProfile profile = findByMemberId(normalizeMemberId(memberId));
		Account account = profile.getAccount();

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
		identifierAvailability.ensureAvailableForUpdate(account.getId(), account.getEmail(), account.getPhone(),
			email, phone);

		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		profile.updateDetails(profileImageUrl, fitnessGoal, emergencyContactName, emergencyContactPhone);
		try {
			accountRepository.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}
		return toProfileResponse(profile);
	}

	private String normalizeMemberId(String memberId) {
		if (memberId == null || memberId.trim().isEmpty()) {
			throw new ReceptionMemberNotFoundException();
		}
		return memberId.trim();
	}

	private void ensureActiveReceptionist(UUID accountId) {
		if (!accountRepository.existsByIdAndRoleAndStatus(accountId, AccountRole.RECEPTIONIST,
				AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private MemberProfile findByMemberId(String memberId) {
		return memberProfileRepository.findVisibleMemberByCode(memberId, AccountRole.MEMBER,
			VISIBLE_MEMBER_STATUSES).orElseThrow(ReceptionMemberNotFoundException::new);
	}

	private ReceptionMemberSearchResponse toSearchResponse(MemberProfile profile) {
		Account account = profile.getAccount();
		return new ReceptionMemberSearchResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), profile.getProfileImageUrl(),
			account.getStatus());
	}

	private MemberProfileResponse toProfileResponse(MemberProfile profile) {
		Account account = profile.getAccount();
		return new MemberProfileResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus(),
			profile.getProfileImageUrl(), profile.getFitnessGoal(), profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone());
	}
}
