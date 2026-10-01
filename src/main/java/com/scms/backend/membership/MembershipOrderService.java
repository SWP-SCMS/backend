package com.scms.backend.membership;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.account.MemberProfile;
import com.scms.backend.account.MemberProfileRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipOrderService {

	private static final Set<AccountStatus> ALL_MEMBER_STATUSES = Set.of(AccountStatus.values());

	private final AccountRepository accountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final MembershipOfferRepository membershipOfferRepository;
	private final MembershipRepository membershipRepository;
	private final MembershipOrderRepository membershipOrderRepository;

	MembershipOrderService(AccountRepository accountRepository,
			MemberProfileRepository memberProfileRepository,
			MembershipOfferRepository membershipOfferRepository,
			MembershipRepository membershipRepository,
			MembershipOrderRepository membershipOrderRepository) {
		this.accountRepository = accountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.membershipOfferRepository = membershipOfferRepository;
		this.membershipRepository = membershipRepository;
		this.membershipOrderRepository = membershipOrderRepository;
	}

	@Transactional
	MembershipOrderResponse createForMember(UUID actorAccountId, MembershipOrderCreateRequest request) {
		MembershipOffer offer = findActiveOffer(request.offerId());
		MemberProfile member = findSelfMember(actorAccountId);
		return create(actorAccountId, member, offer);
	}

	@Transactional
	MembershipOrderResponse createForReceptionist(UUID actorAccountId, String memberId,
			MembershipOrderCreateRequest request) {
		ensureActiveActor(actorAccountId, AccountRole.RECEPTIONIST);
		MembershipOffer offer = findActiveOffer(request.offerId());
		MemberProfile member = findTargetMember(memberId);
		return create(actorAccountId, member, offer);
	}

	@Transactional(readOnly = true)
	Optional<MembershipOrderResponse> resumeForMember(UUID actorAccountId) {
		MemberProfile member = findSelfMember(actorAccountId);
		return findPending(member);
	}

	@Transactional(readOnly = true)
	Optional<MembershipOrderResponse> resumeForReceptionist(UUID actorAccountId, String memberId) {
		ensureActiveActor(actorAccountId, AccountRole.RECEPTIONIST);
		return findPending(findTargetMember(memberId));
	}

	private MembershipOrderResponse create(UUID actorAccountId, MemberProfile member, MembershipOffer offer) {
		ensureActiveMember(member);
		UUID memberAccountId = member.getAccountId();
		if (membershipRepository.existsByMemberAccountIdAndStatus(memberAccountId, MembershipStatus.ACTIVE)) {
			throw new ActiveMembershipExistsException();
		}
		if (membershipOrderRepository.existsByMemberAccountIdAndStatus(memberAccountId,
				MembershipOrderStatus.PENDING_PAYMENT)) {
			throw new PendingMembershipOrderExistsException();
		}

		UUID orderId = UUID.randomUUID();
		String orderNumber = "ORD-" + orderId.toString().replace("-", "").toUpperCase(Locale.ROOT);
		MembershipOrder order = new MembershipOrder(orderId, orderNumber, memberAccountId, actorAccountId, offer);
		try {
			membershipOrderRepository.saveAndFlush(order);
		}
		catch (DataIntegrityViolationException exception) {
			if (MembershipOrderConstraintDetector.isPendingMemberConstraint(exception)) {
				throw new PendingMembershipOrderExistsException();
			}
			throw exception;
		}
		return toResponse(order, member.getMemberCode());
	}

	private Optional<MembershipOrderResponse> findPending(MemberProfile member) {
		ensureActiveMember(member);
		return membershipOrderRepository.findByMemberAccountIdAndStatus(member.getAccountId(),
			MembershipOrderStatus.PENDING_PAYMENT)
			.map(order -> toResponse(order, member.getMemberCode()));
	}

	private MembershipOffer findActiveOffer(UUID offerId) {
		return membershipOfferRepository.findByIdAndStatus(offerId, MembershipOfferStatus.ACTIVE)
			.orElseThrow(MembershipOfferNotFoundException::new);
	}

	private MemberProfile findSelfMember(UUID accountId) {
		MemberProfile member = memberProfileRepository.findById(accountId)
			.orElseThrow(InvalidAuthenticatedAccountException::new);
		Account account = member.getAccount();
		if (account.getRole() != AccountRole.MEMBER || account.getStatus() != AccountStatus.ACTIVE) {
			throw new InvalidAuthenticatedAccountException();
		}
		return member;
	}

	private MemberProfile findTargetMember(String memberId) {
		if (memberId == null || memberId.isBlank()) {
			throw new MembershipOrderMemberNotFoundException();
		}
		return memberProfileRepository.findVisibleMemberByCode(memberId.trim(), AccountRole.MEMBER,
			ALL_MEMBER_STATUSES).orElseThrow(MembershipOrderMemberNotFoundException::new);
	}

	private void ensureActiveActor(UUID accountId, AccountRole role) {
		if (!accountRepository.existsByIdAndRoleAndStatus(accountId, role, AccountStatus.ACTIVE)) {
			throw new InvalidAuthenticatedAccountException();
		}
	}

	private void ensureActiveMember(MemberProfile member) {
		if (member.getAccount().getStatus() != AccountStatus.ACTIVE) {
			throw new MemberNotActiveException();
		}
	}

	private MembershipOrderResponse toResponse(MembershipOrder order, String memberId) {
		return new MembershipOrderResponse(order.getId(), order.getOrderNumber(), memberId, order.getOfferId(),
			order.getOfferNameSnapshot(), order.getPlanCodeSnapshot(), order.getPriceAmountSnapshot(),
			order.getCurrencyCodeSnapshot(), order.getDurationDaysSnapshot(), order.getPaymentMethod(),
			order.getStatus(), order.getCreatedAt());
	}
}
