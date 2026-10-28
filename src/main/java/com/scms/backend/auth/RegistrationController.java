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
import jakarta.servlet.http.HttpServletRequest;
import com.scms.backend.emailverification.EmailVerificationController;
import com.scms.backend.emailverification.EmailVerificationProperties;
import org.springframework.http.HttpHeaders;
import com.scms.backend.emailverification.RailwayClientIpResolver;

@RestController
@RequestMapping("/auth")
public class RegistrationController {

	private final RegistrationService registrationService;
	private final EmailVerificationController emailVerificationController;
	private final EmailVerificationProperties emailVerificationProperties;
	private final RailwayClientIpResolver clientIpResolver;

	RegistrationController(RegistrationService registrationService, EmailVerificationController emailVerificationController,
			EmailVerificationProperties emailVerificationProperties, RailwayClientIpResolver clientIpResolver) {
		this.registrationService = registrationService;
		this.emailVerificationController=emailVerificationController;
		this.emailVerificationProperties=emailVerificationProperties;
		this.clientIpResolver=clientIpResolver;
	}

	@PostMapping("/register")
	@SecurityRequirements
	@ResponseStatus(HttpStatus.CREATED)
	ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request,HttpServletRequest http) {
		RegistrationOutcome outcome=registrationService.register(request,clientIpResolver.resolve(http));
		var response=ResponseEntity.status(HttpStatus.CREATED);
		if(outcome.pendingSessionToken()!=null) response.header(HttpHeaders.SET_COOKIE,
			emailVerificationController.pendingCookie(outcome.pendingSessionToken(),emailVerificationProperties.getRegistrationTtl()).toString());
		return response.body(outcome.response());
	}
}
