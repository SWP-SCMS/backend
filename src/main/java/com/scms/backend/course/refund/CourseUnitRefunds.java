package com.scms.backend.course.refund;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Invoked inside the caller's transaction after locking the run and flushing schedule changes.
 */
final public class CourseUnitRefunds {

	private CourseUnitRefunds() {
	}

	record Unit(UUID id, BigInteger allocation, String disposition, String status, Instant start) {
	}

	static boolean unserved(Unit unit, Instant now) {
		if ("NOT_ENROLLED".equals(unit.disposition()) || "COMPLETED".equals(unit.status()) || "RESOLVED_BY_REFUND".equals(unit.status()))
			return false;
		// A normally held session counts as provided even if the member is absent or the scheduler is late.
		return !(List.of("SCHEDULED", "MAKEUP_SCHEDULED").contains(unit.status()) && !unit.start().isAfter(now));
	}

	static List<Unit> eligible(List<Unit> unclaimedUnits, Instant now, String source) {
		return unclaimedUnits.stream().filter(u -> u.allocation().signum() > 0 && unserved(u, now)).filter(u -> switch(source) {
			case "REDUCED_SESSION_COUNT" ->
				"REMOVED_BY_RUN_CHANGE".equals(u.disposition());
			case "UNRESOLVED_SESSIONS" ->
				"UNRESOLVED".equals(u.disposition());
			default ->
				!"REMOVED_BY_RUN_CHANGE".equals(u.disposition());
		}).toList();
	}

	public static void reduced(JdbcTemplate db, UUID runId, UUID eventId, UUID actorId, Instant now) {
		create(db, runId, eventId, actorId, now, "REDUCED_SESSION_COUNT");
	}

	public static void cancelled(JdbcTemplate db, UUID runId, UUID actorId, Instant now) {
		// Legacy removed units without a claim still retain their original entitlement.
		reduced(db, runId, null, actorId, now);
		create(db, runId, null, actorId, now, "COURSE_RUN_CANCELLED");
	}

	public static void settled(JdbcTemplate db, UUID runId, Instant now) {
		reduced(db, runId, null, null, now);
		create(db, runId, null, null, now, "UNRESOLVED_SESSIONS");
	}

	private static void create(JdbcTemplate db, UUID runId, UUID eventId, UUID actorId, Instant now, String source) {
		var enrollments = db.queryForList("""
			select e.id from course_enrollments e join course_payments p on p.id=e.course_payment_id
			where e.course_run_id=? and p.status='PAID' and e.lifecycle_status in ('ACTIVE','WITHDRAWAL_PENDING')
			order by e.id for update of e
			""", UUID.class, runId);
		for (UUID enrollmentId : enrollments) {
			var units = db.query("""
				select es.id,es.allocation_vnd,es.disposition,
				    coalesce(replacement.status,s.status) effective_status,
				    coalesce(replacement.start_at,s.start_at) effective_start
				from course_enrollment_sessions es join course_sessions s on s.id=es.course_session_id
				left join course_sessions replacement on replacement.id=s.replacement_session_id
				where es.course_enrollment_id=? and not exists
				    (select 1 from course_refund_unit_claims claim where claim.enrollment_session_id=es.id)
				order by es.unit_number for update of es
				""", (rs, row) -> new Unit(rs.getObject("id", UUID.class), rs.getBigDecimal("allocation_vnd").toBigIntegerExact(), rs.getString("disposition"), rs.getString("effective_status"), rs.getTimestamp("effective_start").toInstant()), enrollmentId);
			var eligible = eligible(units, now, source);
			BigInteger amount = eligible.stream().map(Unit::allocation).reduce(BigInteger.ZERO, BigInteger::add);
			if (amount.signum() == 0)
				continue;
			UUID id = UUID.randomUUID();
			String reason = switch(source) {
				case "REDUCED_SESSION_COUNT" ->
					"Hoàn tiền các buổi bị giảm theo phân bổ lúc thanh toán";
				case "UNRESOLVED_SESSIONS" ->
					"Hoàn tiền các buổi chưa được cung cấp";
				default ->
					"Hủy đợt: hoàn các buổi chưa học theo phân bổ lúc thanh toán";
			};
			int inserted = db.update("""
				insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,source_event_id,
				    amount_vnd,requester_account_id,request_reason)
				values(?,?,?,?,?,?,?,?) on conflict(course_enrollment_id,source_type,source_event_id) do nothing
				""", id, enrollmentId, runId, source, eventId, new BigDecimal(amount), actorId, reason);
			// Preserve existing financial decisions; never rewrite approved or paid amounts.
			if (inserted == 0)
				continue;
			for (Unit unit : eligible) db.update("""
				insert into course_refund_unit_claims(enrollment_session_id,course_enrollment_id,refund_obligation_id,allocation_vnd,created_at)
				values(?,?,?,?,?)
				""", unit.id(), enrollmentId, id, new BigDecimal(unit.allocation()), Timestamp.from(now));
			db.update("""
				insert into audit_events(id,actor_account_id,action,target_type,target_id,after_data)
				values(gen_random_uuid(),?,'COURSE_REFUND_CREATED','COURSE_REFUND_OBLIGATION',?,
				    jsonb_build_object('source',cast(? as text),'amountVnd',cast(? as text),'unitCount',cast(? as integer)))
				""", actorId, id, source, amount.toString(), eligible.size());
			if ("REDUCED_SESSION_COUNT".equals(source))
				cancelStaleWithdrawal(db, enrollmentId, actorId, now);
		}
	}

	private static void cancelStaleWithdrawal(JdbcTemplate db, UUID enrollmentId, UUID actorId, Instant now) {
		// Request-time amounts are immutable; a new reduction must not leave an older overlapping snapshot approvable.
		var requests = db.queryForList("""
			update course_withdrawal_requests set status='CANCELLED',cancelled_at=?,cancelled_by_account_id=?,
			    cancellation_reason='REDUCED_SESSION_COUNT'
			where course_enrollment_id=? and status='PENDING' returning id,member_account_id
			""", Timestamp.from(now), actorId, enrollmentId);
		if (requests.isEmpty())
			return;
		db.update("update course_enrollments set lifecycle_status='ACTIVE' where id=? and lifecycle_status='WITHDRAWAL_PENDING'", enrollmentId);
		for (var request : requests) {
			db.update("""
				insert into audit_events(id,actor_account_id,action,target_type,target_id,reason,before_data,after_data)
				values(gen_random_uuid(),?,'COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_REQUEST',?,'REDUCED_SESSION_COUNT',
				    '{"status":"PENDING"}'::jsonb,'{"status":"CANCELLED"}'::jsonb)
				""", actorId, request.get("id"));
			db.update("""
				insert into notification_outbox(id,event_key,recipient_account_id,notification_type,target_type,target_id,payload)
				values(gen_random_uuid(),?,?,'COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_REQUEST',?,
				    '{"reason":"REDUCED_SESSION_COUNT"}'::jsonb) on conflict(event_key) do nothing
				""", "course-withdrawal-cancelled:" + request.get("id"), request.get("member_account_id"), request.get("id"));
		}
	}
}
