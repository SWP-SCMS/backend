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
class CourseRunCancellationMigrationV60Tests {
	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() { flyway(null).clean(); }

	@Test
	void freshMigrationAllowsRunCancellationRefundSourceAndValidatesHistory() {
		flyway(null).migrate();
		Flyway migrated = flyway(null);
		migrated.validate();

		assertThat(db().queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(62);
		assertThat(db().queryForObject("select pg_get_constraintdef(oid) from pg_constraint "
			+ "where conname='chk_course_refund_obligations_source'", String.class))
			.contains("UNRESOLVED_SESSIONS", "COURSE_RUN_CANCELLED");
	}

	@Test
	void version59UpgradeAppliesV60AndValidatesHistory() {
		flyway("59").migrate();
		assertThat(db().queryForObject("select pg_get_constraintdef(oid) from pg_constraint "
			+ "where conname='chk_course_refund_obligations_source'", String.class))
			.doesNotContain("COURSE_RUN_CANCELLED");

		Flyway upgraded = flyway(null);
		upgraded.migrate();
		upgraded.validate();

		assertThat(db().queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(62);
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
