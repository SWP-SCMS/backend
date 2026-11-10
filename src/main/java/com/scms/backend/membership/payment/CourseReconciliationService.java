package com.scms.backend.membership.payment;

import static com.scms.backend.membership.payment.PaymentFulfillmentService.amount;
import static com.scms.backend.membership.payment.PaymentFulfillmentService.courseRunPayable;
import static com.scms.backend.membership.payment.PaymentFulfillmentService.instant;
import static com.scms.backend.membership.payment.PaymentFulfillmentService.string;
import static com.scms.backend.membership.payment.PaymentFulfillmentService.uuid;
import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.course.order.CourseReservationExpiryService;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.ReconciliationQueueStatus;
import com.scms.backend.membership.payment.CourseReconciliationModels.Detail;
import com.scms.backend.membership.payment.CourseReconciliationModels.Page;
import com.scms.backend.membership.payment.CourseReconciliationModels.Result;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
class CourseReconciliationService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final PaymentFulfillmentService fulfillment;

	private final CourseReservationExpiryService expiry;

	private final AuditEventRepository audits;

	private final ObjectMapper json;

	private final Clock clock;

	CourseReconciliationService(JdbcTemplate db, AccountRepository accounts, PaymentFulfillmentService fulfillment, CourseReservationExpiryService expiry, AuditEventRepository audits, ObjectMapper json, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.fulfillment = fulfillment;
		this.expiry = expiry;
		this.audits = audits;
		this.json = json;
		this.clock = clock;
		this.courseReconciliationRepository = new CourseReconciliationRepository(this.db, this.clock);
	}

	@Transactional(readOnly = true)
	Page queue(UUID actor, ReconciliationQueueStatus status, String query, int page, int size) {
		staff(actor);
		if (page < 0 || size < 1 || size > 100 || (query != null && query.length() > 200))
			throw PaymentException.validation("page >= 0, size 1..100 and query <= 200 characters required");
		return queueRows(status, query, page, size);
	}

	@Transactional(readOnly = true)
	Detail detail(UUID actor, UUID paymentId) {
		staff(actor);
		Map<String, Object> row = fulfillment.coursePayment(paymentId);
		String blocked = paidBlockedReason(row, clock.instant());
		boolean failed = canFail(row, clock.instant());
		return new Detail(view(row), string(row, "bank_code_snapshot"), string(row, "bank_account_number_snapshot"), string(row, "bank_account_name_snapshot"), string(row, "bank_transfer_content"), blocked == null, failed, blocked);
	}

	@Transactional(readOnly = true)
	Result result(UUID actor, UUID paymentId) {
		Account account = active(actor);
		if (account.getRole() != AccountRole.MANAGER && account.getRole() != AccountRole.RECEPTIONIST && account.getRole() != AccountRole.MEMBER)
			throw PaymentException.forbidden();
		Map<String, Object> row = fulfillment.coursePayment(paymentId);
		if (account.getRole() == AccountRole.MEMBER && !actor.equals(uuid(row, "member_account_id")))
			throw PaymentException.forbidden();
		return view(row);
	}

	@Transactional
	Result reconcile(UUID actor, UUID paymentId, String key, PaymentActionRequest request) {
		staff(actor);
		validate(key, request);
		Map<String, Object> row = fulfillment.coursePaymentForUpdate(paymentId);
		String payload = json.writeValueAsString(request);
		var saved = courseReconciliationRepository.reconcileFindCoursePaymentReconciliations(actor, paymentId, key);
		if (!saved.isEmpty()) {
			if (!json.readTree(payload).equals(json.readTree((String) saved.getFirst().get("request_json"))))
				throw PaymentException.conflict("Idempotency-Key was used with another payload");
			return json.readValue((String) saved.getFirst().get("response_json"), Result.class);
		}
		boolean changed = false;
		Instant now = clock.instant();
		if (request.status() == PaymentActionRequest.ReconciliationStatus.PAID) {
			if (!amount(row, "payment_amount").equals(request.receivedAmount()) || !string(row, "bank_transfer_content").equals(request.transferContent().trim()))
				throw PaymentException.validation("receivedAmount or transferContent does not match the payment snapshot");
			String tx = request.providerTransactionId().trim();
			fulfillment.lockProviderTransaction("SEPAY", tx);
			now = clock.instant();
			if (fulfillment.providerTransactionClaimed("SEPAY", tx, null, paymentId))
				throw PaymentException.conflict("Provider transaction was already used");
			if ("PAID".equals(string(row, "payment_status"))) {
				if (!"SEPAY".equals(string(row, "payment_method")) || !tx.equals(string(row, "provider_transaction_id")))
					throw PaymentException.conflict("Payment was already confirmed differently");
			} else {
				String blocked = paidBlockedReason(row, now);
				if (blocked != null)
					throw PaymentException.conflict(blocked);
				fulfillment.fulfillCourseReconciliation(row, actor, tx, now);
				changed = true;
			}
		} else {
			if (!canFail(row, now))
				throw PaymentException.conflict("Only an expired pending Course payment can be failed");
			if (!expiry.expireReservation(uuid(row, "reservation_id"), now))
				throw PaymentException.conflict("Reservation changed");
			changed = true;
		}
		Result response = view(fulfillment.coursePayment(paymentId));
		courseReconciliationRepository.reconcileInsertCoursePaymentReconciliations(UUID.randomUUID(), actor, paymentId, key, payload, json.writeValueAsString(response), changed, Timestamp.from(now));
		if (changed)
			audits.save(new AuditEvent(UUID.randomUUID(), actor, "COURSE_PAYMENT_RECONCILED", "COURSE_PAYMENT", paymentId, request.reason().trim(), Map.of("status", string(row, "payment_status")), Map.of("status", response.status(), "evidence", request.evidence().trim(), "orderId", response.orderId().toString())));
		return response;
	}

	private Result view(Map<String, Object> row) {
		return new Result(uuid(row, "payment_id"), uuid(row, "order_id"), uuid(row, "reservation_id"), uuid(row, "course_run_id"), uuid(row, "member_account_id"), string(row, "order_reference"), amount(row, "payment_amount"), string(row, "payment_currency").trim(), string(row, "payment_status"), string(row, "order_status"), string(row, "reservation_status"), string(row, "payment_method"), instant(row, "expires_at"), "PAID".equals(string(row, "payment_status")) ? fulfillment.courseEnrollment(uuid(row, "order_id"), uuid(row, "member_account_id")) : null);
	}

	private String paidBlockedReason(Map<String, Object> row, Instant now) {
		if (!"PENDING".equals(string(row, "payment_status")) || !"PENDING_PAYMENT".equals(string(row, "order_status")) || !"HELD".equals(string(row, "reservation_status")))
			return "PAYMENT_NOT_PENDING";
		if (!"SEPAY".equals(string(row, "payment_method")))
			return "PAYMENT_METHOD_NOT_TRANSFER";
		if (!"VND".equals(string(row, "payment_currency").trim()))
			return "PAYMENT_CURRENCY_MISMATCH";
		if (!now.isBefore(instant(row, "expires_at")) || !now.isBefore(instant(row, "reservation_expires_at")))
			return "PAYMENT_WINDOW_EXPIRED";
		if (!courseRunPayable(string(row, "run_status")))
			return "COURSE_RUN_NOT_PAYABLE";
		for (String field : List.of("bank_code_snapshot", "bank_account_number_snapshot", "bank_account_name_snapshot")) if (string(row, field) == null || string(row, field).isBlank())
			return "DESTINATION_SNAPSHOT_MISSING";
		try {
			fulfillment.courseUnits(row, now);
		} catch (PaymentException ex) {
			return "COURSE_ENROLLMENT_NOT_AVAILABLE";
		}
		return null;
	}

	private boolean canFail(Map<String, Object> row, Instant now) {
		return "PENDING".equals(string(row, "payment_status")) && "SEPAY".equals(string(row, "payment_method")) && "PENDING_PAYMENT".equals(string(row, "order_status")) && "HELD".equals(string(row, "reservation_status")) && !now.isBefore(instant(row, "expires_at")) && !now.isBefore(instant(row, "reservation_expires_at"));
	}

	static void validate(String key, PaymentActionRequest request) {
		if (key == null || key.isBlank() || key.length() > 128)
			throw PaymentException.validation("Idempotency-Key must contain 1..128 characters");
		if (request == null || request.status() == null || blank(request.evidence()) || blank(request.reason()) || request.evidence().length() > 4000 || request.reason().length() > 2000)
			throw PaymentException.validation("status, evidence (1..4000), reason (1..2000) required");
		if (request.status() == PaymentActionRequest.ReconciliationStatus.PAID && (request.receivedAmount() == null || request.receivedAmount().signum() <= 0 || blank(request.transferContent()) || request.transferContent().length() > 200 || blank(request.providerTransactionId()) || request.providerTransactionId().length() > 100))
			throw PaymentException.validation("Positive receivedAmount, transferContent and providerTransactionId required for PAID");
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private Account active(UUID actor) {
		return accounts.findById(actor).filter(a -> a.getStatus() == AccountStatus.ACTIVE).orElseThrow(PaymentException::forbidden);
	}

	private void staff(UUID actor) {
		Account a = active(actor);
		if (a.getRole() != AccountRole.MANAGER && a.getRole() != AccountRole.RECEPTIONIST)
			throw PaymentException.forbidden();
	}

	private Page queueRows(ReconciliationQueueStatus status, String query, int page, int size) {
		return courseReconciliationRepository.queueRows(status, query, page, size);
	}

	private final CourseReconciliationRepository courseReconciliationRepository;
}
