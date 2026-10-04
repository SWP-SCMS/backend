package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
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
class PaymentMigrationV9Tests {
	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void v9BackfillsQaDataDeduplicatesPendingAndProtectsHistory() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("8").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));

		UUID manager = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		UUID offer = UUID.randomUUID();
		UUID pendingOrder = UUID.randomUUID();
		UUID paidOrder = UUID.randomUUID();
		UUID firstPending = UUID.randomUUID();
		UUID duplicatePending = UUID.randomUUID();
		UUID paidPayment = UUID.randomUUID();
		Instant paidAt = Instant.parse("2026-10-04T05:00:00Z");
		account(db, manager, "MANAGER", "manager@example.test", "0900000001");
		account(db, member, "MEMBER", "qa.member@example.test", "0900000002");
		db.update("insert into member_profiles(account_id) values(?)", member);
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id)
			values(?,'BASIC','QA Offer V9','QA history',12000,'VND',30,'ACTIVE',?)
			""", offer, manager);
		order(db, pendingOrder, member, manager, offer, "PENDING_PAYMENT", null);
		order(db, paidOrder, member, manager, offer, "PAID", paidAt);
		payment(db, firstPending, pendingOrder, "PENDING", null, "PENDING-1");
		payment(db, duplicatePending, pendingOrder, "PENDING", null, "PENDING-2");
		payment(db, paidPayment, paidOrder, "PAID", paidAt, "PAID-HISTORY");
		db.update("""
			insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,
				price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at)
			values(?,?,?,?,'BASIC','QA Offer V9',12000,'VND',30,'ACTIVE',?,?)
			""", UUID.randomUUID(), member, paidOrder, offer, Timestamp.from(paidAt),
			Timestamp.from(paidAt.plusSeconds(2592000)));
		db.update("""
			insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,
				currency_code_snapshot,payment_method_snapshot)
			values(?,?,?,?,?,12000,'VND','BANK_TRANSFER')
			""", UUID.randomUUID(), "RC-" + paidPayment, paidPayment, paidOrder, member);

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForObject("select is_test_data from accounts where id=?", Boolean.class, member)).isTrue();
		assertThat(db.queryForObject("select status from accounts where id=?", String.class, member)).isEqualTo("SUSPENDED");
		assertThat(db.queryForObject("select status from membership_offers where id=?", String.class, offer)).isEqualTo("INACTIVE");
		assertThat(db.queryForObject("select count(*) from payments where order_id=? and status='PENDING'", Long.class,
			pendingOrder)).isOne();
		assertThat(db.queryForObject("select status from payments where id=?", String.class, duplicatePending))
			.isEqualTo("FAILED");
		assertThat(db.queryForObject("select status from payments where id=?", String.class, paidPayment)).isEqualTo("PAID");
		assertThat(db.queryForObject("select count(*) from memberships where order_id=?", Long.class, paidOrder)).isOne();
		assertThat(db.queryForObject("select count(*) from receipts where payment_id=?", Long.class, paidPayment)).isOne();

		assertThatThrownBy(() -> payment(db, UUID.randomUUID(), pendingOrder, "PENDING", null, "PENDING-3"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("update payments set status='PENDING' where id=?", duplicatePending))
			.isInstanceOf(org.springframework.dao.DataAccessException.class);
	}

	private void account(JdbcTemplate db, UUID id, String role, String email, String phone) {
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?,?,'ACTIVE','V9 Test',?,?,date '1990-01-01','unused')
			""", id, role, phone, email);
	}

	private void order(JdbcTemplate db, UUID id, UUID member, UUID actor, UUID offer, String status,
			Instant paidAt) {
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				payment_method,status,expires_at,paid_at)
			values(?,?,?, ?,?,'QA Offer V9','BASIC',12000,'VND',30,'BANK_TRANSFER',?,?,?)
			""", id, "ORD-" + id.toString().replace("-", "").toUpperCase(), member, actor, offer, status,
			"PENDING_PAYMENT".equals(status) ? Timestamp.from(Instant.now().plusSeconds(3600)) : null,
			paidAt == null ? null : Timestamp.from(paidAt));
	}

	private void payment(JdbcTemplate db, UUID id, UUID order, String status, Instant paidAt, String reference) {
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,
				provider,provider_reference,provider_transaction_id,paid_at)
			values(?,?,'BANK_TRANSFER',?,12000,'VND',?,'SEPAY',?,?,?)
			""", id, order, status, reference, reference, paidAt == null ? null : "TX-" + id,
			paidAt == null ? null : Timestamp.from(paidAt));
	}
}
