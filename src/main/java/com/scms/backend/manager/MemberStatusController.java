package com.scms.backend.manager;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/members")
public class MemberStatusController {

	private final MemberStatusService service;

	MemberStatusController(MemberStatusService service) {
		this.service = service;
	}

	@PatchMapping("/{memberAccountId}/status")
	ResponseEntity<MemberStatusChangeResponse> changeStatus(@AuthenticationPrincipal Jwt jwt,
			@PathVariable UUID memberAccountId, @RequestBody MemberStatusChangeRequest request) {
		return ResponseEntity.ok(service.changeStatus(UUID.fromString(jwt.getSubject()), memberAccountId, request));
	}
}
