package com.scms.backend.scheduling.coachleave;

import com.scms.backend.scheduling.CoachLeaveException;
import com.scms.backend.scheduling.CoachLeaveStatus;
import com.scms.backend.scheduling.session.AvailableCoachResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoachLeaveImpactService {

	private final CoachLeaveService leaves;

	private final JdbcTemplate db;

	private final Clock clock;

	CoachLeaveImpactService(CoachLeaveService leaves, JdbcTemplate db, Clock clock) {
		this.leaves = leaves;
		this.db = db;
		this.clock = clock;
		this.coachLeaveImpactRepository = new CoachLeaveImpactRepository(this.db, this.clock);
	}

	@Transactional
	Impact detail(UUID managerId, UUID requestId) {
		CoachLeaveResponse request = leaves.managerDetail(managerId, requestId);
		List<UUID> ids = request.affectedSessionIds();
		if (ids.isEmpty())
			return new Impact(request, List.of());
		return affectedSessionRows(request, ids);
	}

	@Transactional
	List<AvailableCoachResponse> available(UUID managerId, UUID requestId, String type, UUID sessionId) {
		Impact impact = detail(managerId, requestId);
		if (impact.request().status() != CoachLeaveStatus.PENDING)
			throw CoachLeaveException.conflict("COACH_LEAVE_NOT_PENDING", "Only pending leave requests can be resolved.");
		AffectedSession session = impact.sessions().stream().filter(s -> s.id().equals(sessionId) && s.type().equals(type)).findFirst().orElseThrow(CoachLeaveException::notFound);
		return available(impact.request().coachId(), session);
	}

	List<AvailableCoachResponse> available(UUID excludedCoachId, AffectedSession session) {
		return availableCoachRows(excludedCoachId, session);
	}

	record Impact(CoachLeaveResponse request, List<AffectedSession> sessions) {
	}

	record AffectedSession(UUID id, String type, String name, UUID courseRunId, String runCode, Integer unitNumber, Instant startTime, Instant endTime, UUID coachId, String coachName, UUID roomId, String roomName, int capacity, long memberCount, String status, long version, boolean canReassign, boolean canReschedule, boolean canCancel) {
	}

	private Impact affectedSessionRows(CoachLeaveResponse request, List<UUID> ids) {
		return coachLeaveImpactRepository.affectedSessionRows(request, ids);
	}

	private List<AvailableCoachResponse> availableCoachRows(UUID excludedCoachId, AffectedSession session) {
		return coachLeaveImpactRepository.availableCoachRows(excludedCoachId, session);
	}

	private final CoachLeaveImpactRepository coachLeaveImpactRepository;
}
