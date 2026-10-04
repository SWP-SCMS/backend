package com.scms.backend.manager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.auth.AccountIdentifierAvailability;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAccountService {

	private final AccountRepository accountRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final Clock clock;

	StaffAccountService(AccountRepository accountRepository, AccountIdentifierAvailability identifierAvailability,
			Clock clock) {
		this.accountRepository = accountRepository;
		this.identifierAvailability = identifierAvailability;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	StaffAccountResponse get(UUID managerId, UUID accountId) {
		ensureActiveManager(managerId);
		return toResponse(findStaff(accountId));
	}

	@Transactional
	StaffAccountResponse update(UUID managerId, UUID accountId, StaffAccountPatchRequest request) {
		ensureActiveManager(managerId);
		if (request == null) {
			throw new StaffAccountValidationException("request", "must be a JSON object");
		}
		Account account = findStaff(accountId);
		String fullName = request.hasFullName() ? normalizeRequired(request.fullName(), "fullName", 200) : account.getFullName();
		String phone = request.hasPhone() ? normalizePhone(request.phone()) : account.getPhone();
		String email = request.hasEmail() ? normalizeEmail(request.email()) : account.getEmail();
		LocalDate birthDate = request.hasBirthDate() ? validateBirthDate(request.birthDate()) : account.getBirthDate();
		identifierAvailability.ensureAvailableForUpdate(account.getId(), account.getEmail(), account.getPhone(), email, phone);
		account.updateMemberProfileDetails(fullName, phone, email, birthDate);
		try {
			accountRepository.flush();
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}
		return toResponse(account);
	}

	@Transactional(readOnly = true)
	StaffAccountPageResponse search(UUID managerId, String query, AccountRole role, AccountStatus status,
			Pageable pageable) {
		if (!accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		String normalizedQuery = normalizeQuery(query);
		if (role == AccountRole.MEMBER) {
			throw new StaffAccountValidationException("role", "must be COACH, RECEPTIONIST or MANAGER");
		}
		Specification<Account> filters = (root, ignored, builder) -> root.get("role")
			.in(AccountRole.COACH, AccountRole.RECEPTIONIST, AccountRole.MANAGER);
		if (role != null) {
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("role"), role));
		}
		if (status != null) {
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("status"), status));
		}
		if (normalizedQuery != null) {
			String pattern = "%" + normalizedQuery.toLowerCase(java.util.Locale.ROOT) + "%";
			filters = filters.and((root, ignored, builder) -> builder.or(
				builder.like(builder.lower(root.get("fullName")), pattern),
				builder.like(builder.lower(root.get("email")), pattern),
				builder.like(root.get("phone"), pattern)));
		}
		Page<Account> accounts = accountRepository.findAll(filters, pageable);
		return new StaffAccountPageResponse(accounts.getContent().stream().map(this::toResponse).toList(),
			accounts.getNumber(), accounts.getSize(), accounts.getTotalElements(), accounts.getTotalPages());
	}

	private String normalizeQuery(String query) {
		if (query == null) {
			return null;
		}
		String normalized = query.trim();
		if (normalized.isEmpty()) {
			return null;
		}
		if (normalized.length() > 100) {
			throw new StaffAccountValidationException("query", "must be at most 100 characters");
		}
		return normalized;
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private Account findStaff(UUID accountId) {
		return accountRepository.findById(accountId)
			.filter(account -> account.getRole() != AccountRole.MEMBER)
			.orElseThrow(StaffAccountNotFoundException::new);
	}

	private String normalizeRequired(String value, String field, int maxLength) {
		if (value == null || value.trim().isEmpty() || value.trim().length() > maxLength) {
			throw new StaffAccountValidationException(field, "must be non-blank and at most " + maxLength + " characters");
		}
		return value.trim();
	}

	private String normalizePhone(String value) {
		String normalized = normalizeRequired(value, "phone", 32);
		if (!normalized.matches("0[0-9]{9}")) {
			throw new StaffAccountValidationException("phone", "must be 10 digits starting with 0");
		}
		return normalized;
	}

	private String normalizeEmail(String value) {
		String normalized = normalizeRequired(value, "email", 320).toLowerCase(java.util.Locale.ROOT);
		if (!normalized.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
			throw new StaffAccountValidationException("email", "must be a well-formed email address");
		}
		return normalized;
	}

	private LocalDate validateBirthDate(LocalDate value) {
		if (value == null || value.isAfter(LocalDate.now(clock))) {
			throw new StaffAccountValidationException("birthDate", "must not be in the future");
		}
		return value;
	}

	private StaffAccountResponse toResponse(Account account) {
		return new StaffAccountResponse(account.getId(), account.getRole(), account.getStatus(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate());
	}
}
