package com.scms.backend.course;

import java.sql.Timestamp;
import java.time.Clock;
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
	@Autowired(required = false) private CourseTransferService transfers;

	CourseScheduleChangeService(JdbcTemplate db, NotificationWriter notifications, AccountRepository accounts, Clock clock) {
		this.db = db; this.notifications = notifications; this.accounts = accounts; this.clock = clock;
	}

	@Transactional
	void notifyReschedule(CourseSession session, Instant oldStart, Instant oldEnd, Instant newStart, Instant newEnd) {
		if (oldStart.equals(newStart) && oldEnd.equals(newEnd)) return;
		Instant sentAt = clock.instant();
		boolean material = CourseScheduleChangePolicy.isMaterial(oldStart, oldEnd, newStart, newEnd);
		Instant deadline = material ? CourseScheduleChangePolicy.deadline(sentAt, newStart) : null;
		List<Map<String, Object>> members = db.queryForList("""
			select es.id enrollment_session_id, e.member_account_id
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where es.course_session_id=? and es.disposition='ENROLLED' and e.lifecycle_status='ACTIVE'
			order by es.id
			""", session.getId());
		if (material && !members.isEmpty() && !deadline.isAfter(sentAt)) throw CourseException.responseDeadline();
		db.update("update course_schedule_change_responses set status='SUPERSEDED' where status='PENDING'"
			+ " and enrollment_session_id in (select id from course_enrollment_sessions where course_session_id=?)", session.getId());
		for (Map<String, Object> member : members) {
			UUID recordId = material ? UUID.randomUUID() : null;
			if (material) db.update("""
				insert into course_schedule_change_responses(id,enrollment_session_id,old_start_at,old_end_at,
				new_start_at,new_end_at,notification_sent_at,response_deadline)
				values(?,?,?,?,?,?,?,?)
				""", recordId, member.get("enrollment_session_id"), Timestamp.from(oldStart), Timestamp.from(oldEnd),
				Timestamp.from(newStart), Timestamp.from(newEnd), Timestamp.from(sentAt), Timestamp.from(deadline));
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
			select r.* from course_schedule_change_responses r
			join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id
			where e.member_account_id=? order by r.created_at desc,r.id
			""", (rs, n) -> new ScheduleChange(rs.getObject("id", UUID.class),
			rs.getObject("enrollment_session_id", UUID.class), rs.getTimestamp("old_start_at").toInstant(),
			rs.getTimestamp("old_end_at").toInstant(), rs.getTimestamp("new_start_at").toInstant(),
			rs.getTimestamp("new_end_at").toInstant(), rs.getTimestamp("notification_sent_at").toInstant(),
			rs.getTimestamp("response_deadline").toInstant(), rs.getString("status"), rs.getString("response"),
			rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant()), memberId);
	}

	@Transactional
	ScheduleChange respond(UUID memberId, UUID responseId, String choice) {
		activeMember(memberId);
		if (choice == null || !List.of("CONTINUE", "REQUEST_WITHDRAWAL", "REQUEST_TRANSFER").contains(choice))
			throw CourseException.invalidResponse();
		List<ScheduleChange> rows = db.query("""
			select r.* from course_schedule_change_responses r join course_enrollment_sessions es on es.id=r.enrollment_session_id
			join course_enrollments e on e.id=es.course_enrollment_id where r.id=? and e.member_account_id=? for update of r
			""", (rs, n) -> new ScheduleChange(rs.getObject("id", UUID.class), rs.getObject("enrollment_session_id", UUID.class),
			rs.getTimestamp("old_start_at").toInstant(), rs.getTimestamp("old_end_at").toInstant(),
			rs.getTimestamp("new_start_at").toInstant(), rs.getTimestamp("new_end_at").toInstant(),
			rs.getTimestamp("notification_sent_at").toInstant(), rs.getTimestamp("response_deadline").toInstant(),
			rs.getString("status"), rs.getString("response"), rs.getTimestamp("responded_at") == null ? null : rs.getTimestamp("responded_at").toInstant()),
			responseId, memberId);
		if (rows.isEmpty()) throw CourseException.responseNotFound();
		ScheduleChange current = rows.getFirst();
		if (current.response() != null) {
			if (current.response().equals(choice)) {
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
		if ("REQUEST_TRANSFER".equals(choice) && transfers != null)
			transfers.requestFromScheduleChange(memberId, responseId, now);
		return new ScheduleChange(current.id(), current.enrollmentSessionId(), current.oldStartAt(), current.oldEndAt(),
			current.newStartAt(), current.newEndAt(), current.notificationSentAt(), current.responseDeadline(),
			"RESPONDED", choice, now);
	}

	private void activeMember(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MEMBER, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}
}
