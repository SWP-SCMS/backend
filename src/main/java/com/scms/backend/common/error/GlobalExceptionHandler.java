package com.scms.backend.common.error;

import com.scms.backend.audit.AuditHistoryValidationException;
import com.scms.backend.auth.ChangePasswordValidationException;
import com.scms.backend.auth.CurrentPasswordIncorrectException;
import com.scms.backend.auth.DuplicateAccountException;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import com.scms.backend.auth.InvalidCredentialsException;
import com.scms.backend.auth.InvalidRefreshTokenException;
import com.scms.backend.auth.NewPasswordSameAsCurrentException;
import com.scms.backend.auth.RegistrationValidationException;
import com.scms.backend.centervisit.CenterVisitException;
import com.scms.backend.course.CourseAttendanceException;
import com.scms.backend.course.CourseException;
import com.scms.backend.course.CourseOrderException;
import com.scms.backend.course.CourseRefundException;
import com.scms.backend.course.CourseTransferException;
import com.scms.backend.course.CourseWaitlistException;
import com.scms.backend.course.CourseWithdrawalException;
import com.scms.backend.emailverification.EmailVerificationException;
import com.scms.backend.emailverification.PasswordResetException;
import com.scms.backend.emailverification.PendingRegistrationException;
import com.scms.backend.identity.memberprofile.MemberProfileValidationException;
import com.scms.backend.identity.receptionmember.ReceptionMemberNotFoundException;
import com.scms.backend.identity.receptionmember.ReceptionMemberValidationException;
import com.scms.backend.manager.MemberAccountNotFoundException;
import com.scms.backend.manager.MemberStatusConflictException;
import com.scms.backend.manager.StaffAccountNotFoundException;
import com.scms.backend.manager.StaffAccountValidationException;
import com.scms.backend.manager.StaffStatusConflictException;
import com.scms.backend.membership.ActiveMembershipExistsException;
import com.scms.backend.membership.InvalidPlanCodeException;
import com.scms.backend.membership.MemberNotActiveException;
import com.scms.backend.membership.MembershipFreezeException;
import com.scms.backend.membership.MembershipOfferNotFoundException;
import com.scms.backend.membership.MembershipOfferUnavailableException;
import com.scms.backend.membership.MembershipOfferValidationException;
import com.scms.backend.membership.MembershipOrderMemberNotFoundException;
import com.scms.backend.membership.MembershipPlanConflictException;
import com.scms.backend.membership.MembershipPlanNotFoundException;
import com.scms.backend.membership.MembershipPlanValidationException;
import com.scms.backend.membership.MembershipRenewalAlreadyExistsException;
import com.scms.backend.membership.MembershipTerminationException;
import com.scms.backend.membership.PaymentException;
import com.scms.backend.membership.PendingMembershipOrderExistsException;
import com.scms.backend.membership.RefundException;
import com.scms.backend.membership.ReportingValidationException;
import com.scms.backend.membership.SepayValidationException;
import com.scms.backend.notification.NotificationNotFoundException;
import com.scms.backend.notification.NotificationValidationException;
import com.scms.backend.scheduling.AttendanceException;
import com.scms.backend.scheduling.BookingException;
import com.scms.backend.scheduling.CoachLeaveException;
import com.scms.backend.scheduling.DisciplineNotFoundException;
import com.scms.backend.scheduling.DisciplineValidationException;
import com.scms.backend.scheduling.DuplicateDisciplineException;
import com.scms.backend.scheduling.PtSessionAssignmentException;
import com.scms.backend.scheduling.RecurringScheduleException;
import com.scms.backend.scheduling.RoomException;
import com.scms.backend.scheduling.SportClassException;
import com.scms.backend.scheduling.WaitlistException;
import com.scms.backend.training.CoachAssignmentException;
import com.scms.backend.training.TrainingFeedbackException;
import com.scms.backend.training.TrainingPlanException;
import com.scms.backend.training.TrainingResultException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<Object> {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
		return body instanceof ProblemDetail problem ? ProblemDetails.complete(problem, request.getURI().getPath()) : body;
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, List<String>> errors = new LinkedHashMap<>();
		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			errors.computeIfAbsent(fieldError.getField(), ignored -> new ArrayList<>()).add(fieldError.getDefaultMessage() == null ? "Invalid value" : fieldError.getDefaultMessage());
		}
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more request fields are invalid.");
		problem.setTitle("Validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body is missing or malformed.");
		problem.setTitle("Malformed request");
		problem.setProperty("code", "MALFORMED_REQUEST");
		return handleExceptionInternal(exception, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "A request parameter or path value is malformed.");
		problem.setTitle("Malformed request");
		problem.setProperty("code", "MALFORMED_REQUEST");
		return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ProblemDetail handleInvalidCredentials() {
		return AuthenticationProblemResponses.handleInvalidCredentials();
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	ProblemDetail handleInvalidRefreshToken() {
		return AuthenticationProblemResponses.handleInvalidRefreshToken();
	}

	@ExceptionHandler(InvalidAuthenticatedAccountException.class)
	ProblemDetail handleInvalidAuthenticatedAccount() {
		return AuthenticationProblemResponses.handleInvalidAuthenticatedAccount();
	}

	@ExceptionHandler(CurrentPasswordIncorrectException.class)
	ProblemDetail handleCurrentPasswordIncorrect() {
		return AuthenticationProblemResponses.handleCurrentPasswordIncorrect();
	}

	@ExceptionHandler(ChangePasswordValidationException.class)
	ProblemDetail handleChangePasswordValidation(ChangePasswordValidationException exception) {
		return AuthenticationProblemResponses.handleChangePasswordValidation(exception);
	}

	@ExceptionHandler(NewPasswordSameAsCurrentException.class)
	ProblemDetail handleNewPasswordSameAsCurrent() {
		return AuthenticationProblemResponses.handleNewPasswordSameAsCurrent();
	}

	@ExceptionHandler(RegistrationValidationException.class)
	ProblemDetail handleRegistrationValidation(RegistrationValidationException exception) {
		return AuthenticationProblemResponses.handleRegistrationValidation(exception);
	}

	@ExceptionHandler(DuplicateAccountException.class)
	ProblemDetail handleDuplicateAccount(DuplicateAccountException exception) {
		return AuthenticationProblemResponses.handleDuplicateAccount(exception);
	}

	@ExceptionHandler(PendingRegistrationException.class)
	ProblemDetail handlePendingRegistration(PendingRegistrationException exception) {
		return AuthenticationProblemResponses.handlePendingRegistration(exception);
	}

	@ExceptionHandler(EmailVerificationException.class)
	ResponseEntity<ProblemDetail> handleEmailVerification(EmailVerificationException exception) {
		return AuthenticationProblemResponses.handleEmailVerification(exception);
	}

	@ExceptionHandler(PasswordResetException.class)
	ProblemDetail handlePasswordReset(PasswordResetException exception) {
		return AuthenticationProblemResponses.handlePasswordReset(exception);
	}

	@ExceptionHandler(MemberProfileValidationException.class)
	ProblemDetail handleMemberProfileValidation(MemberProfileValidationException exception) {
		return AccountProblemResponses.handleMemberProfileValidation(exception);
	}

	@ExceptionHandler(ReceptionMemberValidationException.class)
	ProblemDetail handleReceptionMemberValidation(ReceptionMemberValidationException exception) {
		return AccountProblemResponses.handleReceptionMemberValidation(exception);
	}

	@ExceptionHandler(StaffAccountValidationException.class)
	ProblemDetail handleStaffAccountValidation(StaffAccountValidationException exception) {
		return AccountProblemResponses.handleStaffAccountValidation(exception);
	}

	@ExceptionHandler(StaffAccountNotFoundException.class)
	ProblemDetail handleStaffAccountNotFound() {
		return AccountProblemResponses.handleStaffAccountNotFound();
	}

	@ExceptionHandler(MemberAccountNotFoundException.class)
	ProblemDetail handleMemberAccountNotFound() {
		return AccountProblemResponses.handleMemberAccountNotFound();
	}

	@ExceptionHandler(MemberStatusConflictException.class)
	ProblemDetail handleMemberStatusConflict(MemberStatusConflictException exception) {
		return AccountProblemResponses.handleMemberStatusConflict(exception);
	}

	@ExceptionHandler(StaffStatusConflictException.class)
	ProblemDetail handleStaffStatusConflict(StaffStatusConflictException exception) {
		return AccountProblemResponses.handleStaffStatusConflict(exception);
	}

	@ExceptionHandler(DisciplineValidationException.class)
	ProblemDetail handleDisciplineValidation(DisciplineValidationException exception) {
		return SchedulingProblemResponses.handleDisciplineValidation(exception);
	}

	@ExceptionHandler(DisciplineNotFoundException.class)
	ProblemDetail handleDisciplineNotFound() {
		return SchedulingProblemResponses.handleDisciplineNotFound();
	}

	@ExceptionHandler(DuplicateDisciplineException.class)
	ProblemDetail handleDuplicateDiscipline() {
		return SchedulingProblemResponses.handleDuplicateDiscipline();
	}

	@ExceptionHandler(SportClassException.class)
	ProblemDetail handleSportClass(SportClassException exception) {
		return SchedulingProblemResponses.handleSportClass(exception);
	}

	@ExceptionHandler(RoomException.class)
	ProblemDetail handleRoom(RoomException exception) {
		return SchedulingProblemResponses.handleRoom(exception);
	}

	@ExceptionHandler(CourseException.class)
	ProblemDetail handleCourse(CourseException exception) {
		return CourseProblemResponses.handleCourse(exception);
	}

	@ExceptionHandler(CourseOrderException.class)
	ProblemDetail handleCourseOrder(CourseOrderException exception) {
		return CourseProblemResponses.handleCourseOrder(exception);
	}

	@ExceptionHandler(CourseWaitlistException.class)
	ProblemDetail handleCourseWaitlist(CourseWaitlistException exception) {
		return CourseProblemResponses.handleCourseWaitlist(exception);
	}

	@ExceptionHandler(CourseTransferException.class)
	ProblemDetail handleCourseTransfer(CourseTransferException exception) {
		return CourseProblemResponses.handleCourseTransfer(exception);
	}

	@ExceptionHandler(CourseWithdrawalException.class)
	ProblemDetail handleCourseWithdrawal(CourseWithdrawalException exception) {
		return CourseProblemResponses.handleCourseWithdrawal(exception);
	}

	@ExceptionHandler(CourseRefundException.class)
	ProblemDetail handleCourseRefund(CourseRefundException exception) {
		return CourseProblemResponses.handleCourseRefund(exception);
	}

	@ExceptionHandler(RecurringScheduleException.class)
	ProblemDetail handleRecurringSchedule(RecurringScheduleException exception) {
		return SchedulingProblemResponses.handleRecurringSchedule(exception);
	}

	@ExceptionHandler(PtSessionAssignmentException.class)
	ProblemDetail handlePtSessionAssignment(PtSessionAssignmentException exception) {
		return SchedulingProblemResponses.handlePtSessionAssignment(exception);
	}

	@ExceptionHandler(BookingException.class)
	ProblemDetail handleBooking(BookingException exception) {
		return SchedulingProblemResponses.handleBooking(exception);
	}

	@ExceptionHandler(CoachLeaveException.class)
	ProblemDetail handleCoachLeave(CoachLeaveException exception) {
		return SchedulingProblemResponses.handleCoachLeave(exception);
	}

	@ExceptionHandler(WaitlistException.class)
	ProblemDetail handleWaitlist(WaitlistException exception) {
		return SchedulingProblemResponses.handleWaitlist(exception);
	}

	@ExceptionHandler(AttendanceException.class)
	ProblemDetail handleAttendance(AttendanceException exception) {
		return SchedulingProblemResponses.handleAttendance(exception);
	}

	@ExceptionHandler(CourseAttendanceException.class)
	ProblemDetail handleCourseAttendance(CourseAttendanceException exception) {
		return CourseProblemResponses.handleCourseAttendance(exception);
	}

	@ExceptionHandler(ReceptionMemberNotFoundException.class)
	ProblemDetail handleReceptionMemberNotFound() {
		return AccountProblemResponses.handleReceptionMemberNotFound();
	}

	@ExceptionHandler(CenterVisitException.class)
	ProblemDetail handleCenterVisit(CenterVisitException exception) {
		return AccountProblemResponses.handleCenterVisit(exception);
	}

	@ExceptionHandler(InvalidPlanCodeException.class)
	ProblemDetail handleInvalidPlanCode(InvalidPlanCodeException exception) {
		return MembershipProblemResponses.handleInvalidPlanCode(exception);
	}

	@ExceptionHandler(MembershipOfferNotFoundException.class)
	ProblemDetail handleMembershipOfferNotFound() {
		return MembershipProblemResponses.handleMembershipOfferNotFound();
	}

	@ExceptionHandler(MembershipOfferUnavailableException.class)
	ProblemDetail handleMembershipOfferUnavailable() {
		return MembershipProblemResponses.handleMembershipOfferUnavailable();
	}

	@ExceptionHandler(MembershipOrderMemberNotFoundException.class)
	ProblemDetail handleMembershipOrderMemberNotFound() {
		return MembershipProblemResponses.handleMembershipOrderMemberNotFound();
	}

	@ExceptionHandler(MemberNotActiveException.class)
	ProblemDetail handleMemberNotActive() {
		return MembershipProblemResponses.handleMemberNotActive();
	}

	@ExceptionHandler(ActiveMembershipExistsException.class)
	ProblemDetail handleActiveMembershipExists() {
		return MembershipProblemResponses.handleActiveMembershipExists();
	}

	@ExceptionHandler(PendingMembershipOrderExistsException.class)
	ProblemDetail handlePendingMembershipOrderExists() {
		return MembershipProblemResponses.handlePendingMembershipOrderExists();
	}

	@ExceptionHandler(MembershipRenewalAlreadyExistsException.class)
	ProblemDetail handleMembershipRenewalAlreadyExists() {
		return MembershipProblemResponses.handleMembershipRenewalAlreadyExists();
	}

	@ExceptionHandler(SepayValidationException.class)
	ProblemDetail handleSepayValidation(SepayValidationException exception) {
		return MembershipProblemResponses.handleSepayValidation(exception);
	}

	@ExceptionHandler(MembershipOfferValidationException.class)
	ProblemDetail handleMembershipOfferValidation(MembershipOfferValidationException exception) {
		return MembershipProblemResponses.handleMembershipOfferValidation(exception);
	}

	@ExceptionHandler(MembershipPlanValidationException.class)
	ProblemDetail handleMembershipPlanValidation(MembershipPlanValidationException exception) {
		return MembershipProblemResponses.handleMembershipPlanValidation(exception);
	}

	@ExceptionHandler(MembershipPlanConflictException.class)
	ProblemDetail handleMembershipPlanConflict(MembershipPlanConflictException exception) {
		return MembershipProblemResponses.handleMembershipPlanConflict(exception);
	}

	@ExceptionHandler(MembershipPlanNotFoundException.class)
	ProblemDetail handleMembershipPlanNotFound() {
		return MembershipProblemResponses.handleMembershipPlanNotFound();
	}

	@ExceptionHandler(MembershipTerminationException.class)
	ProblemDetail handleMembershipTermination(MembershipTerminationException exception) {
		return MembershipProblemResponses.handleMembershipTermination(exception);
	}

	@ExceptionHandler(MembershipFreezeException.class)
	ProblemDetail handleMembershipFreeze(MembershipFreezeException exception) {
		return MembershipProblemResponses.handleMembershipFreeze(exception);
	}

	@ExceptionHandler(RefundException.class)
	ProblemDetail handleRefund(RefundException exception) {
		return MembershipProblemResponses.handleRefund(exception);
	}

	@ExceptionHandler(NotificationNotFoundException.class)
	ProblemDetail handleNotificationNotFound() {
		return OperationsProblemResponses.handleNotificationNotFound();
	}

	@ExceptionHandler(NotificationValidationException.class)
	ProblemDetail handleNotificationValidation(NotificationValidationException exception) {
		return OperationsProblemResponses.handleNotificationValidation(exception);
	}

	@ExceptionHandler(AuditHistoryValidationException.class)
	ProblemDetail handleAuditHistoryValidation(AuditHistoryValidationException exception) {
		return OperationsProblemResponses.handleAuditHistoryValidation(exception);
	}

	@ExceptionHandler(PaymentException.class)
	ProblemDetail handlePayment(PaymentException exception) {
		return MembershipProblemResponses.handlePayment(exception);
	}

	@ExceptionHandler(ReportingValidationException.class)
	ProblemDetail handleReportingValidation(ReportingValidationException exception) {
		return MembershipProblemResponses.handleReportingValidation(exception);
	}

	@ExceptionHandler(CoachAssignmentException.class)
	ProblemDetail handleCoachAssignment(CoachAssignmentException exception) {
		return TrainingProblemResponses.handleCoachAssignment(exception);
	}

	@ExceptionHandler(TrainingPlanException.class)
	ProblemDetail handleTrainingPlan(TrainingPlanException exception) {
		return TrainingProblemResponses.handleTrainingPlan(exception);
	}

	@ExceptionHandler(TrainingResultException.class)
	ProblemDetail handleTrainingResult(TrainingResultException exception) {
		return TrainingProblemResponses.handleTrainingResult(exception);
	}

	@ExceptionHandler(TrainingFeedbackException.class)
	ProblemDetail handleTrainingFeedback(TrainingFeedbackException exception) {
		return TrainingProblemResponses.handleTrainingFeedback(exception);
	}

	@Override
	protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The uploaded file is too large.");
		problem.setTitle("Upload validation failed");
		problem.setProperty("code", "VALIDATION_ERROR");
		problem.setProperty("errors", Map.of("file", List.of("must not exceed the configured upload limit")));
		return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception exception) {
		logger.error("Unhandled request failure", exception);
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
		problem.setTitle("Internal server error");
		problem.setProperty("code", "INTERNAL_ERROR");
		return problem;
	}
}
