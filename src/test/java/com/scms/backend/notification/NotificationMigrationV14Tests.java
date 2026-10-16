package com.scms.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class NotificationMigrationV14Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void createsDurableOutboxWithUniqueEventKeysAndRetryState() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("13").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		UUID accountId = UUID.randomUUID();
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?,'MEMBER','ACTIVE','Migration Member','0900000013',?,date '1990-01-01','unused')
			""", accountId, accountId + "@example.test");
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();
		db.update("""
			insert into notification_outbox(id,event_key,recipient_account_id,notification_type,target_type,target_id,payload)
			values(?,?,?,'ACCOUNT_CREATED','ACCOUNT',?,'{}')
			""", UUID.randomUUID(), "migration:" + accountId, accountId, accountId);

		assertThat(db.queryForObject("select status from notification_outbox where event_key=?", String.class,
			"migration:" + accountId)).isEqualTo("PENDING");
		assertThat(db.queryForObject("select attempt_count from notification_outbox where event_key=?", Integer.class,
			"migration:" + accountId)).isZero();
		assertThat(db.queryForObject("select next_attempt_at is not null from notification_outbox where event_key=?",
			Boolean.class, "migration:" + accountId)).isTrue();
		assertThat(db.queryForObject("""
			select count(*) from pg_constraint
			where conrelid='notification_outbox'::regclass and conname='chk_notification_outbox_status'
			""", Integer.class)).isOne();
		assertThat(db.queryForObject("""
			select pg_get_constraintdef(oid) from pg_constraint
			where conrelid='notification_outbox'::regclass and conname='chk_notification_outbox_status'
			""", String.class)).contains("PROCESSING", "TERMINAL_FAILED");

		db.update("update notification_outbox set status='TERMINAL_FAILED',next_attempt_at=null where event_key=?",
			"migration:" + accountId);
		assertThat(db.queryForMap("select status,next_attempt_at from notification_outbox where event_key=?",
			"migration:" + accountId))
			.containsEntry("status", "TERMINAL_FAILED")
			.containsEntry("next_attempt_at", null);
	}
}
