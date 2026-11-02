package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@org.springframework.test.context.TestPropertySource(properties = {
	"sepay.bank-code=MB", "sepay.bank-account=123456789", "sepay.bank-account-name=SCMS GYM",
	"sepay.bank-destination-sha256=7128318f4501a30f6c5648627306786fb6e7a26a15da6850a959c5b35a0a6859",
	"sepay.webhook-api-key=hook-secret"
})
class CourseTransferIntegrationTests {
	private static final Instant NOW = Instant.parse("2035-01-01T00:00:00Z");
	@Container @ServiceConnection static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Autowired CourseManagementService management;
	@Autowired CourseEnrollmentService enrollmentService;
	@Autowired CourseTransferService transfers;
	@Autowired AccountRepository accounts;
	@Autowired MemberProfileRepository profiles;
	@Autowired JdbcTemplate db;
	@Autowired MockMvc mockMvc;
	@Autowired JwtEncoder jwtEncoder;
	@MockitoBean Clock clock;

	private UUID managerId;
	private UUID coachId;
	private UUID memberId;
	private UUID sourceRunId;
	private UUID targetRunId;
	private UUID sourceEnrollmentId;
	private UUID responseId;

	@BeforeEach
	void setUp() {
		when(clock.instant()).thenReturn(NOW);
		managerId = account(AccountRole.MANAGER).getId();
		coachId = account(AccountRole.COACH).getId();
		memberId = account(AccountRole.MEMBER).getId();
		profiles.saveAndFlush(new MemberProfile(accounts.findById(memberId).orElseThrow()));
		UUID disciplineId = UUID.randomUUID();
		db.update("insert into disciplines(id,name,status) values(?,?,'ACTIVE')", disciplineId, "Transfer " + disciplineId);
		CourseResponse course = management.createCourse(managerId, new CourseCreateRequest("TR-" + shortId(),
			"Transfer course", null, disciplineId, null, 1));
		course = management.publishCourse(managerId, course.id(), new VersionRequest(course.version()));
		sourceRunId = openRun(course.id(), "SRC-" + shortId(), BigInteger.valueOf(100_000));
		targetRunId = openRun(course.id(), "TGT-" + shortId(), BigInteger.valueOf(100_000));
		UUID sourceSession = session(sourceRunId, Instant.parse("2035-02-01T02:00:00Z"));
		session(targetRunId, Instant.parse("2035-02-02T02:00:00Z"));
		seedEnrollment(sourceSession);
		responseId = UUID.randomUUID();
		UUID enrollmentSessionId = db.queryForObject("select id from course_enrollment_sessions where course_enrollment_id=?", UUID.class, sourceEnrollmentId);
		db.update("""
			insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,new_start_at,new_end_at,
			 notification_sent_at,response_deadline,status) values(?,?,?,?,?,?,?,?, 'PENDING')
			""", responseId, enrollmentSessionId, Timestamp.from(NOW.plusSeconds(3600)), Timestamp.from(NOW.plusSeconds(7200)),
			Timestamp.from(NOW.plusSeconds(10800)), Timestamp.from(NOW.plusSeconds(14400)), Timestamp.from(NOW),
			Timestamp.from(NOW.plusSeconds(1800)));
	}

