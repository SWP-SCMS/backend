package com.scms.backend.course.enrollment;

import com.scms.backend.course.CourseAttendanceStatus;
import com.scms.backend.course.enrollment.CourseEnrollmentApiModels.RosterEntry;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseEnrollmentRepository {

	List<Map<java.lang.String, java.lang.Object>> memberEnrollmentsFindCourseEnrollments(UUID memberId) {
		return db.queryForList("""
			select e.id enrollment_id,e.course_run_id,c.code course_code,c.name course_name,r.run_code,r.status course_run_status,
				e.lifecycle_status,e.enrolled_at,e.amount_paid_vnd_snapshot,p.status payment_status,p.method payment_method,
				es.id enrollment_session_id,es.course_session_id,es.unit_number,s.start_at,s.end_at,
				es.disposition,s.status session_status,s.is_makeup,s.replacement_session_id,original.id original_session_id,
				es.attendance_status,es.attendance_recorded_at,room.name room_name,coach.full_name coach_name
			from course_enrollments e join course_runs r on r.id=e.course_run_id join courses c on c.id=r.course_id
			join course_enrollment_sessions es on es.course_enrollment_id=e.id
			join course_sessions s on s.id=es.course_session_id
			left join rooms room on room.id=s.room_id
			left join accounts coach on coach.id=s.coach_account_id
			left join course_payments p on p.id=e.course_payment_id
			left join course_sessions original on original.replacement_session_id=s.id
			where e.member_account_id=? order by e.enrolled_at desc,e.id,es.unit_number
			""", memberId);
	}

	List<RosterEntry> rosterFindCourseEnrollmentSessions(UUID coachId, UUID sessionId) {
		return db.query("""
			select es.id,e.member_account_id,p.member_code,a.full_name,p.profile_image_url,
				es.attendance_status,es.attendance_recorded_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join course_sessions s on s.id=es.course_session_id and s.coach_account_id=?
			join accounts a on a.id=e.member_account_id join member_profiles p on p.account_id=a.id
			where es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status='ACTIVE'
			order by a.full_name,a.id
			""", (rs, row) -> new RosterEntry(rs.getObject("id", UUID.class), rs.getString("member_code"), rs.getString("full_name"), rs.getString("profile_image_url"), CourseAttendanceStatus.valueOf(rs.getString("attendance_status")), rs.getTimestamp("attendance_recorded_at") == null ? null : rs.getTimestamp("attendance_recorded_at").toInstant(), rs.getObject("member_account_id", UUID.class)), coachId, sessionId);
	}

	java.lang.Long rosterFindCourseSessions(UUID sessionId, UUID coachId) {
		return db.queryForObject("select count(*) from course_sessions where id=? and coach_account_id=?", Long.class, sessionId, coachId);
	}

	List<Map<java.lang.String, java.lang.Object>> updateAttendanceFindCourseRuns(UUID sessionId, UUID sessionId2, UUID coachId) {
		return db.queryForList("""
			with eligible_run as materialized (
				select r.id,r.status run_status from course_runs r where r.id=(select course_run_id from course_sessions where id=?)
				and r.status<>'CANCELLED' for update of r
			)
			select s.id,s.start_at,s.end_at,s.status,r.run_status from course_sessions s join eligible_run r on r.id=s.course_run_id
			where s.id=? and s.coach_account_id=? for update of s
			""", sessionId, sessionId2, coachId);
	}

	List<Map<java.lang.String, java.lang.Object>> updateAttendanceFindCourseEnrollmentSessions(UUID enrollmentSessionId, UUID sessionId) {
		return db.queryForList("""
			select es.id,e.member_account_id,p.member_code,a.full_name,p.profile_image_url,
				es.attendance_status,es.attendance_recorded_at
			from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			join accounts a on a.id=e.member_account_id join member_profiles p on p.account_id=a.id
			where es.id=? and es.course_session_id=? and es.disposition='ENROLLED'
			  and e.lifecycle_status='ACTIVE' for update of e,es
			""", enrollmentSessionId, sessionId);
	}

	int updateAttendanceUpdateCourseEnrollmentSessions(java.lang.String value1, Timestamp nowTimestamp, UUID coachId, Timestamp nowTimestamp2, UUID enrollmentSessionId) {
		return db.update("""
			update course_enrollment_sessions set attendance_status=?,attendance_recorded_at=coalesce(attendance_recorded_at,?),
			attendance_recorded_by=?,attendance_updated_at=?,version=version+1 where id=?
			""", value1, nowTimestamp, coachId, nowTimestamp2, enrollmentSessionId);
	}

	private final JdbcTemplate db;

	CourseEnrollmentRepository(JdbcTemplate db) {
		this.db = db;
	}
}
