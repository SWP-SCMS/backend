package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.stereotype.Service;

@Service
public class WaitlistPromotionService {
	private final SessionWaitlistRepository waitlists; private final BookingRepository bookings;
	private final WaitlistService eligibility; private final AuditEventRepository audits;
	private final NotificationWriter notifications; private final ClassSessionRepository sessions; private final Clock clock;
	WaitlistPromotionService(SessionWaitlistRepository waitlists,BookingRepository bookings,WaitlistService eligibility,
			AuditEventRepository audits,NotificationWriter notifications,ClassSessionRepository sessions,Clock clock){this.waitlists=waitlists;this.bookings=bookings;this.eligibility=eligibility;this.audits=audits;this.notifications=notifications;this.sessions=sessions;this.clock=clock;}

	public void promoteAvailableBySessionIds(Collection<UUID> sessionIds){
		if(sessionIds==null||sessionIds.isEmpty()) return;
		sessionIds.stream().distinct().sorted(Comparator.comparing(UUID::toString))
			.map(sessions::findByIdForUpdate).flatMap(java.util.Optional::stream)
			.filter(session->session.getStatus()==ClassSessionStatus.SCHEDULED)
			.forEach(this::promoteAvailable);
	}

	void promoteAvailable(ClassSession session){
		Instant now=clock.instant(); if(session.getStatus()!=ClassSessionStatus.SCHEDULED||!session.getStartTime().isAfter(now)){eligibility.expireStarted(now);return;}
		while(bookings.countByClassSessionIdAndStatus(session.getId(),BookingStatus.BOOKED)<session.getCapacity()){
			List<SessionWaitlist> waiting=waitlists.findWaitingBySessionForUpdate(session.getId()); if(waiting.isEmpty()) return;
			SessionWaitlist entry=waiting.getFirst(); UUID membershipId;
			if(!eligibility.tryLockMember(entry.getMemberId())) return;
			try { membershipId=eligibility.validateEligibility(entry.getMemberId(),session); }
			catch(BookingException exception){
				entry.skip(exception.getCode(),now); waitlists.save(entry);
				audits.save(new AuditEvent(UUID.randomUUID(),null,"WAITLIST_SKIPPED","SESSION_WAITLIST",
					entry.getId(),Map.of("reasonCode",exception.getCode())));
				notifications.write("waitlist-skipped:"+entry.getId(),entry.getMemberId(),"WAITLIST_SKIPPED",
					"SESSION_WAITLIST",entry.getId(),Map.of("sessionId",session.getId().toString(),
						"reasonCode",exception.getCode()));
				continue;
			}
			Booking booking=new Booking(UUID.randomUUID(),session.getId(),entry.getMemberId(),membershipId,entry.getJoinedBy(),now);
			bookings.saveAndFlush(booking); entry.promote(now); waitlists.save(entry);
			audits.save(new AuditEvent(UUID.randomUUID(),null,"WAITLIST_PROMOTED","SESSION_WAITLIST",entry.getId(),Map.of("bookingId",booking.getId().toString())));
			notifications.write("waitlist-promoted:"+entry.getId(),entry.getMemberId(),"WAITLIST_PROMOTED","BOOKING",booking.getId(),Map.of("sessionId",session.getId().toString()));
		}
	}

	void cancelWaiting(ClassSession session){
		Instant now=clock.instant(); for(SessionWaitlist entry:waitlists.findWaitingBySessionForUpdate(session.getId())){
			entry.cancel("SESSION_CANCELLED",now); waitlists.save(entry);
			audits.save(new AuditEvent(UUID.randomUUID(),null,"WAITLIST_CANCELLED","SESSION_WAITLIST",entry.getId(),Map.of("reasonCode","SESSION_CANCELLED")));
			notifications.write("waitlist-cancelled:"+entry.getId(),entry.getMemberId(),"WAITLIST_CANCELLED","CLASS_SESSION",session.getId(),Map.of("sessionId",session.getId().toString()));
		}
	}
}
