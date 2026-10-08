package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import jakarta.persistence.EntityManager;
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
	@Autowired MembershipPlanRepository plans;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;
	@Autowired EntityManager entityManager;

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

	@Test
	void auditFailureRollsBackMembershipOfferUpdate() {
		Account creator = saveManager();
		MembershipOffer offer = offers.saveAndFlush(new MembershipOffer(UUID.randomUUID(),
			plans.findById(MembershipPlanCode.BASIC).orElseThrow(), "Original offer", "Original description",
			BigInteger.valueOf(500000), "VND", 30, MembershipOfferStatus.ACTIVE, creator.getId()));
		long originalVersion = ((Number) membershipOfferRow(offer.getId()).get("version")).longValue();
		String constraint = "chk_test_reject_membership_offer_update_audit";
		jdbc.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'MEMBERSHIP_OFFER_UPDATED')");

		try {
			assertThatThrownBy(() -> service.update(creator.getId(), offer.getId(),
				new MembershipOfferPatchRequest(null, "Changed offer", "Changed description",
					BigInteger.valueOf(900000), 60, MembershipOfferStatus.INACTIVE)))
				.rootCause().hasMessageContaining(constraint);
			entityManager.clear();

			Map<String, Object> row = membershipOfferRow(offer.getId());
			assertThat(row).containsEntry("plan_code", "BASIC")
				.containsEntry("name", "Original offer")
				.containsEntry("description", "Original description")
				.containsEntry("currency_code", "VND")
				.containsEntry("duration_days", 30)
				.containsEntry("status", "ACTIVE")
				.containsEntry("created_by_account_id", creator.getId());
			assertThat(((Number) row.get("price_amount")).longValue()).isEqualTo(500000);
			assertThat(((Number) row.get("version")).longValue()).isEqualTo(originalVersion);
			assertNoAudit("MEMBERSHIP_OFFER_UPDATED", offer.getId());
		}
		finally {
			jdbc.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	private Map<String, Object> membershipOfferRow(UUID offerId) {
		return jdbc.queryForMap("""
			select plan_code::text as plan_code, name, description, price_amount,
				currency_code::text as currency_code, duration_days, status::text as status,
				created_by_account_id, version
			from membership_offers where id = ?
			""", offerId);
	}

	private void assertNoAudit(String action, UUID targetId) {
		assertThat(jdbc.queryForObject(
			"select count(*) from audit_events where action = ? and target_id = ?", Long.class, action, targetId))
			.isZero();
	}

	private Account saveManager() {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, AccountRole.MANAGER, AccountStatus.ACTIVE,
			"Rollback manager", "09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
}
