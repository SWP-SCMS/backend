package com.scms.backend.manager;

import java.time.LocalDate;
import java.util.Locale;
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

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ManagerMemberService {

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final MemberProfileDetailsValidator validator;

	ManagerMemberService(AccountRepository accountRepository, MemberProfileRepository memberProfileRepository,
			AccountIdentifierAvailability identifierAvailability, MemberProfileDetailsValidator validator) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.identifierAvailability = identifierAvailability;
		this.validator = validator;
	}

	@Transactional(readOnly = true)
	MemberAccountPageResponse search(UUID managerId, String query, AccountStatus status, Pageable pageable) {
		ensureActiveManager(managerId);
		String normalizedQuery = normalizeQuery(query);
		Specification<MemberProfile> filters = (root, ignored, builder) ->
			builder.equal(root.get("account").get("role"), AccountRole.MEMBER);
		if (status != null) {
			filters = filters.and((root, ignored, builder) ->
				builder.equal(root.get("account").get("status"), status));
		}
		if (normalizedQuery != null) {
			String pattern = "%" + normalizedQuery.toLowerCase(Locale.ROOT) + "%";
			filters = filters.and((root, ignored, builder) -> builder.or(
				builder.like(builder.lower(root.get("memberCode")), pattern),
				builder.like(builder.lower(root.get("account").get("fullName")), pattern),
				builder.like(builder.lower(root.get("account").get("email")), pattern),
				builder.like(root.get("account").get("phone"), pattern)));
		}
		Page<MemberProfile> members = memberProfileRepository.findAll(filters, pageable);
		return new MemberAccountPageResponse(members.getContent().stream().map(this::toResponse).toList(),
			members.getNumber(), members.getSize(), members.getTotalElements(), members.getTotalPages());
	}

	@Transactional(readOnly = true)
	MemberAccountResponse get(UUID managerId, UUID accountId) {
		ensureActiveManager(managerId);
		return toResponse(findMember(accountId));
	}

	@Transactional
	MemberAccountResponse update(UUID managerId, UUID accountId, MemberProfilePatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null) {
			throw new StaffAccountValidationException("request", "must be a JSON object");
		}
		MemberProfile profile = findMember(accountId);
		Account account = profile.getAccount();
		String fullName = request.hasFullName()
			? validator.normalizeRequired(request.fullName(), "fullName", 200) : account.getFullName();
		String phone = request.hasPhone()
			? validator.normalizePhone(request.phone(), "phone") : account.getPhone();
		String email = request.hasEmail() ? validator.normalizeEmail(request.email()) : account.getEmail();
		LocalDate birthDate = request.hasBirthDate()
			? validator.validateBirthDate(request.birthDate()) : account.getBirthDate();
		String profileImageUrl = request.hasProfileImageUrl()
			? validator.normalizeOptional(request.profileImageUrl(), "profileImageUrl", null)
			: profile.getProfileImageUrl();
		String fitnessGoal = request.hasFitnessGoal()
			? validator.normalizeOptional(request.fitnessGoal(), "fitnessGoal", null) : profile.getFitnessGoal();
		String emergencyContactName = request.hasEmergencyContactName()
			? validator.normalizeOptional(request.emergencyContactName(), "emergencyContactName", 200)
			: profile.getEmergencyContactName();
		String emergencyContactPhone = request.hasEmergencyContactPhone()
			? validator.normalizeOptionalPhone(request.emergencyContactPhone()) : profile.getEmergencyContactPhone();

		validator.validateEmergencyContact(emergencyContactName, emergencyContactPhone);
		identifierAvailability.ensureAvailableForUpdate(account.getId(), account.getEmail(), account.getPhone(),
			email, phone);
		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		profile.updateDetails(profileImageUrl, fitnessGoal, emergencyContactName, emergencyContactPhone);
		try {
			accountRepository.flush();
			memberProfileRepository.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}
		return toResponse(profile);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private MemberProfile findMember(UUID accountId) {
		return memberProfileRepository.findById(accountId)
			.filter(profile -> profile.getAccount().getRole() == AccountRole.MEMBER)
			.orElseThrow(MemberAccountNotFoundException::new);
	}

	private String normalizeQuery(String query) {
		if (query == null || query.trim().isEmpty()) {
			return null;
		}
		String normalized = query.trim();
		if (normalized.length() > 100) {
			throw new StaffAccountValidationException("query", "must be at most 100 characters");
		}
		return normalized;
	}

	private MemberAccountResponse toResponse(MemberProfile profile) {
		Account account = profile.getAccount();
		return new MemberAccountResponse(account.getId(), profile.getMemberCode(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus(),
			profile.getProfileImageUrl(), profile.getFitnessGoal(), profile.getEmergencyContactName(),
			profile.getEmergencyContactPhone());
	}
}
