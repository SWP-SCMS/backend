package com.scms.backend.manager;

import java.time.Clock;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.Notification;
import com.scms.backend.notification.NotificationRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberStatusService {

	private final AccountRepository accountRepository;
	private final AuditEventRepository auditRepository;
	private final NotificationRepository notificationRepository;
	private final JdbcTemplate jdbcTemplate;
	private final Clock clock;

	MemberStatusService(AccountRepository accountRepository, AuditEventRepository auditRepository,
			NotificationRepository notificationRepository, JdbcTemplate jdbcTemplate, Clock clock) {
		this.accountRepository = accountRepository;
		this.auditRepository = auditRepository;
		this.notificationRepository = notificationRepository;
		this.jdbcTemplate = jdbcTemplate;
		this.clock = clock;
	}

	@Transactional
	MemberStatusChangeResponse changeStatus(UUID managerId, UUID memberId, MemberStatusChangeRequest request) {
		ensureActiveManager(managerId);
		validate(request);
		Account member = accountRepository.findById(memberId)
			.filter(account -> account.getRole() == AccountRole.MEMBER)
			.orElseThrow(MemberAccountNotFoundException::new);
		AccountStatus before = member.getStatus();
		if (before == request.status()) {
			throw new MemberStatusConflictException("Member is already " + before);
		}

		int cancelledBookings = request.status() == AccountStatus.SUSPENDED
			? cancelFutureBookings(memberId, managerId)
			: 0;
		member.changeMemberStatus(request.status());
		accountRepository.flush();

		String action = request.status() == AccountStatus.SUSPENDED ? "MEMBER_SUSPENDED" : "MEMBER_REACTIVATED";
		auditRepository.save(new AuditEvent(UUID.randomUUID(), managerId, action, "ACCOUNT", memberId,
			request.reason().trim(), Map.of("status", before.name()), Map.of("status", request.status().name(),
				"cancelledFutureBookings", cancelledBookings)));
		notificationRepository.save(new Notification(UUID.randomUUID(), memberId, action, "ACCOUNT", memberId,
			Map.of("status", request.status().name())));
		return new MemberStatusChangeResponse(memberId, request.status(), cancelledBookings);
	}

	private int cancelFutureBookings(UUID memberId, UUID managerId) {
		Instant now = clock.instant();
		return jdbcTemplate.update("""
			update bookings b
			set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'MEMBER_SUSPENDED', cancelled_at = ?, updated_at = ?
			from class_sessions s
			where b.class_session_id = s.id and b.member_account_id = ?
				and b.status = 'BOOKED' and s.start_time > ?
			""", managerId, Timestamp.from(now), Timestamp.from(now), memberId, Timestamp.from(now));
	}

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
