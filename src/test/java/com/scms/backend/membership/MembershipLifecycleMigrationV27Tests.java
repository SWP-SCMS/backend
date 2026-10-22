package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MembershipLifecycleMigrationV27Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void upgradesExistingMembershipsAndEnforcesLifecycleStateInvariants() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("26").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		Seed seed = seedCommerce(db);
		insertMembership(db, seed, UUID.randomUUID(), "ACTIVE",
			Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-02-01T00:00:00Z"));

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForObject("select status from memberships where id=?", String.class, seed.membershipId()))
			.isEqualTo("ACTIVE");
		assertThat(db.queryForObject("select count(*) from memberships where member_account_id=? and status='EXPIRED'",
			Long.class, seed.memberId())).isOne();
		insertMembership(db, seed, UUID.randomUUID(), "RESERVED", null, null);
		assertThatThrownBy(() -> insertMembership(db, seed, UUID.randomUUID(), "RESERVED", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertMembership(db, seed, UUID.randomUUID(), "ACTIVE", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertMembership(db, seed, UUID.randomUUID(), "TERMINATED", null, null))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Seed seedCommerce(JdbcTemplate db) {
		UUID manager = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		insertAccount(db, manager, "MANAGER", "manager" + manager + "@example.com");
		insertAccount(db, member, "MEMBER", "member" + member + "@example.com");
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?, 'PLUS','Plus','Plus',100000,'VND',30,'ACTIVE',?)", offer, manager);
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,expires_at,paid_at) values(?,?,?,?,?,'Plus','PLUS',100000,'VND',30,'CASH','PAID',now(),now())", order, "ORD-" + order, member, manager, offer);
		insertPaidPayment(db, order);
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Plus',100000,'VND',30,'ACTIVE',?,?)",
			membership, member, order, offer, "PLUS", Timestamp.from(Instant.parse("2026-01-01T00:00:00Z")),
			Timestamp.from(Instant.parse("2027-01-01T00:00:00Z")));
		return new Seed(member, offer, membership);
	}

	private void insertMembership(JdbcTemplate db, Seed seed, UUID id, String status, Instant startsAt, Instant endsAt) {
		UUID order = UUID.randomUUID();
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,expires_at,paid_at) select ?,?,?,created_by_account_id,?,'Plus','PLUS',100000,'VND',30,'CASH','PAID',now(),now() from membership_offers where id=?",
			order, "ORD-" + order, seed.memberId(), seed.offerId(), seed.offerId());
		insertPaidPayment(db, order);
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Plus',100000,'VND',30,?,?,?)",
			id, seed.memberId(), order, seed.offerId(), "PLUS", status,
			startsAt == null ? null : Timestamp.from(startsAt), endsAt == null ? null : Timestamp.from(endsAt));
	}

	private void insertPaidPayment(JdbcTemplate db, UUID orderId) {
		db.update("insert into payments(id,order_id,method,status,amount,currency_code,paid_at) values(?,?,'CASH','PAID',100000,'VND',now())",
			UUID.randomUUID(), orderId);
	}

	private void insertAccount(JdbcTemplate db, UUID id, String role, String email) {
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE','Test','09' || substring(?::text,1,8),?,'1990-01-01','hash')", id, role, id, email);
	}

	private record Seed(UUID memberId, UUID offerId, UUID membershipId) { }
}
