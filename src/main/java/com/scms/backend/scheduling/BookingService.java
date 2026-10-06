package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

	private final BookingRepository bookings;
	private final ClassSessionRepository sessions;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	BookingService(BookingRepository bookings, ClassSessionRepository sessions, AccountRepository accounts,
			AuditEventRepository audits, ApplicationEventPublisher events, Clock clock) {
		this.bookings = bookings;
		this.sessions = sessions;
		this.accounts = accounts;
		this.audits = audits;
		this.events = events;
		this.clock = clock;
	}

	@Transactional
	BookingResponse book(UUID memberId, UUID sessionId) {
		var member = accounts.findByIdForUpdate(memberId).orElseThrow(BookingException::memberNotActive);
		if (member.getRole() != AccountRole.MEMBER || member.getStatus() != AccountStatus.ACTIVE) {
			throw BookingException.memberNotActive();
		}
		Instant now = clock.instant();
		sessions.advanceScheduledToInProgress(now);
		sessions.advanceInProgressToCompleted(now);
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

		Booking booking = new Booking(UUID.randomUUID(), sessionId, memberId, membershipId, memberId, now);
		try {
			bookings.saveAndFlush(booking);
		}
		catch (DataIntegrityViolationException exception) {
			throw BookingException.concurrentConflict();
		}
		audits.save(new AuditEvent(UUID.randomUUID(), memberId, "BOOKING_CREATED", "BOOKING", booking.getId(),
			Map.of("sessionId", sessionId.toString(), "membershipId", membershipId.toString(),
				"status", BookingStatus.BOOKED.name())));
		events.publishEvent(new BookingCreatedEvent(booking.getId(), sessionId, memberId));
		return BookingResponse.from(booking);
	}
}
