package com.scms.backend.course.transfer;

import static com.scms.backend.course.transfer.CourseTransferApiModels.TransferOffer;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.CourseTransferException;
import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
import com.scms.backend.notification.NotificationWriter;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseTransferCompletion {

	private final JdbcTemplate db;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final CourseTransferRepository courseTransferRepository;

	CourseTransferCompletion(JdbcTemplate db, AuditEventRepository audits, NotificationWriter notifications, CourseTransferRepository courseTransferRepository) {
		this.db = db;
		this.audits = audits;
		this.notifications = notifications;
		this.courseTransferRepository = courseTransferRepository;
	}

	CourseEnrollmentResponse completeTransfer(TransferOffer offer, UUID orderId, UUID paymentId, UUID actorId, Instant now) {
		if (!"ACTIVE".equals(db.queryForObject("select lifecycle_status from course_enrollments where id=? for update", String.class, offer.sourceEnrollmentId())))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SOURCE_INACTIVE", "The source Enrollment is not active.");
		UUID reservationId = orderId == null ? null : db.queryForObject("select id from course_reservations where course_order_id=?", UUID.class, orderId);
		UUID enrollmentId = UUID.randomUUID();
		UUID initiatorId = orderId == null ? actorId : db.queryForObject("select actor_account_id from course_orders where id=?", UUID.class, orderId);
		int totalUnits = db.queryForObject("select total_units from course_runs where id=?", Integer.class, offer.targetRunId());
		BigInteger appliedCredit = offer.creditVnd().min(offer.targetTuitionVnd());
		BigInteger cashPaid = offer.targetTuitionVnd().subtract(appliedCredit);
		db.update("""
			insert into course_enrollments(id,course_order_id,course_reservation_id,course_payment_id,member_account_id,
			 actor_account_id,processed_by_account_id,course_run_id,amount_paid_vnd_snapshot,total_units_snapshot,enrolled_at,
			 lifecycle_status,source_transfer_offer_id,credit_applied_vnd,cash_paid_vnd)
			 values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
			""", enrollmentId, orderId, reservationId, paymentId, offer.memberAccountId(), initiatorId, actorId, offer.targetRunId(), offer.targetTuitionVnd(), totalUnits, Timestamp.from(now), "ACTIVE", offer.id(), appliedCredit, cashPaid);
		List<Map<String, Object>> units = rows("select id,unit_number,start_at from course_sessions where course_run_id=? and is_makeup=false order by unit_number,id", offer.targetRunId());
		if (units.size() != totalUnits)
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_SCHEDULE_INVALID", "The target schedule does not match its unit count.");
		BigInteger[] split = offer.targetTuitionVnd().divideAndRemainder(BigInteger.valueOf(totalUnits));
		for (int index = 0; index < units.size(); index++) {
			Map<String, Object> unit = units.get(index);
			BigInteger allocation = index == units.size() - 1 ? split[0].add(split[1]) : split[0];
			db.update("insert into course_enrollment_sessions(id,course_enrollment_id,course_session_id,course_run_id,unit_number,allocation_vnd,disposition) values(?,?,?,?,?,?,?)", UUID.randomUUID(), enrollmentId, uuid(unit, "id"), offer.targetRunId(), number(unit, "unit_number"), allocation, instant(unit, "start_at").isBefore(now) ? "NOT_ENROLLED" : "ENROLLED");
		}
		db.update("update course_enrollments set lifecycle_status='TRANSFERRED' where id=? and lifecycle_status='ACTIVE'", offer.sourceEnrollmentId());
		db.update("update course_reservations set status='TRANSFERRED',updated_at=? where id=(select course_reservation_id from course_enrollments where id=?) and status='ENROLLED'", Timestamp.from(now), offer.sourceEnrollmentId());
		db.update("update course_transfer_holds set status='CONSUMED',released_at=? where transfer_offer_id=? and status='HELD'", Timestamp.from(now), offer.id());
		db.update("update course_transfer_offers set status='COMPLETED',completed_at=? where id=?", Timestamp.from(now), offer.id());
		db.update("update course_transfer_requests set status='COMPLETED',completed_at=? where id=?", Timestamp.from(now), offer.requestId());
		if (offer.creditVnd().compareTo(offer.targetTuitionVnd()) > 0)
			db.update("""
				insert into course_refund_obligations(id,course_enrollment_id,course_run_id,source_type,amount_vnd,
				 requester_account_id,request_reason)
				values(?,?,?,'TRANSFER_CREDIT_EXCESS',?,?,'Unused Course transfer credit refund')
				on conflict(course_enrollment_id,source_type,source_event_id) do nothing
				""", UUID.randomUUID(), offer.sourceEnrollmentId(), offer.sourceRunId(), offer.creditVnd().subtract(offer.targetTuitionVnd()), actorId);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(), Map.of("sourceEnrollmentId", offer.sourceEnrollmentId().toString(), "targetEnrollmentId", enrollmentId.toString(), "creditVnd", appliedCredit.toString(), "cashPaidVnd", cashPaid.toString())));
		Map<String, Object> completionPayload = new java.util.LinkedHashMap<>();
		completionPayload.put("targetEnrollmentId", enrollmentId.toString());
		completionPayload.put("targetRunId", offer.targetRunId().toString());
		completionPayload.put("memberAccountId", offer.memberAccountId().toString());
		completionPayload.put("actorAccountId", actorId == null ? null : actorId.toString());
		completionPayload.put("initiatorAccountId", initiatorId.toString());
		notifications.write("course-transfer-completed:" + offer.id(), offer.memberAccountId(), "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(), completionPayload);
		for (UUID managerId : db.queryForList("select id from accounts where role='MANAGER' and status='ACTIVE'", UUID.class)) notifications.write("course-transfer-completed:" + offer.id() + ":" + managerId, managerId, "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(), completionPayload);
		return new CourseEnrollmentResponse(enrollmentId, orderId, offer.memberAccountId(), offer.targetRunId(), offer.targetTuitionVnd(), totalUnits, now);
	}

	List<Map<String, Object>> rows(String sql, Object... args) {
		return courseTransferRepository.rows(sql, args);
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return CourseTransferRepository.uuid(row, key);
	}

	static int number(Map<String, Object> row, String key) {
		return CourseTransferRepository.number(row, key);
	}

	static Instant instant(Map<String, Object> row, String key) {
		return CourseTransferRepository.instant(row, key);
	}
}
