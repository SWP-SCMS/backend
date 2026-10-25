package com.scms.backend.manager;

import java.time.Clock;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.WaitlistPromotionService;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberStatusService {

	private final AccountRepository accountRepository;
	private final AuditEventRepository auditRepository;
	private final NotificationWriter notificationWriter;
	private final JdbcTemplate jdbcTemplate;
	private final WaitlistPromotionService waitlistPromotions;
	private final Clock clock;

	MemberStatusService(AccountRepository accountRepository, AuditEventRepository auditRepository,
			NotificationWriter notificationWriter, JdbcTemplate jdbcTemplate,
			WaitlistPromotionService waitlistPromotions, Clock clock) {
		this.accountRepository = accountRepository;
		this.auditRepository = auditRepository;
		this.notificationWriter = notificationWriter;
		this.jdbcTemplate = jdbcTemplate;
		this.waitlistPromotions = waitlistPromotions;
		this.clock = clock;
	}

	@Transactional
	MemberStatusChangeResponse changeStatus(UUID managerId, UUID memberId, MemberStatusChangeRequest request) {
		ensureActiveManager(managerId);
		validate(request);
		Account member = accountRepository.findByIdForUpdate(memberId)
			.or(() -> accountRepository.findById(memberId))
			.filter(account -> account.getRole() == AccountRole.MEMBER)
			.orElseThrow(MemberAccountNotFoundException::new);
		AccountStatus before = member.getStatus();
		if (member.isEmailVerificationRequired() ||
				(before == AccountStatus.INACTIVE && member.getRegistrationExpiresAt() != null)) {
			throw new MemberStatusConflictException(
				"Pending or expired self-registration must use the email verification workflow");
		}
		if (before == request.status()) {
			throw new MemberStatusConflictException("Member is already " + before);
		}

		List<CancelledBooking> cancelled = request.status() == AccountStatus.SUSPENDED
			? cancelFutureBookings(memberId, managerId)
			: List.of();
		int cancelledBookings = cancelled.size();
		for (CancelledBooking booking : cancelled) {
			auditRepository.save(new AuditEvent(UUID.randomUUID(), managerId, "BOOKING_CANCELLED", "BOOKING",
				booking.id(), "Member suspended", Map.of("status", "BOOKED"),
				Map.of("status", "CANCELLED", "cancellationSource", "MEMBER_SUSPENDED")));
			notificationWriter.write("BOOKING_CANCELLED:" + booking.id(), memberId, "BOOKING_CANCELLED",
				"BOOKING", booking.id(), Map.of("sessionId", booking.sessionId().toString(),
					"cancellationSource", "MEMBER_SUSPENDED"));
		}
		waitlistPromotions.promoteAvailableBySessionIds(cancelled.stream()
			.map(CancelledBooking::sessionId).toList());
		member.changeMemberStatus(request.status());
		accountRepository.flush();

		String action = request.status() == AccountStatus.SUSPENDED ? "MEMBER_SUSPENDED" : "MEMBER_REACTIVATED";
		auditRepository.save(new AuditEvent(UUID.randomUUID(), managerId, action, "ACCOUNT", memberId,
			request.reason().trim(), Map.of("status", before.name()), Map.of("status", request.status().name(),
				"cancelledFutureBookings", cancelledBookings)));
		notificationWriter.write(action + ":" + memberId + ":" + UUID.randomUUID(), memberId, action, "ACCOUNT",
			memberId, Map.of("status", request.status().name()));
		return new MemberStatusChangeResponse(memberId, request.status(), cancelledBookings);
	}

	private List<CancelledBooking> cancelFutureBookings(UUID memberId, UUID managerId) {
		Instant now = clock.instant();
		List<CancelledBooking> affected = jdbcTemplate.query("""
			select b.id, b.class_session_id from bookings b
			join class_sessions s on s.id=b.class_session_id
			where b.member_account_id=? and b.status='BOOKED' and s.start_time > ?
			for update of b
			""", (rs, row) -> new CancelledBooking(rs.getObject("id", UUID.class),
				rs.getObject("class_session_id", UUID.class)), memberId, Timestamp.from(now));
		jdbcTemplate.update("""
			update bookings b
			set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'MEMBER_SUSPENDED', cancelled_at = ?, updated_at = ?, version = b.version + 1
			from class_sessions s
			where b.class_session_id = s.id and b.member_account_id = ?
				and b.status = 'BOOKED' and s.start_time > ?
			""", managerId, Timestamp.from(now), Timestamp.from(now), memberId, Timestamp.from(now));
		return affected;
	}

	private record CancelledBooking(UUID id, UUID sessionId) { }

	private void ensureActiveManager(UUID managerId) {
		if (!accountRepository.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void validate(MemberStatusChangeRequest request) {
		if (request == null || (request.status() != AccountStatus.ACTIVE
				&& request.status() != AccountStatus.SUSPENDED)) {
			throw new StaffAccountValidationException("status", "must be ACTIVE or SUSPENDED");
		}
		if (request.reason() == null || request.reason().trim().isEmpty() || request.reason().trim().length() > 1000) {
			throw new StaffAccountValidationException("reason", "must be non-blank and at most 1000 characters");
		}
	}
}
