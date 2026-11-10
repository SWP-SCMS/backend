# DTO và test theo API

Test được liên kết bằng tham chiếu symbol trong source; đây là điểm bắt đầu đọc, không phải khẳng định toàn bộ nhánh nghiệp vụ đã được test. Quyền bổ sung (account ACTIVE, ownership, thời gian) xem service entrypoint trong API_INDEX.

## DELETE /accounts/me/email-change

`AccountEmailChangeController#cancel` → AccountEmailChangeService#cancel. Tổng 2 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## GET /accounts/me/email-change

`AccountEmailChangeController#status` → AccountEmailChangeService#status. Tổng 2 file test tham chiếu.

DTO: [AccountEmailChangeStatus](../../src/main/java/com/scms/backend/emailverification/AccountEmailChangeStatus.java#L5).

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /accounts/me/email-change

`AccountEmailChangeController#begin` → AccountEmailChangeService#begin. Tổng 2 file test tham chiếu.

DTO: [AccountEmailChangeRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRequest.java#L7); [AccountEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeResponse.java#L5).

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## PUT /accounts/me/email-change

`AccountEmailChangeController#replace` → AccountEmailChangeService#replace. Tổng 2 file test tham chiếu.

DTO: [AccountEmailChangeRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRequest.java#L7); [AccountEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeResponse.java#L5).

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /accounts/me/email-change/resend

`AccountEmailChangeController#resend` → AccountEmailChangeService#resend. Tổng 2 file test tham chiếu.

DTO: [AccountEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeResponse.java#L5).

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /accounts/me/email-change/verify

`AccountEmailChangeController#verify` → RefreshTokenCookieService#clear; AccountEmailChangeService#verify. Tổng 5 file test tham chiếu.

DTO: [AccountEmailChangeVerifyRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeVerifyRequest.java#L6); [AccountEmailChangeVerifyResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeVerifyResponse.java#L3).

- [AuthenticationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java)
- [ChangePasswordIntegrationTests.java](../../src/test/java/com/scms/backend/auth/ChangePasswordIntegrationTests.java)
- [RefreshTokenCookieServiceTests.java](../../src/test/java/com/scms/backend/auth/RefreshTokenCookieServiceTests.java)
- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /auth/change-password

`AuthenticationController#changePassword` → AuthenticationService#changePassword; RefreshTokenCookieService#clear. Tổng 4 file test tham chiếu.

DTO: [ChangePasswordRequest](../../src/main/java/com/scms/backend/auth/ChangePasswordRequest.java#L5).

- [AuthenticationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java)
- [AuthenticationServiceTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationServiceTests.java)
- [ChangePasswordIntegrationTests.java](../../src/test/java/com/scms/backend/auth/ChangePasswordIntegrationTests.java)
- [RefreshTokenCookieServiceTests.java](../../src/test/java/com/scms/backend/auth/RefreshTokenCookieServiceTests.java)

## POST /auth/email-verification/change-email

`EmailVerificationController#change` → EmailVerificationService#changeEmail. Tổng 2 file test tham chiếu.

DTO: [ChangePendingEmailRequest](../../src/main/java/com/scms/backend/emailverification/verification/ChangePendingEmailRequest.java#L6); [ChangePendingEmailResponse](../../src/main/java/com/scms/backend/emailverification/verification/ChangePendingEmailResponse.java#L5).

- [RegistrationServiceTests.java](../../src/test/java/com/scms/backend/auth/RegistrationServiceTests.java)
- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /auth/email-verification/resend

`EmailVerificationController#resend` → EmailVerificationService#resend. Tổng 2 file test tham chiếu.

DTO: [ResendEmailRequest](../../src/main/java/com/scms/backend/emailverification/verification/ResendEmailRequest.java#L6); [ResendEmailResponse](../../src/main/java/com/scms/backend/emailverification/verification/ResendEmailResponse.java#L5).

- [RegistrationServiceTests.java](../../src/test/java/com/scms/backend/auth/RegistrationServiceTests.java)
- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /auth/email-verification/verify

`EmailVerificationController#verify` → EmailVerificationService#verify. Tổng 2 file test tham chiếu.

DTO: [VerifyEmailRequest](../../src/main/java/com/scms/backend/emailverification/verification/VerifyEmailRequest.java#L7); [VerifyEmailResponse](../../src/main/java/com/scms/backend/emailverification/verification/VerifyEmailResponse.java#L5).

- [RegistrationServiceTests.java](../../src/test/java/com/scms/backend/auth/RegistrationServiceTests.java)
- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /auth/login

`AuthenticationController#login` → AuthenticationService#login. Tổng 1 file test tham chiếu.

DTO: [AuthResponse](../../src/main/java/com/scms/backend/auth/AuthResponse.java#L6); [LoginRequest](../../src/main/java/com/scms/backend/auth/LoginRequest.java#L6).

- [AuthenticationServiceTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationServiceTests.java)

## POST /auth/logout

`AuthenticationController#logout` → AuthenticationService#logout; RefreshTokenCookieService#clear. Tổng 4 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [AuthenticationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java)
- [AuthenticationServiceTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationServiceTests.java)
- [ChangePasswordIntegrationTests.java](../../src/test/java/com/scms/backend/auth/ChangePasswordIntegrationTests.java)
- [RefreshTokenCookieServiceTests.java](../../src/test/java/com/scms/backend/auth/RefreshTokenCookieServiceTests.java)

## POST /auth/password-reset/confirm

`PasswordResetController#confirm` → PasswordResetService#confirm; PasswordResetCookieService#clear. Tổng 1 file test tham chiếu.

DTO: [PasswordResetConfirmRequest](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetConfirmRequest.java#L7).

- [PasswordResetIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java)

## POST /auth/password-reset/request

`PasswordResetController#request` → PasswordResetService#request. Tổng 0 file test tham chiếu.

DTO: [PasswordResetAcceptedResponse](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetAcceptedResponse.java#L3); [PasswordResetRequest](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRequest.java#L7).


## POST /auth/password-reset/verify

`PasswordResetController#verify` → PasswordResetService#verify; PasswordResetCookieService#create. Tổng 1 file test tham chiếu.

DTO: [PasswordResetVerifiedResponse](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetVerifiedResponse.java#L5); [PasswordResetVerifyRequest](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetVerifyRequest.java#L8).

- [PasswordResetIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java)

## POST /auth/refresh

`AuthenticationController#refresh` → AuthenticationService#refresh. Tổng 1 file test tham chiếu.

DTO: [AuthResponse](../../src/main/java/com/scms/backend/auth/AuthResponse.java#L6).

- [AuthenticationServiceTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationServiceTests.java)

## POST /auth/register

`RegistrationController#register` → RegistrationService#register. Tổng 1 file test tham chiếu.

DTO: [RegistrationRequest](../../src/main/java/com/scms/backend/auth/RegistrationRequest.java#L13); [RegistrationResponse](../../src/main/java/com/scms/backend/auth/RegistrationResponse.java#L6).

- [RegistrationServiceTests.java](../../src/test/java/com/scms/backend/auth/RegistrationServiceTests.java)

## GET /coach/class-sessions

`CoachTeachingScheduleController#list` → CoachTeachingScheduleService#list. Tổng 1 file test tham chiếu.

DTO: [ClassSessionStatus](../../src/main/java/com/scms/backend/scheduling/ClassSessionStatus.java#L3); [CoachClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionPageResponse.java#L5).

- [CoachTeachingScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachTeachingScheduleServiceTests.java)

## GET /coach/class-sessions/{id}

`CoachTeachingScheduleController#get` → CoachTeachingScheduleService#get. Tổng 1 file test tham chiếu.

DTO: [CoachClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionDetailResponse.java#L11).

- [CoachTeachingScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachTeachingScheduleServiceTests.java)

## GET /coach/class-sessions/{sessionId}/attendance

`AttendanceController#list` → AttendanceService#list. Tổng 2 file test tham chiếu.

DTO: [AttendanceResponse](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceResponse.java#L9).

- [AttendanceServiceTests.java](../../src/test/java/com/scms/backend/scheduling/attendance/AttendanceServiceTests.java)
- [SessionLifecycleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleIntegrationTests.java)

## PATCH /coach/class-sessions/{sessionId}/attendance/{attendanceId}

`AttendanceController#update` → AttendanceService#update. Tổng 2 file test tham chiếu.

DTO: [AttendanceResponse](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceResponse.java#L9); [AttendanceUpdateRequest](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceUpdateRequest.java#L5).

- [AttendanceServiceTests.java](../../src/test/java/com/scms/backend/scheduling/attendance/AttendanceServiceTests.java)
- [SessionLifecycleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleIntegrationTests.java)

## GET /coach/class-sessions/{sessionId}/members/{memberId}/feedback

`SessionTrainingFeedbackController#get` → SessionTrainingFeedbackService#get. Tổng 5 file test tham chiếu.

DTO: [SessionTrainingFeedbackResponse](../../src/main/java/com/scms/backend/training/session/SessionTrainingFeedbackResponse.java#L7).

- [SessionTrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackControllerTests.java)
- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackServiceTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /coach/class-sessions/{sessionId}/members/{memberId}/feedback

`SessionTrainingFeedbackController#create` → SessionTrainingFeedbackService#create. Tổng 5 file test tham chiếu.

DTO: [SessionTrainingFeedbackRequest](../../src/main/java/com/scms/backend/training/session/SessionTrainingFeedbackRequest.java#L7); [SessionTrainingFeedbackResponse](../../src/main/java/com/scms/backend/training/session/SessionTrainingFeedbackResponse.java#L7).

- [SessionTrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackControllerTests.java)
- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackServiceTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PUT /coach/class-sessions/{sessionId}/members/{memberId}/feedback/{feedbackId}

`SessionTrainingFeedbackController#update` → SessionTrainingFeedbackService#update. Tổng 5 file test tham chiếu.

DTO: [SessionTrainingFeedbackRequest](../../src/main/java/com/scms/backend/training/session/SessionTrainingFeedbackRequest.java#L7); [SessionTrainingFeedbackResponse](../../src/main/java/com/scms/backend/training/session/SessionTrainingFeedbackResponse.java#L7).

- [SessionTrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackControllerTests.java)
- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackServiceTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /coach/class-sessions/{sessionId}/members/{memberId}/training-results

`SessionTrainingResultController#get` → SessionTrainingResultService#get. Tổng 6 file test tham chiếu.

DTO: [SessionTrainingResultResponse](../../src/main/java/com/scms/backend/training/session/SessionTrainingResultResponse.java#L8).

- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultControllerTests.java)
- [SessionTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultIntegrationTests.java)
- [SessionTrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultServiceTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /coach/class-sessions/{sessionId}/members/{memberId}/training-results

`SessionTrainingResultController#create` → SessionTrainingResultService#create. Tổng 6 file test tham chiếu.

DTO: [SessionTrainingResultRequest](../../src/main/java/com/scms/backend/training/session/SessionTrainingResultRequest.java#L7); [SessionTrainingResultResponse](../../src/main/java/com/scms/backend/training/session/SessionTrainingResultResponse.java#L8).

- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultControllerTests.java)
- [SessionTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultIntegrationTests.java)
- [SessionTrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultServiceTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PUT /coach/class-sessions/{sessionId}/members/{memberId}/training-results/{resultId}

`SessionTrainingResultController#update` → SessionTrainingResultService#update. Tổng 6 file test tham chiếu.

DTO: [SessionTrainingResultRequest](../../src/main/java/com/scms/backend/training/session/SessionTrainingResultRequest.java#L7); [SessionTrainingResultResponse](../../src/main/java/com/scms/backend/training/session/SessionTrainingResultResponse.java#L8).

- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultControllerTests.java)
- [SessionTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultIntegrationTests.java)
- [SessionTrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultServiceTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PUT /coach/class-sessions/{sessionId}/members/{memberId}/training-summary

`SessionTrainingSummaryController#save` → SessionTrainingSummaryService#save. Tổng 2 file test tham chiếu.

DTO: [Request](../../src/main/java/com/scms/backend/training/session/SessionTrainingSummaryController.java#L29); [SessionTrainingSummaryService.Summary](../../src/main/java/com/scms/backend/training/session/SessionTrainingSummaryService.java#L42).

- [SessionTrainingSummaryControllerTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryControllerTests.java)
- [SessionTrainingSummaryServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java)

## GET /coach/course-sessions

`CourseCoachSessionController#list` → CourseCoachSessionService#list. Tổng 2 file test tham chiếu.

DTO: [CourseCoachSessionPage](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionApiModels.java#L28).

- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)

## GET /coach/course-sessions/{sessionId}

`CourseCoachSessionController#detail` → CourseCoachSessionService#detail. Tổng 2 file test tham chiếu.

DTO: [CourseCoachSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionApiModels.java#L25).

- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)

## GET /coach/course-sessions/{sessionId}/members/{memberId}/training-summary

`BookedSessionTrainingController#course` → BookedSessionTrainingService#get. Tổng 3 file test tham chiếu.

DTO: [BookedSessionTrainingService.Summary](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingService.java#L150).

- [BookedSessionTrainingControllerTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingControllerTests.java)
- [BookedSessionTrainingIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java)
- [BookedSessionTrainingServiceTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java)

## PUT /coach/course-sessions/{sessionId}/members/{memberId}/training-summary

`BookedSessionTrainingController#saveCourse` → BookedSessionTrainingService#save. Tổng 3 file test tham chiếu.

DTO: [BookedSessionTrainingService.Summary](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingService.java#L150); [Request](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingController.java#L44).

- [BookedSessionTrainingControllerTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingControllerTests.java)
- [BookedSessionTrainingIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java)
- [BookedSessionTrainingServiceTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java)

## GET /coach/course-sessions/{sessionId}/roster

`CoachRosterController#roster` → CourseEnrollmentService#roster. Tổng 5 file test tham chiếu.

DTO: [RosterEntry](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L30).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## PATCH /coach/course-sessions/{sessionId}/roster/{enrollmentSessionId}/attendance

`CoachRosterController#attendance` → CourseEnrollmentService#updateAttendance. Tổng 5 file test tham chiếu.

DTO: [AttendanceUpdate](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L34); [RosterEntry](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L30).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /coach/leave-requests

`CoachLeaveController#create` → CoachLeaveService#create. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveCreateRequest](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveCreateRequest.java#L5); [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## DELETE /coach/leave-requests/{requestId}

`CoachLeaveController#withdraw` → CoachLeaveService#withdraw. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## PATCH /coach/leave-requests/{requestId}/cancel

`CoachLeaveController#cancel` → CoachLeaveService#cancel. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## GET /coach/me/leave-requests

`CoachLeaveController#mine` → CoachLeaveService#mine. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## GET /coach/members

`CoachAssignedMemberController#list` → CoachAssignedMemberService#listAssignedMembers. Tổng 3 file test tham chiếu.

DTO: [CoachAssignedMemberPageResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberPageResponse.java#L8).

- [CoachAssignedMemberControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberControllerTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignedMemberServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberServiceTests.java)

## GET /coach/members/{memberId}

`CoachAssignedMemberController#getProfile` → CoachAssignedMemberService#getMemberTrainingProfile. Tổng 3 file test tham chiếu.

DTO: [CoachMemberTrainingProfileResponse](../../src/main/java/com/scms/backend/training/assignment/CoachMemberTrainingProfileResponse.java#L11).

- [CoachAssignedMemberControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberControllerTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignedMemberServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberServiceTests.java)

## GET /coach/members/{memberId}/feedback

`TrainingFeedbackController#list` → TrainingFeedbackService#list. Tổng 4 file test tham chiếu.

DTO: [TrainingFeedbackPageResponse](../../src/main/java/com/scms/backend/training/feedback/TrainingFeedbackPageResponse.java#L5).

- [TrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /coach/members/{memberId}/feedback

`TrainingFeedbackController#create` → TrainingFeedbackService#create. Tổng 4 file test tham chiếu.

DTO: [PersonalTrainingFeedbackRequest](../../src/main/java/com/scms/backend/training/feedback/PersonalTrainingFeedbackRequest.java#L7); [TrainingFeedbackResponse](../../src/main/java/com/scms/backend/training/feedback/TrainingFeedbackResponse.java#L7).

- [TrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PUT /coach/members/{memberId}/feedback/{feedbackId}

`TrainingFeedbackController#update` → TrainingFeedbackService#update. Tổng 4 file test tham chiếu.

DTO: [PersonalTrainingFeedbackRequest](../../src/main/java/com/scms/backend/training/feedback/PersonalTrainingFeedbackRequest.java#L7); [TrainingFeedbackResponse](../../src/main/java/com/scms/backend/training/feedback/TrainingFeedbackResponse.java#L7).

- [TrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /coach/members/{memberId}/training-plans

`TrainingPlanController#list` → TrainingPlanService#list. Tổng 6 file test tham chiếu.

DTO: [TrainingPlanPageResponse](../../src/main/java/com/scms/backend/training/plan/TrainingPlanPageResponse.java#L5).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanControllerTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanControllerTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /coach/members/{memberId}/training-plans

`TrainingPlanController#create` → TrainingPlanService#create. Tổng 6 file test tham chiếu.

DTO: [CreateTrainingPlanRequest](../../src/main/java/com/scms/backend/training/plan/CreateTrainingPlanRequest.java#L11); [TrainingPlanResponse](../../src/main/java/com/scms/backend/training/plan/TrainingPlanResponse.java#L11).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanControllerTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanControllerTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PUT /coach/members/{memberId}/training-plans/{planId}

`TrainingPlanController#update` → TrainingPlanService#update. Tổng 6 file test tham chiếu.

DTO: [TrainingPlanResponse](../../src/main/java/com/scms/backend/training/plan/TrainingPlanResponse.java#L11); [UpdateTrainingPlanRequest](../../src/main/java/com/scms/backend/training/plan/UpdateTrainingPlanRequest.java#L10).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanControllerTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanControllerTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /coach/members/{memberId}/training-plans/{planId}/archive

`TrainingPlanController#archive` → TrainingPlanService#archive. Tổng 6 file test tham chiếu.

DTO: [TrainingPlanResponse](../../src/main/java/com/scms/backend/training/plan/TrainingPlanResponse.java#L11).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanControllerTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanControllerTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /coach/members/{memberId}/training-plans/{planId}/versions

`TrainingPlanController#versions` → TrainingPlanService#versions. Tổng 6 file test tham chiếu.

DTO: [TrainingPlanVersionResponse](../../src/main/java/com/scms/backend/training/plan/TrainingPlanVersionResponse.java#L9).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanControllerTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanControllerTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /coach/members/{memberId}/training-results

`TrainingResultController#list` → TrainingResultService#list. Tổng 5 file test tham chiếu.

DTO: [TrainingResultPageResponse](../../src/main/java/com/scms/backend/training/result/TrainingResultPageResponse.java#L5).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultControllerTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /coach/members/{memberId}/training-results

`TrainingResultController#create` → TrainingResultService#create. Tổng 5 file test tham chiếu.

DTO: [PersonalTrainingResultRequest](../../src/main/java/com/scms/backend/training/result/PersonalTrainingResultRequest.java#L8); [TrainingResultResponse](../../src/main/java/com/scms/backend/training/result/TrainingResultResponse.java#L8).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultControllerTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PUT /coach/members/{memberId}/training-results/{resultId}

`TrainingResultController#update` → TrainingResultService#update. Tổng 5 file test tham chiếu.

DTO: [PersonalTrainingResultRequest](../../src/main/java/com/scms/backend/training/result/PersonalTrainingResultRequest.java#L8); [TrainingResultResponse](../../src/main/java/com/scms/backend/training/result/TrainingResultResponse.java#L8).

- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultControllerTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /coach/pt-sessions

`PtSessionController#coach` → ClassSessionService#staffPtSchedule. Tổng 24 file test tham chiếu.

DTO: [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /coach/pt-sessions/{sessionId}/members/{memberId}/training-summary

`BookedSessionTrainingController#pt` → BookedSessionTrainingService#get. Tổng 3 file test tham chiếu.

DTO: [BookedSessionTrainingService.Summary](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingService.java#L150).

- [BookedSessionTrainingControllerTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingControllerTests.java)
- [BookedSessionTrainingIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java)
- [BookedSessionTrainingServiceTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java)

## PUT /coach/pt-sessions/{sessionId}/members/{memberId}/training-summary

`BookedSessionTrainingController#savePt` → BookedSessionTrainingService#save. Tổng 3 file test tham chiếu.

DTO: [BookedSessionTrainingService.Summary](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingService.java#L150); [Request](../../src/main/java/com/scms/backend/training/session/BookedSessionTrainingController.java#L44).

- [BookedSessionTrainingControllerTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingControllerTests.java)
- [BookedSessionTrainingIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java)
- [BookedSessionTrainingServiceTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java)

## GET /course-payments/reconciliation-queue

`CourseReconciliationController#queue` → CourseReconciliationService#queue. Tổng 1 file test tham chiếu.

DTO: [Page](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationModels.java#L27); [ReconciliationQueueStatus](../../src/main/java/com/scms/backend/membership/ReconciliationQueueStatus.java#L3).

- [CourseReconciliationValidationTests.java](../../src/test/java/com/scms/backend/membership/payment/CourseReconciliationValidationTests.java)

## GET /course-payments/{paymentId}

`CourseReconciliationController#detail` → CourseReconciliationService#detail. Tổng 1 file test tham chiếu.

DTO: [Detail](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationModels.java#L19).

- [CourseReconciliationValidationTests.java](../../src/test/java/com/scms/backend/membership/payment/CourseReconciliationValidationTests.java)

## POST /course-payments/{paymentId}/reconcile

`CourseReconciliationController#reconcile` → CourseReconciliationService#reconcile. Tổng 1 file test tham chiếu.

DTO: [PaymentActionRequest](../../src/main/java/com/scms/backend/membership/payment/PaymentActionRequest.java#L5); [Result](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationModels.java#L15).

- [CourseReconciliationValidationTests.java](../../src/test/java/com/scms/backend/membership/payment/CourseReconciliationValidationTests.java)

## GET /course-payments/{paymentId}/result

`CourseReconciliationController#result` → CourseReconciliationService#result. Tổng 1 file test tham chiếu.

DTO: [Result](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationModels.java#L15).

- [CourseReconciliationValidationTests.java](../../src/test/java/com/scms/backend/membership/payment/CourseReconciliationValidationTests.java)

## GET /course-runs

`CourseCatalogController#listRuns` → CourseCatalogService#listRuns. Tổng 3 file test tham chiếu.

DTO: [CatalogCourseRunPageResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunPageResponse.java#L8).

- [CourseCatalogControllerTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogControllerTests.java)
- [CourseCatalogIntegrationTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogIntegrationTests.java)
- [CourseCatalogServiceTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogServiceTests.java)

## GET /course-runs/{runId}

`CourseCatalogController#getRun` → CourseCatalogService#getRun. Tổng 3 file test tham chiếu.

DTO: [CatalogCourseRunDetailResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunDetailResponse.java#L11).

- [CourseCatalogControllerTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogControllerTests.java)
- [CourseCatalogIntegrationTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogIntegrationTests.java)
- [CourseCatalogServiceTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogServiceTests.java)

## DELETE /course-runs/{runId}/waitlist

`CourseWaitlistController#leave` → CourseWaitlistService#leave. Tổng 5 file test tham chiếu.

DTO: [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## POST /course-runs/{runId}/waitlist

`CourseWaitlistController#join` → CourseWaitlistService#join. Tổng 5 file test tham chiếu.

DTO: [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## POST /course-waitlists/{waitlistId}/accept

`CourseWaitlistController#accept` → CourseWaitlistService#accept. Tổng 5 file test tham chiếu.

DTO: [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## GET /courses

`CourseCatalogController#listCourses` → CourseCatalogService#listCourses. Tổng 3 file test tham chiếu.

DTO: [CatalogCoursePageResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCoursePageResponse.java#L7).

- [CourseCatalogControllerTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogControllerTests.java)
- [CourseCatalogIntegrationTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogIntegrationTests.java)
- [CourseCatalogServiceTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogServiceTests.java)

## GET /courses/{courseId}

`CourseCatalogController#getCourse` → CourseCatalogService#getCourse. Tổng 3 file test tham chiếu.

DTO: [CatalogCourseResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseResponse.java#L7).

- [CourseCatalogControllerTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogControllerTests.java)
- [CourseCatalogIntegrationTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogIntegrationTests.java)
- [CourseCatalogServiceTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogServiceTests.java)

## GET /manager/audit-events

`AuditHistoryController#list` → AuditHistoryService#list. Tổng 3 file test tham chiếu.

DTO: [AuditHistoryPageResponse](../../src/main/java/com/scms/backend/audit/AuditHistoryPageResponse.java#L5).

- [AuditHistoryControllerTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryControllerTests.java)
- [AuditHistoryIntegrationTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryIntegrationTests.java)
- [AuditHistoryServiceTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryServiceTests.java)

## GET /manager/class-sessions

`ClassSessionController#list` → ClassSessionService#list. Tổng 24 file test tham chiếu.

DTO: [ClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionPageResponse.java#L5); [ClassSessionStatus](../../src/main/java/com/scms/backend/scheduling/ClassSessionStatus.java#L3).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## POST /manager/class-sessions

`ClassSessionController#create` → ClassSessionService#create. Tổng 24 file test tham chiếu.

DTO: [ClassSessionCreateRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCreateRequest.java#L6); [ClassSessionResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResponse.java#L8).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## POST /manager/class-sessions/bulk-cancel

`ClassSessionController#bulkCancel` → ClassSessionCancellationService#cancelBulk. Tổng 7 file test tham chiếu.

DTO: [ClassSessionCancellationBatchRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationBatchRequest.java#L7); [ClassSessionCancellationBatchResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationBatchResponse.java#L6).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [ClassSessionCancellationServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)

## POST /manager/class-sessions/cancellation-preview

`ClassSessionController#cancellationPreview` → ClassSessionCancellationService#preview. Tổng 7 file test tham chiếu.

DTO: [ClassSessionCancellationBatchRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationBatchRequest.java#L7); [ClassSessionCancellationPreviewResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationPreviewResponse.java#L6).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [ClassSessionCancellationServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)

## GET /manager/class-sessions/{id}

`ClassSessionController#get` → ClassSessionService#get. Tổng 24 file test tham chiếu.

DTO: [ClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionDetailResponse.java#L12).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## PATCH /manager/class-sessions/{id}/assignment

`ClassSessionController#updateAssignment` → ClassSessionService#updateAssignment. Tổng 24 file test tham chiếu.

DTO: [ClassSessionAssignmentRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionAssignmentRequest.java#L5); [ClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionDetailResponse.java#L12).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /manager/class-sessions/{id}/available-coaches

`ClassSessionController#availableCoaches` → ClassSessionService#availableCoaches. Tổng 24 file test tham chiếu.

DTO: [AvailableCoachResponse](../../src/main/java/com/scms/backend/scheduling/session/AvailableCoachResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## PATCH /manager/class-sessions/{id}/cancel

`ClassSessionController#cancel` → ClassSessionCancellationService#cancel. Tổng 7 file test tham chiếu.

DTO: [ClassSessionCancellationRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationRequest.java#L3); [ClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionDetailResponse.java#L12).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [ClassSessionCancellationServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)

## PATCH /manager/class-sessions/{id}/schedule

`ClassSessionController#reschedule` → ClassSessionService#reschedule. Tổng 24 file test tham chiếu.

DTO: [ClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionDetailResponse.java#L12); [ClassSessionScheduleRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionScheduleRequest.java#L6).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /manager/class-sessions/{sessionId}/waitlist

`WaitlistController#manager` → WaitlistService#sessionList. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## GET /manager/classes

`SportClassController#list` → SportClassService#list. Tổng 3 file test tham chiếu.

DTO: [SportClassResponse](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassResponse.java#L8).

- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)

## POST /manager/classes

`SportClassController#create` → SportClassService#create. Tổng 3 file test tham chiếu.

DTO: [SportClassCreateRequest](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassCreateRequest.java#L6); [SportClassResponse](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassResponse.java#L8).

- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)

## GET /manager/classes/types

`SportClassController#supportedTypes` → SportClassService#supportedTypes. Tổng 3 file test tham chiếu.

DTO: [SportClassType](../../src/main/java/com/scms/backend/scheduling/SportClassType.java#L3).

- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)

## GET /manager/classes/{classId}

`SportClassController#get` → SportClassService#get. Tổng 3 file test tham chiếu.

DTO: [SportClassResponse](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassResponse.java#L8).

- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)

## PATCH /manager/classes/{classId}

`SportClassController#update` → SportClassService#update. Tổng 3 file test tham chiếu.

DTO: [SportClassPatchRequest](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassPatchRequest.java#L6); [SportClassResponse](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassResponse.java#L8).

- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)

## GET /manager/coach-assignments/candidates

`AssignmentDiscoveryController#candidates` → AssignmentDiscoveryService#candidates. Tổng 4 file test tham chiếu.

DTO: [AssignmentCandidatePageResponse](../../src/main/java/com/scms/backend/training/assignment/AssignmentCandidatePageResponse.java#L5).

- [AssignmentDiscoveryControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryControllerTests.java)
- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [AssignmentDiscoveryServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /manager/coach-leave-requests "!page"

`CoachLeaveController#list` → CoachLeaveService#managerList. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9); [CoachLeaveStatus](../../src/main/java/com/scms/backend/scheduling/CoachLeaveStatus.java#L3).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## GET /manager/coach-leave-requests "page"

`CoachLeaveController#page` → CoachLeaveService#managerPage. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9); [CoachLeaveStatus](../../src/main/java/com/scms/backend/scheduling/CoachLeaveStatus.java#L3); [Page](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationModels.java#L27).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## GET /manager/coach-leave-requests/{requestId}/affected-sessions/{type}/{sessionId}/available-coaches

`CoachLeaveImpactController#available` → CoachLeaveImpactService#available. Tổng 3 file test tham chiếu.

DTO: [AvailableCoachResponse](../../src/main/java/com/scms/backend/scheduling/session/AvailableCoachResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveImpactControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactControllerTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)

## PATCH /manager/coach-leave-requests/{requestId}/approve

`CoachLeaveController#approve` → CoachLeaveService#approve. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9); [CoachLeaveReviewRequest](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveReviewRequest.java#L3).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## POST /manager/coach-leave-requests/{requestId}/batch

`CoachLeaveBatchController#resolve` → CoachLeaveBatchService#resolve. Tổng 3 file test tham chiếu.

DTO: [CoachLeaveBatchService.Command](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L54); [CoachLeaveBatchService.Result](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L57).

- [CoachLeaveBatchControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchControllerTests.java)
- [CoachLeaveBatchRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchRollbackIntegrationTests.java)
- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)

## POST /manager/coach-leave-requests/{requestId}/batch/available-coaches

`CoachLeaveBatchController#available` → CoachLeaveBatchService#available. Tổng 3 file test tham chiếu.

DTO: [AvailableCoachResponse](../../src/main/java/com/scms/backend/scheduling/session/AvailableCoachResponse.java#L5); [CoachLeaveBatchService.Command](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L54).

- [CoachLeaveBatchControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchControllerTests.java)
- [CoachLeaveBatchRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchRollbackIntegrationTests.java)
- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)

## GET /manager/coach-leave-requests/{requestId}/impact

`CoachLeaveImpactController#detail` → CoachLeaveImpactService#detail. Tổng 3 file test tham chiếu.

DTO: [CoachLeaveImpactService.Impact](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L52).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveImpactControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactControllerTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)

## PATCH /manager/coach-leave-requests/{requestId}/reject

`CoachLeaveController#reject` → CoachLeaveService#reject. Tổng 10 file test tham chiếu.

DTO: [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9); [CoachLeaveReviewRequest](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveReviewRequest.java#L3).

- [CoachLeaveCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationControllerTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryControllerTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityControllerTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveImpactServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactServiceTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)

## GET /manager/coaches/assignment-workloads

`AssignmentDiscoveryController#workloads` → AssignmentDiscoveryService#workloads. Tổng 4 file test tham chiếu.

DTO: [CoachAssignmentWorkloadResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentWorkloadResponse.java#L5).

- [AssignmentDiscoveryControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryControllerTests.java)
- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [AssignmentDiscoveryServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /manager/coaches/available

`CoachAvailabilityController#available` → ClassSessionService#availableCoaches. Tổng 24 file test tham chiếu.

DTO: [AvailableCoachResponse](../../src/main/java/com/scms/backend/scheduling/session/AvailableCoachResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /manager/course-approval-requests

`CourseApprovalController#queue` → CourseManagementService#listCourseApprovals. Tổng 11 file test tham chiếu.

DTO: [CourseApprovalPageResponse](../../src/main/java/com/scms/backend/course/management/CourseApprovalPageResponse.java#L6).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/course-approval-requests/{courseId}

`CourseApprovalController#detail` → CourseManagementService#getCourseApproval. Tổng 11 file test tham chiếu.

DTO: [CourseApprovalResponse](../../src/main/java/com/scms/backend/course/management/CourseApprovalResponse.java#L8).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-approval-requests/{courseId}/approve

`CourseApprovalController#approve` → CourseManagementService#approveCourse. Tổng 11 file test tham chiếu.

DTO: [CourseApprovalDecision](../../src/main/java/com/scms/backend/course/management/CourseApprovalDecision.java#L8); [CourseApprovalResponse](../../src/main/java/com/scms/backend/course/management/CourseApprovalResponse.java#L8).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-approval-requests/{courseId}/reject

`CourseApprovalController#reject` → CourseManagementService#rejectCourse. Tổng 11 file test tham chiếu.

DTO: [CourseApprovalDecision](../../src/main/java/com/scms/backend/course/management/CourseApprovalDecision.java#L8); [CourseApprovalResponse](../../src/main/java/com/scms/backend/course/management/CourseApprovalResponse.java#L8).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/course-refund-obligations

`CourseRefundObligationController#list` → CourseRefundObligationService#list. Tổng 3 file test tham chiếu.

DTO: [RefundObligationPage](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L29).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /manager/course-refund-obligations/{obligationId}

`CourseRefundObligationController#detail` → CourseRefundObligationService#detail. Tổng 3 file test tham chiếu.

DTO: [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-refund-obligations/{obligationId}/approve

`CourseRefundObligationController#approve` → CourseRefundObligationService#approve. Tổng 3 file test tham chiếu.

DTO: [RefundDecision](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L14); [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-refund-obligations/{obligationId}/fail

`CourseRefundObligationController#fail` → CourseRefundObligationService#fail. Tổng 3 file test tham chiếu.

DTO: [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26); [RefundTerminal](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L23).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-refund-obligations/{obligationId}/reject

`CourseRefundObligationController#reject` → CourseRefundObligationService#reject. Tổng 3 file test tham chiếu.

DTO: [RefundDecision](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L14); [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-refund-obligations/{obligationId}/return

`CourseRefundObligationController#returned` → CourseRefundObligationService#returned. Tổng 3 file test tham chiếu.

DTO: [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26); [RefundTerminal](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L23).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-refund-obligations/{obligationId}/verify

`CourseRefundObligationController#verify` → CourseRefundObligationService#verify. Tổng 3 file test tham chiếu.

DTO: [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26); [RefundVerification](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /manager/course-runs

`CourseController#listRuns` → CourseManagementService#listRuns. Tổng 11 file test tham chiếu.

DTO: [CourseRunPageResponse](../../src/main/java/com/scms/backend/course/run/CourseRunPageResponse.java#L6).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs

`CourseController#createRun` → CourseManagementService#createRun. Tổng 11 file test tham chiếu.

DTO: [CourseRunCreateRequest](../../src/main/java/com/scms/backend/course/run/CourseRunCreateRequest.java#L12); [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs/preview-schedule

`CourseWeeklyRunController#preview` → CourseWeeklyRunService#preview. Tổng 3 file test tham chiếu.

DTO: [CourseWeeklyPreview](../../src/main/java/com/scms/backend/course/run/CourseWeeklyPreview.java#L6); [CourseWeeklyScheduleRequest](../../src/main/java/com/scms/backend/course/run/CourseWeeklyScheduleRequest.java#L12).

- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseWeeklyRunControllerTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunControllerTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)

## POST /manager/course-runs/resource-availability

`CourseResourceAvailabilityController#available` → AccountRepository#existsByIdAndRoleAndStatus; CourseSessionRepository#existsById; CourseResourceAvailabilityRepository#availableFindClassSessions; AccountRepository#findByRoleAndStatusOrderById; RoomRepository#findAll. Tổng 144 file test tham chiếu.

DTO: [Request](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityController.java#L54); [Response](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityController.java#L63).

- [AuditHistoryIntegrationTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryIntegrationTests.java)
- [AuditHistoryServiceTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryServiceTests.java)
- [AccountIdentifierAvailabilityTests.java](../../src/test/java/com/scms/backend/auth/AccountIdentifierAvailabilityTests.java)
- [ActiveAccountJwtValidatorTests.java](../../src/test/java/com/scms/backend/auth/ActiveAccountJwtValidatorTests.java)
- [AuthenticationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java)
- [AuthenticationServiceTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationServiceTests.java)
- [ChangePasswordIntegrationTests.java](../../src/test/java/com/scms/backend/auth/ChangePasswordIntegrationTests.java)
- [RegistrationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/RegistrationIntegrationTests.java)
- [RegistrationServiceTests.java](../../src/test/java/com/scms/backend/auth/RegistrationServiceTests.java)
- [CenterVisitIntegrationTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitIntegrationTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)
- [CourseCatalogRepositoryContractTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogRepositoryContractTests.java)
- [CourseCatalogServiceTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseCatalogServiceTests.java)
- [CourseImageControllerTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseImageControllerTests.java)
- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [ReceptionCourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentServiceTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseLeaveCancellationTests.java](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseResourceAvailabilityTests.java](../../src/test/java/com/scms/backend/course/session/CourseResourceAvailabilityTests.java)
- [CourseSessionServiceTests.java](../../src/test/java/com/scms/backend/course/session/CourseSessionServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)
- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [PasswordResetIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java)
- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)
- [FoundationIntegrationTests.java](../../src/test/java/com/scms/backend/FoundationIntegrationTests.java)
- [MemberProfileIntegrationTests.java](../../src/test/java/com/scms/backend/identity/memberprofile/MemberProfileIntegrationTests.java)
- [MemberProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/memberprofile/MemberProfileServiceTests.java)
- [ReceptionMemberIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberIntegrationTests.java)
- [ReceptionMemberMembershipSummaryIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberMembershipSummaryIntegrationTests.java)
- [ReceptionMemberProfileIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileIntegrationTests.java)
- [ReceptionMemberSearchServiceTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchServiceTests.java)
- [StaffProfileIntegrationTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)
- [ManagerAccountIntegrationTests.java](../../src/test/java/com/scms/backend/manager/ManagerAccountIntegrationTests.java)
- [ManagerMemberServiceTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberServiceTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)
- [OperationalIntegrityIntegrationTests.java](../../src/test/java/com/scms/backend/manager/OperationalIntegrityIntegrationTests.java)
- [ResetPasswordServiceTests.java](../../src/test/java/com/scms/backend/manager/ResetPasswordServiceTests.java)
- [StaffAccountCreateServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountCreateServiceTests.java)
- [StaffAccountServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountServiceTests.java)
- [StaffStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffStatusServiceTests.java)
- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [MembershipFreezeExecutionServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionServiceTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)
- [MembershipFulfillmentModeTests.java](../../src/test/java/com/scms/backend/membership/MembershipFulfillmentModeTests.java)
- [MembershipLifecycleServiceTests.java](../../src/test/java/com/scms/backend/membership/MembershipLifecycleServiceTests.java)
- [MembershipOfferAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminServiceTests.java)
- [MembershipOfferAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAuditRollbackIntegrationTests.java)
- [MembershipOfferDetailIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferDetailIntegrationTests.java)
- [MembershipOfferIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferIntegrationTests.java)
- [MembershipOrderIntegrationTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderIntegrationTests.java)
- [MembershipOrderServiceTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderServiceTests.java)
- [PaymentEvidenceServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentEvidenceServiceTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)
- [MembershipTerminationExecutionServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationExecutionServiceTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)
- [NotificationCenterIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationCenterIntegrationTests.java)
- [NotificationDurabilityIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationDurabilityIntegrationTests.java)
- [NotificationServiceTests.java](../../src/test/java/com/scms/backend/notification/NotificationServiceTests.java)
- [AttendanceServiceTests.java](../../src/test/java/com/scms/backend/scheduling/attendance/AttendanceServiceTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [CoachLeaveBatchRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchRollbackIntegrationTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [DisciplineServiceTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineServiceTests.java)
- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [ClassSessionCancellationServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachTeachingScheduleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachTeachingScheduleIntegrationTests.java)
- [CoachTeachingScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachTeachingScheduleServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleIntegrationTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SessionLifecycleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleIntegrationTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)
- [InitialManagerProvisionerTests.java](../../src/test/java/com/scms/backend/support/InitialManagerProvisionerTests.java)
- [InitialManagerProvisioningIntegrationTests.java](../../src/test/java/com/scms/backend/support/InitialManagerProvisioningIntegrationTests.java)
- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [AssignmentDiscoveryServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryServiceTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignedMemberServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberServiceTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [MemberCoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/MemberCoachAssignmentServiceTests.java)
- [MemberTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/MemberTrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackServiceTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [MemberTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/MemberTrainingResultIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultServiceTests.java)
- [BookedSessionTrainingServiceTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java)
- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackServiceTests.java)
- [SessionTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultIntegrationTests.java)
- [SessionTrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /manager/course-runs/with-schedule

`CourseWeeklyRunController#create` → CourseWeeklyRunService#create. Tổng 3 file test tham chiếu.

DTO: [CourseWeeklyRunCreateRequest](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunCreateRequest.java#L13); [CourseWeeklyRunResponse](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunResponse.java#L6).

- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseWeeklyRunControllerTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunControllerTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)

## GET /manager/course-runs/{runId}

`CourseController#getRun` → CourseManagementService#getRun. Tổng 11 file test tham chiếu.

DTO: [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## PATCH /manager/course-runs/{runId}

`CourseController#patchRun` → CourseManagementService#patchRun. Tổng 11 file test tham chiếu.

DTO: [CourseRunPatchRequest](../../src/main/java/com/scms/backend/course/run/CourseRunPatchRequest.java#L7); [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs/{runId}/cancel

`CourseController#cancelRun` → CourseManagementService#cancelRun. Tổng 11 file test tham chiếu.

DTO: [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9); [VersionRequest](../../src/main/java/com/scms/backend/course/VersionRequest.java#L7).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/course-runs/{runId}/cancellation-impact

`CourseController#cancellationImpact` → CourseManagementService#cancellationImpact. Tổng 11 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs/{runId}/open-registration

`CourseController#openRun` → CourseManagementService#openRun. Tổng 11 file test tham chiếu.

DTO: [CourseRunOpenRequest](../../src/main/java/com/scms/backend/course/run/CourseRunOpenRequest.java#L8); [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs/{runId}/reschedule

`CourseController#rescheduleRun` → CourseRunRescheduleService#reschedule. Tổng 2 file test tham chiếu.

DTO: [CourseRunRescheduleRequest](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRequest.java#L11); [CourseRunRescheduleResponse](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleResponse.java#L8).

- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## PATCH /manager/course-runs/{runId}/schedule

`CourseController#patchRunSchedule` → CourseManagementService#patchRunSchedule. Tổng 11 file test tham chiếu.

DTO: [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9); [CourseRunScheduleRequest](../../src/main/java/com/scms/backend/course/run/CourseRunScheduleRequest.java#L9).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/course-runs/{runId}/sessions

`CourseController#listSessions` → CourseManagementService#listSessions. Tổng 11 file test tham chiếu.

DTO: [CourseSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L10).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs/{runId}/sessions

`CourseController#createSession` → CourseManagementService#createSession. Tổng 11 file test tham chiếu.

DTO: [CourseSessionCreateRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionCreateRequest.java#L10); [CourseSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L10).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/course-runs/{runId}/sessions/{sessionId}

`CourseController#getSession` → CourseManagementService#getSession. Tổng 11 file test tham chiếu.

DTO: [CourseSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L10).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## PATCH /manager/course-runs/{runId}/sessions/{sessionId}

`CourseController#patchSession` → CourseManagementService#patchSession. Tổng 11 file test tham chiếu.

DTO: [CourseSessionPatchRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionPatchRequest.java#L8); [CourseSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L10).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-runs/{runId}/sessions/{sessionId}/cancel

`CourseController#cancelSession` → CourseSessionDisruptionService#cancelFromEndpoint. Tổng 3 file test tham chiếu.

DTO: [CourseSessionDisruptionRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRequest.java#L6); [CourseSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L10).

- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseLeaveCancellationTests.java](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-runs/{runId}/sessions/{sessionId}/cancel-with-makeup

`CourseController#cancelWithMakeup` → CourseSessionDisruptionService#cancelWithMakeup. Tổng 3 file test tham chiếu.

DTO: [CourseSessionCancelWithMakeupRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionCancelWithMakeupRequest.java#L8); [CourseSessionDisruptionService.CourseMakeupResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L227).

- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseLeaveCancellationTests.java](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## POST /manager/course-runs/{runId}/sessions/{sessionId}/makeup

`CourseController#scheduleMakeup` → CourseSessionDisruptionService#scheduleMakeup. Tổng 3 file test tham chiếu.

DTO: [CourseMakeupRequest](../../src/main/java/com/scms/backend/course/session/CourseMakeupRequest.java#L8); [CourseSessionDisruptionService.CourseMakeupResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L227).

- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseLeaveCancellationTests.java](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /manager/course-transfer-requests

`CourseTransferController#queue` → CourseTransferService#managerQueue. Tổng 2 file test tham chiếu.

DTO: [TransferRequest](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L13).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## POST /manager/course-transfer-requests/{requestId}/approve

`CourseTransferController#approve` → CourseTransferService#approve. Tổng 2 file test tham chiếu.

DTO: [ApproveTransfer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L16); [TransferOffer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L22).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## POST /manager/course-transfer-requests/{requestId}/reject

`CourseTransferController#reject` → CourseTransferService#reject. Tổng 2 file test tham chiếu.

DTO: [RejectTransfer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L19); [TransferRequest](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L13).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## GET /manager/course-transfer-requests/{requestId}/targets

`CourseTransferController#targets` → CourseTransferService#eligibleTargets. Tổng 2 file test tham chiếu.

DTO: [TransferTarget](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L25).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## GET /manager/course-withdrawal-requests

`CourseWithdrawalController#managerQueue` → CourseWithdrawalService#managerQueue. Tổng 3 file test tham chiếu.

DTO: [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/course-withdrawal-requests/{requestId}

`CourseWithdrawalController#managerDetail` → CourseWithdrawalService#managerDetail. Tổng 3 file test tham chiếu.

DTO: [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-withdrawal-requests/{requestId}/approve

`CourseWithdrawalController#approve` → CourseWithdrawalService#approve. Tổng 3 file test tham chiếu.

DTO: [WithdrawalDecision](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L17); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/course-withdrawal-requests/{requestId}/reject

`CourseWithdrawalController#reject` → CourseWithdrawalService#reject. Tổng 3 file test tham chiếu.

DTO: [WithdrawalDecision](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L17); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/courses

`CourseController#listCourses` → CourseManagementService#listCourses. Tổng 11 file test tham chiếu.

DTO: [CoursePageResponse](../../src/main/java/com/scms/backend/course/management/CoursePageResponse.java#L6).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/courses

`CourseController#createCourse` → CourseManagementService#createCourse. Tổng 11 file test tham chiếu.

DTO: [CourseCreateRequest](../../src/main/java/com/scms/backend/course/management/CourseCreateRequest.java#L11); [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/courses/images

`CourseImageController#upload` → AccountRepository#existsByIdAndRoleAndStatus. Tổng 142 file test tham chiếu.

DTO: [ImageResponse](../../src/main/java/com/scms/backend/course/catalog/CourseImageController.java#L39).

- [AuditHistoryIntegrationTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryIntegrationTests.java)
- [AuditHistoryServiceTests.java](../../src/test/java/com/scms/backend/audit/AuditHistoryServiceTests.java)
- [AccountIdentifierAvailabilityTests.java](../../src/test/java/com/scms/backend/auth/AccountIdentifierAvailabilityTests.java)
- [ActiveAccountJwtValidatorTests.java](../../src/test/java/com/scms/backend/auth/ActiveAccountJwtValidatorTests.java)
- [AuthenticationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java)
- [AuthenticationServiceTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationServiceTests.java)
- [ChangePasswordIntegrationTests.java](../../src/test/java/com/scms/backend/auth/ChangePasswordIntegrationTests.java)
- [RegistrationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/RegistrationIntegrationTests.java)
- [RegistrationServiceTests.java](../../src/test/java/com/scms/backend/auth/RegistrationServiceTests.java)
- [CenterVisitIntegrationTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitIntegrationTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)
- [CourseImageControllerTests.java](../../src/test/java/com/scms/backend/course/catalog/CourseImageControllerTests.java)
- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [ReceptionCourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentServiceTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseLeaveCancellationTests.java](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseResourceAvailabilityTests.java](../../src/test/java/com/scms/backend/course/session/CourseResourceAvailabilityTests.java)
- [CourseSessionServiceTests.java](../../src/test/java/com/scms/backend/course/session/CourseSessionServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)
- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [PasswordResetIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java)
- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)
- [FoundationIntegrationTests.java](../../src/test/java/com/scms/backend/FoundationIntegrationTests.java)
- [MemberProfileIntegrationTests.java](../../src/test/java/com/scms/backend/identity/memberprofile/MemberProfileIntegrationTests.java)
- [MemberProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/memberprofile/MemberProfileServiceTests.java)
- [ReceptionMemberIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberIntegrationTests.java)
- [ReceptionMemberMembershipSummaryIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberMembershipSummaryIntegrationTests.java)
- [ReceptionMemberProfileIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileIntegrationTests.java)
- [ReceptionMemberSearchServiceTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchServiceTests.java)
- [StaffProfileIntegrationTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)
- [ManagerAccountIntegrationTests.java](../../src/test/java/com/scms/backend/manager/ManagerAccountIntegrationTests.java)
- [ManagerMemberServiceTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberServiceTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)
- [OperationalIntegrityIntegrationTests.java](../../src/test/java/com/scms/backend/manager/OperationalIntegrityIntegrationTests.java)
- [ResetPasswordServiceTests.java](../../src/test/java/com/scms/backend/manager/ResetPasswordServiceTests.java)
- [StaffAccountCreateServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountCreateServiceTests.java)
- [StaffAccountServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountServiceTests.java)
- [StaffStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffStatusServiceTests.java)
- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [MembershipFreezeExecutionServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionServiceTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)
- [MembershipFulfillmentModeTests.java](../../src/test/java/com/scms/backend/membership/MembershipFulfillmentModeTests.java)
- [MembershipLifecycleServiceTests.java](../../src/test/java/com/scms/backend/membership/MembershipLifecycleServiceTests.java)
- [MembershipOfferAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminServiceTests.java)
- [MembershipOfferAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAuditRollbackIntegrationTests.java)
- [MembershipOfferDetailIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferDetailIntegrationTests.java)
- [MembershipOfferIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferIntegrationTests.java)
- [MembershipOrderIntegrationTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderIntegrationTests.java)
- [MembershipOrderServiceTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderServiceTests.java)
- [PaymentEvidenceServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentEvidenceServiceTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)
- [MembershipTerminationExecutionServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationExecutionServiceTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)
- [NotificationCenterIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationCenterIntegrationTests.java)
- [NotificationDurabilityIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationDurabilityIntegrationTests.java)
- [NotificationServiceTests.java](../../src/test/java/com/scms/backend/notification/NotificationServiceTests.java)
- [AttendanceServiceTests.java](../../src/test/java/com/scms/backend/scheduling/attendance/AttendanceServiceTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [CoachLeaveBatchRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchRollbackIntegrationTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [CoachLeaveExpiryIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveExpiryIntegrationTests.java)
- [CoachLeaveIdentityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveIdentityIntegrationTests.java)
- [CoachLeaveNotificationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveNotificationIntegrationTests.java)
- [CoachLeaveServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [DisciplineServiceTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineServiceTests.java)
- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [ClassSessionCancellationServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachTeachingScheduleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachTeachingScheduleIntegrationTests.java)
- [CoachTeachingScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachTeachingScheduleServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleIntegrationTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SessionLifecycleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleIntegrationTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)
- [SportClassServiceTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)
- [InitialManagerProvisionerTests.java](../../src/test/java/com/scms/backend/support/InitialManagerProvisionerTests.java)
- [InitialManagerProvisioningIntegrationTests.java](../../src/test/java/com/scms/backend/support/InitialManagerProvisioningIntegrationTests.java)
- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [AssignmentDiscoveryServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryServiceTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignedMemberServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberServiceTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [MemberCoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/MemberCoachAssignmentServiceTests.java)
- [MemberTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/MemberTrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackServiceTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingPlanServiceTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanServiceTests.java)
- [MemberTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/MemberTrainingResultIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultServiceTests.java)
- [BookedSessionTrainingServiceTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java)
- [SessionTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackIntegrationTests.java)
- [SessionTrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingFeedbackServiceTests.java)
- [SessionTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultIntegrationTests.java)
- [SessionTrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/session/SessionTrainingResultServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /manager/courses/{courseId}

`CourseController#getCourse` → CourseManagementService#getCourse. Tổng 11 file test tham chiếu.

DTO: [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## PATCH /manager/courses/{courseId}

`CourseController#patchCourse` → CourseManagementService#patchCourse. Tổng 11 file test tham chiếu.

DTO: [CoursePatchRequest](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L13); [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/courses/{courseId}/archive

`CourseController#archiveCourse` → CourseManagementService#archiveCourse. Tổng 11 file test tham chiếu.

DTO: [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8); [VersionRequest](../../src/main/java/com/scms/backend/course/VersionRequest.java#L7).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/courses/{courseId}/deactivate

`CourseController#deactivateCourse` → CourseManagementService#deactivateCourse. Tổng 11 file test tham chiếu.

DTO: [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8); [VersionRequest](../../src/main/java/com/scms/backend/course/VersionRequest.java#L7).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /manager/courses/{courseId}/publish

`CourseController#publishCourse` → CourseManagementService#publishCourse. Tổng 11 file test tham chiếu.

DTO: [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8); [VersionRequest](../../src/main/java/com/scms/backend/course/VersionRequest.java#L7).

- [CourseApprovalControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalControllerTests.java)
- [CourseApprovalIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseApprovalIntegrationTests.java)
- [CourseControllerTests.java](../../src/test/java/com/scms/backend/course/management/CourseControllerTests.java)
- [CourseManagementIntegrationTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementIntegrationTests.java)
- [CourseManagementServiceTests.java](../../src/test/java/com/scms/backend/course/management/CourseManagementServiceTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWeeklyRunServiceTests.java](../../src/test/java/com/scms/backend/course/run/CourseWeeklyRunServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseCoachSessionIntegrationTests.java](../../src/test/java/com/scms/backend/course/session/CourseCoachSessionIntegrationTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/dashboard

`ManagerReportingController#dashboard` → ManagerReportingService#dashboard. Tổng 4 file test tham chiếu.

DTO: [ManagerDashboardResponse](../../src/main/java/com/scms/backend/membership/reporting/ManagerDashboardResponse.java#L5).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/disciplines

`DisciplineController#list` → DisciplineService#list. Tổng 4 file test tham chiếu.

DTO: [DisciplineResponse](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineResponse.java#L8).

- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [DisciplineServiceTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineServiceTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## POST /manager/disciplines

`DisciplineController#create` → DisciplineService#create. Tổng 4 file test tham chiếu.

DTO: [DisciplineCreateRequest](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineCreateRequest.java#L7); [DisciplineResponse](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineResponse.java#L8).

- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [DisciplineServiceTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineServiceTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## GET /manager/disciplines/{disciplineId}

`DisciplineController#get` → DisciplineService#get. Tổng 4 file test tham chiếu.

DTO: [DisciplineResponse](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineResponse.java#L8).

- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [DisciplineServiceTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineServiceTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## PATCH /manager/disciplines/{disciplineId}

`DisciplineController#update` → DisciplineService#update. Tổng 4 file test tham chiếu.

DTO: [DisciplinePatchRequest](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplinePatchRequest.java#L6); [DisciplineResponse](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineResponse.java#L8).

- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [DisciplineServiceTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineServiceTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## GET /manager/email-verification/manual-activation-requests

`ManagerEmailVerificationController#list` → ManualActivationService#list. Tổng 1 file test tham chiếu.

DTO: [ManualActivationPageResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationPageResponse.java#L5); [ManualActivationStatus](../../src/main/java/com/scms/backend/emailverification/ManualActivationStatus.java#L3).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## GET /manager/email-verification/manual-activation-requests/{requestId}

`ManagerEmailVerificationController#get` → ManualActivationService#getForActor. Tổng 1 file test tham chiếu.

DTO: [ManualActivationResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationResponse.java#L10).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /manager/email-verification/manual-activation-requests/{requestId}/approve

`ManagerEmailVerificationController#approve` → ManualActivationService#approve. Tổng 1 file test tham chiếu.

DTO: [ManualActivationDecisionRequest](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationDecisionRequest.java#L6); [ManualActivationResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationResponse.java#L10).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /manager/email-verification/manual-activation-requests/{requestId}/reject

`ManagerEmailVerificationController#reject` → ManualActivationService#reject. Tổng 1 file test tham chiếu.

DTO: [ManualActivationDecisionRequest](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationDecisionRequest.java#L6); [ManualActivationResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationResponse.java#L10).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## GET /manager/members

`ManagerMemberController#search` → ManagerMemberService#search. Tổng 2 file test tham chiếu.

DTO: [AccountStatus](../../src/main/java/com/scms/backend/account/AccountStatus.java#L3); [MemberAccountPageResponse](../../src/main/java/com/scms/backend/manager/MemberAccountPageResponse.java#L5).

- [ManagerMemberControllerTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberControllerTests.java)
- [ManagerMemberServiceTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberServiceTests.java)

## GET /manager/members/{id}

`ManagerMemberController#get` → ManagerMemberService#get. Tổng 2 file test tham chiếu.

DTO: [MemberAccountResponse](../../src/main/java/com/scms/backend/manager/MemberAccountResponse.java#L8).

- [ManagerMemberControllerTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberControllerTests.java)
- [ManagerMemberServiceTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberServiceTests.java)

## PATCH /manager/members/{id}

`ManagerMemberController#update` → ManagerMemberService#update. Tổng 2 file test tham chiếu.

DTO: [MemberAccountResponse](../../src/main/java/com/scms/backend/manager/MemberAccountResponse.java#L8); [MemberProfilePatchRequest](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L8).

- [ManagerMemberControllerTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberControllerTests.java)
- [ManagerMemberServiceTests.java](../../src/test/java/com/scms/backend/manager/ManagerMemberServiceTests.java)

## POST /manager/members/{memberAccountId}/membership-termination-requests

`MembershipTerminationController#direct` → MembershipTerminationService#createDirect. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationCreateRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationCreateRequest.java#L5); [MembershipTerminationRequestResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L9).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## GET /manager/members/{memberAccountId}/memberships

`PaymentController#managerMembershipHistory` → PaymentService#managerMembershipHistory. Tổng 12 file test tham chiếu.

DTO: [StaffMembershipResponse](../../src/main/java/com/scms/backend/membership/StaffMembershipResponse.java#L9).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## PATCH /manager/members/{memberAccountId}/status

`MemberStatusController#changeStatus` → MemberStatusService#changeStatus. Tổng 2 file test tham chiếu.

DTO: [MemberStatusChangeRequest](../../src/main/java/com/scms/backend/manager/MemberStatusChangeRequest.java#L5); [MemberStatusChangeResponse](../../src/main/java/com/scms/backend/manager/MemberStatusChangeResponse.java#L6).

- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)
- [OperationalIntegrityIntegrationTests.java](../../src/test/java/com/scms/backend/manager/OperationalIntegrityIntegrationTests.java)

## GET /manager/members/{memberId}/coach-assignment

`ManagerCoachAssignmentController#getActive` → CoachAssignmentService#getActive. Tổng 9 file test tham chiếu.

DTO: [CoachAssignmentResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentResponse.java#L10).

- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [ManagerCoachAssignmentControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/ManagerCoachAssignmentControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /manager/members/{memberId}/coach-assignment

`ManagerCoachAssignmentController#assign` → CoachAssignmentService#assign. Tổng 9 file test tham chiếu.

DTO: [CoachAssignmentAssignRequest](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentAssignRequest.java#L8); [CoachAssignmentResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentResponse.java#L10).

- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [ManagerCoachAssignmentControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/ManagerCoachAssignmentControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## PATCH /manager/members/{memberId}/coach-assignment/end

`ManagerCoachAssignmentController#end` → CoachAssignmentService#endAssignment. Tổng 9 file test tham chiếu.

DTO: [CoachAssignmentEndRequest](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentEndRequest.java#L7); [CoachAssignmentResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentResponse.java#L10).

- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [ManagerCoachAssignmentControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/ManagerCoachAssignmentControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /manager/members/{memberId}/coach-assignment/impact

`ManagerCoachAssignmentController#previewImpact` → CoachAssignmentService#previewImpact. Tổng 9 file test tham chiếu.

DTO: [PtAssignmentImpactResponse](../../src/main/java/com/scms/backend/scheduling/session/PtAssignmentImpactResponse.java#L5).

- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [ManagerCoachAssignmentControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/ManagerCoachAssignmentControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /manager/members/{memberId}/coach-assignments

`ManagerCoachAssignmentController#listHistory` → CoachAssignmentService#listHistory. Tổng 9 file test tham chiếu.

DTO: [CoachAssignmentPageResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentPageResponse.java#L5).

- [AssignmentDiscoveryIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/AssignmentDiscoveryIntegrationTests.java)
- [CoachAssignedMemberIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignedMemberIntegrationTests.java)
- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [CoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentServiceTests.java)
- [ManagerCoachAssignmentControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/ManagerCoachAssignmentControllerTests.java)
- [TrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/TrainingFeedbackIntegrationTests.java)
- [TrainingPlanIntegrationTests.java](../../src/test/java/com/scms/backend/training/plan/TrainingPlanIntegrationTests.java)
- [TrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/TrainingResultIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## POST /manager/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests

`CourseWithdrawalController#managerCreate` → CourseWithdrawalService#createForManager. Tổng 3 file test tham chiếu.

DTO: [WithdrawalCreate](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L14); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /manager/membership-benefits

`MembershipPlanAdminController#benefits` → MembershipPlanAdminService#catalog. Tổng 2 file test tham chiếu.

DTO: [MembershipBenefitResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitResponse.java#L9).

- [MembershipPlanAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminControllerTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)

## GET /manager/membership-freeze-requests

`MembershipFreezeController#managerList` → MembershipFreezeService#listForManager. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeRequestPageResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L6).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /manager/membership-freeze-requests/{requestId}/approve

`MembershipFreezeController#approve` → MembershipFreezeService#approve. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeDecisionRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeDecisionRequest.java#L3); [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /manager/membership-freeze-requests/{requestId}/reject

`MembershipFreezeController#reject` → MembershipFreezeService#reject. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeDecisionRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeDecisionRequest.java#L3); [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## GET /manager/membership-offers

`MembershipOfferAdminController#all` → MembershipOfferAdminService#all. Tổng 3 file test tham chiếu.

DTO: [MembershipOfferAdminDetailResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminDetailResponse.java#L10).

- [MembershipOfferAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminControllerTests.java)
- [MembershipOfferAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminServiceTests.java)
- [MembershipOfferAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAuditRollbackIntegrationTests.java)

## POST /manager/membership-offers

`MembershipOfferAdminController#create` → MembershipOfferAdminService#create. Tổng 3 file test tham chiếu.

DTO: [MembershipOfferAdminRequest](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminRequest.java#L6); [MembershipOfferAdminResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminResponse.java#L8).

- [MembershipOfferAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminControllerTests.java)
- [MembershipOfferAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminServiceTests.java)
- [MembershipOfferAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAuditRollbackIntegrationTests.java)

## PATCH /manager/membership-offers/{id}

`MembershipOfferAdminController#update` → MembershipOfferAdminService#update. Tổng 3 file test tham chiếu.

DTO: [MembershipOfferAdminResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminResponse.java#L8); [MembershipOfferPatchRequest](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferPatchRequest.java#L6).

- [MembershipOfferAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminControllerTests.java)
- [MembershipOfferAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminServiceTests.java)
- [MembershipOfferAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAuditRollbackIntegrationTests.java)

## GET /manager/membership-offers/{offerId}

`MembershipOfferAdminController#get` → MembershipOfferAdminService#get. Tổng 3 file test tham chiếu.

DTO: [MembershipOfferAdminDetailResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminDetailResponse.java#L10).

- [MembershipOfferAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminControllerTests.java)
- [MembershipOfferAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAdminServiceTests.java)
- [MembershipOfferAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/membership/offer/MembershipOfferAuditRollbackIntegrationTests.java)

## GET /manager/membership-plans

`MembershipPlanAdminController#list` → MembershipPlanAdminService#list. Tổng 2 file test tham chiếu.

DTO: [MembershipPlanResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanResponse.java#L10).

- [MembershipPlanAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminControllerTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)

## POST /manager/membership-plans

`MembershipPlanAdminController#create` → MembershipPlanAdminService#create. Tổng 2 file test tham chiếu.

DTO: [MembershipPlanCreateRequest](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanCreateRequest.java#L6); [MembershipPlanResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanResponse.java#L10).

- [MembershipPlanAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminControllerTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)

## GET /manager/membership-plans/{planCode}

`MembershipPlanAdminController#get` → MembershipPlanAdminService#get. Tổng 2 file test tham chiếu.

DTO: [MembershipPlanResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanResponse.java#L10).

- [MembershipPlanAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminControllerTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)

## PATCH /manager/membership-plans/{planCode}

`MembershipPlanAdminController#update` → MembershipPlanAdminService#update. Tổng 2 file test tham chiếu.

DTO: [MembershipPlanPatchRequest](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanPatchRequest.java#L6); [MembershipPlanResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanResponse.java#L10).

- [MembershipPlanAdminControllerTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminControllerTests.java)
- [MembershipPlanAdminServiceTests.java](../../src/test/java/com/scms/backend/membership/plan/MembershipPlanAdminServiceTests.java)

## GET /manager/membership-termination-requests

`MembershipTerminationController#managerList` → MembershipTerminationService#listForManager. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationRequestPageResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L6).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## POST /manager/membership-termination-requests/{requestId}/approve

`MembershipTerminationController#approve` → MembershipTerminationService#approve. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationDecisionRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationDecisionRequest.java#L3); [MembershipTerminationRequestResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L9).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## POST /manager/membership-termination-requests/{requestId}/reject

`MembershipTerminationController#reject` → MembershipTerminationService#reject. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationDecisionRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationDecisionRequest.java#L3); [MembershipTerminationRequestResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L9).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## GET /manager/pt-sessions

`PtSessionController#manager` → ClassSessionService#staffPtSchedule. Tổng 24 file test tham chiếu.

DTO: [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## POST /manager/recurring-schedules

`RecurringScheduleController#create` → RecurringScheduleService#create. Tổng 2 file test tham chiếu.

DTO: [RecurringScheduleCreateRequest](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleCreateRequest.java#L9); [RecurringScheduleResponse](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleResponse.java#L6).

- [RecurringScheduleIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleIntegrationTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)

## GET /manager/refund-requests

`MembershipRefundController#managerList` → MembershipRefundService#listForManager. Tổng 5 file test tham chiếu.

DTO: [RefundRequestPageResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestPageResponse.java#L5).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## POST /manager/refund-requests/{requestId}/final-approval

`MembershipRefundController#finalApproval` → MembershipRefundService#finalApproval. Tổng 5 file test tham chiếu.

DTO: [RefundActionRequest](../../src/main/java/com/scms/backend/membership/refund/RefundActionRequest.java#L3); [RefundRequestResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L13).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## POST /manager/refund-requests/{requestId}/first-approval

`MembershipRefundController#firstApproval` → MembershipRefundService#firstApproval. Tổng 5 file test tham chiếu.

DTO: [RefundFirstApprovalRequest](../../src/main/java/com/scms/backend/membership/refund/RefundFirstApprovalRequest.java#L5); [RefundRequestResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L13).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## POST /manager/refund-requests/{requestId}/reject

`MembershipRefundController#reject` → MembershipRefundService#reject. Tổng 5 file test tham chiếu.

DTO: [RefundActionRequest](../../src/main/java/com/scms/backend/membership/refund/RefundActionRequest.java#L3); [RefundRequestResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L13).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## GET /manager/reports/attendance

`ManagerReportingController#attendance` → ManagerReportingService#attendance. Tổng 4 file test tham chiếu.

DTO: [AttendanceReportResponse](../../src/main/java/com/scms/backend/membership/reporting/AttendanceReportResponse.java#L6).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/reports/bookings

`ManagerReportingController#bookings` → ManagerReportingService#bookings. Tổng 4 file test tham chiếu.

DTO: [BookingReportResponse](../../src/main/java/com/scms/backend/membership/reporting/BookingReportResponse.java#L5).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/reports/center-visits

`ManagerReportingController#centerVisits` → ManagerReportingService#centerVisits. Tổng 4 file test tham chiếu.

DTO: [CenterVisitReportResponse](../../src/main/java/com/scms/backend/membership/reporting/CenterVisitReportResponse.java#L5).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/reports/class-utilization

`ManagerReportingController#classUtilization` → ManagerReportingService#classUtilization. Tổng 4 file test tham chiếu.

DTO: [ClassUtilizationReportResponse](../../src/main/java/com/scms/backend/membership/reporting/ClassUtilizationReportResponse.java#L6).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/reports/course-runs

`ManagerReportingController#courseRuns` → ManagerReportingService#courseRuns. Tổng 4 file test tham chiếu.

DTO: [CourseRunReportPageResponse](../../src/main/java/com/scms/backend/membership/reporting/CourseRunReportPageResponse.java#L5); [CourseRunStatus](../../src/main/java/com/scms/backend/course/CourseRunStatus.java#L3).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/reports/members

`ManagerReportingController#members` → ManagerReportingService#members. Tổng 4 file test tham chiếu.

DTO: [MemberReportResponse](../../src/main/java/com/scms/backend/membership/reporting/MemberReportResponse.java#L5).

- [CourseRunReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/CourseRunReportingIntegrationTests.java)
- [ManagerReportingControllerTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingControllerTests.java)
- [ManagerReportingIntegrationTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)

## GET /manager/reports/membership-revenue

`PaymentController#report` → PaymentService#report. Tổng 12 file test tham chiếu.

DTO: [RevenueReportResponse](../../src/main/java/com/scms/backend/membership/reporting/RevenueReportResponse.java#L6).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /manager/rooms

`RoomController#list` → RoomService#list. Tổng 7 file test tham chiếu.

DTO: [RoomResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L8).

- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## POST /manager/rooms

`RoomController#create` → RoomService#create. Tổng 7 file test tham chiếu.

DTO: [RoomCreateRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomCreateRequest.java#L5); [RoomResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L8).

- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## GET /manager/rooms/availability

`RoomController#availability` → RoomService#availability. Tổng 7 file test tham chiếu.

DTO: [RoomAvailabilityResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityResponse.java#L6).

- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## GET /manager/rooms/{roomId}

`RoomController#get` → RoomService#get. Tổng 7 file test tham chiếu.

DTO: [RoomResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L8).

- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## PATCH /manager/rooms/{roomId}

`RoomController#update` → RoomService#update. Tổng 7 file test tham chiếu.

DTO: [RoomPatchRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomPatchRequest.java#L6); [RoomResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L8).

- [RoomAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomAvailabilityIntegrationTests.java)
- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomServiceTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [ManagedResourceAuditRollbackIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ManagedResourceAuditRollbackIntegrationTests.java)

## POST /manager/rooms/{roomId}/impact-apply

`RoomController#impactApply` → RoomSuspensionService#applyImpact. Tổng 5 file test tham chiếu.

DTO: [RoomImpactPreviewRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewRequest.java#L6); [RoomResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L8).

- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)

## POST /manager/rooms/{roomId}/impact-preview

`RoomController#impactPreview` → RoomSuspensionService#previewImpact. Tổng 5 file test tham chiếu.

DTO: [RoomImpactPreviewRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewRequest.java#L6); [RoomImpactPreviewResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewResponse.java#L9).

- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)

## GET /manager/rooms/{roomId}/suspensions

`RoomController#listSuspensions` → RoomSuspensionService#list. Tổng 5 file test tham chiếu.

DTO: [RoomSuspensionResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionResponse.java#L7).

- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)

## POST /manager/rooms/{roomId}/suspensions

`RoomController#suspend` → RoomSuspensionService#create. Tổng 5 file test tham chiếu.

DTO: [RoomSuspensionCreateRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionCreateRequest.java#L5); [RoomSuspensionResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionResponse.java#L7).

- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)

## PATCH /manager/rooms/{roomId}/suspensions/{suspensionId}

`RoomController#extendSuspension` → RoomSuspensionService#extend. Tổng 5 file test tham chiếu.

DTO: [RoomSuspensionExtensionRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionRequest.java#L8); [RoomSuspensionResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionResponse.java#L7).

- [RoomControllerTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomControllerTests.java)
- [RoomImpactIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomImpactIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)

## GET /manager/staff-accounts

`StaffAccountController#search` → StaffAccountService#search. Tổng 1 file test tham chiếu.

DTO: [AccountRole](../../src/main/java/com/scms/backend/account/AccountRole.java#L3); [AccountStatus](../../src/main/java/com/scms/backend/account/AccountStatus.java#L3); [StaffAccountPageResponse](../../src/main/java/com/scms/backend/manager/StaffAccountPageResponse.java#L5).

- [StaffAccountServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountServiceTests.java)

## POST /manager/staff-accounts

`StaffAccountController#create` → StaffAccountCreateService#create. Tổng 1 file test tham chiếu.

DTO: [StaffAccountCreateRequest](../../src/main/java/com/scms/backend/manager/StaffAccountCreateRequest.java#L8); [StaffAccountResponse](../../src/main/java/com/scms/backend/manager/StaffAccountResponse.java#L8).

- [StaffAccountCreateServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountCreateServiceTests.java)

## GET /manager/staff-accounts/{accountId}

`StaffAccountController#get` → StaffAccountService#get. Tổng 1 file test tham chiếu.

DTO: [StaffAccountResponse](../../src/main/java/com/scms/backend/manager/StaffAccountResponse.java#L8).

- [StaffAccountServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountServiceTests.java)

## PATCH /manager/staff-accounts/{accountId}

`StaffAccountController#update` → StaffAccountService#update. Tổng 1 file test tham chiếu.

DTO: [StaffAccountPatchRequest](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L7); [StaffAccountResponse](../../src/main/java/com/scms/backend/manager/StaffAccountResponse.java#L8).

- [StaffAccountServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffAccountServiceTests.java)

## POST /manager/staff-accounts/{accountId}/reset-password

`StaffAccountController#resetPassword` → ResetPasswordService#managerReset. Tổng 1 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [ResetPasswordServiceTests.java](../../src/test/java/com/scms/backend/manager/ResetPasswordServiceTests.java)

## PATCH /manager/staff-accounts/{accountId}/status

`StaffAccountController#changeStatus` → StaffStatusService#deactivate. Tổng 2 file test tham chiếu.

DTO: [StaffAccountResponse](../../src/main/java/com/scms/backend/manager/StaffAccountResponse.java#L8); [StaffStatusChangeRequest](../../src/main/java/com/scms/backend/manager/StaffStatusChangeRequest.java#L5).

- [OperationalIntegrityIntegrationTests.java](../../src/test/java/com/scms/backend/manager/OperationalIntegrityIntegrationTests.java)
- [StaffStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/StaffStatusServiceTests.java)

## GET /members/class-sessions

`MemberClassScheduleController#list` → ClassSessionService#memberSchedule. Tổng 24 file test tham chiếu.

DTO: [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## POST /members/class-sessions/{sessionId}/bookings

`BookingController#book` → BookingService#book. Tổng 7 file test tham chiếu.

DTO: [BookingResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingResponse.java#L8).

- [BookingControllerTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingControllerTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [WaitlistPromotionConcurrencyIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionConcurrencyIntegrationTests.java)

## DELETE /members/class-sessions/{sessionId}/waitlist

`WaitlistController#leave` → WaitlistService#leave. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## POST /members/class-sessions/{sessionId}/waitlist

`WaitlistController#join` → WaitlistService#join. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## GET /members/me/bookings

`BookingController#listForMember` → BookingService#listForMember. Tổng 7 file test tham chiếu.

DTO: [BookingSchedulePageResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingSchedulePageResponse.java#L5); [BookingStatus](../../src/main/java/com/scms/backend/scheduling/BookingStatus.java#L3).

- [BookingControllerTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingControllerTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [WaitlistPromotionConcurrencyIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionConcurrencyIntegrationTests.java)

## PATCH /members/me/bookings/{bookingId}/cancel

`BookingController#cancel` → BookingService#cancel. Tổng 7 file test tham chiếu.

DTO: [BookingResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingResponse.java#L8).

- [BookingControllerTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingControllerTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [WaitlistPromotionConcurrencyIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionConcurrencyIntegrationTests.java)

## GET /members/me/center-visits/current

`CenterVisitController#current` → CenterVisitService#currentMember. Tổng 2 file test tham chiếu.

DTO: [CenterVisitResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitResponse.java#L7).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)

## PATCH /members/me/center-visits/current/checkout

`CenterVisitController#checkoutMember` → CenterVisitService#checkoutMember. Tổng 2 file test tham chiếu.

DTO: [CenterVisitResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitResponse.java#L7).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)

## GET /members/me/coach-assignment

`MemberCoachAssignmentController#getActive` → MemberCoachAssignmentService#getActive. Tổng 4 file test tham chiếu.

DTO: [MemberCoachAssignmentResponse](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentResponse.java#L9).

- [CoachAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/training/assignment/CoachAssignmentIntegrationTests.java)
- [MemberCoachAssignmentControllerTests.java](../../src/test/java/com/scms/backend/training/assignment/MemberCoachAssignmentControllerTests.java)
- [MemberCoachAssignmentServiceTests.java](../../src/test/java/com/scms/backend/training/assignment/MemberCoachAssignmentServiceTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /members/me/course-enrollments

`CourseEnrollmentController#memberEnrollments` → CourseEnrollmentService#memberEnrollments. Tổng 5 file test tham chiếu.

DTO: [MemberEnrollment](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L18).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /members/me/course-enrollments/{enrollmentId}/withdrawal-requests

`CourseWithdrawalController#memberCreate` → CourseWithdrawalService#createForMember. Tổng 3 file test tham chiếu.

DTO: [WithdrawalCreate](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L14); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /members/me/course-orders

`CourseOrderController#createForMember` → CourseOrderService#createForMember. Tổng 3 file test tham chiếu.

DTO: [CourseOrderCreateRequest](../../src/main/java/com/scms/backend/course/order/CourseOrderCreateRequest.java#L8); [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8).

- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseOrderServiceTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderServiceTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /members/me/course-orders/{orderId}

`CourseOrderController#getForMember` → CourseOrderService#getForMember. Tổng 3 file test tham chiếu.

DTO: [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8).

- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseOrderServiceTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderServiceTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /members/me/course-orders/{orderId}/payments/sepay

`SepayController#createCoursePayment` → SepayService#createCoursePayment. Tổng 4 file test tham chiếu.

DTO: [CoursePaymentResponse](../../src/main/java/com/scms/backend/course/order/CoursePaymentResponse.java#L10).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## GET /members/me/course-refund-obligations

`CourseRefundObligationController#memberList` → CourseRefundObligationService#listForMember. Tổng 3 file test tham chiếu.

DTO: [MemberRefundObligationPage](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L35).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /members/me/course-refund-obligations/{obligationId}

`CourseRefundObligationController#memberDetail` → CourseRefundObligationService#detailForMember. Tổng 3 file test tham chiếu.

DTO: [MemberRefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L32).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /members/me/course-schedule-changes

`CourseEnrollmentController#scheduleChanges` → CourseEnrollmentService#scheduleChanges. Tổng 5 file test tham chiếu.

DTO: [ScheduleChange](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L14).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /members/me/course-schedule-changes/{responseId}/response

`CourseEnrollmentController#respondToScheduleChange` → CourseEnrollmentService#respondToScheduleChange. Tổng 5 file test tham chiếu.

DTO: [ScheduleChange](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L14); [ScheduleChangeResponse](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L22).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /members/me/course-transfer-offers

`CourseTransferController#offers` → CourseTransferService#memberOffers. Tổng 2 file test tham chiếu.

DTO: [TransferOffer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L22).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## POST /members/me/course-transfer-offers/{offerId}/accept

`CourseTransferController#accept` → CourseTransferService#accept. Tổng 2 file test tham chiếu.

DTO: [TransferOffer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L22).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## GET /members/me/course-waitlists

`CourseWaitlistController#mine` → CourseWaitlistService#mine. Tổng 5 file test tham chiếu.

DTO: [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## GET /members/me/course-withdrawal-requests

`CourseWithdrawalController#memberList` → CourseWithdrawalService#listForMember. Tổng 3 file test tham chiếu.

DTO: [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /members/me/course-withdrawal-requests/{requestId}

`CourseWithdrawalController#memberDetail` → CourseWithdrawalService#detailForMember. Tổng 3 file test tham chiếu.

DTO: [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /members/me/course-withdrawal-requests/{requestId}/cancel

`CourseWithdrawalController#memberCancel` → CourseWithdrawalService#cancelForMember. Tổng 3 file test tham chiếu.

DTO: [WithdrawalDecision](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L17); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /members/me/feedback

`MemberTrainingFeedbackController#viewOwnHistory` → MemberTrainingFeedbackService#viewOwnHistory. Tổng 5 file test tham chiếu.

DTO: [MemberTrainingFeedbackPageResponse](../../src/main/java/com/scms/backend/training/feedback/MemberTrainingFeedbackPageResponse.java#L5).

- [MemberTrainingFeedbackControllerTests.java](../../src/test/java/com/scms/backend/training/feedback/MemberTrainingFeedbackControllerTests.java)
- [MemberTrainingFeedbackIntegrationTests.java](../../src/test/java/com/scms/backend/training/feedback/MemberTrainingFeedbackIntegrationTests.java)
- [MemberTrainingFeedbackServiceTests.java](../../src/test/java/com/scms/backend/training/feedback/MemberTrainingFeedbackServiceTests.java)
- [BookedSessionTrainingIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /members/me/membership-freeze-requests

`MembershipFreezeController#memberList` → MembershipFreezeService#listForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeRequestPageResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L6).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /members/me/membership-freeze-requests

`MembershipFreezeController#memberCreate` → MembershipFreezeService#createForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeCreateRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeCreateRequest.java#L5); [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## DELETE /members/me/membership-freeze-requests/{requestId}

`MembershipFreezeController#memberWithdraw` → MembershipFreezeService#withdrawForMember. Tổng 2 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /members/me/membership-freeze-requests/{requestId}/end-early

`MembershipFreezeController#memberEndEarly` → MembershipFreezeService#endEarlyForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeDecisionRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeDecisionRequest.java#L3); [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /members/me/membership-orders

`MembershipOrderController#createForMember` → MembershipOrderService#createForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipOrderCreateRequest](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCreateRequest.java#L7); [MembershipOrderResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderResponse.java#L11).

- [MembershipFulfillmentModeTests.java](../../src/test/java/com/scms/backend/membership/MembershipFulfillmentModeTests.java)
- [MembershipOrderServiceTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderServiceTests.java)

## GET /members/me/membership-orders/pending

`MembershipOrderController#resumeForMember` → MembershipOrderService#resumeForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipOrderResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderResponse.java#L11).

- [MembershipFulfillmentModeTests.java](../../src/test/java/com/scms/backend/membership/MembershipFulfillmentModeTests.java)
- [MembershipOrderServiceTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderServiceTests.java)

## POST /members/me/membership-orders/{orderId}/bank-transfer

`SepayController#create` → SepayService#create. Tổng 4 file test tham chiếu.

DTO: [SepayPaymentResponse](../../src/main/java/com/scms/backend/membership/payment/SepayPaymentResponse.java#L7).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## POST /members/me/membership-orders/{orderId}/payments/sepay

`SepayController#create` → SepayService#create. Tổng 4 file test tham chiếu.

DTO: [SepayPaymentResponse](../../src/main/java/com/scms/backend/membership/payment/SepayPaymentResponse.java#L7).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## GET /members/me/membership-termination-requests

`MembershipTerminationController#memberList` → MembershipTerminationService#listForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationRequestPageResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L6).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## POST /members/me/membership-termination-requests

`MembershipTerminationController#memberCreate` → MembershipTerminationService#createForMember. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationCreateRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationCreateRequest.java#L5); [MembershipTerminationRequestResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L9).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## DELETE /members/me/membership-termination-requests/{requestId}

`MembershipTerminationController#memberWithdraw` → MembershipTerminationService#withdrawForMember. Tổng 2 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## GET /members/me/memberships

`PaymentController#history` → PaymentService#memberHistory. Tổng 12 file test tham chiếu.

DTO: [MembershipHistoryResponse](../../src/main/java/com/scms/backend/membership/MembershipHistoryResponse.java#L10).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /members/me/profile

`MemberProfileController#getProfile` → MemberProfileService#getProfile. Tổng 4 file test tham chiếu.

DTO: [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [MemberProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/memberprofile/MemberProfileServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## PATCH /members/me/profile

`MemberProfileController#updateProfile` → MemberProfileService#updateProfile. Tổng 4 file test tham chiếu.

DTO: [MemberProfilePatchRequest](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L8); [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [MemberProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/memberprofile/MemberProfileServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## POST /members/me/profile/image

`ProfileImageUploadController#uploadMemberProfileImage` → ProfileImageUploadService#uploadForMember. Tổng 4 file test tham chiếu.

DTO: [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9).

- [ProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadControllerTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadControllerTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## GET /members/me/receipts

`PaymentController#receipts` → PaymentService#receipts. Tổng 12 file test tham chiếu.

DTO: [ReceiptResponse](../../src/main/java/com/scms/backend/membership/payment/ReceiptResponse.java#L8).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /members/me/training-plans

`MemberTrainingPlanController#viewOwnPlans` → MemberTrainingPlanService#viewOwnPlans. Tổng 2 file test tham chiếu.

DTO: [MemberTrainingPlanResponse](../../src/main/java/com/scms/backend/training/plan/MemberTrainingPlanResponse.java#L10).

- [MemberTrainingPlanControllerTests.java](../../src/test/java/com/scms/backend/training/plan/MemberTrainingPlanControllerTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /members/me/training-results

`MemberTrainingResultController#viewOwnHistory` → MemberTrainingResultService#viewOwnHistory. Tổng 5 file test tham chiếu.

DTO: [MemberTrainingResultPageResponse](../../src/main/java/com/scms/backend/training/result/MemberTrainingResultPageResponse.java#L5).

- [MemberTrainingResultControllerTests.java](../../src/test/java/com/scms/backend/training/result/MemberTrainingResultControllerTests.java)
- [MemberTrainingResultIntegrationTests.java](../../src/test/java/com/scms/backend/training/result/MemberTrainingResultIntegrationTests.java)
- [MemberTrainingResultServiceTests.java](../../src/test/java/com/scms/backend/training/result/MemberTrainingResultServiceTests.java)
- [BookedSessionTrainingIntegrationTests.java](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java)
- [TrainingBenefitEligibilityIntegrationTests.java](../../src/test/java/com/scms/backend/training/TrainingBenefitEligibilityIntegrationTests.java)

## GET /members/me/waitlists

`WaitlistController#mine` → WaitlistService#mine. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## GET /members/pt-sessions

`PtSessionController#member` → ClassSessionService#memberPtSchedule. Tổng 24 file test tham chiếu.

DTO: [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /membership-offers

`MembershipOfferController#listActiveOffers` → MembershipOfferService#listActiveOffers. Tổng 0 file test tham chiếu.

DTO: [MembershipOfferResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferResponse.java#L11).


## GET /membership-offers/{offerId}

`MembershipOfferController#getActiveOffer` → MembershipOfferService#getActiveOffer. Tổng 0 file test tham chiếu.

DTO: [MembershipOfferResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferResponse.java#L11).


## PATCH /membership-orders/{orderId}/cancel

`PaymentController#cancelOrder` → PaymentService#cancelOrder. Tổng 12 file test tham chiếu.

DTO: [MembershipOrderCancellationRequest](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCancellationRequest.java#L3); [MembershipOrderCancellationResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCancellationResponse.java#L6).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /notifications

`NotificationController#list` → NotificationService#list. Tổng 3 file test tham chiếu.

DTO: [NotificationPageResponse](../../src/main/java/com/scms/backend/notification/NotificationPageResponse.java#L5).

- [NotificationCenterIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationCenterIntegrationTests.java)
- [NotificationControllerTests.java](../../src/test/java/com/scms/backend/notification/NotificationControllerTests.java)
- [NotificationServiceTests.java](../../src/test/java/com/scms/backend/notification/NotificationServiceTests.java)

## PATCH /notifications/read-all

`NotificationController#markAllRead` → NotificationService#markAllRead. Tổng 3 file test tham chiếu.

DTO: [NotificationReadResponse](../../src/main/java/com/scms/backend/notification/NotificationReadResponse.java#L3).

- [NotificationCenterIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationCenterIntegrationTests.java)
- [NotificationControllerTests.java](../../src/test/java/com/scms/backend/notification/NotificationControllerTests.java)
- [NotificationServiceTests.java](../../src/test/java/com/scms/backend/notification/NotificationServiceTests.java)

## PATCH /notifications/{notificationId}/read

`NotificationController#markRead` → NotificationService#markRead. Tổng 3 file test tham chiếu.

DTO: [NotificationReadResponse](../../src/main/java/com/scms/backend/notification/NotificationReadResponse.java#L3).

- [NotificationCenterIntegrationTests.java](../../src/test/java/com/scms/backend/notification/NotificationCenterIntegrationTests.java)
- [NotificationControllerTests.java](../../src/test/java/com/scms/backend/notification/NotificationControllerTests.java)
- [NotificationServiceTests.java](../../src/test/java/com/scms/backend/notification/NotificationServiceTests.java)

## GET /payments/reconciliation-queue

`PaymentController#reconciliationQueue` → PaymentService#reconciliationQueue. Tổng 12 file test tham chiếu.

DTO: [ReconciliationQueuePageResponse](../../src/main/java/com/scms/backend/membership/payment/ReconciliationQueuePageResponse.java#L5); [ReconciliationQueueStatus](../../src/main/java/com/scms/backend/membership/ReconciliationQueueStatus.java#L3).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## POST /payments/sepay/webhook

`SepayController#webhook` → SepayService#webhook. Tổng 4 file test tham chiếu.

DTO: [SepayWebhookRequest](../../src/main/java/com/scms/backend/membership/payment/SepayWebhookRequest.java#L7); [SepayWebhookResponse](../../src/main/java/com/scms/backend/membership/payment/SepayWebhookResponse.java#L7).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## GET /payments/{paymentId}/evidence-images

`PaymentEvidenceController#list` → PaymentEvidenceService#list. Tổng 1 file test tham chiếu.

DTO: [PaymentEvidenceService.Evidence](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L37).

- [PaymentEvidenceServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentEvidenceServiceTests.java)

## POST /payments/{paymentId}/evidence-images

`PaymentEvidenceController#upload` → PaymentEvidenceService#upload. Tổng 1 file test tham chiếu.

DTO: [PaymentEvidenceService.Evidence](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L37).

- [PaymentEvidenceServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentEvidenceServiceTests.java)

## GET /payments/{paymentId}/evidence-images/{evidenceId}

`PaymentEvidenceController#read` → PaymentEvidenceService#read. Tổng 1 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [PaymentEvidenceServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentEvidenceServiceTests.java)

## POST /payments/{paymentId}/reconcile

`PaymentController#reconcile` → PaymentService#reconcile. Tổng 12 file test tham chiếu.

DTO: [PaymentActionRequest](../../src/main/java/com/scms/backend/membership/payment/PaymentActionRequest.java#L5); [PaymentResultResponse](../../src/main/java/com/scms/backend/membership/payment/PaymentResultResponse.java#L8).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /payments/{paymentId}/result

`PaymentController#result` → PaymentService#result. Tổng 12 file test tham chiếu.

DTO: [PaymentResultResponse](../../src/main/java/com/scms/backend/membership/payment/PaymentResultResponse.java#L8).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /receipts/{receiptId}

`PaymentController#receipt` → PaymentService#receipt. Tổng 12 file test tham chiếu.

DTO: [ReceiptResponse](../../src/main/java/com/scms/backend/membership/payment/ReceiptResponse.java#L8).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## POST /reception/center-visits

`CenterVisitController#checkIn` → CenterVisitService#checkIn. Tổng 2 file test tham chiếu.

DTO: [CenterVisitRequest](../../src/main/java/com/scms/backend/centervisit/CenterVisitRequest.java#L5); [CenterVisitResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitResponse.java#L7).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)

## GET /reception/center-visits/open

`CenterVisitController#open` → CenterVisitService#openVisits. Tổng 2 file test tham chiếu.

DTO: [CenterVisitPageResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitPageResponse.java#L5).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)

## GET /reception/class-sessions

`ReceptionClassSessionController#list` → ClassSessionService#receptionSchedule. Tổng 24 file test tham chiếu.

DTO: [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /reception/class-sessions/{sessionId}/waitlist

`WaitlistController#reception` → WaitlistService#sessionList. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## POST /reception/course-refund-obligations/{obligationId}/disburse

`CourseRefundObligationController#disburse` → CourseRefundObligationService#recordDisbursement. Tổng 3 file test tham chiếu.

DTO: [RefundDisbursement](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L17); [RefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L26).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /reception/course-refunds

`CourseRefundObligationController#receptionList` → CourseRefundObligationService#listForReceptionist. Tổng 3 file test tham chiếu.

DTO: [ReceptionCourseRefundPage](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L42).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /reception/course-refunds/{refundId}

`CourseRefundObligationController#receptionDetail` → CourseRefundObligationService#detailForReceptionist. Tổng 3 file test tham chiếu.

DTO: [ReceptionCourseRefund](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L38).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /reception/email-verification/manual-activation-requests

`ReceptionEmailVerificationController#list` → ManualActivationService#list. Tổng 1 file test tham chiếu.

DTO: [ManualActivationPageResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationPageResponse.java#L5); [ManualActivationStatus](../../src/main/java/com/scms/backend/emailverification/ManualActivationStatus.java#L3).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /reception/email-verification/manual-activation-requests

`ReceptionEmailVerificationController#create` → ManualActivationService#create. Tổng 1 file test tham chiếu.

DTO: [ManualActivationCreateRequest](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationCreateRequest.java#L10); [ManualActivationResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationResponse.java#L10).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## GET /reception/email-verification/manual-activation-requests/{requestId}

`ReceptionEmailVerificationController#get` → ManualActivationService#getForActor. Tổng 1 file test tham chiếu.

DTO: [ManualActivationResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationResponse.java#L10).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## GET /reception/email-verification/pending-accounts

`ReceptionEmailVerificationController#search` → ManualActivationService#search. Tổng 1 file test tham chiếu.

DTO: [PendingVerificationAccountResponse](../../src/main/java/com/scms/backend/emailverification/verification/PendingVerificationAccountResponse.java#L6).

- [EmailVerificationIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java)

## POST /reception/members

`ReceptionMemberController#createMember` → ReceptionMemberService#createMember. Tổng 1 file test tham chiếu.

DTO: [ReceptionMemberCreateRequest](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateRequest.java#L13); [ReceptionMemberCreateResponse](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateResponse.java#L8).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)

## GET /reception/members/search

`ReceptionMemberController#searchMembers` → ReceptionMemberProfileService#search. Tổng 6 file test tham chiếu.

DTO: [ReceptionMemberSearchResponse](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchResponse.java#L8).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)
- [ReceptionMemberMembershipSummaryIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberMembershipSummaryIntegrationTests.java)
- [ReceptionMemberSearchServiceTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## PATCH /reception/members/{memberAccountId}/center-visits/current/checkout

`CenterVisitController#checkoutReceptionist` → CenterVisitService#checkoutReceptionist. Tổng 2 file test tham chiếu.

DTO: [CenterVisitResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitResponse.java#L7).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)

## GET /reception/members/{memberCode}/membership-freeze-requests

`MembershipFreezeController#receptionList` → MembershipFreezeService#listForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeRequestPageResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L6).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /reception/members/{memberCode}/membership-freeze-requests

`MembershipFreezeController#receptionCreate` → MembershipFreezeService#createForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeCreateRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeCreateRequest.java#L5); [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## GET /reception/members/{memberCode}/membership-termination-requests

`MembershipTerminationController#receptionList` → MembershipTerminationService#listForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationRequestPageResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L6).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## POST /reception/members/{memberCode}/membership-termination-requests

`MembershipTerminationController#receptionCreate` → MembershipTerminationService#createForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipTerminationCreateRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationCreateRequest.java#L5); [MembershipTerminationRequestResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L9).

- [MembershipTerminationContractTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationContractTests.java)
- [MembershipTerminationServiceTests.java](../../src/test/java/com/scms/backend/membership/termination/MembershipTerminationServiceTests.java)

## GET /reception/members/{memberCode}/memberships

`PaymentController#receptionMembershipHistory` → PaymentService#receptionMembershipHistory. Tổng 12 file test tham chiếu.

DTO: [StaffMembershipResponse](../../src/main/java/com/scms/backend/membership/StaffMembershipResponse.java#L9).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## GET /reception/members/{memberCode}/refund-eligible-sources

`MembershipRefundController#refundEligibleSources` → MembershipRefundService#refundEligibleSources. Tổng 5 file test tham chiếu.

DTO: [RefundEligibleSourceResponse](../../src/main/java/com/scms/backend/membership/refund/RefundEligibleSourceResponse.java#L9).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## POST /reception/members/{memberCode}/refund-requests

`MembershipRefundController#create` → MembershipRefundService#create. Tổng 5 file test tham chiếu.

DTO: [RefundRequestCreateRequest](../../src/main/java/com/scms/backend/membership/refund/RefundRequestCreateRequest.java#L6); [RefundRequestResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L13).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## GET /reception/members/{memberId}/bookings

`BookingController#listForReceptionist` → BookingService#listForReceptionist. Tổng 7 file test tham chiếu.

DTO: [BookingSchedulePageResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingSchedulePageResponse.java#L5); [BookingStatus](../../src/main/java/com/scms/backend/scheduling/BookingStatus.java#L3).

- [BookingControllerTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingControllerTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [WaitlistPromotionConcurrencyIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionConcurrencyIntegrationTests.java)

## POST /reception/members/{memberId}/bookings

`BookingController#bookForMember` → BookingService#bookForMember. Tổng 7 file test tham chiếu.

DTO: [BookingResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingResponse.java#L8); [ReceptionBookingRequest](../../src/main/java/com/scms/backend/scheduling/booking/ReceptionBookingRequest.java#L6).

- [BookingControllerTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingControllerTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [WaitlistPromotionConcurrencyIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionConcurrencyIntegrationTests.java)

## PATCH /reception/members/{memberId}/bookings/{bookingId}/cancel

`BookingController#cancelForMember` → BookingService#cancelForMember. Tổng 7 file test tham chiếu.

DTO: [BookingResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingResponse.java#L8).

- [BookingControllerTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingControllerTests.java)
- [BookingServiceTests.java](../../src/test/java/com/scms/backend/scheduling/booking/BookingServiceTests.java)
- [DisciplineBenefitIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineBenefitIntegrationTests.java)
- [ClassSessionCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionIntegrationTests.java)
- [WaitlistPromotionConcurrencyIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionConcurrencyIntegrationTests.java)

## POST /reception/members/{memberId}/cash-payments

`PaymentController#cash` → PaymentService#cash. Tổng 12 file test tham chiếu.

DTO: [CashPaymentRequest](../../src/main/java/com/scms/backend/membership/payment/CashPaymentRequest.java#L6); [PaymentResultResponse](../../src/main/java/com/scms/backend/membership/payment/PaymentResultResponse.java#L8).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## DELETE /reception/members/{memberId}/class-sessions/{sessionId}/waitlist

`WaitlistController#leaveFor` → WaitlistService#leaveForMember. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## POST /reception/members/{memberId}/class-sessions/{sessionId}/waitlist

`WaitlistController#joinFor` → WaitlistService#joinForMember. Tổng 3 file test tham chiếu.

DTO: [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8).

- [SessionLifecycleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/SessionLifecycleServiceTests.java)
- [WaitlistPromotionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionServiceTests.java)
- [WaitlistServiceTests.java](../../src/test/java/com/scms/backend/scheduling/waitlist/WaitlistServiceTests.java)

## GET /reception/members/{memberId}/course-enrollments

`CourseEnrollmentController#receptionEnrollments` → ReceptionCourseEnrollmentService#list. Tổng 1 file test tham chiếu.

DTO: [ReceptionCourseEnrollmentPage](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L42).

- [ReceptionCourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentServiceTests.java)

## GET /reception/members/{memberId}/course-enrollments/{enrollmentId}

`CourseEnrollmentController#receptionEnrollment` → ReceptionCourseEnrollmentService#detail. Tổng 1 file test tham chiếu.

DTO: [ReceptionCourseEnrollment](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L38).

- [ReceptionCourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentServiceTests.java)

## POST /reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests

`CourseWithdrawalController#receptionCreate` → CourseWithdrawalService#createForReceptionist. Tổng 3 file test tham chiếu.

DTO: [WithdrawalCreate](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L14); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /reception/members/{memberId}/course-orders

`CourseOrderController#createForReceptionist` → CourseOrderService#createForReceptionist. Tổng 3 file test tham chiếu.

DTO: [CourseOrderCreateRequest](../../src/main/java/com/scms/backend/course/order/CourseOrderCreateRequest.java#L8); [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8).

- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseOrderServiceTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderServiceTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /reception/members/{memberId}/course-orders/{orderId}

`CourseOrderController#getForReceptionist` → CourseOrderService#getForReceptionist. Tổng 3 file test tham chiếu.

DTO: [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8).

- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseOrderServiceTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderServiceTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /reception/members/{memberId}/course-orders/{orderId}/payments/cash

`SepayController#fulfillCourseCash` → SepayService#fulfillCourseCash. Tổng 4 file test tham chiếu.

DTO: [CourseEnrollmentResponse](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentResponse.java#L8).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## POST /reception/members/{memberId}/course-orders/{orderId}/payments/sepay

`SepayController#createCoursePaymentForReceptionist` → SepayService#createCoursePaymentForReceptionist. Tổng 4 file test tham chiếu.

DTO: [CoursePaymentResponse](../../src/main/java/com/scms/backend/course/order/CoursePaymentResponse.java#L10).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## GET /reception/members/{memberId}/course-refund-obligations

`CourseReceptionActionsController#refunds` → CourseRefundObligationService#memberHistoryForReceptionist. Tổng 3 file test tham chiếu.

DTO: [MemberRefundObligationPage](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L35).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## GET /reception/members/{memberId}/course-refund-obligations/{obligationId}

`CourseReceptionActionsController#refund` → CourseRefundObligationService#memberDetailForReceptionist. Tổng 3 file test tham chiếu.

DTO: [MemberRefundObligation](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L32).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseRefundObligationIntegrationTests.java](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)

## DELETE /reception/members/{memberId}/course-runs/{runId}/waitlist

`CourseReceptionActionsController#leave` → CourseWaitlistService#leave. Tổng 5 file test tham chiếu.

DTO: [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## POST /reception/members/{memberId}/course-runs/{runId}/waitlist

`CourseReceptionActionsController#join` → CourseWaitlistService#join. Tổng 5 file test tham chiếu.

DTO: [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## GET /reception/members/{memberId}/course-schedule-changes

`CourseReceptionActionsController#changes` → CourseScheduleChangeService#listForReceptionist. Tổng 4 file test tham chiếu.

DTO: [ScheduleChange](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L14).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseSessionServiceTests.java](../../src/test/java/com/scms/backend/course/session/CourseSessionServiceTests.java)

## POST /reception/members/{memberId}/course-schedule-changes/{responseId}/response

`CourseReceptionActionsController#respond` → CourseScheduleChangeService#respond. Tổng 4 file test tham chiếu.

DTO: [ScheduleChange](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L14); [ScheduleChangeResponse](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L22).

- [CourseEnrollmentServiceTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseEnrollmentServiceTests.java)
- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeServiceTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeServiceTests.java)
- [CourseSessionServiceTests.java](../../src/test/java/com/scms/backend/course/session/CourseSessionServiceTests.java)

## GET /reception/members/{memberId}/course-transfer-offers

`CourseReceptionActionsController#offers` → CourseTransferService#offersForReceptionist. Tổng 2 file test tham chiếu.

DTO: [TransferOffer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L22).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## POST /reception/members/{memberId}/course-transfer-offers/{offerId}/accept

`CourseReceptionActionsController#acceptTransfer` → CourseTransferService#accept. Tổng 2 file test tham chiếu.

DTO: [TransferOffer](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L22).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseTransferIntegrationTests.java](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java)

## GET /reception/members/{memberId}/course-waitlists

`CourseReceptionActionsController#waitlists` → CourseWaitlistService#mineForReceptionist. Tổng 5 file test tham chiếu.

DTO: [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## POST /reception/members/{memberId}/course-waitlists/{waitlistId}/accept

`CourseReceptionActionsController#acceptWaitlist` → CourseWaitlistService#accept. Tổng 5 file test tham chiếu.

DTO: [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseOrderIntegrationTests.java](../../src/test/java/com/scms/backend/course/order/CourseOrderIntegrationTests.java)
- [CourseRunClosureIntegrationTests.java](../../src/test/java/com/scms/backend/course/run/CourseRunClosureIntegrationTests.java)
- [CourseWaitlistModelsTests.java](../../src/test/java/com/scms/backend/course/waitlist/CourseWaitlistModelsTests.java)
- [MemberStatusServiceTests.java](../../src/test/java/com/scms/backend/manager/MemberStatusServiceTests.java)

## GET /reception/members/{memberId}/course-withdrawal-requests

`CourseReceptionActionsController#withdrawals` → CourseWithdrawalService#listForReceptionist. Tổng 3 file test tham chiếu.

DTO: [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## GET /reception/members/{memberId}/course-withdrawal-requests/{requestId}

`CourseReceptionActionsController#withdrawal` → CourseWithdrawalService#detailForReceptionist. Tổng 3 file test tham chiếu.

DTO: [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel

`CourseWithdrawalController#receptionCancel` → CourseWithdrawalService#cancelForReceptionist. Tổng 3 file test tham chiếu.

DTO: [WithdrawalDecision](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L17); [WithdrawalRequest](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L20).

- [CourseReceptionActionsControllerTests.java](../../src/test/java/com/scms/backend/course/enrollment/CourseReceptionActionsControllerTests.java)
- [CourseScheduleChangeIntegrationTests.java](../../src/test/java/com/scms/backend/course/schedulechange/CourseScheduleChangeIntegrationTests.java)
- [CourseWithdrawalIntegrationTests.java](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java)

## POST /reception/members/{memberId}/membership-orders

`MembershipOrderController#createForReceptionist` → MembershipOrderService#createForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipOrderCreateRequest](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCreateRequest.java#L7); [MembershipOrderResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderResponse.java#L11).

- [MembershipFulfillmentModeTests.java](../../src/test/java/com/scms/backend/membership/MembershipFulfillmentModeTests.java)
- [MembershipOrderServiceTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderServiceTests.java)

## GET /reception/members/{memberId}/membership-orders/pending

`MembershipOrderController#resumeForReceptionist` → MembershipOrderService#resumeForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipOrderResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderResponse.java#L11).

- [MembershipFulfillmentModeTests.java](../../src/test/java/com/scms/backend/membership/MembershipFulfillmentModeTests.java)
- [MembershipOrderServiceTests.java](../../src/test/java/com/scms/backend/membership/order/MembershipOrderServiceTests.java)

## POST /reception/members/{memberId}/membership-orders/{orderId}/payments/sepay

`SepayController#createForReceptionist` → SepayService#createForReceptionist. Tổng 4 file test tham chiếu.

DTO: [SepayPaymentResponse](../../src/main/java/com/scms/backend/membership/payment/SepayPaymentResponse.java#L7).

- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [SepayControllerCoursePaymentRouteTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayControllerCoursePaymentRouteTests.java)
- [SepayServiceTests.java](../../src/test/java/com/scms/backend/membership/payment/SepayServiceTests.java)

## GET /reception/members/{memberId}/profile

`ReceptionMemberController#getMemberProfile` → ReceptionMemberProfileService#getProfile. Tổng 6 file test tham chiếu.

DTO: [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)
- [ReceptionMemberMembershipSummaryIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberMembershipSummaryIntegrationTests.java)
- [ReceptionMemberSearchServiceTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## PATCH /reception/members/{memberId}/profile

`ReceptionMemberController#updateMemberProfile` → ReceptionMemberProfileService#updateProfile. Tổng 6 file test tham chiếu.

DTO: [MemberProfilePatchRequest](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L8); [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [CenterVisitServiceTests.java](../../src/test/java/com/scms/backend/centervisit/CenterVisitServiceTests.java)
- [ReceptionMemberMembershipSummaryIntegrationTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberMembershipSummaryIntegrationTests.java)
- [ReceptionMemberSearchServiceTests.java](../../src/test/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## POST /reception/members/{memberId}/profile/image

`ProfileImageUploadController#uploadReceptionMemberProfileImage` → ProfileImageUploadService#uploadForReceptionist. Tổng 4 file test tham chiếu.

DTO: [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9).

- [ProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadControllerTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadControllerTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## GET /reception/members/{memberId}/receipts

`PaymentController#receiptsForReceptionist` → PaymentService#receiptsForReceptionist. Tổng 12 file test tham chiếu.

DTO: [ReceiptResponse](../../src/main/java/com/scms/backend/membership/payment/ReceiptResponse.java#L8).

- [FailedReconciliationNotificationTests.java](../../src/test/java/com/scms/backend/membership/FailedReconciliationNotificationTests.java)
- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentFlowIntegrationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)
- [PaymentServiceLockingTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceLockingTests.java)
- [PaymentServiceMembershipHistoryTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceMembershipHistoryTests.java)
- [PaymentServiceOrderCancellationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceOrderCancellationTests.java)
- [PaymentServiceReceiptAccessTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReceiptAccessTests.java)
- [PaymentServiceReconciliationQueueTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentServiceReconciliationQueueTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [ManagerReportingServiceTests.java](../../src/test/java/com/scms/backend/membership/reporting/ManagerReportingServiceTests.java)
- [StaffMembershipHistoryServiceTests.java](../../src/test/java/com/scms/backend/membership/StaffMembershipHistoryServiceTests.java)

## POST /reception/members/{memberId}/reset-password

`ReceptionMemberController#resetPassword` → ResetPasswordService#receptionistResetMember. Tổng 2 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [ResetPasswordServiceTests.java](../../src/test/java/com/scms/backend/manager/ResetPasswordServiceTests.java)

## POST /reception/membership-freeze-requests/{requestId}/cash-payment

`MembershipFreezeController#cash` → MembershipFreezeService#collectCash. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeCashPaymentResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeCashPaymentResponse.java#L8); [MembershipFreezeDecisionRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeDecisionRequest.java#L3).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /reception/membership-freeze-requests/{requestId}/end-early

`MembershipFreezeController#receptionEndEarly` → MembershipFreezeService#endEarlyForReceptionist. Tổng 2 file test tham chiếu.

DTO: [MembershipFreezeDecisionRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeDecisionRequest.java#L3); [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10).

- [MembershipFreezeContractTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeContractTests.java)
- [MembershipFreezeServiceTests.java](../../src/test/java/com/scms/backend/membership/freeze/MembershipFreezeServiceTests.java)

## POST /reception/membership-orders/{orderId}/cash

`PaymentController#deprecatedCash` → Xem controller. Tổng 2 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [PaymentControllerTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentControllerTests.java)
- [PaymentOpenApiDocumentationTests.java](../../src/test/java/com/scms/backend/membership/payment/PaymentOpenApiDocumentationTests.java)

## GET /reception/pt-sessions

`PtSessionController#reception` → ClassSessionService#staffPtSchedule. Tổng 24 file test tham chiếu.

DTO: [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5).

- [CoachLeaveBatchServiceTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java)
- [CoachLeaveCancellationIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveCancellationIntegrationTests.java)
- [DisciplineDisableIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/discipline/DisciplineDisableIntegrationTests.java)
- [RoomSuspensionExtensionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionIntegrationTests.java)
- [RoomSuspensionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/room/RoomSuspensionServiceTests.java)
- [ClassSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentIntegrationTests.java)
- [ClassSessionAssignmentServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAssignmentServiceTests.java)
- [ClassSessionAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionAvailabilityControllerTests.java)
- [ClassSessionCancellationControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationControllerTests.java)
- [ClassSessionDetailServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionDetailServiceTests.java)
- [ClassSessionListIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListIntegrationTests.java)
- [ClassSessionListServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionListServiceTests.java)
- [ClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionServiceTests.java)
- [CoachAvailabilityControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/CoachAvailabilityControllerTests.java)
- [CourseSharedAvailabilityIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityIntegrationTests.java)
- [CourseSharedAvailabilityTests.java](../../src/test/java/com/scms/backend/scheduling/session/CourseSharedAvailabilityTests.java)
- [MemberClassScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/MemberClassScheduleServiceTests.java)
- [PtSessionAssignmentControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentControllerTests.java)
- [PtSessionAssignmentIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/PtSessionAssignmentIntegrationTests.java)
- [ReceptionClassSessionControllerTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionControllerTests.java)
- [ReceptionClassSessionServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/ReceptionClassSessionServiceTests.java)
- [RecurringScheduleServiceTests.java](../../src/test/java/com/scms/backend/scheduling/session/RecurringScheduleServiceTests.java)
- [SingleSessionIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/session/SingleSessionIntegrationTests.java)
- [SportClassDeactivateIntegrationTests.java](../../src/test/java/com/scms/backend/scheduling/sportclass/SportClassDeactivateIntegrationTests.java)

## GET /reception/refund-requests

`MembershipRefundController#receptionList` → MembershipRefundService#listForReceptionist. Tổng 5 file test tham chiếu.

DTO: [RefundRequestPageResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestPageResponse.java#L5).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## POST /reception/refund-requests/{requestId}/cash-disbursement

`MembershipRefundController#cashDisbursement` → MembershipRefundService#cashDisbursement. Tổng 5 file test tham chiếu.

DTO: [RefundCashDisbursementRequest](../../src/main/java/com/scms/backend/membership/refund/RefundCashDisbursementRequest.java#L3); [RefundRequestResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L13).

- [MembershipRefundContractTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundContractTests.java)
- [MembershipRefundIntegrationTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundIntegrationTests.java)
- [MembershipRefundServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/MembershipRefundServiceTests.java)
- [RefundDisplayServiceTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundDisplayServiceTests.java)
- [RefundStaffNotificationTests.java](../../src/test/java/com/scms/backend/membership/refund/RefundStaffNotificationTests.java)

## DELETE /staff/me/email-change

`StaffEmailChangeController#cancel` → AccountEmailChangeService#cancel. Tổng 2 file test tham chiếu.

DTO: Không có DTO riêng trong signature.

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /staff/me/email-change

`StaffEmailChangeController#begin` → AccountEmailChangeService#begin. Tổng 2 file test tham chiếu.

DTO: [StaffEmailChangeRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeRequest.java#L7); [StaffEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeResponse.java#L5).

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /staff/me/email-change/resend

`StaffEmailChangeController#resend` → AccountEmailChangeService#resend. Tổng 2 file test tham chiếu.

DTO: [StaffEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeResponse.java#L5).

- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## POST /staff/me/email-change/verify

`StaffEmailChangeController#verify` → AccountEmailChangeService#verify; RefreshTokenCookieService#clear. Tổng 5 file test tham chiếu.

DTO: [StaffEmailChangeVerifyRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeVerifyRequest.java#L5); [StaffEmailChangeVerifyResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeVerifyResponse.java#L3).

- [AuthenticationIntegrationTests.java](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java)
- [ChangePasswordIntegrationTests.java](../../src/test/java/com/scms/backend/auth/ChangePasswordIntegrationTests.java)
- [RefreshTokenCookieServiceTests.java](../../src/test/java/com/scms/backend/auth/RefreshTokenCookieServiceTests.java)
- [StaffEmailChangeIntegrationTests.java](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)

## GET /staff/me/profile

`StaffProfileController#get` → StaffProfileService#getProfile. Tổng 4 file test tham chiếu.

DTO: [StaffProfileResponse](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileResponse.java#L10).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## PATCH /staff/me/profile

`StaffProfileController#update` → StaffProfileService#updateProfile. Tổng 4 file test tham chiếu.

DTO: [StaffProfilePatchRequest](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfilePatchRequest.java#L7); [StaffProfileResponse](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileResponse.java#L10).

- [FeaturePackageStructureTests.java](../../src/test/java/com/scms/backend/architecture/FeaturePackageStructureTests.java)
- [StaffProfileServiceTests.java](../../src/test/java/com/scms/backend/identity/staffprofile/StaffProfileServiceTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## DELETE /staff/me/profile/image

`ProfileImageUploadController#removeStaffProfileImage` → ProfileImageUploadService#removeForStaff. Tổng 4 file test tham chiếu.

DTO: [StaffProfileResponse](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileResponse.java#L10).

- [ProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadControllerTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadControllerTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)

## POST /staff/me/profile/image

`ProfileImageUploadController#uploadStaffProfileImage` → ProfileImageUploadService#uploadForStaff. Tổng 4 file test tham chiếu.

DTO: [StaffProfileResponse](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileResponse.java#L10).

- [ProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadControllerTests.java)
- [ProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/ProfileImageUploadServiceTests.java)
- [StaffProfileImageUploadControllerTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadControllerTests.java)
- [StaffProfileImageUploadServiceTests.java](../../src/test/java/com/scms/backend/member/StaffProfileImageUploadServiceTests.java)
