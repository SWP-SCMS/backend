package com.scms.backend.training;

import java.time.Instant;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.scheduling.PtAssignmentImpactResponse;

public record CoachAssignmentResponse(
	UUID id,
	UUID memberAccountId,
	UUID coachAccountId,
	String coachFullName,
	String coachPhone,
	String coachEmail,
	UUID assignedByAccountId,
	CoachAssignmentStatus status,
	Instant startsAt,
	Instant endsAt,
	String endReason,
	Instant createdAt,
	PtAssignmentImpactResponse processedImpact
) {
	public CoachAssignmentResponse(UUID id, UUID memberAccountId, UUID coachAccountId, String coachFullName,
			String coachPhone, String coachEmail, UUID assignedByAccountId, CoachAssignmentStatus status,
			Instant startsAt, Instant endsAt, String endReason, Instant createdAt) {
		this(id, memberAccountId, coachAccountId, coachFullName, coachPhone, coachEmail, assignedByAccountId,
			status, startsAt, endsAt, endReason, createdAt, null);
	}

	public static CoachAssignmentResponse from(CoachAssignment assignment, Account coach) {
		return new CoachAssignmentResponse(
			assignment.getId(),
			assignment.getMemberAccountId(),
			assignment.getCoachAccountId(),
			coach != null ? coach.getFullName() : null,
			coach != null ? coach.getPhone() : null,
			coach != null ? coach.getEmail() : null,
			assignment.getAssignedByAccountId(),
			assignment.getStatus(),
			assignment.getStartsAt(),
			assignment.getEndsAt(),
			assignment.getEndReason(),
			assignment.getCreatedAt(),
			null
		);
	}

	public static CoachAssignmentResponse from(CoachAssignment assignment, Account coach,
			PtAssignmentImpactResponse processedImpact) {
		CoachAssignmentResponse base = from(assignment, coach);
		return new CoachAssignmentResponse(base.id(), base.memberAccountId(), base.coachAccountId(),
			base.coachFullName(), base.coachPhone(), base.coachEmail(), base.assignedByAccountId(),
			base.status(), base.startsAt(), base.endsAt(), base.endReason(), base.createdAt(), processedImpact);
	}
}
