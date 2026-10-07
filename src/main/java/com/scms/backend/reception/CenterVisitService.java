package com.scms.backend.reception;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CenterVisitService {

	private final ReceptionMemberProfileService members;
	private final CenterVisitRepository visits;
	private final AccountRepository accounts;
	private final MemberProfileRepository profiles;
	private final AuditEventRepository audits;
	private final Clock clock;

	CenterVisitService(ReceptionMemberProfileService members, CenterVisitRepository visits,
			AccountRepository accounts, MemberProfileRepository profiles, AuditEventRepository audits, Clock clock) {
		this.members = members;
		this.visits = visits;
		this.accounts = accounts;
		this.profiles = profiles;
		this.audits = audits;
		this.clock = clock;
	}

	@Transactional
	CenterVisitResponse checkIn(UUID receptionistId, CenterVisitRequest request, String idempotencyKey) {
		if (request == null || !Boolean.TRUE.equals(request.identityVerified())) {
			throw CenterVisitException.identityVerificationRequired();
		}
		String key = normalizeKey(idempotencyKey);
		ReceptionMemberSearchResponse member = members.search(receptionistId, request.memberId(), request.phone());
		if (key != null) {
			var replay = visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, key);
			if (replay.isPresent()) {
				if (!replay.get().getMemberAccountId().equals(member.accountId())) {
					throw CenterVisitException.idempotencyKeyReused();
				}
				return CenterVisitResponse.from(replay.get(), member.memberId(), false);
			}
		}
		if (member.status() != AccountStatus.ACTIVE) throw CenterVisitException.memberNotActive();
		if (member.profileImageUrl() == null || member.profileImageUrl().isBlank()) {
			throw CenterVisitException.profileImageRequired();
		}
		if (lockMember(member.accountId()).getStatus() != AccountStatus.ACTIVE) {
			throw CenterVisitException.memberNotActive();
		}

		Instant now = clock.instant();
		UUID membershipId = visits.findCurrentMembership(member.accountId(), now)
			.orElseThrow(CenterVisitException::membershipRequired);
		visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(member.accountId())
			.ifPresent(open -> {
				close(open, receptionistId, CheckoutSource.AUTO_REENTRY, now);
				visits.flush();
			});
		CenterVisit visit = new CenterVisit(UUID.randomUUID(), member.accountId(), membershipId,
			receptionistId, now, key);
		try {
			visits.saveAndFlush(visit);
		}
		catch (DataIntegrityViolationException exception) {
			if (key != null) throw CenterVisitException.idempotencyKeyReused();
			throw exception;
		}
		audits.save(new AuditEvent(UUID.randomUUID(), receptionistId, "CENTER_VISIT_CREATED", "CENTER_VISIT",
			visit.getId(), Map.of("identityVerified", true, "memberId", member.accountId().toString(),
				"membershipId", membershipId.toString(), "visitId", visit.getId().toString())));
		return CenterVisitResponse.from(visit, member.memberId(), true);
	}

	@Transactional(readOnly = true)
	CenterVisitResponse currentMember(UUID memberId) {
		ensureActiveMember(memberId);
		CenterVisit visit = visits
			.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId)
			.orElseThrow(CenterVisitException::openVisitNotFound);
		return CenterVisitResponse.from(visit, memberCode(memberId), false);
	}

	@Transactional
	CenterVisitResponse checkoutMember(UUID memberId) {
		if (lockMember(memberId).getStatus() != AccountStatus.ACTIVE) {
			throw new InvalidAuthenticatedAccountException();
		}
		return checkout(memberId, memberId, CheckoutSource.MEMBER);
	}

	@Transactional
	CenterVisitResponse checkoutReceptionist(UUID receptionistId, UUID memberId) {
		ensureActiveReceptionist(receptionistId);
		lockMember(memberId);
		return checkout(memberId, receptionistId, CheckoutSource.RECEPTIONIST);
	}

	@Transactional(readOnly = true)
	CenterVisitPageResponse openVisits(UUID receptionistId, Pageable pageable) {
		ensureActiveReceptionist(receptionistId);
		var page = visits.findByCheckedOutAtIsNull(pageable);
		var content = page.getContent().stream()
			.map(visit -> CenterVisitResponse.from(visit, memberCode(visit.getMemberAccountId()), false)).toList();
		return new CenterVisitPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages());
	}

	private CenterVisitResponse checkout(UUID memberId, UUID actorId, CheckoutSource source) {
		var open = visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId);
		if (open.isEmpty()) {
			CenterVisit replay = visits
				.findFirstByMemberAccountIdAndCheckedOutAtIsNotNullOrderByCheckedOutAtDescIdDesc(memberId)
				.orElseThrow(CenterVisitException::openVisitNotFound);
			return CenterVisitResponse.from(replay, memberCode(memberId), false);
		}
		CenterVisit visit = open.get();
		close(visit, actorId, source, clock.instant());
		visits.save(visit);
		return CenterVisitResponse.from(visit, memberCode(memberId), false);
	}

	private void close(CenterVisit visit, UUID actorId, CheckoutSource source, Instant at) {
		visit.checkout(actorId, source, at);
		audits.save(new AuditEvent(UUID.randomUUID(), actorId, "CENTER_VISIT_CHECKED_OUT", "CENTER_VISIT",
			visit.getId(), source + " checkout", Map.of("checkedOut", false),
			Map.of("checkedOut", true, "checkedOutAt", at.toString(), "checkedOutBy", actorId.toString(),
				"checkoutSource", source.name())));
	}

	private String normalizeKey(String key) {
		if (key == null) return null;
		String normalized = key.trim();
		if (normalized.isEmpty() || normalized.length() > 100) {
			throw CenterVisitException.invalidIdempotencyKey();
		}
		return normalized;
	}

	private Account lockMember(UUID memberId) {
		Account account = accounts.findByIdForUpdate(memberId).orElseThrow(CenterVisitException::memberNotFound);
		if (account.getRole() != AccountRole.MEMBER) throw CenterVisitException.memberNotFound();
		return account;
	}

	private void ensureActiveMember(UUID memberId) {
		if (!accounts.existsByIdAndRoleAndStatus(memberId, AccountRole.MEMBER, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void ensureActiveReceptionist(UUID receptionistId) {
		if (!accounts.existsByIdAndRoleAndStatus(receptionistId, AccountRole.RECEPTIONIST, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private String memberCode(UUID memberId) {
		return profiles.findById(memberId).map(profile -> profile.getMemberCode()).orElse(null);
	}
}
