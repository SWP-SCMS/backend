package com.scms.backend.training;

import com.scms.backend.scheduling.PtAssignmentImpactResponse;
import org.springframework.http.HttpStatus;

public class CoachAssignmentException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final String field;
	private final PtAssignmentImpactResponse impact;

	private CoachAssignmentException(HttpStatus status, String code, String message, String field) {
		this(status, code, message, field, null);
	}

	private CoachAssignmentException(HttpStatus status, String code, String message, String field,
			PtAssignmentImpactResponse impact) {
		super(message);
		this.status = status;
		this.code = code;
		this.field = field;
		this.impact = impact;
	}

	private CoachAssignmentException(HttpStatus status, String code, String message) {
		this(status, code, message, null);
	}

	public static CoachAssignmentException memberNotFound() {
		return new CoachAssignmentException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "The requested Member was not found.");
	}

	public static CoachAssignmentException coachNotFound() {
		return new CoachAssignmentException(HttpStatus.NOT_FOUND, "COACH_NOT_FOUND", "The requested Coach was not found.");
	}

	public static CoachAssignmentException memberNotActive() {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "MEMBER_NOT_ACTIVE", "Coach assignment requires an active Member account.");
	}

	public static CoachAssignmentException coachNotActive() {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "COACH_NOT_ACTIVE", "Coach assignment requires an active Coach account.");
	}

	public static CoachAssignmentException plusMembershipRequired() {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "PLUS_MEMBERSHIP_REQUIRED",
				"Coach assignment requires an active Membership with the PERSONAL_COACHING Benefit.");
	}

	public static CoachAssignmentException membershipFrozen() {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "MEMBERSHIP_FROZEN", "Coach assignment is unavailable while the Membership is frozen.");
	}

	public static CoachAssignmentException alreadyAssignedToThisCoach() {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "COACH_ALREADY_ASSIGNED", "The Member is already actively assigned to this Coach.");
	}

	public static CoachAssignmentException activeAssignmentNotFound() {
		return new CoachAssignmentException(HttpStatus.NOT_FOUND, "COACH_ASSIGNMENT_NOT_FOUND", "No active Coach assignment was found for this Member.");
	}

	public static CoachAssignmentException actorNotActiveManager() {
		return new CoachAssignmentException(HttpStatus.FORBIDDEN, "ACTOR_NOT_ACTIVE_MANAGER", "This action requires an active Manager account.");
	}

	public static CoachAssignmentException concurrentConflict() {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "COACH_ASSIGNMENT_CONFLICT", "The Coach assignment could not be confirmed because state changed concurrently.");
	}

	public static CoachAssignmentException impactConfirmationRequired(PtAssignmentImpactResponse impact) {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "COACH_ASSIGNMENT_IMPACT_CONFIRMATION_REQUIRED",
			"Future PT bookings or waitlists must be previewed and confirmed before changing the Coach assignment.",
			null, impact);
	}

	public static CoachAssignmentException impactChanged(PtAssignmentImpactResponse impact) {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "COACH_ASSIGNMENT_IMPACT_CHANGED",
			"The affected future PT bookings or waitlists changed. Preview and confirm the current impact.",
			null, impact);
	}

	public static CoachAssignmentException coachCapacityExceeded(int maximumMemberCount) {
		return new CoachAssignmentException(HttpStatus.CONFLICT, "COACH_CAPACITY_EXCEEDED",
				"The Coach already has the maximum of " + maximumMemberCount + " active Members.");
	}

	public static CoachAssignmentException validationError(String field, String message) {
		return new CoachAssignmentException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, field);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getField() {
		return field;
	}

	public PtAssignmentImpactResponse getImpact() {
		return impact;
	}
}
