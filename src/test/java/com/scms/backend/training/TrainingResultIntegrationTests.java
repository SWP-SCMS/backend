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
class TrainingResultIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired TrainingResultService results;
	@Autowired TrainingPlanService plans;
	@Autowired CoachAssignmentService assignments;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void createAndUpdatePreservePersonalOwnershipAndAuditHistory() {
		Account manager = createAccount(AccountRole.MANAGER, "Manager US45");
		Account coach = createAccount(AccountRole.COACH, "Coach US45");
		Account member = createAccount(AccountRole.MEMBER, "Member US45");
		Instant now = Instant.now();
		createPlusMembership(member.getId(), manager.getId(), now.minusSeconds(3600), now.plusSeconds(86400));
		CoachAssignmentResponse assignment = assignments.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "US45 integration assignment"));
		TrainingPlanResponse plan = plans.create(coach.getId(), member.getId(),
				new CreateTrainingPlanRequest("Strength", Map.of("goal", "Build strength")));

		TrainingResultResponse created = results.create(coach.getId(), member.getId(),
				new PersonalTrainingResultRequest(plan.versionId(), now.minusSeconds(300),
						Map.of("sets", 3, "reps", 10)));
		TrainingResultResponse updated = results.update(coach.getId(), member.getId(), created.id(),
				new PersonalTrainingResultRequest(plan.versionId(), now.minusSeconds(240),
						Map.of("sets", 4, "reps", 10)));

		assertThat(updated.coachAssignmentId()).isEqualTo(assignment.id());
		assertThat(jdbc.queryForObject(
				"select content ->> 'sets' from training_results where id = ?", String.class, created.id()))
				.isEqualTo("4");
		assertThat(jdbc.queryForObject(
				"select class_session_id is null from training_results where id = ?", Boolean.class, created.id()))
				.isTrue();
		assertThat(jdbc.queryForObject(
				"select count(*) from audit_events where target_type = 'TRAINING_RESULT' and target_id = ?",
				Long.class, created.id())).isEqualTo(2L);
		assertThat(jdbc.queryForObject("""
			select before_data -> 'content' ->> 'sets'
			from audit_events
			where action = 'TRAINING_RESULT_UPDATED' and target_id = ?
			""", String.class, created.id())).isEqualTo("3");
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
			values (?, 'PLUS', ?, 'US45 test offer', 1000, 30, 'ACTIVE', ?)
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