	@Test
	void completedTransferKeepsScheduleResponseRetryIdempotentForReception() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		requestTransfer();
		var request = currentRequest();
		var offer = transfers.approve(managerId, request.id(), targetRunId, "Replay completed transfer");
		transfers.accept(memberId, offer.id(), "completed-replay");
		mockMvc.perform(post("/api/v1/reception/members/{member}/course-schedule-changes/{id}/response", memberId, responseId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.contentType("application/json").content("{\"response\":\"REQUEST_TRANSFER\"}")).andExpect(status().isOk());
		assertThat(db.queryForObject("select count(*) from course_transfer_requests where schedule_change_response_id=?", Long.class, responseId)).isOne();
		assertThat(db.queryForObject("select requester_account_id from course_transfer_requests where id=?", UUID.class, request.id())).isEqualTo(memberId);
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_SCHEDULE_CHANGE_RESPONDED' and target_id=?", Long.class, responseId)).isOne();
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "SOURCE_INACTIVE", "TARGET_SCHEDULE_INVALID" })
	void proxyAcceptanceFailureRollsBackAcceptanceMarkerAndTargetEnrollment(String failure) throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Failure after offer issued");
		if (failure.equals("SOURCE_INACTIVE"))
			db.update("update course_enrollments set lifecycle_status='WITHDRAWN' where id=?", sourceEnrollmentId);
		else db.update("update course_runs set total_units=2 where id=?", targetRunId);
		mockMvc.perform(post("/api/v1/reception/members/{member}/course-transfer-offers/{id}/accept", memberId, offer.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "must-rollback")).andExpect(status().isConflict());
		assertThat(db.queryForObject("select accepted_at from course_transfer_offers where id=?", Timestamp.class, offer.id())).isNull();
		assertThat(db.queryForObject("select acceptance_idempotency_key from course_transfer_offers where id=?", String.class, offer.id())).isNull();
		assertThat(db.queryForObject("select count(*) from course_enrollments where source_transfer_offer_id=?", Long.class, offer.id())).isZero();
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_TRANSFER_ACCEPTED' and target_id=?", Long.class, offer.id())).isZero();
	}

	@Test
	void proxyDifferencePaymentUsesExistingSepayWebhookAndKeepsInitiatorOnReplay() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		db.update("update course_runs set tuition_vnd=120000 where id=?", targetRunId);
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Mock bank difference payment");
		String base = "/api/v1/reception/members/" + memberId, auth = "Bearer " + token(receptionist);
		mockMvc.perform(post(base + "/course-transfer-offers/{id}/accept", offer.id()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, auth).header("Idempotency-Key", "proxy-bank-accept")).andExpect(status().isOk());
		UUID orderId = db.queryForObject("select difference_order_id from course_transfer_offers where id=?", UUID.class, offer.id());
		for (int retry = 0; retry < 2; retry++) mockMvc.perform(post(base + "/course-orders/{id}/payments/sepay", orderId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).header("Idempotency-Key", "proxy-bank-payment"))
			.andExpect(status().isOk());
		String reference = db.queryForObject("select payment_reference from course_payments where course_order_id=?", String.class, orderId);
		String webhook = """
			{"id":830001,"code":"%s","content":"%s","transferAmount":20000,"transferType":"in","accountNumber":"123456789"}
			""".formatted(reference, reference);
		for (int retry = 0; retry < 2; retry++) mockMvc.perform(post("/api/v1/payments/sepay/webhook").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Apikey hook-secret").contentType("application/json").content(webhook))
			.andExpect(status().isOk());
		assertThat(db.queryForObject("select status from course_transfer_offers where id=?", String.class, offer.id())).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select actor_account_id from course_enrollments where source_transfer_offer_id=?", UUID.class, offer.id())).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select processed_by_account_id from course_enrollments where source_transfer_offer_id=?", UUID.class, offer.id())).isNull();
		assertThat(db.queryForObject("select count(*) from course_enrollments where source_transfer_offer_id=?", Long.class, offer.id())).isOne();
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_TRANSFER_COMPLETED' and target_id=?", Long.class, offer.id())).isOne();
		assertThat(db.queryForObject("select payload->>'initiatorAccountId' from notification_outbox where event_key=?", String.class, "course-transfer-completed:" + offer.id())).isEqualTo(receptionist.getId().toString());
		assertThat(db.queryForObject("select payload->>'actorAccountId' from notification_outbox where event_key=?", String.class, "course-transfer-completed:" + offer.id())).isNull();
	}

	@Test
	void proxyAcceptanceReplaysZeroDifferenceAndPreservesActorAndNotifications() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Proxy equal price");
		String path = "/api/v1/reception/members/" + memberId + "/course-transfer-offers/" + offer.id() + "/accept";
		String first = mockMvc.perform(post(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "proxy-zero")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		assertThat(mockMvc.perform(post(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "proxy-zero")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).isEqualTo(first);
		mockMvc.perform(post(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "different-key")).andExpect(status().isConflict());
		assertThat(db.queryForObject("select actor_account_id from course_enrollments where source_transfer_offer_id=?", UUID.class, offer.id())).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select actor_account_id from audit_events where action='COURSE_TRANSFER_ACCEPTED' and target_id=?", UUID.class, offer.id())).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select payload->>'actorAccountId' from notification_outbox where event_key=?", String.class, "course-transfer-completed:" + offer.id())).isEqualTo(receptionist.getId().toString());
		assertThat(db.queryForObject("select recipient_account_id from notification_outbox where event_key=?", UUID.class, "course-transfer-completed:" + offer.id())).isEqualTo(memberId);
		assertThat(db.queryForObject("select count(*) from course_enrollments where source_transfer_offer_id=?", Long.class, offer.id())).isOne();
	}

	@Test
	void proxyResponsePreservesOwnershipDeadlineAndContinueReplay() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST), other = account(AccountRole.MEMBER);
		String auth = "Bearer " + token(receptionist);
		String own = "/api/v1/reception/members/" + memberId + "/course-schedule-changes/" + responseId + "/response";
		mockMvc.perform(post("/api/v1/reception/members/{member}/course-schedule-changes/{id}/response", other.getId(), responseId)
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).contentType("application/json")
			.content("{\"response\":\"CONTINUE\"}")).andExpect(status().isNotFound());
		db.update("update course_schedule_change_responses set response_deadline=? where id=?", Timestamp.from(NOW), responseId);
		mockMvc.perform(post(own).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).contentType("application/json")
			.content("{\"response\":\"CONTINUE\"}")).andExpect(status().isConflict());
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_SCHEDULE_CHANGE_RESPONDED' and target_id=?", Long.class, responseId)).isZero();
		db.update("update course_schedule_change_responses set response_deadline=? where id=?", Timestamp.from(NOW.plusSeconds(1800)), responseId);
		for (int retry = 0; retry < 2; retry++) mockMvc.perform(post(own).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth)
			.contentType("application/json").content("{\"response\":\"CONTINUE\"}")).andExpect(status().isOk());
		mockMvc.perform(post(own).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).contentType("application/json")
			.content("{\"response\":\"REQUEST_WITHDRAWAL\"}")).andExpect(status().isConflict());
		assertThat(db.queryForObject("select actor_account_id from audit_events where action='COURSE_SCHEDULE_CHANGE_RESPONDED' and target_id=?", UUID.class, responseId)).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, sourceEnrollmentId)).isEqualTo("ACTIVE");
	}

	@Test
	void proxyOfferCannotCrossOwnersOmitKeysOrAcceptAtExpiry() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST), other = account(AccountRole.MEMBER);
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Proxy boundaries");
		String auth = "Bearer " + token(receptionist), path = "/api/v1/reception/members/" + memberId + "/course-transfer-offers/" + offer.id() + "/accept";
		mockMvc.perform(post("/api/v1/reception/members/{member}/course-transfer-offers/{id}/accept", other.getId(), offer.id())
			.contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).header("Idempotency-Key", "wrong-owner")).andExpect(status().isNotFound());
		mockMvc.perform(post(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isBadRequest());
		db.update("update accounts set status='SUSPENDED' where id=?", memberId);
		mockMvc.perform(post(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).header("Idempotency-Key", "inactive")).andExpect(status().isUnauthorized());
		db.update("update accounts set status='ACTIVE' where id=?", memberId);
		db.update("update course_transfer_offers set expires_at=? where id=?", Timestamp.from(NOW), offer.id());
		db.update("update course_transfer_holds set expires_at=? where transfer_offer_id=?", Timestamp.from(NOW), offer.id());
		mockMvc.perform(post(path).contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth).header("Idempotency-Key", "expired")).andExpect(status().isConflict());
		assertThat(db.queryForObject("select status from course_transfer_offers where id=?", String.class, offer.id())).isEqualTo("EXPIRED");
		assertThat(db.queryForObject("select status from course_transfer_holds where transfer_offer_id=?", String.class, offer.id())).isEqualTo("RELEASED");
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, sourceEnrollmentId)).isEqualTo("ACTIVE");
	}

	@Test
	void receptionistRequestsAndAcceptsTransferWithCorrectActorThroughExistingCashPayment() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		String auth = "Bearer " + token(receptionist);
		String base = "/api/v1/reception/members/" + memberId;
		db.update("update course_runs set tuition_vnd=120000 where id=?", targetRunId);
		mockMvc.perform(get(base + "/course-schedule-changes").contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth))
			.andExpect(status().isOk());
		mockMvc.perform(post(base + "/course-schedule-changes/{id}/response", responseId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, auth).contentType("application/json").content("{\"response\":\"REQUEST_TRANSFER\"}"))
			.andExpect(status().isOk());
		var request = currentRequest();
		assertThat(db.queryForObject("select requester_account_id from course_transfer_requests where id=?", UUID.class, request.id())).isEqualTo(receptionist.getId());
		var offer = transfers.approve(managerId, request.id(), targetRunId, "Reception transfer");
		mockMvc.perform(get(base + "/course-transfer-offers").contextPath("/api/v1").header(HttpHeaders.AUTHORIZATION, auth))
			.andExpect(status().isOk());
		mockMvc.perform(post(base + "/course-transfer-offers/{id}/accept", offer.id()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, auth).header("Idempotency-Key", "proxy-transfer")).andExpect(status().isOk());
		UUID orderId = db.queryForObject("select difference_order_id from course_transfer_offers where id=?", UUID.class, offer.id());
		assertThat(db.queryForObject("select actor_account_id from course_orders where id=?", UUID.class, orderId)).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select tuition_vnd_snapshot from course_orders where id=?", java.math.BigDecimal.class, orderId)).isEqualByComparingTo("20000");
		Account cashier = account(AccountRole.RECEPTIONIST);
		mockMvc.perform(post(base + "/course-orders/{id}/payments/cash", orderId).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(cashier)).header("Idempotency-Key", "proxy-transfer-cash"))
			.andExpect(status().isOk());
		assertThat(db.queryForObject("select actor_account_id from course_enrollments where source_transfer_offer_id=?", UUID.class, offer.id())).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select processed_by_account_id from course_enrollments where source_transfer_offer_id=?", UUID.class, offer.id())).isEqualTo(cashier.getId());
		assertThat(db.queryForObject("select status from course_transfer_offers where id=?", String.class, offer.id())).isEqualTo("COMPLETED");
	}

	@Test
	void receptionistScheduleWithdrawalCapturesSnapshotAndActorOnce() throws Exception {
		Account receptionist = account(AccountRole.RECEPTIONIST);
		String path = "/api/v1/reception/members/" + memberId + "/course-schedule-changes/" + responseId + "/response";
		for (int retry = 0; retry < 2; retry++) mockMvc.perform(post(path).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist)).contentType("application/json")
			.content("{\"response\":\"REQUEST_WITHDRAWAL\"}")).andExpect(status().isOk());
		assertThat(db.queryForObject("select requester_account_id from course_withdrawal_requests where schedule_change_response_id=?", UUID.class, responseId)).isEqualTo(receptionist.getId());
		assertThat(db.queryForObject("select snapshot_captured_at from course_withdrawal_requests where schedule_change_response_id=?", Timestamp.class, responseId).toInstant()).isEqualTo(NOW);
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, sourceEnrollmentId)).isEqualTo("WITHDRAWAL_PENDING");
		assertThat(db.queryForObject("select count(*) from audit_events where action='COURSE_SCHEDULE_CHANGE_RESPONDED' and target_id=?", Long.class, responseId)).isOne();
		assertThat(db.queryForObject("select actor_account_id from audit_events where action='COURSE_SCHEDULE_CHANGE_RESPONDED' and target_id=?", UUID.class, responseId)).isEqualTo(receptionist.getId());
	}

	@Test
	void equalPriceOfferMovesEnrollmentOnceAndPreservesSourceHistory() {
		enrollmentService.respondToScheduleChange(memberId, responseId,
			new CourseScheduleChangeApiModels.ScheduleChangeResponse("REQUEST_TRANSFER"));
		var request = currentRequest();
		assertThat(request.memberId()).isEqualTo(memberId);
		assertThat(request.memberCode()).isNotBlank();
		assertThat(request.memberName()).isEqualTo("MEMBER transfer test");
		assertThat(request.sourceCourseId()).isNotNull();
		assertThat(request.sourceCourseCode()).startsWith("TR-");
		assertThat(request.sourceCourseName()).isEqualTo("Transfer course");
		assertThat(request.sourceRunCode()).startsWith("SRC-");
		assertThat(request.sourceCourseRunStatus()).isEqualTo("OPEN_FOR_REGISTRATION");
		assertThat(request.targetCourseCode()).isNull();
		var offer = transfers.approve(managerId, request.id(), targetRunId, "Material schedule change");

		assertThat(offer.creditVnd()).isEqualTo(BigInteger.valueOf(100_000));
		assertThat(offer.differenceVnd()).isZero();
		assertThat(offer.memberCode()).isEqualTo(request.memberCode());
		assertThat(offer.memberName()).isEqualTo(request.memberName());
		assertThat(offer.sourceCourseCode()).isEqualTo(request.sourceCourseCode());
		assertThat(offer.targetCourseId()).isEqualTo(request.sourceCourseId());
		assertThat(offer.targetCourseCode()).isEqualTo(request.sourceCourseCode());
		assertThat(offer.targetCourseName()).isEqualTo(request.sourceCourseName());
		assertThat(offer.targetRunCode()).startsWith("TGT-");
		assertThat(offer.targetCourseRunStatus()).isEqualTo("OPEN_FOR_REGISTRATION");
		assertThat(db.queryForObject("select count(*) from course_transfer_holds where transfer_offer_id=? and status='HELD'", Long.class, offer.id())).isOne();

		var completed = transfers.accept(memberId, offer.id(), "accept-equal-price");
		assertThat(completed.status()).isEqualTo("COMPLETED");
		assertThat(transfers.accept(memberId, offer.id(), "accept-equal-price").status()).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, sourceEnrollmentId)).isEqualTo("TRANSFERRED");
		assertThat(db.queryForObject("select count(*) from course_enrollments where member_account_id=? and course_run_id=? and lifecycle_status='ACTIVE'", Long.class, memberId, targetRunId)).isOne();
		assertThat(db.queryForObject("select count(*) from course_enrollments where source_transfer_offer_id=?", Long.class, offer.id())).isOne();
		assertThat(db.queryForObject("select status from course_transfer_holds where transfer_offer_id=?", String.class, offer.id())).isEqualTo("CONSUMED");
		assertThat(db.queryForObject("select count(*) from notification_outbox where recipient_account_id=? and notification_type='COURSE_TRANSFER_COMPLETED'", Long.class, memberId)).isOne();
	}

	@Test
	void lowerPriceCompletesAtomicallyAndCreatesOneCreditExcessObligation() {
		db.update("update course_runs set tuition_vnd=80000 where id=?", targetRunId);
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Lower price target");
		assertThat(offer.differenceVnd()).isEqualTo(BigInteger.valueOf(-20_000));

		assertThat(transfers.accept(memberId, offer.id(), "accept-lower-price").status()).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select amount_vnd from course_refund_obligations where course_enrollment_id=? and source_type='TRANSFER_CREDIT_EXCESS'",
			java.math.BigDecimal.class, sourceEnrollmentId)).isEqualByComparingTo("20000");
		assertThat(transfers.accept(memberId, offer.id(), "accept-lower-price").status()).isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select count(*) from course_refund_obligations where course_enrollment_id=? and source_type='TRANSFER_CREDIT_EXCESS'",
			Long.class, sourceEnrollmentId)).isOne();
	}

	@Test
	void higherPriceUsesOnlyDifferenceOrderThenCashFulfillmentCommitsTransfer() throws Exception {
		db.update("update course_runs set tuition_vnd=120000 where id=?", targetRunId);
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Higher price target");
		var pending = transfers.accept(memberId, offer.id(), "accept-higher-price");
		assertThat(pending.status()).isEqualTo("PENDING_PAYMENT");
		assertThat(db.queryForObject("select tuition_vnd_snapshot from course_orders where id=?", java.math.BigDecimal.class,
			pending.differenceOrderId())).isEqualByComparingTo("20000");
		assertThat(db.queryForObject("select count(*) from course_transfer_holds where transfer_offer_id=? and status='HELD'",
			Long.class, offer.id())).isZero();

		Account receptionist = account(AccountRole.RECEPTIONIST);
		mockMvc.perform(post("/api/v1/reception/members/{memberId}/course-orders/{orderId}/payments/cash",
			memberId, pending.differenceOrderId()).contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(receptionist))
			.header("Idempotency-Key", "transfer-cash-payment"))
			.andExpect(status().isOk());

		assertThat(db.queryForObject("select status from course_transfer_offers where id=?", String.class, offer.id()))
			.isEqualTo("COMPLETED");
		assertThat(db.queryForObject("select cash_paid_vnd from course_enrollments where source_transfer_offer_id=?",
			java.math.BigDecimal.class, offer.id())).isEqualByComparingTo("20000");
		assertThat(db.queryForObject("select credit_applied_vnd from course_enrollments where source_transfer_offer_id=?",
			java.math.BigDecimal.class, offer.id())).isEqualByComparingTo("100000");
	}

	@Test
	void expiryReleasesSeatAndKeepsSourceEnrollmentActive() {
		requestTransfer();
		var offer = transfers.approve(managerId, currentRequest().id(), targetRunId, "Expiring offer");
		db.update("update course_transfer_offers set expires_at=? where id=?", Timestamp.from(NOW), offer.id());
		db.update("update course_transfer_holds set expires_at=? where transfer_offer_id=?", Timestamp.from(NOW), offer.id());

		assertThatThrownBy(() -> transfers.accept(memberId, offer.id(), "accept-expired"))
			.isInstanceOfSatisfying(CourseTransferException.class,
				error -> assertThat(error.code()).isEqualTo("COURSE_TRANSFER_OFFER_EXPIRED"));
		assertThat(db.queryForObject("select status from course_transfer_offers where id=?", String.class, offer.id()))
			.isEqualTo("EXPIRED");
		assertThat(db.queryForObject("select status from course_transfer_holds where transfer_offer_id=?", String.class, offer.id()))
			.isEqualTo("RELEASED");
		assertThat(db.queryForObject("select lifecycle_status from course_enrollments where id=?", String.class, sourceEnrollmentId))
			.isEqualTo("ACTIVE");
	}

	@Test
	void securityKeepsManagerAndMemberSurfacesSeparated() throws Exception {
		Account member = accounts.findById(memberId).orElseThrow();
		Account manager = accounts.findById(managerId).orElseThrow();
		mockMvc.perform(get("/api/v1/manager/course-transfer-requests").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(member))).andExpect(status().isForbidden());
		mockMvc.perform(get("/api/v1/members/me/course-transfer-offers").contextPath("/api/v1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(manager))).andExpect(status().isForbidden());
	}

	private void requestTransfer() {
		enrollmentService.respondToScheduleChange(memberId, responseId,
			new CourseScheduleChangeApiModels.ScheduleChangeResponse("REQUEST_TRANSFER"));
	}

	private CourseTransferApiModels.TransferRequest currentRequest() {
		return transfers.managerQueue(managerId).stream()
			.filter(request -> request.scheduleChangeResponseId().equals(responseId)).findFirst().orElseThrow();
	}

	private UUID openRun(UUID courseId, String code, BigInteger tuition) {
		CourseRunResponse run = management.createRun(managerId, new CourseRunCreateRequest(code, courseId, coachId,
			tuition, 5, 1, NOW.minusSeconds(3600), NOW.plusSeconds(86400 * 10), NOW.plusSeconds(86400 * 20),
			NOW.plusSeconds(86400 * 60), 1, true));
		return management.openRun(managerId, run.id(), new CourseRunOpenRequest(run.version(), "TRANSFER-POLICY")).id();
	}

	private UUID session(UUID runId, Instant start) {
		UUID roomId = UUID.randomUUID();
		db.update("insert into rooms(id,name,capacity,usage_mode,status) values(?,?,10,'EXCLUSIVE','ACTIVE')", roomId, "Transfer room " + roomId);
		return management.createSession(managerId, runId,
			new CourseSessionCreateRequest(1, roomId, coachId, start, start.plusSeconds(3600), BigInteger.valueOf(100_000))).id();
	}

	private void seedEnrollment(UUID sessionId) {
		UUID orderId = UUID.randomUUID(), reservationId = UUID.randomUUID(), paymentId = UUID.randomUUID();
		sourceEnrollmentId = UUID.randomUUID();
		String reference = "COR-" + orderId.toString().replace("-", "").toUpperCase();
		Instant expires = NOW.plusSeconds(86400);
		db.update("insert into course_orders(id,order_reference,member_account_id,actor_account_id,course_run_id,tuition_vnd_snapshot,currency_code,status,idempotency_key) values(?,?,?,?,?,100000,'VND','PAID',?)",
			orderId, reference, memberId, memberId, sourceRunId, "seed-" + orderId);
		db.update("insert into course_reservations(id,course_order_id,member_account_id,course_run_id,status,expires_at) values(?,?,?,?,'ENROLLED',?)",
			reservationId, orderId, memberId, sourceRunId, Timestamp.from(expires));
		db.update("insert into course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id,payment_reference,amount,currency_code,status,deadline_at,method,paid_at) values(?,?,?,?,?,?,100000,'VND','PAID',?,'CASH',?)",
			paymentId, orderId, reservationId, memberId, memberId, reference, Timestamp.from(expires), Timestamp.from(NOW));
		db.update("insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,member_account_id,actor_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,cash_paid_vnd) values(?,?,?,?,?,?,?,100000,1,?,100000)",
			sourceEnrollmentId, orderId, reservationId, paymentId, memberId, memberId, sourceRunId, Timestamp.from(NOW));
		db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,unit_number,allocation_vnd,disposition) values(?,?,?,?,1,100000,'ENROLLED')",
			UUID.randomUUID(), sourceEnrollmentId, sessionId, sourceRunId);
	}

	private Account account(AccountRole role) {
		UUID id = UUID.randomUUID();
		return accounts.saveAndFlush(new Account(id, role, AccountStatus.ACTIVE, role + " transfer test",
			"09" + id.toString().replace("-", "").substring(0, 8), id + "@example.test",
			LocalDate.of(1990, 1, 1), "{noop}password"));
	}
	private String token(Account account) {
		Instant issued = NOW.minusSeconds(60);
		JwtClaimsSet claims = JwtClaimsSet.builder().issuer("scms-backend").issuedAt(issued)
			.expiresAt(NOW.plusSeconds(3600)).subject(account.getId().toString()).claim("role", account.getRole().name())
			.claim("account_status", account.getStatus().name()).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
	private static String shortId() { return UUID.randomUUID().toString().substring(0, 8); }
}
