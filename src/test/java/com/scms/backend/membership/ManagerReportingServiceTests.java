package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class ManagerReportingServiceTests {

	private final JdbcTemplate db = mock(JdbcTemplate.class);
	private final AccountRepository accounts = mock(AccountRepository.class);
	private final PaymentService payments = mock(PaymentService.class);
	private final Instant now = Instant.parse("2026-10-12T00:00:00Z");
	private final ManagerReportingService service = new ManagerReportingService(db, accounts, payments,
		Clock.fixed(now, ZoneOffset.UTC));
	private final UUID managerId = UUID.randomUUID();

	@Test
	void rejectsInactiveOrNonManagerActorBeforeReadingReports() {
		assertThatThrownBy(() -> service.members(managerId, null, null, false))
			.isInstanceOf(InvalidAuthenticatedAccountException.class);

		verify(db, never()).queryForMap(anyString(), any(Object[].class));
	}

	@Test
	void rejectsNonIncreasingHalfOpenRangeBeforeReadingReports() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);

		assertThatThrownBy(() -> service.bookings(managerId, now, now, false))
			.isInstanceOf(ReportingValidationException.class)
			.hasMessage("must be later than from");

		verify(db, never()).queryForMap(anyString(), any(Object[].class));
	}

	@Test
	void attendanceRateUsesFinalAttendanceCounts() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of(
			"reportable_sessions", 2L, "present", 3L, "absent", 1L));

		AttendanceReportResponse result = service.attendance(managerId, null, null, false);

		assertThat(result.participationRate()).isEqualByComparingTo(new BigDecimal("0.7500"));
		assertThat(result.present()).isEqualTo(3);
		assertThat(result.absent()).isEqualTo(1);
	}

	@Test
	void classUtilizationUsesPresentAttendanceAgainstReportableCapacity() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForMap(anyString(), any(Object[].class))).thenReturn(Map.of(
			"total_sessions", 3L, "cancelled_sessions", 1L, "reportable_sessions", 2L,
			"total_capacity", 10L, "booked", 5L, "cancelled", 2L, "present", 4L, "absent", 1L));

		ClassUtilizationReportResponse result = service.classUtilization(managerId, null, null, false);

		assertThat(result.utilizationRate()).isEqualByComparingTo(new BigDecimal("0.4000"));
		assertThat(result.present()).isEqualTo(4);
		assertThat(result.totalCapacity()).isEqualTo(10);
	}

	@Test
	void courseRunReportRejectsRangesWiderThanOneYearBeforeReadingData() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);

		assertThatThrownBy(() -> service.courseRuns(managerId, null, null,
			LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 3), 0, 20))
			.isInstanceOf(ReportingValidationException.class)
			.hasMessage("must not span more than 366 days");

		verify(db, never()).queryForObject(anyString(), eq(Long.class), any(Object[].class));
	}

	@Test
	void courseRunReportRejectsUnboundedPageSize() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);

		assertThatThrownBy(() -> service.courseRuns(managerId, null, null, null, null, 0, 101))
			.isInstanceOf(ReportingValidationException.class)
			.hasMessage("must be between 1 and 100");
	}

	@Test
	void courseRunReportUsesTwoQueriesRegardlessOfPageSizeAndReturnsEmptyPage() {
		when(accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE))
			.thenReturn(true);
		when(db.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(0L);
		when(db.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(List.of());

		CourseRunReportPageResponse result = service.courseRuns(managerId, null, null, null, null, 4, 100);

		assertThat(result.content()).isEmpty();
		assertThat(result.totalElements()).isZero();
		assertThat(result.totalPages()).isZero();
		verify(db).queryForObject(anyString(), eq(Long.class), any(Object[].class));
		verify(db).query(anyString(), any(RowMapper.class), any(Object[].class));
	}
}
