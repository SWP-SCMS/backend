package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import static org.mockito.Mockito.times;

class MembershipRefundServiceTests {
	private static final Instant NOW = Instant.parse("2026-10-23T00:00:00Z");
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
	private final RefundRequestRepository refunds = mock(RefundRequestRepository.class);
	private final MembershipRepository memberships = mock(MembershipRepository.class);
	private final MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final AuditEventRepository audits = mock(AuditEventRepository.class);
	private final NotificationWriter notifications = mock(NotificationWriter.class);
	private final Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));
	private final MembershipRefundService service = new MembershipRefundService(accounts, profiles, refunds, memberships,
		lifecycle, db, audits, notifications, clock);
	private final UUID manager = UUID.randomUUID();
	private final UUID requestId = UUID.randomUUID();
	private final UUID membershipId = UUID.randomUUID();
	private final UUID receptionist = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();

	@Test
	void receptionistCanCreateRefundOnlyForPaidCashAndTerminatedMembershipAndReplayIdempotently() {
		Account receptionistAccount = new Account(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE,
			"Reception", "0900000001", "reception@example.test", LocalDate.of(1990, 1, 1), "hash");
		MemberProfile profile = mock(MemberProfile.class);
		RefundRequestCreateRequest input = new RefundRequestCreateRequest(UUID.randomUUID(), membershipId,
			BigInteger.valueOf(150_000), "Termination refund", "refund-idem-1");
		when(accounts.findByIdForUpdate(receptionist)).thenReturn(Optional.of(receptionistAccount));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(new Account(memberId, AccountRole.MEMBER,
			AccountStatus.ACTIVE, "Member", "0900000002", "member@example.test", LocalDate.of(1990, 1, 1), "hash")));
		when(profiles.findVisibleMemberByCode(eq("MB-123456"), eq(AccountRole.MEMBER), any())).thenReturn(Optional.of(profile));
		when(profile.getAccountId()).thenReturn(memberId);
		RefundRequest replay = refundForReplay(input);
		when(refunds.findByRequesterAccountIdAndIdempotencyKey(receptionist, "refund-idem-1"))
			.thenReturn(Optional.empty(), Optional.of(replay));
		when(db.queryForMap(contains("select p.id"), eq(input.paymentId()), eq(membershipId), eq(memberId)))
			.thenReturn(Map.of("order_id", UUID.randomUUID(), "method", "CASH", "payment_status", "PAID",
				"membership_status", "TERMINATED", "receipt_id", UUID.randomUUID()));
		when(db.queryForMap(contains("select m.starts_at"), eq(membershipId))).thenReturn(Map.of(
			"starts_at", Timestamp.from(Instant.parse("2026-07-14T17:00:00Z")),
			"duration_days_snapshot", 100, "price_amount_snapshot", BigDecimal.valueOf(1_000_000), "credited_days", 30));
		when(refunds.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.create(receptionist, "MB-123456", input);
		service.create(receptionist, "MB-123456", input);
		assertThatThrownBy(() -> service.create(receptionist, "MB-123456",
			new RefundRequestCreateRequest(input.paymentId(), membershipId, input.requestedAmount(),
				"Different reason", input.idempotencyKey())))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_REQUEST_CHANGED");

		verify(refunds, times(1)).saveAndFlush(any());
		verify(db, times(1)).queryForMap(contains("select p.id"), eq(input.paymentId()), eq(membershipId), eq(memberId));
	}

	@Test
	void firstApprovalCalculatesRefundAtThirtyPercentThresholdAndFloorsVnd() {
		RefundRequest request = requested(BigInteger.valueOf(200_000));
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(refunds.findById(requestId)).thenReturn(Optional.of(request));
		when(refunds.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(db.queryForMap(anyString(), eq(membershipId))).thenReturn(Map.of(
			"starts_at", Timestamp.from(Instant.parse("2026-07-14T17:00:00Z")),
			"duration_days_snapshot", 100, "price_amount_snapshot", BigDecimal.valueOf(1_000_001), "credited_days", 30));

		service.firstApproval(manager, requestId, new RefundFirstApprovalRequest(0L, BigInteger.valueOf(150_000)));

		verify(request).firstApprove(manager, BigInteger.valueOf(150_000), BigInteger.valueOf(150_000),
			30, LocalDate.of(2026, 10, 23), NOW);
		verify(refunds).saveAndFlush(request);
	}

	@Test
	void firstApprovalRejectsOneDayBelowThirtyPercentThreshold() {
		RefundRequest request = requested(BigInteger.valueOf(200_000));
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(refunds.findById(requestId)).thenReturn(Optional.of(request));
		when(refunds.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(db.queryForMap(anyString(), eq(membershipId))).thenReturn(Map.of(
			"starts_at", Timestamp.from(Instant.parse("2026-07-14T17:00:00Z")),
			"duration_days_snapshot", 100, "price_amount_snapshot", BigDecimal.valueOf(1_000_000), "credited_days", 29));

		assertThatThrownBy(() -> service.firstApproval(manager, requestId,
			new RefundFirstApprovalRequest(0L, BigInteger.ONE)))
			.isInstanceOf(RefundException.class).extracting("code")
			.isEqualTo("REFUND_REMAINING_DAYS_BELOW_THRESHOLD");
		verify(refunds, never()).saveAndFlush(any());
	}

	@Test
	void staleApprovalVersionDoesNotReadOrChangeMembership() {
		RefundRequest request = requested(BigInteger.valueOf(200_000));
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(refunds.findById(requestId)).thenReturn(Optional.of(request));
		when(refunds.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));

		assertThatThrownBy(() -> service.firstApproval(manager, requestId,
			new RefundFirstApprovalRequest(1L, BigInteger.ONE)))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_REQUEST_CHANGED");
		verify(db, never()).queryForMap(anyString(), any());
		verify(refunds, never()).saveAndFlush(any());
	}

	@Test
	void duplicateSuccessfulRefundIsMappedToConflict() {
		Account receptionistAccount = new Account(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE,
			"Reception", "0900000001", "reception@example.test", LocalDate.of(1990, 1, 1), "hash");
		MemberProfile profile = mock(MemberProfile.class);
		RefundRequestCreateRequest input = new RefundRequestCreateRequest(UUID.randomUUID(), membershipId,
			BigInteger.valueOf(150_000), "Termination refund", "refund-idem-2");
		when(accounts.findByIdForUpdate(receptionist)).thenReturn(Optional.of(receptionistAccount));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(new Account(memberId, AccountRole.MEMBER,
			AccountStatus.ACTIVE, "Member", "0900000002", "member@example.test", LocalDate.of(1990, 1, 1), "hash")));
		when(profiles.findVisibleMemberByCode(eq("MB-123456"), eq(AccountRole.MEMBER), any())).thenReturn(Optional.of(profile));
		when(profile.getAccountId()).thenReturn(memberId);
		when(db.queryForMap(contains("select p.id"), eq(input.paymentId()), eq(membershipId), eq(memberId)))
			.thenReturn(Map.of("order_id", UUID.randomUUID(), "method", "CASH", "payment_status", "PAID",
				"membership_status", "TERMINATED", "receipt_id", UUID.randomUUID()));
		when(db.queryForMap(contains("select m.starts_at"), eq(membershipId))).thenReturn(Map.of(
			"starts_at", Timestamp.from(Instant.parse("2026-07-14T17:00:00Z")),
			"duration_days_snapshot", 100, "price_amount_snapshot", BigDecimal.valueOf(1_000_000), "credited_days", 30));
		when(refunds.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique refund per payment"));

		assertThatThrownBy(() -> service.create(receptionist, "MB-123456", input))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_ALREADY_EXISTS");
		verify(audits, never()).save(any());
		verify(notifications, never()).write(anyString(), any(), anyString(), anyString(), any(), any());
	}

	@Test
	void receptionistCanRequestEightyPercentRefundForPaidCashReservedMembership() {
		Account receptionistAccount = new Account(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE,
			"Reception", "0900000001", "reception@example.test", LocalDate.of(1990, 1, 1), "hash");
		MemberProfile profile = mock(MemberProfile.class);
		RefundRequestCreateRequest input = new RefundRequestCreateRequest(UUID.randomUUID(), membershipId,
			BigInteger.valueOf(80_000), "Cancel reserved package", "refund-reserved-1");
		when(accounts.findByIdForUpdate(receptionist)).thenReturn(Optional.of(receptionistAccount));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(new Account(memberId, AccountRole.MEMBER,
			AccountStatus.ACTIVE, "Member", "0900000002", "member@example.test", LocalDate.of(1990, 1, 1), "hash")));
		when(profiles.findVisibleMemberByCode(eq("MB-123456"), eq(AccountRole.MEMBER), any())).thenReturn(Optional.of(profile));
		when(profile.getAccountId()).thenReturn(memberId);
		when(refunds.findByRequesterAccountIdAndIdempotencyKey(receptionist, "refund-reserved-1")).thenReturn(Optional.empty());
		when(db.queryForMap(contains("select p.id"), eq(input.paymentId()), eq(membershipId), eq(memberId)))
			.thenReturn(Map.of("order_id", UUID.randomUUID(), "method", "CASH", "payment_status", "PAID",
				"membership_status", "RESERVED", "receipt_id", UUID.randomUUID(), "price_amount_snapshot", BigDecimal.valueOf(100_000)));
		when(refunds.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		RefundRequestResponse response = service.create(receptionist, "MB-123456", input);

		assertThat(response.type()).isEqualTo(RefundType.RESERVED_CANCELLATION);
		assertThat(response.suggestedAmount()).isEqualTo(BigInteger.valueOf(80_000));
		assertThat(response.remainingDaysSnapshot()).isNull();
	}

	@Test
	void rejectingReservedCancellationResumesDeferredActivationAtRejectionTime() {
		RefundRequest request = new RefundRequest(requestId, UUID.randomUUID(), UUID.randomUUID(), membershipId,
			UUID.randomUUID(), memberId, receptionist, RefundType.RESERVED_CANCELLATION,
			BigInteger.valueOf(80_000), BigInteger.valueOf(80_000), null, LocalDate.of(2026, 10, 23), "reason", "key");
		when(accounts.existsByIdAndRoleAndStatus(manager, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(new Account(memberId, AccountRole.MEMBER,
			AccountStatus.ACTIVE, "Member", "0900000002", "member@example.test", LocalDate.of(1990, 1, 1), "hash")));
		when(memberships.findByIdForUpdate(membershipId)).thenReturn(Optional.of(
			new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(), "PLUS", "Plus",
				BigInteger.valueOf(100_000), "VND", 30, MembershipStatus.RESERVED, null, null)));
		when(refunds.findById(requestId)).thenReturn(Optional.of(request));
		when(refunds.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		when(refunds.saveAndFlush(request)).thenReturn(request);

		service.reject(manager, requestId, new RefundActionRequest(0L, "not eligible"));

		verify(lifecycle).activateReservedAfterRefundRejected(memberId, NOW);
	}

	private RefundRequest requested(BigInteger amount) {
		RefundRequest request = mock(RefundRequest.class);
		when(request.getId()).thenReturn(requestId);
		when(request.getMembershipId()).thenReturn(membershipId);
		when(request.getMemberAccountId()).thenReturn(UUID.randomUUID());
		when(request.getVersion()).thenReturn(0L);
		when(request.getStatus()).thenReturn(RefundRequestStatus.REQUESTED);
		when(request.getRequestedAmount()).thenReturn(amount);
		when(request.getApprovedAmount()).thenReturn(BigInteger.valueOf(150_000));
		when(request.getPaymentId()).thenReturn(UUID.randomUUID());
		when(request.getReceiptId()).thenReturn(UUID.randomUUID());
		return request;
	}

	private RefundRequest refundForReplay(RefundRequestCreateRequest input) {
		RefundRequest request = mock(RefundRequest.class);
		when(request.getId()).thenReturn(requestId);
		when(request.getMemberAccountId()).thenReturn(memberId);
		when(request.getPaymentId()).thenReturn(input.paymentId());
		when(request.getMembershipId()).thenReturn(input.membershipId());
		when(request.getRequestedAmount()).thenReturn(input.requestedAmount());
		when(request.getVersion()).thenReturn(0L);
		when(request.getStatus()).thenReturn(RefundRequestStatus.REQUESTED);
		when(request.getType()).thenReturn(RefundType.TERMINATED_MEMBERSHIP);
		when(request.getReason()).thenReturn("Termination refund");
		return request;
	}
}
