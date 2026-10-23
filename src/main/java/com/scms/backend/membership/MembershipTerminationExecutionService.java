package com.scms.backend.membership;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.centervisit.CenterVisitRepository;
import com.scms.backend.notification.NotificationWriter;
import com.scms.backend.scheduling.BookingRepository;
import com.scms.backend.training.CoachAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MembershipTerminationExecutionService {
	private final MembershipLifecycleService lifecycle;
	private final MembershipTerminationRequestRepository requests;
	private final MembershipRepository memberships;
	private final AccountRepository accounts;
	private final CenterVisitRepository visits;
	private final BookingRepository bookings;
	private final CoachAssignmentRepository assignments;
	private final MembershipFreezeExecutionService freezes;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;

	MembershipTerminationExecutionService(MembershipLifecycleService lifecycle,
		MembershipTerminationRequestRepository requests, MembershipRepository memberships,
		AccountRepository accounts, CenterVisitRepository visits, BookingRepository bookings, CoachAssignmentRepository assignments,
		MembershipFreezeExecutionService freezes, AuditEventRepository audits, NotificationWriter notifications) {
		this.lifecycle = lifecycle;
		this.requests = requests;
		this.memberships = memberships;
		this.accounts = accounts;
		this.visits = visits;
		this.bookings = bookings;
		this.assignments = assignments;
		this.freezes = freezes;
		this.audits = audits;
		this.notifications = notifications;
	}

	@Transactional
	void executeImmediate(UUID requestId, UUID actorId, Instant now) {
		MembershipTerminationRequest visible = requests.findById(requestId)
			.orElseThrow(MembershipTerminationException::notFound);
		accounts.findByIdForUpdate(visible.getMemberAccountId()).orElseThrow(MembershipTerminationException::notFound);
		Membership active = memberships.findByMemberAccountIdAndStatusForUpdate(visible.getMemberAccountId(), MembershipStatus.ACTIVE)
			.orElseThrow(MembershipTerminationException::notActive);
		MembershipTerminationRequest request = requests.findByIdForUpdate(requestId)
			.orElseThrow(MembershipTerminationException::notFound);
		if (request.getStatus() != MembershipTerminationRequestStatus.APPROVED
			|| request.getMode() != MembershipTerminationMode.IMMEDIATE || !request.getMembershipId().equals(active.getId())) {
			throw MembershipTerminationException.invalidTransition();
		}
		freezes.endActiveForTermination(request.getMemberAccountId(), now);
		lifecycle.terminateActiveAndActivateReserved(request.getMemberAccountId(), now);
		visits.checkoutOpenForMembershipTermination(request.getMemberAccountId(), actorId, now);
		bookings.cancelFutureBookingsForMembershipTermination(request.getMemberAccountId(), actorId, now);
		assignments.endActiveAssignmentsForMembershipTermination(request.getMemberAccountId(), now);
		request.execute();
		requests.saveAndFlush(request);
		writeOutcome(actorId, request, "EXECUTED");
	}

	@Transactional
	void reconcileDue(Instant now) {
		for (MembershipTerminationRequest request : requests.findApprovedDueForUpdate(now)) {
			Membership membership = memberships.findByIdForUpdate(request.getMembershipId()).orElse(null);
			if (membership == null || membership.getStatus() == MembershipStatus.ACTIVE) continue;
			if (membership.getStatus() == MembershipStatus.EXPIRED) {
				request.execute();
				writeOutcome(request.getApproverAccountId(), request, "EXECUTED");
			} else if (membership.getStatus() == MembershipStatus.TERMINATED) {
				request.obsolete();
				writeOutcome(request.getApproverAccountId(), request, "OBSOLETE");
			}
			requests.saveAndFlush(request);
		}
	}

	private void writeOutcome(UUID actorId, MembershipTerminationRequest request, String outcome) {
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "MEMBERSHIP_TERMINATION_" + outcome,
			"MEMBERSHIP_TERMINATION_REQUEST", request.getId(), request.getReason(), Map.of(),
			Map.of("membershipId", request.getMembershipId().toString(), "memberAccountId",
				request.getMemberAccountId().toString(), "status", request.getStatus().name())));
		notifications.write("MEMBERSHIP_TERMINATION_" + outcome + ":" + request.getId(),
			request.getMemberAccountId(), "MEMBERSHIP_TERMINATION_" + outcome,
			"MEMBERSHIP_TERMINATION_REQUEST", request.getId(), Map.of());
	}
}
