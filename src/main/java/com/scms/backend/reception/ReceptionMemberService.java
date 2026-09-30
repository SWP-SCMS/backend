package com.scms.backend.reception;

import java.time.Clock;
import java.time.LocalDate;
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
import com.scms.backend.auth.AccountIdentifierAvailability;
import com.scms.backend.auth.RegistrationResponse;
import com.scms.backend.auth.RegistrationValidationException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReceptionMemberService {

	private static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
	private static final String ACCOUNT = "ACCOUNT";

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final AuditEventRepository auditEventRepository;
	private final AccountIdentifierAvailability identifierAvailability;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final ApplicationEventPublisher eventPublisher;

	ReceptionMemberService(AccountRepository accountRepository, MemberProfileRepository memberProfileRepository,
			AuditEventRepository auditEventRepository, AccountIdentifierAvailability identifierAvailability,
			PasswordEncoder passwordEncoder, Clock clock, ApplicationEventPublisher eventPublisher) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.auditEventRepository = auditEventRepository;
		this.identifierAvailability = identifierAvailability;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	RegistrationResponse createMember(UUID receptionistAccountId, ReceptionMemberCreateRequest request) {
		validateBirthDate(request.birthDate());
		identifierAvailability.ensureAvailableForRegistration(request.email(), request.phone());

		UUID accountId = UUID.randomUUID();
		Account account = new Account(accountId, AccountRole.MEMBER, AccountStatus.ACTIVE, request.fullName(),
			request.phone(), request.email(), request.birthDate(), passwordEncoder.encode(request.phone()));
		try {
			accountRepository.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}

		MemberProfile profile = memberProfileRepository.saveAndFlush(
			new MemberProfile(account, request.profileImageUrl(), null));
		String memberId = profile.getMemberCode();
		Map<String, Object> accountSnapshot = Map.of(
			"accountId", accountId.toString(),
			"memberId", memberId,
			"fullName", account.getFullName(),
			"email", account.getEmail(),
			"birthDate", account.getBirthDate().toString(),
			"role", account.getRole().name(),
			"status", account.getStatus().name());

		eventPublisher.publishEvent(new MemberCreatedEvent(accountId, memberId));
		auditEventRepository.saveAndFlush(new AuditEvent(UUID.randomUUID(), receptionistAccountId, ACCOUNT_CREATED,
			ACCOUNT, accountId, accountSnapshot));

		return new RegistrationResponse(accountId, memberId, account.getFullName(), account.getPhone(),
			account.getEmail(), account.getBirthDate(), account.getRole(), account.getStatus());
	}

	private void validateBirthDate(LocalDate birthDate) {
		if (birthDate.isAfter(LocalDate.now(clock))) {
			throw new RegistrationValidationException("birthDate", "must not be in the future");
		}
	}
}
