package com.scms.backend.membership;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.centervisit.CenterVisitRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.BookingRepository;
import com.scms.backend.scheduling.WaitlistPromotionCoordinator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
class MembershipFreezeExecutionService {
	private static final Logger log = LoggerFactory.getLogger(MembershipFreezeExecutionService.class);
	private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
	private final MembershipFreezeRequestRepository requests;
	private final MembershipRepository memberships;
	private final AccountRepository accounts;
	private final BookingRepository bookings;
	private final CenterVisitRepository visits;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final JdbcTemplate db;
	private final Clock clock;
	private final TransactionTemplate transactions;
	private final WaitlistPromotionCoordinator waitlistPromotions;

	MembershipFreezeExecutionService(MembershipFreezeRequestRepository requests, MembershipRepository memberships,
			AccountRepository accounts, BookingRepository bookings, CenterVisitRepository visits, AuditEventRepository audits,
			NotificationWriter notifications, JdbcTemplate db, Clock clock, PlatformTransactionManager transactionManager,
			WaitlistPromotionCoordinator waitlistPromotions) {
		this.requests = requests; this.memberships = memberships; this.accounts = accounts; this.bookings = bookings;
		this.visits = visits; this.audits = audits; this.notifications = notifications; this.db = db; this.clock = clock;
		this.transactions = new TransactionTemplate(transactionManager);
		this.waitlistPromotions = waitlistPromotions;
	}

	void reconcileDue(Instant now) {
		LocalDate today = LocalDate.ofInstant(now, BUSINESS_ZONE);
		// shortcut: completes or activates at most 100 requests per run; add cursor paging if freeze volume requires same-run catch-up.
		for (UUID id : requests.findDueUnpaidIds(today)) {
			try { transactions.executeWithoutResult(ignored -> obsoleteUnpaid(id)); }
			catch (RuntimeException exception) { log.error("Membership freeze expiry failed for request {}", id, exception); }
		}
		for (UUID id : requests.findDueActiveIds(today)) {
			try { transactions.executeWithoutResult(ignored -> complete(id, today)); }
			catch (RuntimeException exception) { log.error("Membership freeze completion failed for request {}", id, exception); }
		}
		for (UUID id : requests.findDueScheduledIds(today)) {
			try { transactions.executeWithoutResult(ignored -> activate(id, today, now)); }
			catch (RuntimeException exception) { log.error("Membership freeze activation failed for request {}", id, exception); }
		}
	}

	private void obsoleteUnpaid(UUID requestId) {
		MembershipFreezeRequest request = requests.findByIdForUpdate(requestId).orElseThrow(MembershipFreezeException::notFound);
		if (request.getStatus() != MembershipFreezeRequestStatus.APPROVED_PENDING_PAYMENT) return;
		request.obsolete();
		requests.saveAndFlush(request);
	}

