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
class CourseSessionDisruptionMigrationV59Tests {
	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() { flyway(null).clean(); }

	@Test
	void freshMigrationCreatesMakeupAndRefundObligationConstraints() {
		flyway(null).migrate();
		JdbcTemplate db = db();

		assertThat(db.queryForList("select indexname from pg_indexes where schemaname='public'", String.class))
			.contains("uq_course_sessions_run_original_unit", "uq_course_sessions_replacement",
				"idx_course_refund_obligations_review");
		assertThat(db.queryForObject("select count(*) from information_schema.columns where table_schema='public' "
			+ "and table_name='course_sessions' and column_name in ('is_makeup','makeup_warning_at')", Long.class)).isEqualTo(2);
		assertThat(db.queryForObject("select count(*) from information_schema.tables where table_schema='public' "
			+ "and table_name='course_refund_obligations'", Long.class)).isOne();
		assertThat(db.queryForList("select constraint_name from information_schema.table_constraints "
			+ "where table_schema='public' and table_name='course_refund_obligations'", String.class))
			.contains("uq_course_refund_obligations_source", "chk_course_refund_obligations_source",
				"chk_course_refund_obligations_amount", "chk_course_refund_obligations_status");
	}

	@Test
	void version58UpgradeAppliesV59AndValidatesHistory() {
		flyway("58").migrate();
		assertThat(db().queryForObject("select count(*) from information_schema.tables where table_schema='public' "
			+ "and table_name='course_refund_obligations'", Long.class)).isZero();

		Flyway upgraded = flyway(null);
		upgraded.migrate();
		upgraded.validate();

		assertThat(db().queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(60);
		assertThat(db().queryForObject("select count(*) from information_schema.tables where table_schema='public' "
			+ "and table_name='course_refund_obligations'", Long.class)).isOne();
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
