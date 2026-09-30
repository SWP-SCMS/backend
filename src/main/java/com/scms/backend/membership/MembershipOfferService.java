package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipOfferService {

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
			MembershipPlanCode planCode = parsePlanCode(requestedPlanCode);
			offers = repository.findAllByStatusAndPlanCode(MembershipOfferStatus.ACTIVE, planCode);
		}
		return offers.stream().map(MembershipOfferResponse::from).toList();
	}

	@Transactional(readOnly = true)
	MembershipOfferResponse getActiveOffer(UUID offerId) {
		return repository.findByIdAndStatus(offerId, MembershipOfferStatus.ACTIVE)
			.map(MembershipOfferResponse::from)
			.orElseThrow(MembershipOfferNotFoundException::new);
	}

	private MembershipPlanCode parsePlanCode(String requestedPlanCode) {
		try {
			return MembershipPlanCode.valueOf(requestedPlanCode);
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidPlanCodeException();
		}
	}
}
