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
	@Autowired CourseWithdrawalService withdrawals;
	@Autowired CourseRefundObligationService refunds;
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
		db.update("insert into member_profiles(account_id) values(?)", memberId);
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
		seedEnrollment(memberId);
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
	void memberTimelineIncludesPaymentRunAndMakeupMetadata() {
		var enrollment = members.memberEnrollments(memberId).getFirst();
		assertThat(enrollment.courseRunStatus()).isEqualTo(CourseRunStatus.OPEN_FOR_REGISTRATION);
		assertThat(enrollment.payment().amountVnd()).isEqualTo(BigInteger.valueOf(100_000));
		assertThat(enrollment.payment().status()).isEqualTo("PAID");
		assertThat(enrollment.payment().method()).isEqualTo("CASH");
		assertThat(enrollment.sessions()).singleElement().satisfies(original -> {
			assertThat(original.courseSessionId()).isEqualTo(session.id());
			assertThat(original.isMakeup()).isFalse();
			assertThat(original.originalSessionId()).isNull();
			assertThat(original.replacementSessionId()).isNull();
		});

		CourseSessionResponse cancelled = disruptions.cancel(managerId, runId, session.id(), session.version());
		CourseSessionDisruptionService.CourseMakeupResponse makeup = disruptions.scheduleMakeup(managerId, runId, session.id(),
			new CourseMakeupRequest(cancelled.version(), session.roomId(), coachId,
				Instant.parse("2035-01-10T02:00:00Z"), Instant.parse("2035-01-10T03:00:00Z")));

		assertThat(makeup.originalSession().isMakeup()).isFalse();
		assertThat(makeup.originalSession().replacementSessionId()).isEqualTo(makeup.makeupSession().id());
		assertThat(members.memberEnrollments(memberId).getFirst().sessions()).singleElement().satisfies(replacement -> {
			assertThat(replacement.courseSessionId()).isEqualTo(makeup.makeupSession().id());
			assertThat(replacement.isMakeup()).isTrue();
			assertThat(replacement.originalSessionId()).isEqualTo(makeup.originalSession().id());
			assertThat(replacement.replacementSessionId()).isNull();
			assertThat(replacement.unitNumber()).isEqualTo(session.unitNumber());
		});
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
		assertThat(result.originalSession().isMakeup()).isFalse();
		assertThat(result.makeupSession().isMakeup()).isTrue();
		assertThat(result.makeupSession().originalSessionId()).isEqualTo(result.originalSession().id());
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
	void lastUnresolvedUnitCreatesOneConsolidatedObligationAndCompletesRun() {
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
			.isEqualTo("COMPLETED");

		disruptions.advance(afterEnd.plusSeconds(1));
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?",
			Long.class, runId)).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_RUN_SETTLEMENT_PENDING' "
			+ "and target_id=?", Long.class, runId)).isOne();
	}

	@Test
	void reducingStartedRunRemovesOnlyFutureTailAndStillCompletes() {
		db.update("update course_runs set total_units=3,version=version+1 where id=?", runId);
		CourseSessionResponse retained = createSession(2, Instant.parse("2035-01-07T02:00:00Z"));
		CourseSessionResponse removed = createSession(3, Instant.parse("2035-01-08T02:00:00Z"));
		UUID enrollmentId = db.queryForObject("select id from course_enrollments where course_run_id=? and member_account_id=?",
			UUID.class, runId, memberId);
		db.update("update course_enrollments set total_units_snapshot=3 where id=?", enrollmentId);
		db.update("update course_enrollment_sessions set allocation_vnd=30000 where course_enrollment_id=?", enrollmentId);
		seedEnrollmentSession(enrollmentId, retained, 2, BigInteger.valueOf(30_000));
		seedEnrollmentSession(enrollmentId, removed, 3, BigInteger.valueOf(40_000));
		db.update("update course_runs set status='IN_PROGRESS',version=version+1 where id=?", runId);
		Instant changedAt = Instant.parse("2035-01-06T00:00:00Z");
		when(clock.instant()).thenReturn(changedAt);

		managers.patchRunSchedule(managerId, runId, new CourseRunScheduleRequest(runVersion(),
			Instant.parse("2035-01-01T00:00:00Z"), Instant.parse("2035-02-01T00:00:00Z"), 2));

		assertThat(sessionStatus(session.id())).isEqualTo("SCHEDULED");
		assertThat(sessionStatus(retained.id())).isEqualTo("SCHEDULED");
		assertThat(sessionStatus(removed.id())).isEqualTo("REMOVED_BY_RUN_CHANGE");
		disruptions.advance(retained.endAt().plusSeconds(1));
		assertThat(sessionStatus(session.id())).isEqualTo("COMPLETED");
		assertThat(sessionStatus(retained.id())).isEqualTo("COMPLETED");
		assertThat(sessionStatus(removed.id())).isEqualTo("REMOVED_BY_RUN_CHANGE");
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId))
			.isEqualTo("COMPLETED");
	}

	@Test
	void runChangeUsesFutureSessionDeadlineAndWithdrawalResponseIsIdempotent() {
		Instant changedAt = Instant.parse("2035-01-03T00:00:00Z");
		when(clock.instant()).thenReturn(changedAt);
		managers.patchRunSchedule(managerId, runId, new CourseRunScheduleRequest(runVersion(),
			Instant.parse("2035-01-02T00:00:00Z"), Instant.parse("2035-02-01T00:00:00Z"), 1));

		var pending = members.scheduleChanges(memberId).stream()
			.filter(change -> change.notificationSentAt().equals(changedAt)).findFirst().orElseThrow();
		assertThat(pending.responseDeadline()).isEqualTo(session.startAt().minusSeconds(24 * 3600));
		assertThat(pending.responseDeadline()).isAfter(changedAt);
		assertThat(pending.status()).isEqualTo("PENDING");
		assertThat(pending.withdrawalEligible()).isTrue();
		assertThat(pending.withdrawalReason()).isEqualTo("ELIGIBLE_WITH_REFUND");
		assertThat(pending.refundBasisVnd()).isEqualTo(BigInteger.valueOf(100_000));
		assertThat(pending.refundAmountVnd()).isEqualTo(BigInteger.valueOf(80_000));

		var created = members.respondToScheduleChange(memberId, pending.id(),
			new CourseScheduleChangeApiModels.ScheduleChangeResponse("REQUEST_WITHDRAWAL"));
		var retried = members.respondToScheduleChange(memberId, pending.id(),
			new CourseScheduleChangeApiModels.ScheduleChangeResponse("REQUEST_WITHDRAWAL"));
		assertThat(created.withdrawalRequestId()).isNotNull().isEqualTo(retried.withdrawalRequestId());
		var saved = withdrawals.detailForMember(memberId, created.withdrawalRequestId());
		assertThat(saved.snapshotCapturedAt()).isEqualTo(changedAt);
		assertThat(saved.refundAmountVnd()).isEqualTo(BigInteger.valueOf(80_000));
		withdrawals.cancelForMember(memberId, saved.id(), "Continue studying");
		when(clock.instant()).thenReturn(pending.responseDeadline().plusSeconds(1));
		var closedRetry = members.respondToScheduleChange(memberId, pending.id(),
			new CourseScheduleChangeApiModels.ScheduleChangeResponse("REQUEST_WITHDRAWAL"));
		assertThat(closedRetry.withdrawalRequestId()).isEqualTo(saved.id());
		assertThat(closedRetry.withdrawalEligible()).isFalse();
		assertThat(withdrawals.detailForMember(memberId, saved.id()).status()).isEqualTo("CANCELLED");
		assertThat(db.queryForObject("select count(*) from course_withdrawal_requests where schedule_change_response_id=?",
			Long.class, pending.id())).isOne();
	}

	@Test
	void scheduleConflictCauseIsPerMemberAndSessionMoveIsNotOpeningDelay() {
		Account unaffected = account(AccountRole.MEMBER, "Unaffected Schedule Member");
		db.update("insert into member_profiles(account_id) values(?)", unaffected.getId());
		seedEnrollment(unaffected.getId());
		Instant movedStart = session.startAt().plusSeconds(10 * 86400L);
		seedGymBooking(memberId, movedStart.plusSeconds(1800), movedStart.plusSeconds(5400));

		managers.patchSession(managerId, runId, session.id(), new CourseSessionPatchRequest(session.version(), null, null,
			movedStart, movedStart.plusSeconds(3600)));

		var affected = members.scheduleChanges(memberId).stream()
			.filter(change -> change.newStartAt().equals(movedStart)).findFirst().orElseThrow();
		var unaffectedResponse = members.scheduleChanges(unaffected.getId()).stream()
			.filter(change -> change.newStartAt().equals(movedStart)).findFirst().orElseThrow();
		assertThat(affected.cause()).isEqualTo(CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT);
		assertThat(unaffectedResponse.cause()).isEqualTo(CourseWithdrawalCause.VOLUNTARY);
		assertThat(unaffectedResponse.cause()).isNotEqualTo(CourseWithdrawalCause.START_DELAY_OVER_7_DAYS);
	}

	@Test
	void legacySettlementPendingCompletesWithoutWaitingForPayout() {
		db.update("update course_runs set status='SETTLEMENT_PENDING' where id=?", runId);
		db.update("update course_sessions set status='UNRESOLVED' where id=?", session.id());
		db.update("update course_enrollment_sessions set disposition='UNRESOLVED' where course_run_id=?", runId);
		db.update("insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd) "
			+ "select gen_random_uuid(),id,course_run_id,'UNRESOLVED_SESSIONS',100000 from course_enrollments where course_run_id=?", runId);
		disruptions.advance(session.endAt().plusSeconds(1));
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select status from course_refund_obligations where course_run_id=?", String.class, runId)).isEqualTo("PENDING_REVIEW");
		disruptions.advance(session.endAt().plusSeconds(2));
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?", Long.class, runId)).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_RUN_COMPLETED'", Long.class, runId)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where target_id=? and notification_type='COURSE_RUN_COMPLETED'", Long.class, runId)).isOne();
	}

	@Test
	void pendingSnapshotDoesNotBlockCompletionAndRemainsApprovable() {
		UUID enrollment = db.queryForObject("select id from course_enrollments where course_run_id=? and member_account_id=?", UUID.class, runId, memberId);
		var pending = withdrawals.createForMember(memberId, enrollment, "Review after completion");
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		Instant afterEnd = session.endAt().plusSeconds(1);
		when(clock.instant()).thenReturn(afterEnd);
		disruptions.advance(afterEnd);
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, enrollment)).isEqualTo("WITHDRAWAL_PENDING");
		assertThat(withdrawals.approve(managerId, pending.id(), "Snapshot remains valid").refundAmountVnd()).isEqualTo(pending.refundAmountVnd());
	}

	@Test
	void completionWaitsForLastEndAndPreservesAttendanceWindowAndEnrollment() {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		disruptions.advance(session.endAt().minusSeconds(1));
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("IN_PROGRESS");
		disruptions.advance(session.endAt());
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?", Long.class, runId)).isZero();
		assertThat(members.memberEnrollments(memberId).getFirst().lifecycleStatus()).isEqualTo("ACTIVE");
		when(clock.instant()).thenReturn(session.endAt().plusSeconds(1));
		UUID enrollmentSession = db.queryForObject("select id from course_enrollment_sessions where course_run_id=?", UUID.class, runId);
		assertThat(members.updateAttendance(coachId, session.id(), enrollmentSession,
			new CourseEnrollmentApiModels.AttendanceUpdate(CourseAttendanceStatus.PRESENT)).attendanceStatus()).isEqualTo(CourseAttendanceStatus.PRESENT);
	}

	@Test
	void missingUnitsAndLegacyFutureSessionsDoNotComplete() {
		db.update("update course_runs set status='CONFIRMED',total_units=2 where id=?", runId);
		disruptions.advance(session.endAt().plusSeconds(1));
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("IN_PROGRESS");
		db.update("update course_runs set status='SETTLEMENT_PENDING',total_units=1 where id=?", runId);
		disruptions.advance(session.endAt().minusSeconds(1));
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("SETTLEMENT_PENDING");
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(ints = {3, 10})
	void completionUsesEffectiveMakeupEndEvenWhenEarlierThanCancelledOriginal(int day) {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		var cancelled = disruptions.cancel(managerId, runId, session.id(), session.version());
		Instant start = Instant.parse("2035-01-01T02:00:00Z").plusSeconds((day - 1L) * 86400);
		var makeup = disruptions.scheduleMakeup(managerId, runId, session.id(),
			new CourseMakeupRequest(cancelled.version(), session.roomId(), coachId, start, start.plusSeconds(3600)));
		if (day == 10) {
			disruptions.advance(session.endAt().plusSeconds(1));
			assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("IN_PROGRESS");
		}
		disruptions.advance(makeup.makeupSession().endAt());
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("COMPLETED");
		assertThat(sessionStatus(makeup.makeupSession().id())).isEqualTo("COMPLETED");
		assertThat(sessionStatus(session.id())).isEqualTo("CANCELLED_PENDING_MAKEUP");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?", Long.class, runId)).isZero();
	}

	@Test
	void concurrentSchedulerRunsCommitOneCompletionAndOneRefund() throws Exception {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		disruptions.cancel(managerId, runId, session.id(), session.version());
		CountDownLatch start = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			java.util.concurrent.Callable<Object> advance = () -> { start.await(); return disruptions.advance(session.endAt().plusSeconds(1)); };
			var first = pool.submit(advance); var second = pool.submit(advance);
			start.countDown(); first.get(20, TimeUnit.SECONDS); second.get(20, TimeUnit.SECONDS);
		}
		disruptions.advance(session.endAt().plusSeconds(2));
		assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?", Long.class, runId)).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_RUN_COMPLETED'", Long.class, runId)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where target_id=? and notification_type='COURSE_RUN_COMPLETED'", Long.class, runId)).isOne();
	}

	@Test
	void completionNotificationFailureRollsBackSessionsRefundsAndRun() {
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		disruptions.cancel(managerId, runId, session.id(), session.version());
		String constraint = "chk_test_reject_run_completion_notice";
		db.execute("alter table notification_outbox add constraint " + constraint
			+ " check (notification_type<>'COURSE_RUN_COMPLETED' or target_id<>'" + runId + "'::uuid) not valid");
		try {
			assertThatThrownBy(() -> disruptions.advance(session.endAt().plusSeconds(1))).rootCause().hasMessageContaining(constraint);
			assertThat(db.queryForObject("select status from course_runs where id=?", String.class, runId)).isEqualTo("CONFIRMED");
			assertThat(sessionStatus(session.id())).isEqualTo("CANCELLED_PENDING_MAKEUP");
			assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_run_id=?", Long.class, runId)).isZero();
			assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_RUN_COMPLETED'", Long.class, runId)).isZero();
		} finally { db.execute("alter table notification_outbox drop constraint " + constraint); }
	}

	@Test
	void automaticallyCompletedRunStillRequiresManagerApprovalBeforeReceptionDisbursement() {
		UUID receptionist = account(AccountRole.RECEPTIONIST, "Refund Reception").getId();
		db.update("update course_runs set status='CONFIRMED' where id=?", runId);
		disruptions.cancel(managerId, runId, session.id(), session.version());
		Instant afterEnd = session.endAt().plusSeconds(1);
		when(clock.instant()).thenReturn(afterEnd);
		disruptions.advance(afterEnd);
		UUID obligation = db.queryForObject("select id from course_refund_obligations where course_run_id=?", UUID.class, runId);
		var input = new CourseRefundApiModels.RefundDisbursement(0L, BigInteger.valueOf(100_000), "CASH", "receipt", "evidence");
		assertThatThrownBy(() -> refunds.recordDisbursement(receptionist, obligation, "before-approval", input)).isInstanceOf(CourseRefundException.class);
		var approved = refunds.approve(managerId, obligation, 0L, "Unserved unit");
		assertThat(refunds.recordDisbursement(receptionist, obligation, "after-approval", new CourseRefundApiModels.RefundDisbursement(
			approved.version(), approved.approvedAmountVnd(), "CASH", "receipt", "evidence")).status()).isEqualTo("DISBURSEMENT_RECORDED");
	}

	@Test
	void completedRunRejectsNewSessions() {
		db.update("update course_runs set status='COMPLETED',total_units=2 where id=?", runId);
		assertThatThrownBy(() -> createSession(2, Instant.parse("2035-01-10T02:00:00Z"))).isInstanceOf(CourseException.class);
		assertThat(db.queryForObject("select count(*) from course_sessions where course_run_id=?", Long.class, runId)).isOne();
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

	private UUID seedEnrollment(UUID enrolledMemberId) {
		UUID enrollmentId = UUID.randomUUID(), enrollmentSessionId = UUID.randomUUID();
		UUID orderId = UUID.randomUUID(), reservationId = UUID.randomUUID(), paymentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		Instant expiresAt = Instant.parse("2034-12-15T00:00:00Z");
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,100000,'VND','PAID',?)",
			orderId, reference, enrolledMemberId, enrolledMemberId, runId, UUID.randomUUID().toString());
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) "
			+ "values(?,?,?,?,'ENROLLED',?)", reservationId, orderId, enrolledMemberId, runId,
			java.sql.Timestamp.from(expiresAt));
		db.update("""
			insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,
			payment_reference,amount,currency_code,status,deadline_at,bank_code_snapshot,bank_account_number_snapshot,
			bank_account_name_snapshot,method,paid_at)
			values(?,?,?,?,?,?,100000,'VND','PAID',?,'CASH','CASH','CASH','CASH',current_timestamp)
			""", paymentId, orderId, reservationId, enrolledMemberId, enrolledMemberId, reference,
			java.sql.Timestamp.from(expiresAt));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,"
			+ "member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at) "
			+ "values(?,?,?,?,?,?,?,100000,1,current_timestamp)", enrollmentId, orderId, reservationId, paymentId,
			enrolledMemberId, enrolledMemberId, runId);
		db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,"
			+ "unit_number,allocation_vnd,disposition) values(?,?,?,?,1,100000,'ENROLLED')", enrollmentSessionId,
			enrollmentId, session.id(), runId);
		return enrollmentId;
	}

	private void seedEnrollmentSession(UUID enrollmentId, CourseSessionResponse enrolledSession, int unitNumber,
			BigInteger allocation) {
		db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,"
			+ "unit_number,allocation_vnd,disposition) values(?,?,?,?,?,?,'ENROLLED')", UUID.randomUUID(), enrollmentId,
			enrolledSession.id(), runId, unitNumber, allocation);
	}

	private CourseSessionResponse createSession(int unitNumber, Instant startAt) {
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values (?,?,10,'EXCLUSIVE','ACTIVE')",
			roomId, "Schedule room " + roomId);
		return managers.createSession(managerId, runId, new CourseSessionCreateRequest(unitNumber, roomId, coachId,
			startAt, startAt.plusSeconds(3600), BigInteger.valueOf(30_000)));
	}

	private void seedGymBooking(UUID enrolledMemberId, Instant startAt, Instant endAt) {
		UUID offerId = UUID.randomUUID(), orderId = UUID.randomUUID(), membershipId = UUID.randomUUID();
		UUID sportClassId = UUID.randomUUID(), roomId = UUID.randomUUID(), gymSessionId = UUID.randomUUID();
		db.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,"
			+ "status,created_by_account_id) values(?,'PLUS',?,?,100000,'VND',60,'ACTIVE',?)", offerId,
			"Schedule conflict " + offerId, "Schedule conflict", managerId);
		db.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,"
			+ "offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,"
			+ "payment_method,status,expires_at,paid_at) values(?,?,?,?,?,?,'PLUS',100000,'VND',60,'CASH','PAID',?,?)",
			orderId, "GYM-" + shortId(), enrolledMemberId, managerId, offerId, "Schedule conflict",
			java.sql.Timestamp.from(startAt), java.sql.Timestamp.from(startAt.minusSeconds(86400)));
		db.update("""
			insert into payments(id,order_id,method,status,amount,currency_code,provider_reference,paid_at,
			    processed_by_account_id)
			values(?,?,'CASH','PAID',100000,'VND',?,?,?)
			""", UUID.randomUUID(), orderId, "CASH-" + orderId,
			java.sql.Timestamp.from(startAt.minusSeconds(86400)), managerId);
		db.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,"
			+ "price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) "
			+ "values(?,?,?,?,'PLUS','Schedule conflict',100000,'VND',60,'ACTIVE',?,?)", membershipId,
			enrolledMemberId, orderId, offerId, java.sql.Timestamp.from(startAt.minusSeconds(86400)),
			java.sql.Timestamp.from(endAt.plusSeconds(86400)));
		db.update("insert into sport_classes(id,discipline_id,name,class_type,status) values(?,?,?,'GROUP','ACTIVE')",
			sportClassId, disciplineId, "Schedule conflict " + sportClassId);
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values(?,?,10,'EXCLUSIVE','ACTIVE')", roomId,
			"Schedule conflict room " + roomId);
		db.update("insert into class_sessions(id,sport_class_id,teaching_coach_account_id,room_id,start_time,end_time,"
			+ "capacity,status,created_by_account_id) values(?,?,?,?,?,?,10,'SCHEDULED',?)", gymSessionId,
			sportClassId, account(AccountRole.COACH, "Gym Conflict Coach").getId(), roomId,
			java.sql.Timestamp.from(startAt), java.sql.Timestamp.from(endAt), managerId);
		db.update("insert into bookings(id,class_session_id,member_account_id,membership_id,status,booked_by_account_id) "
			+ "values(?,?,?,?,'BOOKED',?)", UUID.randomUUID(), gymSessionId, enrolledMemberId, membershipId,
			enrolledMemberId);
	}

	private Long runVersion() {
		return db.queryForObject("select version from course_runs where id=?", Long.class, runId);
	}

	private String sessionStatus(UUID sessionId) {
		return db.queryForObject("select status from course_sessions where id=?", String.class, sessionId);
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
