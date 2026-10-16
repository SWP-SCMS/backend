package com.scms.backend.support;

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
import com.scms.backend.auth.PasswordConstraints;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitialManagerProvisioner implements ApplicationRunner {

	private static final String LOCK_SQL = "select pg_advisory_xact_lock(6003384269632463698)";

	private final InitialManagerProperties properties;
	private final AccountRepository accounts;
	private final AccountIdentifierAvailability identifiers;
	private final AuditEventRepository audits;
	private final PasswordEncoder encoder;
	private final JdbcTemplate db;
	private final Clock clock;

	InitialManagerProvisioner(InitialManagerProperties properties, AccountRepository accounts,
			AccountIdentifierAvailability identifiers, AuditEventRepository audits, PasswordEncoder encoder,
			JdbcTemplate db, Clock clock) {
		this.properties = properties;
		this.accounts = accounts;
		this.identifiers = identifiers;
		this.audits = audits;
		this.encoder = encoder;
		this.db = db;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments ignored) {
		run();
	}

	void run() {
		if (!properties.enabled()) return;
		validate();
		db.execute(LOCK_SQL);
		if (accounts.existsByRole(AccountRole.MANAGER)) return;

		identifiers.ensureAvailableForRegistration(properties.email(), properties.phone());
		UUID accountId = UUID.randomUUID();
		Account manager = new Account(accountId, AccountRole.MANAGER, AccountStatus.ACTIVE, properties.fullName(),
			properties.phone(), properties.email(), properties.birthDate(), encoder.encode(properties.password()));
		try {
			accounts.saveAndFlush(manager);
		}
		catch (DataIntegrityViolationException exception) {
			throw identifiers.fromDatabase(exception);
		}
		audits.save(new AuditEvent(UUID.randomUUID(), "INITIAL_MANAGER_PROVISIONED", "ACCOUNT", accountId,
			Map.of("accountId", accountId.toString(), "fullName", manager.getFullName(), "email", manager.getEmail(),
				"birthDate", manager.getBirthDate().toString(), "role", manager.getRole().name(), "status",
				manager.getStatus().name())));
	}

	private void validate() {
		if (properties.fullName() == null || properties.fullName().isBlank() || properties.fullName().length() > 200)
			throw invalid("INITIAL_MANAGER_FULL_NAME");
		if (properties.phone() == null || !properties.phone().matches("0[0-9]{9}"))
			throw invalid("INITIAL_MANAGER_PHONE");
		if (properties.email() == null || !properties.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
			throw invalid("INITIAL_MANAGER_EMAIL");
		if (properties.birthDate() == null || properties.birthDate().isAfter(LocalDate.now(clock)))
			throw invalid("INITIAL_MANAGER_BIRTH_DATE");
		if (properties.password() == null || properties.password().length() < 12
				|| !PasswordConstraints.fitsBcrypt(properties.password()))
			throw invalid("INITIAL_MANAGER_PASSWORD");
	}

	private IllegalStateException invalid(String variable) {
		return new IllegalStateException(variable + " is invalid while initial Manager provisioning is enabled");
	}
}
