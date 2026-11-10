package com.scms.backend.membership.payment;

import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.enrollment.CourseEnrollmentResponse;
import com.scms.backend.course.transfer.CourseTransferService;
import com.scms.backend.membership.FulfillmentMode;
import com.scms.backend.membership.MembershipLifecycleService;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.SepayWebhookOutcome;
import com.scms.backend.notification.NotificationWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentFulfillmentService {

	public void notifyFailedReconciliation(UUID paymentId, Map<String, Object> payment) {
		notifications.write("PAYMENT_FAILED:" + paymentId, uuid(payment, "member_account_id"), "PAYMENT_FAILED", "PAYMENT", paymentId, Map.of("orderId", uuid(payment, "order_id").toString(), "status", "FAILED"));
	}

	private static final Logger log = LoggerFactory.getLogger(PaymentFulfillmentService.class);

	private final JdbcTemplate db;

	private final MembershipLifecycleService lifecycle;

	private final AuditEventRepository audits;

	private final NotificationWriter notifications;

	private final Clock clock;

	@Autowired(required = false)
	private CourseTransferService courseTransfers;

	public PaymentFulfillmentService(JdbcTemplate db, MembershipLifecycleService lifecycle, AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.db = db;
		this.lifecycle = lifecycle;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
		this.paymentProviderEventRepository = new PaymentProviderEventRepository(this.db);
		this.paymentFulfillmentRepository = new PaymentFulfillmentRepository(this.db);
		this.membershipPaymentFulfillment = new MembershipPaymentFulfillment(this.db, this.lifecycle, this.audits, this.notifications);
		this.coursePaymentFulfillment = new CoursePaymentFulfillment(this.db, this.audits, this.notifications);
	}

	@Transactional
	PaymentResultResponse fulfill(UUID paymentId, UUID actorId, String providerTransactionId, String evidence, String reason) {
		lifecycle.reconcile(memberIdForPayment(paymentId), clock.instant());
		Map<String, Object> row = paymentForUpdate(paymentId);
		String status = string(row, "payment_status");
		if ("PAID".equals(status)) {
			String existing = string(row, "provider_transaction_id");
			if (providerTransactionId != null && existing != null && !providerTransactionId.equals(existing)) {
				throw PaymentException.conflict("Payment was already confirmed with another provider transaction");
			}
			return result(row);
		}
		if (!"PENDING".equals(status))
			throw PaymentException.conflict("Payment is terminal");
		if (!"PENDING_PAYMENT".equals(string(row, "order_status")))
			throw PaymentException.conflict("Order is not payable");
		if (providerTransactionId == null || providerTransactionId.isBlank()) {
			throw PaymentException.validation("providerTransactionId is required");
		}
		String provider = string(row, "provider");
		if (provider == null)
			provider = "SEPAY";
		lockProviderTransaction(provider, providerTransactionId);
		if (providerTransactionClaimed(provider, providerTransactionId, paymentId, null)) {
			throw PaymentException.conflict("Provider transaction was already used");
		}
		ensureMembershipCanFulfill(row);
		return fulfillLocked(row, paymentId, actorId, provider, providerTransactionId, evidence, reason, clock.instant());
	}

	@Transactional
	SepayWebhookResponse processSepayWebhook(UUID paymentId, UUID coursePaymentId, SepayWebhookRequest request, String normalizedReference, String payloadSha256, Instant receivedAt) {
		Map<String, Object> existingEvent = event(request.id().toString());
		if (existingEvent != null)
			return replayEvent(paymentId, coursePaymentId, request, payloadSha256, existingEvent);
		if (paymentId == null && coursePaymentId != null) {
			return processCourseSepayWebhook(coursePaymentId, request, normalizedReference, payloadSha256, receivedAt);
		}
		if (paymentId == null)
			return finishEvent(null, request, normalizedReference, payloadSha256, SepayWebhookOutcome.REFERENCE_MISMATCH, "Payment reference was not found", false);
		lifecycle.reconcile(memberIdForPayment(paymentId), clock.instant());
		Map<String, Object> row = paymentForUpdate(paymentId);
		String transactionId = request.id().toString();
		lockProviderTransaction("SEPAY", transactionId);
		existingEvent = event(transactionId);
		if (existingEvent != null)
			return replayEvent(paymentId, coursePaymentId, request, payloadSha256, existingEvent);
		if (providerTransactionClaimed("SEPAY", transactionId, paymentId, null)) {
			return finishEvent(null, request, normalizedReference, payloadSha256, SepayWebhookOutcome.ALREADY_PROCESSED, "Provider transaction was already used", false);
		}
		if ("PAID".equals(string(row, "payment_status")) && transactionId.equals(string(row, "provider_transaction_id"))) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.ALREADY_PROCESSED, "Provider transaction was already applied", true);
		}
		if (!"PENDING_PAYMENT".equals(string(row, "order_status")) || !"PENDING".equals(string(row, "payment_status")) || !"in".equalsIgnoreCase(request.transferType())) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.PAYMENT_NOT_PAYABLE, "Payment or Order is not payable", false);
		}
		if (blank(row, "bank_code_snapshot") || blank(row, "bank_account_number_snapshot") || blank(row, "bank_account_name_snapshot")) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.DESTINATION_SNAPSHOT_MISSING, "Payment destination snapshot is missing", false);
		}
		String expectedReference = normalizeReference(string(row, "bank_transfer_content"));
		if (expectedReference.isBlank() || normalizedReference.isBlank() || !normalizedReference.equals(expectedReference) || !normalizeReference(request.content()).contains(expectedReference)) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.REFERENCE_MISMATCH, "Transfer reference or content does not match", false);
		}
		if (!"VND".equalsIgnoreCase(string(row, "payment_currency")) || !amount(row, "payment_amount").equals(request.transferAmount())) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.AMOUNT_MISMATCH, "Transfer amount or currency does not match", false);
		}
		if (request.accountNumber() == null || !string(row, "bank_account_number_snapshot").equals(request.accountNumber().trim())) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.ACCOUNT_MISMATCH, "Destination account does not match", false);
		}
		Instant now = clock.instant();
		Instant expiresAt = instant(row, "expires_at");
		if (expiresAt == null || !now.isBefore(expiresAt)) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED, "Payment window is missing or expired", false);
		}
		try {
			ensureMembershipCanFulfill(row);
		} catch (PaymentException exception) {
			return finishEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.PAYMENT_NOT_PAYABLE, "Membership cannot be fulfilled", false);
		}
		if (!insertFinalEvent(paymentId, request, normalizedReference, payloadSha256, SepayWebhookOutcome.FULFILLED, "Payment fulfilled automatically")) {
			return replayEvent(paymentId, request, payloadSha256, event(request.id().toString()));
		}
		PaymentResultResponse result = fulfillLocked(row, paymentId, null, "SEPAY", transactionId, "SePay webhook transaction " + transactionId, "SePay incoming transfer confirmed", now);
		return new SepayWebhookResponse(true, true, SepayWebhookOutcome.FULFILLED, result.paymentId(), result.orderId(), result.status());
	}

	private SepayWebhookResponse processCourseSepayWebhook(UUID coursePaymentId, SepayWebhookRequest request, String normalizedReference, String payloadSha256, Instant receivedAt) {
		Map<String, Object> row = coursePaymentForUpdate(coursePaymentId);
		String transactionId = request.id().toString();
		lockProviderTransaction("SEPAY", transactionId);
		Map<String, Object> existingEvent = event(transactionId);
		if (existingEvent != null)
			return replayEvent(null, coursePaymentId, request, payloadSha256, existingEvent);
		if (providerTransactionClaimed("SEPAY", transactionId, null, coursePaymentId)) {
			return finishEvent(null, request, normalizedReference, payloadSha256, SepayWebhookOutcome.ALREADY_PROCESSED, "Provider transaction was already used", false);
		}
		if ("PAID".equals(string(row, "payment_status")) && "SEPAY".equals(string(row, "payment_method")) && transactionId.equals(string(row, "provider_transaction_id")) && "in".equalsIgnoreCase(request.transferType()) && amount(row, "payment_amount").equals(request.transferAmount()) && normalizeReference(string(row, "bank_transfer_content")).equals(normalizedReference) && normalizeReference(request.content()).contains(normalizedReference) && string(row, "bank_account_number_snapshot").equals(request.accountNumber())) {
			insertFinalEvent(null, coursePaymentId, request, normalizedReference, payloadSha256, SepayWebhookOutcome.ALREADY_PROCESSED, "Course payment already reconciled");
			return webhookResponse(row, true, SepayWebhookOutcome.ALREADY_PROCESSED);
		}
		if (!"PENDING_PAYMENT".equals(string(row, "order_status")) || !"HELD".equals(string(row, "reservation_status")) || !"PENDING".equals(string(row, "payment_status")) || !courseRunPayable(string(row, "run_status")) || !"in".equalsIgnoreCase(request.transferType())) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.PAYMENT_NOT_PAYABLE, "Course Payment is not payable");
		}
		if (blank(row, "bank_code_snapshot") || blank(row, "bank_account_number_snapshot") || blank(row, "bank_account_name_snapshot")) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.DESTINATION_SNAPSHOT_MISSING, "Course Payment destination snapshot is missing");
		}
		String expectedReference = normalizeReference(string(row, "bank_transfer_content"));
		if (expectedReference.isBlank() || normalizedReference.isBlank() || !normalizedReference.equals(expectedReference) || !normalizeReference(request.content()).contains(expectedReference)) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.REFERENCE_MISMATCH, "Course transfer reference or content does not match");
		}
		if (!"VND".equalsIgnoreCase(string(row, "payment_currency")) || !amount(row, "payment_amount").equals(request.transferAmount())) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.AMOUNT_MISMATCH, "Course transfer amount or currency does not match");
		}
		if (request.accountNumber() == null || !string(row, "bank_account_number_snapshot").equals(request.accountNumber().trim())) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.ACCOUNT_MISMATCH, "Course destination account does not match");
		}
		Instant now = clock.instant();
		Instant deadline = instant(row, "expires_at");
		Instant reservationExpiry = instant(row, "reservation_expires_at");
		if (deadline == null || reservationExpiry == null || !receivedAt.isBefore(deadline) || !receivedAt.isBefore(reservationExpiry)) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.PAYMENT_WINDOW_EXPIRED, "Course payment window is missing or expired");
		}
		List<CourseUnit> units;
		try {
			units = courseUnits(row, now);
		} catch (PaymentException exception) {
			return finishCourseEvent(row, request, normalizedReference, payloadSha256, SepayWebhookOutcome.PAYMENT_NOT_PAYABLE, "Course enrollment is no longer available");
		}
		if (!insertFinalEvent(null, coursePaymentId, request, normalizedReference, payloadSha256, SepayWebhookOutcome.FULFILLED, "Course payment fulfilled")) {
			return replayEvent(null, coursePaymentId, request, payloadSha256, event(request.id().toString()));
		}
		fulfillCourse(row, "SEPAY", null, request.id().toString(), now, units);
		row.put("payment_status", "PAID");
		row.put("order_status", "PAID");
		row.put("reservation_status", "ENROLLED");
		return webhookResponse(row, true, SepayWebhookOutcome.FULFILLED);
	}

	@Transactional
	CourseEnrollmentResponse fulfillCourseCash(UUID memberId, UUID orderId, UUID actorId) {
		Map<String, Object> row = courseOrderForUpdate(memberId, orderId);
		if ("PAID".equals(string(row, "order_status")))
			return courseEnrollment(orderId, memberId);
		if (!"PENDING_PAYMENT".equals(string(row, "order_status")) || !"HELD".equals(string(row, "reservation_status")) || !courseRunPayable(string(row, "run_status")) || !instant(row, "reservation_expires_at").isAfter(clock.instant())) {
			throw PaymentException.conflict("Course Order or Reservation is not payable");
		}
		Instant now = clock.instant();
		return fulfillCourse(row, "CASH", actorId, null, now, courseUnits(row, now));
	}

	CourseEnrollmentResponse fulfillCourseReconciliation(Map<String, Object> row, UUID actor, String transactionId, Instant now) {
		return fulfillCourse(row, "SEPAY", actor, transactionId, now, courseUnits(row, now));
	}

	private CourseEnrollmentResponse fulfillCourse(Map<String, Object> row, String method, UUID actorId, String providerTransactionId, Instant now, List<CourseUnit> units) {
		return coursePaymentFulfillment.fulfillCourse(row, method, actorId, providerTransactionId, now, units, courseTransfers);
	}

	List<CourseUnit> courseUnits(Map<String, Object> row, Instant now) {
		List<CourseUnit> units = paymentFulfillmentRepository.courseUnitsFindCourseSessions(uuid(row, "course_run_id"));
		int totalUnits = ((Number) row.get("total_units")).intValue();
		if (units.size() != totalUnits || units.stream().map(CourseUnit::unitNumber).distinct().count() != totalUnits) {
			throw PaymentException.conflict("Course schedule does not match the published unit count");
		}
		long remainingUnits = units.stream().filter(unit -> !unit.startAt().isBefore(now)).count();
		if (remainingUnits * 10 <= (long) totalUnits * 7) {
			throw PaymentException.conflict("Course enrollment is no longer available");
		}
		return units;
	}

	static boolean courseRunPayable(String status) {
		return "OPEN_FOR_REGISTRATION".equals(status) || "CONFIRMED".equals(status) || "IN_PROGRESS".equals(status);
	}

	CourseEnrollmentResponse courseEnrollment(UUID orderId, UUID memberId) {
		try {
			Map<String, Object> row = paymentFulfillmentRepository.courseEnrollmentFindCourseEnrollments(orderId, memberId);
			return new CourseEnrollmentResponse(uuid(row, "id"), uuid(row, "course_order_id"), uuid(row, "member_account_id"), uuid(row, "course_run_id"), new BigDecimal(row.get("amount_paid_vnd_snapshot").toString()).toBigIntegerExact(), ((Number) row.get("total_units_snapshot")).intValue(), instant(row, "enrolled_at"));
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.conflict("Paid Course Order has incomplete enrollment data");
		}
	}

	private Map<String, Object> courseOrderForUpdate(UUID memberId, UUID orderId) {
		return paymentFulfillmentRepository.courseOrderForUpdate(memberId, orderId);
	}

	@Transactional
	PaymentResultResponse fulfillCash(UUID paymentId, UUID orderId, UUID actorId, String evidence, String reason) {
		lifecycle.reconcile(memberIdForOrder(orderId), clock.instant());
		Map<String, Object> row;
		try {
			row = paymentFulfillmentRepository.fulfillCashFindMembershipOrders(paymentId, orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
		if (!"PENDING_PAYMENT".equals(string(row, "order_status")))
			throw PaymentException.conflict("Order is not payable");
		ensureMembershipCanFulfill(row);
		Instant now = clock.instant();
		try {
			paymentFulfillmentRepository.fulfillCashInsertPayments(paymentId, orderId, row.get("payment_amount"), actorId, Timestamp.from(now));
			paymentFulfillmentRepository.fulfillCashUpdateMembershipOrders(Timestamp.from(now), orderId);
			return createArtifacts(row, paymentId, actorId, evidence, reason, now);
		} catch (DataIntegrityViolationException exception) {
			throw PaymentException.conflict("Payment or Order was already fulfilled");
		}
	}

	private UUID memberIdForPayment(UUID paymentId) {
		return paymentFulfillmentRepository.memberIdForPayment(paymentId);
	}

	private UUID memberIdForOrder(UUID orderId) {
		return paymentFulfillmentRepository.memberIdForOrder(orderId);
	}

	private void ensureMembershipCanFulfill(Map<String, Object> row) {
		Integer active = paymentFulfillmentRepository.ensureMembershipCanFulfillFindMemberships(uuid(row, "member_account_id"));
		if (fulfillmentMode(row) == FulfillmentMode.IMMEDIATE && active != null && active > 0) {
			throw PaymentException.conflict("Member already has an active membership");
		}
	}

	private PaymentResultResponse fulfillLocked(Map<String, Object> row, UUID paymentId, UUID actorId, String provider, String providerTransactionId, String evidence, String reason, Instant now) {
		return membershipPaymentFulfillment.fulfillLocked(row, paymentId, actorId, provider, providerTransactionId, evidence, reason, now);
	}

	private SepayWebhookResponse finishEvent(Map<String, Object> row, SepayWebhookRequest request, String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome, String detail, boolean processed) {
		UUID paymentId = row == null ? null : uuid(row, "payment_id");
		if (!insertFinalEvent(paymentId, request, normalizedReference, payloadSha256, outcome, detail)) {
			return replayEvent(paymentId, request, payloadSha256, event(request.id().toString()));
		}
		return webhookResponse(row, processed, outcome);
	}

	private boolean insertFinalEvent(UUID paymentId, SepayWebhookRequest request, String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome, String detail) {
		return paymentProviderEventRepository.insertFinalEvent(paymentId, request, normalizedReference, payloadSha256, outcome, detail);
	}

	private boolean insertFinalEvent(UUID paymentId, UUID coursePaymentId, SepayWebhookRequest request, String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome, String detail) {
		return paymentProviderEventRepository.insertFinalEvent(paymentId, coursePaymentId, request, normalizedReference, payloadSha256, outcome, detail);
	}

	private SepayWebhookResponse finishCourseEvent(Map<String, Object> row, SepayWebhookRequest request, String normalizedReference, String payloadSha256, SepayWebhookOutcome outcome, String detail) {
		UUID coursePaymentId = uuid(row, "payment_id");
		if (!insertFinalEvent(null, coursePaymentId, request, normalizedReference, payloadSha256, outcome, detail)) {
			return replayEvent(null, coursePaymentId, request, payloadSha256, event(request.id().toString()));
		}
		return webhookResponse(row, false, outcome);
	}

	private Map<String, Object> event(String transactionId) {
		return paymentProviderEventRepository.event(transactionId);
	}

	void lockProviderTransaction(String provider, String transactionId) {
		paymentProviderEventRepository.lockProviderTransaction(provider, transactionId);
	}

	boolean providerTransactionClaimed(String provider, String transactionId, UUID paymentId, UUID coursePaymentId) {
		return paymentProviderEventRepository.providerTransactionClaimed(provider, transactionId, paymentId, coursePaymentId);
	}

	private SepayWebhookResponse replayEvent(UUID requestedPaymentId, SepayWebhookRequest request, String payloadSha256, Map<String, Object> event) {
		return replayEvent(requestedPaymentId, null, request, payloadSha256, event);
	}

	private SepayWebhookResponse replayEvent(UUID requestedPaymentId, UUID requestedCoursePaymentId, SepayWebhookRequest request, String payloadSha256, Map<String, Object> event) {
		UUID existingPaymentId = uuid(event, "payment_id");
		UUID existingCoursePaymentId = uuid(event, "course_payment_id");
		boolean samePayload = payloadSha256.equals(string(event, "payload_sha256"));
		boolean samePayment = Objects.equals(existingPaymentId, requestedPaymentId) && Objects.equals(existingCoursePaymentId, requestedCoursePaymentId);
		if (!samePayload) {
			log.warn("Rejected duplicate SePay transaction with changed canonical payload; transactionId={}, eventId={}", request.id(), event.get("id"));
		}
		Map<String, Object> row = existingPaymentId != null ? payment(existingPaymentId) : existingCoursePaymentId == null ? null : coursePayment(existingCoursePaymentId);
		String storedOutcome = string(event, "outcome");
		boolean processed = samePayload && samePayment && row != null && "PAID".equals(string(row, "payment_status")) && request.id().toString().equals(string(row, "provider_transaction_id")) && (SepayWebhookOutcome.FULFILLED.name().equals(storedOutcome) || SepayWebhookOutcome.ALREADY_PROCESSED.name().equals(storedOutcome));
		return webhookResponse(row, processed, SepayWebhookOutcome.ALREADY_PROCESSED);
	}

	Map<String, Object> coursePaymentForUpdate(UUID coursePaymentId) {
		return paymentFulfillmentRepository.coursePaymentForUpdate(coursePaymentId);
	}

	Map<String, Object> coursePayment(UUID coursePaymentId) {
		return paymentFulfillmentRepository.coursePayment(coursePaymentId);
	}

	private SepayWebhookResponse webhookResponse(Map<String, Object> row, boolean processed, SepayWebhookOutcome outcome) {
		return new SepayWebhookResponse(true, processed, outcome, row == null ? null : uuid(row, "payment_id"), row == null ? null : uuid(row, "order_id"), row == null ? null : string(row, "payment_status"));
	}

	private static boolean blank(Map<String, Object> row, String key) {
		return string(row, key) == null || string(row, key).isBlank();
	}

	private static String normalizeReference(String value) {
		return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
	}

	private PaymentResultResponse createArtifacts(Map<String, Object> row, UUID paymentId, UUID actorId, String evidence, String reason, Instant now) {
		return membershipPaymentFulfillment.createArtifacts(row, paymentId, actorId, evidence, reason, now);
	}

	public Map<String, Object> paymentForUpdate(UUID paymentId) {
		return paymentFulfillmentRepository.paymentForUpdate(paymentId);
	}

	public Map<String, Object> payment(UUID paymentId) {
		return paymentFulfillmentRepository.payment(paymentId);
	}

	public PaymentResultResponse result(Map<String, Object> row) {
		UUID orderId = uuid(row, "order_id");
		Map<String, Object> fulfillment;
		try {
			fulfillment = paymentFulfillmentRepository.resultFindMemberships(orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.conflict("Paid Payment has incomplete fulfillment data");
		}
		return new PaymentResultResponse(uuid(row, "payment_id"), orderId, "PAID", string(row, "payment_method"), uuid(fulfillment, "membership_id"), uuid(fulfillment, "receipt_id"), instant(row, "paid_at"), fulfillmentMode(fulfillment), string(row, "order_number"), string(row, "member_code"), string(row, "member_name"), string(row, "provider_reference"));
	}

	static UUID uuid(Map<String, Object> row, String key) {
		return CoursePaymentFulfillment.uuid(row, key);
	}

	static String string(Map<String, Object> row, String key) {
		return CoursePaymentFulfillment.string(row, key);
	}

	static Number number(Map<String, Object> row, String key) {
		return MembershipPaymentFulfillment.number(row, key);
	}

	static FulfillmentMode fulfillmentMode(Map<String, Object> row) {
		return MembershipPaymentFulfillment.fulfillmentMode(row);
	}

	static BigInteger amount(Map<String, Object> row, String key) {
		return CoursePaymentFulfillment.amount(row, key);
	}

	static Instant instant(Map<String, Object> row, String key) {
		return CoursePaymentFulfillment.instant(row, key);
	}

	record CourseUnit(UUID id, int unitNumber, Instant startAt) {
	}

	private final PaymentProviderEventRepository paymentProviderEventRepository;

	private final PaymentFulfillmentRepository paymentFulfillmentRepository;

	private final MembershipPaymentFulfillment membershipPaymentFulfillment;

	private final CoursePaymentFulfillment coursePaymentFulfillment;
}
