package com.scms.backend.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(properties = {
	"scms.initial-manager.enabled=true",
	"scms.initial-manager.full-name=Initial Manager",
	"scms.initial-manager.phone=0901234567",
	"scms.initial-manager.email=initial.manager@example.com",
	"scms.initial-manager.birth-date=1990-01-01",
	"scms.initial-manager.password=a-strong-bootstrap-password"
})
@ActiveProfiles("test")
class InitialManagerProvisioningIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired AccountRepository accounts;
	@Autowired AuditEventRepository audits;
	@Autowired PasswordEncoder encoder;
	@Autowired InitialManagerProvisioner provisioner;

	@Test
	void startupProvisioningIsSecureAuditedAndIdempotent() throws Exception {
		Account manager = accounts.findAll().stream()
			.filter(account -> account.getRole() == AccountRole.MANAGER)
			.findFirst()
			.orElseThrow();

		assertThat(manager.getStatus()).isEqualTo(AccountStatus.ACTIVE);
		assertThat(manager.getEmail()).isEqualTo("initial.manager@example.com");
		assertThat(manager.getPasswordHash()).isNotEqualTo("a-strong-bootstrap-password");
		assertThat(encoder.matches("a-strong-bootstrap-password", manager.getPasswordHash())).isTrue();

		AuditEvent audit = audits.findAll().stream()
			.filter(event -> "INITIAL_MANAGER_PROVISIONED".equals(event.getAction()))
			.findFirst()
			.orElseThrow();
		assertThat(audit.getActorAccountId()).isNull();
		assertThat(audit.getTargetId()).isEqualTo(manager.getId());
		assertThat(audit.getAfterData()).doesNotContainKeys("password", "passwordHash", "credential");
		assertThat(audit.getAfterData().values())
			.doesNotContain("a-strong-bootstrap-password", manager.getPasswordHash());

		provisioner.run(new DefaultApplicationArguments());

		assertThat(accounts.findAll()).filteredOn(account -> account.getRole() == AccountRole.MANAGER).hasSize(1);
		assertThat(audits.findAll()).filteredOn(event -> "INITIAL_MANAGER_PROVISIONED".equals(event.getAction()))
			.hasSize(1);
	}
}
