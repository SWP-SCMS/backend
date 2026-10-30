package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.CourseEnrollmentApiModels.AttendanceUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class CourseEnrollmentServiceTests {
	@Mock JdbcTemplate db;
	@Mock AccountRepository accounts;
	@Mock AuditEventRepository audits;
	UUID coach = UUID.randomUUID();
	UUID session = UUID.randomUUID();
	UUID row = UUID.randomUUID();
	Instant now = Instant.parse("2026-10-30T10:00:00Z");
	CourseEnrollmentService service;

	@BeforeEach void setUp() {
		service = new CourseEnrollmentService(db, accounts, audits, Clock.fixed(now, ZoneOffset.UTC));
		when(accounts.existsByIdAndRoleAndStatus(coach, AccountRole.COACH, AccountStatus.ACTIVE)).thenReturn(true);
	}

	@Test void notRecordedCannotBeWritten() {
		assertThatThrownBy(() -> service.updateAttendance(coach, session, row,
			new AttendanceUpdate(CourseAttendanceStatus.NOT_RECORDED)))
			.isInstanceOf(CourseAttendanceException.class).extracting("code").isEqualTo("VALIDATION_ERROR");
		verify(db, never()).update(anyString(), any(Object[].class));
	}

	@Test void exactStartAllowsAttendanceAndReturnsMemberWithoutCommerceData() {
		when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(session(now, now.plusSeconds(3600))))
			.thenReturn(List.of(roster()));
		when(db.update(anyString(), any(Object[].class))).thenReturn(1);

		var result = service.updateAttendance(coach, session, row,
			new AttendanceUpdate(CourseAttendanceStatus.PRESENT));

		assertThat(result.enrollmentSessionId()).isEqualTo(row);
		assertThat(result.attendanceStatus()).isEqualTo(CourseAttendanceStatus.PRESENT);
		verify(audits).save(any());
	}

	@Test void exactEndPlusThirtyIsClosed() {
		when(db.queryForList(anyString(), any(Object[].class)))
			.thenReturn(List.of(session(now.minusSeconds(5400), now.minusSeconds(1800))));
		assertThatThrownBy(() -> service.updateAttendance(coach, session, row,
			new AttendanceUpdate(CourseAttendanceStatus.ABSENT)))
			.isInstanceOf(CourseAttendanceException.class).extracting("code")
			.isEqualTo("COURSE_ATTENDANCE_WINDOW_CLOSED");
		verify(db, never()).update(anyString(), any(Object[].class));
	}

	private Map<String, Object> session(Instant start, Instant end) {
		return Map.of("id", session, "start_at", Timestamp.from(start), "end_at", Timestamp.from(end),
			"status", "SCHEDULED");
	}

	private Map<String, Object> roster() {
		return Map.of("id", row, "member_account_id", UUID.randomUUID(), "member_code", "MB-100001",
			"full_name", "Course Member", "attendance_status", "NOT_RECORDED");
	}
}
