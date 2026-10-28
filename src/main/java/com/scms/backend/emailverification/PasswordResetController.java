package com.scms.backend.emailverification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/password-reset")
@SecurityRequirements
public class PasswordResetController {
	private final PasswordResetService service;
	private final PasswordResetCookieService cookies;
	private final RailwayClientIpResolver clientIpResolver;

	PasswordResetController(PasswordResetService service, PasswordResetCookieService cookies,
			RailwayClientIpResolver clientIpResolver) {
		this.service = service;
		this.cookies = cookies;
		this.clientIpResolver = clientIpResolver;
	}

	@PostMapping(value = "/request", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	PasswordResetAcceptedResponse request(@Valid @RequestBody PasswordResetRequest request,
			HttpServletRequest servletRequest) {
		return service.request(request, clientIpResolver.resolve(servletRequest));
	}

	@PostMapping(value = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<PasswordResetVerifiedResponse> verify(@Valid @RequestBody PasswordResetVerifyRequest request,
			HttpServletRequest servletRequest) {
		PasswordResetService.VerifyResult result = service.verify(request, clientIpResolver.resolve(servletRequest));
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookies.create(result.sessionToken()).toString())
			.body(result.response());
	}

	@PostMapping(value = "/confirm", consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	ResponseEntity<Void> confirm(@RequestBody PasswordResetConfirmRequest request,
			@CookieValue(name = PasswordResetCookieService.COOKIE_NAME, required = false) String session) {
		service.confirm(request, session);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
			.build();
	}
}
