package com.scms.backend.membership.payment;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.membership.FulfillmentMode;
import com.scms.backend.membership.MembershipBenefitStatus;
import com.scms.backend.membership.MembershipHistoryResponse;
import com.scms.backend.membership.MembershipLifecycleService;
import com.scms.backend.membership.MembershipOfferUnavailableException;
import com.scms.backend.membership.MembershipRenewalAlreadyExistsException;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.ReconciliationQueueStatus;
import com.scms.backend.membership.StaffMembershipResponse;
import com.scms.backend.membership.order.MembershipOrderCancellationRequest;
import com.scms.backend.membership.order.MembershipOrderCancellationResponse;
import com.scms.backend.membership.order.MembershipOrderConstraintDetector;
import com.scms.backend.membership.plan.MembershipBenefitResponse;
import com.scms.backend.membership.reporting.RevenueReportResponse;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

	private final JdbcTemplate db;

	private final AccountRepository accounts;

	private final AuditEventRepository audits;

	private final PaymentFulfillmentService fulfillment;

	private final MemberProfileRepository profiles;

	private final MembershipLifecycleService lifecycle;

	private final Clock clock;

	public PaymentService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits, PaymentFulfillmentService fulfillment, MemberProfileRepository profiles, MembershipLifecycleService lifecycle, Clock clock) {
		this.db = db;
		this.accounts = accounts;
		this.audits = audits;
		this.fulfillment = fulfillment;
		this.profiles = profiles;
		this.lifecycle = lifecycle;
		this.clock = clock;
		this.paymentRepository = new PaymentRepository(this.db, this.clock);
	}

	@Transactional
	PaymentResultResponse cash(UUID actor, String memberCode, CashPaymentRequest request) {
		return cash(actor, memberCode, request, null);
	}

	@Transactional
	PaymentResultResponse cash(UUID actor, String memberCode, CashPaymentRequest request, String idempotencyKey) {
		ensure(actor, AccountRole.RECEPTIONIST);
		if (request == null || request.offerId() == null)
			throw PaymentException.validation("offerId is required");
		String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
		Map<String, Object> member;
		Map<String, Object> offer;
		try {
			member = paymentRepository.cashFindAccounts(memberCode);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Active Member or Membership Offer was not found");
		}
		UUID memberId = (UUID) member.get("account_id");
		accounts.findByIdForUpdate(memberId).filter(account -> account.getRole() == AccountRole.MEMBER && account.getStatus() == AccountStatus.ACTIVE).orElseThrow(() -> PaymentException.notFound("Active Member or Membership Offer was not found"));
		if (normalizedIdempotencyKey != null) {
			try {
				Map<String, Object> replay = paymentRepository.cashFindMembershipOrders(actor, normalizedIdempotencyKey);
				if (!memberId.equals(replay.get("member_account_id"))) {
					throw PaymentException.conflict("Idempotency-Key was already used for another Member");
				}
				return fulfillment.result(fulfillment.payment((UUID) replay.get("payment_id")));
			} catch (EmptyResultDataAccessException ignored) {
				// This is the first request for the key.
			}
		}
		lifecycle.reconcile(memberId, clock.instant());
		try {
			offer = paymentRepository.cashFindMembershipOffers(request.offerId());
		} catch (EmptyResultDataAccessException exception) {
			throw new MembershipOfferUnavailableException();
		}
		boolean active = paymentRepository.countActiveMemberships(memberId) > 0;
		if (paymentRepository.countReservedMemberships(memberId) > 0)
			throw new MembershipRenewalAlreadyExistsException();
		if (paymentRepository.countPendingOrders(memberId) > 0) {
			if (active)
				throw new MembershipRenewalAlreadyExistsException();
			throw PaymentException.conflict("Member already has a pending membership order");
		}
		if (active && normalizedIdempotencyKey == null) {
			throw PaymentException.conflict("Idempotency-Key is required for cash membership renewal");
		}
		FulfillmentMode mode = active ? FulfillmentMode.RESERVED : FulfillmentMode.IMMEDIATE;
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();
		String orderNumber = "ORD-" + orderId.toString().replace("-", "").toUpperCase();
		try {
			paymentRepository.cashInsertMembershipOrders(orderId, orderNumber, memberId, actor, request.offerId(), offer.get("name"), offer.get("plan_code"), offer.get("price_amount"), offer.get("currency_code"), offer.get("duration_days"), mode.name(), normalizedIdempotencyKey);
		} catch (DataIntegrityViolationException exception) {
			if (MembershipOrderConstraintDetector.isPendingMemberConstraint(exception)) {
				if (active)
					throw new MembershipRenewalAlreadyExistsException();
				throw PaymentException.conflict("Member already has a pending membership order");
			}
			throw exception;
		}
		return fulfillment.fulfillCash(paymentId, orderId, actor, "Cash received by receptionist", "Cash payment confirmed");
	}

	private String normalizeIdempotencyKey(String key) {
		if (key == null)
			return null;
		String normalized = key.trim();
		if (normalized.isEmpty() || normalized.length() > 100) {
			throw PaymentException.validation("Idempotency-Key must be nonblank and at most 100 characters");
		}
		return normalized;
	}

	@Transactional
	public PaymentResultResponse reconcile(UUID actor, UUID paymentId, PaymentActionRequest request) {
		ensureAny(actor);
		if (request == null || request.status() == null || blank(request.reason()) || blank(request.evidence()))
			throw PaymentException.validation("status, evidence and reason are required");
		Map<String, Object> payment = fulfillment.payment(paymentId);
		if (request.status() == PaymentActionRequest.ReconciliationStatus.PAID) {
			if (request.receivedAmount() == null || blank(request.transferContent()) || blank(request.providerTransactionId()))
				throw PaymentException.validation("receivedAmount, transferContent and providerTransactionId are required for PAID");
			if (!PaymentFulfillmentService.amount(payment, "payment_amount").equals(request.receivedAmount()))
				throw PaymentException.validation("receivedAmount does not match Payment amount");
			String expectedContent = PaymentFulfillmentService.string(payment, "bank_transfer_content");
			if (expectedContent == null || !expectedContent.equals(request.transferContent().trim()))
				throw PaymentException.validation("transferContent does not match Payment");
			return fulfillment.fulfill(paymentId, actor, request.providerTransactionId().trim(), request.evidence().trim(), request.reason().trim());
		}
		payment = fulfillment.paymentForUpdate(paymentId);
		String status = PaymentFulfillmentService.string(payment, "payment_status");
		if (!"PENDING".equals(status))
			throw PaymentException.conflict("Payment is terminal");
		Object expires = payment.get("expires_at");
		Instant expiry = expires instanceof Timestamp value ? value.toInstant() : (Instant) expires;
		if (expiry == null || clock.instant().isBefore(expiry))
			throw PaymentException.conflict("Payment window has not expired");
		paymentRepository.reconcileUpdatePayments(request.reason().trim(), request.evidence().trim(), actor, paymentId);
		paymentRepository.reconcileUpdateMembershipOrders(payment.get("order_id"));
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "PAYMENT_RECONCILED", "PAYMENT", paymentId, request.reason().trim(), Map.of("status", "PENDING"), Map.of("status", "FAILED", "evidence", request.evidence().trim())));
		fulfillment.notifyFailedReconciliation(paymentId, payment);
		return new PaymentResultResponse(paymentId, (UUID) payment.get("order_id"), "FAILED", PaymentFulfillmentService.string(payment, "payment_method"), null, null, null, PaymentFulfillmentService.fulfillmentMode(payment), PaymentFulfillmentService.string(payment, "order_number"), PaymentFulfillmentService.string(payment, "member_code"), PaymentFulfillmentService.string(payment, "member_name"), PaymentFulfillmentService.string(payment, "provider_reference"));
	}

	@Transactional
	MembershipOrderCancellationResponse cancelOrder(UUID actor, UUID orderId, MembershipOrderCancellationRequest request) {
		ensureAny(actor);
		if (request == null || blank(request.reason())) {
			throw PaymentException.validation("reason is required");
		}
		String reason = request.reason().trim();
		Map<String, Object> order;
		try {
			order = paymentRepository.cancelOrderFindMembershipOrders(orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
		if (!"PENDING_PAYMENT".equals(PaymentFulfillmentService.string(order, "status"))) {
			throw PaymentException.conflict("Only a pending Membership Order can be cancelled");
		}
		Instant now = clock.instant();
		int cancelledPayments = paymentRepository.cancelOrderUpdatePayments("Order cancelled: " + reason, "Cancelled by authorized staff", actor, Timestamp.from(now), orderId);
		int cancelledOrder = paymentRepository.cancelOrderUpdateMembershipOrders(Timestamp.from(now), orderId);
		if (cancelledOrder != 1) {
			throw PaymentException.conflict("Membership Order is no longer pending");
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "MEMBERSHIP_ORDER_CANCELLED", "MEMBERSHIP_ORDER", orderId, reason, Map.of("status", "PENDING_PAYMENT"), Map.of("status", "EXPIRED", "cancelledPayments", cancelledPayments)));
		return new MembershipOrderCancellationResponse(orderId, "EXPIRED", cancelledPayments, now, reason);
	}

	@Transactional(readOnly = true)
	ReconciliationQueuePageResponse reconciliationQueue(UUID actor, ReconciliationQueueStatus status, String query, int page, int size) {
		ensureAny(actor);
		ReconciliationQueueStatus effectiveStatus = status == null ? ReconciliationQueueStatus.ALL : status;
		String normalizedQuery = query == null ? null : query.trim();
		if (normalizedQuery != null && normalizedQuery.isEmpty())
			normalizedQuery = null;
		return reconciliationRows(page, size, effectiveStatus, normalizedQuery);
	}

	@Transactional(readOnly = true)
	List<ReceiptResponse> receipts(UUID actor) {
		ensure(actor, AccountRole.MEMBER);
		return paymentRepository.receiptsFindReceipts(actor);
	}

	@Transactional(readOnly = true)
	List<ReceiptResponse> receiptsForReceptionist(UUID actor, String memberCode) {
		Account account = active(actor);
		if (account.getRole() != AccountRole.RECEPTIONIST && account.getRole() != AccountRole.MANAGER) {
			throw PaymentException.forbidden();
		}
		if (blank(memberCode))
			throw PaymentException.validation("memberId is required");
		UUID memberId;
		try {
			memberId = paymentRepository.receiptsForReceptionistFindMemberProfiles(memberCode.trim());
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Member was not found");
		}
		return receptionReceiptRows(actor, account, memberId);
	}

	@Transactional(readOnly = true)
	ReceiptResponse receipt(UUID actor, UUID receiptId) {
		Account account = active(actor);
		try {
			Map<String, Object> row = paymentRepository.receiptFindReceipts(receiptId);
			if (account.getRole() == AccountRole.MEMBER && !actor.equals(row.get("member_account_id")))
				throw PaymentException.forbidden();
			if (account.getRole() == AccountRole.RECEPTIONIST && !actor.equals(row.get("processed_by_account_id")))
				throw PaymentException.forbidden();
			if (account.getRole() == AccountRole.COACH)
				throw PaymentException.forbidden();
			return receipt(row);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Receipt was not found");
		}
	}

	@Transactional(readOnly = true)
	PaymentResultResponse result(UUID actor, UUID paymentId) {
		Account account = active(actor);
		Map<String, Object> row = fulfillment.payment(paymentId);
		if (account.getRole() == AccountRole.MEMBER && !actor.equals(row.get("member_account_id")))
			throw PaymentException.forbidden();
		if (account.getRole() == AccountRole.COACH)
			throw PaymentException.forbidden();
		if (!"PAID".equals(row.get("payment_status")))
			return new PaymentResultResponse(paymentId, (UUID) row.get("order_id"), row.get("payment_status").toString(), row.get("payment_method").toString(), null, null, null, PaymentFulfillmentService.fulfillmentMode(row), PaymentFulfillmentService.string(row, "order_number"), PaymentFulfillmentService.string(row, "member_code"), PaymentFulfillmentService.string(row, "member_name"), PaymentFulfillmentService.string(row, "provider_reference"));
		return fulfillment.result(row);
	}

	@Transactional
	List<MembershipHistoryResponse> memberHistory(UUID actor) {
		ensure(actor, AccountRole.MEMBER);
		lifecycle.reconcile(actor, clock.instant());
		List<MembershipHistoryResponse> history = paymentRepository.memberHistoryFindMemberships(actor);
		Map<String, MembershipBenefits> benefits = benefitsByPlanCode(history.stream().map(MembershipHistoryResponse::planCode).toList());
		return history.stream().map(item -> {
			MembershipBenefits planBenefits = benefits.getOrDefault(item.planCode(), MembershipBenefits.EMPTY);
			return new MembershipHistoryResponse(item.id(), item.orderId(), item.planCode(), item.offerName(), item.priceAmount(), item.currencyCode(), item.durationDays(), item.status(), item.startsAt(), item.endsAt(), planBenefits.codes(), planBenefits.benefits());
		}).toList();
	}

	@Transactional
	public List<StaffMembershipResponse> receptionMembershipHistory(UUID actor, String memberCode) {
		ensure(actor, AccountRole.RECEPTIONIST);
		UUID memberId = profiles.findVisibleMemberByCode(memberCode.trim(), AccountRole.MEMBER, List.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED)).map(MemberProfile::getAccountId).orElseThrow(() -> PaymentException.notFound("Member was not found"));
		return staffMembershipHistory(memberId);
	}

	@Transactional
	public List<StaffMembershipResponse> managerMembershipHistory(UUID actor, UUID memberAccountId) {
		ensure(actor, AccountRole.MANAGER);
		UUID memberId = accounts.findById(memberAccountId).filter(account -> account.getRole() == AccountRole.MEMBER).filter(account -> account.getStatus() == AccountStatus.ACTIVE || account.getStatus() == AccountStatus.SUSPENDED).map(Account::getId).orElseThrow(() -> PaymentException.notFound("Member was not found"));
		return staffMembershipHistory(memberId);
	}

	private List<StaffMembershipResponse> staffMembershipHistory(UUID memberId) {
		lifecycle.reconcile(memberId, clock.instant());
		List<StaffMembershipResponse> history = paymentRepository.staffMembershipHistoryFindMemberships(memberId);
		Map<String, MembershipBenefits> benefits = benefitsByPlanCode(history.stream().map(StaffMembershipResponse::planCode).toList());
		return history.stream().map(item -> {
			MembershipBenefits planBenefits = benefits.getOrDefault(item.planCode(), MembershipBenefits.EMPTY);
			return new StaffMembershipResponse(item.membershipId(), item.planCode(), item.offerName(), item.status(), item.startsAt(), item.endsAt(), planBenefits.codes(), planBenefits.benefits());
		}).toList();
	}

	private Map<String, MembershipBenefits> benefitsByPlanCode(List<String> planCodes) {
		return paymentRepository.benefitsByPlanCode(planCodes);
	}

	record PlanBenefitRow(String planCode, String code, String displayName, String description, MembershipBenefitStatus status, int displayOrder, String groupCode, String groupDisplayName, Integer groupDisplayOrder) {
	}

	record MembershipBenefits(List<String> codes, List<MembershipBenefitResponse> benefits) {

		private static final MembershipBenefits EMPTY = new MembershipBenefits(List.of(), List.of());
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	@Transactional(readOnly = true)
	public RevenueReportResponse report(UUID actor, Instant from, Instant to, boolean includeTestData) {
		ensure(actor, AccountRole.MANAGER);
		if (from != null && to != null && !from.isBefore(to))
			throw PaymentException.validation("from must be earlier than to");
		return reportRows(from, to, includeTestData);
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}

	private Account active(UUID id) {
		return accounts.findById(id).filter(a -> a.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
	}

	private void ensure(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private void ensureAny(UUID id) {
		if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE) && !accounts.existsByIdAndRoleAndStatus(id, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE))
			throw new InvalidAuthenticatedAccountException();
	}

	private ReceiptResponse receipt(java.sql.ResultSet rs) throws java.sql.SQLException {
		return paymentRepository.receipt(rs);
	}

	private ReceiptResponse receipt(Map<String, Object> r) {
		return paymentRepository.receipt(r);
	}

	private ReconciliationQueuePageResponse reconciliationRows(int page, int size, ReconciliationQueueStatus effectiveStatus, String normalizedQuery) {
		return paymentRepository.reconciliationRows(page, size, effectiveStatus, normalizedQuery);
	}

	private List<ReceiptResponse> receptionReceiptRows(UUID actor, Account account, UUID memberId) {
		return paymentRepository.receptionReceiptRows(actor, account, memberId);
	}

	private RevenueReportResponse reportRows(Instant from, Instant to, boolean includeTestData) {
		return paymentRepository.reportRows(from, to, includeTestData);
	}

	private final PaymentRepository paymentRepository;
}
