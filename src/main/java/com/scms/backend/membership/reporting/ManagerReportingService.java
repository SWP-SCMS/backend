package com.scms.backend.membership.reporting;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseRunStatus;
import com.scms.backend.membership.ReportingValidationException;
import com.scms.backend.membership.payment.PaymentService;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ManagerReportingService {

	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	private static final int MAX_COURSE_REPORT_PAGE_SIZE = 100;

	private static final long MAX_COURSE_REPORT_DAYS = 366;

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final PaymentService payments;

	private final Clock clock;

	ManagerReportingService(JdbcTemplate db, AccountRepository accounts, PaymentService payments, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.payments = payments;
		this.clock = clock;
		this.managerReportingRepository = new ManagerReportingRepository(this.db, this.clock);
	}

	@Transactional
	ManagerDashboardResponse dashboard(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return new ManagerDashboardResponse(from, to, membersData(from, to, includeTestData), payments.report(actor, from, to, includeTestData), bookingsData(from, to, includeTestData), attendanceData(from, to, includeTestData), visitsData(from, to, includeTestData), utilizationData(from, to, includeTestData), includeTestData);
	}

	@Transactional
	MemberReportResponse members(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return membersData(from, to, includeTestData);
	}

	@Transactional
	BookingReportResponse bookings(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return bookingsData(from, to, includeTestData);
	}

	@Transactional
	AttendanceReportResponse attendance(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return attendanceData(from, to, includeTestData);
	}

	@Transactional
	CenterVisitReportResponse centerVisits(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return visitsData(from, to, includeTestData);
	}

	@Transactional
	ClassUtilizationReportResponse classUtilization(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return utilizationData(from, to, includeTestData);
	}

	@Transactional(readOnly = true)
	CourseRunReportPageResponse courseRuns(UUID actor, UUID runId, CourseRunStatus status, LocalDate startDate, LocalDate endDate, int page, int size) {
		validateCourseRunReport(actor, startDate, endDate, page, size);
		Instant from = startDate == null ? null : startDate.atStartOfDay(BUSINESS_ZONE).toInstant();
		Instant to = endDate == null ? null : endDate.atStartOfDay(BUSINESS_ZONE).toInstant();
		return courseRunRows(runId, status, page, size, from, to);
	}

	private void validateCourseRunReport(UUID actor, LocalDate startDate, LocalDate endDate, int page, int size) {
		validate(actor, null, null);
		if (page < 0)
			throw new ReportingValidationException("page", "must be greater than or equal to 0");
		if (size < 1 || size > MAX_COURSE_REPORT_PAGE_SIZE)
			throw new ReportingValidationException("size", "must be between 1 and 100");
		if (startDate != null && endDate != null) {
			if (!startDate.isBefore(endDate))
				throw new ReportingValidationException("endDate", "must be later than startDate");
			if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_COURSE_REPORT_DAYS)
				throw new ReportingValidationException("endDate", "must not span more than 366 days");
		}
	}

	private BigInteger integer(java.sql.ResultSet result, String column) throws java.sql.SQLException {
		return managerReportingRepository.integer(result, column);
	}

	private MemberReportResponse membersData(Instant from, Instant to, boolean includeTestData) {
		return managerReportingRepository.membersData(from, to, includeTestData);
	}

	private BookingReportResponse bookingsData(Instant from, Instant to, boolean includeTestData) {
		return managerReportingRepository.bookingsData(from, to, includeTestData);
	}

	private AttendanceReportResponse attendanceData(Instant from, Instant to, boolean includeTestData) {
		return managerReportingRepository.attendanceData(from, to, includeTestData);
	}

	private CenterVisitReportResponse visitsData(Instant from, Instant to, boolean includeTestData) {
		return managerReportingRepository.visitsData(from, to, includeTestData);
	}

	private ClassUtilizationReportResponse utilizationData(Instant from, Instant to, boolean includeTestData) {
		return managerReportingRepository.utilizationData(from, to, includeTestData);
	}

	private void validate(UUID actor, Instant from, Instant to) {
		if (!accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		if (from != null && to != null && !from.isBefore(to)) {
			throw new ReportingValidationException("to", "must be later than from");
		}
	}

	private CourseRunReportPageResponse courseRunRows(UUID runId, CourseRunStatus status, int page, int size, Instant from, Instant to) {
		return managerReportingRepository.courseRunRows(runId, status, page, size, from, to);
	}

	private final ManagerReportingRepository managerReportingRepository;
}
