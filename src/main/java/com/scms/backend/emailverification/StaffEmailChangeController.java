package com.scms.backend.emailverification;

import java.util.UUID;

import com.scms.backend.auth.RefreshTokenCookieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/staff/me/email-change")
public class StaffEmailChangeController {
	private final StaffEmailChangeService service;private final RefreshTokenCookieService cookies;
	StaffEmailChangeController(StaffEmailChangeService service,RefreshTokenCookieService cookies){this.service=service;this.cookies=cookies;}
	@PostMapping ResponseEntity<StaffEmailChangeResponse> begin(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody StaffEmailChangeRequest request){return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.begin(id(jwt),request));}
	@PostMapping("/verify") ResponseEntity<StaffEmailChangeVerifyResponse> verify(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody StaffEmailChangeVerifyRequest request){return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookies.clear().toString()).body(service.verify(id(jwt),request));}
	@PostMapping("/resend") ResponseEntity<StaffEmailChangeResponse> resend(@AuthenticationPrincipal Jwt jwt){return ResponseEntity.accepted().body(service.resend(id(jwt)));}
	@DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt){service.cancel(id(jwt));return ResponseEntity.noContent().build();}
	private UUID id(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
