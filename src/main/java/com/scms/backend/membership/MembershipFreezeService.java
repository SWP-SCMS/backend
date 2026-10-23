package com.scms.backend.membership;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MembershipFreezeService {
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private static final Set<MembershipFreezeRequestStatus> OPEN = Set.of(MembershipFreezeRequestStatus.PENDING,
		MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT, MembershipFreezeRequestStatus.SCHEDULED,
		MembershipFreezeRequestStatus.ACTIVE);
	private static final Set<AccountStatus> VISIBLE = Set.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
	private final AccountRepository accounts;
	private final MemberProfileRepository profiles;
	private final MembershipRepository memberships;
	private final MembershipFreezeRequestRepository requests;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final JdbcTemplate db;
	private final Clock clock;

	MembershipFreezeService(AccountRepository accounts, MemberProfileRepository profiles, MembershipRepository memberships,
			MembershipFreezeRequestRepository requests, AuditEventRepository audits, NotificationWriter notifications,
			JdbcTemplate db, Clock clock) {
		this.accounts = accounts; this.profiles = profiles; this.memberships = memberships; this.requests = requests;
		this.audits = audits; this.notifications = notifications; this.db = db; this.clock = clock;
	}

	@Transactional(readOnly = true)
	MembershipFreezeRequestPageResponse listForMember(UUID memberId, int page, int size) {
		ensure(memberId, AccountRole.MEMBER);
		return page(requests.findByMemberAccountIdOrderByCreatedAtDesc(memberId, pageable(page, size)));
	}

	@Transactional
	MembershipFreezeRequestResponse createForMember(UUID memberId, MembershipFreezeCreateRequest input) {
		ensure(memberId, AccountRole.MEMBER); return create(memberId, memberId, input);
	}

	@Transactional(readOnly = true)
	MembershipFreezeRequestPageResponse listForReceptionist(UUID receptionistId, String memberCode, int page, int size) {
		ensure(receptionistId, AccountRole.RECEPTIONIST);
		return page(requests.findByMemberAccountIdOrderByCreatedAtDesc(findMember(memberCode).getAccountId(), pageable(page, size)));
	}

	@Transactional
	MembershipFreezeRequestResponse createForReceptionist(UUID receptionistId, String memberCode, MembershipFreezeCreateRequest input) {
		ensure(receptionistId, AccountRole.RECEPTIONIST); return create(receptionistId, findMember(memberCode).getAccountId(), input);
	}

	@Transactional(readOnly = true)
	MembershipFreezeRequestPageResponse listForManager(UUID managerId, int page, int size) {
		ensure(managerId, AccountRole.MANAGER); return page(requests.findAllByOrderByCreatedAtDesc(pageable(page, size)));
	}

	@Transactional
	MembershipFreezeRequestResponse approve(UUID managerId, UUID requestId, MembershipFreezeDecisionRequest input) {
		ensure(managerId, AccountRole.MANAGER);
		LockedFreeze locked = lockForDecision(requestId);
		MembershipFreezeRequest request = locked.request(); checkVersion(request, input);
		if (request.getStatus() != MembershipFreezeRequestStatus.PENDING) throw MembershipFreezeException.invalidTransition();
		LocalDate today = businessDate();
		if (request.getStartDate().isBefore(today)) throw MembershipFreezeException.invalidPeriod();
		Membership membership = locked.membership();
		if (membership.getStatus() != MembershipStatus.ACTIVE) throw MembershipFreezeException.notActive();
		BigInteger fee = fee(membership, request.getStartDate(), request.getEndDate());
		request.approve(managerId, fee, clock.instant()); requests.saveAndFlush(request);
		audit(managerId, "APPROVED", request, Map.of("feeAmount", fee.toString()));
		notify(request, "APPROVED_PENDING_PAYMENT", Map.of("feeAmount", fee.toString()));
		return MembershipFreezeRequestResponse.from(request);
	}

	@Transactional
	MembershipFreezeRequestResponse reject(UUID managerId, UUID requestId, MembershipFreezeDecisionRequest input) {
		ensure(managerId, AccountRole.MANAGER);
		MembershipFreezeRequest request = lockForDecision(requestId).request(); checkVersion(request, input);
		if (request.getStatus() != MembershipFreezeRequestStatus.PENDING) throw MembershipFreezeException.invalidTransition();
		request.reject(managerId, clock.instant()); requests.saveAndFlush(request);
		audit(managerId, "REJECTED", request, Map.of()); notify(request, "REJECTED", Map.of());
		return MembershipFreezeRequestResponse.from(request);
	}

	@Transactional
	MembershipFreezeCashPaymentResponse collectCash(UUID receptionistId, UUID requestId, MembershipFreezeDecisionRequest input) {
		ensure(receptionistId, AccountRole.RECEPTIONIST);
		LockedFreeze locked = lockForDecision(requestId);
		MembershipFreezeRequest request = locked.request();
		if (request.getStatus() == MembershipFreezeRequestStatus.SCHEDULED) return paidResponse(request);
		checkVersion(request, input);
		if (request.getStatus() != MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT) throw MembershipFreezeException.paymentRequired();
		if (locked.membership().getStatus() != MembershipStatus.ACTIVE) throw MembershipFreezeException.notActive();
		if (request.getStartDate().isBefore(businessDate())) throw MembershipFreezeException.invalidPeriod();
		UUID paymentId = UUID.randomUUID(); Instant now = clock.instant();
		db.update("insert into membership_freeze_fee_payments(id,request_id,amount,currency_code,collected_by_account_id,paid_at) values(?,?,?,'VND',?,?)",
			paymentId, requestId, request.getFeeAmount(), receptionistId, Timestamp.from(now));
		db.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,status) values(?,?,?,?,?,'SCHEDULED')",
			UUID.randomUUID(), requestId, request.getMembershipId(), Date.valueOf(request.getStartDate()), Date.valueOf(request.getEndDate()));
		request.schedule(); requests.saveAndFlush(request);
		audit(receptionistId, "CASH_PAID", request, Map.of("feeAmount", request.getFeeAmount().toString(), "paymentId", paymentId.toString()));
		notify(request, "SCHEDULED", Map.of("feeAmount", request.getFeeAmount().toString()));
		return new MembershipFreezeCashPaymentResponse(requestId, paymentId, request.getFeeAmount(), "VND", request.getStatus(), now);
	}

	@Transactional
	MembershipFreezeRequestResponse endEarlyForMember(UUID memberId, UUID requestId, MembershipFreezeDecisionRequest input) {
		ensure(memberId, AccountRole.MEMBER);
		return endEarly(memberId, requestId, input, memberId);
	}

	@Transactional
	MembershipFreezeRequestResponse endEarlyForReceptionist(UUID receptionistId, UUID requestId, MembershipFreezeDecisionRequest input) {
		ensure(receptionistId, AccountRole.RECEPTIONIST);
		return endEarly(receptionistId, requestId, input, null);
	}

	static BigInteger fee(Membership membership, LocalDate startDate, LocalDate endDate) {
		long days = ChronoUnit.DAYS.between(startDate, endDate);
		return membership.getPriceAmountSnapshot().multiply(BigInteger.valueOf(days)).multiply(BigInteger.valueOf(20))
			.divide(BigInteger.valueOf((long) membership.getDurationDaysSnapshot() * 100));
	}

	private MembershipFreezeRequestResponse create(UUID requesterId, UUID memberId, MembershipFreezeCreateRequest input) {
		validate(input); accounts.findByIdForUpdate(memberId).filter(account -> account.getRole() == AccountRole.MEMBER
			&& account.getStatus() == AccountStatus.ACTIVE).orElseThrow(MembershipFreezeException::notActive);
		Membership membership = memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)
			.orElseThrow(MembershipFreezeException::notActive);
		if (requests.existsByMembershipIdAndStatusIn(membership.getId(), OPEN)) throw MembershipFreezeException.alreadyPending();
		MembershipFreezeRequest request = new MembershipFreezeRequest(UUID.randomUUID(), membership.getId(), memberId, requesterId,
			input.startDate(), input.endDate(), input.reason().trim());
		requests.saveAndFlush(request); audit(requesterId, "REQUESTED", request, Map.of()); notify(request, "PENDING", Map.of());
		return MembershipFreezeRequestResponse.from(request);
	}

	private MembershipFreezeCashPaymentResponse paidResponse(MembershipFreezeRequest request) {
		Map<String, Object> payment = db.queryForMap("select id,amount,paid_at from membership_freeze_fee_payments where request_id=?", request.getId());
		return new MembershipFreezeCashPaymentResponse(request.getId(), (UUID) payment.get("id"), ((BigDecimal) payment.get("amount")).toBigIntegerExact(),
			"VND", request.getStatus(), ((Timestamp) payment.get("paid_at")).toInstant());
	}
	private MembershipFreezeRequestResponse endEarly(UUID actorId, UUID requestId, MembershipFreezeDecisionRequest input,
			UUID ownerId) {
		LockedFreeze locked = lockForDecision(requestId, ownerId);
		MembershipFreezeRequest request = locked.request();
		checkVersion(request, input);
		if (request.getStatus() != MembershipFreezeRequestStatus.ACTIVE) throw MembershipFreezeException.invalidTransition();
		Membership membership = locked.membership();
		if (membership.getStatus() != MembershipStatus.ACTIVE) throw MembershipFreezeException.notActive();
		LocalDate today = businessDate();
		LocalDate actualStart;
		try {
			actualStart = db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update",
				LocalDate.class, requestId);
		}
		catch (org.springframework.dao.EmptyResultDataAccessException exception) {
			throw MembershipFreezeException.invalidTransition();
		}
		if (actualStart == null || actualStart.isAfter(today)) throw MembershipFreezeException.invalidTransition();
		int creditedDays = Math.toIntExact(ChronoUnit.DAYS.between(actualStart, today));
		int ended = db.update("update membership_freeze_periods set status='ENDED_EARLY', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'",
			Date.valueOf(today), creditedDays, requestId);
		if (ended != 1) throw MembershipFreezeException.changed();
		if (creditedDays > 0) membership.extendTo(membership.getEndsAt().plus(creditedDays, ChronoUnit.DAYS));
		request.complete(); requests.saveAndFlush(request);
		audit(actorId, "ENDED_EARLY", request, Map.of("creditedDays", creditedDays));
		notify(request, "ENDED_EARLY", Map.of("creditedDays", creditedDays));
		return MembershipFreezeRequestResponse.from(request);
	}
	private void validate(MembershipFreezeCreateRequest input) {
		if (input == null || input.startDate() == null || input.endDate() == null || !input.endDate().isAfter(input.startDate()) || input.startDate().isBefore(businessDate())) throw MembershipFreezeException.invalidPeriod();
		if (input.reason() == null || input.reason().isBlank() || input.reason().trim().length() > 1000) throw MembershipFreezeException.validation();
	}
	private MembershipFreezeRequest lock(UUID id) { return requests.findByIdForUpdate(id).orElseThrow(MembershipFreezeException::notFound); }
	private LockedFreeze lockForDecision(UUID requestId) { return lockForDecision(requestId, null); }
	private LockedFreeze lockForDecision(UUID requestId, UUID ownerId) {
		MembershipFreezeRequest visible = requests.findById(requestId).orElseThrow(MembershipFreezeException::notFound);
		if (ownerId != null && !visible.getMemberAccountId().equals(ownerId)) throw MembershipFreezeException.notFound();
		accounts.findByIdForUpdate(visible.getMemberAccountId()).orElseThrow(MembershipFreezeException::notFound);
		Membership membership = memberships.findByIdForUpdate(visible.getMembershipId()).orElseThrow(MembershipFreezeException::notFound);
		return new LockedFreeze(lock(requestId), membership);
	}
	private void checkVersion(MembershipFreezeRequest request, MembershipFreezeDecisionRequest input) {
		if (input == null || input.expectedVersion() == null || !input.expectedVersion().equals(request.getVersion())) throw MembershipFreezeException.changed();
	}
	private LocalDate businessDate() { return LocalDate.now(clock.withZone(BUSINESS_ZONE)); }
	private MemberProfile findMember(String code) { if (code == null || code.isBlank()) throw new ReceptionMemberNotFoundException(); return profiles.findVisibleMemberByCode(code.trim(), AccountRole.MEMBER, VISIBLE).orElseThrow(ReceptionMemberNotFoundException::new); }
	private void ensure(UUID id, AccountRole role) { if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException(); }
	private void audit(UUID actor, String action, MembershipFreezeRequest request, Map<String, Object> extra) {
		Map<String, Object> data = new java.util.LinkedHashMap<>(extra); data.put("membershipId", request.getMembershipId().toString()); data.put("status", request.getStatus().name());
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "MEMBERSHIP_FREEZE_" + action, "MEMBERSHIP_FREEZE_REQUEST", request.getId(), request.getReason(), Map.of(), data));
	}
	private void notify(MembershipFreezeRequest request, String event, Map<String, Object> payload) { notifications.write("MEMBERSHIP_FREEZE_" + event + ":" + request.getId(), request.getMemberAccountId(), "MEMBERSHIP_FREEZE_" + event, "MEMBERSHIP_FREEZE_REQUEST", request.getId(), payload); }
	private PageRequest pageable(int page, int size) { return PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending())); }
	private MembershipFreezeRequestPageResponse page(org.springframework.data.domain.Page<MembershipFreezeRequest> page) { return MembershipFreezeRequestPageResponse.from(page); }
	private record LockedFreeze(MembershipFreezeRequest request, Membership membership) { }
}
