package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.time.Clock;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseScheduleChangeIntegrationTests {
	private static final Instant NOW = Instant.parse("2034-11-30T00:00:00Z");

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService managers;
	@Autowired CourseEnrollmentService members;
	@Autowired CourseRepository courses;
	@Autowired CourseRunRepository runs;
	@Autowired CourseSessionRepository sessions;
	@Autowired CourseSessionDisruptionService disruptions;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate db;
	@Autowired TransactionTemplate transactions;
	@Autowired MockMvc mockMvc;
	@Autowired JwtEncoder jwtEncoder;
	@MockitoBean Clock clock;

	private UUID managerId;
	private UUID coachId;
	private UUID disciplineId;
	private UUID memberId;
	private UUID runId;
	private CourseSessionResponse session;
	private UUID responseId;

	@BeforeEach
	void setUp() {
		when(clock.instant()).thenReturn(NOW);
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
		managers.openRun(managerId, runId, new CourseRunOpenRequest(run.version(), "POLICY-1"));
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
	void managerSupersedeBlocksBehindMemberResponseRowLockThenPreservesHistory() throws Exception {
		CountDownLatch memberLocked = new CountDownLatch(1);
		CountDownLatch managerAtReschedule = new CountDownLatch(1);
		CountDownLatch releaseMember = new CountDownLatch(1);
		AtomicInteger memberPid = new AtomicInteger();
		AtomicInteger managerPid = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var member = executor.submit(() -> {
				transactions.executeWithoutResult(status -> {
					memberPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
					db.queryForObject("select id from course_schedule_change_responses where id=? for update",
						UUID.class, responseId);
					memberLocked.countDown();
					await(releaseMember);
					members.respondToScheduleChange(memberId, responseId,
						new CourseScheduleChangeApiModels.ScheduleChangeResponse("CONTINUE"));
				});
				return null;
			});
			assertThat(memberLocked.await(10, TimeUnit.SECONDS)).isTrue();
			var manager = executor.submit(() -> {
				return transactions.execute(status -> {
					managerPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
					managerAtReschedule.countDown();
					return managers.patchSession(managerId, runId, session.id(), new CourseSessionPatchRequest(
						session.version(), null, null, session.startAt().plusSeconds(7200),
						session.endAt().plusSeconds(7200)));
				});
			});
			assertThat(managerAtReschedule.await(10, TimeUnit.SECONDS)).isTrue();
			awaitDatabaseLock(managerPid.get(), memberPid.get());
			releaseMember.countDown();
			assertThat(manager.get(10, TimeUnit.SECONDS)).isNotNull();
			assertThat(member.get(10, TimeUnit.SECONDS)).isNull();
		}
		assertThat(db.queryForObject("select status from course_schedule_change_responses where id=?", String.class,
			responseId)).isEqualTo("RESPONDED");
		assertThat(db.queryForObject("select count(*) from course_schedule_change_responses where enrollment_session_id in "
			+ "(select id from course_enrollment_sessions where course_session_id=?)", Long.class, session.id())).isEqualTo(2);
		assertThat(db.queryForObject("select count(*) from course_schedule_change_responses where enrollment_session_id in "
			+ "(select id from course_enrollment_sessions where course_session_id=?) and status='PENDING'", Long.class,
			session.id())).isOne();
	}

	@Test
	void rescheduleWaitsForRealPaymentFulfillmentAndNotifiesNewMember() throws Exception {
		Account newMember = account(AccountRole.MEMBER, "Late Schedule Member");
		Account receptionist = account(AccountRole.RECEPTIONIST, "Schedule Receptionist");
		UUID heldOrderId = seedHeldOrder(newMember.getId());
		CountDownLatch enrollmentCreated = new CountDownLatch(1);
		CountDownLatch commitFulfillment = new CountDownLatch(1);
		CountDownLatch managerPidReady = new CountDownLatch(1);
		AtomicInteger fulfillmentPid = new AtomicInteger();
		AtomicInteger managerPid = new AtomicInteger();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var payment = executor.submit(() -> transactions.executeWithoutResult(status -> {
				fulfillmentPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				try {
					mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders/{orderId}/payments/cash",
						newMember.getId(), heldOrderId).contextPath("/api/v1")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
						.header("Idempotency-Key", "schedule-race-fulfillment"))
						.andExpect(status().isOk());
				}
				catch (Exception exception) { throw new AssertionError(exception); }
				enrollmentCreated.countDown();
				await(commitFulfillment);
			}));
			assertThat(enrollmentCreated.await(10, TimeUnit.SECONDS)).isTrue();
			var reschedule = executor.submit(() -> transactions.execute(status -> {
				managerPid.set(db.queryForObject("select pg_backend_pid()", Integer.class));
				managerPidReady.countDown();
				return managers.patchSession(managerId, runId, session.id(), new CourseSessionPatchRequest(
					session.version(), null, null, session.startAt().plusSeconds(7200),
					session.endAt().plusSeconds(7200)));
			}));
			try {
				assertThat(managerPidReady.await(10, TimeUnit.SECONDS)).isTrue();
				awaitDatabaseLock(managerPid.get(), fulfillmentPid.get());
				commitFulfillment.countDown();
				payment.get(10, TimeUnit.SECONDS);
				assertThat(reschedule.get(10, TimeUnit.SECONDS)).isNotNull();
			}
			finally {
				commitFulfillment.countDown();
			}
		}
		assertThat(db.queryForObject("select count(*) from course_enrollments where course_run_id=? and member_account_id=?",
			Long.class, runId, newMember.getId())).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where notification_type=? "
			+ "and target_id=? and recipient_account_id=?", Long.class, "COURSE_SCHEDULE_CHANGED", session.id(),
			newMember.getId())).isOne();
	}

	@Test
	void cancellationAndMakeupMoveEnrollmentWithoutChangingUnitOrAllocation() {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		CourseSessionResponse cancelled = disruptions.cancel(managerId, runId, session.id(), session.version());
		assertThat(cancelled.status()).isEqualTo(CourseSessionStatus.CANCELLED_PENDING_MAKEUP);
		assertThat(db.queryForObject("select count(*) from notification_outbox where notification_type=? "
			+ "and target_id=? and recipient_account_id=?", Long.class, "COURSE_SESSION_CANCELLED", session.id(), memberId)).isOne();

		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,10,'EXCLUSIVE','ACTIVE')",
			roomId, "Makeup room " + roomId);
		CourseSessionDisruptionService.CourseMakeupResponse result = disruptions.scheduleMakeup(managerId, runId,
			session.id(), new CourseMakeupRequest(cancelled.version(), roomId, coachId,
				Instant.parse("2035-01-10T02:00:00Z"), Instant.parse("2035-01-10T03:00:00Z")));

		assertThat(result.originalSession().replacementSessionId()).isEqualTo(result.makeupSession().id());
		assertThat(result.makeupSession().unitNumber()).isEqualTo(session.unitNumber());
		assertThat(result.makeupSession().allocationVnd()).isEqualTo(session.allocationVnd());
		assertThat(result.makeupSession().status()).isEqualTo(CourseSessionStatus.MAKEUP_SCHEDULED);
		assertThat(db.queryForObject("select count(*) from course_sessions where course_run_id=? and is_makeup=false",
			Long.class, runId)).isOne();
		assertThat(db.queryForObject("select course_session_id from course_enrollment_sessions where course_run_id=?",
			UUID.class, runId)).isEqualTo(result.makeupSession().id());
		assertThat(db.queryForObject("select allocation_vnd from course_enrollment_sessions where course_run_id=?",
			java.math.BigDecimal.class, runId)).isEqualByComparingTo("100000");
		assertThat(db.queryForObject("select count(*) from notification_outbox where notification_type=? "
			+ "and target_id=? and recipient_account_id=?", Long.class, "COURSE_SESSION_MAKEUP_SCHEDULED",
			result.makeupSession().id(), memberId)).isOne();

		Account lateMember = account(AccountRole.MEMBER, "Late Makeup Member");
		Account receptionist = account(AccountRole.RECEPTIONIST, "Makeup Receptionist");
		UUID heldOrderId = seedHeldOrder(lateMember.getId());
		try {
			mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders/{orderId}/payments/cash",
				lateMember.getId(), heldOrderId).contextPath("/api/v1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
				.header("Idempotency-Key", "makeup-fulfillment-regression"))
				.andExpect(status().isOk());
		}
		catch (Exception exception) { throw new AssertionError(exception); }
		assertThat(db.queryForObject("select total_units_snapshot from course_enrollments where course_run_id=? "
			+ "and member_account_id=?", Integer.class, runId, lateMember.getId())).isOne();
		assertThat(db.queryForObject("select course_session_id from course_enrollment_sessions es "
			+ "join course_enrollments e on e.id=es.course_enrollment_id where e.course_run_id=? "
			+ "and e.member_account_id=?", UUID.class, runId, lateMember.getId())).isEqualTo(result.makeupSession().id());

		assertThatThrownBy(() -> disruptions.scheduleMakeup(managerId, runId, session.id(),
			new CourseMakeupRequest(cancelled.version(), roomId, coachId,
				Instant.parse("2035-01-12T02:00:00Z"), Instant.parse("2035-01-12T03:00:00Z"))))
			.isInstanceOfSatisfying(CourseException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("COURSE_SESSION_CHANGED"));
		assertThat(db.queryForObject("select count(*) from course_sessions where course_run_id=? and is_makeup=true",
			Long.class, runId)).isOne();
	}

	@Test
	void outboxFailureRollsBackCancellationAndAudit() {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		String constraint = "chk_test_reject_course_session_cancel_notice";
		db.execute("alter table notification_outbox add constraint " + constraint
			+ " check (notification_type <> 'COURSE_SESSION_CANCELLED') not valid");
		try {
			assertThatThrownBy(() -> disruptions.cancel(managerId, runId, session.id(), session.version()))
				.rootCause().hasMessageContaining(constraint);
			assertThat(db.queryForObject("select status from course_sessions where id=?", String.class, session.id()))
				.isEqualTo("SCHEDULED");
			assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_SESSION_CANCELLED' "
				+ "and target_id=?", Long.class, session.id())).isZero();
		}
		finally { db.execute("alter table notification_outbox drop constraint " + constraint); }
	}

	@Test
	void makeupNotificationFailureRollsBackReplacementAndEnrollmentMove() {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		CourseSessionResponse cancelled = disruptions.cancel(managerId, runId, session.id(), session.version());
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,10,'EXCLUSIVE','ACTIVE')",
			roomId, "Rollback makeup room " + roomId);
		String constraint = "chk_test_reject_makeup_notice";
		db.execute("alter table notification_outbox add constraint " + constraint
			+ " check (notification_type <> 'COURSE_SESSION_MAKEUP_SCHEDULED') not valid");
		try {
			assertThatThrownBy(() -> disruptions.scheduleMakeup(managerId, runId, session.id(),
				new CourseMakeupRequest(cancelled.version(), roomId, coachId,
					Instant.parse("2035-01-10T02:00:00Z"), Instant.parse("2035-01-10T03:00:00Z"))))
				.rootCause().hasMessageContaining(constraint);
			assertThat(db.queryForObject("select count(*) from course_sessions where course_run_id=? and is_makeup=true",
				Long.class, runId)).isZero();
			assertThat(db.queryForObject("select course_session_id from course_enrollment_sessions where course_run_id=?",
				UUID.class, runId)).isEqualTo(session.id());
			assertThat(db.queryForObject("select replacement_session_id from course_sessions where id=?",
				UUID.class, session.id())).isNull();
		}
		finally { db.execute("alter table notification_outbox drop constraint " + constraint); }
	}

	@Test
	void lastUnresolvedUnitCreatesOneConsolidatedObligationAndMovesRunToSettlement() {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		CourseSessionResponse cancelled = disruptions.cancel(managerId, runId, session.id(), session.version());
		Instant afterEnd = session.endAt().plusSeconds(1);

		assertThat(disruptions.advance(afterEnd)).isEmpty();
		assertThat(db.queryForObject("select status from course_sessions where id=?", String.class, session.id()))
			.isEqualTo("UNRESOLVED");
		assertThat(db.queryForObject("select disposition from course_enrollment_sessions where course_run_id=?",
			String.class, runId)).isEqualTo("UNRESOLVED");
		assertThat(db.queryForObject("select amount_vnd from course_refund_obligations where course_run_id=?",
			java.math.BigDecimal.class, runId)).isEqualByComparingTo("100000");
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId))
			.isEqualTo("SETTLEMENT_PENDING");

		disruptions.advance(afterEnd.plusSeconds(1));
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?",
			Long.class, runId)).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_RUN_SETTLEMENT_PENDING' "
			+ "and target_id=?", Long.class, runId)).isOne();
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
		Instant expiresAt = Instant.parse("2034-12-15T00:00:00Z");
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

	private UUID seedHeldOrder(UUID heldMemberId) {
		UUID orderId = UUID.randomUUID();
		UUID reservationId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		Instant expiresAt = Instant.parse("2034-12-15T00:00:00Z");
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,100000,'VND','PENDING_PAYMENT',?)",
			orderId, reference, heldMemberId, heldMemberId, runId, UUID.randomUUID().toString());
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) "
			+ "values(?,?,?,?,'HELD',?)", reservationId, orderId, heldMemberId, runId,
			java.sql.Timestamp.from(expiresAt));
		return orderId;
	}

	private Account account(AccountRole role, String name) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, name,
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}

	private String shortId() { return UUID.randomUUID().toString().replace("-", "").substring(0, 12); }

	private String token(Account account) {
		Instant issued = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder().subject(account.getId().toString()).issuedAt(issued)
			.expiresAt(issued.plusSeconds(900)).claim("role", account.getRole().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
			.getTokenValue();
	}

	private static void await(CountDownLatch latch) {
		try { if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("Concurrency rendezvous timed out"); }
		catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new AssertionError(exception); }
	}

	private void awaitDatabaseLock(int waiterPid, int blockerPid) {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		do {
			Long waiting = db.queryForObject("""
				select count(*) from pg_stat_activity
				where pid=? and wait_event_type='Lock' and ?=any(pg_blocking_pids(pid))
				""", Long.class, waiterPid, blockerPid);
			if (waiting != null && waiting == 1) return;
			Thread.onSpinWait();
		} while (System.nanoTime() < deadline);
		throw new AssertionError("Timed out waiting for Manager supersede to block on Member response row lock");
	}
}
