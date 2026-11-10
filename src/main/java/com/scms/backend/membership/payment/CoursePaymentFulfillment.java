package com.scms.backend.membership.payment;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
import com.scms.backend.course.transfer.CourseTransferService;
import com.scms.backend.membership.payment.PaymentFulfillmentService.CourseUnit;
import com.scms.backend.notification.NotificationWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CoursePaymentFulfillment {

	private final JdbcTemplate db;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	CoursePaymentFulfillment(JdbcTemplate db, AuditEventRepository audits, NotificationWriter notifications) {
		this.db = db;
		this.audits = audits;
		this.notifications = notifications;
		this.paymentFulfillmentRepository = new PaymentFulfillmentRepository(this.db);
	}

	CourseEnrollmentResponse fulfillCourse(Map<String, Object> row, String method, UUID actorId, String providerTransactionId, Instant now, List<CourseUnit> units, CourseTransferService courseTransfers) {
		BigInteger paidAmount = amount(row, "payment_amount");
		BigInteger[] split = paidAmount.divideAndRemainder(BigInteger.valueOf(units.size()));
		UUID paymentId = uuid(row, "payment_id");
		if (paymentId == null) {
			paymentId = UUID.randomUUID();
			paymentFulfillmentRepository.fulfillCourseInsertCoursePayments(paymentId, uuid(row, "order_id"), uuid(row, "reservation_id"), uuid(row, "member_account_id"), uuid(row, "actor_account_id"), string(row, "order_reference"), row.get("payment_amount"), Timestamp.from(instant(row, "reservation_expires_at")), actorId, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
		} else {
			paymentFulfillmentRepository.fulfillCourseUpdateCoursePayments(method, "SEPAY".equals(method) ? "SEPAY" : null, providerTransactionId, actorId, Timestamp.from(now), paymentId);
		}
		paymentFulfillmentRepository.fulfillCourseUpdateCourseOrders(uuid(row, "order_id"));
		paymentFulfillmentRepository.fulfillCourseUpdateCourseReservations(uuid(row, "reservation_id"));
		if (courseTransfers != null) {
			CourseEnrollmentResponse transferred = courseTransfers.completePaidDifference(uuid(row, "order_id"), paymentId, actorId, now);
			if (transferred != null)
				return transferred;
		}
		UUID enrollmentId = UUID.randomUUID();
		paymentFulfillmentRepository.fulfillCourseInsertCourseEnrollments(enrollmentId, uuid(row, "order_id"), uuid(row, "reservation_id"), paymentId, uuid(row, "member_account_id"), uuid(row, "actor_account_id"), actorId, uuid(row, "course_run_id"), paidAmount, units.size(), Timestamp.from(now), paidAmount);
		for (int index = 0; index < units.size(); index++) {
			CourseUnit unit = units.get(index);
			BigInteger allocation = index == units.size() - 1 ? split[0].add(split[1]) : split[0];
			String disposition = unit.startAt().isBefore(now) ? "NOT_ENROLLED" : "ENROLLED";
			paymentFulfillmentRepository.fulfillCourseInsertCourseEnrollmentSessions(UUID.randomUUID(), enrollmentId, unit.id(), uuid(row, "course_run_id"), unit.unitNumber(), allocation, disposition);
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "COURSE_PAYMENT_PAID", "COURSE_ENROLLMENT", enrollmentId, Map.of("orderId", uuid(row, "order_id").toString(), "paymentId", paymentId.toString(), "memberAccountId", uuid(row, "member_account_id").toString(), "amountPaidVnd", paidAmount.toString())));
		notifications.write("COURSE_ENROLLMENT_CREATED:" + enrollmentId, uuid(row, "member_account_id"), "COURSE_ENROLLMENT_CREATED", "COURSE_ENROLLMENT", enrollmentId, Map.of("orderId", uuid(row, "order_id").toString(), "courseRunId", uuid(row, "course_run_id").toString()));
		return new CourseEnrollmentResponse(enrollmentId, uuid(row, "order_id"), uuid(row, "member_account_id"), uuid(row, "course_run_id"), paidAmount, units.size(), now);
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return MembershipPaymentFulfillment.uuid(row, key);
	}

	static String string(Map<String, Object> row, String key) {
		return MembershipPaymentFulfillment.string(row, key);
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return new BigDecimal(row.get(key).toString()).toBigIntegerExact();
	}

	static Instant instant(Map<String, Object> row, String key) {
		Object value = row.get(key);
		return value instanceof Timestamp timestamp ? timestamp.toInstant() : (Instant) value;
	}

	private final PaymentFulfillmentRepository paymentFulfillmentRepository;
}
