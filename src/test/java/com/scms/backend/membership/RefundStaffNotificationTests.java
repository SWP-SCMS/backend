package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.*;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

class RefundStaffNotificationTests {

	private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
	private final RefundRequestRepository refunds = mock(RefundRequestRepository.class);
	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final NotificationWriter notifications = mock(NotificationWriter.class);
	private final MembershipRefundService service = new MembershipRefundService(accounts, profiles, refunds,
		mock(MembershipRepository.class), mock(MembershipLifecycleService.class), db,
		mock(AuditEventRepository.class), notifications, Clock.fixed(NOW, ZoneOffset.UTC));
	private final UUID member = UUID.randomUUID();
	private final UUID receptionist = UUID.randomUUID();
	private final UUID manager = UUID.randomUUID();
	private final UUID otherReceptionist = UUID.randomUUID();
	private final UUID otherManager = UUID.randomUUID();
	private final List<Written> written = new ArrayList<>();
	private RefundRequest request = request(receptionist);
	private boolean legacyDisplay;

	@BeforeEach
	void setup() {
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.existsByIdAndRoleAndStatus(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(List.of(account(manager, AccountRole.MANAGER), account(otherManager, AccountRole.MANAGER)));
		when(accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(List.of(account(receptionist, AccountRole.RECEPTIONIST), account(otherReceptionist, AccountRole.RECEPTIONIST)));
		when(refunds.findById(any())).thenAnswer(invocation -> Optional.of(request));
		when(refunds.findByIdForUpdate(any())).thenAnswer(invocation -> Optional.of(request));
		when(refunds.saveAndFlush(any())).thenAnswer(invocation -> {
			request = invocation.getArgument(0);
			return request;
		});
		when(db.queryForMap(contains("select m.starts_at"), any())).thenReturn(Map.of(
			"membership_status", "TERMINATED", "starts_at", Timestamp.from(NOW.minusSeconds(50L * 86400)),
			"duration_days_snapshot", 100, "credited_days", 0, "price_amount_snapshot", BigDecimal.valueOf(100_000)));
		when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
			Map<String, Object> row = new HashMap<>();
			row.put("id", request.getId());
			row.put("member_code", legacyDisplay ? null : "MB-123456");
			row.put("member_full_name", legacyDisplay ? null : "Current Member");
			return List.of(row);
		});
		doAnswer(invocation -> {
			written.add(new Written(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2),
				invocation.getArgument(3), invocation.getArgument(4), invocation.getArgument(5)));
			return null;
		}).when(notifications).write(anyString(), any(), anyString(), anyString(), any(), any());
	}

	@ParameterizedTest
	@CsvSource({"REQUESTED,MANAGER", "FIRST_APPROVED,RECEPTIONIST", "CASH_DISBURSED,MANAGER",
		"COMPLETED,REQUESTER", "REJECTED,REQUESTER"})
	void everyTransitionKeepsMemberEventAndRoutesSafePayloadToItsStaffRecipients(String action, String audience) {
		RefundRequestResponse response = transition(action);
		String type = "MEMBERSHIP_REFUND_" + action;
		List<UUID> recipients = switch (audience) {
			case "MANAGER" -> List.of(manager, otherManager);
			case "RECEPTIONIST" -> List.of(receptionist, otherReceptionist);
			default -> List.of(receptionist);
		};
		Map<String, Object> memberPayload = switch (action) {
			case "REQUESTED" -> Map.of("requestedAmount", "100");
			case "FIRST_APPROVED" -> Map.of("approvedAmount", "100", "suggestedAmount", "25000");
			case "CASH_DISBURSED" -> Map.of("approvedAmount", "100");
			case "COMPLETED" -> Map.of("amount", "100");
			default -> Map.of("reason", "private rejection reason");
		};
		assertThat(written).hasSize(recipients.size() + 1).contains(new Written(type + ":" + request.getId(),
			member, type, "REFUND_REQUEST", request.getId(), memberPayload));
		Map<String, Object> staffPayload = new HashMap<>(Map.of("refundRequestId", request.getId().toString(),
			"memberCode", "MB-123456", "memberFullName", "Current Member", "status", action));
		if (!List.of("REQUESTED", "REJECTED").contains(action)) staffPayload.put("approvedAmount", "100");
		for (UUID recipient : recipients) {
			assertThat(written).contains(new Written(type + ":" + request.getId() + ":" + recipient, recipient,
				type, "REFUND_REQUEST", request.getId(), staffPayload));
		}
		assertThat(response.memberCode()).isEqualTo("MB-123456");
		verify(db).queryForList(anyString(), any(Object[].class));
		if (!audience.equals("REQUESTER")) {
			verify(accounts).findAllByRoleAndStatusOrderByIdAsc(AccountRole.valueOf(audience), AccountStatus.ACTIVE);
			verify(accounts, never()).findById(any());
		}
	}

	@ParameterizedTest
	@CsvSource({"COMPLETED,false", "REJECTED,false", "COMPLETED,true", "REJECTED,true"})
	void terminalEventsRequireRequesterStillActiveReceptionist(String action, boolean eligible) {
		when(accounts.existsByIdAndRoleAndStatus(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(eligible);
		transition(action);
		assertThat(written).extracting(Written::recipient).containsExactlyElementsOf(
			eligible ? List.of(member, receptionist) : List.of(member));
		verify(accounts, never()).findAllByRoleAndStatusOrderByIdAsc(any(), any());
	}

	@ParameterizedTest
	@ValueSource(strings = {"COMPLETED", "REJECTED"})
	void terminalEventDoesNotNotifyRequesterWhoIsNowTheActingManager(String action) {
		request = request(manager);
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(true);
		transition(action);
		assertThat(written).extracting(Written::recipient).containsExactly(member);
	}

	@Test
	void roleBroadcastSuppressesActorEvenIfReturnedByRecipientLookup() {
		when(accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(List.of(account(manager, AccountRole.RECEPTIONIST), account(otherReceptionist, AccountRole.RECEPTIONIST)));
		transition("FIRST_APPROVED");
		assertThat(written).extracting(Written::recipient).containsExactly(member, otherReceptionist);
	}

	@Test
	void rejectionAfterFirstApprovalIncludesApprovedAmountAndNullableLegacyDisplay() {
		legacyDisplay = true;
		request.firstApprove(manager, BigInteger.valueOf(100), BigInteger.valueOf(25_000), 50,
			LocalDate.of(2026, 10, 23), NOW);
		transition("REJECTED");
		assertThat(written).filteredOn(event -> event.recipient().equals(receptionist)).singleElement().satisfies(event -> {
			assertThat(event.payload()).containsEntry("memberCode", null).containsEntry("memberFullName", null)
				.containsEntry("approvedAmount", "100").containsEntry("status", "REJECTED").hasSize(5);
		});
	}

	@Test
	void createReplayDoesNotEnqueueAnotherMemberOrStaffEvent() {
		RefundRequestCreateRequest input = createInput();
		RefundRequestResponse created = service.create(receptionist, "MB-123456", input);
		List<Written> original = List.copyOf(written);
		when(refunds.findByRequesterAccountIdAndIdempotencyKey(receptionist, input.idempotencyKey()))
			.thenReturn(Optional.of(request));
		assertThat(service.create(receptionist, "MB-123456", input).id()).isEqualTo(created.id());
		assertThat(original).hasSize(3);
		assertThat(written).containsExactlyElementsOf(original);
		verify(refunds).saveAndFlush(any());
		verify(accounts).findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE);
	}

	@ParameterizedTest
	@ValueSource(strings = {"FIRST_APPROVED", "CASH_DISBURSED", "COMPLETED", "REJECTED"})
	void repeatedDecisionDoesNotEnqueueAnotherEvent(String action) {
		transition(action);
		List<Written> original = List.copyOf(written);
		assertThatThrownBy(() -> decide(action)).isInstanceOf(RefundException.class);
		assertThat(written).containsExactlyElementsOf(original);
	}

	private RefundRequestResponse transition(String action) {
		if (action.equals("REQUESTED")) return service.create(receptionist, "MB-123456", createInput());
		if (List.of("CASH_DISBURSED", "COMPLETED").contains(action)) {
			request.firstApprove(manager, BigInteger.valueOf(100), BigInteger.valueOf(25_000), 50,
				LocalDate.of(2026, 10, 23), NOW);
		}
		if (action.equals("COMPLETED")) request.cashDisburse(receptionist, "private cash evidence", NOW);
		return decide(action);
	}

	private RefundRequestResponse decide(String action) {
		return switch (action) {
			case "FIRST_APPROVED" -> service.firstApproval(manager, request.getId(), new RefundFirstApprovalRequest(0L, BigInteger.valueOf(100)));
			case "CASH_DISBURSED" -> service.cashDisbursement(receptionist, request.getId(), new RefundCashDisbursementRequest(0L, "private cash evidence"));
			case "COMPLETED" -> service.finalApproval(manager, request.getId(), new RefundActionRequest(0L, null));
			default -> service.reject(manager, request.getId(), new RefundActionRequest(0L, "private rejection reason"));
		};
	}

	private RefundRequestCreateRequest createInput() {
		when(accounts.findByIdForUpdate(receptionist)).thenReturn(Optional.of(account(receptionist, AccountRole.RECEPTIONIST)));
		when(accounts.findByIdForUpdate(member)).thenReturn(Optional.of(account(member, AccountRole.MEMBER)));
		MemberProfile profile = mock(MemberProfile.class);
		when(profile.getAccountId()).thenReturn(member);
		when(profiles.findVisibleMemberByCode(eq("MB-123456"), eq(AccountRole.MEMBER), any())).thenReturn(Optional.of(profile));
		when(db.queryForMap(contains("select p.id"), any(), any(), any())).thenReturn(Map.of("order_id", request.getOrderId(),
			"method", "CASH", "payment_status", "PAID", "membership_status", "TERMINATED", "receipt_id", request.getReceiptId()));
		return new RefundRequestCreateRequest(request.getPaymentId(), request.getMembershipId(), BigInteger.valueOf(100),
			"private request reason", "staff-replay");
	}

	private RefundRequest request(UUID requester) {
		return new RefundRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), member, requester, RefundType.TERMINATED_MEMBERSHIP, BigInteger.valueOf(100),
			BigInteger.valueOf(25_000), 50, LocalDate.of(2026, 10, 23), "private request reason", "staff-replay");
	}

	private Account account(UUID id, AccountRole role) {
		return new Account(id, role, AccountStatus.ACTIVE, "Staff", "0900000001", id + "@test.local",
			LocalDate.of(1990, 1, 1), "private password hash");
	}

	private record Written(String key, UUID recipient, String type, String targetType, UUID target, Map<String, Object> payload) { }
}
