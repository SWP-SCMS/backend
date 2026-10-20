package com.scms.backend.identity.staffprofile;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.emailverification.EmailVerificationService;
import com.scms.backend.identity.memberprofile.MemberProfileDetailsValidator;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffProfileService {
	private final AccountRepository accounts;
	private final AccountIdentifierAvailability identifiers;
	private final MemberProfileDetailsValidator validator;
	private final AuditEventRepository audits;

	StaffProfileService(AccountRepository accounts, AccountIdentifierAvailability identifiers,
			MemberProfileDetailsValidator validator, AuditEventRepository audits) {
		this.accounts = accounts;
		this.identifiers = identifiers;
		this.validator = validator;
		this.audits = audits;
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
		if (Objects.equals(fullName, account.getFullName()) && Objects.equals(phone, account.getPhone())
				&& Objects.equals(email, account.getEmail()) && Objects.equals(birthDate, account.getBirthDate())) {
			return toResponse(account);
		}
		Map<String, Object> beforeData = snapshot(account.getFullName(), account.getPhone(), account.getEmail(),
			account.getBirthDate());
		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		try {
			accounts.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw identifiers.fromDatabase(exception);
		}
		audits.save(new AuditEvent(UUID.randomUUID(), accountId, "STAFF_SELF_PROFILE_UPDATED", "ACCOUNT", accountId,
			null, beforeData, snapshot(fullName, phone, email, birthDate)));
		return toResponse(account);
	}

	private Map<String, Object> snapshot(String fullName, String phone, String email, LocalDate birthDate) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("fullName", fullName);
		data.put("phone", "*".repeat(Math.max(0, phone.length() - 4)) + phone.substring(Math.max(0, phone.length() - 4)));
		data.put("email", EmailVerificationService.mask(email));
		data.put("birthDate", birthDate.toString());
		return data;
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
