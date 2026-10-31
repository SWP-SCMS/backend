package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate db;
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
	void pendingKeepsCapacityAndExactSeventyPercentApprovalCreatesFlooredRefund() {
		var request = withdrawals.createForMember(memberId, enrollmentId, "Schedule no longer works");
		assertThat(request.status()).isEqualTo("PENDING");
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
