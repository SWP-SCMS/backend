package com.scms.backend.auth;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class RegistrationController {

	private final RegistrationService registrationService;

	RegistrationController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping("/register")
	@SecurityRequirements
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(registrationService.register(request));
	}
}
