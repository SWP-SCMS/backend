package com.scms.backend.auth;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.Notification;
import com.scms.backend.notification.NotificationRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

	private static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
	private static final String ACCOUNT = "ACCOUNT";

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AuditEventRepository auditEventRepository;
	private final NotificationRepository notificationRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	RegistrationService(AccountRepository accountRepository, MemberProfileRepository memberProfileRepository,
			AuditEventRepository auditEventRepository, NotificationRepository notificationRepository,
			PasswordEncoder passwordEncoder, Clock clock) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.auditEventRepository = auditEventRepository;
		this.notificationRepository = notificationRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional
	RegistrationResponse register(RegistrationRequest request) {
		validatePassword(request.password());
		validateBirthDate(request.birthDate());
		ensureIdentifiersAvailable(request.email(), request.phone());

		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, request.fullName(),
			request.phone(), request.email(), request.birthDate(), passwordEncoder.encode(request.password()));
		try {
			accountRepository.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException exception) {
			throw duplicateFrom(exception);
		}

		MemberProfile profile = memberProfileRepository.saveAndFlush(
			new MemberProfile(account, request.profileImageUrl(), request.fitnessGoal()));
		String memberId = profile.getMemberCode();

		Map<String, Object> accountSnapshot = Map.of(
			"accountId", accountId.toString(),
			"memberId", memberId,
			"fullName", account.getFullName(),
			"phone", account.getPhone(),
			"email", account.getEmail(),
			"birthDate", account.getBirthDate().toString(),
			"role", account.getRole().name(),
			"status", account.getStatus().name());
		auditEventRepository.save(new AuditEvent(UUID.randomUUID(), ACCOUNT_CREATED, ACCOUNT, accountId,
			accountSnapshot));
		notificationRepository.save(new Notification(UUID.randomUUID(), accountId, ACCOUNT_CREATED, ACCOUNT,
			accountId, Map.of("memberId", memberId)));

		return new RegistrationResponse(accountId, memberId, account.getFullName(), account.getPhone(),
			account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus());
	}

	private void validatePassword(String password) {
		if (password == null || password.isBlank()) {
			throw new RegistrationValidationException("password", "must not be blank");
		}
		if (password.length() < 8) {
			throw new RegistrationValidationException("password", "size must be at least 8");
		}
	}

	private void validateBirthDate(LocalDate birthDate) {
		if (birthDate.isAfter(LocalDate.now(clock))) {
			throw new RegistrationValidationException("birthDate", "must not be in the future");
		}
	}

	private void ensureIdentifiersAvailable(String email, String phone) {
		if (accountRepository.existsByEmailIgnoreCaseAndStatusNot(email, AccountStatus.INACTIVE)) {
			throw DuplicateAccountException.email();
		}
		if (accountRepository.existsByPhoneAndStatusNot(phone, AccountStatus.INACTIVE)) {
			throw DuplicateAccountException.phone();
		}
	}

	private DuplicateAccountException duplicateFrom(DataIntegrityViolationException exception) {
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
