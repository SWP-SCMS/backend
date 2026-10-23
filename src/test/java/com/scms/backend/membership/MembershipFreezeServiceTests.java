package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class MembershipFreezeServiceTests {
	@Test
	void managerFreezeListIncludesCurrentMemberIdentityUsingBatchLookups() {
		Fixture fixture = fixture(LocalDate.of(2026, 11, 1));
		UUID managerId = UUID.randomUUID();
		when(fixture.accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)).thenReturn(true);
		MembershipFreezeRequest anotherRequest = new MembershipFreezeRequest(UUID.randomUUID(), fixture.membershipId,
			fixture.memberId, fixture.memberId, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3), "another reason");
		when(fixture.requests.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(fixture.request, anotherRequest), PageRequest.of(0, 20), 2));
		Account member = mock(Account.class);
		when(member.getId()).thenReturn(fixture.memberId);
		when(member.getFullName()).thenReturn("Member One");
		when(fixture.accounts.findAllById(List.of(fixture.memberId))).thenReturn(List.of(member));
		MemberProfile profile = mock(MemberProfile.class);
		when(profile.getAccountId()).thenReturn(fixture.memberId);
		when(profile.getMemberCode()).thenReturn("MB-100001");
		when(fixture.profiles.findAllById(List.of(fixture.memberId))).thenReturn(List.of(profile));

		List<MembershipFreezeRequestResponse> items = fixture.service.listForManager(managerId, 0, 20).content();

		assertThat(items).hasSize(2).allSatisfy(item -> {
			assertThat(item.memberId()).isEqualTo("MB-100001");
			assertThat(item.memberFullName()).isEqualTo("Member One");
		});
		verify(fixture.accounts).findAllById(List.of(fixture.memberId));
		verify(fixture.profiles).findAllById(List.of(fixture.memberId));
	}

	@Test
	void feeUsesHalfOpenBusinessDaysAndRoundsDownVnd() {
		Membership membership = membership(new BigInteger("1000000"), 31);
		assertThat(MembershipFreezeService.fee(membership, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 4)))
			.isEqualTo(new BigInteger("19354"));
	}
	@Test
	void requestMovesOnlyThroughTheInitialPaymentWorkflow() {
		MembershipFreezeRequest request = new MembershipFreezeRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT);
		request.schedule();
		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.SCHEDULED);
		assertThatThrownBy(request::schedule).isInstanceOf(IllegalStateException.class);
	}
	@Test
	void scheduledRequestActivatesExactlyOnceAtBusinessDayStart() {
		MembershipFreezeRequest request = new MembershipFreezeRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		request.schedule();
		request.activate();
		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.ACTIVE);
		assertThatThrownBy(request::activate).isInstanceOf(IllegalStateException.class);
	}
	@Test
	void activeRequestCompletesExactlyOnce() {
		MembershipFreezeRequest request = new MembershipFreezeRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 3), "travel");
		request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
		request.schedule();
		request.activate();

		request.complete();

		assertThat(request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.COMPLETED);
		assertThatThrownBy(request::complete).isInstanceOf(IllegalStateException.class);
	}
	@Test
	void memberEndsOwnActiveFreezeUsingHoChiMinhDateAndExtendsMembershipExactlyOnce() {
		Fixture fixture = fixture(LocalDate.of(2026, 11, 1));
		when(fixture.accounts.existsByIdAndRoleAndStatus(fixture.memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		fixture.lock();
		when(fixture.jdbc.queryForObject(contains("actual_start_date"), eq(LocalDate.class), eq(fixture.requestId)))
			.thenReturn(LocalDate.of(2026, 11, 1));
		when(fixture.jdbc.update(contains("status='ENDED_EARLY'"), eq(Date.valueOf("2026-11-03")), eq(2), eq(fixture.requestId)))
			.thenReturn(1);

		MembershipFreezeRequestResponse response = fixture.service.endEarlyForMember(fixture.memberId, fixture.requestId,
			new MembershipFreezeDecisionRequest(7L));

		assertThat(response.status()).isEqualTo(MembershipFreezeRequestStatus.COMPLETED);
		assertThat(fixture.membership.getEndsAt()).isEqualTo(fixture.originalMembershipEnd.plusSeconds(2 * 86_400L));
		assertThatThrownBy(() -> fixture.service.endEarlyForMember(fixture.memberId, fixture.requestId,
			new MembershipFreezeDecisionRequest(7L))).isInstanceOfSatisfying(MembershipFreezeException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("MEMBERSHIP_FREEZE_INVALID_TRANSITION"));
		verify(fixture.jdbc, times(1)).update(contains("status='ENDED_EARLY'"), eq(Date.valueOf("2026-11-03")), eq(2), eq(fixture.requestId));
		verify(fixture.jdbc, never()).update(contains("membership_freeze_fee_payments"), any(Object[].class));
		ArgumentCaptor<AuditEvent> audit = ArgumentCaptor.forClass(AuditEvent.class);
		verify(fixture.audits).save(audit.capture());
		assertThat(audit.getValue().getAction()).isEqualTo("MEMBERSHIP_FREEZE_ENDED_EARLY");
		assertThat(audit.getValue().getAfterData()).containsEntry("creditedDays", 2);
		verify(fixture.notifications).write(eq("MEMBERSHIP_FREEZE_ENDED_EARLY:" + fixture.requestId), eq(fixture.memberId),
			eq("MEMBERSHIP_FREEZE_ENDED_EARLY"), eq("MEMBERSHIP_FREEZE_REQUEST"), eq(fixture.requestId),
			eq(Map.of("creditedDays", 2)));
	}

	@Test
	void receptionistCanEndActiveFreezeWithZeroSameDayCredit() {
		Fixture fixture = fixture(LocalDate.of(2026, 11, 3));
		UUID receptionistId = UUID.randomUUID();
		when(fixture.accounts.existsByIdAndRoleAndStatus(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(true);
		fixture.lock();
		when(fixture.jdbc.queryForObject(contains("actual_start_date"), eq(LocalDate.class), eq(fixture.requestId)))
			.thenReturn(LocalDate.of(2026, 11, 3));
		when(fixture.jdbc.update(contains("status='ENDED_EARLY'"), eq(Date.valueOf("2026-11-03")), eq(0), eq(fixture.requestId)))
			.thenReturn(1);

		fixture.service.endEarlyForReceptionist(receptionistId, fixture.requestId, new MembershipFreezeDecisionRequest(7L));

		assertThat(fixture.request.getStatus()).isEqualTo(MembershipFreezeRequestStatus.COMPLETED);
		assertThat(fixture.membership.getEndsAt()).isEqualTo(fixture.originalMembershipEnd);
		verify(fixture.jdbc).update(contains("status='ENDED_EARLY'"), eq(Date.valueOf("2026-11-03")), eq(0), eq(fixture.requestId));
	}

	@Test
	void memberCannotEndAnotherMembersFreeze() {
		Fixture fixture = fixture(LocalDate.of(2026, 11, 1));
		UUID otherMember = UUID.randomUUID();
		when(fixture.accounts.existsByIdAndRoleAndStatus(otherMember, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		when(fixture.requests.findById(fixture.requestId)).thenReturn(Optional.of(fixture.request));

		assertThatThrownBy(() -> fixture.service.endEarlyForMember(otherMember, fixture.requestId,
			new MembershipFreezeDecisionRequest(7L))).isInstanceOfSatisfying(MembershipFreezeException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("MEMBERSHIP_FREEZE_NOT_FOUND"));
		verify(fixture.memberships, never()).findByIdForUpdate(any());
		verify(fixture.jdbc, never()).update(any(String.class), any(Object[].class));
	}

	@Test
	void onlyActiveReceptionistCanUseReceptionEarlyEnd() {
		Fixture fixture = fixture(LocalDate.of(2026, 11, 1));
		UUID actorId = UUID.randomUUID();
		when(fixture.accounts.existsByIdAndRoleAndStatus(actorId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(false);

		assertThatThrownBy(() -> fixture.service.endEarlyForReceptionist(actorId, fixture.requestId,
			new MembershipFreezeDecisionRequest(7L))).isInstanceOf(InvalidAuthenticatedAccountException.class);
		verify(fixture.requests, never()).findById(any());
	}

	@Test
	void earlyEndRejectsStaleVersionAndNonActiveRequest() {
		Fixture stale = fixture(LocalDate.of(2026, 11, 1));
		when(stale.accounts.existsByIdAndRoleAndStatus(stale.memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		stale.lock();
		assertThatThrownBy(() -> stale.service.endEarlyForMember(stale.memberId, stale.requestId,
			new MembershipFreezeDecisionRequest(6L))).isInstanceOfSatisfying(MembershipFreezeException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("MEMBERSHIP_FREEZE_CHANGED"));

		Fixture scheduled = fixture(LocalDate.of(2026, 11, 1));
		ReflectionTestUtils.setField(scheduled.request, "status", MembershipFreezeRequestStatus.SCHEDULED);
		when(scheduled.accounts.existsByIdAndRoleAndStatus(scheduled.memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)).thenReturn(true);
		scheduled.lock();
		assertThatThrownBy(() -> scheduled.service.endEarlyForMember(scheduled.memberId, scheduled.requestId,
			new MembershipFreezeDecisionRequest(7L))).isInstanceOfSatisfying(MembershipFreezeException.class,
				exception -> assertThat(exception.getCode()).isEqualTo("MEMBERSHIP_FREEZE_INVALID_TRANSITION"));
		verify(stale.jdbc, never()).update(any(String.class), any(Object[].class));
		verify(scheduled.jdbc, never()).update(any(String.class), any(Object[].class));
	}

	private Membership membership(BigInteger price, int duration) {
		return new Membership(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "PLUS", "Offer", price, "VND", duration, MembershipStatus.ACTIVE, Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));
	}

	private Fixture fixture(LocalDate actualStart) {
		return new Fixture(actualStart);
	}

	private static final class Fixture {
		private final UUID requestId = UUID.randomUUID();
		private final UUID membershipId = UUID.randomUUID();
		private final UUID memberId = UUID.randomUUID();
		private final Instant originalMembershipEnd = Instant.parse("2026-12-01T00:00:00Z");
		private final AccountRepository accounts = mock(AccountRepository.class);
		private final MemberProfileRepository profiles = mock(MemberProfileRepository.class);
		private final MembershipRepository memberships = mock(MembershipRepository.class);
		private final MembershipFreezeRequestRepository requests = mock(MembershipFreezeRequestRepository.class);
		private final AuditEventRepository audits = mock(AuditEventRepository.class);
		private final NotificationWriter notifications = mock(NotificationWriter.class);
		private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
		private final MembershipFreezeRequest request;
		private final Membership membership;
		private final MembershipFreezeService service;

		private Fixture(LocalDate actualStart) {
			request = new MembershipFreezeRequest(requestId, membershipId, memberId, memberId,
				actualStart, LocalDate.of(2026, 11, 10), "travel");
			request.approve(UUID.randomUUID(), BigInteger.valueOf(100), Instant.parse("2026-10-22T00:00:00Z"));
			request.schedule();
			request.activate();
			ReflectionTestUtils.setField(request, "version", 7L);
			membership = new Membership(membershipId, memberId, UUID.randomUUID(), UUID.randomUUID(), "PLUS", "Offer",
				BigInteger.valueOf(100_000), "VND", 30, MembershipStatus.ACTIVE,
				Instant.parse("2026-11-01T00:00:00Z"), originalMembershipEnd);
			service = new MembershipFreezeService(accounts, profiles, memberships, requests, audits, notifications, jdbc,
				Clock.fixed(Instant.parse("2026-11-02T18:00:00Z"), ZoneOffset.UTC));
		}

		private void lock() {
			when(requests.findById(requestId)).thenReturn(Optional.of(request));
			when(accounts.findByIdForUpdate(memberId)).thenReturn(Optional.of(mock(Account.class)));
			when(memberships.findByIdForUpdate(membershipId)).thenReturn(Optional.of(membership));
			when(requests.findByIdForUpdate(requestId)).thenReturn(Optional.of(request));
		}
	}
}
