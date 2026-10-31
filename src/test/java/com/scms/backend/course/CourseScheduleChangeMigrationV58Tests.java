package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CourseScheduleChangeMigrationV58Tests {
	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() { flyway(null).clean(); }

	@Test
	void freshMigrationCreatesResponseConstraintsAndPendingIndex() {
		flyway(null).migrate();
		JdbcTemplate db = db();

		assertThat(db.queryForObject("select count(*) from information_schema.tables where table_schema='public'"
			+ " and table_name='course_schedule_change_responses'", Long.class)).isOne();
		assertThat(db.queryForList("select constraint_name from information_schema.table_constraints"
			+ " where table_schema='public' and table_name='course_schedule_change_responses'", String.class))
			.contains("chk_course_schedule_change_response_choice", "chk_course_schedule_change_response_status",
				"chk_course_schedule_change_response_state");
		assertThat(db.queryForList("select indexname from pg_indexes where schemaname='public'", String.class))
			.contains("idx_course_schedule_change_response_member_pending");
	}

	@Test
	void version57UpgradeAppliesV58AndValidatesHistory() {
		flyway("57").migrate();
		assertThat(db().queryForObject("select count(*) from information_schema.tables where table_schema='public'"
			+ " and table_name='course_schedule_change_responses'", Long.class)).isZero();

		Flyway upgraded = flyway(null);
		upgraded.migrate();
		upgraded.validate();

		assertThat(db().queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(63);
		assertThat(db().queryForObject("select count(*) from information_schema.tables where table_schema='public'"
			+ " and table_name='course_schedule_change_responses'", Long.class)).isOne();
	}

	private Flyway flyway(String target) {
		var configuration = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false);
		if (target != null) configuration.target(target);
		return configuration.load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}
}
