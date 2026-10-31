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
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class CoursePaymentMigrationV53Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() {
		flyway().clean();
	}

	@Test
	void freshSchemaEnforcesCoursePaymentOwnershipReferenceDeadlineAndDestination() {
		flyway().migrate();
		JdbcTemplate db = db();
		Fixture owner = fixture(db);
		UUID payment = insertCoursePayment(db, owner, owner.memberId(), owner.actorId(), owner.reference(),
			500000, owner.expiresAt(), "MB", "123456789", "SCMS GYM");

		assertThat(db.queryForList("select indexname from pg_indexes where tablename='course_payments'",
			String.class)).contains("uq_course_payments_reference", "idx_course_payments_order_created_at");
		assertThatThrownBy(() -> insertCoursePayment(db, owner, owner.memberId(), owner.actorId(), owner.reference(),
			500000, owner.expiresAt(), "MB", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture memberMismatch = fixture(db);
		Fixture otherMember = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, memberMismatch, otherMember.memberId(),
			memberMismatch.actorId(), memberMismatch.reference(), 500000, memberMismatch.expiresAt(),
			"MB", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture actorMismatch = fixture(db);
		Fixture otherActor = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, actorMismatch, actorMismatch.memberId(),
			otherActor.actorId(), actorMismatch.reference(), 500000, actorMismatch.expiresAt(),
			"MB", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture referenceMismatch = fixture(db);
		Fixture otherReference = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, referenceMismatch, referenceMismatch.memberId(),
			referenceMismatch.actorId(), otherReference.reference(), 500000, referenceMismatch.expiresAt(),
			"MB", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture amountMismatch = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, amountMismatch, amountMismatch.memberId(),
			amountMismatch.actorId(), amountMismatch.reference(), 500001, amountMismatch.expiresAt(),
			"MB", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture deadlineMismatch = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, deadlineMismatch, deadlineMismatch.memberId(),
			deadlineMismatch.actorId(), deadlineMismatch.reference(), 500000,
			deadlineMismatch.expiresAt().plusSeconds(1), "MB", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture blankDestination = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, blankDestination, blankDestination.memberId(),
			blankDestination.actorId(), blankDestination.reference(), 500000, blankDestination.expiresAt(),
			" ", "123456789", "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		Fixture nullDestination = fixture(db);
		assertThatThrownBy(() -> insertCoursePayment(db, nullDestination, nullDestination.memberId(),
			nullDestination.actorId(), nullDestination.reference(), 500000, nullDestination.expiresAt(),
			"MB", null, "SCMS GYM"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update(
			"update course_payments set bank_account_number_snapshot='changed' where id=?", payment))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("destination snapshot is immutable");
	}

	@Test
	void providerEventsAllowZeroOrOneTargetAndRemainGlobalAndAppendOnly() {
		flyway().migrate();
		JdbcTemplate db = db();
		Fixture fixture = fixture(db);
		UUID coursePayment = insertCoursePayment(db, fixture, fixture.memberId(), fixture.actorId(),
			fixture.reference(), 500000, fixture.expiresAt(), "MB", "123456789", "SCMS GYM");
		UUID membershipPayment = membershipPayment(db, fixture.memberId(), fixture.actorId());
		UUID courseEvent = insertEvent(db, "course-1", null, coursePayment, "COURSE_PAYMENT_RECORDED");
		insertEvent(db, "membership-1", membershipPayment, null, "FULFILLED");
		insertEvent(db, "unmatched-1", null, null, "REFERENCE_MISMATCH");

		assertThatThrownBy(() -> insertEvent(db, "both", membershipPayment, coursePayment,
			"COURSE_PAYMENT_RECORDED")).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertEvent(db, "course-1", null, coursePayment,
			"COURSE_PAYMENT_RECORDED")).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("update payment_provider_events set detail='changed' where id=?",
			courseEvent)).isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("payment_provider_events is append-only");
		assertThatThrownBy(() -> db.update("delete from payment_provider_events where id=?", courseEvent))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("payment_provider_events is append-only");
	}

	@Test
	void acceptedWaitlistOrderMustBelongToTheSameMemberAndRun() {
		flyway().migrate();
		JdbcTemplate db = db();
		Fixture owner = fixture(db);
		Fixture other = fixture(db);
		UUID waitlist = UUID.randomUUID();
		db.update("insert into course_waitlists(id,course_run_id,member_account_id,status) values(?,?,?,'WAITING')",
			waitlist, owner.runId(), owner.memberId());

		assertThatThrownBy(() -> db.update("""
			update course_waitlists set status='ACCEPTED',invited_at=?,invitation_expires_at=?,accepted_order_id=?
			where id=?
			""", Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")),
			Timestamp.from(Instant.parse("2030-01-02T00:00:00Z")), other.orderId(), waitlist))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void v52UpgradePreservesExistingCourseAndProviderEvidenceRows() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("52").load().migrate();
		JdbcTemplate db = db();
		Fixture fixture = fixture(db);
		UUID event = insertLegacyEvent(db, "legacy-unmatched");
		String orderBefore = snapshot(db, "course_orders", fixture.orderId());
		String reservationBefore = snapshot(db, "course_reservations", fixture.reservationId());
		String eventBefore = snapshot(db, "payment_provider_events", event);

		flyway().migrate();

		assertThat(snapshot(db, "course_orders", fixture.orderId())).isEqualTo(orderBefore);
		assertThat(snapshot(db, "course_reservations", fixture.reservationId())).isEqualTo(reservationBefore);
		assertThat(db.queryForObject("select (to_jsonb(existing) - 'course_payment_id')::text from "
			+ "(select * from payment_provider_events where id=?) existing", String.class, event))
			.isEqualTo(eventBefore);
		assertThat(db.queryForObject("select course_payment_id from payment_provider_events where id=?",
			UUID.class, event)).isNull();
		assertThat(db.queryForObject("select max(version::integer) from flyway_schema_history where success",
			Integer.class)).isEqualTo(62);
	}

	private Fixture fixture(JdbcTemplate db) {
		UUID actor = account(db, "RECEPTIONIST");
		UUID member = account(db, "MEMBER");
		db.update("insert into member_profiles(account_id) values (?)", member);
		UUID coach = account(db, "COACH");
		UUID discipline = UUID.randomUUID();
		UUID course = UUID.randomUUID();
		UUID run = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID reservation = UUID.randomUUID();
		String reference = "COR-" + order.toString().replace("-", "").toUpperCase();
		Instant expiry = Instant.parse("2031-01-01T00:15:00Z");
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
		db.update("""
			insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,
				tuition_vnd_snapshot,currency_code,status,idempotency_key)
			values (?,?,?,?,?,500000,'VND','PENDING_PAYMENT',?)
			""", order, reference, member, actor, run, "key-" + order);
		db.update("""
			insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at)
			values (?,?,?,?,'HELD',?)
			""", reservation, order, member, run, Timestamp.from(expiry));
		return new Fixture(actor, member, order, reservation, reference, expiry, run);
	}

	private UUID account(JdbcTemplate db, String role) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values (?,?,'ACTIVE',?,?,?,date '1990-01-01','unused')
			""", id, role, role + " Migration", "09" + id.toString().substring(0, 8), id + "@test.invalid");
		return id;
	}

	private UUID insertCoursePayment(JdbcTemplate db, Fixture fixture, UUID member, UUID actor, String reference,
			long amount, Instant deadline, String bankCode, String bankAccount, String bankName) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,
				payment_reference,amount,currency_code,status,deadline_at,bank_code_snapshot,
				bank_account_number_snapshot,bank_account_name_snapshot)
			values (?,?,?,?,?,?,?,'VND','PENDING',?,?,?,?)
			""", id, fixture.orderId(), fixture.reservationId(), member, actor, reference, amount,
			Timestamp.from(deadline), bankCode, bankAccount, bankName);
		return id;
	}

	private UUID membershipPayment(JdbcTemplate db, UUID member, UUID actor) {
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		db.update("""
			insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,
				status,created_by_account_id) values(?,'BASIC','V53 Offer','Test',500000,'VND',30,'ACTIVE',?)
			""", offer, actor);
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
				payment_method,status,expires_at)
			values(?,?,?,?,?,'V53 Offer','BASIC',500000,'VND',30,'BANK_TRANSFER','PENDING_PAYMENT',?)
			""", order, "ORD-" + order, member, actor, offer, Timestamp.from(Instant.parse("2031-01-01T00:15:00Z")));
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,
				provider,provider_reference) values(?,?,'BANK_TRANSFER','PENDING',500000,'VND',?,'SEPAY',?)
			""", payment, order, "SCMS-" + order, "SCMS-" + order);
		return payment;
	}

	private UUID insertEvent(JdbcTemplate db, String transaction, UUID membershipPayment, UUID coursePayment,
			String outcome) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into payment_provider_events(id,provider,provider_transaction_id,payment_id,course_payment_id,
				transfer_amount,payment_reference,account_number_last4,payload_sha256,outcome,detail)
			values(?,'SEPAY',?,?,?,500000,'REFERENCE','6789',?,?, 'Migration test')
			""", id, transaction, membershipPayment, coursePayment, "a".repeat(64), outcome);
		return id;
	}

	private UUID insertLegacyEvent(JdbcTemplate db, String transaction) {
		UUID id = UUID.randomUUID();
		db.update("""
			insert into payment_provider_events(id,provider,provider_transaction_id,payment_id,transfer_amount,
				payment_reference,account_number_last4,payload_sha256,outcome,detail)
			values(?,'SEPAY',?,null,500000,'UNKNOWN','6789',?,'REFERENCE_MISMATCH','Migration test')
			""", id, transaction, "a".repeat(64));
		return id;
	}

	private String snapshot(JdbcTemplate db, String table, UUID id) {
		return db.queryForObject("select to_jsonb(snapshot)::text from (select * from " + table
			+ " where id=?) snapshot", String.class, id);
	}

	private Flyway flyway() {
		return Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false).load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(
			postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	private record Fixture(UUID actorId, UUID memberId, UUID orderId, UUID reservationId, String reference,
		Instant expiresAt, UUID runId) { }
}
