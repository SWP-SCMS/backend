package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "scms.membership-lifecycle-initial-delay-ms=3600000")
class MembershipLifecycleIntegrationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired JdbcTemplate db;
	@Autowired MembershipLifecycleScheduler scheduler;
	@MockitoSpyBean MembershipLifecycleService lifecycle;
	@MockitoBean Clock clock;

	@Test
	void schedulerCommitsEachMemberInAnIndependentTransaction() {
		Instant now = Instant.parse("2026-10-21T03:00:00Z");
		when(clock.instant()).thenReturn(now);
		UUID manager = account("MANAGER");
		UUID first = account("MEMBER");
		UUID second = account("MEMBER");
		UUID offer = offer(manager);
		membership(first, manager, offer, now.minusSeconds(7200), now.minusSeconds(3600));
		membership(second, manager, offer, now.minusSeconds(7200), now.minusSeconds(3600));
		doThrow(new IllegalStateException("member-specific failure")).when(lifecycle).reconcile(second, now);

		scheduler.reconcile();

		assertThat(status(first)).isEqualTo("EXPIRED");
		assertThat(status(second)).isEqualTo("ACTIVE");
	}

	@Test
	void schedulerEndsAssignmentWhenMigrationAlreadyExpiredTheMembership() {
		Instant now = Instant.parse("2026-10-21T03:00:00Z");
		when(clock.instant()).thenReturn(now);
		UUID manager = account("MANAGER");
		UUID member = account("MEMBER");
		UUID coach = account("COACH");
		UUID offer = offer(manager);
		membership(member, manager, offer, now.minusSeconds(7200), now.minusSeconds(3600));
		db.update("update memberships set status='EXPIRED' where member_account_id=?", member);
		UUID assignment = UUID.randomUUID();
		db.update("insert into coach_assignments(id,member_account_id,coach_account_id,assigned_by_account_id,status,starts_at) values(?,?,?,?,'ACTIVE',?)",
			assignment, member, coach, manager, java.sql.Timestamp.from(now.minusSeconds(7200)));

		scheduler.reconcile();

		assertThat(db.queryForObject("select status from coach_assignments where id=?", String.class, assignment))
			.isEqualTo("ENDED");
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE','Lifecycle','09' || substring(?::text,1,8),?,'1990-01-01','hash')",
			id, role, id, id + "@example.test");
		return id;
	}

	private UUID offer(UUID manager) {
		UUID id = UUID.randomUUID();
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'PLUS','Lifecycle','Lifecycle',100000,'VND',30,'ACTIVE',?)",
			id, manager);
		return id;
	}

	private void membership(UUID member, UUID manager, UUID offer, Instant startsAt, Instant endsAt) {
		UUID order = UUID.randomUUID();
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,expires_at,paid_at) values(?,?,?,?,?,'Lifecycle','PLUS',100000,'VND',30,'CASH','PAID',now(),now())",
			order, "ORD-" + order, member, manager, offer);
		db.update("insert into payments(id,order_id,method,status,amount,currency_code,paid_at) values(?,?,'CASH','PAID',100000,'VND',now())",
			UUID.randomUUID(), order);
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Lifecycle',100000,'VND',30,'ACTIVE',?,?)",
			UUID.randomUUID(), member, order, offer, "PLUS", java.sql.Timestamp.from(startsAt),
			java.sql.Timestamp.from(endsAt));
	}

	private String status(UUID member) {
		return db.queryForObject("select status from memberships where member_account_id=?", String.class, member);
	}
}
