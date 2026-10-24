package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoachLeaveLifecycleService {

	private final CoachLeaveRepository leaves;
	private final AuditEventRepository audits;

	CoachLeaveLifecycleService(CoachLeaveRepository leaves, AuditEventRepository audits) {
		this.leaves = leaves;
		this.audits = audits;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	int catchUp(Instant now) {
		var started = leaves.findPendingStartedForUpdate(now);
		started.forEach(leave -> expire(leave, now));
		return started.size();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	boolean expireIfStarted(UUID requestId, Instant now) {
		return leaves.findByIdForUpdate(requestId)
			.filter(leave -> leave.getStatus() == CoachLeaveStatus.PENDING)
			.filter(leave -> !leave.getStartTime().isAfter(now))
			.map(leave -> {
				expire(leave, now);
				return true;
			})
			.orElse(false);
	}

	private void expire(CoachLeaveRequest leave, Instant now) {
		leave.expire(now);
		audits.save(new AuditEvent(UUID.randomUUID(), "COACH_LEAVE_EXPIRED", "COACH_LEAVE_REQUEST",
			leave.getId(), Map.of("coachId", leave.getCoach().getId().toString(),
				"startTime", leave.getStartTime().toString(), "endTime", leave.getEndTime().toString(),
				"status", CoachLeaveStatus.EXPIRED.name())));
	}
}
