package com.scms.backend.course.refund;

import com.scms.backend.course.refund.CourseUnitRefunds.Unit;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseUnitRefundsRepository {

	static List<UUID> createFindCourseEnrollments(JdbcTemplate db, UUID runId) {
		return db.queryForList("""
			select e.id from course_enrollments e join course_payments p on p.id=e.course_payment_id
			where e.course_run_id=? and p.status='PAID' and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by e.id for update of e
			""", UUID.class, runId);
	}

	static List<Unit> createFindCourseEnrollmentSessions(JdbcTemplate db, UUID enrollmentId) {
		return db.query("""
			select es.id,es.allocation_vnd,es.disposition,
			    coalesce(replacement.status,s.status) effective_status,
			    coalesce(replacement.start_at,s.start_at) effective_start
			from course_enrollment_sessions es join course_sessions s on s.id=es.course_session_id
			left join course_sessions replacement on replacement.id=s.replacement_session_id
			where es.course_enrollment_id=? and not exists
			    (select 1 from course_refund_unit_claims claim where claim.enrollment_session_id=es.id)
			order by es.unit_number for update of es
			""", (rs, row) -> new Unit(rs.getObject("id", UUID.class), rs.getBigDecimal("allocation_vnd").toBigIntegerExact(), rs.getString("disposition"), rs.getString("effective_status"), rs.getTimestamp("effective_start").toInstant()), enrollmentId);
	}

	static int createInsertCourseRefundObligations(JdbcTemplate db, UUID id, UUID enrollmentId, UUID runId, java.lang.String source, UUID eventId, BigDecimal value6, UUID actorId, java.lang.String reason) {
		return db.update("""
			insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,source_event_id,
			    amount_vnd,requester_account_id,request_reason)
			values(?,?,?,?,?,?,?,?) on conflict(course_enrollment_id,source_type,source_event_id) do nothing
			""", id, enrollmentId, runId, source, eventId, value6, actorId, reason);
	}

	static int createInsertCourseRefundUnitClaims(JdbcTemplate db, UUID unitId, UUID enrollmentId, UUID id, BigDecimal value4, Timestamp nowTimestamp) {
		return db.update("""
			insert into course_refund_unit_claims(enrollment_session_id,course_enrollment_id,refund_obligation_id,allocation_vnd,created_at)
			values(?,?,?,?,?)
			""", unitId, enrollmentId, id, value4, nowTimestamp);
	}

	static int createInsertAuditEvents(JdbcTemplate db, UUID actorId, UUID id, java.lang.String source, java.lang.String amount, int size) {
		return db.update("""
			insert into audit_events(id,actor_account_id,action,target_type,target_id,after_data)
			values(gen_random_uuid(),?,'COURSE_REFUND_CREATED','COURSE_REFUND_OBLIGATION',?,
			    jsonb_build_object('source',cast(? as text),'amountVnd',cast(? as text),'unitCount',cast(? as integer)))
			""", actorId, id, source, amount, size);
	}

	static List<java.util.Map<java.lang.String, java.lang.Object>> cancelStaleWithdrawalFindCourseWithdrawalRequests(JdbcTemplate db, Timestamp nowTimestamp, UUID actorId, UUID enrollmentId) {
		return db.queryForList("""
			update course_withdrawal_requests set status='CANCELLED',cancelled_at=?,cancelled_by_account_id=?,
			    cancellation_reason='REDUCED_SESSION_COUNT'
			where course_enrollment_id=? and status='PENDING' returning id,member_account_id
			""", nowTimestamp, actorId, enrollmentId);
	}

	static int cancelStaleWithdrawalUpdateCourseEnrollments(JdbcTemplate db, UUID enrollmentId) {
		return db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", enrollmentId);
	}

	static int cancelStaleWithdrawalInsertAuditEvents(JdbcTemplate db, UUID actorId, java.lang.Object id) {
		return db.update("""
			insert into audit_events(id,actor_account_id,action,target_type,target_id,reason,before_data,after_data)
			values(gen_random_uuid(),?,'COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_REQUEST',?,'REDUCED_SESSION_COUNT',
			    '{"status":"PENDING"}'::jsonb,'{"status":"CANCELLED"}'::jsonb)
			""", actorId, id);
	}

	static int cancelStaleWithdrawalInsertNotificationOutbox(JdbcTemplate db, java.lang.String value1, java.lang.Object memberAccountId, java.lang.Object id) {
		return db.update("""
			insert into notification_outbox(id,event_key,recipient_account_id,notification_type,target_type,target_id,payload)
			values(gen_random_uuid(),?,?,'COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_REQUEST',?,
			    '{"reason":"REDUCED_SESSION_COUNT"}'::jsonb) on conflict(event_key) do nothing
			""", value1, memberAccountId, id);
	}

	private CourseUnitRefundsRepository() {
	}
}
