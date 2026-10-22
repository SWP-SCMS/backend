package com.scms.backend.common.error;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.auth.DuplicateAccountException;
import com.scms.backend.auth.CurrentPasswordIncorrectException;
import com.scms.backend.auth.ChangePasswordValidationException;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.audit.AuditHistoryValidationException;
import com.scms.backend.auth.InvalidCredentialsException;
import com.scms.backend.auth.InvalidRefreshTokenException;
import com.scms.backend.auth.NewPasswordSameAsCurrentException;
import com.scms.backend.auth.RegistrationValidationException;
import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import com.scms.backend.membership.ActiveMembershipExistsException;
import com.scms.backend.membership.InvalidPlanCodeException;
import com.scms.backend.membership.MemberNotActiveException;
import com.scms.backend.membership.MembershipOfferNotFoundException;
import com.scms.backend.membership.MembershipOfferUnavailableException;
import com.scms.backend.membership.MembershipOrderMemberNotFoundException;
import com.scms.backend.membership.MembershipRenewalAlreadyExistsException;
import com.scms.backend.membership.PendingMembershipOrderExistsException;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.ReportingValidationException;
import com.scms.backend.membership.SepayValidationException;
import com.scms.backend.membership.MembershipOfferValidationException;
import com.scms.backend.membership.MembershipPlanConflictException;
import com.scms.backend.membership.MembershipPlanNotFoundException;
import com.scms.backend.membership.MembershipPlanValidationException;
import com.scms.backend.identity.receptionmember.ReceptionMemberNotFoundException;
import com.scms.backend.identity.receptionmember.ReceptionMemberValidationException;
import com.scms.backend.centervisit.CenterVisitException;
import com.scms.backend.manager.StaffAccountValidationException;
import com.scms.backend.manager.StaffAccountNotFoundException;
import com.scms.backend.manager.MemberAccountNotFoundException;
import com.scms.backend.manager.MemberStatusConflictException;
import com.scms.backend.manager.StaffStatusConflictException;
import com.scms.backend.scheduling.DisciplineNotFoundException;
import com.scms.backend.scheduling.DisciplineValidationException;
import com.scms.backend.scheduling.DuplicateDisciplineException;
import com.scms.backend.scheduling.SportClassException;
import com.scms.backend.scheduling.RoomException;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.BookingException;
import com.scms.backend.scheduling.AttendanceException;
import com.scms.backend.notification.NotificationNotFoundException;
import com.scms.backend.notification.NotificationValidationException;
import com.scms.backend.training.CoachAssignmentException;
import com.scms.backend.training.TrainingFeedbackException;
import com.scms.backend.training.TrainingPlanException;
import com.scms.backend.training.TrainingResultException;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.PendingRegistrationException;
import com.scms.backend.emailverification.PasswordResetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.core.MethodParameter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<Object> {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	public boolean supports(MethodParameter returnType,
			Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {
		return body instanceof ProblemDetail problem
			? ProblemDetails.complete(problem, request.getURI().getPath()) : body;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.computeIfAbsent(fieldError.getField(), ignored -> new ArrayList<>())
				.add(fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage());
		}

		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"The request body is missing or malformed.");
		problem.setTitle("Malformed request");
		problem.setProperty("code", "MALFORMED_REQUEST");
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"A request parameter or path value is malformed.");
		problem.setTitle("Malformed request");
		problem.setProperty("code", "MALFORMED_REQUEST");
		return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
			"Invalid credentials");
		problem.setTitle("Authentication failed");
		problem.setProperty("code", "INVALID_CREDENTIALS");
		return problem;
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ProblemDetail handleInvalidRefreshToken() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
			"The refresh token is invalid or expired.");
		problem.setTitle("Refresh failed");
		problem.setProperty("code", "INVALID_REFRESH_TOKEN");
		return problem;
	}

	@ExceptionHandler(InvalidAuthenticatedAccountException.class)
	ProblemDetail handleInvalidAuthenticatedAccount() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
			"The authenticated account is no longer active.");
		problem.setTitle("Authentication failed");
		problem.setProperty("code", "INVALID_TOKEN");
		return problem;
	}

	@ExceptionHandler(CurrentPasswordIncorrectException.class)
	ProblemDetail handleCurrentPasswordIncorrect() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"The current password is incorrect.");
		problem.setTitle("Password change failed");
		problem.setProperty("code", "CURRENT_PASSWORD_INCORRECT");
		return problem;
	}

	@ExceptionHandler(ChangePasswordValidationException.class)
	ProblemDetail handleChangePasswordValidation(ChangePasswordValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(NewPasswordSameAsCurrentException.class)
	ProblemDetail handleNewPasswordSameAsCurrent() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"The new password must differ from the current password.");
		problem.setTitle("Password change failed");
		problem.setProperty("code", "NEW_PASSWORD_SAME_AS_CURRENT");
		return problem;
	}

	@ExceptionHandler(RegistrationValidationException.class)
	ProblemDetail handleRegistrationValidation(RegistrationValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(DuplicateAccountException.class)
	ProblemDetail handleDuplicateAccount(DuplicateAccountException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Account conflict");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	@ExceptionHandler(PendingRegistrationException.class)
	ProblemDetail handlePendingRegistration(PendingRegistrationException exception) {
		ProblemDetail problem=ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,exception.getMessage());
		problem.setTitle("Email verification pending"); problem.setProperty("code","EMAIL_VERIFICATION_PENDING");
		problem.setProperty("otpExpiresAt",exception.getOtpExpiresAt());
		problem.setProperty("resendAvailableAt",exception.getResendAvailableAt());
		problem.setProperty("registrationExpiresAt",exception.getRegistrationExpiresAt());
		problem.setProperty("serverTime",exception.getServerTime()); return problem;
	}

	@ExceptionHandler(EmailVerificationException.class)
	ResponseEntity<ProblemDetail> handleEmailVerification(EmailVerificationException exception) {
		ProblemDetail problem=ProblemDetail.forStatusAndDetail(exception.getStatus(),exception.getMessage());
		problem.setTitle("Email verification failed"); problem.setProperty("code",exception.getCode());
		ResponseEntity.BodyBuilder response=ResponseEntity.status(exception.getStatus());
		if(exception.getRetryAfter()!=null) response.header(HttpHeaders.RETRY_AFTER,exception.getRetryAfter().toString());
		return response.contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
	}

	@ExceptionHandler(PasswordResetException.class)
	ProblemDetail handlePasswordReset(PasswordResetException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Password reset failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getField() != null)
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getFieldMessage())));
		return problem;
	}

	@ExceptionHandler(MemberProfileValidationException.class)
	ProblemDetail handleMemberProfileValidation(MemberProfileValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(ReceptionMemberValidationException.class)
	ProblemDetail handleReceptionMemberValidation(ReceptionMemberValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(StaffAccountValidationException.class)
	ProblemDetail handleStaffAccountValidation(StaffAccountValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(StaffAccountNotFoundException.class)
	ProblemDetail handleStaffAccountNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested staff account was not found.");
		problem.setTitle("Staff account not found");
		problem.setProperty("code", "STAFF_ACCOUNT_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(MemberAccountNotFoundException.class)
	ProblemDetail handleMemberAccountNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested member account was not found.");
		problem.setTitle("Member not found");
		problem.setProperty("code", "MEMBER_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(MemberStatusConflictException.class)
	ProblemDetail handleMemberStatusConflict(MemberStatusConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Member status conflict");
		problem.setProperty("code", "MEMBER_STATUS_CONFLICT");
		return problem;
	}

	@ExceptionHandler(StaffStatusConflictException.class)
	ProblemDetail handleStaffStatusConflict(StaffStatusConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Staff status conflict");
		problem.setProperty("code", "STAFF_STATUS_CONFLICT");
		return problem;
	}

	@ExceptionHandler(DisciplineValidationException.class)
	ProblemDetail handleDisciplineValidation(DisciplineValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		if (!exception.getAffectedSessions().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessions().stream()
				.map(session -> session.sessionId().toString()).toList());
			problem.setProperty("affectedSessions", exception.getAffectedSessions());
		}
		return problem;
	}

	@ExceptionHandler(DisciplineNotFoundException.class)
	ProblemDetail handleDisciplineNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested discipline was not found.");
		problem.setTitle("Discipline not found");
		problem.setProperty("code", "DISCIPLINE_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(DuplicateDisciplineException.class)
	ProblemDetail handleDuplicateDiscipline() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"A discipline with this name already exists.");
		problem.setTitle("Discipline conflict");
		problem.setProperty("code", "DISCIPLINE_NAME_CONFLICT");
		return problem;
	}

	@ExceptionHandler(SportClassException.class)
	ProblemDetail handleSportClass(SportClassException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Class request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (!exception.getAffectedSessions().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessions().stream()
				.map(session -> session.sessionId().toString()).toList());
			problem.setProperty("affectedSessions", exception.getAffectedSessions());
		}
		return problem;
	}

	@ExceptionHandler(RoomException.class)
	ProblemDetail handleRoom(RoomException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Room request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (!exception.getAffectedSessionIds().isEmpty()) {
			problem.setProperty("affectedSessionIds", exception.getAffectedSessionIds().stream()
				.map(UUID::toString).toList());
		}
		return problem;
	}

	@ExceptionHandler(RecurringScheduleException.class)
	ProblemDetail handleRecurringSchedule(RecurringScheduleException exception) {
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
		return problem;
	}

	@ExceptionHandler(BookingException.class)
	ProblemDetail handleBooking(BookingException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Booking request failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getRequiredBenefitCodes() != null) {
			problem.setProperty("requiredBenefitCodes", exception.getRequiredBenefitCodes());
			problem.setProperty("missingBenefitCodes", exception.getMissingBenefitCodes());
		}
		return problem;
	}

	@ExceptionHandler(AttendanceException.class)
	ProblemDetail handleAttendance(AttendanceException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Attendance request failed"); problem.setProperty("code", exception.getCode()); return problem;
	}

	@ExceptionHandler(ReceptionMemberNotFoundException.class)
	ProblemDetail handleReceptionMemberNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested member was not found.");
		problem.setTitle("Member not found");
		problem.setProperty("code", "MEMBER_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(CenterVisitException.class)
	ProblemDetail handleCenterVisit(CenterVisitException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Center check-in failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	@ExceptionHandler(InvalidPlanCodeException.class)
	ProblemDetail handleInvalidPlanCode(InvalidPlanCodeException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			exception.getMessage());
		problem.setTitle("Invalid plan code");
		problem.setProperty("code", "INVALID_PLAN_CODE");
		return problem;
	}

	@ExceptionHandler(MembershipOfferNotFoundException.class)
	ProblemDetail handleMembershipOfferNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested membership offer was not found.");
		problem.setTitle("Membership offer not found");
		problem.setProperty("code", "MEMBERSHIP_OFFER_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(MembershipOfferUnavailableException.class)
	ProblemDetail handleMembershipOfferUnavailable() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"The requested membership offer is not available.");
		problem.setTitle("Membership offer not available");
		problem.setProperty("code", "MEMBERSHIP_OFFER_NOT_AVAILABLE");
		return problem;
	}

	@ExceptionHandler(MembershipOrderMemberNotFoundException.class)
	ProblemDetail handleMembershipOrderMemberNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested member was not found.");
		problem.setTitle("Member not found");
		problem.setProperty("code", "MEMBER_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(MemberNotActiveException.class)
	ProblemDetail handleMemberNotActive() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"The member account is not active.");
		problem.setTitle("Member not active");
		problem.setProperty("code", "MEMBER_NOT_ACTIVE");
		return problem;
	}

	@ExceptionHandler(ActiveMembershipExistsException.class)
	ProblemDetail handleActiveMembershipExists() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"The member already has an active membership.");
		problem.setTitle("Active membership exists");
		problem.setProperty("code", "ACTIVE_MEMBERSHIP_EXISTS");
		return problem;
	}

	@ExceptionHandler(PendingMembershipOrderExistsException.class)
	ProblemDetail handlePendingMembershipOrderExists() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"The member already has a pending membership order.");
		problem.setTitle("Pending membership order exists");
		problem.setProperty("code", "PENDING_MEMBERSHIP_ORDER_EXISTS");
		return problem;
	}

	@ExceptionHandler(MembershipRenewalAlreadyExistsException.class)
	ProblemDetail handleMembershipRenewalAlreadyExists() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
			"The member already has a reserved membership or pending renewal order.");
		problem.setTitle("Membership renewal already exists");
		problem.setProperty("code", "MEMBERSHIP_RENEWAL_ALREADY_EXISTS");
		return problem;
	}

	@ExceptionHandler(SepayValidationException.class)
	ProblemDetail handleSepayValidation(SepayValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("SePay validation failed");
		problem.setProperty("code", "SEPAY_VALIDATION_ERROR");
		return problem;
	}

	@ExceptionHandler(MembershipOfferValidationException.class)
	ProblemDetail handleMembershipOfferValidation(MembershipOfferValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
		problem.setTitle("Membership offer validation failed");
		problem.setProperty("code", "MEMBERSHIP_OFFER_VALIDATION_ERROR");
		return problem;
	}

	@ExceptionHandler(MembershipPlanValidationException.class)
	ProblemDetail handleMembershipPlanValidation(MembershipPlanValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Membership plan validation failed");
		problem.setProperty("code", "MEMBERSHIP_PLAN_VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(MembershipPlanConflictException.class)
	ProblemDetail handleMembershipPlanConflict(MembershipPlanConflictException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
		problem.setTitle("Membership plan conflict");
		problem.setProperty("code", exception.getMessage());
		return problem;
	}

	@ExceptionHandler(MembershipPlanNotFoundException.class)
	ProblemDetail handleMembershipPlanNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested membership plan was not found.");
		problem.setTitle("Membership plan not found");
		problem.setProperty("code", "MEMBERSHIP_PLAN_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(NotificationNotFoundException.class)
	ProblemDetail handleNotificationNotFound() {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
			"The requested notification was not found.");
		problem.setTitle("Notification not found");
		problem.setProperty("code", "NOTIFICATION_NOT_FOUND");
		return problem;
	}

	@ExceptionHandler(NotificationValidationException.class)
	ProblemDetail handleNotificationValidation(NotificationValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(AuditHistoryValidationException.class)
	ProblemDetail handleAuditHistoryValidation(AuditHistoryValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}

	@ExceptionHandler(PaymentException.class)
	ProblemDetail handlePayment(PaymentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.status(), exception.getMessage());
		problem.setTitle("Payment request failed");
		problem.setProperty("code", exception.code());
		return problem;
	}

	@ExceptionHandler(ReportingValidationException.class)
	ProblemDetail handleReportingValidation(ReportingValidationException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		return problem;
	}
	@ExceptionHandler(CoachAssignmentException.class)
	ProblemDetail handleCoachAssignment(CoachAssignmentException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Coach assignment request failed");
		problem.setProperty("code", exception.getCode());
		if ("VALIDATION_ERROR".equals(exception.getCode()) && exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		return problem;
	}

	@ExceptionHandler(TrainingPlanException.class)
	ProblemDetail handleTrainingPlan(TrainingPlanException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Training Plan request failed");
		problem.setProperty("code", exception.getCode());
		if (exception.getField() != null) {
			problem.setProperty("errors", Map.of(exception.getField(), List.of(exception.getMessage())));
		}
		if (exception.getCurrentVersionNumber() != null) {
			problem.setProperty("currentVersionNumber", exception.getCurrentVersionNumber());
		}
		if (exception.getActivePlanId() != null) {
			problem.setProperty("activePlanId", exception.getActivePlanId());
			problem.setProperty("activePlanTitle", exception.getActivePlanTitle());
		}
		return problem;
	}

	@ExceptionHandler(TrainingResultException.class)
	ProblemDetail handleTrainingResult(TrainingResultException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Training Result request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	@ExceptionHandler(TrainingFeedbackException.class)
	ProblemDetail handleTrainingFeedback(TrainingFeedbackException exception) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
		problem.setTitle("Training Feedback request failed");
		problem.setProperty("code", exception.getCode());
		return problem;
	}

	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception,
		HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
			"The uploaded file is too large.");
		problem.setTitle("Upload validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of("file", List.of("must not exceed the configured upload limit")));
		return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		logger.error("Unhandled request failure", exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
			"An unexpected error occurred.");
		problem.setTitle("Internal server error");
		problem.setProperty("code", "INTERNAL_ERROR");
		return problem;
	}
}
