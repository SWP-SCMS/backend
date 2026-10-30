package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
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
class CourseOrderMigrationV52Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() {
		flyway().clean();
	}

	@Test
	void freshSchemaEnforcesForeignKeysStatusesIdempotencyAndOneHeldReservation() {
		flyway().migrate();
		JdbcTemplate db = db();
		Fixture fixture = fixture(db);
		UUID orderId = insertOrder(db, fixture, "key-1");
		insertReservation(db, fixture, orderId, "HELD", Instant.parse("2031-01-01T00:15:00Z"));

		assertThat(db.queryForList("select table_name from information_schema.tables where table_schema='public'",
			String.class)).contains("course_orders", "course_reservations");
		assertThat(db.queryForList("select indexname from pg_indexes where schemaname='public'", String.class))
			.contains("uq_course_orders_actor_idempotency", "uq_course_orders_reservation_tuple",
				"uq_course_reservations_member_run_held",
				"idx_course_reservations_run_capacity", "idx_course_reservations_expiry_cursor");
		assertThat(db.queryForObject("select count(*) from course_reservations", Long.class)).isOne();
		assertThatThrownBy(() -> insertOrder(db, fixture, "key-1"))
			.isInstanceOf(DataIntegrityViolationException.class);

		UUID secondOrder = insertOrder(db, fixture, "key-2");
		assertThatThrownBy(() -> insertReservation(db, fixture, secondOrder, "HELD",
			Instant.parse("2031-01-01T00:15:00Z"))).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertReservation(db, fixture, secondOrder, "INVALID",
			Instant.parse("2031-01-01T00:15:00Z"))).isInstanceOf(DataIntegrityViolationException.class);
		UUID expiredOrder = insertOrder(db, fixture, "key-3");
		assertThatThrownBy(() -> db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at)
			values (?,?,?,?,'EXPIRED',current_timestamp)
			""", UUID.randomUUID(), expiredOrder, fixture.memberId(), fixture.runId()))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("delete from course_runs where id=?", fixture.runId()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void supportedV51UpgradePreservesCatalogAndAddsV52() {
		Flyway v51 = Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("51").load();
		v51.migrate();
		JdbcTemplate db = db();
		Fixture fixture = fixture(db);

		Flyway upgraded = flyway();
		upgraded.migrate();
		upgraded.validate();

		assertThat(db.queryForObject("select count(*) from course_runs where id=?", Long.class, fixture.runId()))
			.isOne();
		assertThat(db.queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(57);
		assertThat(db.queryForObject("select count(*) from course_orders", Long.class)).isZero();
	}

	@Test
	void reservationMemberAndRunMustMatchTheReferencedOrderTuple() {
		flyway().migrate();
		JdbcTemplate db = db();
		Fixture owner = fixture(db);
		Fixture other = fixture(db);

		UUID mismatchedMemberOrder = insertOrder(db, owner, "tuple-member");
		assertThatThrownBy(() -> insertReservation(db, mismatchedMemberOrder, other.memberId(), owner.runId()))
			.isInstanceOf(DataIntegrityViolationException.class);

		UUID mismatchedRunOrder = insertOrder(db, owner, "tuple-run");
		assertThatThrownBy(() -> insertReservation(db, mismatchedRunOrder, owner.memberId(), other.runId()))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private Fixture fixture(JdbcTemplate db) {
		UUID actor = account(db, "RECEPTIONIST");
		UUID member = account(db, "MEMBER");
		db.update("insert into member_profiles(account_id) values (?)", member);
		UUID coach = account(db, "COACH");
		UUID discipline = UUID.randomUUID();
		UUID course = UUID.randomUUID();
		UUID run = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", discipline, "D-" + discipline);
		db.update("insert into courses(id,code,name,discipline_id,status) values (?,?,?,?,'PUBLISHED')",
			course, "C-" + course, "Course", discipline);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
				minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,
				planned_end_at,total_units,named_coach_commitment,policy_version_snapshot,status)
			values (?,?,?,?,500000,2,1,?,?,?,?,1,true,'P1','OPEN_FOR_REGISTRATION')
			""", run, "R-" + run, course, coach, Timestamp.from(Instant.parse("2030-12-01T00:00:00Z")),
			Timestamp.from(Instant.parse("2030-12-31T00:00:00Z")),
			Timestamp.from(Instant.parse("2031-01-01T00:00:00Z")),
			Timestamp.from(Instant.parse("2031-02-01T00:00:00Z")));
		return new Fixture(actor, member, run);
	}

	private UUID account(JdbcTemplate db, String role) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values (?,?,'ACTIVE',?,?,?,date '1990-01-01','unused')
			""", id, role, role + " Migration", "09" + id.toString().substring(0, 8), id + "@test.invalid");
		return id;
	}

	private UUID insertOrder(JdbcTemplate db, Fixture fixture, String key) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,
				tuition_vnd_snapshot,currency_code,status,idempotency_key)
			values (?,?,?,?,?,500000,'VND','PENDING_PAYMENT',?)
			""", id, "COR-" + id.toString().replace("-", "").toUpperCase(), fixture.memberId(),
			fixture.actorId(), fixture.runId(), key);
		return id;
	}

	private void insertReservation(JdbcTemplate db, Fixture fixture, UUID orderId, String status, Instant expiresAt) {
		db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at)
			values (?,?,?,?,?,?)
			""", UUID.randomUUID(), orderId, fixture.memberId(), fixture.runId(), status,
			Timestamp.from(expiresAt));
	}

	private void insertReservation(JdbcTemplate db, UUID orderId, UUID memberId, UUID runId) {
		db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at)
			values (?,?,?,?,'HELD',?)
			""", UUID.randomUUID(), orderId, memberId, runId,
			Timestamp.from(Instant.parse("2031-01-01T00:15:00Z")));
	}

	private Flyway flyway() {
		return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false).load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(
			postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	private record Fixture(UUID actorId, UUID memberId, UUID runId) { }
}
