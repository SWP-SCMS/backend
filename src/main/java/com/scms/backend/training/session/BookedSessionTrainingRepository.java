package com.scms.backend.training.session;

import com.scms.backend.training.TrainingResultException;
import com.scms.backend.training.session.BookedSessionTrainingService.Session;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class BookedSessionTrainingRepository {

	private final JdbcTemplate db;

	BookedSessionTrainingRepository(JdbcTemplate db) {
		this.db = db;
	}

	boolean enrolled(UUID sessionId, UUID memberId, boolean course, boolean lock) {
		String query = course ? """
			select es.id from course_enrollment_sessions es join course_enrollments e on e.id=es.course_enrollment_id
			where es.course_session_id=? and e.member_account_id=? and es.disposition='ENROLLED' and e.lifecycle_status='ACTIVE'
			""" : "select id from bookings where class_session_id=? and member_account_id=? and status='BOOKED'";
		if (lock)
			query += course ? " for update of e,es" : " for update";
		return !db.queryForList(query, UUID.class, sessionId, memberId).isEmpty();
	}

	Session sessionRow(UUID coachId, UUID id, boolean course, boolean lock) {
		if (course && lock) {
			// Match the Course mutation lock order: Run, then Session, then enrollment.
			db.queryForList("select id from course_runs where id=(select course_run_id from course_sessions where id=? and coach_account_id=?) for update", id, coachId);
		}
		String query = course ? """
			select s.start_at starts_at,s.end_at ends_at,s.status,r.status run_status
			from course_sessions s join course_runs r on r.id=s.course_run_id
			where s.id=? and s.coach_account_id=?
			""" : """
			select s.start_time starts_at,s.end_time ends_at,s.status,'IN_PROGRESS' run_status
			from class_sessions s join sport_classes c on c.id=s.sport_class_id
			where s.id=? and s.teaching_coach_account_id=? and c.class_type='PT_1_1'
			""";
		return db.query(query + (lock ? " for update of s" : ""), (rs, row) -> new Session(rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(), rs.getString("status"), rs.getString("run_status")), id, coachId).stream().findFirst().orElseThrow(TrainingResultException::bookedSessionNotFound);
	}
}
