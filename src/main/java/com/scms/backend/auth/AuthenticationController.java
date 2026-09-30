package com.scms.backend.auth;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

	private final AuthenticationService authenticationService;
	private final RefreshTokenCookieService cookieService;

	AuthenticationController(AuthenticationService authenticationService, RefreshTokenCookieService cookieService) {
		this.authenticationService = authenticationService;
		this.cookieService = cookieService;
	}

	@PostMapping("/login")
	ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthSession session = authenticationService.login(request);
		return withRefreshCookie(session);
	}

	@PostMapping("/refresh")
	ResponseEntity<AuthResponse> refresh(
			@CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
		AuthSession session = authenticationService.refresh(refreshToken);
		return withRefreshCookie(session);
	}

	@PostMapping("/logout")
	ResponseEntity<Void> logout(
			@CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
		authenticationService.logout(refreshToken);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
			.build();
	}

	@PostMapping("/change-password")
	ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
			@RequestBody ChangePasswordRequest request,
			@CookieValue(name = RefreshTokenCookieService.COOKIE_NAME, required = false) String refreshToken) {
		authenticationService.changePassword(UUID.fromString(jwt.getSubject()), request, refreshToken);
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
			.build();
	}

	private ResponseEntity<AuthResponse> withRefreshCookie(AuthSession session) {
		return ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, cookieService.create(session.refreshToken()).toString())
			.body(session.response());
	}
}
