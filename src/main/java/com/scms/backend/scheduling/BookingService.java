package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

	private final BookingRepository bookings;
	private final ClassSessionRepository sessions;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final SessionLifecycleService lifecycle;
	private final Clock clock;

	BookingService(BookingRepository bookings, ClassSessionRepository sessions, AccountRepository accounts,
			AuditEventRepository audits, NotificationWriter notifications, SessionLifecycleService lifecycle,
			Clock clock) {
		this.bookings = bookings;
		this.sessions = sessions;
		this.accounts = accounts;
		this.audits = audits;
		this.notifications = notifications;
		this.lifecycle = lifecycle;
		this.clock = clock;
	}

	@Transactional
	BookingResponse book(UUID memberId, UUID sessionId) {
		return book(memberId, memberId, sessionId);
	}

	@Transactional
	BookingResponse bookForMember(UUID receptionistId, UUID memberId, UUID sessionId) {
		ensureActiveReceptionist(receptionistId);
		return book(receptionistId, memberId, sessionId);
	}

	private BookingResponse book(UUID actorId, UUID memberId, UUID sessionId) {
		ensureActiveMember(memberId);
		Instant now = clock.instant();
		lifecycle.catchUp(now);
		ClassSession session = sessions.findByIdForUpdate(sessionId).orElseThrow(BookingException::sessionNotFound);
		if (session.getStatus() != ClassSessionStatus.SCHEDULED || !session.getStartTime().isAfter(now)) {
			throw BookingException.sessionUnavailable();
		}
		UUID membershipId = bookings.findEligiblePlusMembership(memberId, session.getStartTime())
			.orElseThrow(BookingException::plusRequired);
		if (bookings.existsByMemberAccountIdAndClassSessionIdAndStatus(memberId, sessionId,
				BookingStatus.BOOKED)) throw BookingException.duplicate();
		if (bookings.existsBookedOverlap(memberId, session.getStartTime(), session.getEndTime())) {
			throw BookingException.timeConflict();
		}
		if (bookings.countByClassSessionIdAndStatus(sessionId, BookingStatus.BOOKED) >= session.getCapacity()) {
			throw BookingException.full();
		}

		Booking booking = new Booking(UUID.randomUUID(), sessionId, memberId, membershipId, actorId, now);
		try {
			bookings.saveAndFlush(booking);
		}
		catch (DataIntegrityViolationException exception) {
			throw BookingException.concurrentConflict();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "BOOKING_CREATED", "BOOKING", booking.getId(),
			Map.of("sessionId", sessionId.toString(), "membershipId", membershipId.toString(),
				"status", BookingStatus.BOOKED.name())));
		notifications.write("BOOKING_CREATED:" + booking.getId(), memberId, "BOOKING_CREATED", "BOOKING",
			booking.getId(), Map.of("sessionId", sessionId.toString()));
		return BookingResponse.from(booking);
	}

	@Transactional
	BookingResponse cancel(UUID memberId, UUID bookingId) {
		return cancel(memberId, memberId, bookingId, "MEMBER");
	}

	@Transactional
	BookingResponse cancelForMember(UUID receptionistId, UUID memberId, UUID bookingId) {
		ensureActiveReceptionist(receptionistId);
		return cancel(receptionistId, memberId, bookingId, "RECEPTIONIST");
	}

	private BookingResponse cancel(UUID actorId, UUID memberId, UUID bookingId, String source) {
		ensureActiveMember(memberId);
		Booking booking = bookings.findByIdForUpdate(bookingId).orElseThrow(BookingException::notFound);
		if (!booking.getMemberAccountId().equals(memberId)) throw BookingException.notFound();
		if (booking.getStatus() != BookingStatus.BOOKED) throw BookingException.notBooked();
		ClassSession session = sessions.findByIdForUpdate(booking.getClassSessionId())
			.orElseThrow(BookingException::sessionNotFound);
		Instant now = clock.instant();
		if (now.isAfter(session.getStartTime().minus(Duration.ofHours(2)))) {
			throw BookingException.cancellationWindowClosed();
		}
		booking.cancel(actorId, source, now);
		bookings.saveAndFlush(booking);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "BOOKING_CANCELLED", "BOOKING", bookingId,
			source + " cancellation", Map.of("status", BookingStatus.BOOKED.name()),
			Map.of("status", BookingStatus.CANCELLED.name(), "cancellationSource", source)));
		notifications.write("BOOKING_CANCELLED:" + bookingId, memberId, "BOOKING_CANCELLED", "BOOKING", bookingId,
			Map.of("sessionId", session.getId().toString()));
		return BookingResponse.from(booking);
	}

	private void ensureActiveMember(UUID memberId) {
		var member = accounts.findByIdForUpdate(memberId).orElseThrow(BookingException::memberNotActive);
		if (member.getRole() != AccountRole.MEMBER || member.getStatus() != AccountStatus.ACTIVE) {
			throw BookingException.memberNotActive();
		}
	}

	private void ensureActiveReceptionist(UUID accountId) {
		var actor = accounts.findByIdForUpdate(accountId).orElseThrow(BookingException::actorNotActiveReceptionist);
		if (actor.getRole() != AccountRole.RECEPTIONIST || actor.getStatus() != AccountStatus.ACTIVE) {
			throw BookingException.actorNotActiveReceptionist();
		}
	}
}
