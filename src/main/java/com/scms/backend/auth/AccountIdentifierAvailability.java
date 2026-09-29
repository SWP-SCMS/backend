package com.scms.backend.auth;

import java.util.Locale;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class AccountIdentifierAvailability {

	private final AccountRepository accountRepository;

	AccountIdentifierAvailability(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public void ensureAvailableForRegistration(String email, String phone) {
		if (accountRepository.existsByEmailIgnoreCaseAndStatusNot(email, AccountStatus.INACTIVE)) {
			throw DuplicateAccountException.email();
		}
		if (accountRepository.existsByPhoneAndStatusNot(phone, AccountStatus.INACTIVE)) {
			throw DuplicateAccountException.phone();
		}
	}

	public void ensureAvailableForUpdate(UUID accountId, String currentEmail, String currentPhone, String email,
			String phone) {
		if (!email.equals(currentEmail)
				&& accountRepository.existsByEmailIgnoreCaseAndStatusNotAndIdNot(email, AccountStatus.INACTIVE,
					accountId)) {
			throw DuplicateAccountException.email();
		}
		if (!phone.equals(currentPhone)
				&& accountRepository.existsByPhoneAndStatusNotAndIdNot(phone, AccountStatus.INACTIVE, accountId)) {
			throw DuplicateAccountException.phone();
		}
	}

	public DuplicateAccountException fromDatabase(DataIntegrityViolationException exception) {
		String messages = exceptionMessages(exception).toLowerCase(Locale.ROOT);
		if (messages.contains("uq_accounts_current_email")) {
			return DuplicateAccountException.email();
		}
		if (messages.contains("uq_accounts_current_phone")) {
			return DuplicateAccountException.phone();
		}
		return DuplicateAccountException.identifier();
	}

	private String exceptionMessages(Throwable exception) {
		StringBuilder messages = new StringBuilder();
		Throwable current = exception;
		while (current != null) {
			if (current.getMessage() != null) {
				messages.append(' ').append(current.getMessage());
			}
			current = current.getCause();
		}
		return messages.toString();
	}
}
