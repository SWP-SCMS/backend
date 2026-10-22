package com.scms.backend.emailverification;

import java.util.UUID;

import com.scms.backend.auth.RefreshTokenCookieService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/me/email-change")
public class AccountEmailChangeController {
	private final AccountEmailChangeService service;
	private final RefreshTokenCookieService cookies;

	AccountEmailChangeController(AccountEmailChangeService service, RefreshTokenCookieService cookies) {
		this.service = service; this.cookies = cookies;
	}

	@GetMapping
	AccountEmailChangeStatus status(@AuthenticationPrincipal Jwt jwt) {
		return service.status(id(jwt));
	}

	@PostMapping
	ResponseEntity<AccountEmailChangeResponse> begin(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AccountEmailChangeRequest request, HttpServletRequest http) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.begin(id(jwt), request, http.getRemoteAddr()));
	}

	@PutMapping
	ResponseEntity<AccountEmailChangeResponse> replace(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AccountEmailChangeRequest request, HttpServletRequest http) {
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.replace(id(jwt), request, http.getRemoteAddr()));
	}

	@PostMapping("/verify")
	ResponseEntity<AccountEmailChangeVerifyResponse> verify(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody AccountEmailChangeVerifyRequest request) {
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
			.body(service.verify(id(jwt), request));
	}

	@PostMapping("/resend")
	ResponseEntity<AccountEmailChangeResponse> resend(@AuthenticationPrincipal Jwt jwt) {
		return ResponseEntity.accepted().body(service.resend(id(jwt)));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt) {
		service.cancel(id(jwt)); return ResponseEntity.noContent().build();
	}

	private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
