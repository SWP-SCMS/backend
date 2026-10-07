package com.scms.backend.reception;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CenterVisitController {

	private final CenterVisitService service;

	CenterVisitController(CenterVisitService service) {
		this.service = service;
	}

	@PostMapping("/reception/center-visits")
	ResponseEntity<CenterVisitResponse> checkIn(@AuthenticationPrincipal Jwt jwt,
			@RequestBody CenterVisitRequest request,
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		CenterVisitResponse response = service.checkIn(accountId(jwt), request, idempotencyKey);
		return ResponseEntity.status(response.created() ? 201 : 200).body(response);
	}

	@GetMapping("/members/me/center-visits/current")
	ResponseEntity<CenterVisitResponse> current(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.currentMember(accountId(jwt)));
	}

	@PatchMapping("/members/me/center-visits/current/checkout")
	ResponseEntity<CenterVisitResponse> checkoutMember(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.checkoutMember(accountId(jwt)));
	}

	@PatchMapping("/reception/members/{memberAccountId}/center-visits/current/checkout")
	ResponseEntity<CenterVisitResponse> checkoutReceptionist(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberAccountId) {
		return ResponseEntity.ok(service.checkoutReceptionist(accountId(jwt), memberAccountId));
	}

	@GetMapping("/reception/center-visits/open")
	ResponseEntity<CenterVisitPageResponse> open(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > 100) throw CenterVisitException.invalidPage();
		var pageable = PageRequest.of(page, size,
			Sort.by("checkedInAt").ascending().and(Sort.by("id")));
		return ResponseEntity.ok(service.openVisits(accountId(jwt), pageable));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
