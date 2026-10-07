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
	AttendanceService service;
	private final Instant now = Instant.parse("2026-10-07T03:00:00Z");

	@BeforeEach void setUp() {
		when(accounts.existsByIdAndRoleAndStatus(any(), any(), any())).thenReturn(true);
		service = new AttendanceService(sessions, bookings, attendance, audits, accounts,
			Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test void coachGetsAbsentDefaultsForBookedMembersAtSessionStart() {
		ClassSession session = session(now.minusSeconds(1), now.plusSeconds(3600));
		UUID coachId = session.getTeachingCoach().getId();
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(bookings.findBookedAttendanceMembers(session.getId())).thenReturn(List.of(member(UUID.randomUUID())));
		when(attendance.findByBookingId(any())).thenAnswer(inv -> Optional.of(new Attendance(UUID.randomUUID(), inv.getArgument(0), UUID.randomUUID(), AttendanceStatus.ABSENT, now, null, null, "SYSTEM")));
		when(attendance.save(any())).thenAnswer(inv -> inv.getArgument(0));

		var result = service.list(coachId, session.getId());

		assertThat(result).singleElement().extracting(AttendanceResponse::status)
			.isEqualTo(AttendanceStatus.ABSENT);
		verify(attendance).save(any());
	}

	@Test void coachCannotMarkPresentWithoutCenterVisit() {
		ClassSession session = session(now.minusSeconds(1), now.plusSeconds(3600));
		UUID coachId = session.getTeachingCoach().getId();
		Attendance row = attendance(session.getId());
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));
		when(attendance.findById(row.getId())).thenReturn(Optional.of(row));
		when(bookings.findBookedAttendanceMembers(session.getId())).thenReturn(List.of(new BookingRepository.AttendanceMemberView() {
			public UUID getAttendanceId() { return row.getId(); }
			public UUID getBookingId() { return row.getBookingId(); }
			public UUID getMemberId() { return row.getMemberId(); }
			public String getMemberCode() { return "MB-1"; }
			public String getFullName() { return "Member"; }
		}));
		when(attendance.findCurrentCenterVisit(row.getMemberId(), session.getStartTime(), now)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(coachId, session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.PRESENT)))
			.isInstanceOf(AttendanceException.class).extracting("code").isEqualTo("CENTER_VISIT_REQUIRED");
		verify(audits, never()).save(any());
	}

	@Test void attendanceLocksThirtyMinutesAfterSessionEnd() {
		ClassSession session = session(now.minusSeconds(3601), now.minusSeconds(1801));
		UUID coachId = session.getTeachingCoach().getId();
		Attendance row = attendance(session.getId());
		when(sessions.findByIdAndTeachingCoach_Id(session.getId(), coachId)).thenReturn(Optional.of(session));

		assertThatThrownBy(() -> service.update(coachId, session.getId(), row.getId(),
			new AttendanceUpdateRequest(AttendanceStatus.ABSENT)))
			.isInstanceOf(AttendanceException.class).extracting("code").isEqualTo("ATTENDANCE_WINDOW_CLOSED");
	}

	@Test void inactiveCoachCannotReadAttendance() {
		UUID coachId = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(any(), any(), any())).thenReturn(false);
		assertThatThrownBy(() -> service.list(coachId, UUID.randomUUID()))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);
	}

	private BookingRepository.AttendanceMemberView member(UUID id) { return new BookingRepository.AttendanceMemberView() {
		public UUID getAttendanceId() { return null; }
		public UUID getBookingId() { return id; }
		public UUID getMemberId() { return id; }
		public String getMemberCode() { return "MB-1"; }
		public String getFullName() { return "Member"; }
	}; }

	private Attendance attendance(UUID sessionId) {
		return new Attendance(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), AttendanceStatus.ABSENT,
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
