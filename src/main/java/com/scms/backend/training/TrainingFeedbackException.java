package com.scms.backend.training;

import org.springframework.http.HttpStatus;

public class TrainingFeedbackException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private TrainingFeedbackException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static TrainingFeedbackException actorNotActiveCoach() {
		return new TrainingFeedbackException(HttpStatus.FORBIDDEN, "ACTOR_NOT_ACTIVE_COACH",
				"This action requires an active Coach account.");
	}

	static TrainingFeedbackException plusMembershipRequired() {
		return new TrainingFeedbackException(HttpStatus.CONFLICT, "PLUS_MEMBERSHIP_REQUIRED",
				"Creating or editing Feedback requires an active PLUS Membership.");
	}

	static TrainingFeedbackException activeAssignmentNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "COACH_ASSIGNMENT_NOT_FOUND",
				"No active coaching assignment was found for this Member and Coach.");
	}

	static TrainingFeedbackException planVersionNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_PLAN_VERSION_NOT_FOUND",
				"The Training Plan Version was not found for this Member.");
	}

	static TrainingFeedbackException resultNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_RESULT_NOT_FOUND",
				"The Personal Training Result was not found for this Member.");
	}

	static TrainingFeedbackException feedbackNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_FEEDBACK_NOT_FOUND",
				"The Personal Training Feedback was not found for this Member.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
