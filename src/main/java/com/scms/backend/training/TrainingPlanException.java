package com.scms.backend.training;

import org.springframework.http.HttpStatus;

public class TrainingPlanException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;
	private final Integer currentVersionNumber;
	private final java.util.UUID activePlanId;
	private final String activePlanTitle;

	private TrainingPlanException(HttpStatus status, String code, String message) {
		this(status, code, message, null, null, null, null);
	}

	private TrainingPlanException(HttpStatus status, String code, String message, String field,
			Integer currentVersionNumber) {
		this(status, code, message, field, currentVersionNumber, null, null);
	}

	private TrainingPlanException(HttpStatus status, String code, String message, String field,
			Integer currentVersionNumber, java.util.UUID activePlanId, String activePlanTitle) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
		this.currentVersionNumber = currentVersionNumber;
		this.activePlanId = activePlanId;
		this.activePlanTitle = activePlanTitle;
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

	static TrainingPlanException versionConflict(int currentVersionNumber) {
		return new TrainingPlanException(HttpStatus.CONFLICT, "TRAINING_PLAN_VERSION_CONFLICT",
				"The Training Plan changed after this edit started.", null, currentVersionNumber);
	}

	static TrainingPlanException activePlanExists(TrainingPlan plan, int currentVersionNumber) {
		return new TrainingPlanException(HttpStatus.CONFLICT, "ACTIVE_TRAINING_PLAN_EXISTS",
			"This Member already has an active Training Plan.", null, currentVersionNumber,
			plan.getId(), plan.getTitle());
	}

	static TrainingPlanException activePlanChanged() {
		return new TrainingPlanException(HttpStatus.CONFLICT, "ACTIVE_TRAINING_PLAN_CHANGED",
			"The active Training Plan changed after replacement was confirmed.");
	}

	static TrainingPlanException validationError(String field, String message) {
		return new TrainingPlanException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, field, null);
	}

	public HttpStatus getStatus() { return status; }
	public String getCode() { return code; }
	public String getField() { return field; }
	public Integer getCurrentVersionNumber() { return currentVersionNumber; }
	public java.util.UUID getActivePlanId() { return activePlanId; }
	public String getActivePlanTitle() { return activePlanTitle; }
}
