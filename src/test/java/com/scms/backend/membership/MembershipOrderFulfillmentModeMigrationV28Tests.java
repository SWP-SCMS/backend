package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MembershipOrderFulfillmentModeMigrationV28Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void addsAndBackfillsTheFulfillmentModeConstraint() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("27").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		Seed seed = seedCommerce(db);
		UUID legacyOrder = UUID.randomUUID();
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
				duration_days_snapshot,payment_method,status,expires_at,paid_at)
			values(?,?,?,?,?,'Legacy','BASIC',100000,'VND',30,'CASH','PAID',now(),now())
			""", legacyOrder, "ORD-" + legacyOrder, seed.memberId(), seed.managerId(), seed.offerId());

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForObject("select fulfillment_mode from membership_orders where id=?", String.class,
			legacyOrder)).isEqualTo("IMMEDIATE");
		assertThat(db.queryForObject("select count(*) from pg_constraint where conname='chk_membership_orders_fulfillment_mode'",
			Long.class)).isOne();
		assertThatThrownBy(() -> db.update("update membership_orders set fulfillment_mode='INVALID' where id=?",
			legacyOrder)).isInstanceOf(DataIntegrityViolationException.class);
	}

	private Seed seedCommerce(JdbcTemplate db) {
		UUID manager = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		UUID offer = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) "
			+ "values(?, 'MANAGER', 'ACTIVE', 'Manager', ?, ?, '1990-01-01', 'hash')",
			manager, "09" + manager.toString().replace("-", "").substring(0, 8), manager + "@example.test");
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) "
			+ "values(?, 'MEMBER', 'ACTIVE', 'Member', ?, ?, '1990-01-01', 'hash')",
			member, "09" + member.toString().replace("-", "").substring(0, 8), member + "@example.test");
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) "
			+ "values(?, 'BASIC', 'Basic', 'Basic', 100000, 'VND', 30, 'ACTIVE', ?)", offer, manager);
		return new Seed(manager, member, offer);
	}

	private record Seed(UUID managerId, UUID memberId, UUID offerId) { }
}
