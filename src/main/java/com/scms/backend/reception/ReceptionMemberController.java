package com.scms.backend.reception;

import java.util.UUID;

import com.scms.backend.auth.RegistrationResponse;
import com.scms.backend.member.MemberProfilePatchRequest;
import com.scms.backend.member.MemberProfileResponse;
import jakarta.validation.Valid;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reception/members")
public class ReceptionMemberController {

	private final ReceptionMemberService service;
	private final ReceptionMemberProfileService profileService;

	ReceptionMemberController(ReceptionMemberService service, ReceptionMemberProfileService profileService) {
		this.service = service;
		this.profileService = profileService;
	}

	@PostMapping
	ResponseEntity<RegistrationResponse> createMember(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ReceptionMemberCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.createMember(UUID.fromString(jwt.getSubject()), request));
	}

	@GetMapping("/search")
	ResponseEntity<ReceptionMemberSearchResponse> searchMembers(@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String memberId,
			@RequestParam(required = false) String phone) {
		return ResponseEntity.ok(profileService.search(accountId(jwt), memberId, phone));
	}

	@GetMapping("/{memberId}/profile")
	ResponseEntity<MemberProfileResponse> getMemberProfile(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId) {
		return ResponseEntity.ok(profileService.getProfile(accountId(jwt), memberId));
	}

	@PatchMapping("/{memberId}/profile")
	ResponseEntity<MemberProfileResponse> updateMemberProfile(@AuthenticationPrincipal Jwt jwt,
			@PathVariable String memberId,
			@RequestBody MemberProfilePatchRequest request) {
		if (request == null) {
			throw new ReceptionMemberValidationException("request", "must be a JSON object");
		}
		return ResponseEntity.ok(profileService.updateProfile(accountId(jwt), memberId, request));
	}

	private UUID accountId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
