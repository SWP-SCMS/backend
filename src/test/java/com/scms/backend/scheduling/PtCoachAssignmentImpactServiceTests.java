package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.training.CoachAssignmentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PtCoachAssignmentImpactServiceTests {
	@Mock BookingRepository bookings;
	@Mock SessionWaitlistRepository waitlists;
	@Mock ClassSessionRepository sessions;
	@Mock WaitlistPromotionService promotions;
	@Mock AuditEventRepository audits;
	@Mock NotificationWriter notifications;

	private PtCoachAssignmentImpactService service;
	private final UUID managerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();
	private final UUID coachId = UUID.randomUUID();
	private final UUID sessionId = UUID.randomUUID();
	private final Instant now = Instant.parse("2026-10-25T03:00:00Z");

	@BeforeEach
	void setUp() {
		service = new PtCoachAssignmentImpactService(bookings, waitlists, sessions, promotions, audits,
			notifications);
	}

	@Test
	void resolve_withoutConfirmation_returnsCurrentImpactInConflict() {
		UUID bookingId = UUID.randomUUID();
		BookingRepository.PtImpactView row = bookingImpact(bookingId);
		when(bookings.findFuturePtImpact(memberId, coachId, now)).thenReturn(List.of(row));
		when(waitlists.findFuturePtImpact(memberId, coachId, now)).thenReturn(List.of());
		when(sessions.findByIdForUpdate(sessionId)).thenReturn(Optional.of(mock(ClassSession.class)));

		assertThatThrownBy(() -> service.resolve(managerId, memberId, coachId, now, null))
			.isInstanceOf(CoachAssignmentException.class)
			.hasFieldOrPropertyWithValue("code", "COACH_ASSIGNMENT_IMPACT_CONFIRMATION_REQUIRED")
			.extracting("impact.bookingCount").isEqualTo(1);
	}

	@Test
	void resolve_whenConfirmationIsStale_rejectsMutation() {
		UUID bookingId = UUID.randomUUID();
		BookingRepository.PtImpactView row = bookingImpact(bookingId);
		when(bookings.findFuturePtImpact(memberId, coachId, now)).thenReturn(List.of(row));
		when(waitlists.findFuturePtImpact(memberId, coachId, now)).thenReturn(List.of());
		when(sessions.findByIdForUpdate(sessionId)).thenReturn(Optional.of(mock(ClassSession.class)));

		PtAssignmentImpactConfirmation stale = new PtAssignmentImpactConfirmation(List.of(UUID.randomUUID()),
			List.of());
		assertThatThrownBy(() -> service.resolve(managerId, memberId, coachId, now, stale))
			.isInstanceOf(CoachAssignmentException.class)
			.hasFieldOrPropertyWithValue("code", "COACH_ASSIGNMENT_IMPACT_CHANGED");
	}

	@Test
	void resolve_cancelsBookingAndWaitlist_notifiesMemberAndPromotesFreedSlot() {
		UUID bookingId = UUID.randomUUID();
		UUID waitlistId = UUID.randomUUID();
		UUID membershipId = UUID.randomUUID();
		Booking booking = new Booking(bookingId, sessionId, memberId, membershipId, memberId,
			now.minusSeconds(60));
		SessionWaitlist waitlist = new SessionWaitlist(waitlistId, sessionId, memberId, memberId,
			"MEMBER", now.minusSeconds(30));
		ClassSession session = mock(ClassSession.class);
		BookingRepository.PtImpactView bookingRow = bookingImpact(bookingId);
		SessionWaitlistRepository.PtImpactView waitlistRow = waitlistImpact(waitlistId);
		when(bookings.findFuturePtImpact(memberId, coachId, now)).thenReturn(List.of(bookingRow));
		when(waitlists.findFuturePtImpact(memberId, coachId, now)).thenReturn(List.of(waitlistRow));
		when(sessions.findByIdForUpdate(sessionId)).thenReturn(Optional.of(session));
		when(bookings.findBookedByIdsForUpdate(List.of(bookingId))).thenReturn(List.of(booking));
		when(waitlists.findWaitingByIdsForUpdate(List.of(waitlistId))).thenReturn(List.of(waitlist));

		PtAssignmentImpactResponse result = service.resolve(managerId, memberId, coachId, now,
			new PtAssignmentImpactConfirmation(List.of(bookingId), List.of(waitlistId)));

		assertThat(result.bookingCount()).isEqualTo(1);
		assertThat(result.waitlistCount()).isEqualTo(1);
		assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(booking.getCancellationSource()).isEqualTo("COACH_ASSIGNMENT_CHANGED");
		assertThat(waitlist.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
		assertThat(waitlist.getTerminalReasonCode()).isEqualTo("COACH_ASSIGNMENT_CHANGED");
		verify(notifications).write(eq("COACH_ASSIGNMENT_CHANGED:BOOKING:" + bookingId), eq(memberId),
			eq("PT_BOOKING_CANCELLED"), eq("BOOKING"), eq(bookingId), any());
		verify(notifications).write(eq("COACH_ASSIGNMENT_CHANGED:WAITLIST:" + waitlistId), eq(memberId),
			eq("PT_WAITLIST_CANCELLED"), eq("SESSION_WAITLIST"), eq(waitlistId), any());
		verify(promotions).promoteAvailable(session);
	}

	private BookingRepository.PtImpactView bookingImpact(UUID id) {
		BookingRepository.PtImpactView row = mock(BookingRepository.PtImpactView.class);
		when(row.getId()).thenReturn(id);
		when(row.getSessionId()).thenReturn(sessionId);
		when(row.getSessionStartTime()).thenReturn(now.plusSeconds(3600));
		return row;
	}

	private SessionWaitlistRepository.PtImpactView waitlistImpact(UUID id) {
		SessionWaitlistRepository.PtImpactView row = mock(SessionWaitlistRepository.PtImpactView.class);
		when(row.getId()).thenReturn(id);
		when(row.getSessionId()).thenReturn(sessionId);
		when(row.getSessionStartTime()).thenReturn(now.plusSeconds(3600));
		return row;
	}
}
