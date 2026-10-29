package com.scms.backend.identity.staffprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class StaffProfileImageMigrationV50Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() {
		flyway().clean();
	}

	@Test
	void freshDatabaseAddsNullableStaffImageAndProtectsMemberStorage() {
		flyway("50").migrate();
		JdbcTemplate db = db();
		UUID manager = insertAccount(db, "MANAGER", "manager-v50@example.test", "0900000050");
		UUID member = insertAccount(db, "MEMBER", "member-v50@example.test", "0900000051");

		assertThat(db.queryForObject("select profile_image_url from accounts where id=?", String.class, manager))
			.isNull();
		assertThat(db.update("update accounts set profile_image_url=? where id=?",
			"/profile-images/staff.png", manager)).isOne();
		assertThatThrownBy(() -> db.update("update accounts set profile_image_url=? where id=?",
			"/profile-images/member.png", member)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void v49UpgradePreservesExistingStaffAccount() {
		flyway("49").migrate();
		JdbcTemplate db = db();
		UUID coach = insertAccount(db, "COACH", "coach-v50@example.test", "0900000052");
		String before = db.queryForObject("select to_jsonb(a)::text from accounts a where id=?", String.class, coach);

		flyway("50").migrate();

		assertThat(db.queryForObject("select profile_image_url from accounts where id=?", String.class, coach)).isNull();
		assertThat(db.queryForObject("select to_jsonb(a) - 'profile_image_url' from accounts a where id=?",
			String.class, coach)).isEqualTo(before);
	}

	private UUID insertAccount(JdbcTemplate db, String role, String email, String phone) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into accounts(id, role, status, full_name, phone, email, birth_date, password_hash)
			values (?, ?, 'ACTIVE', 'V50 Staff', ?, ?, date '1990-01-01', 'unused')
			""", id, role, phone, email);
		return id;
	}

	private Flyway flyway() {
		return Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false)
			.load();
	}

	private Flyway flyway(String target) {
		return Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false)
			.target(target)
			.load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(
			postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}
}
