package com.scms.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
class NotificationMigrationV12Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void v12BackfillsLegacyKeysThenRequiresUniqueEventKeys() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("11").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		UUID accountId = UUID.randomUUID();
		UUID notificationId = UUID.randomUUID();
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?,'MEMBER','ACTIVE','Migration Member','0900000012',?,date '1990-01-01','unused')
			""", accountId, accountId + "@example.test");
		db.update("""
			insert into notifications(id,recipient_account_id,notification_type,target_type,target_id,payload,delivery_status)
			values(?,?,'LEGACY','ACCOUNT',?,'{}','PENDING')
			""", notificationId, accountId, accountId);

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForObject("select event_key from notifications where id=?", String.class, notificationId))
			.isEqualTo("legacy:" + notificationId);
		assertThat(db.queryForObject("""
			select is_nullable from information_schema.columns
			where table_schema='public' and table_name='notifications' and column_name='event_key'
			""", String.class)).isEqualTo("NO");
		assertThatThrownBy(() -> db.update("""
			insert into notifications(id,event_key,recipient_account_id,notification_type,target_type,target_id,payload,delivery_status)
			values(?,?,?,'DUPLICATE','ACCOUNT',?,'{}','PENDING')
			""", UUID.randomUUID(), "legacy:" + notificationId, accountId, accountId))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
