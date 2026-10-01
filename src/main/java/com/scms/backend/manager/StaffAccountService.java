package com.scms.backend.manager;

import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAccountService {

	private final AccountRepository accountRepository;

	StaffAccountService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	@Transactional(readOnly = true)
	StaffAccountPageResponse search(UUID managerId, String query, AccountRole role, AccountStatus status,
			Pageable pageable) {
		if (!accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		String normalizedQuery = normalizeQuery(query);
		Page<Account> accounts = accountRepository.searchStaff(normalizedQuery, role, status, pageable);
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

	private StaffAccountResponse toResponse(Account account) {
		return new StaffAccountResponse(account.getId(), account.getRole(), account.getStatus(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate());
	}
}
