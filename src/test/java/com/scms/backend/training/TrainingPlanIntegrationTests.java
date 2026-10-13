package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TimeZone;
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
class TrainingPlanIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired TrainingPlanService trainingPlans;
	@Autowired CoachAssignmentService assignments;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void createPersistsPlanInitialVersionAndAuditAtomically() {
		Account manager = createAccount(AccountRole.MANAGER, "Manager US42");
		Account coach = createAccount(AccountRole.COACH, "Coach US42");
		Account member = createAccount(AccountRole.MEMBER, "Member US42");
		Instant now = Instant.now();
		createPlusMembership(member.getId(), manager.getId(), now.minusSeconds(3600), now.plusSeconds(86400));
		CoachAssignmentResponse assignment = assignments.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "US42 integration assignment"));

		TrainingPlanResponse response = trainingPlans.create(coach.getId(), member.getId(),
				new CreateTrainingPlanRequest("Strength foundation",
						Map.of("goal", "Build strength", "sessionsPerWeek", 3)));

		assertThat(response.coachAssignmentId()).isEqualTo(assignment.id());
		assertThat(jdbc.queryForObject("select title from training_plans where id = ?", String.class, response.id()))
				.isEqualTo("Strength foundation");
		assertThat(jdbc.queryForObject(
				"select version_number from training_plan_versions where id = ?", Integer.class, response.versionId()))
				.isEqualTo(1);
		assertThat(jdbc.queryForObject(
				"select content ->> 'goal' from training_plan_versions where id = ?", String.class, response.versionId()))
				.isEqualTo("Build strength");
		assertThat(jdbc.queryForObject(
				"select count(*) from audit_events where action = 'TRAINING_PLAN_CREATED' and target_id = ?",
				Long.class, response.id())).isEqualTo(1L);
	}

	@Test
	void updatePreservesVersionOneAndAppendsVersionTwo() {
		Account manager = createAccount(AccountRole.MANAGER, "Manager US43");
		Account coach = createAccount(AccountRole.COACH, "Coach US43");
		Account member = createAccount(AccountRole.MEMBER, "Member US43");
		Instant now = Instant.now();
		createPlusMembership(member.getId(), manager.getId(), now.minusSeconds(3600), now.plusSeconds(86400));
		assignments.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "US43 integration assignment"));
		TrainingPlanResponse created = trainingPlans.create(coach.getId(), member.getId(),
				new CreateTrainingPlanRequest("Foundation", Map.of("goal", "Foundation")));

		TrainingPlanResponse updated = trainingPlans.update(coach.getId(), member.getId(), created.id(),
				new UpdateTrainingPlanRequest("Progressive strength", Map.of("goal", "Progressive overload")));

		assertThat(updated.versionNumber()).isEqualTo(2);
		assertThat(jdbc.queryForObject("select title from training_plans where id = ?", String.class, created.id()))
				.isEqualTo("Progressive strength");
		assertThat(jdbc.queryForObject(
				"select count(*) from training_plan_versions where training_plan_id = ?", Long.class, created.id()))
				.isEqualTo(2L);
		assertThat(jdbc.queryForObject(
				"select content ->> 'goal' from training_plan_versions where id = ?", String.class,
				created.versionId())).isEqualTo("Foundation");
		assertThat(jdbc.queryForObject(
				"select content ->> 'goal' from training_plan_versions where id = ?", String.class,
				updated.versionId())).isEqualTo("Progressive overload");
		assertThat(jdbc.queryForObject(
				"select count(*) from audit_events where action = 'TRAINING_PLAN_UPDATED' and target_id = ?",
				Long.class, created.id())).isEqualTo(1L);
	}

	private Account createAccount(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		String unique = id.toString().substring(0, 8);
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name, "09" + unique,
				unique + "@example.test", LocalDate.of(1990, 1, 1), "{noop}secret"));
	}

	private void createPlusMembership(UUID memberId, UUID actorId, Instant startsAt, Instant endsAt) {
		UUID offerId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID();
		jdbc.update("""
			insert into membership_offers (id, plan_code, name, description, price_amount, duration_days,
				status, created_by_account_id)
			values (?, 'PLUS', ?, 'US42 test offer', 1000, 30, 'ACTIVE', ?)
			""", offerId, "Offer " + offerId, actorId);
		jdbc.update("""
			insert into membership_orders (id, order_number, member_account_id, created_by_account_id, offer_id,
				offer_name_snapshot, plan_code_snapshot, price_amount_snapshot, currency_code_snapshot,
				duration_days_snapshot, payment_method, status, paid_at)
			values (?, ?, ?, ?, ?, 'Offer', 'PLUS', 1000, 'VND', 30, 'CASH', 'PAID', ?)
			""", orderId, "ORD-" + orderId, memberId, actorId, offerId, Timestamp.from(startsAt));
		jdbc.update("""
			insert into payments (id, order_id, method, status, amount, currency_code,
				processed_by_account_id, paid_at)
			values (?, ?, 'CASH', 'PAID', 1000, 'VND', ?, ?)
			""", UUID.randomUUID(), orderId, actorId, Timestamp.from(startsAt));
		jdbc.update("""
			insert into memberships (id, member_account_id, order_id, offer_id, plan_code_snapshot,
				offer_name_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
				status, starts_at, ends_at)
			values (?, ?, ?, ?, 'PLUS', 'Offer', 1000, 'VND', 30, 'ACTIVE', ?, ?)
			""", UUID.randomUUID(), memberId, orderId, offerId, Timestamp.from(startsAt), Timestamp.from(endsAt));
	}
}
