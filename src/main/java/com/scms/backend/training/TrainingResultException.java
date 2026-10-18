package com.scms.backend.training;

import org.springframework.http.HttpStatus;

public class TrainingResultException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private TrainingResultException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static TrainingResultException actorNotActiveCoach() {
		return new TrainingResultException(HttpStatus.FORBIDDEN, "ACTOR_NOT_ACTIVE_COACH",
				"This action requires an active Coach account.");
	}

	static TrainingResultException plusMembershipRequired() {
		return new TrainingResultException(HttpStatus.CONFLICT, "PLUS_MEMBERSHIP_REQUIRED",
				"Recording or editing a Training Result requires an active Membership with the TRAINING_RESULT Benefit.");
	}

	static TrainingResultException activeAssignmentNotFound() {
		return new TrainingResultException(HttpStatus.NOT_FOUND, "COACH_ASSIGNMENT_NOT_FOUND",
				"No active coaching assignment was found for this Member and Coach.");
	}

	static TrainingResultException planVersionNotFound() {
		return new TrainingResultException(HttpStatus.NOT_FOUND, "TRAINING_PLAN_VERSION_NOT_FOUND",
				"The Training Plan Version was not found for this Member.");
	}

	static TrainingResultException resultNotFound() {
		return new TrainingResultException(HttpStatus.NOT_FOUND, "TRAINING_RESULT_NOT_FOUND",
				"The Personal Training Result was not found for this Member.");
	}

	static TrainingResultException sessionNotFound() {
		return new TrainingResultException(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND",
				"The Group or Yoga Session was not found in this Teaching Coach's scope.");
	}

	static TrainingResultException sessionMemberNotFound() {
		return new TrainingResultException(HttpStatus.NOT_FOUND, "SESSION_MEMBER_NOT_FOUND",
				"The Member does not have a booked place in this Session.");
	}

	static TrainingResultException sessionResultNotFound() {
		return new TrainingResultException(HttpStatus.NOT_FOUND, "TRAINING_RESULT_NOT_FOUND",
				"The Session Training Result was not found for this Member.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
