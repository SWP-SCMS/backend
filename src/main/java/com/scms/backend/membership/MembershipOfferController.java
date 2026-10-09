package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@RestController
@RequestMapping("/membership-offers")
public class MembershipOfferController {

	private final MembershipOfferService service;

	MembershipOfferController(MembershipOfferService service) {
		this.service = service;
	}

	@GetMapping
	@SecurityRequirements
	ResponseEntity<List<MembershipOfferResponse>> listActiveOffers(
			@RequestParam(name = "planCode", required = false) String planCode) {
		return ResponseEntity.ok(service.listActiveOffers(planCode));
	}

	@GetMapping("/{offerId}")
	@SecurityRequirements
	ResponseEntity<MembershipOfferResponse> getActiveOffer(@PathVariable UUID offerId) {
		return ResponseEntity.ok(service.getActiveOffer(offerId));
	}
}
