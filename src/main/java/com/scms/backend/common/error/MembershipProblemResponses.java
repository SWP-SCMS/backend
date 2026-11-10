package com.scms.backend.common.error;

import com.scms.backend.membership.InvalidPlanCodeException;
import com.scms.backend.membership.MembershipFreezeException;
import com.scms.backend.membership.MembershipOfferValidationException;
import com.scms.backend.membership.MembershipPlanConflictException;
import com.scms.backend.membership.MembershipPlanValidationException;
import com.scms.backend.membership.MembershipTerminationException;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.RefundException;
import com.scms.backend.membership.ReportingValidationException;
import com.scms.backend.membership.SepayValidationException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

final class MembershipProblemResponses {

	static ProblemDetail handleInvalidPlanCode(InvalidPlanCodeException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Invalid plan code");
		problem.setProperty("code", "INVALID_PLAN_CODE");
		return problem;
	}

	static ProblemDetail handleMembershipOfferNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested membership offer was not found.");
		problem.setTitle("Membership offer not found");
		problem.setProperty("code", "MEMBERSHIP_OFFER_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleMembershipOfferUnavailable() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The requested membership offer is not available.");
		problem.setTitle("Membership offer not available");
		problem.setProperty("code", "MEMBERSHIP_OFFER_NOT_AVAILABLE");
		return problem;
	}

	static ProblemDetail handleMembershipOrderMemberNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested member was not found.");
		problem.setTitle("Member not found");
		problem.setProperty("code", "MEMBER_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleMemberNotActive() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The member account is not active.");
		problem.setTitle("Member not active");
		problem.setProperty("code", "MEMBER_NOT_ACTIVE");
		return problem;
	}

	static ProblemDetail handleActiveMembershipExists() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The member already has an active membership.");
		problem.setTitle("Active membership exists");
		problem.setProperty("code", "ACTIVE_MEMBERSHIP_EXISTS");
		return problem;
	}

	static ProblemDetail handlePendingMembershipOrderExists() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The member already has a pending membership order.");
		problem.setTitle("Pending membership order exists");
		problem.setProperty("code", "PENDING_MEMBERSHIP_ORDER_EXISTS");
		return problem;
	}

	static ProblemDetail handleMembershipRenewalAlreadyExists() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The member already has a reserved membership or pending renewal order.");
		problem.setTitle("Membership renewal already exists");
		problem.setProperty("code", "MEMBERSHIP_RENEWAL_ALREADY_EXISTS");
		return problem;
	}

	static ProblemDetail handleSepayValidation(SepayValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("SePay validation failed");
		problem.setProperty("code", "SEPAY_VALIDATION_ERROR");
		return problem;
	}

	static ProblemDetail handleMembershipOfferValidation(MembershipOfferValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Membership offer validation failed");
		problem.setProperty("code", "MEMBERSHIP_OFFER_VALIDATION_ERROR");
		return problem;
	}

	static ProblemDetail handleMembershipPlanValidation(MembershipPlanValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Membership plan validation failed");
		problem.setProperty("code", "MEMBERSHIP_PLAN_VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	static ProblemDetail handleMembershipPlanConflict(MembershipPlanConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Membership plan conflict");
		problem.setProperty("code", exception.getMessage());
		return problem;
	}

	static ProblemDetail handleMembershipPlanNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested membership plan was not found.");
		problem.setTitle("Membership plan not found");
		problem.setProperty("code", "MEMBERSHIP_PLAN_NOT_FOUND");
		return problem;
	}

	static ProblemDetail handleMembershipTermination(MembershipTerminationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Membership termination request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handleMembershipFreeze(MembershipFreezeException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Membership freeze request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handleRefund(RefundException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Refund request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	static ProblemDetail handlePayment(PaymentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle("Payment request failed");
		problem.setProperty("code", exception.code());
		return problem;
	}

	static ProblemDetail handleReportingValidation(ReportingValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}
}
