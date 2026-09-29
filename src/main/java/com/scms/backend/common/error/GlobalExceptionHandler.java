package com.scms.backend.common.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.scms.backend.auth.DuplicateAccountException;
import com.scms.backend.auth.InvalidCredentialsException;
import com.scms.backend.auth.InvalidRefreshTokenException;
import com.scms.backend.auth.RegistrationValidationException;
import com.scms.backend.member.MemberProfileValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.computeIfAbsent(fieldError.getField(), ignored -> new ArrayList<>())
				.add(fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage());
		}

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"The request body is missing or malformed.");
		problem.setTitle("Malformed request");
		problem.setProperty("code", "MALFORMED_REQUEST");
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
			"Invalid credentials");
		problem.setTitle("Authentication failed");
		problem.setProperty("code", "INVALID_CREDENTIALS");
		return problem;
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ProblemDetail handleInvalidRefreshToken() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
			"The refresh token is invalid or expired.");
		problem.setTitle("Refresh failed");
		problem.setProperty("code", "INVALID_REFRESH_TOKEN");
		return problem;
	}

	@ExceptionHandler(RegistrationValidationException.class)
	ProblemDetail handleRegistrationValidation(RegistrationValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(DuplicateAccountException.class)
	ProblemDetail handleDuplicateAccount(DuplicateAccountException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Account conflict");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	@ExceptionHandler(MemberProfileValidationException.class)
	ProblemDetail handleMemberProfileValidation(MemberProfileValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		logger.error("Unhandled request failure", exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
			"An unexpected error occurred.");
		problem.setTitle("Internal server error");
		problem.setProperty("code", "INTERNAL_ERROR");
		return problem;
	}
}
