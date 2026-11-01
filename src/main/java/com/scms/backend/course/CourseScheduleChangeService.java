package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.scms.backend.course.CourseScheduleChangeApiModels.ScheduleChange;

@Service
class CourseScheduleChangeService {
	private final JdbcTemplate db;
	private final NotificationWriter notifications;
	private final AccountRepository accounts;
	private final Clock clock;
	private final CourseWithdrawalService withdrawals;
	@Autowired(required = false) private CourseTransferService transfers;

	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts, Clock clock) {
		this(db, notifications, accounts, clock, null);
	}

	@org.springframework.beans.factory.annotation.Autowired
	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts,
			Clock clock, CourseWithdrawalService withdrawals) {
		this.db = db; this.notifications = notifications; this.accounts = accounts; this.clock = clock;
		this.withdrawals = withdrawals;
	}

	@Transactional
	void notifyReschedule(CourseSession session, Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd) {
		if (oldStart.equals(newStart) && oldEnd.equals(newEnd)) return;
		Instant sentAt = clock.instant();
		boolean material = CourseScheduleChangePolicy.isMaterial(oldStart, oldEnd, newStart, newEnd);
		Instant deadline = material ? CourseScheduleChangePolicy.deadline(sentAt, newStart) : null;
		CourseWithdrawalCause cause = Duration.between(oldStart, newStart).compareTo(Duration.ofDays(7)) > 0
			? CourseWithdrawalCause.START_DELAY_OVER_7_DAYS : CourseWithdrawalCause.VOLUNTARY;
		UUID eventId = UUID.randomUUID();
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id, e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by es.id
			""", session.getId());
		if (material && !members.isEmpty() && !deadline.isAfter(sentAt)) throw CourseException.responseDeadline();
		if (hasMemberScheduleConflict(session.getId(), newStart, newEnd))
			cause = CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT;
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,cause_code,old_planned_start_at,
			old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,?,?,?,?,?,?)
			""", eventId, session.getCourseRun().getId(), cause.name(), timestamp(session.getCourseRun().getPlannedStartAt()),
			timestamp(session.getCourseRun().getPlannedEndAt()), timestamp(session.getCourseRun().getPlannedStartAt()),
			timestamp(session.getCourseRun().getPlannedEndAt()), session.getCourseRun().getTotalUnits(),
			session.getCourseRun().getTotalUnits());
		db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'"
			+ " and enrollment_session_id in (select id from course_enrollment_sessions where course_session_id=?)", session.getId());
		for (Map<String, Object> member : members) {
			UUID recordId = material ? UUID.randomUUID() : null;
			if (material) db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
				values(?,?,?,?,?,?,?,?,?,?)
				""", recordId, member.get("enrollment_session_id"), Timestamp.from(oldStart), Timestamp.from(oldEnd),
				Timestamp.from(newStart), Timestamp.from(newEnd), Timestamp.from(sentAt), Timestamp.from(deadline), cause.name(), eventId);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("courseCode", session.getCourseRun().getCourse().getCode());
			payload.put("runCode", session.getCourseRun().getRunCode());
			payload.put("unitNumber", session.getUnitNumber());
			payload.put("oldStartAt", oldStart.toString());
			payload.put("oldEndAt", oldEnd.toString());
			payload.put("newStartAt", newStart.toString());
			payload.put("newEndAt", newEnd.toString());
			payload.put("responseDeadline", deadline == null ? null : deadline.toString());
			payload.put("recordId", recordId);
			notifications.write("COURSE_SESSION_RESCHEDULED:" + session.getId() + ":" + session.getVersion()
				+ ":" + member.get("enrollment_session_id"), (UUID) member.get("member_account_id"),
				"COURSE_SCHEDULE_CHANGED", "COURSE_SESSION", session.getId(), payload);
		}
	}

	@Transactional(readOnly = true)
	List<ScheduleChange> list(UUID memberId) {
		activeMember(memberId);
		return db.query("""
			select r.*,w.id withdrawal_request_id,w.refund_basis_vnd,w.refund_amount_vnd
			from course_schedule_change_responses r
			join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			left join course_withdrawal_requests w on w.schedule_change_response_id=r.id
			where e.member_account_id=? order by r.created_at desc,r.id
			""", this::map, memberId);
	}

	@Transactional
	ScheduleChange respond(UUID memberId, UUID responseId, String choice) {
		activeMember(memberId);
		if (choice == null || !List.of("CONTINUE", "REQUEST_WITHDRAWAL", "REQUEST_TRANSFER").contains(choice))
			throw CourseException.invalidResponse();
		List<ScheduleChange> rows = db.query("""
			select r.*,w.id withdrawal_request_id,w.refund_basis_vnd,w.refund_amount_vnd
			from course_schedule_change_responses r join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			left join course_withdrawal_requests w on w.schedule_change_response_id=r.id
			where r.id=? and e.member_account_id=? for update of r
			""", this::map,
			responseId, memberId);
		if (rows.isEmpty()) throw CourseException.responseNotFound();
		ScheduleChange current = rows.getFirst();
		if (current.response() != null) {
			if (current.response().equals(choice)) {
				if ("REQUEST_WITHDRAWAL".equals(choice) && withdrawals != null) {
					withdrawals.createForScheduleChange(memberId, responseId);
					return reload(memberId, responseId);
				}
				if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
					transfers.requestFromScheduleChange(memberId, responseId,
						current.respondedAt() == null ? clock.instant() : current.respondedAt());
				return current;
			}
			throw CourseException.responseConflict();
		}
		Instant now = clock.instant();
		if (!current.status().equals("PENDING") || !now.isBefore(current.responseDeadline()))
			throw CourseException.responseClosed();
		db.update("update course_schedule_change_responses set response=?,responded_at=?,status='RESPONDED' where id=?",
			choice, Timestamp.from(now), responseId);
		if ("REQUEST_WITHDRAWAL".equals(choice) && withdrawals != null) {
			withdrawals.createForScheduleChange(memberId, responseId);
			return reload(memberId, responseId);
		}
		if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
			transfers.requestFromScheduleChange(memberId, responseId, now);
		return new ScheduleChange(current.id(), current.enrollmentSessionId(), current.oldStartAt(), current.oldEndAt(),
			current.newStartAt(), current.newEndAt(), current.notificationSentAt(), current.responseDeadline(),
			"RESPONDED", choice, now, current.changeEventId(), current.cause(), null, false, null, null, null);
	}

	@Transactional
	void createRunScheduleResponses(UUID eventId, CourseRun run, CourseWithdrawalCause cause, Instant sentAt) {
		Instant deadline = CourseScheduleChangePolicy.deadline(sentAt, run.getPlannedStartAt());
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id,e.member_account_id,s.start_at,s.end_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join course_sessions s on s.id=es.course_session_id
			where e.course_run_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""", run.getId());
		for (Map<String, Object> member : members) {
			UUID responseId = UUID.randomUUID();
			db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
				values(?,?,?,?,?,?,?,?,?,?)
				""", responseId, member.get("enrollment_session_id"), member.get("start_at"), member.get("end_at"),
				member.get("start_at"), member.get("end_at"), Timestamp.from(sentAt), Timestamp.from(deadline), cause.name(), eventId);
			notifications.write("COURSE_RUN_SCHEDULE_CHANGED:" + eventId + ":" + member.get("enrollment_session_id"),
				(UUID) member.get("member_account_id"), "COURSE_SCHEDULE_CHANGED", "COURSE_RUN", run.getId(),
				Map.of("recordId", responseId, "cause", cause.name(), "responseDeadline", deadline.toString()));
		}
	}

	private ScheduleChange reload(UUID memberId, UUID responseId) {
		return db.query("""
			select r.*,w.id withdrawal_request_id,w.refund_basis_vnd,w.refund_amount_vnd
			from course_schedule_change_responses r join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			left join course_withdrawal_requests w on w.schedule_change_response_id=r.id
			where r.id=? and e.member_account_id=?
			""", this::map, responseId, memberId).stream().findFirst().orElseThrow(CourseException::responseNotFound);
	}

	private ScheduleChange map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
		String cause = rs.getString("cause_code");
		return new ScheduleChange(rs.getObject("id", UUID.class), rs.getObject("enrollment_session_id", UUID.class),
			rs.getTimestamp("old_start_at").toInstant(), rs.getTimestamp("old_end_at").toInstant(),
			rs.getTimestamp("new_start_at").toInstant(), rs.getTimestamp("new_end_at").toInstant(),
			rs.getTimestamp("notification_sent_at").toInstant(), rs.getTimestamp("response_deadline").toInstant(),
			rs.getString("status"), rs.getString("response"), rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant(),
			rs.getObject("change_event_id", UUID.class), cause == null ? CourseWithdrawalCause.VOLUNTARY : CourseWithdrawalCause.valueOf(cause),
			rs.getObject("withdrawal_request_id", UUID.class), rs.getObject("withdrawal_request_id") != null,
			rs.getObject("withdrawal_request_id") == null ? null : "ELIGIBLE_WITH_REFUND",
			rs.getBigDecimal("refund_basis_vnd") == null ? null : rs.getBigDecimal("refund_basis_vnd").toBigIntegerExact(),
			rs.getBigDecimal("refund_amount_vnd") == null ? null : rs.getBigDecimal("refund_amount_vnd").toBigIntegerExact());
	}

	private static Timestamp timestamp(Instant value) { return value == null ? null : Timestamp.from(value); }

	private boolean hasMemberScheduleConflict(UUID sessionId, Instant startAt, Instant endAt) {
		return Boolean.TRUE.equals(db.queryForObject("""
			select exists(select 1
			from course_enrollment_sessions current_es
			join course_enrollments current_enrollment on current_enrollment.id=current_es.course_enrollment_id
			where current_es.course_session_id=? and current_es.disposition='ENROLLED'
			and exists(select 1
				from course_enrollment_sessions other_es
				join course_enrollments other_enrollment on other_enrollment.id=other_es.course_enrollment_id
				join course_sessions other_session on other_session.id=other_es.course_session_id
				where other_enrollment.member_account_id=current_enrollment.member_account_id
				and other_es.course_session_id<>? and other_es.disposition='ENROLLED'
				and other_session.status in ('SCHEDULED','MAKEUP_SCHEDULED')
				and other_session.start_at < ? and other_session.end_at > ?))
			""", Boolean.class, sessionId, sessionId, Timestamp.from(endAt), Timestamp.from(startAt)));
	}

	private void activeMember(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}
}
