package com.scms.backend.common.error;

import com.scms.backend.scheduling.AttendanceException;
import com.scms.backend.scheduling.BookingException;
import com.scms.backend.scheduling.CoachLeaveException;
import com.scms.backend.scheduling.DisciplineValidationException;
import com.scms.backend.scheduling.PtSessionAssignmentException;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.RoomException;
import com.scms.backend.scheduling.SportClassException;
import com.scms.backend.scheduling.WaitlistException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

final class SchedulingProblemResponses {

	static ProblemDetail handleDisciplineValidation(DisciplineValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		if (!exception.getAffectedSessions().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessions().stream().map(session -> session.sessionId().toString()).toList());
			problem.setProperty("affectedSessions", exception.getAffectedSessions());
		}
		return problem;
	}

	static ProblemDetail handleDisciplineNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested discipline was not found.");
		problem.setTitle("Discipline not found");
		problem.setProperty("code", "DISCIPLINE_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleDuplicateDiscipline() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A discipline with this name already exists.");
		problem.setTitle("Discipline conflict");
		problem.setProperty("code", "DISCIPLINE_NAME_CONFLICT");
		return problem;
	}

	static ProblemDetail handleSportClass(SportClassException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Class request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (!exception.getAffectedSessions().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessions().stream().map(session -> session.sessionId().toString()).toList());
			problem.setProperty("affectedSessions", exception.getAffectedSessions());
		}
		return problem;
	}

	static ProblemDetail handleRoom(RoomException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Room request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (!exception.getAffectedSessionIds().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessionIds().stream().map(UUID::toString).toList());
		}
		return problem;
	}

	static ProblemDetail handleRecurringSchedule(RecurringScheduleException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Recurring schedule request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if ("SESSION_BULK_SELECTION_CHANGED".equals(exception.getCode())) {
			problem.setProperty("eligibleSessionIds", exception.getEligibleSessionIds());
			problem.setProperty("ineligibleSessionIds", exception.getIneligibleSessionIds());
		}
		if (exception.getRoomCapacity() != null) {
			problem.setProperty("roomCapacity", exception.getRoomCapacity());
			problem.setProperty("occupiedCapacity", exception.getOccupiedCapacity());
			problem.setProperty("requestedCapacity", exception.getRequestedCapacity());
		}
		return problem;
	}

	static ProblemDetail handlePtSessionAssignment(PtSessionAssignmentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("PT Session assignment failed");
		problem.setProperty("code", exception.getCode());
		problem.setProperty("affectedBookings", exception.getAffectedBookings());
		problem.setProperty("affectedWaitlists", exception.getAffectedWaitlists());
		return problem;
	}

	static ProblemDetail handleBooking(BookingException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Booking request failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getRequiredBenefitCodes() != null) {
			problem.setProperty("requiredBenefitCodes", exception.getRequiredBenefitCodes());
			problem.setProperty("missingBenefitCodes", exception.getMissingBenefitCodes());
		}
		return problem;
	}

	static ProblemDetail handleCoachLeave(CoachLeaveException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Coach leave request failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (!exception.getAffectedSessionIds().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessionIds());
		}
		return problem;
	}

	static ProblemDetail handleWaitlist(WaitlistException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Waitlist request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handleAttendance(AttendanceException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Attendance request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}
}
