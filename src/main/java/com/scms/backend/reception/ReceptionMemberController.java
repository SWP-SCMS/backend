package com.scms.backend.reception;

import java.util.UUID;

import com.scms.backend.auth.RegistrationResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reception/members")
public class ReceptionMemberController {

	private final ReceptionMemberService service;

	ReceptionMemberController(ReceptionMemberService service) {
		this.service = service;
	}

	@PostMapping
	ResponseEntity<RegistrationResponse> createMember(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ReceptionMemberCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(service.createMember(UUID.fromString(jwt.getSubject()), request));
	}
}
