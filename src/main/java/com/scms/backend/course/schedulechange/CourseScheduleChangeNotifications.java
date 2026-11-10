package com.scms.backend.course.schedulechange;

import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseRun;
import com.scms.backend.course.CourseSession;
import com.scms.backend.course.CourseWithdrawalCause;
import com.scms.backend.course.run.CourseRunRescheduleService;
import com.scms.backend.notification.NotificationWriter;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import com.scms.backend.course.schedulechange.CourseScheduleChangeService.AtomicResponse;

final class CourseScheduleChangeNotifications {

	private final JdbcTemplate db;

	private final NotificationWriter notifications;

	private final Clock clock;

	private final CourseScheduleChangeRepository courseScheduleChangeRepository;

	CourseScheduleChangeNotifications(JdbcTemplate db, NotificationWriter notifications, Clock clock, CourseScheduleChangeRepository courseScheduleChangeRepository) {
		this.db = db;
		this.notifications = notifications;
		this.clock = clock;
		this.courseScheduleChangeRepository = courseScheduleChangeRepository;
	}

	void notifyReschedule(CourseSession session, Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd) {
		if (oldStart.equals(newStart) && oldEnd.equals(newEnd))
			return;
		Instant sentAt = clock.instant();
		boolean material = CourseScheduleChangePolicy.isMaterial(oldStart, oldEnd, newStart, newEnd);
		Set<UUID> conflicts = conflictingEnrollmentSessions(session.getCourseRun().getId(), session.getId(), newStart, newEnd);
		Instant deadline = material || !conflicts.isEmpty() ? CourseScheduleChangePolicy.deadline(sentAt, newStart) : null;
		CourseWithdrawalCause cause = CourseWithdrawalCause.VOLUNTARY;
		UUID eventId = UUID.randomUUID();
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id, e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by es.id
			""", session.getId());
		if (deadline != null && !members.isEmpty() && !deadline.isAfter(sentAt))
			throw CourseException.responseDeadline();
		db.update("""
			insert into course_schedule_change_events(id,course_run_id,cause_code,old_planned_start_at,
			old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,?,?,?,?,?,?)
			""", eventId, session.getCourseRun().getId(), cause.name(), timestamp(session.getCourseRun().getPlannedStartAt()), timestamp(session.getCourseRun().getPlannedEndAt()), timestamp(session.getCourseRun().getPlannedStartAt()), timestamp(session.getCourseRun().getPlannedEndAt()), session.getCourseRun().getTotalUnits(), session.getCourseRun().getTotalUnits());
		db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'" + " and enrollment_session_id in (select id from course_enrollment_sessions where course_session_id=?)", session.getId());
		for (Map<String, Object> member : members) {
			boolean conflicted = conflicts.contains(member.get("enrollment_session_id"));
			CourseWithdrawalCause memberCause = conflicted ? CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT : cause;
			UUID recordId = material || conflicted ? UUID.randomUUID() : null;
			if (recordId != null)
				db.update("""
					insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
					new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
					values(?,?,?,?,?,?,?,?,?,?)
					""", recordId, member.get("enrollment_session_id"), Timestamp.from(oldStart), Timestamp.from(oldEnd), Timestamp.from(newStart), Timestamp.from(newEnd), Timestamp.from(sentAt), Timestamp.from(deadline), memberCause.name(), eventId);
			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("courseCode", session.getCourseRun().getCourse().getCode());
			payload.put("runCode", session.getCourseRun().getRunCode());
			payload.put("unitNumber", session.getUnitNumber());
			payload.put("oldStartAt", oldStart.toString());
			payload.put("oldEndAt", oldEnd.toString());
			payload.put("newStartAt", newStart.toString());
			payload.put("newEndAt", newEnd.toString());
			payload.put("responseDeadline", recordId == null ? null : deadline.toString());
			payload.put("recordId", recordId);
			notifications.write("COURSE_SESSION_RESCHEDULED:" + session.getId() + ":" + session.getVersion() + ":" + member.get("enrollment_session_id"), (UUID) member.get("member_account_id"), "COURSE_SCHEDULE_CHANGED", "COURSE_SESSION", session.getId(), payload);
		}
	}

	void createRunScheduleResponses(UUID eventId, CourseRun run, CourseWithdrawalCause cause) {
		Instant sentAt = clock.instant();
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id,e.member_account_id,s.start_at,s.end_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join course_sessions s on s.id=es.course_session_id
			where e.course_run_id=? and es.disposition in ('ENROLLED','REMOVED_BY_RUN_CHANGE') and s.start_at>?
			  and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""", run.getId(), Timestamp.from(sentAt));
		if (members.isEmpty())
			return;
		Instant nextStart = members.stream().map(row -> ((Timestamp) row.get("start_at")).toInstant()).min(Instant::compareTo).orElseThrow();
		Instant deadline = CourseScheduleChangePolicy.deadline(sentAt, nextStart);
		if (!deadline.isAfter(sentAt))
			throw CourseException.responseDeadline();
		Set<UUID> conflicts = conflictingEnrollmentSessions(run.getId(), null, null, null);
		db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'" + " and enrollment_session_id in (select id from course_enrollment_sessions where course_run_id=?)", run.getId());
		for (Map<String, Object> member : members) {
			CourseWithdrawalCause memberCause = cause == CourseWithdrawalCause.VOLUNTARY && conflicts.contains(member.get("enrollment_session_id")) ? CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT : cause;
			UUID responseId = UUID.randomUUID();
			db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id)
				values(?,?,?,?,?,?,?,?,?,?)
				""", responseId, member.get("enrollment_session_id"), member.get("start_at"), member.get("end_at"), member.get("start_at"), member.get("end_at"), Timestamp.from(sentAt), Timestamp.from(deadline), memberCause.name(), eventId);
			notifications.write("COURSE_RUN_SCHEDULE_CHANGED:" + eventId + ":" + member.get("enrollment_session_id"), (UUID) member.get("member_account_id"), "COURSE_SCHEDULE_CHANGED", "COURSE_RUN", run.getId(), Map.of("recordId", responseId, "cause", memberCause.name(), "responseDeadline", deadline.toString()));
		}
	}

	List<AtomicResponse> prepareAtomicRunResponses(UUID runId, CourseWithdrawalCause cause, boolean runChanged, List<CourseRunRescheduleService.PlannedSession> plan, Instant now) {
		Map<UUID, CourseRunRescheduleService.PlannedSession> byId = new LinkedHashMap<>();
		for (var item : plan) byId.put(item.session().getId(), item);
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id,es.course_session_id,e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where e.course_run_id=? and es.disposition='ENROLLED'
			 and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by es.id
			""", runId);
		Map<UUID, List<Map<String, Object>>> membersBySession = members.stream().collect(java.util.stream.Collectors.groupingBy(row -> (UUID) row.get("course_session_id")));
		Map<UUID, List<CourseRunRescheduleService.PlannedSession>> memberSessions = new LinkedHashMap<>();
		for (var member : members) memberSessions.computeIfAbsent((UUID) member.get("member_account_id"), ignored -> new ArrayList<>()).add(byId.get(member.get("course_session_id")));
		List<AtomicResponse> responses = new ArrayList<>();
		for (var item : plan) {
			if ((!runChanged && !item.changed()) || !item.startAt().isAfter(now))
				continue;
			Set<UUID> conflicts = item.removed() ? Set.of() : conflictingEnrollmentSessions(runId, item.session().getId(), item.startAt(), item.endAt(), true);
			for (var member : membersBySession.getOrDefault(item.session().getId(), List.of())) {
				boolean conflict = conflicts.contains(member.get("enrollment_session_id"));
				if (!item.removed())
					for (var other : memberSessions.get(member.get("member_account_id"))) {
						if (other != item && !other.removed() && CourseRunRescheduleService.active(other.session()) && CourseRunRescheduleService.overlaps(item, other))
							conflict = true;
					}
				CourseWithdrawalCause memberCause = cause == CourseWithdrawalCause.VOLUNTARY && conflict ? CourseWithdrawalCause.MEMBER_SCHEDULE_CONFLICT : cause;
				boolean responseRequired = runChanged || conflict || item.removed() || CourseScheduleChangePolicy.isMaterial(item.session().getStartAt(), item.session().getEndAt(), item.startAt(), item.endAt());
				responses.add(new AtomicResponse((UUID) member.get("enrollment_session_id"), (UUID) member.get("member_account_id"), item.session().getId(), item.session().getStartAt(), item.session().getEndAt(), item.startAt(), item.endAt(), memberCause, responseRequired ? UUID.randomUUID() : null, null));
			}
		}
		Instant nextStart = responses.stream().filter(r -> r.responseId() != null).map(AtomicResponse::newStart).min(Instant::compareTo).orElse(null);
		Instant deadline = nextStart == null ? null : CourseScheduleChangePolicy.deadline(now, nextStart);
		if (deadline != null && !deadline.isAfter(now))
			throw CourseException.responseDeadline();
		return responses.stream().map(r -> new AtomicResponse(r.enrollmentSessionId(), r.memberId(), r.sessionId(), r.oldStart(), r.oldEnd(), r.newStart(), r.newEnd(), r.cause(), r.responseId(), r.responseId() == null ? null : deadline)).toList();
	}

	void writeAtomicRunResponses(UUID eventId, CourseRun run, List<AtomicResponse> responses, Instant now) {
		Map<UUID, List<Map<String, Object>>> notices = new LinkedHashMap<>();
		for (var r : responses) {
			db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING' and enrollment_session_id=?", r.enrollmentSessionId());
			if (r.responseId() != null)
				db.update("""
					insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
					 new_start_at,new_end_at,notification_sent_at,response_deadline,cause_code,change_event_id) values(?,?,?,?,?,?,?,?,?,?)
					""", r.responseId(), r.enrollmentSessionId(), timestamp(r.oldStart()), timestamp(r.oldEnd()), timestamp(r.newStart()), timestamp(r.newEnd()), timestamp(now), timestamp(r.deadline()), r.cause().name(), eventId);
			Map<String, Object> change = new LinkedHashMap<>();
			change.put("courseSessionId", r.sessionId());
			change.put("recordId", r.responseId());
			change.put("oldStartAt", r.oldStart().toString());
			change.put("oldEndAt", r.oldEnd().toString());
			change.put("newStartAt", r.newStart().toString());
			change.put("newEndAt", r.newEnd().toString());
			change.put("cause", r.cause().name());
			change.put("responseDeadline", r.deadline() == null ? null : r.deadline().toString());
			notices.computeIfAbsent(r.memberId(), ignored -> new ArrayList<>()).add(change);
		}
		for (var notice : notices.entrySet()) notifications.write("COURSE_RUN_RESCHEDULED:" + eventId + ":" + notice.getKey(), notice.getKey(), "COURSE_SCHEDULE_CHANGED", "COURSE_RUN", run.getId(), Map.of("changeEventId", eventId, "courseCode", run.getCourse().getCode(), "runCode", run.getRunCode(), "changes", notice.getValue()));
	}

	static Timestamp timestamp(Instant value) {
		return CourseScheduleChangeRepository.timestamp(value);
	}

	Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt) {
		return courseScheduleChangeRepository.conflictingEnrollmentSessions(runId, sessionId, startAt, endAt);
	}

	Set<UUID> conflictingEnrollmentSessions(UUID runId, UUID sessionId, Instant startAt, Instant endAt, boolean excludeCurrentRun) {
		return courseScheduleChangeRepository.conflictingEnrollmentSessions(runId, sessionId, startAt, endAt, excludeCurrentRun);
	}
}
