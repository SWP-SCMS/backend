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
class CourseRefundMigrationV64Tests {
	@Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach void cleanDatabase() { flyway(null).clean(); }

	@Test
	void version63UpgradeAddsDisbursementIdempotencyWithoutChangingExistingRows() {
		flyway("63").migrate();
		Flyway upgraded = flyway(null);
		upgraded.migrate();
		upgraded.validate();

		JdbcTemplate db = db();
		assertThat(db.queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(64);
		assertThat(db.queryForList("select column_name from information_schema.columns where table_schema='public' "
			+ "and table_name='course_refund_obligations'", String.class))
			.contains("disbursement_idempotency_key");
		assertThat(db.queryForList("select constraint_name from information_schema.table_constraints "
			+ "where table_schema='public' and table_name='course_refund_obligations'", String.class))
			.contains("uq_course_refund_disbursement_actor_key", "chk_course_refund_disbursement_metadata");
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
