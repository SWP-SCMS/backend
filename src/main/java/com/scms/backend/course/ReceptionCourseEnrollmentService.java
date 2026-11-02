package com.scms.backend.course;

import static com.scms.backend.course.CourseEnrollmentApiModels.ReceptionCourseEnrollment;
import static com.scms.backend.course.CourseEnrollmentApiModels.ReceptionCourseEnrollmentPage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReceptionCourseEnrollmentService {
	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final Clock clock;

	ReceptionCourseEnrollmentService(JdbcTemplate db, AccountRepository accounts, Clock clock) {
		this.db = db; this.accounts = accounts; this.clock = clock;
	}

	@Transactional(readOnly = true)
	ReceptionCourseEnrollmentPage list(UUID receptionistId, UUID memberId, int page, int size,
			String status, Boolean eligible) {
		active(receptionistId);
		page(page, size);
		String normalizedStatus = status(status);
		String source = "select * from (" + select()
			+ " where a.id=? and (cast(? as varchar) is null or e.lifecycle_status=?)) enrollment "
			+ "where (cast(? as boolean) is null or withdrawal_eligible=?)";
		Timestamp now = Timestamp.from(clock.instant());
		long total = db.queryForObject("select count(*) from (" + source + ") e", Long.class,
			now, now, now, memberId, normalizedStatus, normalizedStatus, eligible, eligible);
		List<ReceptionCourseEnrollment> content = db.query(source + " order by next_session,course_run_id,enrollment_id limit ? offset ?",
			this::map, now, now, now, memberId, normalizedStatus, normalizedStatus, eligible, eligible, size, (long) page * size);
		return new ReceptionCourseEnrollmentPage(content, page, size, total, (int) ((total + size - 1) / size));
	}

	@Transactional(readOnly = true)
	ReceptionCourseEnrollment detail(UUID receptionistId, UUID memberId, UUID enrollmentId) {
		active(receptionistId);
		Timestamp now = Timestamp.from(clock.instant());
		return db.query(select() + " where a.id=? and e.id=?", this::map,
			now, now, now, memberId, enrollmentId)
			.stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	private String select() {
		return "select a.id member_id,mp.member_code,a.full_name member_name,c.id course_id,c.code course_code,c.name course_name,"
			+ "r.id course_run_id,r.run_code,r.status course_run_status,e.id enrollment_id,e.lifecycle_status enrollment_status,"
			+ "e.enrolled_at,e.total_units_snapshot total_units,e.amount_paid_vnd_snapshot,"
			+ "coalesce(u.remaining_units,0) remaining_units,coalesce(u.unused_allocation,0) unused_allocation,"
			+ "u.next_session, (e.lifecycle_status='ACTIVE' and r.status in ('OPEN_FOR_REGISTRATION','CONFIRMED','IN_PROGRESS') "
			+ "and coalesce(u.remaining_units,0)>0) withdrawal_eligible,"
			+ "case when e.lifecycle_status<>'ACTIVE' then 'ENROLLMENT_NOT_ACTIVE' "
			+ "when r.status not in ('OPEN_FOR_REGISTRATION','CONFIRMED','IN_PROGRESS') then 'COURSE_RUN_NOT_WITHDRAWABLE' "
			+ "when coalesce(u.remaining_units,0)=0 then 'FINAL_UNIT_ALREADY_STARTED' "
			+ "when coalesce(u.remaining_units,0)*100 < e.total_units_snapshot*70 then 'ELIGIBLE_WITHOUT_REFUND' "
			+ "else 'ELIGIBLE_WITH_REFUND' end withdrawal_reason "
			+ "from course_enrollments e join accounts a on a.id=e.member_account_id "
			+ "join member_profiles mp on mp.account_id=a.id join course_runs r on r.id=e.course_run_id "
			+ "join courses c on c.id=r.course_id left join lateral (select count(*) filter(where s.start_at>?) remaining_units, "
			+ "coalesce(sum(es.allocation_vnd) filter(where s.start_at>?),0) unused_allocation, "
			+ "min(s.start_at) filter(where s.start_at>?) next_session from course_enrollment_sessions es "
			+ "join course_sessions s on s.id=es.course_session_id where es.course_enrollment_id=e.id) u on true";
	}

	private ReceptionCourseEnrollment map(ResultSet rs, int row) throws SQLException {
		int remaining = rs.getInt("remaining_units"), total = rs.getInt("total_units");
		String lifecycle = rs.getString("enrollment_status"), runStatus = rs.getString("course_run_status");
		boolean runWithdrawable = List.of("OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS").contains(runStatus);
		boolean eligible = "ACTIVE".equals(lifecycle) && runWithdrawable
			&& CourseWithdrawalPolicy.canWithdraw(remaining, total);
		BigInteger unusedAllocation = amount(rs.getBigDecimal("unused_allocation"))
			.min(amount(rs.getBigDecimal("amount_paid_vnd_snapshot")));
		BigInteger refund = "ACTIVE".equals(lifecycle) && runWithdrawable && remaining != 0
			? CourseWithdrawalPolicy.refund(remaining, total, unusedAllocation) : null;
		CourseWithdrawalEligibilityReason reason = CourseWithdrawalEligibilityReason.valueOf(!"ACTIVE".equals(lifecycle) ? "ENROLLMENT_NOT_ACTIVE"
			: !runWithdrawable ? "COURSE_RUN_NOT_WITHDRAWABLE"
			: remaining == 0 ? "FINAL_UNIT_ALREADY_STARTED"
			: refund.signum() == 0
				? "ELIGIBLE_WITHOUT_REFUND" : "ELIGIBLE_WITH_REFUND");
		return new ReceptionCourseEnrollment(rs.getObject("member_id", UUID.class), rs.getString("member_code"),
			rs.getString("member_name"), rs.getObject("course_id", UUID.class), rs.getString("course_code"),
			rs.getString("course_name"), rs.getObject("course_run_id", UUID.class), rs.getString("run_code"),
			rs.getString("course_run_status"), rs.getObject("enrollment_id", UUID.class),
			lifecycle, rs.getTimestamp("enrolled_at").toInstant(), total, remaining, eligible, reason, eligible ? refund : null, "WITHDRAWAL_PENDING".equals(lifecycle));
	}
	private BigInteger amount(BigDecimal value) { return value == null ? BigInteger.ZERO : value.toBigIntegerExact(); }
	private String status(String value) {
		if (value == null || value.isBlank()) return null;
		String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
		if (!Set.of("ACTIVE", "TRANSFERRED", "COMPLETED", "WITHDRAWAL_PENDING", "WITHDRAWN").contains(normalized))
			throw CourseException.validation("status", "Unknown enrollment status.");
		return normalized;
	}
	private void active(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}
	private void page(int page, int size) {
		if (page < 0 || size < 1 || size > 100) throw CourseException.validation("page", "Invalid page or size.");
	}
}
