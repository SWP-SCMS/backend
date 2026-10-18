package com.scms.backend.membership;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class MembershipPlanAdminController {
	private final MembershipPlanAdminService service;

	MembershipPlanAdminController(MembershipPlanAdminService service) { this.service = service; }

	@GetMapping("/membership-benefits")
	ResponseEntity<List<MembershipBenefitResponse>> benefits(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.catalog(accountId(jwt)));
	}

	@GetMapping("/membership-plans")
	ResponseEntity<List<MembershipPlanResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(service.list(accountId(jwt)));
	}

	@GetMapping("/membership-plans/{planCode}")
	ResponseEntity<MembershipPlanResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String planCode) {
		return ResponseEntity.ok(service.get(accountId(jwt), planCode));
	}

	@PostMapping("/membership-plans")
	ResponseEntity<MembershipPlanResponse> create(@AuthenticationPrincipal Jwt jwt,
			@RequestBody MembershipPlanCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(service.create(accountId(jwt), request));
	}

	@PatchMapping("/membership-plans/{planCode}")
	ResponseEntity<MembershipPlanResponse> update(@AuthenticationPrincipal Jwt jwt, @PathVariable String planCode,
			@RequestBody MembershipPlanPatchRequest request) {
		return ResponseEntity.ok(service.update(accountId(jwt), planCode, request));
	}

	private UUID accountId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
