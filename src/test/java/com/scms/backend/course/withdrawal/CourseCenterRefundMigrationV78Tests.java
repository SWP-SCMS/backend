package com.scms.backend.course.withdrawal;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CourseCenterRefundMigrationV78Tests {

	@Test
	void migrationClassifiesOnlyUnansweredRegularMovesAndPreservesAllWithdrawalSnapshots() throws Exception {
		String external = System.getenv("COURSE_REFUND_MIGRATION_TEST_URL");
		if (external != null) {
			try (var db = DriverManager.getConnection(external, System.getenv("COURSE_REFUND_MIGRATION_TEST_USER"), System.getenv("COURSE_REFUND_MIGRATION_TEST_PASSWORD"))) { verifyMigration(db); }
		} else {
			try (var postgres = new PostgreSQLContainer("postgres:17")) {
				postgres.start();
				try (var db = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) { verifyMigration(db); }
			}
		}
	}

	private void verifyMigration(Connection db) throws Exception {
		db.setAutoCommit(false);
		try (var sql = db.createStatement()) {
			// Every table is temporary and search_path is restricted to pg_temp.
			// The optional external connection never reads or writes application tables.
			sql.execute("set local search_path=pg_temp");
			String allowed = "cause_code in ('VOLUNTARY','START_DELAY_OVER_7_DAYS','MEMBER_SCHEDULE_CONFLICT','REDUCED_UNIT_COUNT')";
			sql.execute("create temporary table course_schedule_change_events(id uuid primary key,cause_code text constraint chk_course_schedule_change_event_cause check (" + allowed + "))");
			sql.execute("create temporary table course_sessions(id uuid primary key,is_makeup boolean)");
			sql.execute("create temporary table course_enrollment_sessions(id uuid primary key,course_session_id uuid,course_enrollment_id uuid)");
			sql.execute("create temporary table course_schedule_change_responses(id uuid primary key,enrollment_session_id uuid,change_event_id uuid,status text,response text,cause_code text constraint chk_course_schedule_change_response_cause check (" + allowed + "),old_start_at timestamptz,old_end_at timestamptz,new_start_at timestamptz,new_end_at timestamptz)");
			sql.execute("create temporary table course_withdrawal_requests(id uuid primary key,course_enrollment_id uuid,schedule_change_response_id uuid,change_event_id uuid,status text,refund_amount_vnd numeric,cause_code text constraint chk_course_withdrawal_cause check (" + allowed + "))");
			UUID move = fixture(db, false, "PENDING", "VOLUNTARY", true, null);
			UUID window = fixture(db, false, "PENDING", "VOLUNTARY", false, null);
			UUID makeup = fixture(db, true, "PENDING", "VOLUNTARY", true, null);
			UUID delay = fixture(db, false, "PENDING", "START_DELAY_OVER_7_DAYS", true, null);
			UUID reduced = fixture(db, false, "PENDING", "REDUCED_UNIT_COUNT", true, null);
			UUID conflict = fixture(db, false, "PENDING", "MEMBER_SCHEDULE_CONFLICT", true, null);
			UUID historical = fixture(db, false, "SUPERSEDED", "VOLUNTARY", true, null);
			for (String status : java.util.List.of("PENDING", "APPROVED", "CANCELLED")) fixture(db, false, "RESPONDED", "VOLUNTARY", true, status);
			UUID existingGroupRequest = fixture(db, false, "PENDING", "VOLUNTARY", true, "PENDING");
			String before = snapshots(db);
			try (var input = getClass().getResourceAsStream("/db/migration/V78__center_session_reschedule_refunds.sql")) {
				sql.execute(new String(input.readAllBytes(), StandardCharsets.UTF_8));
			}
			assertThat(cause(db, move)).isEqualTo("CENTER_SESSION_RESCHEDULE");
			for (UUID unchanged : java.util.List.of(window, makeup, historical, existingGroupRequest)) assertThat(cause(db, unchanged)).isEqualTo("VOLUNTARY");
			assertThat(cause(db, delay)).isEqualTo("START_DELAY_OVER_7_DAYS");
			assertThat(cause(db, reduced)).isEqualTo("REDUCED_UNIT_COUNT");
			assertThat(cause(db, conflict)).isEqualTo("MEMBER_SCHEDULE_CONFLICT");
			assertThat(snapshots(db)).isEqualTo(before);
			sql.execute("insert into course_schedule_change_events values(gen_random_uuid(),'CENTER_SESSION_RESCHEDULE')");
			sql.execute("insert into course_withdrawal_requests(id,cause_code) values(gen_random_uuid(),'CENTER_SESSION_RESCHEDULE')");
		} finally { db.rollback(); }
	}

	private UUID fixture(Connection db, boolean makeup, String status, String cause, boolean moved, String withdrawalStatus) throws Exception {
		UUID session = UUID.randomUUID(), enrollment = UUID.randomUUID(), unit = UUID.randomUUID(), event = UUID.randomUUID(), response = UUID.randomUUID();
		try (var sql = db.createStatement()) {
			sql.execute("insert into course_sessions values('" + session + "'," + makeup + ")");
			sql.execute("insert into course_enrollment_sessions values('" + unit + "','" + session + "','" + enrollment + "')");
			sql.execute("insert into course_schedule_change_responses values('" + response + "','" + unit + "','" + event + "','" + status + "'," + (status.equals("RESPONDED") ? "'REQUEST_WITHDRAWAL'" : "null") + ",'" + cause + "','2035-01-01 02:00Z','2035-01-01 03:00Z','2035-01-01 " + (moved ? "04:00Z','2035-01-01 05:00Z'" : "02:00Z','2035-01-01 03:00Z'") + ")");
			if (withdrawalStatus != null) sql.execute("insert into course_withdrawal_requests values(gen_random_uuid(),'" + enrollment + "',null,'" + event + "','" + withdrawalStatus + "',800000,'" + cause + "')");
		}
		return response;
	}

	private String cause(Connection db, UUID id) throws Exception {
		try (var sql = db.prepareStatement("select cause_code from course_schedule_change_responses where id=?")) {
			sql.setObject(1, id);
			try (var rows = sql.executeQuery()) { rows.next(); return rows.getString(1); }
		}
	}

	private String snapshots(Connection db) throws Exception {
		try (var sql = db.createStatement(); var rows = sql.executeQuery("select jsonb_agg(to_jsonb(w) order by id)::text from course_withdrawal_requests w")) { rows.next(); return rows.getString(1); }
	}
}
