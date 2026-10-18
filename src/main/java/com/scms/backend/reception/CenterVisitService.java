package com.scms.backend.reception;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
		var replay = findReplay(receptionistId, key, member);
		if (replay.isPresent()) return replay.get();
		if (member.status() != AccountStatus.ACTIVE) throw CenterVisitException.memberNotActive();
		if (member.profileImageUrl() == null || member.profileImageUrl().isBlank()) {
			throw CenterVisitException.profileImageRequired();
		}
		Account lockedMember = lockMember(member.accountId());
		replay = findReplay(receptionistId, key, member);
		if (replay.isPresent()) return replay.get();
		if (lockedMember.getStatus() != AccountStatus.ACTIVE) {
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
		return CenterVisitResponse.from(visit, member.memberId(), member.fullName(), true);
	}

	private Optional<CenterVisitResponse> findReplay(UUID receptionistId, String key,
			ReceptionMemberSearchResponse member) {
		if (key == null) return Optional.empty();
		return visits.findByCheckedInByAccountIdAndIdempotencyKey(receptionistId, key).map(visit -> {
			if (!visit.getMemberAccountId().equals(member.accountId())) {
				throw CenterVisitException.idempotencyKeyReused();
			}
			return CenterVisitResponse.from(visit, member.memberId(), member.fullName(), false);
		});
	}

	@Transactional(readOnly = true)
	CenterVisitResponse currentMember(UUID memberId) {
		ensureActiveMember(memberId);
		CenterVisit visit = visits
			.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId)
			.orElseThrow(CenterVisitException::openVisitNotFound);
		String memberName = accounts.findById(memberId).map(Account::getFullName).orElse(null);
		return CenterVisitResponse.from(visit, memberCode(memberId), memberName, false);
	}

	@Transactional
	CenterVisitResponse checkoutMember(UUID memberId) {
		Account member = lockMember(memberId);
		if (member.getStatus() != AccountStatus.ACTIVE) {
			throw new InvalidAuthenticatedAccountException();
		}
		return checkout(memberId, memberId, CheckoutSource.MEMBER, member.getFullName());
	}

	@Transactional
	CenterVisitResponse checkoutReceptionist(UUID receptionistId, UUID memberId) {
		ensureActiveReceptionist(receptionistId);
		Account member = lockMember(memberId);
		return checkout(memberId, receptionistId, CheckoutSource.RECEPTIONIST, member.getFullName());
	}

	@Transactional(readOnly = true)
	CenterVisitPageResponse openVisits(UUID receptionistId, Pageable pageable) {
		ensureActiveReceptionist(receptionistId);
		var page = visits.findByCheckedOutAtIsNull(pageable);
		var memberIds = page.getContent().stream().map(CenterVisit::getMemberAccountId)
			.collect(Collectors.toSet());
		var accountsById = accounts.findAllById(memberIds).stream()
			.collect(Collectors.toMap(Account::getId, account -> account));
		var profilesById = profiles.findAllById(memberIds).stream()
			.collect(Collectors.toMap(profile -> profile.getAccountId(), profile -> profile));
		var content = page.getContent().stream()
			.map(visit -> {
				UUID memberId = visit.getMemberAccountId();
				var account = accountsById.get(memberId);
				var profile = profilesById.get(memberId);
				return CenterVisitResponse.from(visit, profile == null ? null : profile.getMemberCode(),
					account == null ? null : account.getFullName(), false);
			}).toList();
		return new CenterVisitPageResponse(content, page.getNumber(), page.getSize(), page.getTotalElements(),
			page.getTotalPages());
	}

	private CenterVisitResponse checkout(UUID memberId, UUID actorId, CheckoutSource source, String memberName) {
		var open = visits.findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc(memberId);
		if (open.isEmpty()) {
			CenterVisit replay = visits
				.findFirstByMemberAccountIdAndCheckedOutAtIsNotNullOrderByCheckedOutAtDescIdDesc(memberId)
				.orElseThrow(CenterVisitException::openVisitNotFound);
			return CenterVisitResponse.from(replay, memberCode(memberId), memberName, false);
		}
		CenterVisit visit = open.get();
		close(visit, actorId, source, clock.instant());
		visits.save(visit);
		return CenterVisitResponse.from(visit, memberCode(memberId), memberName, false);
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
