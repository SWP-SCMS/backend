package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.audit.AuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CenterVisitServiceTests {

	@Mock ReceptionMemberProfileService members;
	@Mock CenterVisitRepository visits;
	@Mock AccountRepository accounts;
	@Mock MemberProfileRepository profiles;
	@Mock AuditEventRepository audits;
	CenterVisitService service;

	private final Instant now = Instant.parse("2026-10-06T05:00:00Z");
	private UUID receptionistId;
	private UUID memberId;

	@BeforeEach
	void setUp() {
		service = new CenterVisitService(members, visits, accounts, profiles, audits,
			Clock.fixed(now, ZoneOffset.UTC));
		receptionistId = UUID.randomUUID();
		memberId = UUID.randomUUID();
	}

	@Test
	void identityVerificationIsRequired() {
		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, null), null), "IDENTITY_VERIFICATION_REQUIRED");
		verify(visits, never()).findCurrentMembership(any(), any());
	}

	@Test
	void createsVisitAndMarksResponseCreated() {
		UUID membershipId = UUID.randomUUID();
		stubEligibleMember(membershipId);
		when(visits.saveAndFlush(any(CenterVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CenterVisitResponse response = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), " request-1 ");

		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.membershipId()).isEqualTo(membershipId);
		assertThat(response.checkedInBy()).isEqualTo(receptionistId);
		assertThat(response.checkedInAt()).isEqualTo(now);
		assertThat(response.created()).isTrue();
		ArgumentCaptor<CenterVisit> saved = ArgumentCaptor.forClass(CenterVisit.class);
		verify(visits).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getIdempotencyKey()).isEqualTo("request-1");
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("CENTER_VISIT_CREATED");
		assertThat(audit.getValue().getAfterData()).containsEntry("identityVerified", true)
			.containsEntry("memberId", memberId.toString())
			.containsEntry("membershipId", membershipId.toString());
	}

	@Test
	void sameIdempotencyKeyAndMemberReplaysWithoutSaving() {
		CenterVisit existing = visit(now.minusSeconds(30), "request-1");
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1"))
			.thenReturn(Optional.of(existing));

		CenterVisitResponse response = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), "request-1");

		assertThat(response.id()).isEqualTo(existing.getId());
		assertThat(response.checkedInAt()).isEqualTo(now.minusSeconds(30));
		assertThat(response.created()).isFalse();
		verify(visits, never()).saveAndFlush(any());
	}

	@Test
	void keyedRequestThatMissesBeforeLockReplaysWinnerAfterLock() {
		CenterVisit existing = visit(now.minusSeconds(30), "request-1");
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount(AccountStatus.SUSPENDED)));
		when(visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1"))
			.thenReturn(Optional.empty(), Optional.of(existing));

		CenterVisitResponse response = assertDoesNotThrow(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), "request-1"));

		assertThat(response.id()).isEqualTo(existing.getId());
		assertThat(response.checkedInAt()).isEqualTo(now.minusSeconds(30));
		assertThat(response.created()).isFalse();
		var order = inOrder(visits, accounts);
		order.verify(visits).findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1");
		order.verify(accounts).findByIdForUpdate(memberId);
		order.verify(visits).findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1");
		verify(visits, never()).findCurrentMembership(any(), any());
		verify(visits, never())
			.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(any());
		verify(visits, never()).saveAndFlush(any());
		verify(audits, never()).save(any());
	}

	@Test
	void keyedRequestThatMissesBeforeLockRejectsWinnerForDifferentMember() {
		CenterVisit existing = new CenterVisit(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			receptionistId, now.minusSeconds(30), "request-1");
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1"))
			.thenReturn(Optional.empty(), Optional.of(existing));

		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), "request-1"), "IDEMPOTENCY_KEY_REUSED");
		verify(visits, never()).findCurrentMembership(any(), any());
		verify(visits, never())
			.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(any());
		verify(visits, never()).saveAndFlush(any());
		verify(audits, never()).save(any());
	}

	@Test
	void keyedReplaySurvivesMemberSuspensionAndRemovedImage() {
		CenterVisit existing = visit(now.minusSeconds(30), "request-1");
		when(members.search(receptionistId, "MB-100001", null))
			.thenReturn(member(AccountStatus.SUSPENDED, "  "));
		when(visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1"))
			.thenReturn(Optional.of(existing));

		CenterVisitResponse response = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), "request-1");

		assertThat(response.id()).isEqualTo(existing.getId());
		assertThat(response.checkedInAt()).isEqualTo(now.minusSeconds(30));
		assertThat(response.created()).isFalse();
		verify(accounts, never()).findByIdForUpdate(any());
		verify(visits, never()).findCurrentMembership(any(), any());
	}

	@Test
	void sameIdempotencyKeyForAnotherMemberIsRejected() {
		CenterVisit existing = new CenterVisit(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
			receptionistId, now.minusSeconds(30), "request-1");
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, "request-1"))
			.thenReturn(Optional.of(existing));

		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), "request-1"), "IDEMPOTENCY_KEY_REUSED");
		verify(visits, never()).saveAndFlush(any());
	}

	@Test
	void newEntryClosesOpenVisitAtNewCheckInTime() {
		UUID membershipId = UUID.randomUUID();
		CenterVisit open = visit(now.minusSeconds(300), null);
		stubEligibleMember(membershipId);
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.of(open));
		when(visits.saveAndFlush(any(CenterVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.checkIn(receptionistId, new CenterVisitRequest("MB-100001", null, true), "request-2");

		assertThat(open.getCheckedOutAt()).isEqualTo(now);
		assertThat(open.getCheckedOutByAccountId()).isEqualTo(receptionistId);
		assertThat(open.getCheckoutSource()).isEqualTo(CheckoutSource.AUTO_REENTRY);
		var order = inOrder(visits);
		order.verify(visits).flush();
		order.verify(visits).saveAndFlush(any(CenterVisit.class));
	}

	@Test
	void memberCheckoutReplayKeepsOriginalTimestamp() {
		CenterVisit closed = visit(now.minusSeconds(300), null);
		closed.checkout(memberId, CheckoutSource.MEMBER, now.minusSeconds(60));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(profiles.findById(memberId)).thenReturn(Optional.of(profile()));
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.empty());
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNotNullOrderByCheckedOutAtDescIdDesc(memberId))
			.thenReturn(Optional.of(closed));

		CenterVisitResponse response = service.checkoutMember(memberId);

		assertThat(response.checkedOutAt()).isEqualTo(now.minusSeconds(60));
		verify(visits, never()).save(any());
		verify(audits, never()).save(any());
	}

	@Test
	void memberCheckoutRecordsActorSourceAndBeforeAfterAudit() {
		CenterVisit open = visit(now.minusSeconds(300), null);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(profiles.findById(memberId)).thenReturn(Optional.of(profile()));
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.of(open));

		CenterVisitResponse response = service.checkoutMember(memberId);

		assertThat(response.checkedOutAt()).isEqualTo(now);
		assertThat(response.checkoutSource()).isEqualTo(CheckoutSource.MEMBER);
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("CENTER_VISIT_CHECKED_OUT");
		assertThat(audit.getValue().getActorAccountId()).isEqualTo(memberId);
		assertThat(audit.getValue().getBeforeData()).containsEntry("checkedOut", false);
		assertThat(audit.getValue().getAfterData()).containsEntry("checkoutSource", "MEMBER")
			.containsEntry("checkedOutBy", memberId.toString());
	}

	@Test
	void suspendedMemberCannotCheckIn() {
		when(members.search(receptionistId, null, "0901000001"))
			.thenReturn(member(AccountStatus.SUSPENDED, "photo"));

		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest(null, "0901000001", true), null), "CENTER_VISIT_MEMBER_NOT_ACTIVE");
		verify(visits, never()).findCurrentMembership(any(), any());
	}

	@Test
	void profileImageIsRequired() {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "  "));

		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), null), "CENTER_VISIT_PROFILE_IMAGE_REQUIRED");
		verify(visits, never()).findCurrentMembership(any(), any());
	}

	@Test
	void currentBasicOrPlusMembershipIsRequired() {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.findCurrentMembership(memberId, now)).thenReturn(Optional.empty());

		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), null), "CENTER_VISIT_MEMBERSHIP_REQUIRED");
		verify(visits, never()).saveAndFlush(any());
	}

	private void stubEligibleMember(UUID membershipId) {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.findCurrentMembership(memberId, now)).thenReturn(Optional.of(membershipId));
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.empty());
	}

	private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code) {
		assertThatThrownBy(action).isInstanceOf(CenterVisitException.class).extracting("code").isEqualTo(code);
	}

	private ReceptionMemberSearchResponse member(AccountStatus status, String image) {
		return new ReceptionMemberSearchResponse(memberId, "MB-100001", "Member", "0901000001",
			"member@example.test", LocalDate.of(1990, 1, 1), image, status);
	}

	private Account memberAccount() {
		return memberAccount(AccountStatus.ACTIVE);
	}

	private Account memberAccount(AccountStatus status) {
		return new Account(memberId, AccountRole.MEMBER, status, "Member", "0901000001",
			"member@example.test", LocalDate.of(1990, 1, 1), "password");
	}

	private MemberProfile profile() {
		return new MemberProfile(memberAccount(), "photo", null);
	}

	private CenterVisit visit(Instant checkedInAt, String key) {
		return new CenterVisit(UUID.randomUUID(), memberId, UUID.randomUUID(), receptionistId, checkedInAt, key);
	}
}
