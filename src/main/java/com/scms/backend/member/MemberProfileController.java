package com.scms.backend.member;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members/me/profile")
public class MemberProfileController {

	private final MemberProfileService memberProfileService;

	MemberProfileController(MemberProfileService memberProfileService) {
		this.memberProfileService = memberProfileService;
	}

	@GetMapping
	ResponseEntity<MemberProfileResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.ok(memberProfileService.getProfile(accountId(jwt)));
	}

	@PatchMapping
	ResponseEntity<MemberProfileResponse> updateProfile(@AuthenticationPrincipal Jwt jwt,
			@RequestBody MemberProfilePatchRequest request) {
		if (request == null) {
			throw new MemberProfileValidationException("request", "must be a JSON object");
		}
		return ResponseEntity.ok(memberProfileService.updateProfile(accountId(jwt), request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
