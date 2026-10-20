package com.scms.backend.emailverification;

import org.springframework.http.HttpStatus;

public class EmailVerificationException extends RuntimeException {
	private final HttpStatus status; private final String code; private final Long retryAfter;
	EmailVerificationException(HttpStatus status, String code, String message) { this(status,code,message,null); }
	EmailVerificationException(HttpStatus status, String code, String message, Long retryAfter) {
		super(message); this.status=status; this.code=code; this.retryAfter=retryAfter;
	}
	public HttpStatus getStatus(){return status;} public String getCode(){return code;} public Long getRetryAfter(){return retryAfter;}
}