	void activate(UUID requestId, LocalDate today, Instant now) {
		MembershipFreezeRequest request = requests.findById(requestId).orElse(null);
		if (request == null || request.getStatus() != MembershipFreezeRequestStatus.SCHEDULED || request.getStartDate().isAfter(today)) return;
		accounts.findByIdForUpdate(request.getMemberAccountId()).orElseThrow(MembershipFreezeException::notFound);
		Membership membership = memberships.findByIdForUpdate(request.getMembershipId()).orElseThrow(MembershipFreezeException::notFound);
		request = requests.findByIdForUpdate(requestId).orElseThrow(MembershipFreezeException::notFound);
		if (request.getStatus() != MembershipFreezeRequestStatus.SCHEDULED) return;
		if (membership.getStatus() != MembershipStatus.ACTIVE) {
			db.update("update membership_freeze_periods set status='OBSOLETE', updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", requestId);
			request.obsolete(); requests.saveAndFlush(request);
			return;
		}
		if (!request.getEndDate().isAfter(today)) {
			int creditedDays = Math.toIntExact(ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()));
			int completed = db.update("update membership_freeze_periods set status='COMPLETED', actual_start_date=?, actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'",
				Date.valueOf(request.getStartDate()), Date.valueOf(request.getEndDate()), creditedDays, requestId);
			if (completed != 1) return;
			membership.extendTo(membership.getEndsAt().plus(creditedDays, ChronoUnit.DAYS));
			request.activate();
			request.complete();
			requests.saveAndFlush(request);
			audits.save(new AuditEvent(UUID.randomUUID(), "MEMBERSHIP_FREEZE_COMPLETED", "MEMBERSHIP_FREEZE_REQUEST", requestId,
				Map.of("membershipId", request.getMembershipId().toString(), "creditedDays", creditedDays)));
			notifications.write("MEMBERSHIP_FREEZE_COMPLETED:" + requestId, request.getMemberAccountId(),
				"MEMBERSHIP_FREEZE_COMPLETED", "MEMBERSHIP_FREEZE_REQUEST", requestId, Map.of("creditedDays", creditedDays));
			return;
		}
		int activated = db.update("update membership_freeze_periods set status='ACTIVE', actual_start_date=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", Date.valueOf(request.getStartDate()), requestId);
		if (activated != 1) return;
		request.activate(); requests.saveAndFlush(request);
		Instant start = request.getStartDate().atStartOfDay(BUSINESS_ZONE).toInstant();
		Instant end = request.getEndDate().atStartOfDay(BUSINESS_ZONE).toInstant();
		var cancelledSessionIds = bookings.cancelFutureBookingsForMembershipFreeze(
			request.getMemberAccountId(), null, start, end, now);
		visits.checkoutOpenForMembershipFreeze(request.getMemberAccountId(), null, now);
		audits.save(new AuditEvent(UUID.randomUUID(), "MEMBERSHIP_FREEZE_ACTIVATED", "MEMBERSHIP_FREEZE_REQUEST", requestId,
			Map.of("membershipId", request.getMembershipId().toString(), "startDate", request.getStartDate().toString(), "endDate", request.getEndDate().toString())));
		notifications.write("MEMBERSHIP_FREEZE_ACTIVE:" + requestId, request.getMemberAccountId(), "MEMBERSHIP_FREEZE_ACTIVE",
			"MEMBERSHIP_FREEZE_REQUEST", requestId, Map.of("startDate", request.getStartDate().toString(), "endDate", request.getEndDate().toString()));
		promoteAfterCommit(cancelledSessionIds);
	}

	private void promoteAfterCommit(java.util.List<UUID> sessionIds) {
		if (sessionIds.isEmpty()) return;
		var uniqueSessionIds = sessionIds.stream().distinct().toList();
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override public void afterCommit() { uniqueSessionIds.forEach(waitlistPromotions::promote); }
		});
	}

	void complete(UUID requestId, LocalDate today) {
		MembershipFreezeRequest request = requests.findById(requestId).orElse(null);
		if (request == null || request.getStatus() != MembershipFreezeRequestStatus.ACTIVE || request.getEndDate().isAfter(today)) return;
		accounts.findByIdForUpdate(request.getMemberAccountId()).orElseThrow(MembershipFreezeException::notFound);
		Membership membership = memberships.findByIdForUpdate(request.getMembershipId()).orElseThrow(MembershipFreezeException::notFound);
		request = requests.findByIdForUpdate(requestId).orElseThrow(MembershipFreezeException::notFound);
		if (request.getStatus() != MembershipFreezeRequestStatus.ACTIVE || request.getEndDate().isAfter(today)) return;
		if (membership.getStatus() != MembershipStatus.ACTIVE) throw new IllegalStateException("Frozen Membership is not ACTIVE");
		LocalDate actualStart = db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update",
			LocalDate.class, requestId);
		int creditedDays = Math.toIntExact(ChronoUnit.DAYS.between(actualStart, request.getEndDate()));
		if (creditedDays < 1) throw new IllegalStateException("Active freeze period must credit at least one day");
		int completed = db.update("update membership_freeze_periods set status='COMPLETED', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'",
			Date.valueOf(request.getEndDate()), creditedDays, requestId);
		if (completed != 1) return;
		membership.extendTo(membership.getEndsAt().plus(creditedDays, ChronoUnit.DAYS));
		request.complete();
		requests.saveAndFlush(request);
		audits.save(new AuditEvent(UUID.randomUUID(), "MEMBERSHIP_FREEZE_COMPLETED", "MEMBERSHIP_FREEZE_REQUEST", requestId,
			Map.of("membershipId", request.getMembershipId().toString(), "creditedDays", creditedDays)));
		notifications.write("MEMBERSHIP_FREEZE_COMPLETED:" + requestId, request.getMemberAccountId(),
			"MEMBERSHIP_FREEZE_COMPLETED", "MEMBERSHIP_FREEZE_REQUEST", requestId,
			Map.of("creditedDays", creditedDays));
	}

	void endActiveForTermination(UUID memberAccountId, Instant now) {
		LocalDate today = LocalDate.ofInstant(now, BUSINESS_ZONE);
		for (UUID requestId : requests.findActiveIdsByMemberAccountId(memberAccountId)) {
			endEarlyForTermination(requestId, today);
		}
	}

	private void endEarlyForTermination(UUID requestId, LocalDate today) {
		MembershipFreezeRequest request = requests.findByIdForUpdate(requestId).orElseThrow(MembershipFreezeException::notFound);
		if (request.getStatus() != MembershipFreezeRequestStatus.ACTIVE) return;
		Membership membership = memberships.findByIdForUpdate(request.getMembershipId()).orElseThrow(MembershipFreezeException::notFound);
		LocalDate actualStart = db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update",
			LocalDate.class, requestId);
		int creditedDays = Math.toIntExact(ChronoUnit.DAYS.between(actualStart, today));
		int ended = db.update("update membership_freeze_periods set status='ENDED_EARLY', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'",
			Date.valueOf(today), creditedDays, requestId);
		if (ended != 1) return;
		if (creditedDays > 0) membership.extendTo(membership.getEndsAt().plus(creditedDays, ChronoUnit.DAYS));
		request.complete();
		requests.saveAndFlush(request);
		audits.save(new AuditEvent(UUID.randomUUID(), "MEMBERSHIP_FREEZE_ENDED_EARLY", "MEMBERSHIP_FREEZE_REQUEST", requestId,
			Map.of("membershipId", request.getMembershipId().toString(), "creditedDays", creditedDays)));
		notifications.write("MEMBERSHIP_FREEZE_ENDED_EARLY:" + requestId, request.getMemberAccountId(),
			"MEMBERSHIP_FREEZE_ENDED_EARLY", "MEMBERSHIP_FREEZE_REQUEST", requestId, Map.of("creditedDays", creditedDays));
	}
}
