package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
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
class CourseScheduleChangeIntegrationTests {
	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService managers;
	@Autowired CourseEnrollmentService members;
	@Autowired CourseRepository courses;
	@Autowired CourseRunRepository runs;
	@Autowired CourseSessionRepository sessions;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate db;

	private UUID managerId;
	private UUID coachId;
	private UUID disciplineId;
	private UUID memberId;
	private UUID runId;
	private CourseSessionResponse session;
	private UUID responseId;

	@BeforeEach
	void setUp() {
		managerId = account(AccountRole.MANAGER, "Schedule Manager").getId();
		coachId = account(AccountRole.COACH, "Schedule Coach").getId();
		memberId = account(AccountRole.MEMBER, "Schedule Member").getId();
		disciplineId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", disciplineId, "Schedule " + disciplineId);
		CourseResponse course = managers.createCourse(managerId,
			new CourseCreateRequest("SCH-" + shortId(), "Schedule course", null, disciplineId, null, 1));
		course = managers.publishCourse(managerId, course.id(), new VersionRequest(course.version()));
		CourseRunResponse run = managers.createRun(managerId, new CourseRunCreateRequest("SCH-RUN-" + shortId(),
			course.id(), coachId, BigInteger.valueOf(100_000), 10, 1, Instant.parse("2034-12-01T00:00:00Z"),
			Instant.parse("2034-12-15T00:00:00Z"), Instant.parse("2035-01-01T00:00:00Z"),
			Instant.parse("2035-02-01T00:00:00Z"), 1, true));
		runId = run.id();
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,10,'EXCLUSIVE','ACTIVE')",
			roomId, "Schedule room " + roomId);
		Instant startAt = Instant.parse("2035-01-05T02:00:00Z");
		session = managers.createSession(managerId, runId,
			new CourseSessionCreateRequest(1, roomId, coachId, startAt, startAt.plusSeconds(3600), BigInteger.TEN));
		seedEnrollment();
		CourseSessionResponse changed = managers.patchSession(managerId, runId, session.id(),
			new CourseSessionPatchRequest(session.version(), null, null, startAt.plusSeconds(7200),
				startAt.plusSeconds(10800)));
		session = changed;
		responseId = db.queryForObject("select id from course_schedule_change_responses where enrollment_session_id in "
			+ "(select id from course_enrollment_sessions where course_session_id=?)", UUID.class, session.id());
	}

	@Test
	void outboxFailureRollsBackSessionVersionResponseAndAudit() {
		CourseSessionResponse before = session;
		String constraint = "chk_test_reject_schedule_notification";
		db.execute("alter table notification_outbox add constraint " + constraint
			+ " check (notification_type <> 'COURSE_SCHEDULE_CHANGED') not valid");
		try {
			assertThatThrownBy(() -> rescheduleAgain())
				.rootCause().hasMessageContaining(constraint);
			assertAtomicRollback(before);
		}
		finally { db.execute("alter table notification_outbox drop constraint " + constraint); }
	}

	@Test
	void auditFailureRollsBackSessionResponseAndOutbox() {
		CourseSessionResponse before = session;
		String constraint = "chk_test_reject_schedule_audit";
		db.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_SESSION_RESCHEDULED') not valid");
		try {
			assertThatThrownBy(() -> rescheduleAgain())
				.rootCause().hasMessageContaining(constraint);
			assertAtomicRollback(before);
		}
		finally { db.execute("alter table audit_events drop constraint " + constraint); }
	}

	@Test
	void concurrentMemberResponseAndManagerSupersedePreserveOneConsistentOutcome() throws Exception {
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var member = executor.submit(() -> {
				ready.countDown(); await(start);
				try { members.respondToScheduleChange(memberId, responseId,
					new CourseScheduleChangeApiModels.ScheduleChangeResponse("CONTINUE")); return "RESPONDED"; }
				catch (CourseException error) { return error.getCode(); }
			});
			var manager = executor.submit(() -> {
				ready.countDown(); await(start);
				return managers.patchSession(managerId, runId, session.id(), new CourseSessionPatchRequest(
					session.version(), null, null, session.startAt().plusSeconds(7200),
					session.endAt().plusSeconds(7200)));
			});
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			assertThat(manager.get(10, TimeUnit.SECONDS)).isNotNull();
			assertThat(member.get(10, TimeUnit.SECONDS)).isIn("RESPONDED", "COURSE_SCHEDULE_CHANGE_RESPONSE_CLOSED");
		}
		assertThat(db.queryForObject("select status from course_schedule_change_responses where id=?", String.class,
			responseId)).isIn("RESPONDED", "SUPERSEDED");
		assertThat(db.queryForObject("select count(*) from course_schedule_change_responses where enrollment_session_id in "
			+ "(select id from course_enrollment_sessions where course_session_id=?)", Long.class, session.id())).isEqualTo(2);
		assertThat(db.queryForObject("select count(*) from course_schedule_change_responses where enrollment_session_id in "
			+ "(select id from course_enrollment_sessions where course_session_id=?) and status='PENDING'", Long.class,
			session.id())).isOne();
	}

	private void rescheduleAgain() {
		managers.patchSession(managerId, runId, session.id(), new CourseSessionPatchRequest(session.version(), null,
			null, session.startAt().plusSeconds(7200), session.endAt().plusSeconds(7200)));
	}

	private void assertAtomicRollback(CourseSessionResponse before) {
		CourseSession stored = sessions.findByIdAndCourseRunId(before.id(), runId).orElseThrow();
		assertThat(stored.getStartAt()).isEqualTo(before.startAt());
		assertThat(stored.getEndAt()).isEqualTo(before.endAt());
		assertThat(stored.getVersion()).isEqualTo(before.version());
		assertThat(db.queryForObject("select count(*) from course_schedule_change_responses where enrollment_session_id in "
			+ "(select id from course_enrollment_sessions where course_session_id=?)", Long.class, before.id())).isOne();
		assertThat(db.queryForObject("select count(*) from course_schedule_change_responses where id=?", Long.class,
			responseId)).isOne();
		assertThat(db.queryForObject("select status from course_schedule_change_responses where id=?", String.class,
			responseId)).isEqualTo("PENDING");
		assertThat(db.queryForObject("select count(*) from notification_outbox where notification_type='COURSE_SCHEDULE_CHANGED'"
			+ " and target_id=?", Long.class, before.id())).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_SESSION_RESCHEDULED'"
			+ " and target_id=?", Long.class, before.id())).isOne();
	}

	private void seedEnrollment() {
		UUID enrollmentId = UUID.randomUUID(), enrollmentSessionId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID(), reservationId = UUID.randomUUID(), paymentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		Instant expiresAt = Instant.parse("2040-01-01T00:00:00Z");
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,100000,'VND','PAID',?)",
			orderId, reference, memberId, memberId, runId, UUID.randomUUID().toString());
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) "
			+ "values(?,?,?,?,'ENROLLED',?)", reservationId, orderId, memberId, runId,
			java.sql.Timestamp.from(expiresAt));
		db.update("""
			insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,
			payment_reference,amount,currency_code,status,deadline_at,bank_code_snapshot,bank_account_number_snapshot,
			bank_account_name_snapshot,method,paid_at)
			values(?,?,?,?,?,?,100000,'VND','PAID',?,'CASH','CASH','CASH','CASH',current_timestamp)
			""", paymentId, orderId, reservationId, memberId, memberId, reference,
			java.sql.Timestamp.from(expiresAt));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,"
			+ "member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at) "
			+ "values(?,?,?,?,?,?,?,100000,1,current_timestamp)", enrollmentId, orderId, reservationId, paymentId,
			memberId, memberId, runId);
		db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,"
			+ "unit_number,allocation_vnd,disposition) values(?,?,?,?,1,100000,'ENROLLED')", enrollmentSessionId,
			enrollmentId, session.id(), runId);
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }

	private static void await(CountDownLatch latch) {
		try { if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Concurrency rendezvous timed out"); }
		catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new AssertionError(exception); }
	}
}
