package com.scms.backend.scheduling;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionCancellationService {

	private final ClassSessionRepository sessions;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final JdbcTemplate jdbc;
	private final NotificationWriter notifications;
	private final SessionLifecycleService lifecycle;
	private final Clock clock;

	ClassSessionCancellationService(ClassSessionRepository sessions, AccountRepository accounts,
			AuditEventRepository audits, JdbcTemplate jdbc, NotificationWriter notifications,
			SessionLifecycleService lifecycle, Clock clock) {
		this.sessions = sessions;
		this.accounts = accounts;
		this.audits = audits;
		this.jdbc = jdbc;
		this.notifications = notifications;
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
		memberIds.forEach(memberId -> notifications.write("SESSION_CANCELLED:" + sessionId + ":" + memberId,
			memberId, "SESSION_CANCELLED", "CLASS_SESSION", sessionId, Map.of("sessionId", sessionId.toString())));
		return ClassSessionDetailResponse.from(session);
	}

	@Transactional(readOnly = true)
	ClassSessionCancellationPreviewResponse preview(UUID managerId, ClassSessionCancellationBatchRequest request) {
		ensureActiveManager(managerId);
		List<UUID> eligible = findEligible(request).stream()
			.map(ClassSession::getId).toList();
		return new ClassSessionCancellationPreviewResponse(eligible.size(), eligible);
	}

	@Transactional
	ClassSessionCancellationBatchResponse cancelBulk(UUID managerId, ClassSessionCancellationBatchRequest request) {
		ensureActiveManager(managerId);
		String reason = validate(request == null ? null : new ClassSessionCancellationRequest(request.reason()));
		List<UUID> requestedIds = validateSelector(request);
		List<UUID> eligible = findEligible(request).stream()
			.map(ClassSession::getId).toList();
		if (requestedIds != null && eligible.size() != requestedIds.size()) {
			throw RecurringScheduleException.validation("sessionIds",
				"all sessionIds must identify future SCHEDULED Sessions; eligible IDs: " + eligible);
		}
		for (UUID sessionId : eligible) {
			cancel(managerId, sessionId, new ClassSessionCancellationRequest(reason));
		}
		return new ClassSessionCancellationBatchResponse(eligible.size(), eligible);
	}

	private List<ClassSession> findEligible(ClassSessionCancellationBatchRequest request) {
		validateSelector(request);
		Instant now = clock.instant();
		Specification<ClassSession> filter = (root, ignored, builder) -> builder.and(
			builder.equal(root.get("status"), ClassSessionStatus.SCHEDULED),
			builder.greaterThan(root.get("startTime"), now));
		if (request.sessionIds() != null) {
			filter = filter.and((root, ignored, builder) -> root.get("id").in(request.sessionIds()));
		}
		if (request.classId() != null) {
			filter = filter.and((root, ignored, builder) ->
				builder.equal(root.get("sportClass").get("id"), request.classId()));
		}
		if (request.from() != null) {
			filter = filter.and((root, ignored, builder) ->
				builder.greaterThan(root.get("endTime"), request.from()));
		}
		if (request.to() != null) {
			filter = filter.and((root, ignored, builder) ->
				builder.lessThan(root.get("startTime"), request.to()));
		}
		return sessions.findAll(filter, Sort.by("startTime").ascending().and(Sort.by("id").ascending()));
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

	private List<UUID> validateSelector(ClassSessionCancellationBatchRequest request) {
		if (request == null) {
			throw RecurringScheduleException.validation("selector", "at least one cancellation filter is required");
		}
		if (request.from() != null && request.to() != null && !request.to().isAfter(request.from())) {
			throw RecurringScheduleException.validation("to", "must be later than from");
		}
		if (request.sessionIds() != null && (request.sessionIds().isEmpty()
				|| request.sessionIds().stream().anyMatch(id -> id == null)
				|| request.sessionIds().size() != new HashSet<>(request.sessionIds()).size())) {
			throw RecurringScheduleException.validation("sessionIds", "must contain unique Session IDs");
		}
		if (request.sessionIds() == null && request.classId() == null
				&& request.from() == null && request.to() == null) {
			throw RecurringScheduleException.validation("selector",
				"sessionIds, classId, from, or to is required");
		}
		return request.sessionIds();
	}
}
