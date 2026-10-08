package com.scms.backend.scheduling;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionCancellationService {

	private final ClassSessionRepository sessions;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final JdbcTemplate jdbc;
	private final ApplicationEventPublisher events;
	private final SessionLifecycleService lifecycle;
	private final Clock clock;

	ClassSessionCancellationService(ClassSessionRepository sessions, AccountRepository accounts,
			AuditEventRepository audits, JdbcTemplate jdbc, ApplicationEventPublisher events,
			SessionLifecycleService lifecycle, Clock clock) {
		this.sessions = sessions;
		this.accounts = accounts;
		this.audits = audits;
		this.jdbc = jdbc;
		this.events = events;
		this.lifecycle = lifecycle;
		this.clock = clock;
	}

	@Transactional
	ClassSessionDetailResponse cancel(UUID managerId, UUID sessionId, ClassSessionCancellationRequest request) {
		ensureActiveManager(managerId);
		String reason = validate(request);
		Instant now = clock.instant();
		lifecycle.catchUp(now);
		ClassSession session = sessions.findByIdForUpdate(sessionId)
			.orElseThrow(() -> RecurringScheduleException.notFound("session"));
		if (session.getStatus() != ClassSessionStatus.SCHEDULED) {
			throw RecurringScheduleException.sessionNotScheduled();
		}

		List<UUID> memberIds = jdbc.queryForList("""
			select member_account_id from bookings
			where class_session_id = ? and status = 'BOOKED'
			for update
			""", UUID.class, sessionId);
		session.cancel(now, reason);
		sessions.saveAndFlush(session);
		int cancelledBookings = jdbc.update("""
			update bookings set status = 'CANCELLED', cancelled_by_account_id = ?,
				cancellation_source = 'SESSION_CANCELLED', cancelled_at = ?, updated_at = ?, version = version + 1
			where class_session_id = ? and status = 'BOOKED'
			""", managerId, Timestamp.from(now), Timestamp.from(now), sessionId);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "SESSION_CANCELLED", "CLASS_SESSION", sessionId,
			reason, Map.of("status", ClassSessionStatus.SCHEDULED.name()),
			Map.of("status", ClassSessionStatus.CANCELLED.name(), "cancelledBookings", cancelledBookings)));
		if (!memberIds.isEmpty()) events.publishEvent(new ClassSessionCancelledEvent(sessionId, memberIds));
		return ClassSessionDetailResponse.from(session);
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String validate(ClassSessionCancellationRequest request) {
		if (request == null || request.reason() == null || request.reason().trim().isEmpty()
				|| request.reason().trim().length() > 1000) {
			throw RecurringScheduleException.validation("reason", "must be non-blank and at most 1000 characters");
		}
		return request.reason().trim();
	}
}
