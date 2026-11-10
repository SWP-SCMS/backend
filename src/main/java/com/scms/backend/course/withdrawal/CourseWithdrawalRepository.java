package com.scms.backend.course.withdrawal;

import static com.scms.backend.course.withdrawal.CourseWithdrawalApiModels.WithdrawalRequest;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.CourseWithdrawalException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseWithdrawalRepository {

	private final JdbcTemplate db;

	CourseWithdrawalRepository(JdbcTemplate db) {
		this.db = db;
	}

	String lockRun(UUID runId) {
		return db.queryForObject("select status from course_runs where id=? for update", String.class, runId);
	}

	Map<String, Object> lockEnrollment(UUID enrollmentId, UUID memberId) {
		return db.queryForList("select id,course_run_id,lifecycle_status,total_units_snapshot,amount_paid_vnd_snapshot from course_enrollments " + "where id=? and member_account_id=? for update", enrollmentId, memberId).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	WithdrawalRequest lock(UUID id) {
		return db.query(select() + " where w.id=? for update of w", this::map, id).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	WithdrawalRequest find(UUID id) {
		return db.query(select() + " where w.id=?", this::map, id).stream().findFirst().orElseThrow(CourseWithdrawalException::notFound);
	}

	String select() {
		return "select w.*,a.id member_id,mp.member_code,a.full_name member_name,c.id course_id," + "c.code course_code,c.name course_name,r.run_code,r.status course_run_status,e.lifecycle_status " + "from course_withdrawal_requests w join course_enrollments e on e.id=w.course_enrollment_id " + "join accounts a on a.id=e.member_account_id join member_profiles mp on mp.account_id=a.id " + "join course_runs r on r.id=e.course_run_id join courses c on c.id=r.course_id";
	}

	WithdrawalRequest map(ResultSet rs, int row) throws SQLException {
		Instant capturedAt = instant(rs.getTimestamp("snapshot_captured_at"));
		boolean pending = "PENDING".equals(rs.getString("status"));
		boolean eligible = pending && capturedAt != null && !"CANCELLED".equals(rs.getString("course_run_status"));
		BigInteger refund = amount(rs.getBigDecimal("refund_amount_vnd"));
		String source = capturedAt != null ? "REQUEST_TIME" : refund != null ? "LEGACY_APPROVAL" : "UNAVAILABLE";
		String eligibilityReason = eligible ? rs.getString("withdrawal_reason_snapshot") : capturedAt == null ? "LEGACY_SNAPSHOT_MISSING" : "REQUEST_NOT_PENDING";
		return new WithdrawalRequest(rs.getObject("id", UUID.class), rs.getObject("course_enrollment_id", UUID.class), rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getObject("requester_account_id", UUID.class), rs.getString("status"), rs.getString("request_reason"), rs.getString("decision_reason"), CourseWithdrawalCause.valueOf(rs.getString("cause_code")), eligible, eligibilityReason, rs.getTimestamp("requested_at").toInstant(), rs.getObject("decided_by_account_id", UUID.class), instant(rs.getTimestamp("decided_at")), rs.getObject("total_units_snapshot", Integer.class), rs.getObject("remaining_units_snapshot", Integer.class), amount(rs.getBigDecimal("refund_basis_vnd")), refund, rs.getObject("member_id", UUID.class), rs.getString("member_code"), rs.getString("member_name"), rs.getObject("course_id", UUID.class), rs.getString("course_code"), rs.getString("course_name"), rs.getString("run_code"), rs.getString("course_run_status"), eligible ? refund : null, rs.getObject("schedule_change_response_id", UUID.class), rs.getObject("change_event_id", UUID.class), capturedAt, source, rs.getObject("withdrawal_eligible_snapshot", Boolean.class), rs.getString("withdrawal_reason_snapshot"), instant(rs.getTimestamp("cancelled_at")), rs.getObject("cancelled_by_account_id", UUID.class), rs.getString("cancellation_reason"), pending, "WITHDRAWAL_PENDING".equals(rs.getString("lifecycle_status")));
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return new BigDecimal(row.get(key).toString()).toBigIntegerExact();
	}

	static BigInteger amount(BigDecimal value) {
		return value == null ? null : value.toBigIntegerExact();
	}

	static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value instanceof Instant instant ? instant : ((Timestamp) value).toInstant();
	}

	static Instant instant(Timestamp value) {
		return value == null ? null : value.toInstant();
	}
}
