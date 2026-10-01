package com.scms.backend.manager;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.Notification;
import com.scms.backend.notification.NotificationRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffAccountCreateService {

	private final AccountRepository accountRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final AuditEventRepository auditRepository;
	private final NotificationRepository notificationRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	StaffAccountCreateService(AccountRepository accountRepository, AccountIdentifierAvailability identifierAvailability,
			AuditEventRepository auditRepository, NotificationRepository notificationRepository,
			PasswordEncoder passwordEncoder, Clock clock) {
		this.accountRepository = accountRepository;
		this.identifierAvailability = identifierAvailability;
		this.auditRepository = auditRepository;
		this.notificationRepository = notificationRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional
	StaffAccountResponse create(UUID managerId, StaffAccountCreateRequest request) {
		ensureActiveManager(managerId);
		validate(request);
		identifierAvailability.ensureAvailableForRegistration(request.email(), request.phone());
		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, request.role(), AccountStatus.ACTIVE, request.fullName(), request.phone(),
			request.email(), request.birthDate(), passwordEncoder.encode(request.phone()));
		try {
			accountRepository.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}
		Map<String, Object> snapshot = Map.of("accountId", accountId.toString(), "fullName", account.getFullName(),
			"phone", account.getPhone(), "email", account.getEmail(), "birthDate", account.getBirthDate().toString(),
			"role", account.getRole().name(), "status", account.getStatus().name());
		auditRepository.save(new AuditEvent(UUID.randomUUID(), managerId, "ACCOUNT_CREATED", "ACCOUNT", accountId,
			snapshot));
		notificationRepository.save(new Notification(UUID.randomUUID(), accountId, "ACCOUNT_CREATED", "ACCOUNT", accountId,
			Map.of("accountId", accountId.toString(), "role", account.getRole().name())));
		return new StaffAccountResponse(accountId, account.getRole(), account.getStatus(), account.getFullName(),
			account.getPhone(), account.getEmail(), account.getBirthDate());
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void validate(StaffAccountCreateRequest request) {
		if (request == null) throw new StaffAccountValidationException("request", "must be a JSON object");
		if (request.fullName() == null || request.fullName().isBlank() || request.fullName().length() > 200)
			throw new StaffAccountValidationException("fullName", "must be non-blank and at most 200 characters");
		if (request.phone() == null || !request.phone().matches("0[0-9]{9}"))
			throw new StaffAccountValidationException("phone", "must be 10 digits starting with 0");
		if (request.email() == null || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
			throw new StaffAccountValidationException("email", "must be a well-formed email address");
		if (request.birthDate() == null || request.birthDate().isAfter(LocalDate.now(clock)))
			throw new StaffAccountValidationException("birthDate", "must not be in the future");
		if (request.role() == null || request.role() == AccountRole.MEMBER)
			throw new StaffAccountValidationException("role", "must be COACH, RECEPTIONIST or MANAGER");
	}
}
