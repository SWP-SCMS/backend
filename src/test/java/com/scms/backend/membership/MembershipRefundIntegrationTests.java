package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.sql.Timestamp;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.scms.backend.ScmsBackendApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.backend.notification.NotificationOutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(classes = ScmsBackendApplication.class,
	properties = "scms.notification-outbox-initial-delay-ms=86400000")
@ActiveProfiles("test")
class MembershipRefundIntegrationTests {
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
	@MockitoSpyBean JdbcTemplate jdbc;
	@Autowired MembershipRefundService refunds;
	@Autowired PaymentService payments;
	@Autowired MembershipLifecycleService lifecycle;
	@Autowired NotificationOutboxDispatcher dispatcher;

	@Test
	void refundStaffMatrixSurvivesOutboxDeliveryFailureReplayAndRetry() throws Exception {
		UUID manager = account("MANAGER");
		account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		account("RECEPTIONIST");
		for (String role : List.of("MANAGER", "RECEPTIONIST")) {
			for (String status : List.of("INACTIVE", "SUSPENDED")) {
				UUID excluded = account(role);
				jdbc.update("update accounts set status=? where id=?", status, excluded);
			}
		}
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		String code = jdbc.queryForObject("select member_code from member_profiles where account_id=?", String.class, member);
		SourceFixture source = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(),
			LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(50));
		RefundRequestCreateRequest input = new RefundRequestCreateRequest(source.paymentId(), source.membershipId(),
			BigInteger.valueOf(100), "private request reason", UUID.randomUUID().toString());
		RefundRequestResponse created = refunds.create(receptionist, code, input);
		assertStaffOutbox(created, activeStaff("MANAGER"));
		long initialCount = refundOutboxCount(created.id());
		assertThat(refunds.create(receptionist, code, input).id()).isEqualTo(created.id());
		assertThat(refundOutboxCount(created.id())).isEqualTo(initialCount);
		jdbc.update("update accounts set full_name='Renamed Refund Member' where id=?", member);
		RefundRequestResponse approved = refunds.firstApproval(manager, created.id(),
			new RefundFirstApprovalRequest(created.version(), BigInteger.valueOf(100)));
		assertStaffOutbox(approved, activeStaff("RECEPTIONIST"));
		RefundRequestResponse disbursed = refunds.cashDisbursement(receptionist, created.id(),
			new RefundCashDisbursementRequest(approved.version(), "private cash evidence"));
		assertStaffOutbox(disbursed, activeStaff("MANAGER"));
		RefundRequestResponse completed = refunds.finalApproval(manager, created.id(), new RefundActionRequest(disbursed.version(), null));
		assertStaffOutbox(completed, List.of(receptionist));
		long completedCount = refundOutboxCount(created.id());
		assertThatThrownBy(() -> refunds.finalApproval(manager, created.id(), new RefundActionRequest(disbursed.version(), null)))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_REQUEST_CHANGED");
		assertThat(refundOutboxCount(created.id())).isEqualTo(completedCount);
		assertThat(jdbc.queryForObject("select count(*) from notifications where target_id=?", Long.class, created.id())).isZero();

		String staffKey = "MEMBERSHIP_REFUND_COMPLETED:" + created.id() + ":" + receptionist;
		jdbc.execute("create function fail_refund_staff_delivery() returns trigger language plpgsql as $$ "
			+ "begin raise exception 'forced refund staff delivery failure'; end $$");
		jdbc.execute("create trigger fail_refund_staff_delivery before insert on notifications for each row "
			+ "when (new.event_key='" + staffKey + "') execute function fail_refund_staff_delivery()");
		try {
			dispatchRefundEvents(created.id());
			assertThat(jdbc.queryForObject("select status from notification_outbox where event_key=?", String.class, staffKey)).isEqualTo("FAILED");
			assertThat(jdbc.queryForObject("select count(*) from notifications where event_key=?", Long.class, staffKey)).isZero();
		} finally {
			jdbc.execute("drop trigger fail_refund_staff_delivery on notifications");
			jdbc.execute("drop function fail_refund_staff_delivery()");
		}
		jdbc.update("update notification_outbox set next_attempt_at=current_timestamp where event_key=?", staffKey);
		dispatchRefundEvents(created.id());
		assertDeliveredRefundEvents(created.id(), completedCount);
		// A delivery replay must also be harmless after the inbox insert already committed.
		jdbc.update("update notification_outbox set status='FAILED',next_attempt_at=current_timestamp where event_key=?", staffKey);
		dispatchRefundEvents(created.id());
		assertDeliveredRefundEvents(created.id(), completedCount);

