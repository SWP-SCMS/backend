package com.scms.backend.centervisit;

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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.identity.receptionmember.ReceptionMemberProfileService;
import com.scms.backend.identity.receptionmember.ReceptionMemberSearchResponse;
import com.scms.backend.identity.receptionmember.ReceptionActiveMembershipResponse;
import com.scms.backend.membership.MembershipStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
	void createsVisitUsingRevalidatedMembershipAndPreservesResponse() {
		UUID membershipId = UUID.randomUUID();
		stubEligibleMember(membershipId);
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(new ReceptionMemberSearchResponse(
			memberId, "MB-100001", "Member", "0901000001", "member@example.test", LocalDate.of(1990, 1, 1),
			"photo", AccountStatus.ACTIVE, new ReceptionActiveMembershipResponse(UUID.randomUUID(), "Old offer",
				"Old plan", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), MembershipStatus.ACTIVE,
				List.of("CENTER_ACCESS"), true)));
		when(visits.saveAndFlush(any(CenterVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CenterVisitResponse response = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), " request-1 ");

		assertThat(response.memberId()).isEqualTo(memberId);
		assertThat(response.memberName()).isEqualTo("Member");
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
		assertThat(response.memberName()).isEqualTo("Member");
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

		assertThat(response.memberName()).isEqualTo("Member");
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

		assertThat(response.memberName()).isEqualTo("Member");
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
	void currentMemberUsesCurrentAccountName() {
		CenterVisit open = visit(now.minusSeconds(300), null);
		when(accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accounts.findById(memberId)).thenReturn(Optional.of(memberAccount("Renamed Member")));
		when(profiles.findById(memberId)).thenReturn(Optional.of(profile()));
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.of(open));

		CenterVisitResponse response = service.currentMember(memberId);

		assertThat(response.memberName()).isEqualTo("Renamed Member");
	}

	@Test
	void receptionistCheckoutUsesCurrentAccountName() {
		CenterVisit open = visit(now.minusSeconds(300), null);
		when(accounts.existsByIdAndRoleAndStatus(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount("Checkout Member")));
		when(profiles.findById(memberId)).thenReturn(Optional.of(profile()));
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.of(open));

		CenterVisitResponse response = service.checkoutReceptionist(receptionistId, memberId);

		assertThat(response.memberName()).isEqualTo("Checkout Member");
		assertThat(response.checkoutSource()).isEqualTo(CheckoutSource.RECEPTIONIST);
	}

	@Test
	void openVisitsBatchUniqueMemberAccountsAndProfiles() {
		UUID secondMemberId = UUID.randomUUID();
		CenterVisit first = visit(now.minusSeconds(300), null);
		CenterVisit second = new CenterVisit(UUID.randomUUID(), secondMemberId, UUID.randomUUID(),
			receptionistId, now.minusSeconds(200), null);
		var pageable = PageRequest.of(0, 20);
		Set<UUID> memberIds = Set.of(memberId, secondMemberId);
		Account firstAccount = memberAccount("First Member");
		Account secondAccount = memberAccount(secondMemberId, "Second Member");
		when(accounts.existsByIdAndRoleAndStatus(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(visits.findByCheckedOutAtIsNull(pageable))
			.thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
		when(accounts.findAllById(memberIds)).thenReturn(List.of(firstAccount, secondAccount));
		when(profiles.findAllById(memberIds))
			.thenReturn(List.of(new MemberProfile(firstAccount), new MemberProfile(secondAccount)));

		CenterVisitPageResponse response = service.openVisits(receptionistId, pageable);

		assertThat(response.content()).extracting(CenterVisitResponse::memberName)
			.containsExactly("First Member", "Second Member");
		verify(accounts).findAllById(memberIds);
		verify(profiles).findAllById(memberIds);
		verify(accounts, never()).findById(any());
		verify(profiles, never()).findById(any());
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
	void currentMembershipWithCenterAccessBenefitIsRequired() {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.findCurrentMembership(memberId, now)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true), null))
			.isInstanceOf(CenterVisitException.class)
			.hasMessage("An active membership with the CENTER_ACCESS benefit is required for center check-in.");
		verify(visits, never()).saveAndFlush(any());
	}

	@Test
	void courseEnrollmentAllowsCheckInWithoutMembershipAndKeepsCourseLinkage() {
		UUID courseSessionId = UUID.randomUUID();
		UUID enrollmentId = UUID.randomUUID();
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.findEligibleCourseEnrollmentId(memberId, courseSessionId, now))
			.thenReturn(Optional.of(enrollmentId));
		when(visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId))
			.thenReturn(Optional.empty());
		when(visits.saveAndFlush(any(CenterVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CenterVisitResponse response = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true, courseSessionId), "course-entry");

		assertThat(response.membershipId()).isNull();
		assertThat(response.source()).isEqualTo(CenterVisitSource.COURSE);
		assertThat(response.courseEnrollmentId()).isEqualTo(enrollmentId);
		assertThat(response.courseSessionId()).isEqualTo(courseSessionId);
		verify(visits, never()).findCurrentMembership(any(), any());
		verify(visits, never()).hasActiveMembershipFreeze(any());
	}

	@Test
	void courseCheckInOutsideWindowIsRejectedWithoutFallingBackToMembership() {
		UUID courseSessionId = UUID.randomUUID();
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.findEligibleCourseEnrollmentId(memberId, courseSessionId, now)).thenReturn(Optional.empty());

		assertCode(() -> service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null, true, courseSessionId), null),
			"COURSE_CHECK_IN_NOT_ELIGIBLE");

		verify(visits, never()).findCurrentMembership(any(), any());
		verify(visits, never()).saveAndFlush(any());
	}

	@Test
	void activeFreezeBlocksCheckInBeforeMembershipLookup() {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(memberAccount()));
		when(visits.hasActiveMembershipFreeze(memberId)).thenReturn(true);

		assertCode(() -> service.checkIn(receptionistId, new CenterVisitRequest("MB-100001", null, true), null), "MEMBERSHIP_FROZEN");
		verify(visits, never()).findCurrentMembership(any(), any());
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
			"member@example.test", LocalDate.of(1990, 1, 1), image, status, null);
	}

	private Account memberAccount() {
		return memberAccount(AccountStatus.ACTIVE);
	}

	private Account memberAccount(AccountStatus status) {
		return memberAccount(memberId, status, "Member");
	}

	private Account memberAccount(String fullName) {
		return memberAccount(memberId, AccountStatus.ACTIVE, fullName);
	}

	private Account memberAccount(UUID accountId, String fullName) {
		return memberAccount(accountId, AccountStatus.ACTIVE, fullName);
	}

	private Account memberAccount(UUID accountId, AccountStatus status, String fullName) {
		return new Account(accountId, AccountRole.MEMBER, status, fullName, "0901000001",
			"member@example.test", LocalDate.of(1990, 1, 1), "password");
	}

	private MemberProfile profile() {
		return new MemberProfile(memberAccount(), "photo", null);
	}

	private CenterVisit visit(Instant checkedInAt, String key) {
		return new CenterVisit(UUID.randomUUID(), memberId, UUID.randomUUID(), receptionistId, checkedInAt, key);
	}
}
