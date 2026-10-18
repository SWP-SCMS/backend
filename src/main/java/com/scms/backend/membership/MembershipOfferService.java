package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipOfferService {
	private static final Pattern PLAN_CODE = Pattern.compile("[A-Z][A-Z0-9_]{1,49}");

	private final MembershipOfferRepository repository;

	MembershipOfferService(MembershipOfferRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	List<MembershipOfferResponse> listActiveOffers(String requestedPlanCode) {
		List<MembershipOffer> offers;
		if (requestedPlanCode == null) {
			offers = repository.findAllByStatus(MembershipOfferStatus.ACTIVE);
		}
		else {
			offers = repository.findAllByStatusAndPlanCode(MembershipOfferStatus.ACTIVE, parsePlanCode(requestedPlanCode));
		}
		return offers.stream().map(MembershipOfferResponse::from).toList();
	}

	@Transactional(readOnly = true)
	MembershipOfferResponse getActiveOffer(UUID offerId) {
		return repository.findByIdAndStatusAndActivePlan(offerId, MembershipOfferStatus.ACTIVE)
			.map(MembershipOfferResponse::from)
			.orElseThrow(MembershipOfferNotFoundException::new);
	}

	private String parsePlanCode(String requestedPlanCode) {
		if (!PLAN_CODE.matcher(requestedPlanCode).matches()) throw new InvalidPlanCodeException();
		return requestedPlanCode;
	}
}
