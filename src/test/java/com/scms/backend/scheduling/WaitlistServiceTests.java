package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.training.CoachAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WaitlistServiceTests {
	@Mock SessionWaitlistRepository waitlists; @Mock ClassSessionRepository sessions;
	@Mock BookingRepository bookings; @Mock DisciplineRepository disciplines; @Mock AccountRepository accounts;
	@Mock CoachAssignmentRepository assignments; @Mock AuditEventRepository audits; @Mock NotificationWriter notifications;
	WaitlistService service; Instant now=Instant.parse("2026-10-22T01:00:00Z"); UUID memberId=UUID.randomUUID();
	ClassSession session;

	@BeforeEach void setUp(){
		service=new WaitlistService(waitlists,sessions,bookings,disciplines,accounts,assignments,audits,notifications,Clock.fixed(now,ZoneOffset.UTC));
		session=session(1);
	}

	@Test void fullSessionJoinsWaitlistAtFifoPosition(){
		when(accounts.findById(memberId)).thenReturn(Optional.of(account(memberId,AccountRole.MEMBER)));
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		when(disciplines.findBySessionIdForUpdate(session.getId())).thenReturn(Optional.of(session.getSportClass().getDiscipline()));
		when(bookings.countByClassSessionIdAndStatus(session.getId(),BookingStatus.BOOKED)).thenReturn(1L);
		UUID membershipId=UUID.randomUUID(); when(bookings.findBookingBenefitEligibility(memberId,session.getId(),session.getStartTime())).thenReturn(List.of(eligibility(membershipId)));
		when(waitlists.saveAndFlush(any())).thenAnswer(invocation->invocation.getArgument(0));
		AccountRepository.MemberIdentityView identity=new AccountRepository.MemberIdentityView(){
			public String getMemberCode(){return "MB-100001";} public String getFullName(){return "Member One";}
			public String getPhone(){return "0900000000";}};
		when(accounts.findMemberIdentity(memberId)).thenReturn(Optional.of(identity));
		WaitlistResponse response=service.join(memberId,session.getId());
		assertThat(response.status()).isEqualTo(WaitlistStatus.WAITING); assertThat(response.position()).isEqualTo(1);
		assertThat(response.memberCode()).isEqualTo("MB-100001");
		verify(audits).save(any());
	}

	@Test void sessionWithCapacityRejectsWaitlistJoin(){
		when(accounts.findById(memberId)).thenReturn(Optional.of(account(memberId,AccountRole.MEMBER)));
		when(sessions.findByIdForUpdate(session.getId())).thenReturn(Optional.of(session));
		when(disciplines.findBySessionIdForUpdate(session.getId())).thenReturn(Optional.of(session.getSportClass().getDiscipline()));
		when(bookings.countByClassSessionIdAndStatus(session.getId(),BookingStatus.BOOKED)).thenReturn(0L);
		assertThatThrownBy(()->service.join(memberId,session.getId())).isInstanceOfSatisfying(WaitlistException.class,
			error->assertThat(error.getCode()).isEqualTo("SESSION_NOT_FULL"));
	}

	@Test void expirationNotifiesMemberExactlyOnce(){
		SessionWaitlist entry=new SessionWaitlist(UUID.randomUUID(),session.getId(),memberId,memberId,"MEMBER",now.minusSeconds(60));
		when(waitlists.findExpiredForUpdate(now)).thenReturn(List.of(entry),List.of());

		service.expireStarted(now);
		service.expireStarted(now);

		assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
		verify(notifications,times(1)).write("waitlist-expired:"+entry.getId(),memberId,"WAITLIST_EXPIRED",
			"SESSION_WAITLIST",entry.getId(),Map.of("sessionId",session.getId().toString(),
				"reasonCode","SESSION_STARTED"));
	}

	private ClassSession session(int capacity){
		Discipline discipline=new Discipline(UUID.randomUUID(),"Group",null,DisciplineStatus.ACTIVE);
		SportClass type=new SportClass(UUID.randomUUID(),discipline,"Group",SportClassType.GROUP,null,SportClassStatus.ACTIVE);
		return new ClassSession(UUID.randomUUID(),type,null,account(UUID.randomUUID(),AccountRole.COACH),
			new Room(UUID.randomUUID(),"Room",20,RoomStatus.ACTIVE),now.plusSeconds(3600),now.plusSeconds(7200),capacity,UUID.randomUUID());
	}
	private Account account(UUID id,AccountRole role){return new Account(id,role,AccountStatus.ACTIVE,role.name(),"090"+id.toString().substring(0,8),id+"@test.local",LocalDate.of(1990,1,1),"{noop}x");}
	private BookingRepository.BookingBenefitEligibilityView eligibility(UUID membershipId){return new BookingRepository.BookingBenefitEligibilityView(){public UUID getMembershipId(){return membershipId;}public String getRequiredBenefitCode(){return "CLASS_BOOKING";}public boolean getProvided(){return true;}};}
}
