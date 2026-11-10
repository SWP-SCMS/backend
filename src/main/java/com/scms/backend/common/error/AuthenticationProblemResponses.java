package com.scms.backend.common.error;

import com.scms.backend.auth.ChangePasswordValidationException;
import com.scms.backend.auth.DuplicateAccountException;
import com.scms.backend.auth.RegistrationValidationException;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.PasswordResetException;
import com.scms.backend.emailverification.PendingRegistrationException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

final class AuthenticationProblemResponses {

	static ProblemDetail handleInvalidCredentials() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid credentials");
		problem.setTitle("Authentication failed");
		problem.setProperty("code", "INVALID_CREDENTIALS");
		return problem;
	}

	static ProblemDetail handleInvalidRefreshToken() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "The refresh token is invalid or expired.");
		problem.setTitle("Refresh failed");
		problem.setProperty("code", "INVALID_REFRESH_TOKEN");
		return problem;
	}

	static ProblemDetail handleInvalidAuthenticatedAccount() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "The authenticated account is no longer active.");
		problem.setTitle("Authentication failed");
		problem.setProperty("code", "INVALID_TOKEN");
		return problem;
	}

	static ProblemDetail handleCurrentPasswordIncorrect() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The current password is incorrect.");
		problem.setTitle("Password change failed");
		problem.setProperty("code", "CURRENT_PASSWORD_INCORRECT");
		return problem;
	}

	static ProblemDetail handleChangePasswordValidation(ChangePasswordValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleNewPasswordSameAsCurrent() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The new password must differ from the current password.");
		problem.setTitle("Password change failed");
		problem.setProperty("code", "NEW_PASSWORD_SAME_AS_CURRENT");
		return problem;
	}

	static ProblemDetail handleRegistrationValidation(RegistrationValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleDuplicateAccount(DuplicateAccountException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Account conflict");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handlePendingRegistration(PendingRegistrationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Email verification pending");
		problem.setProperty("code", "EMAIL_VERIFICATION_PENDING");
		problem.setProperty("otpExpiresAt", exception.getOtpExpiresAt());
		problem.setProperty("resendAvailableAt", exception.getResendAvailableAt());
		problem.setProperty("registrationExpiresAt", exception.getRegistrationExpiresAt());
		problem.setProperty("serverTime", exception.getServerTime());
		return problem;
	}

	static ResponseEntity<ProblemDetail> handleEmailVerification(EmailVerificationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Email verification failed");
		problem.setProperty("code", exception.getCode());
		ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
		if (exception.getRetryAfter() != null)
			response.header(HttpHeaders.RETRY_AFTER, exception.getRetryAfter().toString());
		return response.contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
	}

	static ProblemDetail handlePasswordReset(PasswordResetException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Password reset failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getField() != null)
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getFieldMessage())));
		return problem;
	}
}
