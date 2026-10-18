package com.scms.backend.training;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.manager.StaffAccountValidationException;
import com.scms.backend.membership.MembershipRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentDiscoveryService {

	private final CoachAssignmentRepository assignments;
	private final MembershipRepository memberships;
	private final AccountRepository accounts;
	private final Clock clock;

	public AssignmentDiscoveryService(CoachAssignmentRepository assignments, MembershipRepository memberships,
			AccountRepository accounts, Clock clock) {
		this.assignments = assignments;
		this.memberships = memberships;
		this.accounts = accounts;
		this.clock = clock;
	}

	@Transactional
	public AssignmentCandidatePageResponse candidates(UUID managerId, String query,
			AssignmentCandidateStatus assignmentStatus, Pageable pageable) {
		ensureActiveManager(managerId);
		String normalizedQuery = normalizeQuery(query);
		Instant now = reconcile();
		Page<CoachAssignmentRepository.AssignmentCandidateView> page = assignments.findAssignmentCandidates(
			normalizedQuery, assignmentStatus.name(), now, pageable);
		return new AssignmentCandidatePageResponse(page.getContent().stream()
			.map(AssignmentCandidateResponse::from).toList(), page.getNumber(), page.getSize(),
			page.getTotalElements(), page.getTotalPages());
	}

	@Transactional
	public List<CoachAssignmentWorkloadResponse> workloads(UUID managerId) {
		ensureActiveManager(managerId);
		reconcile();
		return assignments.findAssignmentWorkloads().stream()
			.map(CoachAssignmentWorkloadResponse::from).toList();
	}

	private Instant reconcile() {
		Instant now = clock.instant();
		memberships.expireDue(now);
		assignments.endAllActiveAssignmentsWithExpiredPlus(now);
		return now;
	}

	private void ensureActiveManager(UUID managerId) {
		if (!accounts.existsByIdAndRoleAndStatus(managerId, AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String normalizeQuery(String query) {
		if (query == null || query.trim().isEmpty()) return null;
		String normalized = query.trim();
		if (normalized.length() > 100) {
			throw new StaffAccountValidationException("query", "must be at most 100 characters");
		}
		return normalized;
	}
}
