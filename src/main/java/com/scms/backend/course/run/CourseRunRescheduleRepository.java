package com.scms.backend.course.run;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseRunRescheduleRepository {

	List<UUID> rescheduleFindCourseRuns(UUID runId) {
		return db.queryForList("select id from course_runs where id=? for update", UUID.class, runId);
	}

	List<Map<java.lang.String, java.lang.Object>> rescheduleFindCourseRunReschedules(UUID runId, UUID actorId, java.lang.String key) {
		return db.queryForList("select request_fingerprint,response_json::text from course_run_reschedules " + "where course_run_id=? and actor_account_id=? and idempotency_key=?", runId, actorId, key);
	}

	List<UUID> rescheduleFindCourseSessions(UUID runId) {
		return db.queryForList("select id from course_sessions where course_run_id=? order by id for update", UUID.class, runId);
	}

	java.lang.Boolean hasConflictingCourseSessions(UUID runId, UUID value3, UUID value4, Timestamp value5, Timestamp value6) {
		return db.queryForObject("""
			select exists(select 1 from course_sessions where course_run_id<>? and status in ('SCHEDULED','MAKEUP_SCHEDULED')
			 and (coach_account_id=? or room_id=?) and start_at<? and end_at>?)
			""", Boolean.class, runId, value3, value4, value5, value6);
	}

	int rescheduleInsertCourseScheduleChangeEvents(UUID eventId, UUID runId, UUID actorId, java.lang.String cause, Timestamp value5, Timestamp value6, Timestamp value7, Timestamp value8, int totalUnits, java.lang.Integer totalUnits2) {
		return db.update("""
			insert into course_schedule_change_events(id,course_run_id,actor_account_id,cause_code,old_planned_start_at,
			 old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units) values(?,?,?,?,?,?,?,?,?,?)
			""", eventId, runId, actorId, cause, value5, value6, value7, value8, totalUnits, totalUnits2);
	}

	int rescheduleUpdateCourseEnrollmentSessions(UUID value1) {
		return db.update("update course_enrollment_sessions set disposition='REMOVED_BY_RUN_CHANGE' where course_session_id=? and disposition='ENROLLED'", value1);
	}

	int rescheduleInsertCourseRunReschedules(UUID runId, UUID actorId, java.lang.String key, java.lang.String fingerprint, UUID eventId, java.lang.String value6) {
		return db.update("insert into course_run_reschedules(course_run_id,actor_account_id,idempotency_key,request_fingerprint,change_event_id,response_json) " + "values(?,?,?,?,?,?::jsonb)", runId, actorId, key, fingerprint, eventId, value6);
	}

	List<Map<java.lang.String, java.lang.Object>> resourcesFindCourseSessions(UUID runId) {
		return db.queryForList("select id,coach_account_id,room_id from course_sessions where course_run_id=? order by id", runId);
	}

	private final JdbcTemplate db;

	CourseRunRescheduleRepository(JdbcTemplate db) {
		this.db = db;
	}
}
