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
	private final AccountEmailChangeService service;private final RefreshTokenCookieService cookies;
	private final RailwayClientIpResolver clientIpResolver;
	StaffEmailChangeController(AccountEmailChangeService service,RefreshTokenCookieService cookies,RailwayClientIpResolver clientIpResolver){this.service=service;this.cookies=cookies;this.clientIpResolver=clientIpResolver;}
	@PostMapping ResponseEntity<StaffEmailChangeResponse> begin(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody StaffEmailChangeRequest request,jakarta.servlet.http.HttpServletRequest http){var result=service.begin(id(jwt),new AccountEmailChangeRequest(request.newEmail(),request.currentPassword()),clientIpResolver.resolve(http));return ResponseEntity.status(HttpStatus.ACCEPTED).body(response(result));}
	@PostMapping("/verify") ResponseEntity<StaffEmailChangeVerifyResponse> verify(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody StaffEmailChangeVerifyRequest request){var result=service.verify(id(jwt),new AccountEmailChangeVerifyRequest(request.otp()));return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookies.clear().toString()).body(new StaffEmailChangeVerifyResponse(result.verified(),result.email()));}
	@PostMapping("/resend") ResponseEntity<StaffEmailChangeResponse> resend(@AuthenticationPrincipal Jwt jwt){return ResponseEntity.accepted().body(response(service.resend(id(jwt))));}
	@DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT) ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt){service.cancel(id(jwt));return ResponseEntity.noContent().build();}
	private StaffEmailChangeResponse response(AccountEmailChangeResponse value){return new StaffEmailChangeResponse(value.pending(),value.maskedEmail(),value.otpExpiresAt(),value.resendAvailableAt(),value.serverTime());}
	private UUID id(Jwt jwt){return UUID.fromString(jwt.getSubject());}
}
