package com.scms.backend.common.error;

import com.scms.backend.audit.AuditHistoryValidationException;
import com.scms.backend.notification.NotificationValidationException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

final class OperationsProblemResponses {

	static ProblemDetail handleNotificationNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested notification was not found.");
		problem.setTitle("Notification not found");
		problem.setProperty("code", "NOTIFICATION_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleNotificationValidation(NotificationValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleAuditHistoryValidation(AuditHistoryValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}
}