		SourceFixture rejectedSource = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(),
			LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(50));
		RefundRequestResponse pending = refunds.create(receptionist, code, new RefundRequestCreateRequest(rejectedSource.paymentId(),
			rejectedSource.membershipId(), BigInteger.ONE, "private request reason", UUID.randomUUID().toString()));
		RefundRequestResponse rejected = refunds.reject(manager, pending.id(), new RefundActionRequest(pending.version(), "private rejection reason"));
		assertStaffOutbox(rejected, List.of(receptionist));
		dispatchRefundEvents(rejected.id());
		assertDeliveredRefundEvents(rejected.id(), refundOutboxCount(rejected.id()));
	}

	@ParameterizedTest
	@CsvSource({"COMPLETED,INACTIVE,RECEPTIONIST", "REJECTED,INACTIVE,RECEPTIONIST",
		"COMPLETED,SUSPENDED,RECEPTIONIST", "REJECTED,SUSPENDED,RECEPTIONIST",
		"COMPLETED,ACTIVE,MANAGER", "REJECTED,ACTIVE,MANAGER",
		"COMPLETED,ACTIVE,COACH", "REJECTED,ACTIVE,COACH"})
	void terminalStaffRoutingRechecksRequesterStatusAndRole(String action, String status, String role) throws Exception {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		String code = jdbc.queryForObject("select member_code from member_profiles where account_id=?", String.class, member);
		SourceFixture source = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(),
			LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(50));
		RefundRequestResponse current = refunds.create(receptionist, code, new RefundRequestCreateRequest(source.paymentId(),
			source.membershipId(), BigInteger.ONE, "private reason", UUID.randomUUID().toString()));
		if (action.equals("COMPLETED")) {
			current = refunds.firstApproval(manager, current.id(), new RefundFirstApprovalRequest(current.version(), BigInteger.ONE));
			current = refunds.cashDisbursement(receptionist, current.id(), new RefundCashDisbursementRequest(current.version(), "private evidence"));
		}
		jdbc.update("update accounts set status=?,role=? where id=?", status, role, receptionist);
		UUID actor = role.equals("MANAGER") ? receptionist : manager;
		RefundRequestResponse terminal = action.equals("COMPLETED")
			? refunds.finalApproval(actor, current.id(), new RefundActionRequest(current.version(), null))
			: refunds.reject(actor, current.id(), new RefundActionRequest(current.version(), "private rejection reason"));
		assertStaffOutbox(terminal, List.of());
	}

	@ParameterizedTest
	@ValueSource(strings = {"FIRST_APPROVED", "COMPLETED"})
	void staffEnqueueFailureRollsBackTransitionAuditMemberEventAndCreditNote(String action) {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		SourceFixture source = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(),
			LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(50));
		refund(source, receptionist, "REQUESTED");
		UUID id = jdbc.queryForObject("select id from refund_requests where payment_id=?", UUID.class, source.paymentId());
		long version = 0;
		if (action.equals("COMPLETED")) {
			RefundRequestResponse approved = refunds.firstApproval(manager, id, new RefundFirstApprovalRequest(0L, BigInteger.ONE));
			version = refunds.cashDisbursement(receptionist, id, new RefundCashDisbursementRequest(approved.version(), "private evidence")).version();
		}
		long expectedVersion = version;
		String type = "MEMBERSHIP_REFUND_" + action;
		jdbc.execute("create function fail_refund_staff_enqueue() returns trigger language plpgsql as $$ "
			+ "begin raise exception 'forced refund staff enqueue failure'; end $$");
		jdbc.execute("create trigger fail_refund_staff_enqueue before insert on notification_outbox for each row "
			+ "when (new.target_id='" + id + "'::uuid and new.notification_type='" + type
			+ "' and new.recipient_account_id='" + receptionist + "'::uuid) execute function fail_refund_staff_enqueue()");
		try {
			assertThatThrownBy(() -> {
				if (action.equals("COMPLETED")) refunds.finalApproval(manager, id, new RefundActionRequest(expectedVersion, null));
				else refunds.firstApproval(manager, id, new RefundFirstApprovalRequest(expectedVersion, BigInteger.ONE));
			}).isInstanceOf(RuntimeException.class).hasStackTraceContaining("forced refund staff enqueue failure");
		} finally {
			jdbc.execute("drop trigger fail_refund_staff_enqueue on notification_outbox");
			jdbc.execute("drop function fail_refund_staff_enqueue()");
		}
		assertThat(jdbc.queryForObject("select status from refund_requests where id=?", String.class, id))
			.isEqualTo(action.equals("COMPLETED") ? "CASH_DISBURSED" : "REQUESTED");
		assertThat(jdbc.queryForObject("select version from refund_requests where id=?", Long.class, id)).isEqualTo(expectedVersion);
		assertThat(jdbc.queryForObject("select count(*) from audit_events where target_id=? and action=?", Long.class, id, type)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from notification_outbox where target_id=? and notification_type=?", Long.class, id, type)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from notifications where target_id=? and notification_type=?", Long.class, id, type)).isZero();
		assertThat(jdbc.queryForObject("select count(*) from credit_notes where refund_request_id=?", Long.class, id)).isZero();
		if (action.equals("COMPLETED")) refunds.finalApproval(manager, id, new RefundActionRequest(expectedVersion, null));
		else refunds.firstApproval(manager, id, new RefundFirstApprovalRequest(expectedVersion, BigInteger.ONE));
		assertThat(jdbc.queryForObject("select count(*) from notification_outbox where event_key=?", Long.class,
			type + ":" + id + ":" + receptionist)).isOne();
	}

	private List<UUID> activeStaff(String role) {
		return jdbc.queryForList("select id from accounts where role=? and status='ACTIVE'", UUID.class, role);
	}

	private long refundOutboxCount(UUID id) {
		return jdbc.queryForObject("select count(*) from notification_outbox where target_id=?", Long.class, id);
	}

	private void assertStaffOutbox(RefundRequestResponse response, List<UUID> staff) throws Exception {
		String type = "MEMBERSHIP_REFUND_" + response.status();
		List<Map<String, Object>> events = jdbc.queryForList("select event_key,recipient_account_id,target_type,payload::text from notification_outbox where target_id=? and notification_type=?",
			response.id(), type);
		assertThat(events).extracting(row -> row.get("recipient_account_id"))
			.containsExactlyInAnyOrderElementsOf(java.util.stream.Stream.concat(java.util.stream.Stream.of(response.memberAccountId()), staff.stream()).toList());
		for (Map<String, Object> event : events) {
			UUID recipient = (UUID) event.get("recipient_account_id");
			assertThat(event.get("target_type")).isEqualTo("REFUND_REQUEST");
			boolean isMember = recipient.equals(response.memberAccountId());
			assertThat(event.get("event_key")).isEqualTo(type + ":" + response.id() + (isMember ? "" : ":" + recipient));
			if (isMember) continue;
			Map<String, Object> expected = new HashMap<>();
			expected.put("refundRequestId", response.id().toString());
			expected.put("memberCode", response.memberCode());
			expected.put("memberFullName", response.memberFullName());
			expected.put("status", response.status().name());
			if (response.approvedAmount() != null) expected.put("approvedAmount", response.approvedAmount().toString());
			ObjectMapper json = new ObjectMapper();
			assertThat(json.readTree((String) event.get("payload"))).isEqualTo(json.valueToTree(expected));
		}
	}

	private void dispatchRefundEvents(UUID id) {
		for (int attempt = 0; attempt < 20 && dueRefundEvents(id) > 0; attempt++) dispatcher.dispatchAvailable();
		assertThat(dueRefundEvents(id)).isZero();
	}

	private long dueRefundEvents(UUID id) {
		return jdbc.queryForObject("select count(*) from notification_outbox where target_id=? and status in ('PENDING','FAILED','PROCESSING') and next_attempt_at<=current_timestamp", Long.class, id);
	}

	private void assertDeliveredRefundEvents(UUID id, long expectedCount) {
		assertThat(jdbc.queryForObject("select count(*) from notification_outbox where target_id=? and status='SENT'", Long.class, id)).isEqualTo(expectedCount);
		assertThat(jdbc.queryForObject("select count(*) from notifications where target_id=?", Long.class, id)).isEqualTo(expectedCount);
		assertThat(jdbc.queryForObject("select count(*) from notification_outbox o join notifications n on n.event_key=o.event_key where o.target_id=? and n.recipient_account_id=o.recipient_account_id and n.notification_type=o.notification_type and n.target_type=o.target_type and n.target_id=o.target_id and n.payload=o.payload", Long.class, id)).isEqualTo(expectedCount);
	}

	@Test
	void refundDisplayJoinsCurrentSourcesAcrossWorkflowReplayAndInactiveHistory() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		String code = jdbc.queryForObject("select member_code from member_profiles where account_id=?", String.class, member);
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
		String plan = "REFUND_" + UUID.randomUUID().toString().replace("-", "");
		jdbc.update("insert into membership_plans(plan_code,display_name,supports_booking,supports_personal_coaching) values(?,'Refund history plan',false,false)", plan);
		SourceFixture source = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(), today.minusDays(50),
			"actual-refund-cash-reference", plan);
		jdbc.update("update membership_plans set status='INACTIVE' where plan_code=?", plan);
		jdbc.update("update membership_offers set status='INACTIVE' where id=(select offer_id from membership_orders where id=?)", source.orderId());
		RefundRequestCreateRequest input = new RefundRequestCreateRequest(source.paymentId(), source.membershipId(),
			BigInteger.valueOf(10_000), "display workflow", UUID.randomUUID().toString());
		RefundRequestResponse created = refunds.create(receptionist, code, input);
		assertDisplay(created, source, code, "Refund MEMBER", "actual-refund-cash-reference");
		assertThat(created.version()).isZero();
		jdbc.update("update accounts set full_name='Renamed Refund Member' where id=?", member);
		RefundRequestResponse replay = refunds.create(receptionist, code, input);
		assertDisplay(replay, source, code, "Renamed Refund Member", "actual-refund-cash-reference");
		assertThat(replay.id()).isEqualTo(created.id());
		assertThat(replay.version()).isEqualTo(created.version());
		RefundRequestResponse approved = refunds.firstApproval(manager, created.id(), new RefundFirstApprovalRequest(created.version(), BigInteger.valueOf(10_000)));
		assertDisplay(approved, source, code, "Renamed Refund Member", "actual-refund-cash-reference");
		assertThat(approved.version()).isEqualTo(1);
		RefundRequestResponse disbursed = refunds.cashDisbursement(receptionist, created.id(), new RefundCashDisbursementRequest(approved.version(), "voucher"));
		assertDisplay(disbursed, source, code, "Renamed Refund Member", "actual-refund-cash-reference");
		assertThat(disbursed.version()).isEqualTo(2);
		RefundRequestResponse completed = refunds.finalApproval(manager, created.id(), new RefundActionRequest(disbursed.version(), null));
		assertDisplay(completed, source, code, "Renamed Refund Member", "actual-refund-cash-reference");
		assertThat(completed.version()).isEqualTo(3);

		SourceFixture rejectedSource = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(), today.minusDays(50));
		RefundRequestResponse pending = refunds.create(receptionist, code, new RefundRequestCreateRequest(rejectedSource.paymentId(),
			rejectedSource.membershipId(), BigInteger.ONE, "reject display", UUID.randomUUID().toString()));
		RefundRequestResponse rejected = refunds.reject(manager, pending.id(), new RefundActionRequest(pending.version(), "declined"));
		assertDisplay(rejected, rejectedSource, code, "Renamed Refund Member", null);
		assertThat(rejected.version()).isEqualTo(1);
		jdbc.update("update accounts set status='INACTIVE' where id=?", member);
		for (boolean managerPage : List.of(true, false)) {
			org.mockito.Mockito.clearInvocations(jdbc);
			RefundRequestPageResponse page = managerPage ? refunds.listForManager(manager, 0, 100)
				: refunds.listForReceptionist(receptionist, 0, 100);
			org.mockito.Mockito.verify(jdbc).queryForList(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object[].class));
			assertThat(page.content()).filteredOn(row -> row.id().equals(created.id())).singleElement()
				.satisfies(row -> assertDisplay(row, source, code, "Renamed Refund Member", "actual-refund-cash-reference"));
			assertThat(page.content()).filteredOn(row -> row.id().equals(rejected.id())).singleElement()
				.satisfies(row -> assertDisplay(row, rejectedSource, code, "Renamed Refund Member", null));
		}
	}

	@Test
	void refundDisplayKeepsLegacyMissingProfileAndStablePaginationWithOneJdbcBatchPerPage() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		// Legacy accounts can lack a profile; the refund must still be returned.
		SourceFixture source = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH", Instant.now(),
			LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(50));
		refund(source, receptionist, "REQUESTED");
		UUID id = jdbc.queryForObject("select id from refund_requests where payment_id=?", UUID.class, source.paymentId());
		jdbc.update("update refund_requests set requested_at=timestamp with time zone '2099-01-01 00:00:00+00' where id=?", id);
		org.mockito.Mockito.clearInvocations(jdbc);
		RefundRequestPageResponse first = refunds.listForManager(manager, 0, 1);
		org.mockito.Mockito.verify(jdbc).queryForList(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object[].class));
		assertThat(first.content()).singleElement().satisfies(row -> {
			assertThat(row.id()).isEqualTo(id);
			assertDisplay(row, source, null, "Refund MEMBER", null);
		});
		RefundRequestResponse rejected = refunds.reject(manager, id, new RefundActionRequest(0L, "legacy"));
		assertDisplay(rejected, source, null, "Refund MEMBER", null);
		org.mockito.Mockito.clearInvocations(jdbc);
		RefundRequestPageResponse reception = refunds.listForReceptionist(receptionist, 0, 1);
		org.mockito.Mockito.verify(jdbc).queryForList(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Object[].class));
		assertThat(reception.content()).singleElement().satisfies(row -> {
			assertThat(row).usingRecursiveComparison().ignoringFields("rejectedAt").isEqualTo(rejected);
			// PostgreSQL stores microseconds; the mutation response can still contain nanoseconds.
			assertThat(row.rejectedAt()).isCloseTo(rejected.rejectedAt(), within(1, ChronoUnit.MICROS));
		});
		assertThat(reception.totalElements()).isEqualTo(first.totalElements());
		assertThat(reception.totalPages()).isEqualTo(first.totalPages());
		assertThat(refunds.listForManager(manager, 1, 1).content()).noneMatch(row -> row.id().equals(id));
	}

	private void assertDisplay(RefundRequestResponse response, SourceFixture source, String code, String name, String reference) {
		assertThat(response).extracting("memberCode", "memberFullName", "orderNumber", "paymentReference", "receiptNumber")
			.containsExactly(code, name, "RFS-" + source.orderId(), reference, "RCS-" + source.receiptId());
		assertThat(response.paymentId()).isEqualTo(source.paymentId());
		assertThat(response.orderId()).isEqualTo(source.orderId());
		assertThat(response.membershipId()).isEqualTo(source.membershipId());
		assertThat(response.receiptId()).isEqualTo(source.receiptId());
	}

	@Test
	void discoveryFiltersIneligibleSourcesAndOrdersByPaidAtThenPaymentId() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		Instant newest = Instant.now().minusSeconds(60);
		LocalDate exactThreshold = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(70);
		SourceFixture newestEligible = source(UUID.fromString("00000000-0000-0000-0000-000000000003"),
			member, manager, "TERMINATED", "CASH", newest, exactThreshold);
		SourceFixture rejectedEligible = source(UUID.fromString("00000000-0000-0000-0000-000000000002"),
			member, manager, "TERMINATED", "CASH", newest.minusSeconds(60), exactThreshold);
		SourceFixture cancelledEligible = source(UUID.fromString("00000000-0000-0000-0000-000000000001"),
			member, manager, "TERMINATED", "CASH", newest.minusSeconds(60), exactThreshold);
		SourceFixture reservedEligible = source(UUID.randomUUID(), member, manager, "RESERVED", "CASH",
			newest.minusSeconds(120), null);
		refund(rejectedEligible, receptionist, "REJECTED");
		refund(cancelledEligible, receptionist, "CANCELLED");
		for (String status : List.of("REQUESTED", "FIRST_APPROVED", "CASH_DISBURSED", "COMPLETED")) {
			SourceFixture blocked = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH",
				newest.minusSeconds(180), exactThreshold);
			refund(blocked, receptionist, status);
		}
		source(UUID.randomUUID(), member, manager, "TERMINATED", "BANK_TRANSFER",
			newest.minusSeconds(180), exactThreshold);
		source(UUID.randomUUID(), member, manager, "ACTIVE", "CASH",
			newest.minusSeconds(180), exactThreshold);
		source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH",
			newest.minusSeconds(180), exactThreshold.minusDays(1));
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?",
			String.class, member);

		List<RefundEligibleSourceResponse> response = refunds.refundEligibleSources(receptionist, memberCode);

		assertThat(response).extracting(RefundEligibleSourceResponse::paymentId).containsExactly(
			newestEligible.paymentId(), rejectedEligible.paymentId(), cancelledEligible.paymentId(),
			reservedEligible.paymentId());
		assertThat(response).extracting(RefundEligibleSourceResponse::type).containsExactly(
			RefundType.TERMINATED_MEMBERSHIP, RefundType.TERMINATED_MEMBERSHIP,
			RefundType.TERMINATED_MEMBERSHIP, RefundType.RESERVED_CANCELLATION);
	}

	@Test
	void freezeCreditsUseExactThresholdAndStayConsistentThroughApproval() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
		SourceFixture source = source(UUID.randomUUID(), member, manager, "TERMINATED", "CASH",
			Instant.now(), today.minusDays(71));
		freezePeriod(source, member, "COMPLETED", "COMPLETED", 1, today.minusDays(30));
		freezePeriod(source, member, "ACTIVE", "ACTIVE", 40, today.minusDays(20));
		freezePeriod(source, member, "OBSOLETE", "OBSOLETE", 40, today.minusDays(10));
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?",
			String.class, member);

		RefundEligibleSourceResponse exactThreshold = refunds.refundEligibleSources(receptionist, memberCode)
			.getFirst();

		assertThat(exactThreshold.paymentId()).isEqualTo(source.paymentId());
		assertThat(exactThreshold.suggestedAmount()).isEqualTo(BigInteger.valueOf(15_000));

		freezePeriod(source, member, "COMPLETED", "ENDED_EARLY", 1, today.minusDays(5));
		RefundEligibleSourceResponse bothAcceptedStatuses = refunds.refundEligibleSources(receptionist, memberCode)
			.getFirst();
		assertThat(bothAcceptedStatuses.suggestedAmount()).isEqualTo(BigInteger.valueOf(15_500));

		RefundRequestResponse created = refunds.create(receptionist, memberCode,
			new RefundRequestCreateRequest(source.paymentId(), source.membershipId(), BigInteger.valueOf(15_500),
				"freeze credit quote", "freeze-credit-quote"));
		assertThat(created.suggestedAmount()).isEqualTo(bothAcceptedStatuses.suggestedAmount());
		assertThat(created.remainingDaysSnapshot()).isEqualTo(31);

		RefundRequestResponse approved = refunds.firstApproval(manager, created.id(),
			new RefundFirstApprovalRequest(created.version(), BigInteger.valueOf(15_500)));
		assertThat(approved.suggestedAmount()).isEqualTo(bothAcceptedStatuses.suggestedAmount());
		assertThat(approved.remainingDaysSnapshot()).isEqualTo(31);
	}

	@Test
	void mutationAfterDiscoveryIsRecheckedByCreate() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		SourceFixture source = source(UUID.randomUUID(), member, manager, "RESERVED", "CASH",
			Instant.now(), null);
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?",
			String.class, member);
		assertThat(refunds.refundEligibleSources(receptionist, memberCode))
			.extracting(RefundEligibleSourceResponse::paymentId).containsExactly(source.paymentId());
		jdbc.update("update memberships set status='CANCELLED' where id=?", source.membershipId());

		assertThatThrownBy(() -> refunds.create(receptionist, memberCode,
			new RefundRequestCreateRequest(source.paymentId(), source.membershipId(), BigInteger.ONE,
				"membership changed", "race-key")))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_NOT_ELIGIBLE");
		assertThat(jdbc.queryForObject("select count(*) from refund_requests where payment_id=?", Long.class,
			source.paymentId())).isZero();
	}

	@Test
	void duplicateRefundAfterDiscoveryIsRejectedByTheDatabaseConstraint() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		SourceFixture source = source(UUID.randomUUID(), member, manager, "RESERVED", "CASH",
			Instant.now(), null);
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?",
			String.class, member);
		assertThat(refunds.refundEligibleSources(receptionist, memberCode))
			.extracting(RefundEligibleSourceResponse::paymentId).containsExactly(source.paymentId());
		refunds.create(receptionist, memberCode, new RefundRequestCreateRequest(source.paymentId(),
			source.membershipId(), BigInteger.valueOf(80_000), "first", "duplicate-key-1"));

		assertThatThrownBy(() -> refunds.create(receptionist, memberCode,
			new RefundRequestCreateRequest(source.paymentId(), source.membershipId(), BigInteger.valueOf(80_000),
				"second", "duplicate-key-2")))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_ALREADY_EXISTS");
		assertThat(jdbc.queryForObject("select count(*) from refund_requests where payment_id=?", Long.class,
			source.paymentId())).isOne();
	}

	@Test
	void concurrentRefundAttemptsForOneDiscoveredSourcePersistExactlyOneRequest() throws Exception {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		SourceFixture source = source(UUID.randomUUID(), member, manager, "RESERVED", "CASH",
			Instant.now(), null);
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?",
			String.class, member);
		assertThat(refunds.refundEligibleSources(receptionist, memberCode))
			.extracting(RefundEligibleSourceResponse::paymentId).containsExactly(source.paymentId());
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		var executor = Executors.newFixedThreadPool(2);
		try {
			var first = executor.submit(() -> concurrentCreate(ready, start, receptionist, memberCode, source,
				"concurrent-key-1"));
			var second = executor.submit(() -> concurrentCreate(ready, start, receptionist, memberCode, source,
				"concurrent-key-2"));
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			List<Object> results = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));

			assertThat(results).filteredOn(RefundRequestResponse.class::isInstance).hasSize(1);
			assertThat(results).filteredOn(RefundException.class::isInstance).singleElement().satisfies(result ->
				assertThat(((RefundException) result).getCode()).isEqualTo("REFUND_ALREADY_EXISTS"));
			assertThat(jdbc.queryForObject("select count(*) from refund_requests where payment_id=?", Long.class,
				source.paymentId())).isOne();
		}
		finally {
			start.countDown();
			executor.shutdownNow();
		}
	}

	@Test
	void refundLedgerMigrationInstallsExpectedTablesIndexesAndImmutableCreditNoteGuard() {
		assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name in ('refund_requests','credit_notes')", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select pg_get_expr(i.indpred, i.indrelid) from pg_index i join pg_class c on c.oid=i.indexrelid where c.relname='uq_refund_requests_nonrejected_payment'", String.class))
			.contains("REJECTED", "CANCELLED").doesNotContain("COMPLETED");
		assertThat(jdbc.queryForObject("select exists(select 1 from pg_trigger where tgname='trg_credit_notes_immutable' and not tgisinternal)", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select version from flyway_schema_history where version='39' and success", String.class)).isEqualTo("39");
	}

	@Test
	void finalApprovalWritesOneCreditNoteAndOneEventAndRetryCannotDuplicateThem() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		UUID receipt = UUID.randomUUID();
		UUID refund = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'BASIC','Refund test','Refund test',100000,'VND',30,'ACTIVE',?)",
			offer, manager);
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at,fulfillment_mode) values(?,?,?, ?,?,'Refund test','BASIC',100000,'VND',30,'CASH','PAID',?,'IMMEDIATE')",
			order, "RF-" + order, member, manager, offer, Timestamp.from(now));
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,paid_at) values(?,?,'CASH','PAID',100000,'VND',?)",
			payment, order, Timestamp.from(now));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,'BASIC','Refund test',100000,'VND',30,'TERMINATED',?,?)",
			membership, member, order, offer, Timestamp.from(now.minusSeconds(30L * 86400)), Timestamp.from(now.minusSeconds(86400)));
		jdbc.update("insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot) values(?,?,?,?,?,100000,'VND','CASH')",
			receipt, "RC-" + receipt, payment, order, member);
		jdbc.update("insert into refund_requests(id,payment_id,order_id,membership_id,receipt_id,member_account_id,requester_account_id,refund_type,status,requested_amount,suggested_amount,approved_amount,remaining_days_snapshot,calculation_date,reason,disbursement_evidence,idempotency_key,cash_disbursed_at) values(?,?,?,?,?,?,?,'TERMINATED_MEMBERSHIP','CASH_DISBURSED',1000,1000,1000,1,current_date,'test refund','cash receipt','integration-key',?)",
			refund, payment, order, membership, receipt, member, receptionist, Timestamp.from(now));

		refunds.finalApproval(manager, refund, new RefundActionRequest(0L, null));

		assertThat(jdbc.queryForObject("select status from refund_requests where id=?", String.class, refund)).isEqualTo("COMPLETED");
		assertThat(jdbc.queryForObject("select count(*) from credit_notes where refund_request_id=?", Long.class, refund)).isOne();
		assertThat(jdbc.queryForObject("select count(*) from audit_events where target_id=? and action='MEMBERSHIP_REFUND_COMPLETED'", Long.class, refund)).isOne();
		assertThat(jdbc.queryForObject("select count(*) from notification_outbox where event_key=?", Long.class,
			"MEMBERSHIP_REFUND_COMPLETED:" + refund)).isOne();
		assertThatThrownBy(() -> refunds.finalApproval(manager, refund, new RefundActionRequest(0L, null)))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_REQUEST_CHANGED");
		assertThat(jdbc.queryForObject("select count(*) from credit_notes where refund_request_id=?", Long.class, refund)).isOne();
		assertThatThrownBy(() -> jdbc.update("insert into refund_requests(id,payment_id,order_id,membership_id,receipt_id,member_account_id,requester_account_id,refund_type,status,requested_amount,reason,idempotency_key) values(?,?,?,?,?,?,?,'TERMINATED_MEMBERSHIP','REQUESTED',1,'duplicate','second-key')",
			UUID.randomUUID(), payment, order, membership, receipt, member, receptionist))
			.isInstanceOf(DataIntegrityViolationException.class);
		RevenueReportResponse report = payments.report(manager, now.minusSeconds(5), now.plusSeconds(5), false);
		assertThat(report.paidRevenue()).isEqualTo(java.math.BigInteger.valueOf(100_000));
		assertThat(report.completedRefundAmount()).isEqualTo(java.math.BigInteger.valueOf(1_000));
		assertThat(report.netRevenue()).isEqualTo(java.math.BigInteger.valueOf(99_000));
	}

	@Test
	void finalApprovalCancelsReservedMembershipOnlyAfterCashIsRecorded() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		UUID receipt = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'BASIC','Reserved refund','Reserved refund',100000,'VND',30,'ACTIVE',?)", offer, manager);
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at,fulfillment_mode) values(?,?,?, ?,?,'Reserved refund','BASIC',100000,'VND',30,'CASH','PAID',?,'RESERVED')",
			order, "RFR-" + order, member, manager, offer, Timestamp.from(now));
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,paid_at) values(?,?,'CASH','PAID',100000,'VND',?)", payment, order, Timestamp.from(now));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,'BASIC','Reserved refund',100000,'VND',30,'RESERVED',null,null)", membership, member, order, offer);
		jdbc.update("insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot) values(?,?,?,?,?,100000,'VND','CASH')", receipt, "RC-" + receipt, payment, order, member);
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?", String.class, member);
		RefundRequestResponse created = refunds.create(receptionist, memberCode,
			new RefundRequestCreateRequest(payment, membership, java.math.BigInteger.valueOf(80_000), "Cancel reserved membership", "reserved-refund-key"));
		assertThat(created.type()).isEqualTo(RefundType.RESERVED_CANCELLATION);
		assertThat(created.suggestedAmount()).isEqualTo(java.math.BigInteger.valueOf(80_000));
		assertThat(jdbc.queryForObject("select status from memberships where id=?", String.class, membership)).isEqualTo("RESERVED");
		RefundRequestResponse approved = refunds.firstApproval(manager, created.id(),
			new RefundFirstApprovalRequest(created.version(), java.math.BigInteger.valueOf(80_000)));
		RefundRequestResponse disbursed = refunds.cashDisbursement(receptionist, created.id(),
			new RefundCashDisbursementRequest(approved.version(), "cash voucher"));

		RefundRequestResponse completed = refunds.finalApproval(manager, created.id(), new RefundActionRequest(disbursed.version(), null));

		assertThat(completed.status()).isEqualTo(RefundRequestStatus.COMPLETED);
		assertThat(jdbc.queryForObject("select status from memberships where id=?", String.class, membership)).isEqualTo("CANCELLED");
		assertThat(jdbc.queryForObject("select amount from credit_notes where refund_request_id=?", java.math.BigDecimal.class, created.id()))
			.isEqualByComparingTo("80000");
	}

	@Test
	void rejectingReservedRefundAfterImmediateTerminationActivatesMembershipAtRejectionTime() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		UUID offer = UUID.randomUUID();
		UUID reservedOrder = UUID.randomUUID();
		UUID activeOrder = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID reservedId = UUID.randomUUID();
		UUID activeId = UUID.randomUUID();
		UUID receipt = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'BASIC','Deferred activation','Deferred activation',100000,'VND',30,'ACTIVE',?)", offer, manager);
		insertPaidOrder(reservedOrder, member, manager, offer, payment, now, "RESERVED");
		insertPaidOrder(activeOrder, member, manager, offer, UUID.randomUUID(), now, "IMMEDIATE");
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,'BASIC','Deferred activation',100000,'VND',30,'RESERVED',null,null)", reservedId, member, reservedOrder, offer);
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,'BASIC','Deferred activation',100000,'VND',30,'ACTIVE',?,?)", activeId, member, activeOrder, offer, Timestamp.from(now.minusSeconds(86400)), Timestamp.from(now.plusSeconds(86400)));
		jdbc.update("insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot) values(?,?,?,?,?,100000,'VND','CASH')", receipt, "RC-" + receipt, payment, reservedOrder, member);
		String memberCode = jdbc.queryForObject("select member_code from member_profiles where account_id=?", String.class, member);
		RefundRequestResponse created = refunds.create(receptionist, memberCode,
			new RefundRequestCreateRequest(payment, reservedId, java.math.BigInteger.valueOf(80_000), "Cancel reserved", "deferred-refund-key"));
		lifecycle.terminateActiveAndActivateReserved(member, now);
		assertThat(jdbc.queryForObject("select status from memberships where id=?", String.class, reservedId)).isEqualTo("RESERVED");

		refunds.reject(manager, created.id(), new RefundActionRequest(created.version(), "not eligible"));

		assertThat(jdbc.queryForObject("select status from refund_requests where id=?", String.class, created.id())).isEqualTo("REJECTED");
		assertThat(jdbc.queryForObject("select status from memberships where id=?", String.class, reservedId)).isEqualTo("ACTIVE");
		Instant startsAt = jdbc.queryForObject("select starts_at from memberships where id=?", Timestamp.class, reservedId).toInstant();
		assertThat(startsAt).isAfterOrEqualTo(now);
		assertThat(startsAt).isBeforeOrEqualTo(Instant.now());
	}

	private void insertPaidOrder(UUID order, UUID member, UUID manager, UUID offer, UUID payment, Instant now, String fulfillment) {
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at,fulfillment_mode) values(?,?,?,?,?,'Deferred activation','BASIC',100000,'VND',30,'CASH','PAID',?,?)",
			order, "RFD-" + order, member, manager, offer, Timestamp.from(now), fulfillment);
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,paid_at) values(?,?,'CASH','PAID',100000,'VND',?)",
			payment, order, Timestamp.from(now));
	}

	private SourceFixture source(UUID payment, UUID member, UUID manager, String membershipStatus,
			String paymentMethod, Instant paidAt, LocalDate startDate) {
		return source(payment, member, manager, membershipStatus, paymentMethod, paidAt, startDate, null, "BASIC");
	}

	private SourceFixture source(UUID payment, UUID member, UUID manager, String membershipStatus,
			String paymentMethod, Instant paidAt, LocalDate startDate, String paymentReference, String planCode) {
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		UUID receipt = UUID.randomUUID();
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,?,'Discovery refund','Discovery refund',100000,'VND',100,'ACTIVE',?)",
			offer, planCode, manager);
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at,fulfillment_mode) values(?,?,?,?,?,'Discovery refund',?,100000,'VND',100,?,'PAID',?,'IMMEDIATE')",
			order, "RFS-" + order, member, manager, offer, planCode, paymentMethod, Timestamp.from(paidAt));
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,bank_transfer_content,paid_at,provider_reference) values(?,?,?,'PAID',100000,'VND',?,?,?)",
			payment, order, paymentMethod,
			"BANK_TRANSFER".equals(paymentMethod) ? "transfer-" + payment : null, Timestamp.from(paidAt), paymentReference);
		Timestamp startsAt = startDate == null ? null : Timestamp.from(startDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
		Timestamp endsAt = startsAt == null ? null : Timestamp.from(startsAt.toInstant().plusSeconds(
			"ACTIVE".equals(membershipStatus) ? 100L * 86400 : 86400));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,?,'Discovery refund',100000,'VND',100,?,?,?)",
			membership, member, order, offer, planCode, membershipStatus, startsAt, endsAt);
		jdbc.update("insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot) values(?,?,?,?,?,100000,'VND',?)",
			receipt, "RCS-" + receipt, payment, order, member, paymentMethod);
		return new SourceFixture(payment, order, membership, receipt);
	}

	private void freezePeriod(SourceFixture source, UUID member, String requestStatus, String periodStatus,
			int creditedDays, LocalDate scheduledStart) {
		UUID request = UUID.randomUUID();
		LocalDate scheduledEnd = scheduledStart.plusDays(2);
		jdbc.update("insert into membership_freeze_requests(id,membership_id,member_account_id,requester_account_id,start_date,end_date,reason,status) values(?,?,?,?,?,?,?,?)",
			request, source.membershipId(), member, member, scheduledStart, scheduledEnd,
			"refund quote aggregation", requestStatus);
		LocalDate actualStart = "OBSOLETE".equals(periodStatus) ? null : scheduledStart;
		LocalDate actualEnd = switch (periodStatus) {
			case "COMPLETED" -> scheduledEnd;
			case "ENDED_EARLY" -> scheduledStart.plusDays(1);
			default -> null;
		};
		jdbc.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,actual_start_date,actual_end_date,credited_days,status) values(?,?,?,?,?,?,?,?,?)",
			UUID.randomUUID(), request, source.membershipId(), scheduledStart, scheduledEnd, actualStart, actualEnd,
			creditedDays, periodStatus);
	}

	private void refund(SourceFixture source, UUID receptionist, String status) {
		jdbc.update("insert into refund_requests(id,payment_id,order_id,membership_id,receipt_id,member_account_id,requester_account_id,refund_type,status,requested_amount,suggested_amount,reason,idempotency_key) select ?,?,?,?, ?,m.member_account_id,?,'TERMINATED_MEMBERSHIP',?,1,1,'existing refund',? from memberships m where m.id=?",
			UUID.randomUUID(), source.paymentId(), source.orderId(), source.membershipId(), source.receiptId(),
			receptionist, status, UUID.randomUUID().toString(), source.membershipId());
	}

	private Object concurrentCreate(CountDownLatch ready, CountDownLatch start, UUID receptionist,
			String memberCode, SourceFixture source, String idempotencyKey) throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			return refunds.create(receptionist, memberCode, new RefundRequestCreateRequest(source.paymentId(),
				source.membershipId(), BigInteger.valueOf(80_000), "concurrent refund", idempotencyKey));
		}
		catch (RefundException exception) {
			return exception;
		}
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE',?,?,?,date '1990-01-01','test')",
			id, role, "Refund " + role, "09" + id.toString().replace("-", "").substring(0, 9), id + "@refund.test");
		return id;
	}

	private record SourceFixture(UUID paymentId, UUID orderId, UUID membershipId, UUID receiptId) { }
}
