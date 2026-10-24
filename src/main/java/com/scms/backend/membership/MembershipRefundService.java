package com.scms.backend.membership;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.identity.receptionmember.ReceptionMemberNotFoundException;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MembershipRefundService {
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final Set<AccountStatus> VISIBLE = Set.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
	private final AccountRepository accounts;
	private final MemberProfileRepository profiles;
	private final RefundRequestRepository refunds;
	private final MembershipRepository memberships;
	private final MembershipLifecycleService lifecycle;
	private final JdbcTemplate db;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	MembershipRefundService(AccountRepository accounts, MemberProfileRepository profiles,
			RefundRequestRepository refunds, MembershipRepository memberships, MembershipLifecycleService lifecycle,
			JdbcTemplate db, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.accounts = accounts; this.profiles = profiles; this.refunds = refunds; this.memberships = memberships;
		this.lifecycle = lifecycle; this.db = db;
		this.audits = audits; this.notifications = notifications; this.clock = clock;
	}

	@Transactional(readOnly = true)
	RefundRequestPageResponse listForReceptionist(UUID actor, int page, int size) {
		ensureRole(actor, AccountRole.RECEPTIONIST); validatePage(page, size);
		return page(refunds.findAllByOrderByRequestedAtDescIdDesc(PageRequest.of(page, size)));
	}

	@Transactional(readOnly = true)
	RefundRequestPageResponse listForManager(UUID actor, int page, int size) {
		ensureRole(actor, AccountRole.MANAGER); validatePage(page, size);
		return page(refunds.findAllByOrderByRequestedAtDescIdDesc(PageRequest.of(page, size)));
	}

	@Transactional(readOnly = true)
	List<RefundEligibleSourceResponse> refundEligibleSources(UUID actor, String memberCode) {
		ensureRole(actor, AccountRole.RECEPTIONIST);
		UUID memberId = member(memberCode);
		LocalDate calculationDate = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
		return db.queryForList("""
			select p.id payment_id, m.id membership_id, r.id receipt_id, r.receipt_number,
				o.order_number, m.offer_name_snapshot offer_name, profile.member_code,
				account.full_name member_full_name, p.provider_reference payment_reference,
				m.status membership_status, p.amount paid_amount, p.currency_code currency,
				m.starts_at, m.duration_days_snapshot, m.price_amount_snapshot,
				coalesce(freeze_totals.credited_days, 0) credited_days
			from payments p
			join membership_orders o on o.id=p.order_id
			join memberships m on m.order_id=o.id and m.member_account_id=o.member_account_id
			join receipts r on r.payment_id=p.id and r.order_id=o.id and r.member_account_id=o.member_account_id
			join accounts account on account.id=o.member_account_id
			join member_profiles profile on profile.account_id=account.id
			left join (
				select membership_id, sum(credited_days) credited_days
				from membership_freeze_periods
				where status in ('COMPLETED','ENDED_EARLY')
				group by membership_id
			) freeze_totals on freeze_totals.membership_id=m.id
			where o.member_account_id=? and p.method='CASH' and p.status='PAID'
				and m.status in ('TERMINATED','RESERVED')
				and not exists (
					select 1 from refund_requests refund
					where refund.payment_id=p.id and refund.status not in ('REJECTED','CANCELLED')
				)
			order by p.paid_at desc, p.id desc
			""", memberId).stream()
			.map(row -> eligibleSource(row, calculationDate))
			.flatMap(Optional::stream)
			.toList();
	}

	@Transactional
	RefundRequestResponse create(UUID actor, String memberCode, RefundRequestCreateRequest input) {
		if (input == null || input.paymentId() == null || input.membershipId() == null
				|| input.requestedAmount() == null || input.requestedAmount().signum() <= 0
				|| input.reason() == null || input.reason().isBlank() || input.reason().trim().length() > 1000
				|| input.idempotencyKey() == null || input.idempotencyKey().isBlank() || input.idempotencyKey().trim().length() > 100) {
			throw RefundException.validation();
		}
		accounts.findByIdForUpdate(actor).filter(account -> account.getRole() == AccountRole.RECEPTIONIST
			&& account.getStatus() == AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);
		UUID memberId = member(memberCode);
		accounts.findByIdForUpdate(memberId)
			.filter(account -> account.getRole() == AccountRole.MEMBER && VISIBLE.contains(account.getStatus()))
			.orElseThrow(RefundException::notEligible);
		var existing = refunds.findByRequesterAccountIdAndIdempotencyKey(actor, input.idempotencyKey().trim());
		if (existing.isPresent()) {
			RefundRequest replay = existing.get();
			if (!replay.getMemberAccountId().equals(memberId) || !replay.getPaymentId().equals(input.paymentId())
					|| !replay.getMembershipId().equals(input.membershipId()) || !replay.getRequestedAmount().equals(input.requestedAmount())
					|| !replay.getReason().equals(input.reason().trim())) {
				throw RefundException.changed();
			}
			return response(replay);
		}
		Map<String, Object> source;
		try {
			source = db.queryForMap("""
				select p.id payment_id, p.order_id, p.method, p.status payment_status,
					m.id membership_id, m.member_account_id, m.status membership_status, m.price_amount_snapshot, r.id receipt_id
				from payments p join membership_orders o on o.id=p.order_id
				join memberships m on m.order_id=o.id join receipts r on r.payment_id=p.id
				where p.id=? and m.id=? and m.member_account_id=?
				order by r.issued_at desc, r.id limit 1
				for update of p, m
				""", input.paymentId(), input.membershipId(), memberId);
		} catch (org.springframework.dao.EmptyResultDataAccessException exception) {
			throw RefundException.notEligible();
		}
		if (!"CASH".equals(source.get("method"))) throw RefundException.unsupportedPayment();
		if (!"PAID".equals(source.get("payment_status"))) {
			throw RefundException.notEligible();
		}
		LocalDate calculationDate = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
		Map<String, Object> quoteSource = "TERMINATED".equals(source.get("membership_status"))
			? refundSource(input.membershipId()) : source;
		RefundQuote quote = eligibleQuote(quoteSource, calculationDate);
		if (input.requestedAmount().compareTo(quote.suggestedAmount()) > 0) throw RefundException.amountLimit();
		RefundRequest refund = new RefundRequest(UUID.randomUUID(), input.paymentId(), (UUID) source.get("order_id"),
			input.membershipId(), (UUID) source.get("receipt_id"), memberId, actor, quote.type(),
			input.requestedAmount(), quote.suggestedAmount(), quote.remainingDays(), calculationDate,
			input.reason().trim(), input.idempotencyKey().trim());
		try {
			refunds.saveAndFlush(refund);
		} catch (DataIntegrityViolationException exception) {
			throw RefundException.exists();
		}
		return write(actor, refund, "REQUESTED", Map.of("requestedAmount", input.requestedAmount().toString()));
	}

	@Transactional
	RefundRequestResponse firstApproval(UUID actor, UUID refundId, RefundFirstApprovalRequest input) {
		ensureRole(actor, AccountRole.MANAGER);
		if (input == null || input.expectedVersion() == null || input.approvedAmount() == null) throw RefundException.validation();
		RefundRequest visible = refunds.findById(refundId).orElseThrow(RefundException::notFound);
		Membership reserved = visible.getType() == RefundType.RESERVED_CANCELLATION
			? lockReservedMembership(visible) : null;
		RefundRequest request = lock(refundId);
		checkVersion(request, input.expectedVersion());
		if (request.getStatus() != RefundRequestStatus.REQUESTED) throw RefundException.transition();
		LocalDate calculationDate = LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE);
		RefundQuote quote;
		if (request.getType() == RefundType.RESERVED_CANCELLATION) {
			if (reserved == null || reserved.getStatus() != MembershipStatus.RESERVED) throw RefundException.notEligible();
			quote = eligibleQuote("RESERVED", reserved.getPriceAmountSnapshot(), null, 0, 0, calculationDate);
		} else {
			quote = eligibleQuote(refundSource(request.getMembershipId()), calculationDate);
		}
		if (input.approvedAmount().signum() <= 0 || input.approvedAmount().compareTo(request.getRequestedAmount()) > 0
				|| input.approvedAmount().compareTo(quote.suggestedAmount()) > 0) throw RefundException.amountLimit();
		Instant now = clock.instant();
		request.firstApprove(actor, input.approvedAmount(), quote.suggestedAmount(), quote.remainingDays(), calculationDate, now);
		refunds.saveAndFlush(request);
		return write(actor, request, "FIRST_APPROVED", Map.of("approvedAmount", input.approvedAmount().toString(),
			"suggestedAmount", quote.suggestedAmount().toString()));
	}

	@Transactional
	RefundRequestResponse cashDisbursement(UUID actor, UUID refundId, RefundCashDisbursementRequest input) {
		ensureRole(actor, AccountRole.RECEPTIONIST);
		if (input == null || input.expectedVersion() == null || input.evidence() == null || input.evidence().isBlank()
				|| input.evidence().trim().length() > 2000) throw RefundException.validation();
		RefundRequest request = lock(refundId); checkVersion(request, input.expectedVersion());
		if (request.getStatus() != RefundRequestStatus.FIRST_APPROVED) throw RefundException.transition();
		Instant now = clock.instant(); request.cashDisburse(actor, input.evidence(), now); refunds.saveAndFlush(request);
		return write(actor, request, "CASH_DISBURSED", Map.of("approvedAmount", request.getApprovedAmount().toString()));
	}

	@Transactional
	RefundRequestResponse finalApproval(UUID actor, UUID refundId, RefundActionRequest input) {
		ensureRole(actor, AccountRole.MANAGER);
		if (input == null || input.expectedVersion() == null) throw RefundException.validation();
		RefundRequest visible = refunds.findById(refundId).orElseThrow(RefundException::notFound);
		Membership reserved = visible.getType() == RefundType.RESERVED_CANCELLATION
			? lockReservedMembership(visible) : null;
		RefundRequest request = lock(refundId); checkVersion(request, input.expectedVersion());
		if (request.getStatus() != RefundRequestStatus.CASH_DISBURSED) throw RefundException.transition();
		if (request.getType() == RefundType.RESERVED_CANCELLATION) {
			if (reserved == null || reserved.getStatus() != MembershipStatus.RESERVED) throw RefundException.notEligible();
			reserved.cancelReserved();
			memberships.saveAndFlush(reserved);
		}
		Instant now = clock.instant(); request.complete(actor, now); refunds.saveAndFlush(request);
		db.update("insert into credit_notes(id,refund_request_id,receipt_id,payment_id,member_account_id,amount,issued_by_account_id,issued_at) values(?,?,?,?,?,?,?,?)",
			UUID.randomUUID(), request.getId(), request.getReceiptId(), request.getPaymentId(), request.getMemberAccountId(),
			request.getApprovedAmount(), actor, Timestamp.from(now));
		return write(actor, request, "COMPLETED", Map.of("amount", request.getApprovedAmount().toString()));
	}

	@Transactional
	RefundRequestResponse reject(UUID actor, UUID refundId, RefundActionRequest input) {
		ensureRole(actor, AccountRole.MANAGER);
		if (input == null || input.expectedVersion() == null || input.reason() == null || input.reason().isBlank()
				|| input.reason().trim().length() > 1000) throw RefundException.validation();
		RefundRequest visible = refunds.findById(refundId).orElseThrow(RefundException::notFound);
		boolean reservedCancellation = visible.getType() == RefundType.RESERVED_CANCELLATION;
		if (reservedCancellation) lockReservedMembership(visible);
		RefundRequest request = lock(refundId); checkVersion(request, input.expectedVersion());
		try { request.reject(input.reason(), clock.instant()); }
		catch (IllegalStateException exception) { throw RefundException.transition(); }
		refunds.saveAndFlush(request);
		RefundRequestResponse response = write(actor, request, "REJECTED", Map.of("reason", input.reason().trim()));
		if (reservedCancellation) lifecycle.activateReservedAfterRefundRejected(request.getMemberAccountId(), clock.instant());
		return response;
	}

	private Map<String, Object> refundSource(UUID membershipId) {
		return db.queryForMap("""
			select m.starts_at, m.status membership_status, m.duration_days_snapshot, m.price_amount_snapshot,
				coalesce(sum(period.credited_days), 0) credited_days
			from memberships m left join membership_freeze_periods period
				on period.membership_id=m.id and period.status in ('COMPLETED','ENDED_EARLY')
			where m.id=? group by m.id
			""", membershipId);
	}
	private Optional<RefundEligibleSourceResponse> eligibleSource(Map<String, Object> row, LocalDate date) {
		RefundQuote quote = quote(row, date);
		if (!quote.eligible() || quote.suggestedAmount().signum() <= 0) return Optional.empty();
		return Optional.of(new RefundEligibleSourceResponse((UUID) row.get("payment_id"),
			(UUID) row.get("membership_id"), (UUID) row.get("receipt_id"), row.get("receipt_number").toString(),
			row.get("order_number").toString(), row.get("offer_name").toString(), row.get("member_code").toString(),
			row.get("member_full_name").toString(), (String) row.get("payment_reference"), quote.type(),
			big(row, "paid_amount"), quote.suggestedAmount(), row.get("currency").toString().trim(), date));
	}
	private RefundQuote eligibleQuote(Map<String, Object> row, LocalDate date) {
		RefundQuote quote = quote(row, date);
		if (!quote.eligible()) throw RefundException.threshold();
		return quote;
	}
	private RefundQuote quote(Map<String, Object> row, LocalDate date) {
		String status = row.get("membership_status").toString();
		if ("RESERVED".equals(status)) {
			return eligibleQuoteValues(status, big(row, "price_amount_snapshot"), null, 0, 0, date);
		}
		if (!"TERMINATED".equals(status)) throw RefundException.notEligible();
		return eligibleQuoteValues(status, big(row, "price_amount_snapshot"), (Timestamp) row.get("starts_at"),
			((Number) row.get("duration_days_snapshot")).intValue(),
			((Number) row.get("credited_days")).intValue(), date);
	}
	private RefundQuote eligibleQuote(String status, BigInteger price, Timestamp startsAt, int duration,
			int creditedDays, LocalDate date) {
		RefundQuote quote = eligibleQuoteValues(status, price, startsAt, duration, creditedDays, date);
		if (!quote.eligible()) throw RefundException.threshold();
		return quote;
	}
	private RefundQuote eligibleQuoteValues(String status, BigInteger price, Timestamp startsAt, int duration,
			int creditedDays, LocalDate date) {
		if ("RESERVED".equals(status)) {
			return new RefundQuote(RefundType.RESERVED_CANCELLATION,
				price.multiply(BigInteger.valueOf(80)).divide(BigInteger.valueOf(100)), null, true);
		}
		if (!"TERMINATED".equals(status)) throw RefundException.notEligible();
		LocalDate start = startsAt.toInstant().atZone(BUSINESS_ZONE).toLocalDate();
		int remaining = (int) Math.max(0, ChronoUnit.DAYS.between(date,
			start.plusDays((long) duration + creditedDays)));
		BigInteger suggested = price.multiply(BigInteger.valueOf(remaining)).multiply(BigInteger.valueOf(50))
			.divide(BigInteger.valueOf((long) duration * 100));
		return new RefundQuote(RefundType.TERMINATED_MEMBERSHIP, suggested, remaining,
			remaining * 100L >= duration * 30L);
	}
	private BigInteger big(Map<String, Object> row, String key) {
		return new BigInteger(row.get(key).toString());
	}
	private Membership lockReservedMembership(RefundRequest request) {
		accounts.findByIdForUpdate(request.getMemberAccountId())
			.filter(account -> account.getRole() == AccountRole.MEMBER && VISIBLE.contains(account.getStatus()))
			.orElseThrow(RefundException::notEligible);
		return memberships.findByIdForUpdate(request.getMembershipId()).orElseThrow(RefundException::notEligible);
	}

	private UUID member(String code) {
		if (code == null || code.isBlank()) throw new ReceptionMemberNotFoundException();
		MemberProfile profile = profiles.findVisibleMemberByCode(code.trim(), AccountRole.MEMBER, VISIBLE)
			.orElseThrow(ReceptionMemberNotFoundException::new);
		return profile.getAccountId();
	}
	private RefundRequest lock(UUID id) { return refunds.findByIdForUpdate(id).orElseThrow(RefundException::notFound); }
	private void checkVersion(RefundRequest request, Long expectedVersion) {
		if (!expectedVersion.equals(request.getVersion())) throw RefundException.changed();
	}
	private void ensureRole(UUID id, AccountRole role) {
		if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException();
	}
	private void validatePage(int page, int size) {
		if (page < 0 || size < 1 || size > 100) throw RefundException.validation();
	}
	private RefundRequestPageResponse page(org.springframework.data.domain.Page<RefundRequest> page) {
		Map<UUID, Map<String, Object>> display = display(page.getContent());
		return RefundRequestPageResponse.from(page.map(request ->
			RefundRequestResponse.from(request, display.getOrDefault(request.getId(), Map.of()))));
	}
	private RefundRequestResponse response(RefundRequest request) {
		return RefundRequestResponse.from(request, display(List.of(request)).getOrDefault(request.getId(), Map.of()));
	}
	private Map<UUID, Map<String, Object>> display(List<RefundRequest> requests) {
		if (requests.isEmpty()) return Map.of();
		String placeholders = String.join(",", java.util.Collections.nCopies(requests.size(), "?"));
		return db.queryForList("""
			select refund.id, profile.member_code, account.full_name member_full_name,
				o.order_number, p.provider_reference payment_reference, r.receipt_number
			from refund_requests refund
			left join accounts account on account.id=refund.member_account_id
			left join member_profiles profile on profile.account_id=refund.member_account_id
			left join membership_orders o on o.id=refund.order_id
			left join payments p on p.id=refund.payment_id
			left join receipts r on r.id=refund.receipt_id
			where refund.id in (
			""" + placeholders + ")", requests.stream().map(RefundRequest::getId).toArray())
			.stream().collect(java.util.stream.Collectors.toMap(row -> (UUID) row.get("id"), row -> row));
	}
	private RefundRequestResponse write(UUID actor, RefundRequest request, String action, Map<String, Object> metadata) {
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "MEMBERSHIP_REFUND_" + action, "REFUND_REQUEST",
			request.getId(), null, Map.of(), metadata));
		notifications.write("MEMBERSHIP_REFUND_" + action + ":" + request.getId(), request.getMemberAccountId(),
			"MEMBERSHIP_REFUND_" + action, "REFUND_REQUEST", request.getId(), metadata);
		RefundRequestResponse response = response(request);
		Map<String, Object> payload = new java.util.LinkedHashMap<>();
		payload.put("refundRequestId", request.getId().toString());
		payload.put("memberCode", response.memberCode());
		payload.put("memberFullName", response.memberFullName());
		payload.put("status", request.getStatus().name());
		if (request.getApprovedAmount() != null) payload.put("approvedAmount", request.getApprovedAmount().toString());
		List<UUID> recipients = switch (action) {
			case "REQUESTED", "CASH_DISBURSED" -> accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.MANAGER, AccountStatus.ACTIVE)
				.stream().map(account -> account.getId()).toList();
			case "FIRST_APPROVED" -> accounts.findAllByRoleAndStatusOrderByIdAsc(AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)
				.stream().map(account -> account.getId()).toList();
			case "COMPLETED", "REJECTED" -> accounts.existsByIdAndRoleAndStatus(request.getRequesterAccountId(),
				AccountRole.RECEPTIONIST, AccountStatus.ACTIVE) ? List.of(request.getRequesterAccountId()) : List.of();
			default -> List.of();
		};
		for (UUID recipient : recipients) {
			if (!recipient.equals(actor)) notifications.write("MEMBERSHIP_REFUND_" + action + ":" + request.getId() + ":" + recipient,
				recipient, "MEMBERSHIP_REFUND_" + action, "REFUND_REQUEST", request.getId(), payload);
		}
		return response;
	}
	private record RefundQuote(RefundType type, BigInteger suggestedAmount, Integer remainingDays, boolean eligible) { }
}
