package com.scms.backend.membership;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/manager/membership-offers")
class MembershipOfferAdminController {
	private final MembershipOfferAdminService service;
	MembershipOfferAdminController(MembershipOfferAdminService service) { this.service = service; }
	@GetMapping ResponseEntity<?> all(@AuthenticationPrincipal Jwt jwt) { return ResponseEntity.ok(service.all(UUID.fromString(jwt.getSubject()))); }
	@PostMapping ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt, @RequestBody MembershipOfferAdminRequest r) { return ResponseEntity.status(201).body(service.create(UUID.fromString(jwt.getSubject()), r)); }
	@PatchMapping("/{id}") ResponseEntity<?> update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody MembershipOfferPatchRequest r) { return ResponseEntity.ok(service.update(UUID.fromString(jwt.getSubject()), id, r)); }
}
