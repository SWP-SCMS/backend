package com.scms.backend.auth;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.common.validation.PersonNameRules;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.emailverification.EmailVerificationRegistration;
import com.scms.backend.emailverification.EmailVerificationService;

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
	private final NotificationWriter notificationWriter;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final AccountIdentifierAvailability identifierAvailability;
	private final EmailVerificationService emailVerification;

	RegistrationService(AccountRepository accountRepository, MemberProfileRepository memberProfileRepository,
			AuditEventRepository auditEventRepository, NotificationWriter notificationWriter,
			PasswordEncoder passwordEncoder, Clock clock, AccountIdentifierAvailability identifierAvailability,
			EmailVerificationService emailVerification) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.auditEventRepository = auditEventRepository;
		this.notificationWriter = notificationWriter;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.identifierAvailability = identifierAvailability;
		this.emailVerification = emailVerification;
	}

	@Transactional
	RegistrationOutcome register(RegistrationRequest request) { return register(request, "internal"); }

	@Transactional
	RegistrationOutcome register(RegistrationRequest request, String remoteAddress) {
		if (!PersonNameRules.isValid(request.fullName())) {
			throw new RegistrationValidationException("fullName", "must contain letters and valid name separators only");
		}
		validatePassword(request.password());
		validateBirthDate(request.birthDate());
		emailVerification.rateLimitRegister(remoteAddress, request.email(), request.phone());
		if (emailVerification.enabled()) {
			var pending=emailVerification.pendingConflict(request.email());
			if (pending!=null) throw pending;
		}
		identifierAvailability.ensureAvailableForRegistration(request.email(), request.phone());

		Instant now=clock.instant();
		Instant registrationExpiresAt=emailVerification.enabled()?emailVerification.registrationExpiry():null;
		String passwordHash=passwordEncoder.encode(request.password());
		Account account; boolean revived=false;
		if(emailVerification.enabled()) {
			var expired=accountRepository.findExpiredSelfRegistrationForUpdate(request.email(),request.phone());
			if(expired.isEmpty()) account=Account.pendingSelfRegistration(UUID.randomUUID(),request.fullName(),request.phone(),request.email(),request.birthDate(),passwordHash,registrationExpiresAt);
			else { account=expired.getFirst();account.reviveSelfRegistration(request.fullName(),request.phone(),request.email(),request.birthDate(),passwordHash,registrationExpiresAt);revived=true; }
		} else account=new Account(UUID.randomUUID(),AccountRole.MEMBER,AccountStatus.ACTIVE,request.fullName(),request.phone(),request.email(),request.birthDate(),passwordHash);
		UUID accountId=account.getId();
		try {
			accountRepository.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException exception) {
			throw identifierAvailability.fromDatabase(exception);
		}

		MemberProfile profile;
		if(revived){profile=memberProfileRepository.findById(accountId).orElseThrow();profile.updateDetails(request.profileImageUrl(),request.fitnessGoal(),null,null);profile=memberProfileRepository.saveAndFlush(profile);}
		else profile=memberProfileRepository.saveAndFlush(new MemberProfile(account, request.profileImageUrl(), request.fitnessGoal()));
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
		String createdAction=revived?"ACCOUNT_REGISTRATION_REVIVED":emailVerification.enabled()?"ACCOUNT_CREATED_PENDING_EMAIL_VERIFICATION":ACCOUNT_CREATED;
		auditEventRepository.save(new AuditEvent(UUID.randomUUID(), createdAction, ACCOUNT, accountId,
			accountSnapshot));
		if (!emailVerification.enabled()) {
			notificationWriter.write("ACCOUNT_CREATED:" + accountId, accountId, ACCOUNT_CREATED, ACCOUNT, accountId,
				Map.of("memberId", memberId));
			return new RegistrationOutcome(new RegistrationResponse(false,AccountStatus.ACTIVE,
				EmailVerificationService.mask(account.getEmail()),null,null,null,null,now),null);
		}
		EmailVerificationRegistration verification=emailVerification.begin(accountId,account.getEmail(),registrationExpiresAt);
		return new RegistrationOutcome(new RegistrationResponse(true,AccountStatus.INACTIVE,
			EmailVerificationService.mask(account.getEmail()),verification.csrfToken(),verification.otpExpiresAt(),
			verification.resendAvailableAt(),verification.registrationExpiresAt(),verification.serverTime()),
			verification.sessionToken());
	}

	private void validatePassword(String password) {
		PasswordConstraints.validate("password", password, RegistrationValidationException::new);
	}

	private void validateBirthDate(LocalDate birthDate) {
		if (birthDate == null || birthDate.isAfter(LocalDate.now(clock))) {
			throw new RegistrationValidationException("birthDate", "must not be in the future");
		}
		if (birthDate.plusYears(16).isAfter(LocalDate.now(clock))) {
			throw new RegistrationValidationException("birthDate", "member must be at least 16 years old");
		}
	}

}
