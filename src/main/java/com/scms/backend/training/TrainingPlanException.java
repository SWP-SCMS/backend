package com.scms.backend.training;

import org.springframework.http.HttpStatus;

public class TrainingPlanException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	private TrainingPlanException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	static TrainingPlanException actorNotActiveCoach() {
		return new TrainingPlanException(HttpStatus.FORBIDDEN, "ACTOR_NOT_ACTIVE_COACH",
				"This action requires an active Coach account.");
	}

	static TrainingPlanException plusMembershipRequired() {
		return new TrainingPlanException(HttpStatus.CONFLICT, "PLUS_MEMBERSHIP_REQUIRED",
				"Creating a Training Plan requires an active PLUS Membership.");
	}

	static TrainingPlanException activeAssignmentNotFound() {
		return new TrainingPlanException(HttpStatus.NOT_FOUND, "COACH_ASSIGNMENT_NOT_FOUND",
				"No active coaching assignment was found for this Member and Coach.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
