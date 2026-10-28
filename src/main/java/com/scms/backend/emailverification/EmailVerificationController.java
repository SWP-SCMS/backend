package com.scms.backend.emailverification;

import java.time.Duration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/auth/email-verification")
@SecurityRequirements
public class EmailVerificationController {
	public static final String COOKIE_NAME="scms_pending_verification";
	private final EmailVerificationService service; private final EmailVerificationProperties properties;
	private final PendingOriginPolicy originPolicy;
	private final RailwayClientIpResolver clientIpResolver;
	EmailVerificationController(EmailVerificationService service,EmailVerificationProperties properties,
			PendingOriginPolicy originPolicy,RailwayClientIpResolver clientIpResolver){this.service=service;this.properties=properties;this.originPolicy=originPolicy;this.clientIpResolver=clientIpResolver;}

	@PostMapping(value="/verify",consumes=MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<VerifyEmailResponse> verify(@Valid @RequestBody VerifyEmailRequest request,
			@CookieValue(name=COOKIE_NAME,required=false) String cookie,HttpServletRequest http){
		var result=service.verify(request,cookie,clientIpResolver.resolve(http)); var response=ResponseEntity.ok();
		if(result.clearCookie()) response.header(HttpHeaders.SET_COOKIE,clearCookie().toString()); return response.body(result.response());
	}

	@PostMapping(value="/resend",consumes=MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.ACCEPTED)
	ResponseEntity<ResendEmailResponse> resend(@Valid @RequestBody ResendEmailRequest request,HttpServletRequest http){
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.resend(request,clientIpResolver.resolve(http)));
	}

	@PostMapping(value="/change-email",consumes=MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<ChangePendingEmailResponse> change(@Valid @RequestBody ChangePendingEmailRequest request,
			@CookieValue(name=COOKIE_NAME,required=false) String cookie,
			@RequestHeader(name="X-Pending-CSRF",required=false) String csrf,
			@RequestHeader(name=HttpHeaders.ORIGIN,required=false) String origin,HttpServletRequest http){
		if(!originPolicy.allows(origin)) throw new EmailVerificationException(HttpStatus.FORBIDDEN,
			"INVALID_PENDING_ORIGIN","The request origin is not allowed.");
		var result=service.changeEmail(request,cookie,csrf,clientIpResolver.resolve(http));
		Duration remaining=Duration.between(result.response().serverTime(),result.response().registrationExpiresAt());
		return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,pendingCookie(result.sessionToken(),remaining).toString()).body(result.response());
	}

	public ResponseCookie pendingCookie(String token,Duration maxAge){return ResponseCookie.from(COOKIE_NAME,token)
		.httpOnly(true).secure(properties.isCookieSecure()).sameSite(properties.getCookieSameSite())
		.path("/api/v1/auth/email-verification").maxAge(maxAge).build();}
	private ResponseCookie clearCookie(){return ResponseCookie.from(COOKIE_NAME,"").httpOnly(true)
		.secure(properties.isCookieSecure()).sameSite(properties.getCookieSameSite())
		.path("/api/v1/auth/email-verification").maxAge(Duration.ZERO).build();}
}
