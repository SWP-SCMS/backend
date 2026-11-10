package com.scms.backend.course.management;

import static org.assertj.core.api.Assertions.assertThat;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CourseReportingMigrationV65Tests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired
	DataSource dataSource;

	@Test
	void createsIndexesForActualReportFiltersAndStableSort() {
		JdbcTemplate db = new JdbcTemplate(dataSource);
		assertThat(indexDefinition(db, "idx_course_runs_reporting_start")).contains("planned_start_at DESC", "id DESC");
		assertThat(indexDefinition(db, "idx_course_runs_reporting_status_start")).contains("status", "planned_start_at DESC", "id DESC");
	}

	private String indexDefinition(JdbcTemplate db, String name) {
		return db.queryForObject("select indexdef from pg_indexes where schemaname='public' and indexname=?", String.class, name);
	}
}
