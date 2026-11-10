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
		var enrollments = CourseUnitRefundsRepository.createFindCourseEnrollments(db, runId);
		for (UUID enrollmentId : enrollments) {
			var units = CourseUnitRefundsRepository.createFindCourseEnrollmentSessions(db, enrollmentId);
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
			int inserted = CourseUnitRefundsRepository.createInsertCourseRefundObligations(db, id, enrollmentId, runId, source, eventId, new BigDecimal(amount), actorId, reason);
			// Preserve existing financial decisions; never rewrite approved or paid amounts.
			if (inserted == 0)
				continue;
			for (Unit unit : eligible) CourseUnitRefundsRepository.createInsertCourseRefundUnitClaims(db, unit.id(), enrollmentId, id, new BigDecimal(unit.allocation()), Timestamp.from(now));
			CourseUnitRefundsRepository.createInsertAuditEvents(db, actorId, id, source, amount.toString(), eligible.size());
			if ("REDUCED_SESSION_COUNT".equals(source))
				cancelStaleWithdrawal(db, enrollmentId, actorId, now);
		}
	}

	private static void cancelStaleWithdrawal(JdbcTemplate db, UUID enrollmentId, UUID actorId, Instant now) {
		// Request-time amounts are immutable; a new reduction must not leave an older overlapping snapshot approvable.
		var requests = CourseUnitRefundsRepository.cancelStaleWithdrawalFindCourseWithdrawalRequests(db, Timestamp.from(now), actorId, enrollmentId);
		if (requests.isEmpty())
			return;
		CourseUnitRefundsRepository.cancelStaleWithdrawalUpdateCourseEnrollments(db, enrollmentId);
		for (var request : requests) {
			CourseUnitRefundsRepository.cancelStaleWithdrawalInsertAuditEvents(db, actorId, request.get("id"));
			CourseUnitRefundsRepository.cancelStaleWithdrawalInsertNotificationOutbox(db, "course-withdrawal-cancelled:" + request.get("id"), request.get("member_account_id"), request.get("id"));
		}
	}
}
