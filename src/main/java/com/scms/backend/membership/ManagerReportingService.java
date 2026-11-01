package com.scms.backend.membership;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.course.CourseRunStatus;

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
	}

	@Transactional
	ManagerDashboardResponse dashboard(UUID actor, Instant from, Instant to, boolean includeTestData) {
		validate(actor, from, to);
		return new ManagerDashboardResponse(from, to, membersData(from, to, includeTestData),
			payments.report(actor, from, to, includeTestData), bookingsData(from, to, includeTestData),
			attendanceData(from, to, includeTestData), visitsData(from, to, includeTestData),
			utilizationData(from, to, includeTestData), includeTestData);
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
	ClassUtilizationReportResponse classUtilization(UUID actor, Instant from, Instant to,
			boolean includeTestData) {
		validate(actor, from, to);
		return utilizationData(from, to, includeTestData);
	}

	@Transactional(readOnly = true)
	CourseRunReportPageResponse courseRuns(UUID actor, UUID runId, CourseRunStatus status,
			LocalDate startDate, LocalDate endDate, int page, int size) {
		validateCourseRunReport(actor, startDate, endDate, page, size);
		Instant from = startDate == null ? null : startDate.atStartOfDay(BUSINESS_ZONE).toInstant();
		Instant to = endDate == null ? null : endDate.atStartOfDay(BUSINESS_ZONE).toInstant();
		List<Object> filters = new ArrayList<>();
		String where = courseRunWhere(runId, status, from, to, filters);
		Long count = db.queryForObject("select count(*) from course_runs run " + where,
			Long.class, filters.toArray());
		long total = count == null ? 0 : count;
		List<Object> queryArgs = new ArrayList<>(filters);
		queryArgs.add(size);
		queryArgs.add((long)page * size);
		List<CourseRunReportItem> content = db.query(courseRunReportSql(where), (result, row) ->
			new CourseRunReportItem(result.getObject("run_id", UUID.class), result.getString("run_code"),
				result.getObject("course_id", UUID.class), result.getString("course_code"),
				result.getString("course_name"), result.getString("status"),
				result.getTimestamp("planned_start_at").toInstant(),
				result.getTimestamp("planned_end_at").toInstant(), integer(result, "tuition_vnd"),
				result.getLong("active_enrollments"), result.getLong("transferred_enrollments"),
				result.getLong("completed_enrollments"), result.getLong("withdrawal_pending_enrollments"),
				result.getLong("withdrawn_enrollments"), result.getLong("attendance_present"),
				result.getLong("attendance_absent"), result.getLong("attendance_not_recorded"),
				integer(result, "gross_amount_vnd"), integer(result, "paid_amount_vnd"),
				result.getLong("pending_payments"), integer(result, "pending_payment_amount_vnd"),
				result.getLong("failed_payments"), integer(result, "failed_payment_amount_vnd"),
				result.getLong("outstanding_refund_obligations"), integer(result, "outstanding_refund_amount_vnd"),
				result.getLong("completed_refunds"), integer(result, "completed_refund_amount_vnd"),
				result.getLong("waitlist_waiting"), result.getLong("waitlist_invited"),
				result.getLong("waitlist_accepted"), result.getLong("transfer_pending"),
				result.getLong("transfer_offered"), result.getLong("transfer_rejected"),
				result.getLong("transfer_completed"), result.getLong("transfer_expired")),
			queryArgs.toArray());
		int totalPages = total == 0 ? 0 : (int)Math.min(Integer.MAX_VALUE, Math.ceilDiv(total, (long)size));
		return new CourseRunReportPageResponse(content, page, size, total, totalPages);
	}

	private void validateCourseRunReport(UUID actor, LocalDate startDate, LocalDate endDate, int page, int size) {
		validate(actor, null, null);
		if (page < 0) throw new ReportingValidationException("page", "must be greater than or equal to 0");
		if (size < 1 || size > MAX_COURSE_REPORT_PAGE_SIZE)
			throw new ReportingValidationException("size", "must be between 1 and 100");
		if (startDate != null && endDate != null) {
			if (!startDate.isBefore(endDate))
				throw new ReportingValidationException("endDate", "must be later than startDate");
			if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_COURSE_REPORT_DAYS)
				throw new ReportingValidationException("endDate", "must not span more than 366 days");
		}
	}

	private String courseRunWhere(UUID runId, CourseRunStatus status, Instant from, Instant to,
			List<Object> args) {
		StringBuilder where = new StringBuilder("where true");
		if (runId != null) { where.append(" and run.id=?"); args.add(runId); }
		if (status != null) { where.append(" and run.status=?"); args.add(status.name()); }
		if (from != null) { where.append(" and run.planned_start_at>=?"); args.add(Timestamp.from(from)); }
		if (to != null) { where.append(" and run.planned_start_at<?"); args.add(Timestamp.from(to)); }
		return where.toString();
	}

	private String courseRunReportSql(String where) {
		return """
			with page_runs as (
			 select run.* from course_runs run %s
			 order by run.planned_start_at desc,run.id desc limit ? offset ?
			), enrollments as (
			 select course_run_id,
			  count(*) filter(where lifecycle_status='ACTIVE') active_enrollments,
			  count(*) filter(where lifecycle_status='TRANSFERRED') transferred_enrollments,
			  count(*) filter(where lifecycle_status='COMPLETED') completed_enrollments,
			  count(*) filter(where lifecycle_status='WITHDRAWAL_PENDING') withdrawal_pending_enrollments,
			  count(*) filter(where lifecycle_status='WITHDRAWN') withdrawn_enrollments
			 from course_enrollments where course_run_id in (select id from page_runs) group by course_run_id
			), attendance as (
			 select enrollment.course_run_id,
			  count(*) filter(where session.attendance_status='PRESENT') attendance_present,
			  count(*) filter(where session.attendance_status='ABSENT') attendance_absent,
			  count(*) filter(where session.attendance_status='NOT_RECORDED') attendance_not_recorded
			 from course_enrollment_sessions session join course_enrollments enrollment
			  on enrollment.id=session.course_enrollment_id
			 where enrollment.course_run_id in (select id from page_runs) group by enrollment.course_run_id
			), money as (
			 select orders.course_run_id,coalesce(sum(orders.tuition_vnd_snapshot),0) gross_amount_vnd,
			  coalesce(sum(payment.amount) filter(where payment.status='PAID'),0) paid_amount_vnd,
			  count(payment.id) filter(where payment.status='PENDING') pending_payments,
			  coalesce(sum(payment.amount) filter(where payment.status='PENDING'),0) pending_payment_amount_vnd,
			  count(payment.id) filter(where payment.status='FAILED') failed_payments,
			  coalesce(sum(payment.amount) filter(where payment.status='FAILED'),0) failed_payment_amount_vnd
			 from course_orders orders left join course_payments payment on payment.course_order_id=orders.id
			 where orders.course_run_id in (select id from page_runs) group by orders.course_run_id
			), refunds as (
			 select course_run_id,
			  count(*) filter(where status in ('PENDING_REVIEW','APPROVED','DISBURSEMENT_RECORDED','PENDING_VERIFICATION')) outstanding_refund_obligations,
			  coalesce(sum(coalesce(approved_amount_vnd,amount_vnd)-verified_amount_vnd)
			   filter(where status in ('PENDING_REVIEW','APPROVED','DISBURSEMENT_RECORDED','PENDING_VERIFICATION')),0) outstanding_refund_amount_vnd,
			  count(*) filter(where status='COMPLETED') completed_refunds,
			  coalesce(sum(verified_amount_vnd) filter(where status='COMPLETED'),0) completed_refund_amount_vnd
			 from course_refund_obligations where course_run_id in (select id from page_runs) group by course_run_id
			), waitlists as (
			 select course_run_id,count(*) filter(where status='WAITING') waitlist_waiting,
			  count(*) filter(where status='INVITED') waitlist_invited,
			  count(*) filter(where status='ACCEPTED') waitlist_accepted
			 from course_waitlists where course_run_id in (select id from page_runs) group by course_run_id
			), transfers as (
			 select source_run_id course_run_id,count(*) filter(where status='PENDING') transfer_pending,
			  count(*) filter(where status='OFFERED') transfer_offered,
			  count(*) filter(where status='REJECTED') transfer_rejected,
			  count(*) filter(where status='COMPLETED') transfer_completed,
			  count(*) filter(where status='EXPIRED') transfer_expired
			 from course_transfer_requests where source_run_id in (select id from page_runs) group by source_run_id
			)
			select run.id run_id,run.run_code,run.course_id,course.code course_code,course.name course_name,
			 run.status,run.planned_start_at,run.planned_end_at,run.tuition_vnd,
			 coalesce(enrollment.active_enrollments,0) active_enrollments,
			 coalesce(enrollment.transferred_enrollments,0) transferred_enrollments,
			 coalesce(enrollment.completed_enrollments,0) completed_enrollments,
			 coalesce(enrollment.withdrawal_pending_enrollments,0) withdrawal_pending_enrollments,
			 coalesce(enrollment.withdrawn_enrollments,0) withdrawn_enrollments,
			 coalesce(attendance.attendance_present,0) attendance_present,
			 coalesce(attendance.attendance_absent,0) attendance_absent,
			 coalesce(attendance.attendance_not_recorded,0) attendance_not_recorded,
			 coalesce(money.gross_amount_vnd,0) gross_amount_vnd,coalesce(money.paid_amount_vnd,0) paid_amount_vnd,
			 coalesce(money.pending_payments,0) pending_payments,
			 coalesce(money.pending_payment_amount_vnd,0) pending_payment_amount_vnd,
			 coalesce(money.failed_payments,0) failed_payments,
			 coalesce(money.failed_payment_amount_vnd,0) failed_payment_amount_vnd,
			 coalesce(refund.outstanding_refund_obligations,0) outstanding_refund_obligations,
			 coalesce(refund.outstanding_refund_amount_vnd,0) outstanding_refund_amount_vnd,
			 coalesce(refund.completed_refunds,0) completed_refunds,
			 coalesce(refund.completed_refund_amount_vnd,0) completed_refund_amount_vnd,
			 coalesce(waitlist.waitlist_waiting,0) waitlist_waiting,
			 coalesce(waitlist.waitlist_invited,0) waitlist_invited,
			 coalesce(waitlist.waitlist_accepted,0) waitlist_accepted,
			 coalesce(transfer.transfer_pending,0) transfer_pending,
			 coalesce(transfer.transfer_offered,0) transfer_offered,
			 coalesce(transfer.transfer_rejected,0) transfer_rejected,
			 coalesce(transfer.transfer_completed,0) transfer_completed,
			 coalesce(transfer.transfer_expired,0) transfer_expired
			from page_runs run join courses course on course.id=run.course_id
			left join enrollments enrollment on enrollment.course_run_id=run.id
			left join attendance on attendance.course_run_id=run.id left join money on money.course_run_id=run.id
			left join refunds refund on refund.course_run_id=run.id
			left join waitlists waitlist on waitlist.course_run_id=run.id
			left join transfers transfer on transfer.course_run_id=run.id
			order by run.planned_start_at desc,run.id desc
			""".formatted(where);
	}

	private BigInteger integer(java.sql.ResultSet result, String column) throws java.sql.SQLException {
		return result.getBigDecimal(column).toBigIntegerExact();
	}

	private MemberReportResponse membersData(Instant from, Instant to, boolean includeTestData) {
		String range = range("a.created_at", from, to);
		String test = includeTestData ? "" : " and not a.is_test_data";
		List<Object> accountArgs = args(from, to);
		Map<String, Object> member = db.queryForMap("select count(*) total, "
			+ "count(*) filter(where a.status='ACTIVE') active, "
			+ "count(*) filter(where a.status='SUSPENDED') suspended from accounts a "
			+ "where a.role='MEMBER'" + range + test, accountArgs.toArray());
		List<Object> membershipArgs = args(from, to);
		Map<String, Object> membership = db.queryForMap("select "
			+ "count(*) filter(where m.status='ACTIVE' and m.starts_at <= current_timestamp and m.ends_at > current_timestamp) active, "
			+ "count(*) filter(where m.status='EXPIRED' or (m.status='ACTIVE' and m.ends_at <= current_timestamp)) expired from memberships m "
			+ "join accounts a on a.id=m.member_account_id where true"
			+ range("m.created_at", from, to) + test, membershipArgs.toArray());
		return new MemberReportResponse(from, to, number(member, "total"), number(member, "active"),
			number(member, "suspended"), number(membership, "active"), number(membership, "expired"),
			includeTestData);
	}

	private BookingReportResponse bookingsData(Instant from, Instant to, boolean includeTestData) {
		String test = includeTestData ? "" : " and not a.is_test_data";
		Map<String, Object> row = db.queryForMap("select count(*) total, "
			+ "count(*) filter(where b.status='BOOKED') booked, "
			+ "count(*) filter(where b.status='CANCELLED') cancelled from bookings b "
			+ "join accounts a on a.id=b.member_account_id where true"
			+ range("b.created_at", from, to) + test, args(from, to).toArray());
		return new BookingReportResponse(from, to, number(row, "total"), number(row, "booked"),
			number(row, "cancelled"), includeTestData);
	}

	private AttendanceReportResponse attendanceData(Instant from, Instant to, boolean includeTestData) {
		String test = includeTestData ? "" : " and exists (select 1 from accounts member "
			+ "where member.id=b.member_account_id and not member.is_test_data)";
		List<Object> args = args(from, to);
		args.add(Timestamp.from(clock.instant()));
		Map<String, Object> row = db.queryForMap("select count(distinct cs.id) reportable_sessions, "
			+ "count(*) filter(where att.status='PRESENT') present, "
			+ "count(*) filter(where att.status='ABSENT') absent from class_sessions cs "
			+ "left join bookings b on b.class_session_id=cs.id and b.status='BOOKED'" + test + " "
			+ "left join attendance att on att.booking_id=b.id "
			+ "where cs.status<>'CANCELLED'" + range("cs.start_time", from, to)
			+ " and cs.end_time + interval '30 minutes' <= ?", args.toArray());
		long present = number(row, "present");
		long absent = number(row, "absent");
		return new AttendanceReportResponse(from, to, number(row, "reportable_sessions"), present, absent,
			rate(present, present + absent), includeTestData);
	}

	private CenterVisitReportResponse visitsData(Instant from, Instant to, boolean includeTestData) {
		String test = includeTestData ? "" : " and not member.is_test_data";
		Map<String, Object> row = db.queryForMap("select count(*) total, "
			+ "count(distinct v.member_account_id) unique_members, "
			+ "count(*) filter(where v.checked_out_at is null) open from center_visits v "
			+ "join accounts member on member.id=v.member_account_id where true"
			+ range("v.checked_in_at", from, to) + test, args(from, to).toArray());
		return new CenterVisitReportResponse(from, to, number(row, "total"), number(row, "unique_members"),
			number(row, "open"), includeTestData);
	}

	private ClassUtilizationReportResponse utilizationData(Instant from, Instant to, boolean includeTestData) {
		String bookingTest = includeTestData ? "" : " and exists (select 1 from accounts member "
			+ "where member.id=b.member_account_id and not member.is_test_data)";
		List<Object> queryArgs = new ArrayList<>();
		queryArgs.add(Timestamp.from(clock.instant()));
		queryArgs.addAll(args(from, to));
		Map<String, Object> row = db.queryForMap("""
			with session_stats as (
			 select cs.id,cs.status,cs.capacity,(cs.status<>'CANCELLED' and cs.end_time + interval '30 minutes' <= ?) reportable,
			 count(b.id) filter(where b.status='BOOKED') booked,
			 count(b.id) filter(where b.status='CANCELLED') cancelled,
			 count(att.id) filter(where b.status='BOOKED' and att.status='PRESENT') present,
			 count(att.id) filter(where b.status='BOOKED' and att.status='ABSENT') absent
			 from class_sessions cs
			 left join bookings b on b.class_session_id=cs.id %s
			 left join attendance att on att.booking_id=b.id
			 where true %s
			 group by cs.id
			)
			select count(*) total_sessions,count(*) filter(where status='CANCELLED') cancelled_sessions,
			 count(*) filter(where reportable) reportable_sessions,
			 coalesce(sum(capacity) filter(where reportable),0) total_capacity,
			 coalesce(sum(booked),0) booked,coalesce(sum(cancelled),0) cancelled,
			 coalesce(sum(present) filter(where reportable),0) present,
			 coalesce(sum(absent) filter(where reportable),0) absent from session_stats
			""".formatted(bookingTest, range("cs.start_time", from, to)), queryArgs.toArray());
		long present = number(row, "present");
		long capacity = number(row, "total_capacity");
		return new ClassUtilizationReportResponse(from, to, number(row, "total_sessions"),
			number(row, "cancelled_sessions"), number(row, "reportable_sessions"), capacity,
			number(row, "booked"), number(row, "cancelled"), present, number(row, "absent"),
			rate(present, capacity), includeTestData);
	}

	private void validate(UUID actor, Instant from, Instant to) {
		if (!accounts.existsByIdAndRoleAndStatus(actor, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
		if (from != null && to != null && !from.isBefore(to)) {
			throw new ReportingValidationException("to", "must be later than from");
		}
	}

	private String range(String column, Instant from, Instant to) {
		return (from == null ? "" : " and " + column + ">=?") + (to == null ? "" : " and " + column + "<?");
	}

	private List<Object> args(Instant from, Instant to) {
		List<Object> args = new ArrayList<>();
		if (from != null) args.add(Timestamp.from(from));
		if (to != null) args.add(Timestamp.from(to));
		return args;
	}

	private long number(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).longValue();
	}

	private BigDecimal rate(long numerator, long denominator) {
		return denominator == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(numerator)
			.divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
	}
}
