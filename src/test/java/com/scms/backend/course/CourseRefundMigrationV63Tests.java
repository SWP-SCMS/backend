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
class CourseRefundMigrationV63Tests {
	@Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach void cleanDatabase() { flyway(null).clean(); }

	@Test
	void version62UpgradeAddsRefundWorkflowSnapshotsAndPreservesIdempotency() {
		flyway("62").migrate();
		Flyway upgraded = flyway(null);
		upgraded.migrate();
		upgraded.validate();

		JdbcTemplate db = db();
		assertThat(db.queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(63);
		assertThat(db.queryForObject("select pg_get_constraintdef(oid) from pg_constraint "
			+ "where conname='chk_course_refund_obligations_source'", String.class))
			.contains("MEMBER_WITHDRAWAL", "COURSE_RUN_CANCELLED", "MATERIAL_CHANGE", "UNRESOLVED_SESSIONS",
				"TRANSFER_CREDIT_EXCESS", "PAYMENT_EXCEPTION");
		assertThat(db.queryForList("select column_name from information_schema.columns where table_schema='public' "
			+ "and table_name='course_refund_obligations'", String.class))
			.contains("member_account_id", "course_order_id", "course_payment_id", "approved_amount_vnd",
				"disbursed_amount_vnd", "verified_amount_vnd", "currency_code", "requester_account_id",
				"approved_by_account_id", "request_reason", "approval_reason", "evidence_reference", "version");
		assertThat(db.queryForList("select constraint_name from information_schema.table_constraints "
			+ "where table_schema='public' and table_name='course_refund_obligations'", String.class))
			.contains("uq_course_refund_obligations_source");
		assertThat(db.queryForList("select trigger_name from information_schema.triggers where event_object_table="
			+ "'course_refund_obligations'", String.class)).contains("trg_course_refund_obligation_snapshot",
				"trg_course_refund_obligation_transition");
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
