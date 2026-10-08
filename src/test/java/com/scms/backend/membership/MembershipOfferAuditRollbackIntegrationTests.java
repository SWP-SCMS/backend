package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class MembershipOfferAuditRollbackIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MembershipOfferAdminService service;
	@Autowired MembershipOfferRepository offers;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void auditFailureRollsBackMembershipOfferCreation() {
		Account manager = saveManager();
		long before = offers.count();
		String constraint = "chk_test_reject_membership_offer_audit";
		jdbc.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'MEMBERSHIP_OFFER_CREATED')");

		try {
			assertThatThrownBy(() -> service.create(manager.getId(), new MembershipOfferAdminRequest(
				MembershipPlanCode.BASIC, "Rollback offer", "Must roll back", BigInteger.valueOf(500000), 30,
				MembershipOfferStatus.ACTIVE)))
				.rootCause().hasMessageContaining(constraint);
			assertThat(offers.count()).isEqualTo(before);
		}
		finally {
			jdbc.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	private Account saveManager() {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, AccountRole.MANAGER, AccountStatus.ACTIVE,
			"Rollback manager", "09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
