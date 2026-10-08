package com.scms.backend.membership;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipOfferAdminService {
	private final MembershipOfferRepository offers;
	private final MembershipPlanRepository plans;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;

	MembershipOfferAdminService(MembershipOfferRepository offers, MembershipPlanRepository plans,
			AccountRepository accounts, AuditEventRepository audits) {
		this.offers = offers;
		this.plans = plans;
		this.accounts = accounts;
		this.audits = audits;
	}

	@Transactional
	MembershipOfferAdminResponse create(UUID managerId, MembershipOfferAdminRequest request) {
		ensureManager(managerId); validate(request, true);
		MembershipOffer offer = new MembershipOffer(UUID.randomUUID(), plans.findById(request.planCode()).orElseThrow(),
			request.name().trim(), request.description().trim(), request.priceAmount(), "VND", request.durationDays(),
			request.status() == null ? MembershipOfferStatus.ACTIVE : request.status(), managerId);
		offers.save(offer);
		audits.save(new AuditEvent(UUID.randomUUID(), managerId, "MEMBERSHIP_OFFER_CREATED", "MEMBERSHIP_OFFER",
			offer.getId(), snapshot(offer)));
		return MembershipOfferAdminResponse.from(offer);
	}

	@Transactional(readOnly = true)
	java.util.List<MembershipOfferAdminResponse> all(UUID managerId) { ensureManager(managerId); return offers.findAll().stream().map(MembershipOfferAdminResponse::from).toList(); }

	@Transactional
	MembershipOfferAdminResponse update(UUID managerId, UUID id, MembershipOfferPatchRequest request) {
		ensureManager(managerId); validatePatch(request);
		MembershipOffer offer = offers.findById(id).orElseThrow(MembershipOfferNotFoundException::new);
		Map<String, Object> before = snapshot(offer);
		offer.patch(trim(request.name()), trim(request.description()), request.priceAmount(), request.durationDays());
		if (request.status() != null) offer.changeStatus(request.status());
		Map<String, Object> after = snapshot(offer);
		if (!before.equals(after)) {
			audits.save(new AuditEvent(UUID.randomUUID(), managerId, "MEMBERSHIP_OFFER_UPDATED", "MEMBERSHIP_OFFER",
				id, null, before, after));
		}
		return MembershipOfferAdminResponse.from(offer);
	}

	private Map<String, Object> snapshot(MembershipOffer offer) {
		Map<String, Object> data = new HashMap<>();
		data.put("planCode", offer.getPlan().getPlanCode().name());
		data.put("name", offer.getName());
		data.put("description", offer.getDescription());
		data.put("priceAmount", offer.getPriceAmount());
		data.put("currencyCode", offer.getCurrencyCode());
		data.put("durationDays", offer.getDurationDays());
		data.put("status", offer.getStatus().name());
		data.put("createdByAccountId", offer.getCreatedByAccountId().toString());
		return data;
	}

	private void ensureManager(UUID id) { if (!accounts.existsByIdAndRoleAndStatus(id, AccountRole.MANAGER, AccountStatus.ACTIVE)) throw new InvalidAuthenticatedAccountException(); }
	private void validate(MembershipOfferAdminRequest r, boolean create) {
		if (r == null || (create && r.planCode() == null) || r.name() == null || r.name().trim().isEmpty()
				|| r.description() == null || r.description().trim().isEmpty() || r.priceAmount() == null || r.priceAmount().signum() <= 0
				|| r.durationDays() == null || r.durationDays() <= 0) throw new MembershipOfferValidationException("Invalid membership offer");
	}
	private void validatePatch(MembershipOfferPatchRequest r) {
		if (r == null || (r.planCode() == null && r.name() == null && r.description() == null && r.priceAmount() == null
				&& r.durationDays() == null && r.status() == null)) throw new MembershipOfferValidationException("At least one field is required");
		if (r.planCode() != null) throw new MembershipOfferValidationException("planCode cannot be changed");
		if (r.name() != null && r.name().isBlank()) throw new MembershipOfferValidationException("name must not be blank");
		if (r.description() != null && r.description().isBlank()) throw new MembershipOfferValidationException("description must not be blank");
		if (r.priceAmount() != null && r.priceAmount().signum() <= 0) throw new MembershipOfferValidationException("priceAmount must be positive");
		if (r.durationDays() != null && r.durationDays() <= 0) throw new MembershipOfferValidationException("durationDays must be positive");
	}
	private String trim(String value) { return value == null ? null : value.trim(); }
}
