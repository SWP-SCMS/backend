package com.scms.backend.membership;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
	private final JdbcTemplate db;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	MembershipRefundService(AccountRepository accounts, MemberProfileRepository profiles,
			RefundRequestRepository refunds, JdbcTemplate db, AuditEventRepository audits,
			NotificationWriter notifications, Clock clock) {
		this.accounts = accounts; this.profiles = profiles; this.refunds = refunds; this.db = db;
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
			return RefundRequestResponse.from(replay);
		}
		Map<String, Object> source;
		try {
			source = db.queryForMap("""
				select p.id payment_id, p.order_id, p.method, p.status payment_status,
					m.id membership_id, m.member_account_id, m.status membership_status, r.id receipt_id
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
		if (!"PAID".equals(source.get("payment_status")) || !"TERMINATED".equals(source.get("membership_status"))) {
			throw RefundException.notEligible();
		}
		RefundCalculation quote = calculation(input.membershipId(), LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE));
		if (quote.remainingDays() * 100L < quote.durationDays() * 30L) throw RefundException.threshold();
		BigInteger suggested = suggested(quote);
		if (input.requestedAmount().compareTo(suggested) > 0) throw RefundException.amountLimit();
		RefundRequest refund = new RefundRequest(UUID.randomUUID(), input.paymentId(), (UUID) source.get("order_id"),
			input.membershipId(), (UUID) source.get("receipt_id"), memberId, actor, RefundType.TERMINATED_MEMBERSHIP,
			input.requestedAmount(), suggested, quote.remainingDays(), quote.date(), input.reason().trim(), input.idempotencyKey().trim());
		try {
			refunds.saveAndFlush(refund);
		} catch (DataIntegrityViolationException exception) {
			throw RefundException.exists();
		}
		write(actor, refund, "REQUESTED", Map.of("requestedAmount", input.requestedAmount().toString()));
		return RefundRequestResponse.from(refund);
	}

	@Transactional
	RefundRequestResponse firstApproval(UUID actor, UUID refundId, RefundFirstApprovalRequest input) {
		ensureRole(actor, AccountRole.MANAGER);
		if (input == null || input.expectedVersion() == null || input.approvedAmount() == null) throw RefundException.validation();
		RefundRequest request = lock(refundId);
		checkVersion(request, input.expectedVersion());
		if (request.getStatus() != RefundRequestStatus.REQUESTED) throw RefundException.transition();
		RefundCalculation calculation = calculation(request.getMembershipId(), LocalDate.ofInstant(clock.instant(), BUSINESS_ZONE));
		if (calculation.remainingDays() * 100L < calculation.durationDays() * 30L) throw RefundException.threshold();
		BigInteger suggested = suggested(calculation);
		if (input.approvedAmount().signum() <= 0 || input.approvedAmount().compareTo(request.getRequestedAmount()) > 0
				|| input.approvedAmount().compareTo(suggested) > 0) throw RefundException.amountLimit();
		Instant now = clock.instant();
		request.firstApprove(actor, input.approvedAmount(), suggested, calculation.remainingDays(), calculation.date(), now);
		refunds.saveAndFlush(request);
		write(actor, request, "FIRST_APPROVED", Map.of("approvedAmount", input.approvedAmount().toString(), "suggestedAmount", suggested.toString()));
		return RefundRequestResponse.from(request);
	}

	@Transactional
	RefundRequestResponse cashDisbursement(UUID actor, UUID refundId, RefundCashDisbursementRequest input) {
		ensureRole(actor, AccountRole.RECEPTIONIST);
		if (input == null || input.expectedVersion() == null || input.evidence() == null || input.evidence().isBlank()
				|| input.evidence().trim().length() > 2000) throw RefundException.validation();
		RefundRequest request = lock(refundId); checkVersion(request, input.expectedVersion());
		if (request.getStatus() != RefundRequestStatus.FIRST_APPROVED) throw RefundException.transition();
		Instant now = clock.instant(); request.cashDisburse(actor, input.evidence(), now); refunds.saveAndFlush(request);
		write(actor, request, "CASH_DISBURSED", Map.of("approvedAmount", request.getApprovedAmount().toString()));
		return RefundRequestResponse.from(request);
	}

	@Transactional
	RefundRequestResponse finalApproval(UUID actor, UUID refundId, RefundActionRequest input) {
		ensureRole(actor, AccountRole.MANAGER);
		if (input == null || input.expectedVersion() == null) throw RefundException.validation();
		RefundRequest request = lock(refundId); checkVersion(request, input.expectedVersion());
		if (request.getStatus() != RefundRequestStatus.CASH_DISBURSED) throw RefundException.transition();
		Instant now = clock.instant(); request.complete(actor, now); refunds.saveAndFlush(request);
		db.update("insert into credit_notes(id,refund_request_id,receipt_id,payment_id,member_account_id,amount,issued_by_account_id,issued_at) values(?,?,?,?,?,?,?,?)",
			UUID.randomUUID(), request.getId(), request.getReceiptId(), request.getPaymentId(), request.getMemberAccountId(),
			request.getApprovedAmount(), actor, Timestamp.from(now));
		write(actor, request, "COMPLETED", Map.of("amount", request.getApprovedAmount().toString()));
		return RefundRequestResponse.from(request);
	}

	@Transactional
	RefundRequestResponse reject(UUID actor, UUID refundId, RefundActionRequest input) {
		ensureRole(actor, AccountRole.MANAGER);
		if (input == null || input.expectedVersion() == null || input.reason() == null || input.reason().isBlank()
				|| input.reason().trim().length() > 1000) throw RefundException.validation();
		RefundRequest request = lock(refundId); checkVersion(request, input.expectedVersion());
		try { request.reject(input.reason(), clock.instant()); }
		catch (IllegalStateException exception) { throw RefundException.transition(); }
		refunds.saveAndFlush(request);
		write(actor, request, "REJECTED", Map.of("reason", input.reason().trim()));
		return RefundRequestResponse.from(request);
	}

	private RefundCalculation calculation(UUID membershipId, LocalDate today) {
		Map<String, Object> row = db.queryForMap("""
			select m.starts_at, m.duration_days_snapshot, m.price_amount_snapshot,
				coalesce(sum(period.credited_days), 0) credited_days
			from memberships m left join membership_freeze_periods period
				on period.membership_id=m.id and period.status in ('COMPLETED','ENDED_EARLY')
			where m.id=? group by m.id
			""", membershipId);
		LocalDate start = ((Timestamp) row.get("starts_at")).toInstant().atZone(BUSINESS_ZONE).toLocalDate();
		int duration = ((Number) row.get("duration_days_snapshot")).intValue();
		int credited = ((Number) row.get("credited_days")).intValue();
		LocalDate originalEnd = start.plusDays((long) duration + credited);
		int remaining = (int) Math.max(0, ChronoUnit.DAYS.between(today, originalEnd));
		return new RefundCalculation(today, remaining, duration, new BigInteger(row.get("price_amount_snapshot").toString()));
	}
	private BigInteger suggested(RefundCalculation calculation) {
		return calculation.price().multiply(BigInteger.valueOf(calculation.remainingDays())).multiply(BigInteger.valueOf(50))
			.divide(BigInteger.valueOf((long) calculation.durationDays() * 100));
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
		return RefundRequestPageResponse.from(page);
	}
	private void write(UUID actor, RefundRequest request, String action, Map<String, Object> metadata) {
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "MEMBERSHIP_REFUND_" + action, "REFUND_REQUEST",
			request.getId(), null, Map.of(), metadata));
		notifications.write("MEMBERSHIP_REFUND_" + action + ":" + request.getId(), request.getMemberAccountId(),
			"MEMBERSHIP_REFUND_" + action, "REFUND_REQUEST", request.getId(), metadata);
	}
	private record RefundCalculation(LocalDate date, int remainingDays, int durationDays, BigInteger price) { }
}
