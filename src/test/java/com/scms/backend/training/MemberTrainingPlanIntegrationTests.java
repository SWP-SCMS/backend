package com.scms.backend.training;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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
class MemberTrainingPlanIntegrationTests {

	static {
		TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC));
	}

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired MemberTrainingPlanService memberTrainingPlans;
	@Autowired TrainingPlanService trainingPlans;
	@Autowired CoachAssignmentService assignments;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate jdbc;

	@Test
	void viewOwnPlansReturnsAllOwnVersionsAfterMembershipAndAssignmentEnd() {
		Account manager = createAccount(AccountRole.MANAGER, "Manager US44");
		Account coach = createAccount(AccountRole.COACH, "Coach US44");
		Account member = createAccount(AccountRole.MEMBER, "Member US44");
		Account otherMember = createAccount(AccountRole.MEMBER, "Other Member US44");
		Instant now = Instant.now();

		createPlusMembership(member.getId(), manager.getId(), now.minusSeconds(3600), now.plusSeconds(86400));
		createPlusMembership(otherMember.getId(), manager.getId(), now.minusSeconds(3600), now.plusSeconds(86400));
		assignments.assign(manager.getId(), member.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "US44 own assignment"));
		assignments.assign(manager.getId(), otherMember.getId(),
				new CoachAssignmentAssignRequest(coach.getId(), "US44 other assignment"));

		TrainingPlanResponse ownPlan = trainingPlans.create(coach.getId(), member.getId(),
				new CreateTrainingPlanRequest("Foundation", Map.of("goal", "Foundation")));
		trainingPlans.update(coach.getId(), member.getId(), ownPlan.id(),
				new UpdateTrainingPlanRequest("Progressive strength", Map.of("goal", "Progressive overload")));
		TrainingPlanResponse otherPlan = trainingPlans.create(coach.getId(), otherMember.getId(),
				new CreateTrainingPlanRequest("Other plan", Map.of("goal", "Other member")));

		jdbc.update("update memberships set status = 'EXPIRED', ends_at = ? where member_account_id = ?",
				Timestamp.from(now.minusSeconds(1)), member.getId());
		jdbc.update("""
			update coach_assignments
			set status = 'ENDED', ends_at = greatest(starts_at, ?),
				end_reason = 'PLUS membership expired', version = version + 1
			where member_account_id = ? and status = 'ACTIVE'
			""", Timestamp.from(now), member.getId());

		List<MemberTrainingPlanResponse> result = memberTrainingPlans.viewOwnPlans(member.getId());

		assertThat(result).singleElement().satisfies(plan -> {
			assertThat(plan.id()).isEqualTo(ownPlan.id());
			assertThat(plan.memberAccountId()).isEqualTo(member.getId());
			assertThat(plan.title()).isEqualTo("Progressive strength");
			assertThat(plan.versions()).extracting(MemberTrainingPlanVersionResponse::versionNumber)
					.containsExactly(1, 2);
			assertThat(plan.versions()).extracting(version -> version.content().get("goal"))
					.containsExactly("Foundation", "Progressive overload");
		});
		assertThat(result).extracting(MemberTrainingPlanResponse::id).doesNotContain(otherPlan.id());
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
			values (?, 'PLUS', ?, 'US44 test offer', 1000, 30, 'ACTIVE', ?)
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
