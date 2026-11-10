package com.scms.backend.course.run;

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
class CourseRunRescheduleMigrationV69Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	private JdbcTemplate db;

	private UUID actor, run, event;

	@BeforeEach
	void prepare() {
		flyway(null).clean();
		db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	@Test
	void freshDatabaseEnforcesReplayKeyFingerprintAndForeignKeys() {
		flyway(null).migrate();
		fixture();
		insert("key", "a".repeat(64), event, "{}");
		UUID spare = UUID.randomUUID();
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,actor_account_id,cause_code,old_planned_start_at,old_planned_end_at,
			 new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			select ?,course_run_id,actor_account_id,cause_code,old_planned_start_at,old_planned_end_at,
			 new_planned_start_at,new_planned_end_at,old_total_units,new_total_units from course_schedule_change_events where id=?
			""", spare, event);
		assertThatThrownBy(() -> insert("key", "a".repeat(64), spare, "{}")).rootCause().hasMessageContaining("course_run_reschedules_pkey");
		assertThatThrownBy(() -> insert("other", "invalid", spare, "{}")).rootCause().hasMessageContaining("chk_course_reschedule_fingerprint");
		assertThatThrownBy(() -> insert(" ", "a".repeat(64), spare, "{}")).rootCause().hasMessageContaining("chk_course_reschedule_key");
		assertThatThrownBy(() -> insert("other", "a".repeat(64), spare, "[]")).rootCause().hasMessageContaining("chk_course_reschedule_response");
		assertThatThrownBy(() -> insert("other", "a".repeat(64), UUID.randomUUID(), "{}")).rootCause().hasMessageContaining("course_run_reschedules_change_event_id_fkey");
		assertThat(db.queryForObject("select count(*) from course_run_reschedules", Long.class)).isOne();
		assertThatThrownBy(() -> db.update("delete from course_schedule_change_events where id=?", event)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void upgradeFromV68PreservesExistingRunEventsAndAudit() {
		flyway("68").migrate();
		fixture();
		db.update("insert into audit_events(id,actor_account_id,action,target_type,target_id,after_data) values(?,?,'COURSE_RUN_SCHEDULE_CHANGED','COURSE_RUN',?,'{}'::jsonb)", UUID.randomUUID(), actor, run);
		var runs = db.queryForList("select * from course_runs order by id");
		var events = db.queryForList("select * from course_schedule_change_events order by id");
		var audits = db.queryForList("select * from audit_events order by id");
		flyway(null).migrate();
		flyway(null).migrate();
		flyway(null).validate();
		assertThat(db.queryForList("select * from course_runs order by id")).isEqualTo(runs);
		assertThat(db.queryForList("select * from course_schedule_change_events order by id")).isEqualTo(events);
		assertThat(db.queryForList("select * from audit_events order by id")).isEqualTo(audits);
		assertThat(db.queryForObject("select count(*) from course_run_reschedules", Long.class)).isZero();
		insert("after-upgrade", "b".repeat(64), event, "{\"changeEventId\":\"" + event + "\"}");
		assertThat(db.queryForObject("select response_json->>'changeEventId' from course_run_reschedules", String.class)).isEqualTo(event.toString());
	}

	private void insert(String key, String fingerprint, UUID eventId, String response) {
		db.update("insert into course_run_reschedules(course_run_id,actor_account_id,idempotency_key,request_fingerprint,change_event_id,response_json) values(?,?,?,?,?,?::jsonb)", run, actor, key, fingerprint, eventId, response);
	}

	private void fixture() {
		actor = UUID.randomUUID();
		run = UUID.randomUUID();
		event = UUID.randomUUID();
		UUID discipline = UUID.randomUUID(), course = UUID.randomUUID();
		db.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,'MANAGER','ACTIVE','Migration Manager','0900000001','manager@example.test','1990-01-01','hash')", actor);
		db.update("insert into disciplines(id,name,status) values(?,'Migration','ACTIVE')", discipline);
		db.update("insert into courses(id,code,name,discipline_id,status) values(?,'MIG','Migration',?,'PUBLISHED')", course, discipline);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,minimum_enrollment,
			 registration_opens_at,registration_closes_at,planned_start_at,planned_end_at,total_units,named_coach_commitment,policy_version_snapshot,status)
			values(?,'MIG-RUN',?,?,100000,10,1,'2034-12-01T00:00:00Z','2034-12-15T00:00:00Z','2035-01-01T00:00:00Z','2035-02-01T00:00:00Z',1,false,'POLICY','OPEN_FOR_REGISTRATION')
			""", run, course, actor);
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,actor_account_id,cause_code,old_planned_start_at,old_planned_end_at,
			 new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,'VOLUNTARY','2035-01-01T00:00:00Z','2035-02-01T00:00:00Z','2035-01-02T00:00:00Z','2035-02-02T00:00:00Z',1,1)
			""", event, run, actor);
	}

	private Flyway flyway(String target) {
		var config = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).cleanDisabled(false);
		if (target != null)
			config.target(target);
		return config.load();
	}
}
