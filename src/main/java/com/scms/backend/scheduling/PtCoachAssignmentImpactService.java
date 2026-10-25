package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.training.CoachAssignmentException;
import org.springframework.stereotype.Service;

@Service
public class PtCoachAssignmentImpactService {
	private static final String CHANGE_REASON = "COACH_ASSIGNMENT_CHANGED";

	private final BookingRepository bookings;
	private final SessionWaitlistRepository waitlists;
	private final ClassSessionRepository sessions;
	private final WaitlistPromotionService promotions;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;

	PtCoachAssignmentImpactService(BookingRepository bookings, SessionWaitlistRepository waitlists,
			ClassSessionRepository sessions, WaitlistPromotionService promotions,
			AuditEventRepository audits, NotificationWriter notifications) {
		this.bookings = bookings;
		this.waitlists = waitlists;
		this.sessions = sessions;
		this.promotions = promotions;
		this.audits = audits;
		this.notifications = notifications;
	}

	public PtAssignmentImpactResponse preview(UUID memberId, UUID coachId, Instant now) {
		List<PtAssignmentImpactItem> affectedBookings = bookings.findFuturePtImpact(memberId, coachId, now)
			.stream().map(row -> new PtAssignmentImpactItem(row.getId(), row.getSessionId(),
				row.getSessionStartTime())).toList();
		List<PtAssignmentImpactItem> affectedWaitlists = waitlists.findFuturePtImpact(memberId, coachId, now)
			.stream().map(row -> new PtAssignmentImpactItem(row.getId(), row.getSessionId(),
				row.getSessionStartTime())).toList();
		return new PtAssignmentImpactResponse(affectedBookings.size(), affectedWaitlists.size(),
			affectedBookings, affectedWaitlists);
	}

	public PtAssignmentImpactResponse resolve(UUID managerId, UUID memberId, UUID coachId, Instant now,
			PtAssignmentImpactConfirmation confirmation) {
		PtAssignmentImpactResponse initial = preview(memberId, coachId, now);
		Set<UUID> sessionIds = new LinkedHashSet<>();
		initial.bookings().forEach(item -> sessionIds.add(item.sessionId()));
		initial.waitlists().forEach(item -> sessionIds.add(item.sessionId()));

		Map<UUID, ClassSession> lockedSessions = new LinkedHashMap<>();
		sessionIds.stream().sorted().forEach(sessionId -> sessions.findByIdForUpdate(sessionId)
			.ifPresent(session -> lockedSessions.put(sessionId, session)));

		PtAssignmentImpactResponse current = preview(memberId, coachId, now);
		if (current.bookingCount() + current.waitlistCount() > 0 && confirmation == null) {
			throw CoachAssignmentException.impactConfirmationRequired(current);
		}
		if (confirmation != null && !matches(current, confirmation)) {
			throw CoachAssignmentException.impactChanged(current);
		}
		if (current.bookingCount() + current.waitlistCount() == 0) {
			return current;
		}

		List<UUID> bookingIds = current.bookings().stream().map(PtAssignmentImpactItem::id).toList();
		List<UUID> waitlistIds = current.waitlists().stream().map(PtAssignmentImpactItem::id).toList();
		List<Booking> affectedBookings = bookingIds.isEmpty() ? List.of()
			: bookings.findBookedByIdsForUpdate(bookingIds);
		List<SessionWaitlist> affectedWaitlists = waitlistIds.isEmpty() ? List.of()
			: waitlists.findWaitingByIdsForUpdate(waitlistIds);
		if (affectedBookings.size() != bookingIds.size() || affectedWaitlists.size() != waitlistIds.size()) {
			throw CoachAssignmentException.impactChanged(preview(memberId, coachId, now));
		}

		for (SessionWaitlist entry : affectedWaitlists) {
			entry.cancel(CHANGE_REASON, now);
			waitlists.save(entry);
			audits.save(new AuditEvent(UUID.randomUUID(), managerId, "WAITLIST_CANCELLED",
				"SESSION_WAITLIST", entry.getId(), Map.of("sessionId", entry.getSessionId().toString(),
					"reasonCode", CHANGE_REASON)));
			notifications.write("COACH_ASSIGNMENT_CHANGED:WAITLIST:" + entry.getId(), memberId,
				"PT_WAITLIST_CANCELLED", "SESSION_WAITLIST", entry.getId(),
				Map.of("sessionId", entry.getSessionId().toString(), "reason", CHANGE_REASON));
		}

		List<ClassSession> freedSessions = new ArrayList<>();
		for (Booking booking : affectedBookings) {
			booking.cancel(managerId, CHANGE_REASON, now);
			bookings.save(booking);
			audits.save(new AuditEvent(UUID.randomUUID(), managerId, "BOOKING_CANCELLED", "BOOKING",
				booking.getId(), Map.of("sessionId", booking.getClassSessionId().toString(),
					"reasonCode", CHANGE_REASON)));
			notifications.write("COACH_ASSIGNMENT_CHANGED:BOOKING:" + booking.getId(), memberId,
				"PT_BOOKING_CANCELLED", "BOOKING", booking.getId(),
				Map.of("sessionId", booking.getClassSessionId().toString(), "reason", CHANGE_REASON));
			ClassSession session = lockedSessions.get(booking.getClassSessionId());
			if (session != null) freedSessions.add(session);
		}
		bookings.flush();
		freedSessions.stream().sorted(Comparator.comparing(session -> session.getId().toString()))
			.distinct().forEach(promotions::promoteAvailable);
		return current;
	}

	private boolean matches(PtAssignmentImpactResponse impact, PtAssignmentImpactConfirmation confirmation) {
		Set<UUID> currentBookingIds = impact.bookings().stream().map(PtAssignmentImpactItem::id)
			.collect(java.util.stream.Collectors.toSet());
		Set<UUID> currentWaitlistIds = impact.waitlists().stream().map(PtAssignmentImpactItem::id)
			.collect(java.util.stream.Collectors.toSet());
		return currentBookingIds.equals(Set.copyOf(confirmation.bookingIds()))
			&& currentWaitlistIds.equals(Set.copyOf(confirmation.waitlistIds()))
			&& confirmation.bookingIds().size() == currentBookingIds.size()
			&& confirmation.waitlistIds().size() == currentWaitlistIds.size();
	}
}
