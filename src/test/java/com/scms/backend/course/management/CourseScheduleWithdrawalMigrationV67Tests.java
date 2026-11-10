package com.scms.backend.course.management;

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
class CourseScheduleWithdrawalMigrationV67Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() {
		flyway(null).clean();
	}

	@Test
	void freshMigrationCreatesScheduleEventsCauseLinksAndRemovedStates() {
		flyway(null).migrate();
		JdbcTemplate db = db();
		assertThat(db.queryForObject("select count(*) from information_schema.tables where table_name='course_schedule_change_events'", Long.class)).isOne();
		assertThat(db.queryForList("select column_name from information_schema.columns where table_name='course_withdrawal_requests'", String.class)).contains("cause_code", "schedule_change_response_id", "change_event_id");
		assertThat(db.queryForList("select column_name from information_schema.columns where table_name='course_schedule_change_responses'", String.class)).contains("cause_code", "change_event_id");
		assertThat(db.queryForList("select constraint_name from information_schema.table_constraints where table_name='course_schedule_change_events'", String.class)).contains("chk_course_schedule_change_event_cause");
	}

	@Test
	void upgradeFromV66WidensDispositionForRemovedUnits() {
		flyway("66").migrate();
		flyway(null).migrate();
		assertThat(db().queryForObject("""
			select character_maximum_length from information_schema.columns
			where table_schema='public' and table_name='course_enrollment_sessions' and column_name='disposition'
			""", Integer.class)).isEqualTo(32);
	}

	private Flyway flyway(String target) {
		var configuration = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).cleanDisabled(false);
		if (target != null)
			configuration.target(target);
		return configuration.load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}
}
