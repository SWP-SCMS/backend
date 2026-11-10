package com.scms.backend.scheduling.session;

import com.scms.backend.scheduling.ClassSession;
import com.scms.backend.scheduling.ClassSessionStatus;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.SportClassType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

final class ClassSessionCatalog {

	private final ClassSessionRepository sessions;

	private final SessionLifecycleService lifecycle;

	private final Clock clock;

	ClassSessionCatalog(ClassSessionRepository sessions, SessionLifecycleService lifecycle, Clock clock) {
		this.sessions = sessions;
		this.lifecycle = lifecycle;
		this.clock = clock;
	}

	MemberClassSessionPageResponse futureCatalogue(Instant from, Instant to, UUID classId, UUID coachId, boolean ptOnly, Pageable pageable) {
		if (from != null && to != null && !to.isAfter(from)) {
			throw RecurringScheduleException.validation("to", "must be later than from");
		}
		advanceStatuses();
		Instant now = clock.instant();
		Specification<ClassSession> filters = (root, ignored, builder) -> builder.and(builder.equal(root.get("status"), ClassSessionStatus.SCHEDULED), builder.greaterThan(root.get("startTime"), now), ptOnly ? builder.equal(root.get("sportClass").get("classType"), SportClassType.PT_1_1) : builder.notEqual(root.get("sportClass").get("classType"), SportClassType.PT_1_1));
		if (from != null)
			filters = filters.and((root, ignored, builder) -> builder.greaterThan(root.get("endTime"), from));
		if (to != null)
			filters = filters.and((root, ignored, builder) -> builder.lessThan(root.get("startTime"), to));
		if (classId != null)
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("sportClass").get("id"), classId));
		if (coachId != null)
			filters = filters.and((root, ignored, builder) -> builder.equal(root.get("teachingCoach").get("id"), coachId));
		var page = sessions.findAll(filters, pageable);
		var ids = page.getContent().stream().map(ClassSession::getId).toList();
		Map<UUID, Long> booked = ids.isEmpty() ? Map.of() : sessions.countBookedBySessionIds(ids).stream().collect(Collectors.toMap(ClassSessionRepository.BookingCountView::getSessionId, ClassSessionRepository.BookingCountView::getBookedCount));
		Map<UUID, List<String>> requiredBenefits = ids.isEmpty() ? Map.of() : sessions.findRequiredBenefitsBySessionIds(ids).stream().collect(Collectors.groupingBy(ClassSessionRepository.SessionRequiredBenefitView::getSessionId, Collectors.mapping(ClassSessionRepository.SessionRequiredBenefitView::getBenefitCode, Collectors.toList())));
		var content = page.getContent().stream().map(session -> MemberClassSessionResponse.from(session, booked.getOrDefault(session.getId(), 0L), now, requiredBenefits.getOrDefault(session.getId(), List.of()))).toList();
		return new MemberClassSessionPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}

	MemberClassSessionPageResponse emptyCatalogue(Pageable pageable) {
		return new MemberClassSessionPageResponse(List.of(), pageable.getPageNumber(), pageable.getPageSize(), 0, 0);
	}

	void advanceStatuses() {
		lifecycle.catchUp(clock.instant());
	}
}
