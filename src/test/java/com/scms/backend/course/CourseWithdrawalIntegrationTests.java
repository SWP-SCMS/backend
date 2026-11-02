package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CourseWithdrawalIntegrationTests {
	private static final Instant NOW = Instant.parse("2035-01-15T00:00:00Z");
	@Container @ServiceConnection static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService management;
	@Autowired CourseWithdrawalService withdrawals;
	@Autowired CourseOrderService orders;
	@Autowired CourseEnrollmentService enrollments;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate db;
	@Autowired com.scms.backend.centervisit.CenterVisitRepository visits;
	@Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
	@Autowired MockMvc mockMvc;
	@Autowired JwtEncoder jwtEncoder;
	@MockitoBean Clock clock;
	@MockitoBean CourseWithdrawalWaitlistPromotion promotions;

	private UUID managerId;
	private UUID secondManagerId;
	private UUID receptionistId;
	private UUID memberId;
	private UUID otherMemberId;
	private UUID coachId;
	private UUID runId;
	private UUID enrollmentId;

	@BeforeEach
	void setUp() {
		when(clock.instant()).thenReturn(NOW);
		managerId = account(AccountRole.MANAGER).getId();
		secondManagerId = account(AccountRole.MANAGER).getId();
		receptionistId = account(AccountRole.RECEPTIONIST).getId();
		memberId = account(AccountRole.MEMBER).getId();
		otherMemberId = account(AccountRole.MEMBER).getId();
		profiles.saveAndFlush(new MemberProfile(accounts.findById(memberId).orElseThrow()));
		profiles.saveAndFlush(new MemberProfile(accounts.findById(otherMemberId).orElseThrow()));
		coachId = account(AccountRole.COACH).getId();
		UUID disciplineId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", disciplineId,
			"Withdrawal " + disciplineId);
		CourseResponse course = management.createCourse(managerId, new CourseCreateRequest("WD-" + shortId(),
			"Withdrawal course", null, disciplineId, null, 1));
		course = management.publishCourse(managerId, course.id(), new VersionRequest(course.version()));
		CourseRunResponse run = management.createRun(managerId, new CourseRunCreateRequest("WD-RUN-" + shortId(),
			course.id(), coachId, BigInteger.valueOf(1_000_000), 1, 1, NOW.minusSeconds(86400),
			NOW.plusSeconds(86400), NOW.plusSeconds(86400 * 2), NOW.plusSeconds(86400 * 30), 10, true));
		runId = management.openRun(managerId, run.id(), new CourseRunOpenRequest(run.version(), "WITHDRAWAL-POLICY")).id();
		seedEnrollment();
	}

	@Test
	void receptionHistoryMatchesMemberDtoAndEnforcesBeneficiaryOwnership() throws Exception {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Front desk history");
		String auth = "Bearer " + token(receptionistId);
		String base = "/api/v1/reception/members/" + memberId;
		mockMvc.perform(get(base + "/course-withdrawal-requests").contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth))
			.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(request.id().toString()));
		String expected = mockMvc.perform(get("/api/v1/members/me/course-withdrawal-requests/{id}", request.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId)))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(mockMvc.perform(get(base + "/course-withdrawal-requests/{id}", request.id()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isEqualTo(expected);
		mockMvc.perform(get("/api/v1/reception/members/{member}/course-withdrawal-requests/{id}", otherMemberId, request.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNotFound());
		withdrawals.approve(managerId, request.id(), "History approved");
		UUID refund = db.queryForObject("select id from course_refund_obligations where course_enrollment_id=?", UUID.class, enrollmentId);
		mockMvc.perform(get(base + "/course-refund-obligations").contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth))
			.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(refund.toString()));
		String memberRefund = mockMvc.perform(get("/api/v1/members/me/course-refund-obligations/{id}", refund)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId)))
			.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(mockMvc.perform(get(base + "/course-refund-obligations/{id}", refund).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isEqualTo(memberRefund);
		mockMvc.perform(get("/api/v1/reception/members/{member}/course-refund-obligations/{id}", otherMemberId, refund)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNotFound());
	}

	@Test
	void pendingKeepsCapacityAndExactSeventyPercentApprovalCreatesFlooredRefund() {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Schedule no longer works");
		assertThat(request.status()).isEqualTo("PENDING");
		assertThat(request.refundAmountVnd()).isEqualTo(BigInteger.valueOf(560_000));
		assertThat(request.estimatedRefundAmount()).isEqualTo(BigInteger.valueOf(560_000));
		assertThatThrownBy(() -> withdrawals.createForMember(memberId, enrollmentId, "Duplicate"))
			.isInstanceOfSatisfying(CourseWithdrawalException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_WITHDRAWAL_ALREADY_PENDING"));
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class,
			enrollmentId)).isEqualTo("WITHDRAWAL_PENDING");
		assertThatThrownBy(() -> orders.createForMember(otherMemberId, new CourseOrderCreateRequest(runId),
			"pending-keeps-seat"))
			.isInstanceOfSatisfying(CourseOrderException.class,
				error -> assertThat(error.getCode()).isEqualTo("COURSE_CAPACITY_FULL"));

		var approved = withdrawals.approve(managerId, request.id(), "Approved under withdrawal policy");
		assertThat(approved.remainingUnits()).isEqualTo(7);
		assertThat(approved.totalUnits()).isEqualTo(10);
		assertThat(approved.refundBasisVnd()).isEqualTo(BigInteger.valueOf(700_000));
		assertThat(approved.refundAmountVnd()).isEqualTo(BigInteger.valueOf(560_000));
		assertThat(approved.withdrawalEligible()).isFalse();
		assertThat(approved.estimatedRefundAmount()).isNull();
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class,
			enrollmentId)).isEqualTo("WITHDRAWN");
		assertThat(db.queryForObject("select amount_vnd from course_refund_obligations where course_enrollment_id=? "
			+ "and source_type='MEMBER_WITHDRAWAL'", java.math.BigDecimal.class, enrollmentId))
			.isEqualByComparingTo("560000");
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_WITHDRAWAL_APPROVED' "
			+ "and target_id=?", Long.class, request.id())).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where "
			+ "notification_type='COURSE_WITHDRAWAL_APPROVED' and recipient_account_id=?", Long.class, memberId)).isOne();
		verify(promotions).promote(runId, NOW);
	}

	@Test
	void belowSeventyPercentStillWithdrawsWithZeroRefundObligation() {
		db.update("update course_sessions set start_at=?,end_at=? where course_run_id=? and unit_number=4 and is_makeup=false",
			Timestamp.from(NOW), Timestamp.from(NOW.plusSeconds(3600)), runId);
		var request = withdrawals.createForMember(memberId, enrollmentId, "Withdraw without refund");
		var approved = withdrawals.approve(managerId, request.id(), "Below refund threshold");

		assertThat(approved.remainingUnits()).isEqualTo(6);
		assertThat(approved.refundBasisVnd()).isEqualTo(BigInteger.valueOf(600_000));
		assertThat(approved.refundAmountVnd()).isZero();
		assertThat(db.queryForObject("select amount_vnd from course_refund_obligations where course_enrollment_id=? "
			+ "and source_type='MEMBER_WITHDRAWAL'", java.math.BigDecimal.class, enrollmentId)).isEqualByComparingTo("0");
	}

	@Test
	void rejectionRestoresActiveEnrollmentAndManagerCanSelfApproveOwnRequest() {
		var request = withdrawals.createForReceptionist(receptionistId, memberId, enrollmentId, "Submitted at front desk");
		var rejected = withdrawals.reject(managerId, request.id(), "Insufficient supporting information");
		assertThat(rejected.status()).isEqualTo("REJECTED");
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class,
			enrollmentId)).isEqualTo("ACTIVE");

		var managerRequest = withdrawals.createForManager(managerId, memberId, enrollmentId, "Manager-created request");
		assertThat(managerRequest.requesterAccountId()).isEqualTo(managerId);
		assertThat(withdrawals.approve(managerId, managerRequest.id(), "Self-approved with audit").status())
			.isEqualTo("APPROVED");
	}

	@Test
	void finalUnitBoundaryClosesWindowAndMemberDetailIsIdorSafe() throws Exception {
		db.update("update course_sessions set start_at=?,end_at=? where course_run_id=? and is_makeup=false",
			Timestamp.from(NOW), Timestamp.from(NOW.plusSeconds(3600)), runId);
		assertThatThrownBy(() -> withdrawals.createForMember(memberId, enrollmentId, "Too late"))
			.isInstanceOfSatisfying(CourseWithdrawalException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_WITHDRAWAL_WINDOW_CLOSED"));

		db.update("update course_sessions set start_at=start_at+interval '1 day',end_at=end_at+interval '1 day' "
			+ "where course_run_id=? and unit_number=10 and is_makeup=false", runId);
		var request = withdrawals.createForMember(memberId, enrollmentId, "Visible only to owner");
		mockMvc.perform(get("/api/v1/members/me/course-withdrawal-requests/{requestId}", request.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(otherMemberId)))
			.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/manager/course-withdrawal-requests").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/members/me/course-withdrawal-requests").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(managerId))).andExpect(status().isForbidden());
	}

	@Test
	void receptionistEnrollmentDiscoveryIsOwnedAndNotAvailableToOtherRoles() throws Exception {
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments", memberId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].memberId").value(memberId.toString()))
			.andExpect(jsonPath("$.content[0].remainingUnits").value(7))
			.andExpect(jsonPath("$.content[0].estimatedRefundAmount").value(560000));
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments", memberId)
			.param("status", "ACTIVE").param("withdrawalEligible", "true").param("page", "0").param("size", "1")
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments/{enrollmentId}",
			memberId, enrollmentId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId))).andExpect(status().isOk())
			.andExpect(jsonPath("$.estimatedRefundAmount").value(560000));
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments/{enrollmentId}",
			otherMemberId, enrollmentId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId))).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments", memberId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(managerId)))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments", memberId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId)))
			.andExpect(status().isForbidden());
		var request = withdrawals.createForReceptionist(receptionistId, memberId, enrollmentId, "Display test");
		assertThat(request.memberCode()).isNotBlank();
		assertThat(request.courseCode()).isNotBlank();
		assertThat(request.runCode()).isNotBlank();
		assertThat(request.totalUnits()).isEqualTo(10);
		assertThat(request.remainingUnits()).isEqualTo(7);
		assertThat(request.estimatedRefundAmount()).isEqualTo(BigInteger.valueOf(560_000));
		mockMvc.perform(get("/api/v1/reception/members/{memberId}/course-enrollments", memberId)
			.param("status", "WITHDRAWAL_PENDING").param("withdrawalEligible", "false")
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void concurrentManagerDecisionsCommitExactlyOneOutcome() throws Exception {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Concurrent decision");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var approve = executor.submit(() -> decide(start, () -> withdrawals.approve(managerId, request.id(), "Approve")));
			var reject = executor.submit(() -> decide(start, () -> withdrawals.reject(secondManagerId, request.id(), "Reject")));
			start.countDown();
			Object first = approve.get();
			Object second = reject.get();
			assertThat(java.util.stream.Stream.of(first, second).filter(CourseWithdrawalApiModels.WithdrawalRequest.class::isInstance).count())
				.isOne();
			assertThat(java.util.stream.Stream.of(first, second).filter(CourseWithdrawalException.class::isInstance).count())
				.isOne();
		}
		assertThat(withdrawals.managerDetail(managerId, request.id()).status()).isIn("APPROVED", "REJECTED");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_enrollment_id=? "
			+ "and source_type='MEMBER_WITHDRAWAL'", Long.class, enrollmentId)).isBetween(0L, 1L);
	}

	@Test
	@org.springframework.transaction.annotation.Transactional
	void courseEntryRequiresActiveEnrollmentAndUncancelledRun() {
		UUID sessionId = db.queryForObject("select id from course_sessions where course_run_id=? and unit_number=3", UUID.class, runId);
		assertThat(visits.findEligibleCourseEnrollmentId(memberId, sessionId, NOW)).contains(enrollmentId);
		for (String state : java.util.List.of("WITHDRAWAL_PENDING", "WITHDRAWN", "TRANSFERRED", "COMPLETED")) {
			db.update("update course_enrollments set lifecycle_status=? where id=?", state, enrollmentId);
			assertThat(visits.findEligibleCourseEnrollmentId(memberId, sessionId, NOW)).as(state).isEmpty();
		}
		db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=?", enrollmentId);
		db.update("update course_runs set status='CANCELLED' where id=?", runId);
		assertThat(visits.findEligibleCourseEnrollmentId(memberId, sessionId, NOW)).isEmpty();
	}

	@Test
	void courseEntryWaitsForConcurrentRunCancellationAndRechecksEligibility() throws Exception {
		UUID sessionId = db.queryForObject("select id from course_sessions where course_run_id=? and unit_number=3", UUID.class, runId);
		var transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
		CountDownLatch locked = new CountDownLatch(1), release = new CountDownLatch(1), checking = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var cancellation = executor.submit(() -> transactions.execute(status -> {
				db.update("update course_runs set status='CANCELLED' where id=?", runId);
				locked.countDown();
				try {
					if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Release timed out");
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(interrupted);
				}
				return null;
			}));
			try {
				assertThat(locked.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
				var eligibility = executor.submit(() -> transactions.execute(status -> {
					checking.countDown();
					return visits.findEligibleCourseEnrollmentId(memberId, sessionId, NOW);
				}));
				assertThat(checking.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
				assertThatThrownBy(() -> eligibility.get(200, java.util.concurrent.TimeUnit.MILLISECONDS))
					.isInstanceOf(java.util.concurrent.TimeoutException.class);
				release.countDown();
				cancellation.get(10, java.util.concurrent.TimeUnit.SECONDS);
				assertThat(eligibility.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEmpty();
			} finally { release.countDown(); }
		}
	}

	@Test
	void approvalUsesRequestTimeSnapshotEvenAfterAnotherUnitStarts() {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Snapshot at seven units");
		when(clock.instant()).thenReturn(NOW.plusSeconds(86400));
		var approved = withdrawals.approve(managerId, request.id(), "Review next day");
		assertThat(approved.remainingUnits()).isEqualTo(7);
		assertThat(approved.refundAmountVnd()).isEqualTo(BigInteger.valueOf(560_000));
	}

	@Test
	void validRequestCanBeApprovedAfterRunCompletes() {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Submitted before final unit");
		when(clock.instant()).thenReturn(NOW.plusSeconds(86400 * 8));
		db.update("update course_runs set status='COMPLETED' where id=?", runId);
		assertThat(withdrawals.approve(managerId, request.id(), "Late review").refundAmountVnd())
			.isEqualTo(BigInteger.valueOf(560_000));
	}

	@Test
	void pendingWithdrawalSuspendsRosterAndAttendance() {
		UUID sessionId = db.queryForObject("select id from course_sessions where course_run_id=? and unit_number=3", UUID.class, runId);
		UUID enrollmentSessionId = db.queryForObject("select id from course_enrollment_sessions where course_enrollment_id=? and course_session_id=?",
			UUID.class, enrollmentId, sessionId);
		assertThat(enrollments.roster(coachId, sessionId)).hasSize(1);
		withdrawals.createForMember(memberId, enrollmentId, "Suspend learning");
		assertThat(enrollments.roster(coachId, sessionId)).isEmpty();
		assertThatThrownBy(() -> enrollments.updateAttendance(coachId, sessionId, enrollmentSessionId,
			new CourseEnrollmentApiModels.AttendanceUpdate(CourseAttendanceStatus.PRESENT))).isInstanceOf(CourseAttendanceException.class);
	}

	@Test
	void memberCanCancelPendingRequestAndRetryWithoutDuplicatingEffects() throws Exception {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Cancel later");
		for (int retry = 0; retry < 2; retry++) mockMvc.perform(post("/api/v1/members/me/course-withdrawal-requests/{requestId}/cancel", request.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId))
			.contentType("application/json").content("{\"reason\":\"Continue studying\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, enrollmentId)).isEqualTo("ACTIVE");
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action='COURSE_WITHDRAWAL_CANCELLED'", Long.class, request.id())).isOne();
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_enrollment_id=?", Long.class, enrollmentId)).isZero();
	}

	@Test
	void cancellationRestoresRightsWithoutChangingHistoryOrGrantingMakeup() {
		UUID sessionId = db.queryForObject("select id from course_sessions where course_run_id=? and unit_number=3", UUID.class, runId);
		UUID enrollmentSessionId = db.queryForObject("select id from course_enrollment_sessions where course_enrollment_id=? and course_session_id=?",
			UUID.class, enrollmentId, sessionId);
		var request = withdrawals.createForMember(memberId, enrollmentId, "Suspend");
		assertThat(enrollments.memberEnrollments(memberId).getFirst().learningSuspended()).isTrue();
		when(clock.instant()).thenReturn(NOW.plusSeconds(5400));
		var cancelled = withdrawals.cancelForReceptionist(receptionistId, memberId, request.id(), "Resume");
		assertThat(cancelled.cancelledByAccountId()).isEqualTo(receptionistId);
		assertThat(cancelled.learningSuspended()).isFalse();
		assertThat(cancelled.refundAmountVnd()).isEqualTo(BigInteger.valueOf(560_000));
		assertThat(enrollments.memberEnrollments(memberId).getFirst().sessions()).hasSize(10);
		assertThat(enrollments.roster(coachId, sessionId)).hasSize(1);
		assertThatThrownBy(() -> enrollments.updateAttendance(coachId, sessionId, enrollmentSessionId,
			new CourseEnrollmentApiModels.AttendanceUpdate(CourseAttendanceStatus.PRESENT)))
			.isInstanceOfSatisfying(CourseAttendanceException.class, error -> assertThat(error.getCode()).isEqualTo("COURSE_ATTENDANCE_WINDOW_CLOSED"));
		assertThat(db.queryForObject("select count(*) from course_sessions where course_run_id=? and is_makeup=true", Long.class, runId)).isZero();
		assertThat(db.queryForObject("select attendance_status from course_enrollment_sessions where id=?", String.class, enrollmentSessionId)).isEqualTo("NOT_RECORDED");
		assertThatThrownBy(() -> orders.createForMember(otherMemberId, new CourseOrderCreateRequest(runId), "cancel-keeps-seat"))
			.isInstanceOfSatisfying(CourseOrderException.class, error -> assertThat(error.getCode()).isEqualTo("COURSE_CAPACITY_FULL"));
	}

	@Test
	void cancelOwnershipRolesInactiveAccountsAndTerminalConflictsAreEnforced() throws Exception {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Owned request");
		assertThatThrownBy(() -> withdrawals.cancelForMember(otherMemberId, request.id(), "Wrong owner"))
			.isInstanceOfSatisfying(CourseWithdrawalException.class, error -> assertThat(error.code()).isEqualTo("COURSE_WITHDRAWAL_NOT_FOUND"));
		mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel", otherMemberId, request.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId))
			.contentType("application/json").content("{\"reason\":\"Wrong owner\"}"))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/members/me/course-withdrawal-requests/{requestId}/cancel", request.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(coachId))
			.contentType("application/json").content("{\"reason\":\"Wrong role\"}"))
			.andExpect(status().isForbidden());
		db.update("update accounts set status='INACTIVE' where id=?", memberId);
		assertThatThrownBy(() -> withdrawals.cancelForMember(memberId, request.id(), "Inactive"))
			.isInstanceOf(com.scms.backend.auth.InvalidAuthenticatedAccountException.class);
		db.update("update accounts set status='ACTIVE' where id=?", memberId);
		withdrawals.reject(managerId, request.id(), "Rejected");
		assertThatThrownBy(() -> withdrawals.cancelForMember(memberId, request.id(), "Already rejected"))
			.isInstanceOfSatisfying(CourseWithdrawalException.class, error -> assertThat(error.code()).isEqualTo("COURSE_WITHDRAWAL_INVALID_TRANSITION"));
		var next = withdrawals.createForMember(memberId, enrollmentId, "New request");
		withdrawals.approve(managerId, next.id(), "Approved");
		assertThatThrownBy(() -> withdrawals.cancelForMember(memberId, next.id(), "Already approved"))
			.isInstanceOfSatisfying(CourseWithdrawalException.class, error -> assertThat(error.code()).isEqualTo("COURSE_WITHDRAWAL_INVALID_TRANSITION"));
	}

	@Test
	void cancellationApprovalAndRejectionCompeteInRealTransactions() throws Exception {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Three-way race");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(3)) {
			var approve = executor.submit(() -> decide(start, () -> withdrawals.approve(managerId, request.id(), "Approve")));
			var reject = executor.submit(() -> decide(start, () -> withdrawals.reject(secondManagerId, request.id(), "Reject")));
			var cancel = executor.submit(() -> decide(start, () -> withdrawals.cancelForMember(memberId, request.id(), "Cancel")));
			start.countDown();
			var outcomes = java.util.List.of(approve.get(15, java.util.concurrent.TimeUnit.SECONDS),
				reject.get(15, java.util.concurrent.TimeUnit.SECONDS), cancel.get(15, java.util.concurrent.TimeUnit.SECONDS));
			assertThat(outcomes.stream().filter(CourseWithdrawalApiModels.WithdrawalRequest.class::isInstance).count()).isOne();
			assertThat(outcomes.stream().filter(CourseWithdrawalException.class::isInstance).count()).isEqualTo(2);
		}
		var terminal = withdrawals.detailForMember(memberId, request.id());
		assertThat(db.queryForObject("select count(*) from audit_events where target_id=? and action in "
			+ "('COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_APPROVED','COURSE_WITHDRAWAL_REJECTED')", Long.class, request.id())).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where target_id=? and notification_type in "
			+ "('COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_APPROVED','COURSE_WITHDRAWAL_REJECTED')", Long.class, request.id())).isOne();
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_enrollment_id=?", Long.class, enrollmentId))
			.isEqualTo("APPROVED".equals(terminal.status()) ? 1L : 0L);
	}

	@Test
	void runCancellationClosesPendingRequestAndOnlyCreatesRunCancellationRefund() {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Pending when run cancelled");
		Long version = db.queryForObject("select version from course_runs where id=?", Long.class, runId);
		management.cancelRun(managerId, runId, new VersionRequest(version));
		var closed = withdrawals.detailForMember(memberId, request.id());
		assertThat(closed.status()).isEqualTo("CANCELLED");
		assertThat(closed.cancellationReason()).isEqualTo("COURSE_RUN_CANCELLED");
		assertThat(withdrawals.cancelForMember(memberId, request.id(), "Retry").cancellationReason()).isEqualTo("COURSE_RUN_CANCELLED");
		assertThatThrownBy(() -> withdrawals.approve(managerId, request.id(), "Too late"))
			.isInstanceOf(CourseWithdrawalException.class);
		assertThat(db.queryForList("select source_type from course_refund_obligations where course_enrollment_id=?", String.class, enrollmentId))
			.containsExactly("COURSE_RUN_CANCELLED");
		assertThat(db.queryForObject("select amount_vnd from course_refund_obligations where course_enrollment_id=?", java.math.BigDecimal.class, enrollmentId))
			.isEqualByComparingTo("1000000");
	}

	@Test
	void snapshotsAreImmutableAndRollbackIncludesAuditOutboxAndEnrollment() {
		var transactions = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
		UUID rolledBack = transactions.execute(status -> {
			var request = withdrawals.createForMember(memberId, enrollmentId, "Rollback");
			status.setRollbackOnly();
			return request.id();
		});
		for (String table : java.util.List.of("course_withdrawal_requests", "audit_events", "notification_outbox")) {
			String column = table.equals("course_withdrawal_requests") ? "id" : "target_id";
			assertThat(db.queryForObject("select count(*) from " + table + " where " + column + "=?", Long.class, rolledBack)).isZero();
		}
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, enrollmentId)).isEqualTo("ACTIVE");
		var request = withdrawals.createForMember(memberId, enrollmentId, "Immutable");
		assertThat(request.snapshotCapturedAt()).isEqualTo(NOW);
		assertThat(request.snapshotSource()).isEqualTo("REQUEST_TIME");
		assertThatThrownBy(() -> db.update("update course_withdrawal_requests set refund_amount_vnd=0 where id=?", request.id()))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThat(withdrawals.detailForMember(memberId, request.id()).refundAmountVnd()).isEqualTo(BigInteger.valueOf(560_000));
	}

	private Object decide(CountDownLatch start, java.util.concurrent.Callable<Object> decision) {
		try { start.await(); return decision.call(); }
		catch (Exception exception) { return exception; }
	}

	private void seedEnrollment() {
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values(?,?,10,'EXCLUSIVE','ACTIVE')",
			roomId, "Withdrawal room " + roomId);
		UUID[] sessions = new UUID[10];
		for (int unit = 1; unit <= 10; unit++) {
			Instant start = unit <= 2 ? NOW.minusSeconds((3L - unit) * 86400) : unit == 3 ? NOW
				: NOW.plusSeconds((long) (unit - 3) * 86400);
			sessions[unit - 1] = UUID.randomUUID();
			db.update("insert into course_sessions(id,course_run_id,unit_number,room_id,coach_account_id,start_at,end_at,"
				+ "original_start_at,original_end_at,allocation_vnd,status) values(?,?,?,?,?,?,?,?,?,100000,'SCHEDULED')",
				sessions[unit - 1], runId, unit, roomId, coachId, Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)),
				Timestamp.from(start), Timestamp.from(start.plusSeconds(3600)));
		}
		UUID orderId = UUID.randomUUID(), reservationId = UUID.randomUUID(), paymentId = UUID.randomUUID();
		enrollmentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,1000000,'VND','PAID',?)",
			orderId, reference, memberId, memberId, runId, "withdrawal-seed-" + orderId);
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) "
			+ "values(?,?,?,?,'ENROLLED',?)", reservationId, orderId, memberId, runId, Timestamp.from(NOW.plusSeconds(86400)));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,"
			+ "payment_reference,amount,currency_code,status,deadline_at,method,paid_at) "
			+ "values(?,?,?,?,?,?,1000000,'VND','PAID',?,'CASH',?)", paymentId, orderId, reservationId,
			memberId, memberId, reference, Timestamp.from(NOW.plusSeconds(86400)), Timestamp.from(NOW));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,"
			+ "member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,"
			+ "cash_paid_vnd) values(?,?,?,?,?,?,?,1000000,10,?,1000000)", enrollmentId, orderId, reservationId,
			paymentId, memberId, memberId, runId, Timestamp.from(NOW));
		for (int unit = 1; unit <= 10; unit++) db.update("insert into course_enrollment_sessions(id,course_enrollment_id,"
			+ "course_session_id,course_run_id,unit_number,allocation_vnd,disposition) values(?,?,?,?,?,100000,'ENROLLED')",
			UUID.randomUUID(), enrollmentId, sessions[unit - 1], runId, unit);
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " withdrawal test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
	private String token(UUID accountId) {
		Account account = accounts.findById(accountId).orElseThrow();
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("scms-backend").issuedAt(NOW.minusSeconds(60))
			.expiresAt(NOW.plusSeconds(3600)).subject(accountId.toString()).claim("role", account.getRole().name())
			.claim("account_status", account.getStatus().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
	private static String shortId() { return UUID.randomUUID().toString().substring(0, 8); }
}
