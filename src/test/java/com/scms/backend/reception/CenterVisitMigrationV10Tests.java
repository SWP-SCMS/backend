package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CenterVisitMigrationV10Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void v10BackfillsLegacyVisitsAndEnforcesOneOpenVisitPerMember() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("9").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		UUID receptionist = account(db, "RECEPTIONIST", "reception");
		UUID member = account(db, "MEMBER", "member");
		UUID membership = membership(db, member, receptionist);
		UUID morning = visit(db, member, membership, receptionist, Instant.parse("2026-10-05T01:00:00Z"));
		UUID evening = visit(db, member, membership, receptionist, Instant.parse("2026-10-05T11:00:00Z"));

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForObject("select checked_out_at from center_visits where id=?", Instant.class, morning))
			.isEqualTo(Instant.parse("2026-10-05T11:00:00Z"));
		assertThat(db.queryForObject("select checked_out_at from center_visits where id=?", Instant.class, evening))
			.isEqualTo(Instant.parse("2026-10-05T17:00:00Z"));
		assertThat(db.queryForObject("select checkout_source from center_visits where id=?", String.class, morning))
			.isEqualTo("MIGRATION");
		assertThat(db.queryForObject("select count(*) from attendance", Long.class)).isZero();

		db.update("insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at) values(?,?,?,?,?)",
			UUID.randomUUID(), member, membership, receptionist, Timestamp.from(Instant.parse("2026-10-06T01:00:00Z")));
		assertThatThrownBy(() -> db.update("insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at) values(?,?,?,?,?)",
			UUID.randomUUID(), member, membership, receptionist, Timestamp.from(Instant.parse("2026-10-06T02:00:00Z"))))
			.isInstanceOf(org.springframework.dao.DataAccessException.class);
	}

	private UUID account(JdbcTemplate db, String role, String suffix) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?, 'ACTIVE','Migration',?,?,date '1990-01-01','unused')",
			id, role, "09" + Math.abs(id.hashCode()), suffix + id + "@test.invalid");
		return id;
	}

	private UUID membership(JdbcTemplate db, UUID member, UUID actor) {
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,duration_days,status,created_by_account_id) values(?,'BASIC','Migration','Migration',1,30,'ACTIVE',?)", offer, actor);
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at) values(?,?,?,?,?,'Migration','BASIC',1,'VND',30,'CASH','PAID',?)",
			order, "ORD-" + order, member, actor, offer, Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")));
		db.update("insert into payments(id,order_id,method,status,amount,currency_code,processed_by_account_id,paid_at) values(?,?,'CASH','PAID',1,'VND',?,?)",
			payment, order, actor, Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")));
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?, 'BASIC','Migration',1,'VND',30,'ACTIVE',?,?)",
			membership, member, order, offer, Timestamp.from(Instant.parse("2026-10-01T00:00:00Z")), Timestamp.from(Instant.parse("2026-11-01T00:00:00Z")));
		return membership;
	}

	private UUID visit(JdbcTemplate db, UUID member, UUID membership, UUID actor, Instant checkedInAt) {
		UUID id = UUID.randomUUID();
		db.update("insert into center_visits(id,member_account_id,membership_id,checked_in_by_account_id,checked_in_at) values(?,?,?,?,?)",
			id, member, membership, actor, Timestamp.from(checkedInAt));
		return id;
	}
}
