package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class CourseRefundObligationIntegrationTests {
	private static final Instant NOW = Instant.parse("2035-02-01T00:00:00Z");
	@Container @ServiceConnection static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseRefundObligationService refunds;
	@Autowired CourseRefundObligationWriter writer;
	@Autowired AccountRepository accounts;
	@Autowired JdbcTemplate db;
	@Autowired MockMvc mockMvc;
	@Autowired JwtEncoder jwtEncoder;
	@MockitoBean Clock clock;

	private UUID managerId;
	private UUID secondManagerId;
	private UUID receptionistId;
	private UUID memberId;
	private UUID coachId;
	private UUID runId;
	private UUID enrollmentId;

	@BeforeEach
	void setUp() {
		when(clock.instant()).thenReturn(NOW);
		managerId = account(AccountRole.MANAGER);
		secondManagerId = account(AccountRole.MANAGER);
		receptionistId = account(AccountRole.RECEPTIONIST);
		memberId = account(AccountRole.MEMBER);
		coachId = account(AccountRole.COACH);
		seedEnrollment(BigInteger.valueOf(1_000_000));
	}

	@Test
	void managerApprovesZeroAmountAndSameDecisionReplayIsIdempotent() {
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.MATERIAL_CHANGE,
			BigInteger.ZERO, managerId, "Material schedule change");

		var approved = refunds.approve(managerId, obligationId, 0L, "Approved without payable amount");
		var replay = refunds.approve(managerId, obligationId, 0L, "Approved without payable amount");

		assertThat(approved.status()).isEqualTo("APPROVED");
		assertThat(approved.approvedAmountVnd()).isZero();
		assertThat(replay.version()).isEqualTo(approved.version());
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_REFUND_APPROVED' "
			+ "and target_id=?", Long.class, obligationId)).isOne();
		assertThat(db.queryForObject("select count(*) from notification_outbox where notification_type="
			+ "'COURSE_REFUND_APPROVED' and recipient_account_id=?", Long.class, memberId)).isOne();
		assertThatThrownBy(() -> db.update("update course_refund_obligations set status='PENDING_REVIEW',"
			+ "approved_amount_vnd=null,approved_by_account_id=null,approval_reason=null,approved_at=null where id=?",
			obligationId)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	@Test
	void duplicateSourceIsIdempotentButChangedReplayConflicts() {
		UUID first = writer.create(enrollmentId, runId, CourseRefundSource.PAYMENT_EXCEPTION,
			BigInteger.valueOf(25_001), receptionistId, "Duplicate provider capture");
		UUID replay = writer.create(enrollmentId, runId, CourseRefundSource.PAYMENT_EXCEPTION,
			BigInteger.valueOf(25_001), receptionistId, "Duplicate provider capture");

		assertThat(replay).isEqualTo(first);
		assertThatThrownBy(() -> writer.create(enrollmentId, runId, CourseRefundSource.PAYMENT_EXCEPTION,
			BigInteger.valueOf(25_002), receptionistId, "Duplicate provider capture"))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_SOURCE_CONFLICT"));
	}

	@Test
	void vndNineteenDigitBoundaryIsStoredAndOverflowIsRejected() {
		BigInteger maximum = new BigInteger("9999999999999999999");
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.MATERIAL_CHANGE,
			maximum, null, "Maximum numeric boundary");
		assertThat(refunds.detail(managerId, obligationId).grossObligationVnd()).isEqualTo(maximum);

		assertThatThrownBy(() -> writer.create(enrollmentId, runId, CourseRefundSource.PAYMENT_EXCEPTION,
			new BigInteger("10000000000000000000"), null, "Numeric overflow"))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_INVALID"));
	}

	@Test
	void completedRefundReducesAvailablePaidAmountAndRejectIsTerminal() {
		UUID completed = writer.create(enrollmentId, runId, CourseRefundSource.UNRESOLVED_SESSIONS,
			BigInteger.valueOf(700_000), null, "Unresolved sessions");
		complete(completed, BigInteger.valueOf(700_000));
		UUID excessive = writer.create(enrollmentId, runId, CourseRefundSource.MEMBER_WITHDRAWAL,
			BigInteger.valueOf(300_001), memberId, "Withdrawal");

		assertThatThrownBy(() -> refunds.approve(managerId, excessive, 0L, "Would exceed paid amount"))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_AMOUNT_EXCEEDS_AVAILABLE"));
		assertThat(refunds.detail(managerId, excessive).status()).isEqualTo("PENDING_REVIEW");
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_REFUND_APPROVED' "
			+ "and target_id=?", Long.class, excessive)).isZero();

		var rejected = refunds.reject(managerId, excessive, 0L, "Accounting cap exceeded");
		assertThat(rejected.status()).isEqualTo("REJECTED");
		assertThat(refunds.reject(managerId, excessive, 0L, "Accounting cap exceeded").version())
			.isEqualTo(rejected.version());
		assertThatThrownBy(() -> refunds.approve(managerId, excessive, 0L, "Conflicting replay"))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_ALREADY_DECIDED"));
	}

	@Test
	void concurrentConflictingManagerDecisionsCommitExactlyOneOutcome() throws Exception {
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.COURSE_RUN_CANCELLED,
			BigInteger.valueOf(100_000), null, "Cancelled run");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var approve = executor.submit(() -> decide(start,
				() -> refunds.approve(managerId, obligationId, 0L, "Approve")));
			var reject = executor.submit(() -> decide(start,
				() -> refunds.reject(secondManagerId, obligationId, 0L, "Reject")));
			start.countDown();
			Object first = approve.get();
			Object second = reject.get();
			assertThat(java.util.stream.Stream.of(first, second)
				.filter(CourseRefundApiModels.RefundObligation.class::isInstance).count()).isOne();
			assertThat(java.util.stream.Stream.of(first, second)
				.filter(CourseRefundException.class::isInstance).count()).isOne();
		}
		assertThat(refunds.detail(managerId, obligationId).status()).isIn("APPROVED", "REJECTED");
	}

	@Test
	void managerApiIsRoleProtectedAndQueueIsPaginated() throws Exception {
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.TRANSFER_CREDIT_EXCESS,
			BigInteger.valueOf(50_000), memberId, "Unused transfer credit");
		assertThat(refunds.list(managerId, 0, 20, "PENDING_REVIEW").items())
			.extracting(CourseRefundApiModels.RefundObligation::id).contains(obligationId);

		mockMvc.perform(get("/api/v1/manager/course-refund-obligations/{id}", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(managerId)))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/manager/course-refund-obligations/{id}", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/manager/course-refund-obligations/{id}/approve", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId))
			.contentType("application/json").content("{\"expectedVersion\":0,\"reason\":\"Forbidden\"}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void receptionistRecordsExactlyOneDisbursementAndManagerVerifiesMatchingEvidence() {
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.MEMBER_WITHDRAWAL,
			BigInteger.valueOf(250_000), memberId, "Withdrawal refund");
		var approved = refunds.approve(managerId, obligationId, 0L, "Approved");
		var input = new CourseRefundApiModels.RefundDisbursement(approved.version(), BigInteger.valueOf(250_000),
			"BANK_TRANSFER", "BANK-REF-1", "evidence:receipt-1");

		var recorded = refunds.recordDisbursement(receptionistId, obligationId, "disburse-once", input);
		var replay = refunds.recordDisbursement(receptionistId, obligationId, "disburse-once", input);
		assertThat(recorded.status()).isEqualTo("DISBURSEMENT_RECORDED");
		assertThat(replay.version()).isEqualTo(recorded.version());
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_REFUND_DISBURSED' "
			+ "and target_id=?", Long.class, obligationId)).isOne();

		var verification = new CourseRefundApiModels.RefundVerification(recorded.version(),
			BigInteger.valueOf(250_000), "BANK-REF-1", "evidence:receipt-1", "Evidence matched");
		var completed = refunds.verify(managerId, obligationId, verification);
		assertThat(completed.status()).isEqualTo("COMPLETED");
		assertThat(completed.verifiedAmountVnd()).isEqualTo(BigInteger.valueOf(250_000));
		assertThat(refunds.verify(managerId, obligationId, verification).version()).isEqualTo(completed.version());
		assertThatThrownBy(() -> refunds.recordDisbursement(receptionistId, obligationId, "second-disbursement", input))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_ALREADY_DISBURSED"));
	}

	@Test
	void disbursementRejectsConflictingReplayAndVerificationMismatch() {
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.PAYMENT_EXCEPTION,
			BigInteger.valueOf(100_000), receptionistId, "Payment exception");
		var approved = refunds.approve(managerId, obligationId, 0L, "Approved");
		var recorded = refunds.recordDisbursement(receptionistId, obligationId, "retry-key",
			new CourseRefundApiModels.RefundDisbursement(approved.version(), BigInteger.valueOf(100_000),
				"CASH", "CASH-1", "evidence:voucher-1"));

		assertThatThrownBy(() -> refunds.recordDisbursement(receptionistId, obligationId, "retry-key",
			new CourseRefundApiModels.RefundDisbursement(approved.version(), BigInteger.valueOf(100_000),
				"CASH", "CASH-2", "evidence:voucher-1")))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_IDEMPOTENCY_CONFLICT"));
		assertThatThrownBy(() -> refunds.verify(managerId, obligationId,
			new CourseRefundApiModels.RefundVerification(recorded.version(), BigInteger.valueOf(99_999),
				"CASH-1", "evidence:voucher-1", "Mismatch")))
			.isInstanceOfSatisfying(CourseRefundException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_REFUND_EVIDENCE_MISMATCH"));
		assertThat(refunds.detail(managerId, obligationId).status()).isEqualTo("DISBURSEMENT_RECORDED");
	}

	@Test
	void memberHistoryIsOwnedAndRedactedWhileRolesRemainSeparated() throws Exception {
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.MATERIAL_CHANGE,
			BigInteger.valueOf(10_000), managerId, "Material change");
		var approved = refunds.approve(managerId, obligationId, 0L, "Approved");
		refunds.recordDisbursement(receptionistId, obligationId, "member-visible",
			new CourseRefundApiModels.RefundDisbursement(approved.version(), BigInteger.valueOf(10_000),
				"CASH", "CASH-REDACT", "private:evidence"));

		mockMvc.perform(get("/api/v1/members/me/course-refund-obligations/{id}", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId)))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.evidenceReference").doesNotExist())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.disbursedByAccountId").doesNotExist());
		mockMvc.perform(get("/api/v1/members/me/course-refund-obligations/{id}", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(account(AccountRole.MEMBER))))
			.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/reception/course-refund-obligations/{id}/disburse", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(memberId))
			.header("Idempotency-Key", "forbidden").contentType("application/json")
			.content("{\"expectedVersion\":2,\"amountVnd\":10000,\"method\":\"CASH\",\"reference\":\"X\",\"evidenceReference\":\"Y\"}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/members/me/course-refund-obligations/{id}", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(coachId)))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/reception/course-refund-obligations/{id}/disburse", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(managerId))
			.header("Idempotency-Key", "manager-forbidden").contentType("application/json").content("{}"))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/v1/manager/course-refund-obligations/{id}/verify", obligationId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionistId))
			.contentType("application/json").content("{}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void twoReceptionistsCompetingToDisburseCommitExactlyOneRecord() throws Exception {
		UUID otherReceptionist = account(AccountRole.RECEPTIONIST);
		UUID obligationId = writer.create(enrollmentId, runId, CourseRefundSource.COURSE_RUN_CANCELLED,
			BigInteger.valueOf(75_000), managerId, "Cancelled run");
		var approved = refunds.approve(managerId, obligationId, 0L, "Approved");
		var input = new CourseRefundApiModels.RefundDisbursement(approved.version(), BigInteger.valueOf(75_000),
			"BANK_TRANSFER", "COMPETE-REF", "evidence:compete");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> decide(start,
				() -> refunds.recordDisbursement(receptionistId, obligationId, "compete-one", input)));
			var second = executor.submit(() -> decide(start,
				() -> refunds.recordDisbursement(otherReceptionist, obligationId, "compete-two", input)));
			start.countDown();
			assertThat(java.util.stream.Stream.of(first.get(), second.get())
				.filter(CourseRefundApiModels.RefundObligation.class::isInstance).count()).isOne();
		}
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_REFUND_DISBURSED' "
			+ "and target_id=?", Long.class, obligationId)).isOne();
	}

	@Test
	void managerCanFailApprovedObligationOrMarkRecordedDisbursementReturned() {
		UUID failedId = writer.create(enrollmentId, runId, CourseRefundSource.UNRESOLVED_SESSIONS,
			BigInteger.valueOf(20_000), managerId, "Unresolved sessions");
		var failedApproved = refunds.approve(managerId, failedId, 0L, "Approved");
		var failed = refunds.fail(managerId, failedId,
			new CourseRefundApiModels.RefundTerminal(failedApproved.version(), "Beneficiary details invalid"));
		assertThat(failed.status()).isEqualTo("FAILED");

		UUID returnedId = writer.create(enrollmentId, runId, CourseRefundSource.TRANSFER_CREDIT_EXCESS,
			BigInteger.valueOf(30_000), managerId, "Transfer excess");
		var returnedApproved = refunds.approve(managerId, returnedId, 0L, "Approved");
		var recorded = refunds.recordDisbursement(receptionistId, returnedId, "returned-payment",
			new CourseRefundApiModels.RefundDisbursement(returnedApproved.version(), BigInteger.valueOf(30_000),
				"BANK_TRANSFER", "RETURN-REF", "evidence:return"));
		var returned = refunds.returned(managerId, returnedId,
			new CourseRefundApiModels.RefundTerminal(recorded.version(), "Bank returned transfer"));
		assertThat(returned.status()).isEqualTo("RETURNED");
		assertThat(refunds.returned(managerId, returnedId,
			new CourseRefundApiModels.RefundTerminal(recorded.version(), "Bank returned transfer")).version())
			.isEqualTo(returned.version());
	}

	private Object decide(CountDownLatch start, java.util.concurrent.Callable<Object> decision) {
		try { start.await(); return decision.call(); }
		catch (Exception exception) { return exception; }
	}

	private void complete(UUID id, BigInteger amount) {
		db.update("""
			update course_refund_obligations set status='APPROVED',approved_amount_vnd=?,approved_by_account_id=?,
			 approval_reason='Approved',approved_at=?,version=1 where id=?
			""", amount, managerId, Timestamp.from(NOW), id);
		db.update("""
			update course_refund_obligations set status='DISBURSEMENT_RECORDED',disbursed_amount_vnd=?,
			 disbursed_by_account_id=?,disbursed_at=?,disbursement_method='CASH',
			 disbursement_reference='TEST-REF',evidence_reference='evidence:test',version=2 where id=?
			""", amount, receptionistId, Timestamp.from(NOW), id);
		db.update("update course_refund_obligations set status='PENDING_VERIFICATION',version=3 where id=?", id);
		db.update("""
			update course_refund_obligations set status='COMPLETED',verified_amount_vnd=?,verified_by_account_id=?,
			 verification_reason='Verified',verified_at=?,version=4 where id=?
			""", amount, managerId, Timestamp.from(NOW), id);
	}

	private void seedEnrollment(BigInteger paid) {
		UUID disciplineId = UUID.randomUUID(), courseId = UUID.randomUUID();
		runId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", disciplineId, "Refund " + disciplineId);
		db.update("insert into courses(id,code,name,discipline_id,status) values(?,?,?,?,'PUBLISHED')",
			courseId, "RF-" + shortId(), "Refund course", disciplineId);
		db.update("""
			insert into course_runs(id,run_code,course_id,primary_coach_account_id,tuition_vnd,capacity,
			 minimum_enrollment,registration_opens_at,registration_closes_at,planned_start_at,planned_end_at,
			 total_units,named_coach_commitment,policy_version_snapshot,status)
			values(?,?,?,?,?,10,1,?,?,?,?,10,true,'REFUND-POLICY','CONFIRMED')
			""", runId, "RF-RUN-" + shortId(), courseId, coachId, paid, Timestamp.from(NOW.minusSeconds(86400)),
			Timestamp.from(NOW.plusSeconds(86400)), Timestamp.from(NOW.plusSeconds(172800)),
			Timestamp.from(NOW.plusSeconds(864000)));
		UUID orderId = UUID.randomUUID(), reservationId = UUID.randomUUID(), paymentId = UUID.randomUUID();
		enrollmentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,"
			+ "tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,?,'VND','PAID',?)",
			orderId, reference, memberId, memberId, runId, paid, "refund-seed-" + orderId);
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) "
			+ "values(?,?,?,?,'ENROLLED',?)", reservationId, orderId, memberId, runId,
			Timestamp.from(NOW.plusSeconds(86400)));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,"
			+ "payment_reference,amount,currency_code,status,deadline_at,method,paid_at) "
			+ "values(?,?,?,?,?,?,?,'VND','PAID',?,'CASH',?)", paymentId, orderId, reservationId,
			memberId, memberId, reference, paid, Timestamp.from(NOW.plusSeconds(86400)), Timestamp.from(NOW));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,"
			+ "member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,"
			+ "cash_paid_vnd) values(?,?,?,?,?,?,?,?,10,?,?)", enrollmentId, orderId, reservationId, paymentId,
			memberId, memberId, runId, paid, Timestamp.from(NOW), paid);
	}

	private UUID account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " refund test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password")).getId();
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
