package com.scms.backend.course.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.course.Course;
import com.scms.backend.course.CourseWithdrawalEligibilityReason;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.ReceptionCourseEnrollment;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class ReceptionCourseEnrollmentServiceTests {

	@Mock
	JdbcTemplate db;

	@Mock
	AccountRepository accounts;

	@Test
	void cappedPaidAmountDeterminesReasonAndZeroRefundEstimate() throws Exception {
		UUID receptionist = UUID.randomUUID(), member = UUID.randomUUID(), enrollment = UUID.randomUUID();
		when(accounts.existsByIdAndRoleAndStatus(receptionist, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)).thenReturn(true);
		ResultSet row = mock(ResultSet.class);
		when(row.getInt("remaining_units")).thenReturn(7);
		when(row.getInt("total_units")).thenReturn(10);
		when(row.getString("enrollment_status")).thenReturn("ACTIVE");
		when(row.getString("course_run_status")).thenReturn("CONFIRMED");
		when(row.getBigDecimal("unused_allocation")).thenReturn(BigDecimal.valueOf(100));
		when(row.getBigDecimal("amount_paid_vnd_snapshot")).thenReturn(BigDecimal.ZERO);
		when(row.getObject(anyString(), eq(UUID.class))).thenReturn(UUID.randomUUID());
		when(row.getTimestamp("enrolled_at")).thenReturn(Timestamp.from(Instant.parse("2026-10-30T10:00:00Z")));
		when(row.getString("member_code")).thenReturn("MB-100001");
		when(row.getString("member_name")).thenReturn("Course Member");
		when(row.getString("course_code")).thenReturn("CRS-001");
		when(row.getString("course_name")).thenReturn("Course");
		when(row.getString("run_code")).thenReturn("RUN-001");
		when(db.query(anyString(), ArgumentMatchers.<RowMapper<ReceptionCourseEnrollment>>any(), any(Object[].class))).thenAnswer(invocation -> java.util.List.of(invocation.<RowMapper<ReceptionCourseEnrollment>>getArgument(1).mapRow(row, 0)));
		ReceptionCourseEnrollmentService service = new ReceptionCourseEnrollmentService(db, accounts, Clock.fixed(Instant.parse("2026-10-30T10:00:00Z"), ZoneOffset.UTC));
		ReceptionCourseEnrollment result = service.detail(receptionist, member, enrollment);
		assertThat(result.estimatedRefundAmount()).isZero();
		assertThat(result.withdrawalReason()).isEqualTo(CourseWithdrawalEligibilityReason.ELIGIBLE_WITHOUT_REFUND);
	}
}
