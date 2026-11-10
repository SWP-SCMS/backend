package com.scms.backend.course.session;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseSessionDisruptionRepository {

	int cancelInternalUpdateCourseEnrollmentSessions(UUID originalId, UUID sessionId) {
		return db.update("update course_enrollment_sessions set course_session_id=? where course_session_id=? and disposition='ENROLLED'", originalId, sessionId);
	}

	int cancelWithMakeupUpdateCourseEnrollmentSessions(UUID makeupId, UUID sessionId) {
		return db.update("update course_enrollment_sessions set course_session_id=? where course_session_id=? and disposition='ENROLLED'", makeupId, sessionId);
	}

	int scheduleMakeupUpdateCourseEnrollmentSessions(UUID makeupId, UUID sessionId) {
		return db.update("update course_enrollment_sessions set course_session_id=? where course_session_id=? and disposition='ENROLLED'", makeupId, sessionId);
	}

	List<UUID> advanceFindCourseRuns(Timestamp at, Timestamp at2, Timestamp warningBefore, Timestamp at3, Timestamp at4) {
		return db.queryForList("""
			select run.id from course_runs run
			where run.status in ('OPEN_FOR_REGISTRATION','CONFIRMED','IN_PROGRESS','SETTLEMENT_PENDING')
			 and ((run.status='CONFIRMED' and run.planned_start_at<=?)
			  or exists (select 1 from course_sessions s where s.course_run_id=run.id
			   and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.end_at<=?)
			  or exists (select 1 from course_sessions s where s.course_run_id=run.id
			   and s.status='CANCELLED_PENDING_MAKEUP' and s.replacement_session_id is null
			   and s.is_makeup=false and s.original_end_at<=? and s.makeup_warning_at is null)
			  or (run.status in ('IN_PROGRESS','SETTLEMENT_PENDING') and
			""" + COMPLETABLE + ")) order by run.id limit 100 for update of run skip locked", UUID.class, at, at2, warningBefore, at3, at4);
	}

	List<UUID> advanceFindCourseSessions(UUID runId) {
		return db.queryForList("select id from course_sessions where course_run_id=? order by id for update", UUID.class, runId);
	}

	List<UUID> markMakeupWarnings(Timestamp at, UUID runId, Timestamp warningBefore) {
		return db.queryForList("""
			update course_sessions set makeup_warning_at=? where course_run_id=?
			 and status='CANCELLED_PENDING_MAKEUP' and replacement_session_id is null
			 and is_makeup=false and original_end_at<=? and makeup_warning_at is null returning id
			""", UUID.class, at, runId, warningBefore);
	}

	int advanceUpdateCourseRuns(Timestamp at, UUID runId, Timestamp at2) {
		return db.update("update course_runs set status='IN_PROGRESS',version=version+1,updated_at=? " + "where id=? and status='CONFIRMED' and planned_start_at<=?", at, runId, at2);
	}

	int advanceUpdateCourseSessions(Timestamp at, UUID runId, Timestamp at2) {
		return db.update("update course_sessions set status='COMPLETED',version=version+1,updated_at=? " + "where course_run_id=? and status in ('SCHEDULED','MAKEUP_SCHEDULED') and end_at<=?", at, runId, at2);
	}

	java.lang.Boolean isRunCompletable(UUID runId, Timestamp at, Timestamp at2) {
		return db.queryForObject("select exists(select 1 from course_runs run where run.id=? " + "and run.status in ('IN_PROGRESS','SETTLEMENT_PENDING') and " + COMPLETABLE + ")", Boolean.class, runId, at, at2);
	}

	int settleUpdateCourseSessions(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_sessions set status='UNRESOLVED',version=version+1,updated_at=? " + "where course_run_id=? and status='CANCELLED_PENDING_MAKEUP' and replacement_session_id is null " + "and is_makeup=false", nowTimestamp, runId);
	}

	int settleUpdateCourseEnrollmentSessions(UUID runId) {
		return db.update("update course_enrollment_sessions enrollment_session set disposition='UNRESOLVED' " + "from course_sessions session where enrollment_session.course_session_id=session.id " + "and session.course_run_id=? and session.status='UNRESOLVED' and enrollment_session.disposition='ENROLLED'", runId);
	}

	java.lang.Integer settleFindCourseRefundObligations(UUID runId) {
		return db.queryForObject("select count(*) from course_refund_obligations " + "where course_run_id=? and source_type='UNRESOLVED_SESSIONS'", Integer.class, runId);
	}

	int settleUpdateCourseRuns(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_runs set status='SETTLEMENT_PENDING',version=version+1,updated_at=? " + "where id=? and status='IN_PROGRESS'", nowTimestamp, runId);
	}

	int completeSettledRun(Timestamp nowTimestamp, UUID runId) {
		return db.update("update course_runs set status='COMPLETED',version=version+1,updated_at=? " + "where id=? and status='SETTLEMENT_PENDING'", nowTimestamp, runId);
	}

	List<UUID> settleFindCourseEnrollments(UUID runId) {
		return db.queryForList("select distinct member_account_id from course_enrollments where course_run_id=? order by member_account_id", UUID.class, runId);
	}

	List<UUID> enrolledMembersFindCourseEnrollmentSessions(UUID sessionId) {
		return db.query("select e.member_account_id from course_enrollment_sessions es " + "join course_enrollments e on e.id=es.course_enrollment_id " + "where es.course_session_id=? and es.disposition='ENROLLED' and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING') order by e.member_account_id", (row, index) -> row.getObject(1, UUID.class), sessionId);
	}

	private static final String COMPLETABLE = """
		(select count(*) from course_sessions original where original.course_run_id=run.id
		 and original.is_makeup=false and original.status<>'REMOVED_BY_RUN_CHANGE')=run.total_units
		and not exists (select 1 from course_sessions original
		 left join course_sessions replacement on replacement.id=original.replacement_session_id
		 where original.course_run_id=run.id and original.is_makeup=false and original.status<>'REMOVED_BY_RUN_CHANGE'
		 and (coalesce(replacement.end_at,original.end_at)>?
		  or not (coalesce(replacement.status,original.status) in ('SCHEDULED','MAKEUP_SCHEDULED','COMPLETED','UNRESOLVED','RESOLVED_BY_REFUND')
		   or (original.status='CANCELLED_PENDING_MAKEUP' and original.replacement_session_id is null))))
		and not exists (select 1 from course_sessions s where s.course_run_id=run.id
		 and s.status in ('SCHEDULED','MAKEUP_SCHEDULED') and s.end_at>?)
		""";

	private final JdbcTemplate db;

	CourseSessionDisruptionRepository(JdbcTemplate db) {
		this.db = db;
	}
}
