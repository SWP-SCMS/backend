package com.scms.backend.reception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.AccountStatus;
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
	CenterVisitService service;

	private final Instant now = Instant.parse("2026-10-06T05:00:00Z");
	private UUID receptionistId;
	private UUID memberId;

	@BeforeEach
	void setUp() {
		service = new CenterVisitService(members, visits, Clock.fixed(now, ZoneOffset.UTC));
		receptionistId = UUID.randomUUID();
		memberId = UUID.randomUUID();
	}

	@Test
	void createsNewVisitForEveryEntryUsingCurrentMembership() {
		UUID membershipId = UUID.randomUUID();
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(visits.findCurrentMembership(memberId, now)).thenReturn(Optional.of(membershipId));
		when(visits.save(any(CenterVisit.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CenterVisitResponse first = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null));
		CenterVisitResponse second = service.checkIn(receptionistId,
			new CenterVisitRequest("MB-100001", null));

		assertThat(first.id()).isNotEqualTo(second.id());
		assertThat(first.memberId()).isEqualTo(memberId);
		assertThat(first.membershipId()).isEqualTo(membershipId);
		assertThat(first.checkedInBy()).isEqualTo(receptionistId);
		assertThat(first.checkedInAt()).isEqualTo(now);
		ArgumentCaptor<CenterVisit> saved = ArgumentCaptor.forClass(CenterVisit.class);
		verify(visits, org.mockito.Mockito.times(2)).save(saved.capture());
		assertThat(saved.getAllValues()).extracting(CenterVisit::getId).doesNotHaveDuplicates();
	}

	@Test
	void suspendedMemberCannotCheckIn() {
		when(members.search(receptionistId, null, "0901000001"))
			.thenReturn(member(AccountStatus.SUSPENDED, "photo"));

		assertCode(new CenterVisitRequest(null, "0901000001"), "CENTER_VISIT_MEMBER_NOT_ACTIVE");
		verify(visits, never()).findCurrentMembership(any(), any());
	}

	@Test
	void profileImageIsRequired() {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "  "));

		assertCode(new CenterVisitRequest("MB-100001", null), "CENTER_VISIT_PROFILE_IMAGE_REQUIRED");
		verify(visits, never()).findCurrentMembership(any(), any());
	}

	@Test
	void currentBasicOrPlusMembershipIsRequired() {
		when(members.search(receptionistId, "MB-100001", null)).thenReturn(member(AccountStatus.ACTIVE, "photo"));
		when(visits.findCurrentMembership(memberId, now)).thenReturn(Optional.empty());

		assertCode(new CenterVisitRequest("MB-100001", null), "CENTER_VISIT_MEMBERSHIP_REQUIRED");
		verify(visits, never()).save(any());
	}

	private void assertCode(CenterVisitRequest request, String code) {
		assertThatThrownBy(() -> service.checkIn(receptionistId, request))
			.isInstanceOf(CenterVisitException.class)
			.extracting("code").isEqualTo(code);
	}

	private ReceptionMemberSearchResponse member(AccountStatus status, String image) {
		return new ReceptionMemberSearchResponse(memberId, "MB-100001", "Member", "0901000001",
			"member@example.test", LocalDate.of(1990, 1, 1), image, status);
	}
}
