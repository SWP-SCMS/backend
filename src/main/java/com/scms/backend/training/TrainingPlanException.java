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
				"Creating or editing a Training Plan requires an active Membership with the TRAINING_PLAN Benefit.");
	}

	static TrainingPlanException activeAssignmentNotFound() {
		return new TrainingPlanException(HttpStatus.NOT_FOUND, "COACH_ASSIGNMENT_NOT_FOUND",
				"No active coaching assignment was found for this Member and Coach.");
	}

	static TrainingPlanException planNotFound() {
		return new TrainingPlanException(HttpStatus.NOT_FOUND, "TRAINING_PLAN_NOT_FOUND",
				"The requested Training Plan was not found for this Member.");
	}

	static TrainingPlanException planNotActive() {
		return new TrainingPlanException(HttpStatus.CONFLICT, "TRAINING_PLAN_NOT_ACTIVE",
				"Only an active Training Plan can be updated.");
	}

	static TrainingPlanException versionHistoryNotFound() {
		return new TrainingPlanException(HttpStatus.CONFLICT, "TRAINING_PLAN_VERSION_NOT_FOUND",
				"The Training Plan has no version history to update.");
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
}
