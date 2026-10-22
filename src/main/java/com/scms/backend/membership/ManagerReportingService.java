package com.scms.backend.membership;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ManagerReportingService {

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
