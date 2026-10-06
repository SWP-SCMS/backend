package com.scms.backend.scheduling;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoachTeachingScheduleService {

	private final ClassSessionRepository sessions;
	private final BookingRepository bookings;
	private final AccountRepository accounts;
	private final Clock clock;

	CoachTeachingScheduleService(ClassSessionRepository sessions, BookingRepository bookings,
			AccountRepository accounts, Clock clock) {
		this.sessions = sessions;
		this.bookings = bookings;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional
	ClassSessionPageResponse list(UUID coachId, Instant from, Instant to, ClassSessionStatus status,
			Pageable pageable) {
		ensureActiveCoach(coachId);
		validateRange(from, to);
		advanceStatuses();
		Specification<ClassSession> filters = (root, ignored, builder) ->
			builder.equal(root.get("teachingCoach").get("id"), coachId);
		if (from != null) filters = filters.and((root, ignored, builder) ->
			builder.greaterThan(root.get("endTime"), from));
		if (to != null) filters = filters.and((root, ignored, builder) ->
			builder.lessThan(root.get("startTime"), to));
		if (status != null) filters = filters.and((root, ignored, builder) ->
			builder.equal(root.get("status"), status));
		var page = sessions.findAll(filters, pageable);
		return new ClassSessionPageResponse(page.getContent().stream().map(ClassSessionResponse::from).toList(),
			page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

	@Transactional
	CoachClassSessionDetailResponse get(UUID coachId, UUID sessionId) {
		ensureActiveCoach(coachId);
		advanceStatuses();
		ClassSession session = sessions.findByIdAndTeachingCoach_Id(sessionId, coachId)
			.orElseThrow(() -> RecurringScheduleException.notFound("session"));
		var members = bookings.findBookedMembersBySessionId(sessionId).stream()
			.map(CoachSessionMemberResponse::from).toList();
		return CoachClassSessionDetailResponse.from(session, members);
	}

	private void ensureActiveCoach(UUID coachId) {
		if (!accounts.existsByIdAndRoleAndStatus(coachId, AccountRole.COACH, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void validateRange(Instant from, Instant to) {
		if (from != null && to != null && !to.isAfter(from)) {
			throw RecurringScheduleException.validation("to", "must be later than from");
		}
	}

	private void advanceStatuses() {
		Instant now = clock.instant();
		sessions.advanceScheduledToInProgress(now);
		sessions.advanceInProgressToCompleted(now);
	}
}
