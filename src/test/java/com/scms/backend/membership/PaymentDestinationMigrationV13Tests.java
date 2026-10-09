package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

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
class PaymentDestinationMigrationV13Tests {
	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void v13AddsNullableDestinationSnapshotsWithoutInventingLegacyValues() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("12").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		UUID manager = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		account(db, manager, "MANAGER", "manager-v13@example.test", "0900000013");
		account(db, member, "MEMBER", "member-v13@example.test", "0900000014");
		db.update("insert into member_profiles(account_id) values(?)", member);
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id)
			values(?,'BASIC','V13 Offer','Migration test',12000,'VND',30,'ACTIVE',?)
			""", offer, manager);
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				payment_method,status,expires_at)
			values(?,?,?, ?,?,'V13 Offer','BASIC',12000,'VND',30,'BANK_TRANSFER','PENDING_PAYMENT',?)
			""", order, "ORD-" + order.toString().replace("-", "").toUpperCase(), member, member, offer,
			Timestamp.from(Instant.now().plusSeconds(3600)));
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,
				provider,provider_reference)
			values(?,?,'BANK_TRANSFER','PENDING',12000,'VND','LEGACY-V13','SEPAY','LEGACY-V13')
			""", payment, order);

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		for (String column : new String[] {
			"bank_code_snapshot", "bank_account_number_snapshot", "bank_account_name_snapshot" }) {
			assertThat(db.queryForObject("""
				select is_nullable from information_schema.columns
				where table_schema='public' and table_name='payments' and column_name=?
				""", String.class, column)).isEqualTo("YES");
		}
		assertThat(db.queryForObject("""
			select count(*) from payments where id=? and bank_code_snapshot is null
				and bank_account_number_snapshot is null and bank_account_name_snapshot is null
			""", Long.class, payment)).isOne();
	}

	private void account(JdbcTemplate db, UUID id, String role, String email, String phone) {
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?,?,'ACTIVE','V13 Test',?,?,date '1990-01-01','unused')
			""", id, role, phone, email);
	}
}
