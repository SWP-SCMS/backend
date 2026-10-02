package com.scms.backend.membership;

import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipOfferAdminService {
	private final MembershipOfferRepository offers;
	private final MembershipPlanRepository plans;
	private final AccountRepository accounts;

	MembershipOfferAdminService(MembershipOfferRepository offers, MembershipPlanRepository plans, AccountRepository accounts) {
		this.offers = offers;
		this.plans = plans;
		this.accounts = accounts;
	}

	@Transactional
	MembershipOfferResponse create(UUID managerId, MembershipOfferAdminRequest request) {
		ensureManager(managerId); validate(request, true);
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plans.findById(request.planCode()).orElseThrow(),
			request.name().trim(), request.description().trim(), request.priceAmount(), "VND", request.durationDays(),
			request.status() == null ? MembershipOfferStatus.ACTIVE : request.status(), managerId);
		return MembershipOfferResponse.from(offers.save(offer));
	}

	@Transactional(readOnly = true)
	java.util.List<MembershipOfferResponse> all(UUID managerId) { ensureManager(managerId); return offers.findAll().stream().map(MembershipOfferResponse::from).toList(); }

	@Transactional
	MembershipOfferResponse update(UUID managerId, UUID id, MembershipOfferAdminRequest request) {
		ensureManager(managerId); validate(request, false);
		MembershipOffer offer = offers.findById(id).orElseThrow(MembershipOfferNotFoundException::new);
		offer.update(request.name().trim(), request.description().trim(), request.priceAmount(), request.durationDays());
		if (request.status() != null) offer.changeStatus(request.status());
		return MembershipOfferResponse.from(offer);
	}

	private void ensureManager(UUID id) { if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException(); }
	private void validate(MembershipOfferAdminRequest r, boolean create) {
		if (r == null || (create && r.planCode() == null) || r.name() == null || r.name().trim().isEmpty()
				|| r.description() == null || r.description().trim().isEmpty() || r.priceAmount() == null || r.priceAmount().signum() <= 0
				|| r.durationDays() == null || r.durationDays() <= 0) throw new IllegalArgumentException("Invalid membership offer");
	}
}
