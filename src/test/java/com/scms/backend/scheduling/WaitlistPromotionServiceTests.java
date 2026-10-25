package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitlistPromotionServiceTests {
	@Mock SessionWaitlistRepository waitlists; @Mock BookingRepository bookings; @Mock WaitlistService eligibility;
	@Mock AuditEventRepository audits; @Mock NotificationWriter notifications; @Mock ClassSessionRepository sessions;
	Instant now=Instant.parse("2026-10-22T01:00:00Z");

	@Test void skipsIneligibleHeadAndPromotesNextEntry(){
		ClassSession session=session(); SessionWaitlist first=entry(session),second=entry(session);
		when(bookings.countByClassSessionIdAndStatus(session.getId(),BookingStatus.BOOKED)).thenReturn(0L,0L,1L);
		when(waitlists.findWaitingBySessionForUpdate(session.getId())).thenReturn(List.of(first,second),List.of(second));
		when(eligibility.validateEligibility(first.getMemberId(),session)).thenThrow(BookingException.timeConflict());
		UUID membershipId=UUID.randomUUID(); when(eligibility.validateEligibility(second.getMemberId(),session)).thenReturn(membershipId);
		when(bookings.saveAndFlush(any())).thenAnswer(invocation->invocation.getArgument(0));
		WaitlistPromotionService service=new WaitlistPromotionService(waitlists,bookings,eligibility,audits,notifications,sessions,Clock.fixed(now,ZoneOffset.UTC));
		service.promoteAvailable(session);
		assertThat(first.getStatus()).isEqualTo(WaitlistStatus.SKIPPED); assertThat(first.getTerminalReasonCode()).isEqualTo("BOOKING_TIME_CONFLICT");
		assertThat(second.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
		verify(notifications).write("waitlist-skipped:"+first.getId(),first.getMemberId(),"WAITLIST_SKIPPED",
			"SESSION_WAITLIST",first.getId(),Map.of("sessionId",session.getId().toString(),
				"reasonCode","BOOKING_TIME_CONFLICT"));
		verify(notifications).write(eq("waitlist-promoted:"+second.getId()),eq(second.getMemberId()),
			eq("WAITLIST_PROMOTED"),eq("BOOKING"),any(),
			eq(Map.of("sessionId",session.getId().toString())));
		service.promoteAvailable(session);
		verify(notifications,times(1)).write("waitlist-skipped:"+first.getId(),first.getMemberId(),"WAITLIST_SKIPPED",
			"SESSION_WAITLIST",first.getId(),Map.of("sessionId",session.getId().toString(),
				"reasonCode","BOOKING_TIME_CONFLICT"));
	}

	@Test void locksEachDistinctScheduledSessionBeforePromotion(){
		ClassSession scheduled=session(); ClassSession cancelled=session(); cancelled.cancel(now.minusSeconds(60),"cancelled");
		when(sessions.findByIdForUpdate(scheduled.getId())).thenReturn(java.util.Optional.of(scheduled));
		when(sessions.findByIdForUpdate(cancelled.getId())).thenReturn(java.util.Optional.of(cancelled));
		when(bookings.countByClassSessionIdAndStatus(scheduled.getId(),BookingStatus.BOOKED)).thenReturn(1L);
		WaitlistPromotionService service=new WaitlistPromotionService(waitlists,bookings,eligibility,audits,notifications,sessions,Clock.fixed(now,ZoneOffset.UTC));
		service.promoteAvailableBySessionIds(List.of(scheduled.getId(),cancelled.getId(),scheduled.getId()));
		verify(sessions,times(1)).findByIdForUpdate(scheduled.getId());
		verify(sessions,times(1)).findByIdForUpdate(cancelled.getId());
		verify(bookings,times(1)).countByClassSessionIdAndStatus(scheduled.getId(),BookingStatus.BOOKED);
	}

	private SessionWaitlist entry(ClassSession session){return new SessionWaitlist(UUID.randomUUID(),session.getId(),UUID.randomUUID(),UUID.randomUUID(),"MEMBER",now.minusSeconds(60));}
	private ClassSession session(){UUID id=UUID.randomUUID();Account coach=new Account(UUID.randomUUID(),AccountRole.COACH,AccountStatus.ACTIVE,"Coach","090"+id.toString().substring(0,8),id+"@test.local",LocalDate.of(1990,1,1),"{noop}x");Discipline discipline=new Discipline(UUID.randomUUID(),"Group",null,DisciplineStatus.ACTIVE);SportClass type=new SportClass(UUID.randomUUID(),discipline,"Group",SportClassType.GROUP,null,SportClassStatus.ACTIVE);return new ClassSession(UUID.randomUUID(),type,null,coach,new Room(UUID.randomUUID(),"Room",20,RoomStatus.ACTIVE),now.plusSeconds(3600),now.plusSeconds(7200),1,UUID.randomUUID());}
}
