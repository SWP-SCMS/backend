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
		if (!"ACTIVE".equals(courseTransferRepository.completeTransferFindCourseEnrollments(offer.sourceEnrollmentId())))
			throw CourseTransferException.conflict("COURSE_TRANSFER_SOURCE_INACTIVE", "The source Enrollment is not active.");
		UUID reservationId = orderId == null ? null : courseTransferRepository.completeTransferFindCourseReservations(orderId);
		UUID enrollmentId = UUID.randomUUID();
		UUID initiatorId = orderId == null ? actorId : courseTransferRepository.completeTransferFindCourseOrders(orderId);
		int totalUnits = courseTransferRepository.completeTransferFindCourseRuns(offer.targetRunId());
		BigInteger appliedCredit = offer.creditVnd().min(offer.targetTuitionVnd());
		BigInteger cashPaid = offer.targetTuitionVnd().subtract(appliedCredit);
		courseTransferRepository.completeTransferInsertCourseEnrollments(enrollmentId, orderId, reservationId, paymentId, offer.memberAccountId(), initiatorId, actorId, offer.targetRunId(), offer.targetTuitionVnd(), totalUnits, Timestamp.from(now), "ACTIVE", offer.id(), appliedCredit, cashPaid);
		List<Map<String, Object>> units = courseTransferRepository.completeTransferRowsCourseSessions(offer.targetRunId());
		if (units.size() != totalUnits)
			throw CourseTransferException.conflict("COURSE_TRANSFER_TARGET_SCHEDULE_INVALID", "The target schedule does not match its unit count.");
		BigInteger[] split = offer.targetTuitionVnd().divideAndRemainder(BigInteger.valueOf(totalUnits));
		for (int index = 0; index < units.size(); index++) {
			Map<String, Object> unit = units.get(index);
			BigInteger allocation = index == units.size() - 1 ? split[0].add(split[1]) : split[0];
			courseTransferRepository.completeTransferInsertCourseEnrollmentSessions(UUID.randomUUID(), enrollmentId, uuid(unit, "id"), offer.targetRunId(), number(unit, "unit_number"), allocation, instant(unit, "start_at").isBefore(now) ? "NOT_ENROLLED" : "ENROLLED");
		}
		courseTransferRepository.completeTransferUpdateCourseEnrollments(offer.sourceEnrollmentId());
		courseTransferRepository.completeTransferUpdateCourseReservations(Timestamp.from(now), offer.sourceEnrollmentId());
		courseTransferRepository.completeTransferUpdateCourseTransferHolds(Timestamp.from(now), offer.id());
		courseTransferRepository.completeTransferUpdateCourseTransferOffers(Timestamp.from(now), offer.id());
		courseTransferRepository.completeTransferUpdateCourseTransferRequests(Timestamp.from(now), offer.requestId());
		if (offer.creditVnd().compareTo(offer.targetTuitionVnd()) > 0)
			courseTransferRepository.completeTransferInsertCourseRefundObligations(UUID.randomUUID(), offer.sourceEnrollmentId(), offer.sourceRunId(), offer.creditVnd().subtract(offer.targetTuitionVnd()), actorId);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(), Map.of("sourceEnrollmentId", offer.sourceEnrollmentId().toString(), "targetEnrollmentId", enrollmentId.toString(), "creditVnd", appliedCredit.toString(), "cashPaidVnd", cashPaid.toString())));
		Map<String, Object> completionPayload = new java.util.LinkedHashMap<>();
		completionPayload.put("targetEnrollmentId", enrollmentId.toString());
		completionPayload.put("targetRunId", offer.targetRunId().toString());
		completionPayload.put("memberAccountId", offer.memberAccountId().toString());
		completionPayload.put("actorAccountId", actorId == null ? null : actorId.toString());
		completionPayload.put("initiatorAccountId", initiatorId.toString());
		notifications.write("course-transfer-completed:" + offer.id(), offer.memberAccountId(), "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(), completionPayload);
		for (UUID managerId : courseTransferRepository.completeTransferFindAccounts()) notifications.write("course-transfer-completed:" + offer.id() + ":" + managerId, managerId, "COURSE_TRANSFER_COMPLETED", "COURSE_TRANSFER_OFFER", offer.id(), completionPayload);
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
