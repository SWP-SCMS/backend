package com.scms.backend.common.error;

import com.scms.backend.centervisit.CenterVisitException;
import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import com.scms.backend.identity.receptionmember.ReceptionMemberValidationException;
import com.scms.backend.manager.MemberStatusConflictException;
import com.scms.backend.manager.StaffAccountValidationException;
import com.scms.backend.manager.StaffStatusConflictException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

final class AccountProblemResponses {

	static ProblemDetail handleMemberProfileValidation(MemberProfileValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleReceptionMemberValidation(ReceptionMemberValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleStaffAccountValidation(StaffAccountValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleStaffAccountNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested staff account was not found.");
		problem.setTitle("Staff account not found");
		problem.setProperty("code", "STAFF_ACCOUNT_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleMemberAccountNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested member account was not found.");
		problem.setTitle("Member not found");
		problem.setProperty("code", "MEMBER_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleMemberStatusConflict(MemberStatusConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Member status conflict");
		problem.setProperty("code", "MEMBER_STATUS_CONFLICT");
		return problem;
	}

	static ProblemDetail handleStaffStatusConflict(StaffStatusConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Staff status conflict");
		problem.setProperty("code", "STAFF_STATUS_CONFLICT");
		return problem;
	}

	static ProblemDetail handleReceptionMemberNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested member was not found.");
		problem.setTitle("Member not found");
		problem.setProperty("code", "MEMBER_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleCenterVisit(CenterVisitException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Center check-in failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}
}
