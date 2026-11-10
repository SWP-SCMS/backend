package com.scms.backend.course.management;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseManagementRepository {

	Map<java.lang.String, java.lang.Object> cancellationImpactFindCourseEnrollments(UUID runId, UUID runId2) {
		return db.queryForMap("""
			select (select count(*) from course_enrollments where course_run_id=? and lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')) as "enrollmentCount",
			       (select count(*) from course_orders where course_run_id=? and status='PENDING_PAYMENT') as "pendingOrderCount"
			""", runId, runId2);
	}

	private final JdbcTemplate db;

	CourseManagementRepository(JdbcTemplate db) {
		this.db = db;
	}

	int patchRunScheduleInsertCourseScheduleChangeEvents(UUID eventId, UUID runId, UUID managerId, java.lang.String cause, Timestamp oldStartTimestamp, Timestamp oldEndTimestamp, Timestamp value7, Timestamp value8, int oldUnits, java.lang.Integer totalUnits) {
		return db.update("""
			insert into course_schedule_change_events(id,course_run_id,actor_account_id,cause_code,
			old_planned_start_at,old_planned_end_at,new_planned_start_at,new_planned_end_at,old_total_units,new_total_units)
			values(?,?,?,?,?,?,?,?,?,?)
			""", eventId, runId, managerId, cause, oldStartTimestamp, oldEndTimestamp, value7, value8, oldUnits, totalUnits);
	}

	int patchRunScheduleUpdateCourseEnrollmentSessions(UUID runId, int unitNumber) {
		return db.update("update course_enrollment_sessions set disposition='REMOVED_BY_RUN_CHANGE' " + "where course_run_id=? and unit_number=? and disposition='ENROLLED'", runId, unitNumber);
	}
}
