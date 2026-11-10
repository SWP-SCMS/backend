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

	public static TrainingFeedbackException actorNotActiveCoach() {
		return new TrainingFeedbackException(HttpStatus.FORBIDDEN, "ACTOR_NOT_ACTIVE_COACH", "This action requires an active Coach account.");
	}

	public static TrainingFeedbackException plusMembershipRequired() {
		return new TrainingFeedbackException(HttpStatus.CONFLICT, "PLUS_MEMBERSHIP_REQUIRED", "Creating or editing Feedback requires an active Membership with the TRAINING_FEEDBACK Benefit.");
	}

	public static TrainingFeedbackException activeAssignmentNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "COACH_ASSIGNMENT_NOT_FOUND", "No active coaching assignment was found for this Member and Coach.");
	}

	public static TrainingFeedbackException planVersionNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_PLAN_VERSION_NOT_FOUND", "The Training Plan Version was not found for this Member.");
	}

	public static TrainingFeedbackException resultNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_RESULT_NOT_FOUND", "The Personal Training Result was not found for this Member.");
	}

	public static TrainingFeedbackException feedbackNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_FEEDBACK_NOT_FOUND", "The Personal Training Feedback was not found for this Member.");
	}

	public static TrainingFeedbackException sessionNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "The Group or Yoga Session was not found in this Teaching Coach's scope.");
	}

	public static TrainingFeedbackException sessionResultNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_RESULT_NOT_FOUND", "The Session Training Result was not found for this Member.");
	}

	public static TrainingFeedbackException sessionFeedbackNotFound() {
		return new TrainingFeedbackException(HttpStatus.NOT_FOUND, "TRAINING_FEEDBACK_NOT_FOUND", "The Session Training Feedback was not found for this Member and Session.");
	}

	public static TrainingFeedbackException sessionNotEnded() {
		return new TrainingFeedbackException(HttpStatus.CONFLICT, "SESSION_TRAINING_NOT_AVAILABLE", "Results and feedback can only be recorded after a non-cancelled session ends.");
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
