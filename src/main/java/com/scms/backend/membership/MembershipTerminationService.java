package com.scms.backend.membership;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.identity.receptionmember.ReceptionMemberNotFoundException;
import com.scms.backend.notification.NotificationWriter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class MembershipTerminationService {
	private static final Set<MembershipTerminationRequestStatus> OPEN = Set.of(
		MembershipTerminationRequestStatus.PENDING, MembershipTerminationRequestStatus.APPROVED);
	private static final Set<AccountStatus> VISIBLE = Set.of(AccountStatus.ACTIVE, AccountStatus.SUSPENDED);
	private final AccountRepository accounts;
	private final MemberProfileRepository profiles;
	private final MembershipRepository memberships;
	private final MembershipTerminationRequestRepository requests;
	private final AuditEventRepository audits;
	private final NotificationWriter notifications;
	private final Clock clock;

	MembershipTerminationService(AccountRepository accounts, MemberProfileRepository profiles,
			MembershipRepository memberships, MembershipTerminationRequestRepository requests,
			AuditEventRepository audits, NotificationWriter notifications, Clock clock) {
		this.accounts = accounts;
		this.profiles = profiles;
		this.memberships = memberships;
		this.requests = requests;
		this.audits = audits;
		this.notifications = notifications;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	MembershipTerminationRequestPageResponse listForMember(UUID memberId, int page, int size) {
		ensureRole(memberId, AccountRole.MEMBER);
		return page(requests.findByMemberAccountIdOrderByCreatedAtDesc(memberId, pageable(page, size)));
	}

	@Transactional
	MembershipTerminationRequestResponse createForMember(UUID memberId, MembershipTerminationCreateRequest input) {
		ensureRole(memberId, AccountRole.MEMBER);
		return create(memberId, memberId, input, true);
	}

	@Transactional
	MembershipTerminationRequestPageResponse listForReceptionist(UUID receptionistId, String memberCode, int page, int size) {
		ensureRole(receptionistId, AccountRole.RECEPTIONIST);
		return page(requests.findByMemberAccountIdOrderByCreatedAtDesc(findMember(memberCode).getAccountId(), pageable(page, size)));
	}

	@Transactional
	MembershipTerminationRequestResponse createForReceptionist(UUID receptionistId, String memberCode,
			MembershipTerminationCreateRequest input) {
		ensureRole(receptionistId, AccountRole.RECEPTIONIST);
		return create(receptionistId, findMember(memberCode).getAccountId(), input, true);
	}

	@Transactional
	void withdrawForMember(UUID memberId, UUID requestId) {
		ensureRole(memberId, AccountRole.MEMBER);
		MembershipTerminationRequest request = requests.findByIdForUpdate(requestId)
			.filter(value -> value.getMemberAccountId().equals(memberId)).orElseThrow(MembershipTerminationException::notFound);
		if (request.getStatus() != MembershipTerminationRequestStatus.PENDING) throw MembershipTerminationException.invalidTransition();
		request.withdraw(Instant.now(clock));
		requests.saveAndFlush(request);
		audit(memberId, "WITHDRAWN", request);
		notifications.write("MEMBERSHIP_TERMINATION_WITHDRAWN:" + requestId, memberId,
			"MEMBERSHIP_TERMINATION_WITHDRAWN", "MEMBERSHIP_TERMINATION_REQUEST", requestId, Map.of());
		notifyManagers(request, "MEMBERSHIP_TERMINATION_WITHDRAWN");
	}

	@Transactional(readOnly = true)
	MembershipTerminationRequestPageResponse listForManager(UUID managerId, int page, int size) {
		ensureRole(managerId, AccountRole.MANAGER);
		return page(requests.findAllByOrderByCreatedAtDesc(pageable(page, size)));
	}

	@Transactional
	MembershipTerminationRequestResponse approve(UUID managerId, UUID requestId, MembershipTerminationDecisionRequest decision) {
		ensureRole(managerId, AccountRole.MANAGER);
		MembershipTerminationRequest request = lockRequest(requestId);
		checkVersion(request, decision);
		if (request.getStatus() != MembershipTerminationRequestStatus.PENDING) throw MembershipTerminationException.invalidTransition();
		Membership membership = memberships.findByIdForUpdate(request.getMembershipId()).orElseThrow(MembershipTerminationException::notFound);
		if (membership.getStatus() != MembershipStatus.ACTIVE) throw MembershipTerminationException.notActive();
		Instant now = Instant.now(clock);
		request.approve(managerId, request.getMode() == MembershipTerminationMode.IMMEDIATE ? now : membership.getEndsAt(), now);
		requests.saveAndFlush(request);
		audit(managerId, "APPROVED", request);
		notifications.write("MEMBERSHIP_TERMINATION_APPROVED:" + requestId, request.getMemberAccountId(),
			"MEMBERSHIP_TERMINATION_APPROVED", "MEMBERSHIP_TERMINATION_REQUEST", requestId, Map.of("mode", request.getMode().name()));
		return response(request);
	}

	@Transactional
	MembershipTerminationRequestResponse reject(UUID managerId, UUID requestId, MembershipTerminationDecisionRequest decision) {
		ensureRole(managerId, AccountRole.MANAGER);
		MembershipTerminationRequest request = lockRequest(requestId);
		checkVersion(request, decision);
		if (request.getStatus() != MembershipTerminationRequestStatus.PENDING) throw MembershipTerminationException.invalidTransition();
		request.reject(managerId, Instant.now(clock));
		requests.saveAndFlush(request);
		audit(managerId, "REJECTED", request);
		notifications.write("MEMBERSHIP_TERMINATION_REJECTED:" + requestId, request.getMemberAccountId(),
			"MEMBERSHIP_TERMINATION_REJECTED", "MEMBERSHIP_TERMINATION_REQUEST", requestId, Map.of());
		return response(request);
	}

	@Transactional
	MembershipTerminationRequestResponse createDirect(UUID managerId, UUID memberAccountId,
			MembershipTerminationCreateRequest input) {
		ensureRole(managerId, AccountRole.MANAGER);
		MembershipTerminationRequestResponse response = create(managerId, memberAccountId, input, false);
		return approve(managerId, response.id(), new MembershipTerminationDecisionRequest(response.version()));
	}

	private MembershipTerminationRequestResponse create(UUID requesterId, UUID memberId,
			MembershipTerminationCreateRequest input, boolean notifyPendingQueue) {
		validate(input);
		accounts.findByIdForUpdate(memberId).filter(account -> account.getRole() == AccountRole.MEMBER
			&& account.getStatus() == AccountStatus.ACTIVE).orElseThrow(MembershipTerminationException::notActive);
		Membership membership = memberships.findByMemberAccountIdAndStatusForUpdate(memberId, MembershipStatus.ACTIVE)
			.orElseThrow(MembershipTerminationException::notActive);
		if (requests.existsByMembershipIdAndStatusIn(membership.getId(), OPEN)) throw MembershipTerminationException.alreadyPending();
		MembershipTerminationRequest request = new MembershipTerminationRequest(UUID.randomUUID(), membership.getId(),
			memberId, requesterId, input.mode(), input.reason().trim());
		requests.saveAndFlush(request);
		audit(requesterId, "REQUESTED", request);
		notifications.write("MEMBERSHIP_TERMINATION_REQUESTED:" + request.getId(), memberId,
			"MEMBERSHIP_TERMINATION_REQUESTED", "MEMBERSHIP_TERMINATION_REQUEST", request.getId(), Map.of("mode", input.mode().name()));
		if (notifyPendingQueue) notifyManagers(request, "MEMBERSHIP_TERMINATION_REQUESTED");
		return response(request);
	}

	private MembershipTerminationRequest lockRequest(UUID id) { return requests.findByIdForUpdate(id).orElseThrow(MembershipTerminationException::notFound); }
	private void checkVersion(MembershipTerminationRequest request, MembershipTerminationDecisionRequest decision) {
		if (decision == null || decision.expectedVersion() == null || !decision.expectedVersion().equals(request.getVersion())) throw MembershipTerminationException.changed();
	}
	private void validate(MembershipTerminationCreateRequest input) {
		if (input == null || input.mode() == null || input.reason() == null || input.reason().isBlank() || input.reason().trim().length() > 1000) {
			throw new MembershipTerminationException(org.springframework.http.HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "mode and a non-blank reason of at most 1000 characters are required.");
		}
	}
	private MemberProfile findMember(String code) {
		if (code == null || code.isBlank()) throw new ReceptionMemberNotFoundException();
		return profiles.findVisibleMemberByCode(code.trim(), AccountRole.MEMBER, VISIBLE).orElseThrow(ReceptionMemberNotFoundException::new);
	}
	private void ensureRole(UUID id, AccountRole role) { if (!accounts.existsByIdAndRoleAndStatus(id, role, AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException(); }
	private void audit(UUID actor, String action, MembershipTerminationRequest request) {
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "MEMBERSHIP_TERMINATION_" + action,
			"MEMBERSHIP_TERMINATION_REQUEST", request.getId(), request.getReason(), Map.of(),
			Map.of("membershipId", request.getMembershipId().toString(), "memberAccountId", request.getMemberAccountId().toString(), "status", request.getStatus().name())));
	}
	private PageRequest pageable(int page, int size) { return PageRequest.of(page, size, Sort.by("createdAt").descending().and(Sort.by("id").descending())); }
	private MembershipTerminationRequestResponse response(MembershipTerminationRequest request) {
		return MembershipTerminationRequestResponse.from(request,
			accounts.findMemberIdentity(request.getMemberAccountId()).orElse(null));
	}
	private void notifyManagers(MembershipTerminationRequest request, String event) {
		var member = accounts.findMemberIdentity(request.getMemberAccountId()).orElse(null);
		Map<String, Object> payload = new java.util.LinkedHashMap<>();
		payload.put("requestId", request.getId().toString());
		payload.put("memberAccountId", request.getMemberAccountId().toString());
		if (member != null) {
			payload.put("memberId", member.getMemberCode());
			payload.put("memberFullName", member.getFullName());
		}
		payload.put("requestType", "TERMINATION");
		payload.put("status", request.getStatus().name());
		for (Account manager : accounts.findByRoleAndStatusOrderById(AccountRole.MANAGER, AccountStatus.ACTIVE)) {
			notifications.write(event + ":" + request.getId() + ":" + manager.getId(), manager.getId(),
				event, "MEMBERSHIP_TERMINATION_REQUEST", request.getId(), payload);
		}
	}
	private MembershipTerminationRequestPageResponse page(org.springframework.data.domain.Page<MembershipTerminationRequest> page) {
		return MembershipTerminationRequestPageResponse.from(page, this::response);
	}
}
