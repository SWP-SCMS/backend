# Phụ lục API và class của Receptionist

Nguồn: bản cleanup local từ 9c08093. 104 mapping trong 29 controller của bộ tài liệu lễ tân. Có endpoint dùng chung, Manager hỗ trợ và webhook; quyền cụ thể nằm tại từng mục.

Prefix thực tế `/api/v1`. String memberId thường là mã MB-…, UUID memberId là account UUID.

## POST /auth/login

[AuthenticationController#login](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L31)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:129`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AuthenticationService#login.

DTO: AuthResponse, LoginRequest.

## POST /auth/refresh

[AuthenticationController#refresh](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L38)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:129`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AuthenticationService#refresh.

DTO: AuthResponse.

## POST /auth/logout

[AuthenticationController#logout](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L45)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:129`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AuthenticationService#logout; RefreshTokenCookieService#clear.

DTO: Xem signature.

## POST /auth/change-password

[AuthenticationController#changePassword](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L53)

Quyền filter: **Authenticated (fallback)**. Nguồn: `SecurityConfiguration#securityFilterChain`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AuthenticationService#changePassword; RefreshTokenCookieService#clear.

DTO: ChangePasswordRequest.

## POST /reception/center-visits

[CenterVisitController#checkIn](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L27)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:180`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CenterVisitService#checkIn.

DTO: CenterVisitRequest, CenterVisitResponse.

## PATCH /reception/members/{memberAccountId}/center-visits/current/checkout

[CenterVisitController#checkoutReceptionist](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L43)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CenterVisitService#checkoutReceptionist.

DTO: CenterVisitResponse.

## GET /reception/center-visits/open

[CenterVisitController#open](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L48)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:180`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CenterVisitService#openVisits.

DTO: CenterVisitPageResponse.

## GET /courses

[CourseCatalogController#listCourses](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L27)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:135`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseCatalogService#listCourses.

DTO: CatalogCoursePageResponse.

## GET /courses/{courseId}

[CourseCatalogController#getCourse](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L33)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:135`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseCatalogService#getCourse.

DTO: CatalogCourseResponse.

## GET /course-runs

[CourseCatalogController#listRuns](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L39)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:135`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseCatalogService#listRuns.

DTO: CatalogCourseRunPageResponse.

## GET /course-runs/{runId}

[CourseCatalogController#getRun](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L45)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:135`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseCatalogService#getRun.

DTO: CatalogCourseRunDetailResponse.

## GET /reception/members/{memberId}/course-enrollments

[CourseEnrollmentController#receptionEnrollments](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L36)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:172`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ReceptionCourseEnrollmentService#list.

DTO: ReceptionCourseEnrollmentPage.

## GET /reception/members/{memberId}/course-enrollments/{enrollmentId}

[CourseEnrollmentController#receptionEnrollment](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L41)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:172`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ReceptionCourseEnrollmentService#detail.

DTO: ReceptionCourseEnrollment.

## GET /reception/members/{memberId}/course-waitlists

[CourseReceptionActionsController#waitlists](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L57)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWaitlistService#mineForReceptionist.

DTO: CourseWaitlistResponse.

## POST /reception/members/{memberId}/course-runs/{runId}/waitlist

[CourseReceptionActionsController#join](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L62)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWaitlistService#join.

DTO: CourseWaitlistResponse.

## DELETE /reception/members/{memberId}/course-runs/{runId}/waitlist

[CourseReceptionActionsController#leave](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L67)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWaitlistService#leave.

DTO: CourseWaitlistResponse.

## POST /reception/members/{memberId}/course-waitlists/{waitlistId}/accept

[CourseReceptionActionsController#acceptWaitlist](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L72)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWaitlistService#accept.

DTO: CourseOrderResponse.

## GET /reception/members/{memberId}/course-schedule-changes

[CourseReceptionActionsController#changes](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L78)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseScheduleChangeService#listForReceptionist.

DTO: ScheduleChange.

## POST /reception/members/{memberId}/course-schedule-changes/{responseId}/response

[CourseReceptionActionsController#respond](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L83)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseScheduleChangeService#respond.

DTO: ScheduleChange, ScheduleChangeResponse.

## GET /reception/members/{memberId}/course-transfer-offers

[CourseReceptionActionsController#offers](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L88)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseTransferService#offersForReceptionist.

DTO: TransferOffer.

## POST /reception/members/{memberId}/course-transfer-offers/{offerId}/accept

[CourseReceptionActionsController#acceptTransfer](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L93)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseTransferService#accept.

DTO: TransferOffer.

## GET /reception/members/{memberId}/course-withdrawal-requests

[CourseReceptionActionsController#withdrawals](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L98)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWithdrawalService#listForReceptionist.

DTO: WithdrawalRequest.

## GET /reception/members/{memberId}/course-withdrawal-requests/{requestId}

[CourseReceptionActionsController#withdrawal](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L103)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWithdrawalService#detailForReceptionist.

DTO: WithdrawalRequest.

## GET /reception/members/{memberId}/course-refund-obligations

[CourseReceptionActionsController#refunds](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L108)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseRefundObligationService#memberHistoryForReceptionist.

DTO: MemberRefundObligationPage.

## GET /reception/members/{memberId}/course-refund-obligations/{obligationId}

[CourseReceptionActionsController#refund](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L113)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseRefundObligationService#memberDetailForReceptionist.

DTO: MemberRefundObligation.

## POST /reception/members/{memberId}/course-orders

[CourseOrderController#createForReceptionist](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L39)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:170`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseOrderService#createForReceptionist.

DTO: CourseOrderCreateRequest, CourseOrderResponse.

## GET /reception/members/{memberId}/course-orders/{orderId}

[CourseOrderController#getForReceptionist](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L44)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:171`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseOrderService#getForReceptionist.

DTO: CourseOrderResponse.

## GET /reception/course-refunds

[CourseRefundObligationController#receptionList](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L48)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:196`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseRefundObligationService#listForReceptionist.

DTO: ReceptionCourseRefundPage.

## GET /reception/course-refunds/{refundId}

[CourseRefundObligationController#receptionDetail](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L54)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:196`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseRefundObligationService#detailForReceptionist.

DTO: ReceptionCourseRefund.

## POST /reception/course-refund-obligations/{obligationId}/disburse

[CourseRefundObligationController#disburse](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L70)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:196`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseRefundObligationService#recordDisbursement.

DTO: RefundDisbursement, RefundObligation.

## POST /reception/members/{memberId}/course-enrollments/{enrollmentId}/withdrawal-requests

[CourseWithdrawalController#receptionCreate](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L42)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWithdrawalService#createForReceptionist.

DTO: WithdrawalCreate, WithdrawalRequest.

## POST /reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel

[CourseWithdrawalController#receptionCancel](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L52)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseWithdrawalService#cancelForReceptionist.

DTO: WithdrawalDecision, WithdrawalRequest.

## GET /accounts/me/email-change

[AccountEmailChangeController#status](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L39)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:270`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#status.

DTO: AccountEmailChangeStatus.

## POST /accounts/me/email-change

[AccountEmailChangeController#begin](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L44)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:270`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#begin.

DTO: AccountEmailChangeRequest, AccountEmailChangeResponse.

## PUT /accounts/me/email-change

[AccountEmailChangeController#replace](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L49)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:270`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#replace.

DTO: AccountEmailChangeRequest, AccountEmailChangeResponse.

## POST /accounts/me/email-change/verify

[AccountEmailChangeController#verify](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L54)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:270`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: RefreshTokenCookieService#clear; AccountEmailChangeService#verify.

DTO: AccountEmailChangeVerifyRequest, AccountEmailChangeVerifyResponse.

## POST /accounts/me/email-change/resend

[AccountEmailChangeController#resend](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L59)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:270`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#resend.

DTO: AccountEmailChangeResponse.

## DELETE /accounts/me/email-change

[AccountEmailChangeController#cancel](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L64)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:270`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#cancel.

DTO: Xem signature.

## POST /staff/me/email-change

[StaffEmailChangeController#begin](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L35)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:283`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#begin.

DTO: StaffEmailChangeRequest, StaffEmailChangeResponse.

## POST /staff/me/email-change/verify

[StaffEmailChangeController#verify](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L41)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:283`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#verify; RefreshTokenCookieService#clear.

DTO: StaffEmailChangeVerifyRequest, StaffEmailChangeVerifyResponse.

## POST /staff/me/email-change/resend

[StaffEmailChangeController#resend](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L47)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:283`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#resend.

DTO: StaffEmailChangeResponse.

## DELETE /staff/me/email-change

[StaffEmailChangeController#cancel](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L52)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:283`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: AccountEmailChangeService#cancel.

DTO: Xem signature.

## POST /auth/password-reset/request

[PasswordResetController#request](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L35)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:130`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PasswordResetService#request.

DTO: PasswordResetAcceptedResponse, PasswordResetRequest.

## POST /auth/password-reset/verify

[PasswordResetController#verify](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L41)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:130`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PasswordResetService#verify; PasswordResetCookieService#create.

DTO: PasswordResetVerifiedResponse, PasswordResetVerifyRequest.

## POST /auth/password-reset/confirm

[PasswordResetController#confirm](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L47)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:130`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PasswordResetService#confirm; PasswordResetCookieService#clear.

DTO: PasswordResetConfirmRequest.

## GET /reception/email-verification/pending-accounts

[ReceptionEmailVerificationController#search](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L35)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:178`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ManualActivationService#search.

DTO: PendingVerificationAccountResponse.

## POST /reception/email-verification/manual-activation-requests

[ReceptionEmailVerificationController#create](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L40)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:178`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ManualActivationService#create.

DTO: ManualActivationCreateRequest, ManualActivationResponse.

## GET /reception/email-verification/manual-activation-requests

[ReceptionEmailVerificationController#list](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L46)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:178`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ManualActivationService#list.

DTO: ManualActivationPageResponse, ManualActivationStatus.

## GET /reception/email-verification/manual-activation-requests/{requestId}

[ReceptionEmailVerificationController#get](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L51)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:178`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ManualActivationService#getForActor.

DTO: ManualActivationResponse.

## POST /reception/members/{memberId}/reset-password

[ReceptionMemberController#resetPassword](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L38)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ResetPasswordService#receptionistResetMember.

DTO: Xem signature.

## POST /reception/members

[ReceptionMemberController#createMember](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L45)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:174`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ReceptionMemberService#createMember.

DTO: ReceptionMemberCreateRequest, ReceptionMemberCreateResponse.

## GET /reception/members/search

[ReceptionMemberController#searchMembers](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L51)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ReceptionMemberProfileService#search.

DTO: ReceptionMemberSearchResponse.

## GET /reception/members/{memberId}/profile

[ReceptionMemberController#getMemberProfile](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L56)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ReceptionMemberProfileService#getProfile.

DTO: MemberProfileResponse.

## PATCH /reception/members/{memberId}/profile

[ReceptionMemberController#updateMemberProfile](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L61)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ReceptionMemberProfileService#updateProfile.

DTO: MemberProfilePatchRequest, MemberProfileResponse.

## GET /staff/me/profile

[StaffProfileController#get](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L23)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:282`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: StaffProfileService#getProfile.

DTO: StaffProfileResponse.

## PATCH /staff/me/profile

[StaffProfileController#update](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L28)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:282`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: StaffProfileService#updateProfile.

DTO: StaffProfilePatchRequest, StaffProfileResponse.

## GET /reception/members/{memberCode}/membership-freeze-requests

[MembershipFreezeController#receptionList](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L48)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:155`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipFreezeService#listForReceptionist.

DTO: MembershipFreezeRequestPageResponse.

## POST /reception/members/{memberCode}/membership-freeze-requests

[MembershipFreezeController#receptionCreate](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L53)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:156`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipFreezeService#createForReceptionist.

DTO: MembershipFreezeCreateRequest, MembershipFreezeRequestResponse.

## POST /reception/membership-freeze-requests/{requestId}/cash-payment

[MembershipFreezeController#cash](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L73)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:157`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipFreezeService#collectCash.

DTO: MembershipFreezeCashPaymentResponse, MembershipFreezeDecisionRequest.

## POST /reception/membership-freeze-requests/{requestId}/end-early

[MembershipFreezeController#receptionEndEarly](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L78)

Quyền filter: **Authenticated (fallback)**. Nguồn: `SecurityConfiguration#securityFilterChain`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipFreezeService#endEarlyForReceptionist.

DTO: MembershipFreezeDecisionRequest, MembershipFreezeRequestResponse.

## GET /membership-offers

[MembershipOfferController#listActiveOffers](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferController.java#L23)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:133`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipOfferService#listActiveOffers.

DTO: MembershipOfferResponse.

## GET /membership-offers/{offerId}

[MembershipOfferController#getActiveOffer](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferController.java#L29)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:133`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipOfferService#getActiveOffer.

DTO: MembershipOfferResponse.

## POST /reception/members/{memberId}/membership-orders

[MembershipOrderController#createForReceptionist](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L37)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:168`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipOrderService#createForReceptionist.

DTO: MembershipOrderCreateRequest, MembershipOrderResponse.

## GET /reception/members/{memberId}/membership-orders/pending

[MembershipOrderController#resumeForReceptionist](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L43)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:169`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipOrderService#resumeForReceptionist.

DTO: MembershipOrderResponse.

## GET /course-payments/reconciliation-queue

[CourseReconciliationController#queue](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L30)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:273`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseReconciliationService#queue.

DTO: Page, ReconciliationQueueStatus.

## GET /course-payments/{paymentId}

[CourseReconciliationController#detail](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L36)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:273`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseReconciliationService#detail.

DTO: Detail.

## GET /course-payments/{paymentId}/result

[CourseReconciliationController#result](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L42)

Quyền filter: **MEMBER, RECEPTIONIST, MANAGER**. Nguồn: `SecurityConfiguration.java:271`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseReconciliationService#result.

DTO: Result.

## POST /course-payments/{paymentId}/reconcile

[CourseReconciliationController#reconcile](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L48)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:273`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: CourseReconciliationService#reconcile.

DTO: PaymentActionRequest, Result.

## POST /reception/members/{memberId}/cash-payments

[PaymentController#cash](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L39)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:173`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#cash.

DTO: CashPaymentRequest, PaymentResultResponse.

## POST /reception/membership-orders/{orderId}/cash

[PaymentController#deprecatedCash](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L45)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:245`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: Xem controller.

DTO: Xem signature.

## POST /payments/{paymentId}/reconcile

[PaymentController#reconcile](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L51)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:274`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#reconcile.

DTO: PaymentActionRequest, PaymentResultResponse.

## PATCH /membership-orders/{orderId}/cancel

[PaymentController#cancelOrder](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L56)

Quyền filter: **RECEPTIONIST, MANAGER**. Nguồn: `SecurityConfiguration.java:272`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#cancelOrder.

DTO: MembershipOrderCancellationRequest, MembershipOrderCancellationResponse.

## GET /payments/reconciliation-queue

[PaymentController#reconciliationQueue](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L61)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:274`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#reconciliationQueue.

DTO: ReconciliationQueuePageResponse, ReconciliationQueueStatus.

## GET /reception/members/{memberId}/receipts

[PaymentController#receiptsForReceptionist](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L74)

Quyền filter: **RECEPTIONIST, MANAGER**. Nguồn: `SecurityConfiguration.java:175`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#receiptsForReceptionist.

DTO: ReceiptResponse.

## GET /receipts/{receiptId}

[PaymentController#receipt](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L79)

Quyền filter: **MEMBER, RECEPTIONIST, MANAGER**. Nguồn: `SecurityConfiguration.java:271`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#receipt.

DTO: ReceiptResponse.

## GET /payments/{paymentId}/result

[PaymentController#result](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L84)

Quyền filter: **MEMBER, RECEPTIONIST, MANAGER**. Nguồn: `SecurityConfiguration.java:271`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#result.

DTO: PaymentResultResponse.

## GET /reception/members/{memberCode}/memberships

[PaymentController#receptionMembershipHistory](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L94)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentService#receptionMembershipHistory.

DTO: StaffMembershipResponse.

## GET /payments/{paymentId}/evidence-images

[PaymentEvidenceController#list](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L28)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:274`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentEvidenceService#list.

DTO: PaymentEvidenceService.Evidence.

## POST /payments/{paymentId}/evidence-images

[PaymentEvidenceController#upload](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L33)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:274`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentEvidenceService#upload.

DTO: PaymentEvidenceService.Evidence.

## GET /payments/{paymentId}/evidence-images/{evidenceId}

[PaymentEvidenceController#read](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L38)

Quyền filter: **MANAGER, RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:274`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: PaymentEvidenceService#read.

DTO: Xem signature.

## POST /reception/members/{memberId}/membership-orders/{orderId}/payments/sepay

[SepayController#createForReceptionist](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L39)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: SepayService#createForReceptionist.

DTO: SepayPaymentResponse.

## POST /reception/members/{memberId}/course-orders/{orderId}/payments/sepay

[SepayController#createCoursePaymentForReceptionist](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L52)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: SepayService#createCoursePaymentForReceptionist.

DTO: CoursePaymentResponse.

## POST /reception/members/{memberId}/course-orders/{orderId}/payments/cash

[SepayController#fulfillCourseCash](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L59)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: SepayService#fulfillCourseCash.

DTO: CourseEnrollmentResponse.

## POST /payments/sepay/webhook

[SepayController#webhook](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L65)

Quyền filter: **PUBLIC**. Nguồn: `SecurityConfiguration.java:268`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: SepayService#webhook.

DTO: SepayWebhookRequest, SepayWebhookResponse.

## GET /reception/refund-requests

[MembershipRefundController#receptionList](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L27)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:148`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipRefundService#listForReceptionist.

DTO: RefundRequestPageResponse.

## GET /reception/members/{memberCode}/refund-eligible-sources

[MembershipRefundController#refundEligibleSources](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L32)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipRefundService#refundEligibleSources.

DTO: RefundEligibleSourceResponse.

## POST /reception/members/{memberCode}/refund-requests

[MembershipRefundController#create](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L37)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:149`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipRefundService#create.

DTO: RefundRequestCreateRequest, RefundRequestResponse.

## POST /reception/refund-requests/{requestId}/cash-disbursement

[MembershipRefundController#cashDisbursement](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L43)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:149`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipRefundService#cashDisbursement.

DTO: RefundCashDisbursementRequest, RefundRequestResponse.

## GET /reception/members/{memberCode}/membership-termination-requests

[MembershipTerminationController#receptionList](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L43)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:143`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipTerminationService#listForReceptionist.

DTO: MembershipTerminationRequestPageResponse.

## POST /reception/members/{memberCode}/membership-termination-requests

[MembershipTerminationController#receptionCreate](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L48)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:144`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: MembershipTerminationService#createForReceptionist.

DTO: MembershipTerminationCreateRequest, MembershipTerminationRequestResponse.

## POST /reception/members/{memberId}/profile/image

[ProfileImageUploadController#uploadReceptionMemberProfileImage](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L31)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ProfileImageUploadService#uploadForReceptionist.

DTO: MemberProfileResponse.

## POST /staff/me/profile/image

[ProfileImageUploadController#uploadStaffProfileImage](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L36)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:281`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ProfileImageUploadService#uploadForStaff.

DTO: StaffProfileResponse.

## DELETE /staff/me/profile/image

[ProfileImageUploadController#removeStaffProfileImage](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L41)

Quyền filter: **MANAGER, RECEPTIONIST, COACH**. Nguồn: `SecurityConfiguration.java:280`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ProfileImageUploadService#removeForStaff.

DTO: StaffProfileResponse.

## GET /notifications

[NotificationController#list](../../src/main/java/com/scms/backend/notification/NotificationController.java#L28)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:269`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: NotificationService#list.

DTO: NotificationPageResponse.

## PATCH /notifications/{notificationId}/read

[NotificationController#markRead](../../src/main/java/com/scms/backend/notification/NotificationController.java#L34)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:269`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: NotificationService#markRead.

DTO: NotificationReadResponse.

## PATCH /notifications/read-all

[NotificationController#markAllRead](../../src/main/java/com/scms/backend/notification/NotificationController.java#L40)

Quyền filter: **Authenticated**. Nguồn: `SecurityConfiguration.java:269`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: NotificationService#markAllRead.

DTO: NotificationReadResponse.

## POST /reception/members/{memberId}/bookings

[BookingController#bookForMember](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L47)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: BookingService#bookForMember.

DTO: BookingResponse, ReceptionBookingRequest.

## PATCH /reception/members/{memberId}/bookings/{bookingId}/cancel

[BookingController#cancelForMember](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L53)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: BookingService#cancelForMember.

DTO: BookingResponse.

## GET /reception/members/{memberId}/bookings

[BookingController#listForReceptionist](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L58)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: BookingService#listForReceptionist.

DTO: BookingSchedulePageResponse, BookingStatus.

## GET /reception/pt-sessions

[PtSessionController#reception](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L30)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:203`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ClassSessionService#staffPtSchedule.

DTO: MemberClassSessionPageResponse.

## GET /reception/class-sessions

[ReceptionClassSessionController#list](../../src/main/java/com/scms/backend/scheduling/session/ReceptionClassSessionController.java#L26)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:176`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: ClassSessionService#receptionSchedule.

DTO: MemberClassSessionPageResponse.

## POST /reception/members/{memberId}/class-sessions/{sessionId}/waitlist

[WaitlistController#joinFor](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L39)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: WaitlistService#joinForMember.

DTO: WaitlistResponse.

## DELETE /reception/members/{memberId}/class-sessions/{sessionId}/waitlist

[WaitlistController#leaveFor](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L44)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:179`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: WaitlistService#leaveForMember.

DTO: WaitlistResponse.

## GET /reception/class-sessions/{sessionId}/waitlist

[WaitlistController#reception](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L49)

Quyền filter: **RECEPTIONIST**. Nguồn: `SecurityConfiguration.java:234`. Điều kiện ACTIVE/ownership/trạng thái phải đọc trong service.

Luồng trực tiếp: WaitlistService#sessionList.

DTO: WaitlistResponse.
