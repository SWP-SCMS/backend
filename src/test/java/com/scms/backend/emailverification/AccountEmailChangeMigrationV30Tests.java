package com.scms.backend.emailverification;

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
class AccountEmailChangeMigrationV30Tests {
	@Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void preservesLegacyPendingRequestAndStopsPendingEmailReservation() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("26").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		insertAccount(db, first, "legacy@example.test", "0900000001");
		insertAccount(db, second, "second@example.test", "0900000002");
		UUID legacyRequest = UUID.randomUUID();
		db.update("""
			insert into staff_email_change_requests(id,account_id,pending_email,otp_digest,hmac_key_version,
			 expires_at,resend_available_at,max_attempts,status,created_at,updated_at)
			values (?,?,?,'legacy-digest','v1',now()+interval '10 minutes',now(),5,'PENDING',now(),now())
			""", legacyRequest, first, "shared@example.test");

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForMap("select status,purpose_bound from staff_email_change_requests where id=?", legacyRequest))
			.containsEntry("status", "PENDING").containsEntry("purpose_bound", false);
		db.update("""
			insert into staff_email_change_requests(id,account_id,pending_email,otp_digest,hmac_key_version,
			 expires_at,resend_available_at,max_attempts,status,created_at,updated_at,purpose_bound)
			values (?,?,?,'bound-digest','v1',now()+interval '10 minutes',now(),5,'PENDING',now(),now(),true)
			""", UUID.randomUUID(), second, "shared@example.test");
		assertThat(db.queryForObject("select count(*) from staff_email_change_requests where pending_email=?",
			Integer.class, "shared@example.test")).isEqualTo(2);
	}

	private void insertAccount(JdbcTemplate db, UUID id, String email, String phone) {
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values (?,'MEMBER','ACTIVE','Migration account',?,?,date '2000-01-01','hash')
			""", id, phone, email);
	}
}
