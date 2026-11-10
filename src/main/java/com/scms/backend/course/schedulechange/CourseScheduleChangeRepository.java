package com.scms.backend.course.schedulechange;

import static com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.withdrawal.CourseWithdrawalPolicy;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseScheduleChangeRepository {

	private final JdbcTemplate db;

	private final Clock clock;

	CourseScheduleChangeRepository(JdbcTemplate db, Clock clock) {
		this.db = db;
		this.clock = clock;
	}

	ScheduleChange reload(UUID memberId, UUID responseId) {
		Timestamp now = Timestamp.from(clock.instant());
		return db.query(select() + " where r.id=? and e.member_account_id=?", this::map, now, now, responseId, memberId).stream().findFirst().orElseThrow(CourseException::responseNotFound);
	}

	String select() {
		return """
			select r.*,w.id withdrawal_request_id,w.refund_basis_vnd,w.refund_amount_vnd,
			 w.status withdrawal_status,w.snapshot_captured_at,w.withdrawal_reason_snapshot,
			 e.lifecycle_status,e.total_units_snapshot,e.amount_paid_vnd_snapshot,run.status course_run_status,
			 u.remaining_units,u.unused_allocation
			from course_schedule_change_responses r join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id join course_runs run on run.id=e.course_run_id
			left join course_withdrawal_requests w on w.schedule_change_response_id=r.id
			left join lateral (select count(*) filter(where s.start_at>?) remaining_units,
			 coalesce(sum(unit.allocation_vnd) filter(where s.start_at>?),0) unused_allocation
			 from course_enrollment_sessions unit join course_sessions s on s.id=unit.course_session_id
			 where unit.course_enrollment_id=e.id and not exists (select 1 from course_refund_unit_claims c where c.enrollment_session_id=unit.id)) u on true
			""";
	}

	ScheduleChange map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
		CourseWithdrawalCause cause = CourseWithdrawalCause.valueOf(rs.getString("cause_code"));
		CourseWithdrawalPolicy.Decision decision = CourseWithdrawalPolicy.evaluate(cause, rs.getInt("remaining_units"), rs.getInt("total_units_snapshot"), rs.getBigDecimal("unused_allocation").toBigIntegerExact().min(rs.getBigDecimal("amount_paid_vnd_snapshot").toBigIntegerExact()));
		String reason = decision.reason();
		boolean eligible = decision.eligible();
		if (!List.of("ACTIVE", "WITHDRAWAL_PENDING").contains(rs.getString("lifecycle_status"))) {
			eligible = false;
			reason = "ENROLLMENT_NOT_ACTIVE";
		} else if (!List.of("OPEN_FOR_REGISTRATION", "CONFIRMED", "IN_PROGRESS").contains(rs.getString("course_run_status"))) {
			eligible = false;
			reason = "COURSE_RUN_NOT_WITHDRAWABLE";
		} else if (!"REQUEST_WITHDRAWAL".equals(rs.getString("response")) && (!"PENDING".equals(rs.getString("status")) || !clock.instant().isBefore(rs.getTimestamp("response_deadline").toInstant()))) {
			eligible = false;
			reason = "SCHEDULE_CHANGE_RESPONSE_CLOSED";
		}
		UUID withdrawalId = rs.getObject("withdrawal_request_id", UUID.class);
		if (withdrawalId != null) {
			eligible = "PENDING".equals(rs.getString("withdrawal_status")) && rs.getTimestamp("snapshot_captured_at") != null;
			reason = eligible ? rs.getString("withdrawal_reason_snapshot") : "REQUEST_NOT_PENDING";
		}
		return new ScheduleChange(rs.getObject("id", UUID.class), rs.getObject("enrollment_session_id", UUID.class), rs.getTimestamp("old_start_at").toInstant(), rs.getTimestamp("old_end_at").toInstant(), rs.getTimestamp("new_start_at").toInstant(), rs.getTimestamp("new_end_at").toInstant(), rs.getTimestamp("notification_sent_at").toInstant(), rs.getTimestamp("response_deadline").toInstant(), rs.getString("status"), rs.getString("response"), rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant(), rs.getObject("change_event_id", UUID.class), cause, rs.getObject("withdrawal_request_id", UUID.class), eligible, reason, withdrawalId == null ? decision.refundBasisVnd() : rs.getBigDecimal("refund_basis_vnd") == null ? null : rs.getBigDecimal("refund_basis_vnd").toBigIntegerExact(), withdrawalId == null ? decision.refundAmountVnd() : rs.getBigDecimal("refund_amount_vnd") == null ? null : rs.getBigDecimal("refund_amount_vnd").toBigIntegerExact());
	}

	static Timestamp timestamp(Instant value) {
		return value == null ? null : Timestamp.from(value);
	}

	Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt) {
		return conflictingEnrollmentSessions(runId, sessionId, startAt, endAt, false);
	}

	Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt, boolean excludeCurrentRun) {
		return new HashSet<>(db.queryForList("""
			select distinct current_es.id
			from course_enrollment_sessions current_es
			join course_enrollments current_enrollment on current_enrollment.id=current_es.course_enrollment_id
			join course_sessions current_session on current_session.id=current_es.course_session_id
			where current_enrollment.course_run_id=? and current_es.disposition='ENROLLED'
			and current_enrollment.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			and (cast(? as uuid) is null or current_es.course_session_id=?)
			and (exists(select 1
				from course_enrollment_sessions other_es
				join course_enrollments other_enrollment on other_enrollment.id=other_es.course_enrollment_id
				join course_sessions other_session on other_session.id=other_es.course_session_id
				where other_enrollment.member_account_id=current_enrollment.member_account_id
				and (?=false or other_enrollment.course_run_id<>current_enrollment.course_run_id)
				and other_es.course_session_id<>current_es.course_session_id and other_es.disposition='ENROLLED'
				and other_enrollment.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
				and other_session.status in ('SCHEDULED','MAKEUP_SCHEDULED')
				and other_session.start_at < coalesce(cast(? as timestamptz),current_session.end_at)
				and other_session.end_at > coalesce(cast(? as timestamptz),current_session.start_at))
			or exists(select 1 from bookings b join class_sessions gym on gym.id=b.class_session_id
				where b.member_account_id=current_enrollment.member_account_id and b.status='BOOKED'
				and gym.start_time < coalesce(cast(? as timestamptz),current_session.end_at)
				and gym.end_time > coalesce(cast(? as timestamptz),current_session.start_at)))
			""", UUID.class, runId, sessionId, sessionId, excludeCurrentRun, timestamp(endAt), timestamp(startAt), timestamp(endAt), timestamp(startAt)));
	}
}
