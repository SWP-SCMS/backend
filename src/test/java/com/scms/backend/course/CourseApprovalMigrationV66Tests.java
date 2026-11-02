package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CourseApprovalMigrationV66Tests {
	@Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach void cleanDatabase() { flyway(null).clean(); }

	@Test
	void version65UpgradePreservesCoursesAndAddsApprovalLifecycle() {
		flyway("65").migrate();
		JdbcTemplate db = db();
		UUID disciplineId = UUID.randomUUID();
		UUID courseId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", disciplineId, "Legacy discipline");
		db.update("insert into courses(id,code,name,discipline_id,status) values (?,?,?,?,'DRAFT')",
			courseId, "LEGACY-COURSE", "Legacy course", disciplineId);

		Flyway upgraded = flyway(null);
		upgraded.migrate();
		upgraded.validate();

		assertThat(db.queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(69);
		assertThat(db.queryForObject("select name from courses where id=?", String.class, courseId))
			.isEqualTo("Legacy course");
		assertThat(db.queryForObject("select submitted_by_account_id is null and reviewed_by_account_id is null "
			+ "from courses where id=?", Boolean.class, courseId)).isTrue();
		assertThat(indexDefinition(db, "idx_courses_pending_approval"))
			.contains("submitted_at", "WHERE ((status)::text = 'PENDING_APPROVAL'::text)");

		UUID receptionistId = account(db, "RECEPTIONIST");
		db.update("insert into courses(id,code,name,discipline_id,status,submitted_by_account_id,submitted_at) "
			+ "values (?,?,?,?,'PENDING_APPROVAL',?,current_timestamp)", UUID.randomUUID(), "PENDING-COURSE",
			"Pending course", disciplineId, receptionistId);
		assertThatThrownBy(() -> db.update("insert into courses(id,code,name,discipline_id,status) "
			+ "values (?,?,?,?,'PENDING_APPROVAL')", UUID.randomUUID(), "INVALID-PENDING", "Invalid pending",
			disciplineId)).rootCause().hasMessageContaining("chk_courses_approval_metadata");
	}

	private UUID account(JdbcTemplate db, String role) {
		UUID id = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) "
			+ "values (?,?,'ACTIVE',?,?,?,date '1990-01-01','hash')", id, role, "Reception " + id,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test");
		return id;
	}

	private String indexDefinition(JdbcTemplate db, String name) {
		return db.queryForObject("select indexdef from pg_indexes where schemaname='public' and indexname=?",
			String.class, name);
	}

	private Flyway flyway(String target) {
		var configuration = Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false);
		if (target != null) configuration.target(target);
		return configuration.load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(),
			postgres.getPassword()));
	}
}
