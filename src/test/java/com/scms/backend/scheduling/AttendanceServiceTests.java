package com.scms.backend.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.reception.CenterVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTests {

	@Mock ClassSessionRepository sessions;
	@Mock BookingRepository bookings;
	@Mock AttendanceRepository attendance;
	@Mock AuditEventRepository audits;
	@Mock AccountRepository accounts;
	@Mock CenterVisitRepository visits;
	AttendanceService service;
	private final Instant now = Instant.parse("2026-10-07T03:00:00Z");

	@BeforeEach void setUp() {
		when(accounts.existsByIdAndRoleAndStatus(any(), any(), any())).thenReturn(true);
		service = new AttendanceService(sessions, bookings, attendance, audits, accounts, visits,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test void attendanceListIsReadOnlyAndUsesBookingMemberIdentity() {
		ClassSession session = session(now.minusSeconds(1), now.plusSeconds(3600));
		UUID coachId = session.getTeachingCoach().getId();
		UUID memberId = UUID.randomUUID();
		UUID bookingId = UUID.randomUUID();
		UUID attendanceId = UUID.randomUUID();
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(bookings.findBookedAttendanceMembers(session.getId()))
			.thenReturn(List.of(member(attendanceId, bookingId, memberId)));
		when(attendance.findByBookingId(bookingId)).thenReturn(Optional.of(new Attendance(attendanceId,
			bookingId, AttendanceStatus.ABSENT, session.getStartTime(), null, null, "SYSTEM")));

		var result = service.list(coachId, session.getId());

		assertThat(result).singleElement().satisfies(item -> {
			assertThat(item.memberId()).isEqualTo(memberId);
			assertThat(item.recordedAt()).isEqualTo(session.getStartTime());
		});
		verify(attendance, never()).save(any());
	}

	@Test void coachCannotMarkPresentWithoutCenterVisit() {
		ClassSession session = session(now.minusSeconds(1), now.plusSeconds(3600));
		UUID coachId = session.getTeachingCoach().getId();
		Attendance row = attendance(session.getId());
		UUID memberId = UUID.randomUUID();
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(attendance.findById(row.getId())).thenReturn(Optional.of(row));
		when(bookings.findBookedAttendanceMembers(session.getId())).thenReturn(List.of(new BookingRepository.AttendanceMemberView() {
			public UUID getAttendanceId() { return row.getId(); }
			public UUID getBookingId() { return row.getBookingId(); }
			public UUID getMemberId() { return memberId; }
			public String getMemberCode() { return "MB-1"; }
			public String getFullName() { return "Member"; }
		}));
		when(visits.findOverlappingVisitId(any(), org.mockito.ArgumentMatchers.eq(session.getStartTime()),
			org.mockito.ArgumentMatchers.eq(session.getEndTime()), org.mockito.ArgumentMatchers.eq(now)))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(coachId, session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.PRESENT)))
			.isInstanceOf(AttendanceException.class).extracting("code").isEqualTo("CENTER_VISIT_REQUIRED");
		verify(audits, never()).save(any());
	}

	@Test void attendanceLocksThirtyMinutesAfterSessionEnd() {
		ClassSession session = session(now.minusSeconds(5400), now.minusSeconds(1800));
		UUID coachId = session.getTeachingCoach().getId();
		Attendance row = attendance(session.getId());
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> service.update(coachId, session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.ABSENT)))
			.isInstanceOf(AttendanceException.class).extracting("code").isEqualTo("ATTENDANCE_WINDOW_CLOSED");
	}

	@Test void attendanceUpdateAtExactStartIsAllowed() {
		ClassSession session = session(now, now.plusSeconds(3600));
		UUID coachId = session.getTeachingCoach().getId();
		Attendance row = attendance(session.getId());
		var member = member(row.getId(), row.getBookingId(), UUID.randomUUID());
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(attendance.findById(row.getId())).thenReturn(Optional.of(row));
		when(bookings.findBookedAttendanceMembers(session.getId())).thenReturn(List.of(member));

		AttendanceResponse result = service.update(coachId, session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.ABSENT));

		assertThat(result.memberId()).isEqualTo(member.getMemberId());
	}

	@Test void presentUsesOverlappingVisitForBookedMember() {
		ClassSession session = session(now.minusSeconds(1800), now.plusSeconds(1800));
		UUID coachId = session.getTeachingCoach().getId();
		Attendance row = attendance(session.getId());
		UUID memberId = UUID.randomUUID();
		UUID visitId = UUID.randomUUID();
		var member = member(row.getId(), row.getBookingId(), memberId);
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(attendance.findById(row.getId())).thenReturn(Optional.of(row));
		when(bookings.findBookedAttendanceMembers(session.getId())).thenReturn(List.of(member));
		when(visits.findOverlappingVisitId(memberId, session.getStartTime(), session.getEndTime(), now))
			.thenReturn(Optional.of(visitId));

		AttendanceResponse result = service.update(coachId, session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.PRESENT));

		assertThat(result.centerVisitId()).isEqualTo(visitId);
	}

	@Test void inactiveCoachCannotReadAttendance() {
		UUID coachId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(any(), any(), any())).thenReturn(false);
		assertThatThrownBy(() -> service.list(coachId, UUID.randomUUID()))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	private BookingRepository.AttendanceMemberView member(UUID attendanceId, UUID bookingId, UUID memberId) {
		return new BookingRepository.AttendanceMemberView() {
		public UUID getAttendanceId() { return attendanceId; }
		public UUID getBookingId() { return bookingId; }
		public UUID getMemberId() { return memberId; }
		public String getMemberCode() { return "MB-1"; }
		public String getFullName() { return "Member"; }
	}; }

	private Attendance attendance(UUID sessionId) {
		return new Attendance(UUID.randomUUID(), UUID.randomUUID(), AttendanceStatus.ABSENT,
			now, null, null, "SYSTEM");
	}
	private ClassSession session(Instant start, Instant end) {
		Discipline d=new Discipline(UUID.randomUUID(),"Yoga",null,DisciplineStatus.ACTIVE);
		SportClass c=new SportClass(UUID.randomUUID(),d,"Vinyasa",SportClassType.YOGA,null,SportClassStatus.ACTIVE);
		Room r=new Room(UUID.randomUUID(),"Studio",20,RoomStatus.ACTIVE); UUID id=UUID.randomUUID();
		Account coach=new Account(id,AccountRole.COACH,AccountStatus.ACTIVE,"Coach","0900000001",id+"@test",LocalDate.of(1990,1,1),"pw");
		return new ClassSession(UUID.randomUUID(),c,null,coach,r,start,end,10,UUID.randomUUID());
	}
}
