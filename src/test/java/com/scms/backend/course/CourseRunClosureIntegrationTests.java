package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
class CourseRunClosureIntegrationTests {
	private static final Instant CLOSE = Instant.parse("2030-01-15T00:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService management;
	@Autowired CourseRunClosureService closure;
	@Autowired CourseWaitlistService waitlists;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate db;
	@Autowired PlatformTransactionManager transactionManager;

	private UUID managerId;
	private UUID coachId;
	private UUID disciplineId;

	@BeforeEach
	void setUp() {
		managerId = account(AccountRole.MANAGER).getId();
		coachId = account(AccountRole.COACH).getId();
		disciplineId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values (?,?,'ACTIVE')", disciplineId,
			"Closure discipline " + disciplineId);
	}

	@Test
	void exactRegistrationBoundaryConfirmsAndClosesDueHoldsAndWaitlist() {
		CourseRunResponse run = openRun(1);
		UUID paidMember = paidEnrollment(run.id());
		UUID heldOrder = heldOrder(run.id());
		UUID waiting = waitlist(run.id(), "WAITING");
		UUID invited = waitlist(run.id(), "INVITED");

		assertThat(closure.closeAtRegistration(run.id(), CLOSE.minusNanos(1))).isFalse();
		assertThat(status(run.id())).isEqualTo("OPEN_FOR_REGISTRATION");
		assertThat(closure.closeAtRegistration(run.id(), CLOSE)).isTrue();

		assertThat(status(run.id())).isEqualTo("CONFIRMED");
		assertThat(status("course_reservations", heldOrder)).isEqualTo("EXPIRED");
		assertThat(status("course_orders", heldOrder)).isEqualTo("EXPIRED");
		assertThat(status("course_payments", heldOrder)).isEqualTo("FAILED");
		assertThat(status("course_waitlists", waiting)).isEqualTo("CANCELLED");
		assertThat(status("course_waitlists", invited)).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select count(*) from notification_outbox where recipient_account_id=? "
			+ "and notification_type='COURSE_RUN_CONFIRMED'", Long.class, paidMember)).isOne();
		assertThat(closure.closeAtRegistration(run.id(), CLOSE)).isFalse();
		assertThat(auditCount("COURSE_RUN_CONFIRMED", run.id())).isOne();
	}

	@Test
	void insufficientEnrollmentCreatesFullRefundOnlyForPaidEnrollmentAndIsRetrySafe() {
		CourseRunResponse run = openRun(2);
		UUID paidMember = paidEnrollment(run.id());
		UUID heldOrder = heldOrder(run.id());
		waitlist(run.id(), "WAITING");

		assertThat(closure.closeAtRegistration(run.id(), CLOSE)).isTrue();
		assertThat(status(run.id())).isEqualTo("CANCELLED");
		assertThat(status("course_reservations", heldOrder)).isEqualTo("EXPIRED");
		assertThat(status("course_orders", heldOrder)).isEqualTo("EXPIRED");
		assertThat(status("course_payments", heldOrder)).isEqualTo("FAILED");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=? "
			+ "and source_type='COURSE_RUN_CANCELLED'", Long.class, run.id())).isOne();
		assertThat(db.queryForObject("select amount_vnd from course_refund_obligations where course_run_id=?",
			BigDecimal.class, run.id())).isEqualByComparingTo("1000000");
		assertThat(db.queryForObject("select count(*) from notification_outbox where recipient_account_id=? "
			+ "and notification_type='COURSE_RUN_CANCELLED'", Long.class, paidMember)).isOne();
		UUID heldMember = db.queryForObject("select member_account_id from course_orders where id=?", UUID.class,
			heldOrder);
		assertThat(db.queryForObject("select count(*) from notification_outbox where recipient_account_id=? "
			+ "and notification_type='COURSE_RUN_CANCELLED'", Long.class, heldMember)).isOne();

		assertThat(closure.closeAtRegistration(run.id(), CLOSE)).isFalse();
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?",
			Long.class, run.id())).isOne();
		assertThat(auditCount("COURSE_RUN_CANCELLED", run.id())).isOne();
	}

	@Test
	void managerCancellationUsesTheSameEffectsAndRepeatedCancellationAddsNoArtifacts() {
		CourseRunResponse run = openRun(2);
		paidEnrollment(run.id());
		UUID heldOrder = heldOrder(run.id());

		CourseRunResponse cancelled = management.cancelRun(managerId, run.id(), new VersionRequest(run.version()));
		assertThat(cancelled.status()).isEqualTo(CourseRunStatus.CANCELLED);
		assertThat(status("course_reservations", heldOrder)).isEqualTo("EXPIRED");
		assertThat(status("course_orders", heldOrder)).isEqualTo("EXPIRED");
		assertThat(status("course_payments", heldOrder)).isEqualTo("FAILED");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?",
			Long.class, run.id())).isOne();
		assertThatThrownBy(() -> management.cancelRun(managerId, run.id(), new VersionRequest(cancelled.version())))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_RUN_INVALID_TRANSITION"));
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?",
			Long.class, run.id())).isOne();
		assertThat(auditCount("COURSE_RUN_CANCELLED", run.id())).isOne();
		assertThat(db.queryForObject("select actor_account_id from audit_events where action='COURSE_RUN_CANCELLED' "
			+ "and target_id=?", UUID.class, run.id())).isEqualTo(managerId);
		assertThat(db.queryForObject("select after_data->>'reasonCode' from audit_events "
			+ "where action='COURSE_RUN_CANCELLED' and target_id=?", String.class, run.id()))
			.isEqualTo("MANAGER_CANCELLED");
	}

	@Test
	void auditFailureRollsBackEveryCancellationArtifact() {
		CourseRunResponse run = openRun(2);
		paidEnrollment(run.id());
		UUID heldOrder = heldOrder(run.id());
		UUID waiting = waitlist(run.id(), "WAITING");
		String constraint = "chk_test_reject_course_run_cancel_audit";
		db.execute("alter table audit_events add constraint " + constraint
			+ " check (action <> 'COURSE_RUN_CANCELLED') not valid");
		try {
			assertThatThrownBy(() -> closure.closeAtRegistration(run.id(), CLOSE))
				.rootCause().hasMessageContaining(constraint);
			assertThat(status(run.id())).isEqualTo("OPEN_FOR_REGISTRATION");
			assertThat(status("course_reservations", heldOrder)).isEqualTo("HELD");
			assertThat(status("course_orders", heldOrder)).isEqualTo("PENDING_PAYMENT");
			assertThat(status("course_payments", heldOrder)).isEqualTo("PENDING");
			assertThat(status("course_waitlists", waiting)).isEqualTo("WAITING");
			assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?",
				Long.class, run.id())).isZero();
			assertThat(db.queryForObject("select count(*) from notification_outbox where target_id=?",
				Long.class, run.id())).isZero();
		}
		finally {
			db.execute("alter table audit_events drop constraint " + constraint);
		}
	}

	@Test
	void promotionAndClosureWaitOnTheCourseRunLockWithoutFormingALockCycle() throws Exception {
		CourseRunResponse run = openRun(2);
		waitlist(run.id(), "WAITING");
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		CountDownLatch blockerLocked = new CountDownLatch(1);
		CountDownLatch releaseBlocker = new CountDownLatch(1);
		CountDownLatch promotionStarted = new CountDownLatch(1);
		CountDownLatch closureStarted = new CountDownLatch(1);
		AtomicInteger blockerPid = new AtomicInteger();
		AtomicInteger promotionPid = new AtomicInteger();
		AtomicInteger closurePid = new AtomicInteger();

		try (var executor = Executors.newFixedThreadPool(3)) {
			var blocker = executor.submit(() -> transactions.executeWithoutResult(status -> {
				blockerPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				db.queryForObject("select id from course_runs where id=? for update", UUID.class, run.id());
				blockerLocked.countDown();
				await(releaseBlocker);
			}));
			assertThat(blockerLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var promotion = executor.submit(() -> transactions.execute(status -> {
				promotionPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				promotionStarted.countDown();
				return waitlists.promote(run.id(), CLOSE);
			}));
			var close = executor.submit(() -> transactions.execute(status -> {
				closurePid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				closureStarted.countDown();
				return closure.closeAtRegistration(run.id(), CLOSE);
			}));
			try {
				assertThat(promotionStarted.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(closureStarted.await(10, TimeUnit.SECONDS)).isTrue();
				awaitDatabaseLock(promotionPid.get(), blockerPid.get());
				awaitDatabaseLock(closurePid.get(), blockerPid.get());
				releaseBlocker.countDown();
				assertThat(promotion.get(10, TimeUnit.SECONDS)).isNotNull();
				assertThat(close.get(10, TimeUnit.SECONDS)).isTrue();
				blocker.get(10, TimeUnit.SECONDS);
			}
			finally {
				releaseBlocker.countDown();
			}
		}
		assertThat(status(run.id())).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select status from course_waitlists where course_run_id=?",
			String.class, run.id())).isEqualTo("CANCELLED");
	}

	@Test
	void acceptanceAndClosureWaitOnTheCourseRunLockWithoutLeavingAnOpenOrderOnCancelledRun() throws Exception {
		CourseRunResponse run = openRun(2);
		UUID invited = waitlist(run.id(), "INVITED");
		UUID memberId = db.queryForObject("select member_account_id from course_waitlists where id=?", UUID.class,
			invited);
		profiles.saveAndFlush(new MemberProfile(accounts.findById(memberId).orElseThrow()));
		TransactionTemplate transactions = new TransactionTemplate(transactionManager);
		CountDownLatch blockerLocked = new CountDownLatch(1);
		CountDownLatch releaseBlocker = new CountDownLatch(1);
		CountDownLatch acceptanceStarted = new CountDownLatch(1);
		CountDownLatch closureStarted = new CountDownLatch(1);
		AtomicInteger blockerPid = new AtomicInteger();
		AtomicInteger acceptancePid = new AtomicInteger();
		AtomicInteger closurePid = new AtomicInteger();
		boolean accepted = false;

		try (var executor = Executors.newFixedThreadPool(3)) {
			var blocker = executor.submit(() -> transactions.executeWithoutResult(status -> {
				blockerPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				db.queryForObject("select id from course_runs where id=? for update", UUID.class, run.id());
				blockerLocked.countDown();
				await(releaseBlocker);
			}));
			assertThat(blockerLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var acceptance = executor.submit(() -> transactions.execute(status -> {
				acceptancePid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				acceptanceStarted.countDown();
				try {
					waitlists.accept(memberId, invited, "accept-close-race-" + invited);
					return true;
				}
				catch (CourseWaitlistException expectedAfterClosure) {
					return false;
				}
			}));
			var close = executor.submit(() -> transactions.execute(status -> {
				closurePid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				closureStarted.countDown();
				return closure.closeAtRegistration(run.id(), CLOSE);
			}));
			try {
				assertThat(acceptanceStarted.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(closureStarted.await(10, TimeUnit.SECONDS)).isTrue();
				awaitDatabaseLock(acceptancePid.get(), blockerPid.get());
				awaitDatabaseLock(closurePid.get(), blockerPid.get());
				releaseBlocker.countDown();
				accepted = acceptance.get(10, TimeUnit.SECONDS);
				assertThat(close.get(10, TimeUnit.SECONDS)).isTrue();
				blocker.get(10, TimeUnit.SECONDS);
			}
			finally {
				releaseBlocker.countDown();
			}
		}

		assertThat(status(run.id())).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select status from course_waitlists where id=?", String.class, invited))
			.isEqualTo(accepted ? "ACCEPTED" : "CANCELLED");
		assertThat(db.queryForObject("select count(*) from course_orders where course_run_id=? "
			+ "and status='PENDING_PAYMENT'", Long.class, run.id())).isZero();
		assertThat(db.queryForObject("select count(*) from course_reservations where course_run_id=? "
			+ "and status='HELD'", Long.class, run.id())).isZero();
		UUID acceptedOrder = db.queryForObject("select accepted_order_id from course_waitlists where id=?",
			UUID.class, invited);
		if (acceptedOrder != null) {
			assertThat(db.queryForObject("select status from course_orders where id=?", String.class, acceptedOrder))
				.isEqualTo("EXPIRED");
			assertThat(db.queryForObject("select status from course_reservations where course_order_id=?",
				String.class, acceptedOrder)).isEqualTo("EXPIRED");
		}
	}

	private CourseRunResponse openRun(int minimum) {
		CourseResponse course = management.createCourse(managerId,
			new CourseCreateRequest("CLOSE-" + UUID.randomUUID().toString().substring(0, 8), "Closure course",
				null, disciplineId, null, minimum));
		course = management.publishCourse(managerId, course.id(), new VersionRequest(course.version()));
		CourseRunResponse run = management.createRun(managerId, new CourseRunCreateRequest(
			"CLOSE-" + UUID.randomUUID().toString().substring(0, 8), course.id(), coachId,
			BigInteger.valueOf(1_000_000), 10, minimum, Instant.parse("2030-01-01T00:00:00Z"), CLOSE,
			Instant.parse("2030-02-01T00:00:00Z"), Instant.parse("2030-03-01T00:00:00Z"), 10, true));
		return management.openRun(managerId, run.id(), new CourseRunOpenRequest(run.version(), "policy-v1"));
	}

	private UUID paidEnrollment(UUID runId) {
		Account member = account(AccountRole.MEMBER);
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		UUID enrollmentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		Instant created = CLOSE.minusSeconds(3600);
		Instant expires = CLOSE.plusSeconds(3600);
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key,created_at,updated_at) "
			+ "values(?,?,?,?,?,1000000,'VND','PAID',?,?,?)", orderId, reference, member.getId(), managerId, runId,
			"paid-" + orderId, java.sql.Timestamp.from(created), java.sql.Timestamp.from(created));
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,"
			+ "created_at,updated_at) values(?,?,?,?,'ENROLLED',?,?,?)", reservationId, orderId, member.getId(), runId,
			java.sql.Timestamp.from(expires), java.sql.Timestamp.from(created), java.sql.Timestamp.from(created));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,"
			+ "payment_reference,amount,currency_code,status,deadline_at,method,processed_by_account_id,paid_at) "
			+ "values(?,?,?,?,?, ?,1000000,'VND','PAID',?,'CASH',?,?)", paymentId, orderId, reservationId,
			member.getId(), managerId, reference, java.sql.Timestamp.from(expires), managerId,
			java.sql.Timestamp.from(created));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,"
			+ "member_account_id,actor_account_id,processed_by_account_id,course_run_id,amount_paid_vnd_snapshot,"
			+ "total_units_snapshot,enrolled_at) values(?,?,?,?,?,?,?,?,1000000,10,?)", enrollmentId, orderId,
			reservationId, paymentId, member.getId(), managerId, managerId, runId, java.sql.Timestamp.from(created));
		return member.getId();
	}

	private UUID heldOrder(UUID runId) {
		Account member = account(AccountRole.MEMBER);
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		Instant created = CLOSE.minusSeconds(600);
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key,created_at,updated_at) "
			+ "values(?,?,?,?,?,1000000,'VND','PENDING_PAYMENT',?,?,?)", orderId, reference, member.getId(), managerId,
			runId, "held-" + orderId, java.sql.Timestamp.from(created), java.sql.Timestamp.from(created));
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at,"
			+ "created_at,updated_at) values(?,?,?,?,'HELD',?,?,?)", reservationId, orderId, member.getId(), runId,
			java.sql.Timestamp.from(CLOSE), java.sql.Timestamp.from(created), java.sql.Timestamp.from(created));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,"
			+ "payment_reference,amount,currency_code,status,deadline_at,method) values(?,?,?,?,?, ?,1000000,'VND',"
			+ "'PENDING',?,'CASH')", paymentId, orderId, reservationId, member.getId(), managerId, reference,
			java.sql.Timestamp.from(CLOSE));
		return orderId;
	}

	private UUID waitlist(UUID runId, String status) {
		UUID memberId = account(AccountRole.MEMBER).getId();
		UUID id = UUID.randomUUID();
		if ("INVITED".equals(status)) {
			db.update("insert into course_waitlists(id,course_run_id,member_account_id,status,invited_at,"
				+ "invitation_expires_at,created_at,updated_at) values(?,?,?,'INVITED',?,?,?,?)", id, runId,
				memberId, java.sql.Timestamp.from(CLOSE.minusSeconds(3600)), java.sql.Timestamp.from(CLOSE),
				java.sql.Timestamp.from(CLOSE.minusSeconds(7200)), java.sql.Timestamp.from(CLOSE.minusSeconds(7200)));
		}
		else {
			db.update("insert into course_waitlists(id,course_run_id,member_account_id,status,created_at,updated_at) "
			+ "values(?,?,?,'WAITING',?,?)", id, runId, memberId,
			java.sql.Timestamp.from(CLOSE.minusSeconds(7200)), java.sql.Timestamp.from(CLOSE.minusSeconds(7200)));
		}
		return id;
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " closure test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private String status(UUID runId) {
		return db.queryForObject("select status from course_runs where id=?", String.class, runId);
	}

	private String status(String table, UUID orderId) {
		String column = switch (table) {
			case "course_orders", "course_waitlists" -> "id";
			case "course_reservations", "course_payments" -> "course_order_id";
			default -> throw new IllegalArgumentException("Unsupported status table: " + table);
		};
		return db.queryForObject("select status from " + table + " where " + column + "=?", String.class, orderId);
	}

	private long auditCount(String action, UUID runId) {
		return db.queryForObject("select count(*) from audit_events where action=? and target_id=?", Long.class,
			action, runId);
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(30, TimeUnit.SECONDS)) throw new AssertionError("Concurrency rendezvous timed out");
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new AssertionError(exception);
		}
	}

	private void awaitDatabaseLock(int waiterPid, int blockerPid) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			Long waiting = db.queryForObject("select count(*) from pg_stat_activity where pid=? "
				+ "and wait_event_type='Lock' and ?=any(pg_blocking_pids(pid))", Long.class, waiterPid, blockerPid);
			if (waiting != null && waiting == 1) return;
			Thread.onSpinWait();
		} while (System.nanoTime() < deadline);
		throw new AssertionError("Timed out waiting for CourseRun lock serialization");
	}
}
