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
import java.util.Map;
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

	List<Map<java.lang.String, java.lang.Object>> notifyRescheduleFindCourseEnrollmentSessions(UUID sessionId) {
		return db.queryForList("""
			select es.id enrollment_session_id, e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by es.id
			""", sessionId);
	}

	int notifyRescheduleInsertCourseScheduleChangeEvents(UUID eventId, UUID value2, java.lang.String cause, Timestamp value4, Timestamp value5, Timestamp value6, Timestamp value7, int value8, int value9) {
		return db.update("""
			insert into course_schedule_change_events(id,course_run_id,cause_code,old_planned_start_at,
			old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,?,?,?,?,?,?)
			""", eventId, value2, cause, value4, value5, value6, value7, value8, value9);
	}

	int notifyRescheduleUpdateCourseScheduleChangeResponses(UUID sessionId) {
		return db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'" + " and enrollment_session_id in (select id from course_enrollment_sessions where course_session_id=?)", sessionId);
	}

	int notifyRescheduleInsertCourseScheduleChangeResponses(UUID recordId, java.lang.Object enrollmentSessionId, Timestamp oldStartTimestamp, Timestamp oldEndTimestamp, Timestamp newStartTimestamp, Timestamp newEndTimestamp, Timestamp sentAtTimestamp, Timestamp deadlineTimestamp, java.lang.String memberCause, UUID eventId) {
		return db.update("""
			insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
			new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
			values(?,?,?,?,?,?,?,?,?,?)
			""", recordId, enrollmentSessionId, oldStartTimestamp, oldEndTimestamp, newStartTimestamp, newEndTimestamp, sentAtTimestamp, deadlineTimestamp, memberCause, eventId);
	}

	List<Map<java.lang.String, java.lang.Object>> createRunScheduleResponsesFindCourseEnrollmentSessions(UUID runId, Timestamp sentAtTimestamp) {
		return db.queryForList("""
			select es.id enrollment_session_id,e.member_account_id,s.start_at,s.end_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join course_sessions s on s.id=es.course_session_id
			where e.course_run_id=? and es.disposition in ('ENROLLED','REMOVED_BY_RUN_CHANGE') and s.start_at>?
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""", runId, sentAtTimestamp);
	}

	int createRunScheduleResponsesUpdateCourseScheduleChangeResponses(UUID runId) {
		return db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'" + " and enrollment_session_id in (select id from course_enrollment_sessions where course_run_id=?)", runId);
	}

	int createRunScheduleResponsesInsertCourseScheduleChangeResponses(UUID responseId, java.lang.Object enrollmentSessionId, java.lang.Object startAt, java.lang.Object endAt, java.lang.Object startAt2, java.lang.Object endAt2, Timestamp sentAtTimestamp, Timestamp deadlineTimestamp, java.lang.String memberCause, UUID eventId) {
		return db.update("""
			insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
			new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
			values(?,?,?,?,?,?,?,?,?,?)
			""", responseId, enrollmentSessionId, startAt, endAt, startAt2, endAt2, sentAtTimestamp, deadlineTimestamp, memberCause, eventId);
	}

	List<Map<java.lang.String, java.lang.Object>> prepareAtomicRunResponsesFindCourseEnrollmentSessions(UUID runId) {
		return db.queryForList("""
			select es.id enrollment_session_id,es.course_session_id,e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where e.course_run_id=? and es.disposition='ENROLLED'
			 and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""", runId);
	}

	int writeAtomicRunResponsesUpdateCourseScheduleChangeResponses(UUID enrollmentSessionId) {
		return db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING' and enrollment_session_id=?", enrollmentSessionId);
	}

	int writeAtomicRunResponsesInsertCourseScheduleChangeResponses(UUID responseId, UUID enrollmentSessionId, Timestamp value3, Timestamp value4, Timestamp value5, Timestamp value6, Timestamp value7, Timestamp value8, java.lang.String value9, UUID eventId) {
		return db.update("""
			insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
			 new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id) values(?,?,?,?,?,?,?,?,?,?)
			""", responseId, enrollmentSessionId, value3, value4, value5, value6, value7, value8, value9, eventId);
	}

	List<com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange> listFindRows(Timestamp now, Timestamp now2, UUID memberId) {
		return db.query(select() + " where e.member_account_id=? order by r.created_at desc,r.id", this::map, now, now2, memberId);
	}

	List<UUID> respondFindCourseScheduleChangeResponses(UUID responseId, UUID memberId) {
		return db.queryForList("select e.course_run_id from course_schedule_change_responses r " + "join course_enrollment_sessions es on es.id=r.enrollment_session_id " + "join course_enrollments e on e.id=es.course_enrollment_id where r.id=? and e.member_account_id=?", UUID.class, responseId, memberId);
	}

	UUID respondFindCourseRuns(UUID first) {
		return db.queryForObject("select id from course_runs where id=? for update", UUID.class, first);
	}

	List<com.scms.backend.course.schedulechange.CourseScheduleChangeApiModels.ScheduleChange> respondFindOf(Timestamp queryTime, Timestamp queryTime2, UUID responseId, UUID memberId) {
		return db.query(select() + """
			 where r.id in (
			   select sibling.id from course_schedule_change_responses target
			   join course_enrollment_sessions target_unit on target_unit.id=target.enrollment_session_id
			   join course_enrollments owner on owner.id=target_unit.course_enrollment_id
			   join course_enrollment_sessions sibling_unit on sibling_unit.course_enrollment_id=owner.id
			   join course_schedule_change_responses sibling on sibling.enrollment_session_id=sibling_unit.id
			   where target.id=? and owner.member_account_id=? and (sibling.id=target.id or
			     (target.status<>'SUPERSEDED' and sibling.status<>'SUPERSEDED'
			       and target.change_event_id is not null and sibling.change_event_id=target.change_event_id))
			 ) order by r.id for update of r
			 """, this::map, queryTime, queryTime2, responseId, memberId);
	}

	int respondUpdateCourseScheduleChangeResponses(java.lang.String choice, Timestamp nowTimestamp, UUID responseId) {
		return db.update("""
			update course_schedule_change_responses set response=?,responded_at=?,status='RESPONDED'
			where status='PENDING' and id in (
			  select sibling.id from course_schedule_change_responses target
			  join course_enrollment_sessions target_unit on target_unit.id=target.enrollment_session_id
			  join course_enrollment_sessions sibling_unit on sibling_unit.course_enrollment_id=target_unit.course_enrollment_id
			  join course_schedule_change_responses sibling on sibling.enrollment_session_id=sibling_unit.id
			  where target.id=? and (sibling.id=target.id or
			    (target.change_event_id is not null and sibling.change_event_id=target.change_event_id))
			)
			""", choice, nowTimestamp, responseId);
	}
}
