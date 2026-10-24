package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

class RefundDisplayServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
	private final RefundRequestRepository refunds = mock(RefundRequestRepository.class);
	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final MembershipRefundService service = new MembershipRefundService(accounts, profiles, refunds,
		mock(MembershipRepository.class), mock(MembershipLifecycleService.class), db,
		mock(AuditEventRepository.class), mock(NotificationWriter.class), Clock.fixed(NOW, ZoneOffset.UTC));
	private final UUID member = UUID.randomUUID();
	private final UUID receptionist = UUID.randomUUID();
	private final UUID manager = UUID.randomUUID();
	private static final String[] DISPLAY_FIELDS = {
		"memberCode", "memberFullName", "orderNumber", "paymentReference", "receiptNumber"
	};

	@Test
	void everyMutationAndReplayUsesCurrentDisplayDataAndPreservesExistingFields() {
		RefundRequestCreateRequest input = new RefundRequestCreateRequest(UUID.randomUUID(), UUID.randomUUID(),
			BigInteger.valueOf(100), "reason", "display-replay");
		when(accounts.findByIdForUpdate(receptionist)).thenReturn(Optional.of(account(receptionist, AccountRole.RECEPTIONIST)));
		when(accounts.findByIdForUpdate(member)).thenReturn(Optional.of(account(member, AccountRole.MEMBER)));
		MemberProfile profile = mock(MemberProfile.class);
		when(profile.getAccountId()).thenReturn(member);
		when(profiles.findVisibleMemberByCode(eq("MB-123456"), eq(AccountRole.MEMBER), any())).thenReturn(Optional.of(profile));
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.existsByIdAndRoleAndStatus(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(true);
		when(db.queryForMap(contains("select p.id"), eq(input.paymentId()), eq(input.membershipId()), eq(member)))
			.thenReturn(Map.of("order_id", UUID.randomUUID(), "method", "CASH", "payment_status", "PAID",
				"membership_status", "TERMINATED", "receipt_id", UUID.randomUUID()));
		when(db.queryForMap(contains("select m.starts_at"), eq(input.membershipId())))
			.thenReturn(Map.of("membership_status", "TERMINATED", "starts_at", Timestamp.from(NOW.minusSeconds(50L * 86400)),
				"duration_days_snapshot", 100, "credited_days", 0, "price_amount_snapshot", BigDecimal.valueOf(100_000)));
		List<RefundRequest> saved = new ArrayList<>();
		when(refunds.saveAndFlush(any())).thenAnswer(invocation -> {
			RefundRequest request = invocation.getArgument(0);
			saved.add(request);
			when(refunds.findById(request.getId())).thenReturn(Optional.of(request));
			when(refunds.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
			when(refunds.findByRequesterAccountIdAndIdempotencyKey(receptionist, input.idempotencyKey()))
				.thenReturn(Optional.of(request));
			return request;
		});
		Map<String, Object> display = display(UUID.randomUUID(), "Current Member", null);
		when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
			display.put("id", saved.getFirst().getId());
			return List.of(display);
		});

		RefundRequestResponse created = service.create(receptionist, "MB-123456", input);
		RefundRequest request = saved.getFirst();
		assertResponse(created, request, "Current Member", null);
		display.put("member_full_name", "Renamed Member");
		display.put("payment_reference", "actual-cash-reference");
		assertResponse(service.create(receptionist, "MB-123456", input), request, "Renamed Member", "actual-cash-reference");
		assertResponse(service.firstApproval(manager, request.getId(), new RefundFirstApprovalRequest(0L, BigInteger.valueOf(100))),
			request, "Renamed Member", "actual-cash-reference");
		assertResponse(service.cashDisbursement(receptionist, request.getId(), new RefundCashDisbursementRequest(0L, "voucher")),
			request, "Renamed Member", "actual-cash-reference");
		assertResponse(service.finalApproval(manager, request.getId(), new RefundActionRequest(0L, null)),
			request, "Renamed Member", "actual-cash-reference");

		RefundRequest rejected = request();
		when(refunds.findById(rejected.getId())).thenReturn(Optional.of(rejected));
		when(refunds.findByIdForUpdate(rejected.getId())).thenReturn(Optional.of(rejected));
		when(db.queryForList(anyString(), any(Object[].class)))
			.thenReturn(List.of(display(rejected.getId(), "Renamed Member", "actual-cash-reference")));
		assertResponse(service.reject(manager, rejected.getId(), new RefundActionRequest(0L, "rejected")),
			rejected, "Renamed Member", "actual-cash-reference");
		verify(db, times(6)).queryForList(anyString(), any(Object[].class));
		verify(db, times(1)).queryForMap(contains("select p.id"), eq(input.paymentId()), eq(input.membershipId()), eq(member));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 20, 100})
	void bothStaffPagesPaginateBeforeOneBatchLookupAndKeepOrderAndLegacyRows(int size) {
		for (AccountRole role : List.of(AccountRole.RECEPTIONIST, AccountRole.MANAGER)) {
			List<RefundRequest> requests = java.util.stream.IntStream.range(0, size).mapToObj(i -> request()).toList();
			PageRequest pageable = PageRequest.of(2, size);
			when(accounts.existsByIdAndRoleAndStatus(manager, role, AccountStatus.ACTIVE)).thenReturn(true);
			when(refunds.findAllByOrderByRequestedAtDescIdDesc(pageable))
				.thenReturn(new PageImpl<>(requests, pageable, size * 4L));
			// The first row has current display data; legacy rows may have no joined data.
			when(db.queryForList(anyString(), any(Object[].class)))
				.thenReturn(List.of(display(requests.getFirst().getId(), "Inactive Member", null)));
			clearInvocations(db, refunds);

			RefundRequestPageResponse page = role == AccountRole.MANAGER
				? service.listForManager(manager, 2, size) : service.listForReceptionist(manager, 2, size);

			assertThat(page.page()).isEqualTo(2);
			assertThat(page.size()).isEqualTo(size);
			assertThat(page.totalElements()).isEqualTo(size * 4L);
			assertThat(page.totalPages()).isEqualTo(4);
			assertThat(page.content()).extracting(RefundRequestResponse::id)
				.containsExactlyElementsOf(requests.stream().map(RefundRequest::getId).toList());
			assertResponse(page.content().getFirst(), requests.getFirst(), "Inactive Member", null);
			for (int i = 1; i < size; i++) {
				assertThat(page.content().get(i)).extracting(DISPLAY_FIELDS).containsOnlyNulls();
			}
			ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
			ArgumentCaptor<Object[]> ids = ArgumentCaptor.forClass(Object[].class);
			var order = inOrder(refunds, db);
			order.verify(refunds).findAllByOrderByRequestedAtDescIdDesc(pageable);
			order.verify(db).queryForList(sql.capture(), ids.capture());
			verifyNoMoreInteractions(db);
			assertThat(ids.getValue()).containsExactlyElementsOf(requests.stream().map(RefundRequest::getId).toList());
			assertThat(sql.getValue()).contains("left join accounts", "left join member_profiles",
				"left join membership_orders", "left join payments", "left join receipts", "p.provider_reference")
				.doesNotContain("ACTIVE", "membership_offers", "coalesce");
		}
	}

	@Test
	void missingLegacyDisplayIsNullOnMutationAndEmptyPagesDoNotQueryDisplay() {
		RefundRequest request = request();
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(refunds.findById(request.getId())).thenReturn(Optional.of(request));
		when(refunds.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
		assertThat(service.reject(manager, request.getId(), new RefundActionRequest(0L, "reason")))
			.extracting(DISPLAY_FIELDS).containsOnlyNulls();
		clearInvocations(db);
		when(refunds.findAllByOrderByRequestedAtDescIdDesc(PageRequest.of(0, 20)))
			.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
		assertThat(service.listForManager(manager, 0, 20).content()).isEmpty();
		verifyNoInteractions(db);
	}

	@Test
	void staleVersionsKeepConflictAndDoNotQueryDisplayForAnyDecision() {
		RefundRequest request = request();
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.existsByIdAndRoleAndStatus(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(true);
		when(refunds.findById(request.getId())).thenReturn(Optional.of(request));
		when(refunds.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
		List<Runnable> actions = List.of(
			() -> service.firstApproval(manager, request.getId(), new RefundFirstApprovalRequest(1L, BigInteger.ONE)),
			() -> service.cashDisbursement(receptionist, request.getId(), new RefundCashDisbursementRequest(1L, "voucher")),
			() -> service.finalApproval(manager, request.getId(), new RefundActionRequest(1L, null)),
			() -> service.reject(manager, request.getId(), new RefundActionRequest(1L, "reason")));
		for (Runnable action : actions) {
			assertThatThrownBy(action::run).isInstanceOf(RefundException.class)
				.extracting("code").isEqualTo("REFUND_REQUEST_CHANGED");
		}
		verifyNoInteractions(db);
		verify(refunds, never()).saveAndFlush(any());
	}

	private RefundRequest request() {
		return new RefundRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			UUID.randomUUID(), member, receptionist, RefundType.TERMINATED_MEMBERSHIP, BigInteger.valueOf(100),
			BigInteger.valueOf(200), 50, LocalDate.of(2026, 10, 23), "reason", UUID.randomUUID().toString());
	}

	private Account account(UUID id, AccountRole role) {
		return new Account(id, role, AccountStatus.ACTIVE, "Member", "0900000001", id + "@test.local",
			LocalDate.of(1990, 1, 1), "hash");
	}

	private Map<String, Object> display(UUID id, String name, String reference) {
		Map<String, Object> row = new HashMap<>();
		row.put("id", id);
		row.put("member_code", "MB-123456");
		row.put("member_full_name", name);
		row.put("order_number", "ORD-123");
		row.put("payment_reference", reference);
		row.put("receipt_number", "RC-123");
		return row;
	}

	private void assertResponse(RefundRequestResponse response, RefundRequest request, String name, String reference) {
		assertThat(response).extracting(DISPLAY_FIELDS).containsExactly("MB-123456", name, "ORD-123", reference, "RC-123");
		assertThat(response).extracting("id", "paymentId", "orderId", "membershipId", "receiptId", "memberAccountId",
			"type", "status", "requestedAmount", "suggestedAmount", "approvedAmount", "remainingDaysSnapshot",
			"calculationDate", "reason", "rejectionReason", "disbursementEvidence", "requestedAt", "firstApprovedAt",
			"cashDisbursedAt", "completedAt", "rejectedAt", "version")
			.containsExactly(request.getId(), request.getPaymentId(), request.getOrderId(), request.getMembershipId(),
				request.getReceiptId(), request.getMemberAccountId(), request.getType(), request.getStatus(),
				request.getRequestedAmount(), request.getSuggestedAmount(), request.getApprovedAmount(), request.getRemainingDaysSnapshot(),
				request.getCalculationDate(), request.getReason(), request.getRejectionReason(), request.getDisbursementEvidence(),
				request.getRequestedAt(), request.getFirstApprovedAt(), request.getCashDisbursedAt(), request.getCompletedAt(),
				request.getRejectedAt(), request.getVersion());
	}
}
