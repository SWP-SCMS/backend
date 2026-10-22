package com.scms.backend.membership;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class MembershipLifecycleSchedulerTests {

	@Test
	void reconcilesEachDueMemberIndependentlyThroughTheLifecycleAuthority() {
		Instant now = Instant.parse("2026-10-21T03:00:00Z");
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		MembershipRepository memberships = mock(MembershipRepository.class);
		MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
		when(memberships.findDueMemberAccountIds(now, null)).thenReturn(List.of(first, second));

		new MembershipLifecycleScheduler(memberships, lifecycle, Clock.fixed(now, ZoneOffset.UTC)).reconcile();

		InOrder order = inOrder(lifecycle);
		order.verify(lifecycle).reconcile(first, now);
		order.verify(lifecycle).reconcile(second, now);
		order.verify(lifecycle).reconcileExpiredAssignments(now);
	}

	@Test
	void oneMemberFailureDoesNotPreventTheRemainingMembersFromReconciling() {
		Instant now = Instant.parse("2026-10-21T03:00:00Z");
		UUID first = UUID.randomUUID();
		UUID second = UUID.randomUUID();
		MembershipRepository memberships = mock(MembershipRepository.class);
		MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
		when(memberships.findDueMemberAccountIds(now, null)).thenReturn(List.of(first, second));
		doThrow(new IllegalStateException("broken member")).when(lifecycle).reconcile(first, now);

		new MembershipLifecycleScheduler(memberships, lifecycle, Clock.fixed(now, ZoneOffset.UTC)).reconcile();

		org.mockito.Mockito.verify(lifecycle).reconcile(second, now);
		org.mockito.Mockito.verify(lifecycle).reconcileExpiredAssignments(now);
	}

	@Test
	void continuesWithTheNextDueMemberPageAfterTheFirstPageIsFull() {
		Instant now = Instant.parse("2026-10-21T03:00:00Z");
		List<UUID> firstPage = java.util.stream.IntStream.range(0, 100)
			.mapToObj(ignored -> UUID.randomUUID()).toList();
		UUID nextPageMember = UUID.randomUUID();
		MembershipRepository memberships = mock(MembershipRepository.class);
		MembershipLifecycleService lifecycle = mock(MembershipLifecycleService.class);
		when(memberships.findDueMemberAccountIds(now, null)).thenReturn(firstPage);
		when(memberships.findDueMemberAccountIds(now, firstPage.get(99))).thenReturn(List.of(nextPageMember));

		new MembershipLifecycleScheduler(memberships, lifecycle, Clock.fixed(now, ZoneOffset.UTC)).reconcile();

		org.mockito.Mockito.verify(lifecycle).reconcile(nextPageMember, now);
	}
}
