# Bản đồ class lễ tân kèm số dòng

Nguồn: backend-refactor local từ 9c08093. Số dòng trỏ đến đầu khai báo, có thể bắt đầu bằng annotation. Dùng `Class#method` làm khóa tra cứu ổn định.

Phạm vi gồm các class trọng tâm của bộ tài liệu trước và phụ thuộc theo nghiệp vụ. Class dùng chung/Manager/Coach xuất hiện để giải thích phối hợp; không có nghĩa lễ tân được gọi mọi method.

## Class trọng tâm

### [AccountIdentifierAvailability](../../src/main/java/com/scms/backend/auth/AccountIdentifierAvailability.java#L10)

Package `com.scms.backend.auth`. Chống trùng email/điện thoại

- `AccountIdentifierAvailability#ensureAvailableForRegistration`: [L19](../../src/main/java/com/scms/backend/auth/AccountIdentifierAvailability.java#L19)
- `AccountIdentifierAvailability#ensureAvailableForUpdate`: [L28](../../src/main/java/com/scms/backend/auth/AccountIdentifierAvailability.java#L28)
- `AccountIdentifierAvailability#fromDatabase`: [L37](../../src/main/java/com/scms/backend/auth/AccountIdentifierAvailability.java#L37)
- `AccountIdentifierAvailability#exceptionMessages`: [L48](../../src/main/java/com/scms/backend/auth/AccountIdentifierAvailability.java#L48)

### [ActiveAccountJwtValidator](../../src/main/java/com/scms/backend/auth/ActiveAccountJwtValidator.java#L13)

Package `com.scms.backend.auth`. Kiểm tra account ACTIVE và authVersion

- `ActiveAccountJwtValidator#validate`: [L24](../../src/main/java/com/scms/backend/auth/ActiveAccountJwtValidator.java#L24)

### [AuthenticationController](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L18)

Package `com.scms.backend.auth`. Đăng nhập, refresh, logout, đổi mật khẩu

- `AuthenticationController#login`: [L31](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L31)
- `AuthenticationController#refresh`: [L38](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L38)
- `AuthenticationController#logout`: [L45](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L45)
- `AuthenticationController#changePassword`: [L53](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L53)
- `AuthenticationController#withRefreshCookie`: [L60](../../src/main/java/com/scms/backend/auth/AuthenticationController.java#L60)

### [AuthenticationService](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L13)

Package `com.scms.backend.auth`. Nghiệp vụ xác thực dùng chung

- `AuthenticationService#login`: [L34](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L34)
- `AuthenticationService#refresh`: [L49](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L49)
- `AuthenticationService#logout`: [L55](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L55)
- `AuthenticationService#changePassword`: [L60](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L60)
- `AuthenticationService#issueSession`: [L75](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L75)
- `AuthenticationService#findActiveAccount`: [L81](../../src/main/java/com/scms/backend/auth/AuthenticationService.java#L81)

### [JwtTokenService](../../src/main/java/com/scms/backend/auth/JwtTokenService.java#L14)

Package `com.scms.backend.auth`. Phát JWT

- `JwtTokenService#issue`: [L29](../../src/main/java/com/scms/backend/auth/JwtTokenService.java#L29)

### [RefreshTokenCookieService](../../src/main/java/com/scms/backend/auth/RefreshTokenCookieService.java#L7)

Package `com.scms.backend.auth`. Cookie phiên đăng nhập

- `RefreshTokenCookieService#create`: [L20](../../src/main/java/com/scms/backend/auth/RefreshTokenCookieService.java#L20)
- `RefreshTokenCookieService#clear`: [L24](../../src/main/java/com/scms/backend/auth/RefreshTokenCookieService.java#L24)
- `RefreshTokenCookieService#baseCookie`: [L28](../../src/main/java/com/scms/backend/auth/RefreshTokenCookieService.java#L28)

### [RefreshTokenService](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L17)

Package `com.scms.backend.auth`. Cấp, xoay và thu hồi refresh token

- `RefreshTokenService#issue`: [L39](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L39)
- `RefreshTokenService#rotate`: [L44](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L44)
- `RefreshTokenService#revokeIfPresent`: [L61](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L61), [L69](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L69)
- `RefreshTokenService#revokeAll`: [L78](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L78)
- `RefreshTokenService#create`: [L85](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L85)
- `RefreshTokenService#generateToken`: [L92](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L92)
- `RefreshTokenService#hash`: [L98](../../src/main/java/com/scms/backend/auth/RefreshTokenService.java#L98)

### [CenterVisitController](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L18)

Package `com.scms.backend.centervisit`. API check-in/check-out và khách đang ở trong trung tâm

- `CenterVisitController#checkIn`: [L27](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L27)
- `CenterVisitController#current`: [L33](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L33)
- `CenterVisitController#checkoutMember`: [L38](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L38)
- `CenterVisitController#checkoutReceptionist`: [L43](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L43)
- `CenterVisitController#open`: [L48](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L48)
- `CenterVisitController#accountId`: [L56](../../src/main/java/com/scms/backend/centervisit/CenterVisitController.java#L56)

### [CenterVisitService](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L24)

Package `com.scms.backend.centervisit`. Kiểm tra quyền vào cửa và ghi visit

- `CenterVisitService#checkIn`: [L48](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L48)
- `CenterVisitService#findReplay`: [L107](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L107)
- `CenterVisitService#currentMember`: [L118](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L118)
- `CenterVisitService#checkoutMember`: [L126](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L126)
- `CenterVisitService#checkoutReceptionist`: [L135](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L135)
- `CenterVisitService#openVisits`: [L142](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L142)
- `CenterVisitService#checkout`: [L158](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L158)
- `CenterVisitService#close`: [L170](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L170)
- `CenterVisitService#normalizeKey`: [L175](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L175)
- `CenterVisitService#lockMember`: [L185](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L185)
- `CenterVisitService#ensureActiveMember`: [L192](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L192)
- `CenterVisitService#ensureActiveReceptionist`: [L198](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L198)
- `CenterVisitService#memberCode`: [L204](../../src/main/java/com/scms/backend/centervisit/CenterVisitService.java#L204)

### [GlobalExceptionHandler](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L93)

Package `com.scms.backend.common.error`. Chuyển lỗi nghiệp vụ thành HTTP response dùng chung

- `GlobalExceptionHandler#supports`: [L98](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L98)
- `GlobalExceptionHandler#beforeBodyWrite`: [L103](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L103)
- `GlobalExceptionHandler#handleMethodArgumentNotValid`: [L108](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L108)
- `GlobalExceptionHandler#handleHttpMessageNotReadable`: [L121](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L121)
- `GlobalExceptionHandler#handleTypeMismatch`: [L129](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L129)
- `GlobalExceptionHandler#handleInvalidCredentials`: [L137](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L137)
- `GlobalExceptionHandler#handleInvalidRefreshToken`: [L142](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L142)
- `GlobalExceptionHandler#handleInvalidAuthenticatedAccount`: [L147](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L147)
- `GlobalExceptionHandler#handleCurrentPasswordIncorrect`: [L152](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L152)
- `GlobalExceptionHandler#handleChangePasswordValidation`: [L157](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L157)
- `GlobalExceptionHandler#handleNewPasswordSameAsCurrent`: [L162](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L162)
- `GlobalExceptionHandler#handleRegistrationValidation`: [L167](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L167)
- `GlobalExceptionHandler#handleDuplicateAccount`: [L172](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L172)
- `GlobalExceptionHandler#handlePendingRegistration`: [L177](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L177)
- `GlobalExceptionHandler#handleEmailVerification`: [L182](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L182)
- `GlobalExceptionHandler#handlePasswordReset`: [L187](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L187)
- `GlobalExceptionHandler#handleMemberProfileValidation`: [L192](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L192)
- `GlobalExceptionHandler#handleReceptionMemberValidation`: [L197](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L197)
- `GlobalExceptionHandler#handleStaffAccountValidation`: [L202](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L202)
- `GlobalExceptionHandler#handleStaffAccountNotFound`: [L207](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L207)
- `GlobalExceptionHandler#handleMemberAccountNotFound`: [L212](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L212)
- `GlobalExceptionHandler#handleMemberStatusConflict`: [L217](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L217)
- `GlobalExceptionHandler#handleStaffStatusConflict`: [L222](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L222)
- `GlobalExceptionHandler#handleDisciplineValidation`: [L227](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L227)
- `GlobalExceptionHandler#handleDisciplineNotFound`: [L232](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L232)
- `GlobalExceptionHandler#handleDuplicateDiscipline`: [L237](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L237)
- `GlobalExceptionHandler#handleSportClass`: [L242](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L242)
- `GlobalExceptionHandler#handleRoom`: [L247](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L247)
- `GlobalExceptionHandler#handleCourse`: [L252](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L252)
- `GlobalExceptionHandler#handleCourseOrder`: [L257](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L257)
- `GlobalExceptionHandler#handleCourseWaitlist`: [L262](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L262)
- `GlobalExceptionHandler#handleCourseTransfer`: [L267](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L267)
- `GlobalExceptionHandler#handleCourseWithdrawal`: [L272](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L272)
- `GlobalExceptionHandler#handleCourseRefund`: [L277](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L277)
- `GlobalExceptionHandler#handleRecurringSchedule`: [L282](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L282)
- `GlobalExceptionHandler#handlePtSessionAssignment`: [L287](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L287)
- `GlobalExceptionHandler#handleBooking`: [L292](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L292)
- `GlobalExceptionHandler#handleCoachLeave`: [L297](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L297)
- `GlobalExceptionHandler#handleWaitlist`: [L302](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L302)
- `GlobalExceptionHandler#handleAttendance`: [L307](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L307)
- `GlobalExceptionHandler#handleCourseAttendance`: [L312](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L312)
- `GlobalExceptionHandler#handleReceptionMemberNotFound`: [L317](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L317)
- `GlobalExceptionHandler#handleCenterVisit`: [L322](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L322)
- `GlobalExceptionHandler#handleInvalidPlanCode`: [L327](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L327)
- `GlobalExceptionHandler#handleMembershipOfferNotFound`: [L332](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L332)
- `GlobalExceptionHandler#handleMembershipOfferUnavailable`: [L337](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L337)
- `GlobalExceptionHandler#handleMembershipOrderMemberNotFound`: [L342](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L342)
- `GlobalExceptionHandler#handleMemberNotActive`: [L347](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L347)
- `GlobalExceptionHandler#handleActiveMembershipExists`: [L352](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L352)
- `GlobalExceptionHandler#handlePendingMembershipOrderExists`: [L357](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L357)
- `GlobalExceptionHandler#handleMembershipRenewalAlreadyExists`: [L362](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L362)
- `GlobalExceptionHandler#handleSepayValidation`: [L367](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L367)
- `GlobalExceptionHandler#handleMembershipOfferValidation`: [L372](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L372)
- `GlobalExceptionHandler#handleMembershipPlanValidation`: [L377](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L377)
- `GlobalExceptionHandler#handleMembershipPlanConflict`: [L382](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L382)
- `GlobalExceptionHandler#handleMembershipPlanNotFound`: [L387](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L387)
- `GlobalExceptionHandler#handleMembershipTermination`: [L392](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L392)
- `GlobalExceptionHandler#handleMembershipFreeze`: [L397](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L397)
- `GlobalExceptionHandler#handleRefund`: [L402](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L402)
- `GlobalExceptionHandler#handleNotificationNotFound`: [L407](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L407)
- `GlobalExceptionHandler#handleNotificationValidation`: [L412](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L412)
- `GlobalExceptionHandler#handleAuditHistoryValidation`: [L417](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L417)
- `GlobalExceptionHandler#handlePayment`: [L422](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L422)
- `GlobalExceptionHandler#handleReportingValidation`: [L427](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L427)
- `GlobalExceptionHandler#handleCoachAssignment`: [L432](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L432)
- `GlobalExceptionHandler#handleTrainingPlan`: [L437](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L437)
- `GlobalExceptionHandler#handleTrainingResult`: [L442](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L442)
- `GlobalExceptionHandler#handleTrainingFeedback`: [L447](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L447)
- `GlobalExceptionHandler#handleMaxUploadSizeExceededException`: [L452](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L452)
- `GlobalExceptionHandler#handleUnexpected`: [L461](../../src/main/java/com/scms/backend/common/error/GlobalExceptionHandler.java#L461)

### [SecurityProblemDetailHandler](../../src/main/java/com/scms/backend/common/error/SecurityProblemDetailHandler.java#L17)

Package `com.scms.backend.common.error`. Phản hồi 401/403 dùng chung

- `SecurityProblemDetailHandler#commence`: [L26](../../src/main/java/com/scms/backend/common/error/SecurityProblemDetailHandler.java#L26)
- `SecurityProblemDetailHandler#handle`: [L32](../../src/main/java/com/scms/backend/common/error/SecurityProblemDetailHandler.java#L32)
- `SecurityProblemDetailHandler#write`: [L37](../../src/main/java/com/scms/backend/common/error/SecurityProblemDetailHandler.java#L37)

### [SecurityConfiguration](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L38)

Package `com.scms.backend.config`. Quyền API, JWT và mật khẩu

- `SecurityConfiguration#securityFilterChain`: [L51](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L51)
- `SecurityConfiguration#passwordEncoder`: [L81](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L81)
- `SecurityConfiguration#jwtSecretKey`: [L86](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L86)
- `SecurityConfiguration#jwtEncoder`: [L91](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L91)
- `SecurityConfiguration#jwtDecoder`: [L96](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L96)
- `SecurityConfiguration#corsConfigurationSource`: [L104](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L104)
- `SecurityConfiguration#jwtAuthenticationConverter`: [L117](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L117)
- `SecurityConfiguration#publicEndpoints`: [L126](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L126)
- `SecurityConfiguration#membershipDecisions`: [L138](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L138)
- `SecurityConfiguration#ordersAndReception`: [L162](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L162)
- `SecurityConfiguration#managementAndSessions`: [L184](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L184)
- `SecurityConfiguration#trainingAndBookings`: [L211](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L211)
- `SecurityConfiguration#commerceAndMemberData`: [L241](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L241)
- `SecurityConfiguration#sharedAndStaffAccess`: [L266](../../src/main/java/com/scms/backend/config/SecurityConfiguration.java#L266)

### [CourseCatalogController](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L17)

Package `com.scms.backend.course.catalog`. Danh mục khóa/đợt để tư vấn

- `CourseCatalogController#listCourses`: [L27](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L27)
- `CourseCatalogController#getCourse`: [L33](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L33)
- `CourseCatalogController#listRuns`: [L39](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L39)
- `CourseCatalogController#getRun`: [L45](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L45)
- `CourseCatalogController#page`: [L51](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogController.java#L51)

### [CourseCatalogService](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogService.java#L17)

Package `com.scms.backend.course.catalog`. Truy vấn danh mục khóa công khai

- `CourseCatalogService#listCourses`: [L35](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogService.java#L35)
- `CourseCatalogService#getCourse`: [L40](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogService.java#L40)
- `CourseCatalogService#listRuns`: [L45](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogService.java#L45)
- `CourseCatalogService#getRun`: [L51](../../src/main/java/com/scms/backend/course/catalog/CourseCatalogService.java#L51)

### [CourseImageResources](../../src/main/java/com/scms/backend/course/catalog/CourseImageResources.java#L9)

Package `com.scms.backend.course.catalog`. Phục vụ ảnh khóa học

- `CourseImageResources#addResourceHandlers`: [L18](../../src/main/java/com/scms/backend/course/catalog/CourseImageResources.java#L18)

### [CourseEnrollmentController](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L24)

Package `com.scms.backend.course.enrollment`. Tra cứu enrollment của hội viên cho lễ tân

- `CourseEnrollmentController#receptionEnrollments`: [L36](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L36)
- `CourseEnrollmentController#receptionEnrollment`: [L41](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L41)
- `CourseEnrollmentController#subject`: [L46](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L46)
- `CourseEnrollmentController#memberEnrollments`: [L50](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L50)
- `CourseEnrollmentController#scheduleChanges`: [L55](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L55)
- `CourseEnrollmentController#respondToScheduleChange`: [L60](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentController.java#L60)

### [CourseReceptionActionsController](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L35)

Package `com.scms.backend.course.enrollment`. API chờ khóa, đổi lịch, chuyển/rút và lịch sử hoàn

- `CourseReceptionActionsController#waitlists`: [L57](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L57)
- `CourseReceptionActionsController#join`: [L62](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L62)
- `CourseReceptionActionsController#leave`: [L67](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L67)
- `CourseReceptionActionsController#acceptWaitlist`: [L72](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L72)
- `CourseReceptionActionsController#changes`: [L78](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L78)
- `CourseReceptionActionsController#respond`: [L83](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L83)
- `CourseReceptionActionsController#offers`: [L88](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L88)
- `CourseReceptionActionsController#acceptTransfer`: [L93](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L93)
- `CourseReceptionActionsController#withdrawals`: [L98](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L98)
- `CourseReceptionActionsController#withdrawal`: [L103](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L103)
- `CourseReceptionActionsController#refunds`: [L108](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L108)
- `CourseReceptionActionsController#refund`: [L113](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L113)
- `CourseReceptionActionsController#subject`: [L118](../../src/main/java/com/scms/backend/course/enrollment/CourseReceptionActionsController.java#L118)

### [ReceptionCourseEnrollmentRepository](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L18)

Package `com.scms.backend.course.enrollment`. Component/repository tách từ ReceptionCourseEnrollmentService; facade giữ entrypoint và transaction.

- `ReceptionCourseEnrollmentRepository#select`: [L29](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L29)
- `ReceptionCourseEnrollmentRepository#map`: [L33](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L33)
- `ReceptionCourseEnrollmentRepository#amount`: [L44](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L44)
- `ReceptionCourseEnrollmentRepository#enrollmentPage`: [L48](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L48)
- `ReceptionCourseEnrollmentRepository#enrollmentDetail`: [L56](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentRepository.java#L56)

### [ReceptionCourseEnrollmentService](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L19)

Package `com.scms.backend.course.enrollment`. Danh sách/chi tiết enrollment và eligibility

- `ReceptionCourseEnrollmentService#list`: [L35](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L35)
- `ReceptionCourseEnrollmentService#detail`: [L43](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L43)
- `ReceptionCourseEnrollmentService#amount`: [L49](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L49)
- `ReceptionCourseEnrollmentService#status`: [L53](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L53)
- `ReceptionCourseEnrollmentService#active`: [L62](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L62)
- `ReceptionCourseEnrollmentService#page`: [L67](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L67)
- `ReceptionCourseEnrollmentService#enrollmentPage`: [L72](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L72)
- `ReceptionCourseEnrollmentService#enrollmentDetail`: [L76](../../src/main/java/com/scms/backend/course/enrollment/ReceptionCourseEnrollmentService.java#L76)

### [CourseOrderController](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L20)

Package `com.scms.backend.course.order`. API mua khóa học thay hội viên

- `CourseOrderController#createForMember`: [L29](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L29)
- `CourseOrderController#getForMember`: [L34](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L34)
- `CourseOrderController#createForReceptionist`: [L39](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L39)
- `CourseOrderController#getForReceptionist`: [L44](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L44)
- `CourseOrderController#createdOrReplayed`: [L49](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L49)
- `CourseOrderController#accountId`: [L53](../../src/main/java/com/scms/backend/course/order/CourseOrderController.java#L53)

### [CourseOrderRepository](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L12)

Package `com.scms.backend.course.order`. Component/repository tách từ CourseOrderService; facade giữ entrypoint và transaction.

- `CourseOrderRepository#countMemberHeldReservations`: [L14](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L14)
- `CourseOrderRepository#countMemberEnrollments`: [L21](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L21)
- `CourseOrderRepository#countRunEnrollments`: [L25](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L25)
- `CourseOrderRepository#countRunHeldReservations`: [L29](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L29)
- `CourseOrderRepository#createCountCourseWaitlists`: [L35](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L35)
- `CourseOrderRepository#createCountCourseTransferHolds`: [L42](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L42)
- `CourseOrderRepository#createInsertCourseOrders`: [L48](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L48)
- `CourseOrderRepository#createInsertCourseReservations`: [L56](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L56)
- `CourseOrderRepository#hasHeldCourseOverlapFindCourseEnrollments`: [L64](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L64)
- `CourseOrderRepository#findByKeyFindRows`: [L86](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L86)
- `CourseOrderRepository#findOrderFindRows`: [L90](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L90)
- `CourseOrderRepository#count`: [L94](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L94)
- `CourseOrderRepository#map`: [L98](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L98)
- `CourseOrderRepository#orderQuery`: [L109](../../src/main/java/com/scms/backend/course/order/CourseOrderRepository.java#L109)

### [CourseOrderService](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L32)

Package `com.scms.backend.course.order`. Tạo order và giữ chỗ 15 phút

- `CourseOrderService#createForMember`: [L69](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L69)
- `CourseOrderService#createForReceptionist`: [L76](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L76)
- `CourseOrderService#createFromWaitlist`: [L84](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L84)
- `CourseOrderService#getForMember`: [L91](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L91)
- `CourseOrderService#getForReceptionist`: [L99](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L99)
- `CourseOrderService#holdExpiresAt`: [L105](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L105)
- `CourseOrderService#create`: [L110](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L110)
- `CourseOrderService#current`: [L150](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L150)
- `CourseOrderService#validateRun`: [L159](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L159)
- `CourseOrderService#validateSchedule`: [L168](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L168)
- `CourseOrderService#hasHeldCourseOverlap`: [L178](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L178)
- `CourseOrderService#lockActive`: [L182](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L182)
- `CourseOrderService#lockActiveMember`: [L186](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L186)
- `CourseOrderService#findMember`: [L194](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L194)
- `CourseOrderService#normalizeKey`: [L200](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L200)
- `CourseOrderService#findByKey`: [L207](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L207)
- `CourseOrderService#findOrder`: [L212](../../src/main/java/com/scms/backend/course/order/CourseOrderService.java#L212)

### [CourseReservationExpiryRepository](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L9)

Package `com.scms.backend.course.order`. Component/repository tách từ CourseReservationExpiryService; facade giữ entrypoint và transaction.

- `CourseReservationExpiryRepository#findDueReservationIdsFindCourseReservations`: [L11](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L11)
- `CourseReservationExpiryRepository#expireReservationFindCourseReservations`: [L19](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L19)
- `CourseReservationExpiryRepository#expireReservationFindCourseRuns`: [L26](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L26)
- `CourseReservationExpiryRepository#expireReservationFindCourseOrders`: [L30](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L30)
- `CourseReservationExpiryRepository#lockReservationForExpiry`: [L34](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L34)
- `CourseReservationExpiryRepository#expireReservationUpdateCourseReservations`: [L38](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L38)
- `CourseReservationExpiryRepository#expireReservationUpdateCourseOrders`: [L46](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L46)
- `CourseReservationExpiryRepository#expireReservationFindCoursePayments`: [L53](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L53)
- `CourseReservationExpiryRepository#lockPaymentForExpiry`: [L57](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L57)
- `CourseReservationExpiryRepository#expireReservationUpdateCoursePayments`: [L61](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L61)
- `CourseReservationExpiryRepository#expireDueForRunFindCourseReservations`: [L65](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryRepository.java#L65)

### [CourseReservationExpiryScheduler](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryScheduler.java#L10)

Package `com.scms.backend.course.order`. Tác vụ nền hết hạn giữ chỗ

- `CourseReservationExpiryScheduler#expireAvailable`: [L28](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryScheduler.java#L28)

### [CourseReservationExpiryService](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryService.java#L14)

Package `com.scms.backend.course.order`. Giải phóng hold hết hạn

- `CourseReservationExpiryService#findDueReservationIds`: [L27](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryService.java#L27)
- `CourseReservationExpiryService#expireReservation`: [L31](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryService.java#L31)
- `CourseReservationExpiryService#expireDueForRun`: [L53](../../src/main/java/com/scms/backend/course/order/CourseReservationExpiryService.java#L53)

### [CourseRefundObligationController](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L29)

Package `com.scms.backend.course.refund`. Queue và ghi chi hoàn khóa học

- `CourseRefundObligationController#list`: [L38](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L38)
- `CourseRefundObligationController#detail`: [L43](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L43)
- `CourseRefundObligationController#receptionList`: [L48](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L48)
- `CourseRefundObligationController#receptionDetail`: [L54](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L54)
- `CourseRefundObligationController#approve`: [L60](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L60)
- `CourseRefundObligationController#reject`: [L65](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L65)
- `CourseRefundObligationController#disburse`: [L70](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L70)
- `CourseRefundObligationController#verify`: [L75](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L75)
- `CourseRefundObligationController#fail`: [L80](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L80)
- `CourseRefundObligationController#returned`: [L85](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L85)
- `CourseRefundObligationController#memberList`: [L90](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L90)
- `CourseRefundObligationController#memberDetail`: [L95](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L95)
- `CourseRefundObligationController#subject`: [L100](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L100)
- `CourseRefundObligationController#version`: [L104](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L104)
- `CourseRefundObligationController#reason`: [L108](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationController.java#L108)

### [CourseRefundObligationRepository](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L18)

Package `com.scms.backend.course.refund`. Component/repository tách từ CourseRefundObligationService; facade giữ entrypoint và transaction.

- `CourseRefundObligationRepository#find`: [L26](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L26)
- `CourseRefundObligationRepository#select`: [L30](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L30)
- `CourseRefundObligationRepository#receptionSelect`: [L34](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L34)
- `CourseRefundObligationRepository#map`: [L56](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L56)
- `CourseRefundObligationRepository#mapReception`: [L60](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L60)
- `CourseRefundObligationRepository#amount`: [L64](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L64)
- `CourseRefundObligationRepository#instant`: [L68](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L68)
- `CourseRefundObligationRepository#receptionRefundPage`: [L72](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L72)
- `CourseRefundObligationRepository#listFindCourseRefundObligations`: [L80](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L80)
- `CourseRefundObligationRepository#listFindRows`: [L84](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L84)
- `CourseRefundObligationRepository#listForMemberFindCourseRefundObligations`: [L88](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L88)
- `CourseRefundObligationRepository#listForMemberFindRows`: [L92](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L92)
- `CourseRefundObligationRepository#detailForMemberFindRows`: [L96](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L96)
- `CourseRefundObligationRepository#detailForReceptionistFindRows`: [L100](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L100)
- `CourseRefundObligationRepository#approveUpdateCourseRefundObligations`: [L104](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L104)
- `CourseRefundObligationRepository#rejectUpdateCourseRefundObligations`: [L112](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L112)
- `CourseRefundObligationRepository#recordDisbursementFindCourseRefundObligations`: [L119](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L119)
- `CourseRefundObligationRepository#recordDisbursementUpdateCourseRefundObligations`: [L123](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L123)
- `CourseRefundObligationRepository#verifyUpdateCourseRefundObligations`: [L132](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L132)
- `CourseRefundObligationRepository#completeVerifiedRefund`: [L136](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L136)
- `CourseRefundObligationRepository#terminalUpdateCourseRefundObligations`: [L144](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L144)
- `CourseRefundObligationRepository#lockFindCourseRefundObligations`: [L148](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L148)
- `CourseRefundObligationRepository#lockFindCourseRuns`: [L152](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L152)
- `CourseRefundObligationRepository#lockFindCourseEnrollments`: [L156](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L156)
- `CourseRefundObligationRepository#lockFindOf`: [L160](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L160)
- `CourseRefundObligationRepository#findDisbursementIdempotencyKey`: [L164](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationRepository.java#L164)

### [CourseRefundObligationService](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L36)

Package `com.scms.backend.course.refund`. Chi hoàn và lịch sử; approve/verify thuộc Manager

- `CourseRefundObligationService#list`: [L62](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L62)
- `CourseRefundObligationService#detail`: [L73](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L73)
- `CourseRefundObligationService#memberHistoryForReceptionist`: [L79](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L79)
- `CourseRefundObligationService#memberDetailForReceptionist`: [L85](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L85)
- `CourseRefundObligationService#listForMember`: [L91](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L91)
- `CourseRefundObligationService#detailForMember`: [L100](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L100)
- `CourseRefundObligationService#listForReceptionist`: [L106](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L106)
- `CourseRefundObligationService#detailForReceptionist`: [L113](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L113)
- `CourseRefundObligationService#approve`: [L119](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L119)
- `CourseRefundObligationService#reject`: [L143](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L143)
- `CourseRefundObligationService#recordDisbursement`: [L162](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L162)
- `CourseRefundObligationService#verify`: [L200](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L200)
- `CourseRefundObligationService#fail`: [L232](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L232)
- `CourseRefundObligationService#returned`: [L237](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L237)
- `CourseRefundObligationService#terminal`: [L242](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L242)
- `CourseRefundObligationService#lockForDecision`: [L261](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L261)
- `CourseRefundObligationService#lock`: [L265](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L265)
- `CourseRefundObligationService#active`: [L278](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L278)
- `CourseRefundObligationService#reason`: [L289](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L289)
- `CourseRefundObligationService#status`: [L293](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L293)
- `CourseRefundObligationService#page`: [L297](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L297)
- `CourseRefundObligationService#key`: [L301](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L301)
- `CourseRefundObligationService#disbursement`: [L305](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L305)
- `CourseRefundObligationService#verification`: [L309](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L309)
- `CourseRefundObligationService#matches`: [L313](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L313)
- `CourseRefundObligationService#idempotencyConflict`: [L317](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L317)
- `CourseRefundObligationService#transition`: [L321](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L321)
- `CourseRefundObligationService#version`: [L325](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L325)
- `CourseRefundObligationService#changed`: [L329](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L329)
- `CourseRefundObligationService#alreadyDecided`: [L333](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L333)
- `CourseRefundObligationService#find`: [L337](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L337)
- `CourseRefundObligationService#map`: [L341](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L341)
- `CourseRefundObligationService#member`: [L345](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L345)
- `CourseRefundObligationService#notifyMember`: [L349](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L349)
- `CourseRefundObligationService#amount`: [L353](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L353)
- `CourseRefundObligationService#instant`: [L357](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L357)
- `CourseRefundObligationService#receptionRefundPage`: [L372](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationService.java#L372)

### [CourseRefundStateMachine](../../src/main/java/com/scms/backend/course/refund/CourseRefundStateMachine.java#L7)

Package `com.scms.backend.course.refund`. Chuyển trạng thái hoàn tiền khóa

- `CourseRefundStateMachine#canTransition`: [L14](../../src/main/java/com/scms/backend/course/refund/CourseRefundStateMachine.java#L14)

### [CourseRefundValidation](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L11)

Package `com.scms.backend.course.refund`. Component/repository tách từ CourseRefundObligationService; facade giữ entrypoint và transaction.

- `CourseRefundValidation#reason`: [L13](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L13)
- `CourseRefundValidation#status`: [L19](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L19)
- `CourseRefundValidation#page`: [L29](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L29)
- `CourseRefundValidation#key`: [L34](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L34)
- `CourseRefundValidation#disbursement`: [L40](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L40)
- `CourseRefundValidation#verification`: [L50](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L50)
- `CourseRefundValidation#vnd`: [L57](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L57)
- `CourseRefundValidation#blank`: [L61](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L61)
- `CourseRefundValidation#matches`: [L65](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L65)
- `CourseRefundValidation#idempotencyConflict`: [L69](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L69)
- `CourseRefundValidation#transition`: [L73](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L73)
- `CourseRefundValidation#version`: [L77](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L77)
- `CourseRefundValidation#changed`: [L84](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L84)
- `CourseRefundValidation#alreadyDecided`: [L88](../../src/main/java/com/scms/backend/course/refund/CourseRefundValidation.java#L88)

### [CourseScheduleChangeNotifications](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L21)

Package `com.scms.backend.course.schedulechange`. Component/repository tách từ CourseScheduleChangeService; facade giữ entrypoint và transaction.

- `CourseScheduleChangeNotifications#notifyReschedule`: [L38](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L38)
- `CourseScheduleChangeNotifications#createRunScheduleResponses`: [L72](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L72)
- `CourseScheduleChangeNotifications#prepareAtomicRunResponses`: [L91](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L91)
- `CourseScheduleChangeNotifications#writeAtomicRunResponses`: [L122](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L122)
- `CourseScheduleChangeNotifications#timestamp`: [L142](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L142)
- `CourseScheduleChangeNotifications#conflictingEnrollmentSessions`: [L146](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L146), [L150](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeNotifications.java#L150)

### [CourseScheduleChangePolicy](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangePolicy.java#L7)

Package `com.scms.backend.course.schedulechange`. Điều kiện và thời hạn phản hồi đổi lịch

- `CourseScheduleChangePolicy#isMaterial`: [L16](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangePolicy.java#L16)
- `CourseScheduleChangePolicy#deadline`: [L20](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangePolicy.java#L20)

### [CourseScheduleChangeRepository](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L17)

Package `com.scms.backend.course.schedulechange`. Component/repository tách từ CourseScheduleChangeNotifications, CourseScheduleChangeService; facade giữ entrypoint và transaction.

- `CourseScheduleChangeRepository#reload`: [L28](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L28)
- `CourseScheduleChangeRepository#select`: [L33](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L33)
- `CourseScheduleChangeRepository#map`: [L49](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L49)
- `CourseScheduleChangeRepository#timestamp`: [L72](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L72)
- `CourseScheduleChangeRepository#conflictingEnrollmentSessions`: [L76](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L76), [L80](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L80)
- `CourseScheduleChangeRepository#notifyRescheduleFindCourseEnrollmentSessions`: [L107](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L107)
- `CourseScheduleChangeRepository#notifyRescheduleInsertCourseScheduleChangeEvents`: [L117](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L117)
- `CourseScheduleChangeRepository#notifyRescheduleUpdateCourseScheduleChangeResponses`: [L125](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L125)
- `CourseScheduleChangeRepository#notifyRescheduleInsertCourseScheduleChangeResponses`: [L129](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L129)
- `CourseScheduleChangeRepository#createRunScheduleResponsesFindCourseEnrollmentSessions`: [L137](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L137)
- `CourseScheduleChangeRepository#createRunScheduleResponsesUpdateCourseScheduleChangeResponses`: [L147](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L147)
- `CourseScheduleChangeRepository#createRunScheduleResponsesInsertCourseScheduleChangeResponses`: [L151](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L151)
- `CourseScheduleChangeRepository#prepareAtomicRunResponsesFindCourseEnrollmentSessions`: [L159](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L159)
- `CourseScheduleChangeRepository#writeAtomicRunResponsesUpdateCourseScheduleChangeResponses`: [L168](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L168)
- `CourseScheduleChangeRepository#writeAtomicRunResponsesInsertCourseScheduleChangeResponses`: [L172](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L172)
- `CourseScheduleChangeRepository#listFindRows`: [L179](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L179)
- `CourseScheduleChangeRepository#respondFindCourseScheduleChangeResponses`: [L183](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L183)
- `CourseScheduleChangeRepository#respondFindCourseRuns`: [L187](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L187)
- `CourseScheduleChangeRepository#respondFindOf`: [L191](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L191)
- `CourseScheduleChangeRepository#respondUpdateCourseScheduleChangeResponses`: [L195](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeRepository.java#L195)

### [CourseScheduleChangeService](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L31)

Package `com.scms.backend.course.schedulechange`. Lễ tân ghi nhận phản hồi đổi lịch

- `CourseScheduleChangeService#notifyReschedule`: [L65](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L65)
- `CourseScheduleChangeService#listForReceptionist`: [L70](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L70)
- `CourseScheduleChangeService#list`: [L77](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L77)
- `CourseScheduleChangeService#respond`: [L84](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L84), [L89](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L89)
- `CourseScheduleChangeService#createRunScheduleResponses`: [L136](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L136)
- `CourseScheduleChangeService#reload`: [L141](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L141)
- `CourseScheduleChangeService#prepareAtomicRunResponses`: [L145](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L145)
- `CourseScheduleChangeService#writeAtomicRunResponses`: [L149](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L149)
- `CourseScheduleChangeService#conflictingEnrollmentSessions`: [L156](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L156), [L160](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L160)
- `CourseScheduleChangeService#activeMember`: [L164](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeService.java#L164)

### [CourseTransferCompletion](../../src/main/java/com/scms/backend/course/transfer/CourseTransferCompletion.java#L17)

Package `com.scms.backend.course.transfer`. Component/repository tách từ CourseTransferService; facade giữ entrypoint và transaction.

- `CourseTransferCompletion#completeTransfer`: [L34](../../src/main/java/com/scms/backend/course/transfer/CourseTransferCompletion.java#L34)
- `CourseTransferCompletion#rows`: [L72](../../src/main/java/com/scms/backend/course/transfer/CourseTransferCompletion.java#L72)
- `CourseTransferCompletion#uuid`: [L76](../../src/main/java/com/scms/backend/course/transfer/CourseTransferCompletion.java#L76)
- `CourseTransferCompletion#number`: [L80](../../src/main/java/com/scms/backend/course/transfer/CourseTransferCompletion.java#L80)
- `CourseTransferCompletion#instant`: [L84](../../src/main/java/com/scms/backend/course/transfer/CourseTransferCompletion.java#L84)

### [CourseTransferExpiryScheduler](../../src/main/java/com/scms/backend/course/transfer/CourseTransferExpiryScheduler.java#L7)

Package `com.scms.backend.course.transfer`. Hết hạn đề nghị chuyển

- `CourseTransferExpiryScheduler#expire`: [L19](../../src/main/java/com/scms/backend/course/transfer/CourseTransferExpiryScheduler.java#L19)

### [CourseTransferPolicy](../../src/main/java/com/scms/backend/course/transfer/CourseTransferPolicy.java#L7)

Package `com.scms.backend.course.transfer`. Credit và thời hạn chuyển khóa

- `CourseTransferPolicy#offerExpiresAt`: [L12](../../src/main/java/com/scms/backend/course/transfer/CourseTransferPolicy.java#L12)
- `CourseTransferPolicy#unusedCredit`: [L21](../../src/main/java/com/scms/backend/course/transfer/CourseTransferPolicy.java#L21)

### [CourseTransferRepository](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L19)

Package `com.scms.backend.course.transfer`. Component/repository tách từ CourseTransferCompletion, CourseTransferService; facade giữ entrypoint và transaction.

- `CourseTransferRepository#unusedCredit`: [L27](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L27)
- `CourseTransferRepository#usedSeats`: [L35](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L35)
- `CourseTransferRepository#usedSeatsExcludingOffer`: [L39](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L39)
- `CourseTransferRepository#hasScheduleConflict`: [L49](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L49)
- `CourseTransferRepository#target`: [L61](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L61)
- `CourseTransferRepository#lockRuns`: [L67](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L67)
- `CourseTransferRepository#capacity`: [L71](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L71)
- `CourseTransferRepository#activeEnrollmentExists`: [L75](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L75)
- `CourseTransferRepository#lockRequest`: [L79](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L79)
- `CourseTransferRepository#findRequest`: [L83](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L83)
- `CourseTransferRepository#findRequestByResponse`: [L87](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L87)
- `CourseTransferRepository#lockOffer`: [L91](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L91)
- `CourseTransferRepository#findOffer`: [L95](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L95)
- `CourseTransferRepository#findOfferByRequest`: [L99](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L99)
- `CourseTransferRepository#targetEnrollment`: [L103](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L103)
- `CourseTransferRepository#requestSelect`: [L108](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L108)
- `CourseTransferRepository#offerSelect`: [L112](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L112)
- `CourseTransferRepository#mapRequest`: [L116](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L116)
- `CourseTransferRepository#mapOffer`: [L120](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L120)
- `CourseTransferRepository#rows`: [L124](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L124)
- `CourseTransferRepository#uuid`: [L128](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L128)
- `CourseTransferRepository#number`: [L132](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L132)
- `CourseTransferRepository#amount`: [L136](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L136)
- `CourseTransferRepository#instant`: [L140](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L140)
- `CourseTransferRepository#completeTransferFindCourseEnrollments`: [L145](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L145)
- `CourseTransferRepository#completeTransferFindCourseReservations`: [L149](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L149)
- `CourseTransferRepository#completeTransferFindCourseOrders`: [L153](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L153)
- `CourseTransferRepository#completeTransferFindCourseRuns`: [L157](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L157)
- `CourseTransferRepository#completeTransferInsertCourseEnrollments`: [L161](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L161)
- `CourseTransferRepository#completeTransferRowsCourseSessions`: [L170](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L170)
- `CourseTransferRepository#completeTransferInsertCourseEnrollmentSessions`: [L174](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L174)
- `CourseTransferRepository#completeTransferUpdateCourseEnrollments`: [L178](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L178)
- `CourseTransferRepository#completeTransferUpdateCourseReservations`: [L182](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L182)
- `CourseTransferRepository#completeTransferUpdateCourseTransferHolds`: [L186](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L186)
- `CourseTransferRepository#completeTransferUpdateCourseTransferOffers`: [L190](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L190)
- `CourseTransferRepository#completeTransferUpdateCourseTransferRequests`: [L194](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L194)
- `CourseTransferRepository#completeTransferInsertCourseRefundObligations`: [L198](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L198)
- `CourseTransferRepository#completeTransferFindAccounts`: [L207](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L207)
- `CourseTransferRepository#requestFromScheduleChangeRowsCourseScheduleChangeResponses`: [L211](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L211)
- `CourseTransferRepository#requestFromScheduleChangeInsertCourseTransferRequests`: [L221](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L221)
- `CourseTransferRepository#requestFromScheduleChangeFindAccounts`: [L228](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L228)
- `CourseTransferRepository#managerQueueFindRows`: [L232](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L232)
- `CourseTransferRepository#eligibleTargetsFindCourseSessions`: [L236](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L236)
- `CourseTransferRepository#approveRowsCourseSessions`: [L248](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L248)
- `CourseTransferRepository#approveFindCourseEnrollments`: [L257](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L257)
- `CourseTransferRepository#findTargetRunSessionTimes`: [L261](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L261)
- `CourseTransferRepository#approveInsertCourseTransferOffers`: [L265](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L265)
- `CourseTransferRepository#approveInsertCourseTransferHolds`: [L273](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L273)
- `CourseTransferRepository#approveUpdateCourseTransferRequests`: [L277](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L277)
- `CourseTransferRepository#rejectUpdateCourseTransferRequests`: [L281](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L281)
- `CourseTransferRepository#memberOffersFindRows`: [L285](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L285)
- `CourseTransferRepository#acceptFindCourseTransferOffers`: [L289](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L289)
- `CourseTransferRepository#acceptUpdateCourseTransferOffers`: [L293](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L293)
- `CourseTransferRepository#recordOfferAcceptance`: [L297](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L297)
- `CourseTransferRepository#completePaidDifferenceFindOf`: [L301](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L301)
- `CourseTransferRepository#expireDueFindOf`: [L305](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L305)
- `CourseTransferRepository#createDifferenceOrderInsertCourseOrders`: [L309](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L309)
- `CourseTransferRepository#createDifferenceOrderInsertCourseReservations`: [L316](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L316)
- `CourseTransferRepository#createDifferenceOrderUpdateCourseTransferHolds`: [L320](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L320)
- `CourseTransferRepository#expireOfferUpdateCourseTransferOffers`: [L324](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L324)
- `CourseTransferRepository#expireOfferUpdateCourseTransferHolds`: [L328](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L328)
- `CourseTransferRepository#expireOfferUpdateCourseTransferRequests`: [L332](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L332)
- `CourseTransferRepository#expireOfferUpdateCourseReservations`: [L336](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L336)
- `CourseTransferRepository#expireOfferUpdateCourseOrders`: [L340](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L340)
- `CourseTransferRepository#expireOfferUpdateCoursePayments`: [L344](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L344)
- `CourseTransferRepository#expireDueForRunUpdateCourseTransferHolds`: [L348](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L348)
- `CourseTransferRepository#expireDueForRunUpdateCourseTransferRequests`: [L352](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L352)
- `CourseTransferRepository#expireDueForRunUpdateCourseTransferOffers`: [L356](../../src/main/java/com/scms/backend/course/transfer/CourseTransferRepository.java#L356)

### [CourseTransferService](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L31)

Package `com.scms.backend.course.transfer`. Lễ tân xem/accept đề nghị chuyển; approve thuộc Manager

- `CourseTransferService#requestFromScheduleChange`: [L57](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L57)
- `CourseTransferService#managerQueue`: [L78](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L78)
- `CourseTransferService#eligibleTargets`: [L84](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L84)
- `CourseTransferService#approve`: [L94](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L94)
- `CourseTransferService#reject`: [L141](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L141)
- `CourseTransferService#offersForReceptionist`: [L157](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L157)
- `CourseTransferService#memberOffers`: [L163](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L163)
- `CourseTransferService#accept`: [L169](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L169), [L174](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L174)
- `CourseTransferService#recordAcceptance`: [L210](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L210)
- `CourseTransferService#completePaidDifference`: [L214](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L214)
- `CourseTransferService#expireDue`: [L227](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L227)
- `CourseTransferService#completeTransfer`: [L234](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L234)
- `CourseTransferService#createDifferenceOrder`: [L238](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L238)
- `CourseTransferService#expireOffer`: [L250](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L250)
- `CourseTransferService#expireDueForRun`: [L263](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L263)
- `CourseTransferService#unusedCredit`: [L269](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L269)
- `CourseTransferService#usedSeats`: [L273](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L273)
- `CourseTransferService#usedSeatsExcludingOffer`: [L277](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L277)
- `CourseTransferService#hasScheduleConflict`: [L281](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L281)
- `CourseTransferService#target`: [L285](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L285)
- `CourseTransferService#lockRuns`: [L289](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L289)
- `CourseTransferService#capacity`: [L293](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L293)
- `CourseTransferService#activeEnrollmentExists`: [L297](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L297)
- `CourseTransferService#active`: [L301](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L301)
- `CourseTransferService#reason`: [L306](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L306)
- `CourseTransferService#key`: [L312](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L312)
- `CourseTransferService#lockRequest`: [L318](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L318)
- `CourseTransferService#findRequest`: [L322](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L322)
- `CourseTransferService#findRequestByResponse`: [L326](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L326)
- `CourseTransferService#lockOffer`: [L330](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L330)
- `CourseTransferService#findOffer`: [L334](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L334)
- `CourseTransferService#findOfferByRequest`: [L338](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L338)
- `CourseTransferService#targetEnrollment`: [L342](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L342)
- `CourseTransferService#uuid`: [L346](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L346)
- `CourseTransferService#text`: [L350](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L350)
- `CourseTransferService#number`: [L354](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L354)
- `CourseTransferService#amount`: [L358](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L358)
- `CourseTransferService#instant`: [L362](../../src/main/java/com/scms/backend/course/transfer/CourseTransferService.java#L362)

### [CourseWaitlistRepository](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L13)

Package `com.scms.backend.course.waitlist`. Component/repository tách từ CourseWaitlistService; facade giữ entrypoint và transaction.

- `CourseWaitlistRepository#joinInsertCourseWaitlists`: [L15](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L15)
- `CourseWaitlistRepository#leaveUpdateCourseWaitlists`: [L19](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L19)
- `CourseWaitlistRepository#mineFindRows`: [L23](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L23)
- `CourseWaitlistRepository#acceptFindCourseWaitlists`: [L27](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L27)
- `CourseWaitlistRepository#lockInvitationForAcceptance`: [L31](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L31)
- `CourseWaitlistRepository#acceptUpdateCourseWaitlists`: [L35](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L35)
- `CourseWaitlistRepository#markInvitationAccepted`: [L39](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L39)
- `CourseWaitlistRepository#promoteFindCourseWaitlists`: [L43](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L43)
- `CourseWaitlistRepository#expireInvitationsBeforePromotion`: [L47](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L47)
- `CourseWaitlistRepository#expireInvitationsDuringPromotion`: [L51](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L51)
- `CourseWaitlistRepository#cancelWaitingAndInvitedForRun`: [L55](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L55)
- `CourseWaitlistRepository#cancelWaitingCandidate`: [L59](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L59)
- `CourseWaitlistRepository#lockNextWaitingCandidate`: [L63](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L63)
- `CourseWaitlistRepository#promoteFindCourseSessions`: [L67](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L67)
- `CourseWaitlistRepository#inviteWaitingCandidate`: [L71](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L71)
- `CourseWaitlistRepository#cancelForRunFindCourseWaitlists`: [L75](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L75)
- `CourseWaitlistRepository#cancelForRunUpdateCourseWaitlists`: [L79](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L79)
- `CourseWaitlistRepository#cancelForMemberFindCourseWaitlists`: [L83](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L83)
- `CourseWaitlistRepository#cancelForMemberUpdateCourseWaitlists`: [L87](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L87)
- `CourseWaitlistRepository#candidateRunIdsFindCourseWaitlists`: [L91](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L91)
- `CourseWaitlistRepository#usedSeatsFindCourseEnrollments`: [L100](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L100)
- `CourseWaitlistRepository#hasEnrollmentOrHoldFindCourseEnrollments`: [L109](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L109)
- `CourseWaitlistRepository#activeIdFindCourseWaitlists`: [L119](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L119)
- `CourseWaitlistRepository#findFindRows`: [L123](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L123)
- `CourseWaitlistRepository#map`: [L127](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L127)
- `CourseWaitlistRepository#selectWithPosition`: [L134](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistRepository.java#L134)

### [CourseWaitlistScheduler](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistScheduler.java#L11)

Package `com.scms.backend.course.waitlist`. Tác vụ nền mời người chờ

- `CourseWaitlistScheduler#promoteAvailable`: [L27](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistScheduler.java#L27)

### [CourseWaitlistService](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L28)

Package `com.scms.backend.course.waitlist`. Hàng chờ, lời mời và accept khóa

- `CourseWaitlistService#invitationExpiresAt`: [L62](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L62)
- `CourseWaitlistService#join`: [L71](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L71), [L76](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L76)
- `CourseWaitlistService#leave`: [L99](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L99), [L104](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L104)
- `CourseWaitlistService#mineForReceptionist`: [L117](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L117)
- `CourseWaitlistService#mine`: [L124](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L124)
- `CourseWaitlistService#accept`: [L131](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L131), [L136](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L136)
- `CourseWaitlistService#promote`: [L161](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L161)
- `CourseWaitlistService#cancelForRun`: [L197](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L197)
- `CourseWaitlistService#cancelForMember`: [L209](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L209)
- `CourseWaitlistService#candidateRunIds`: [L222](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L222)
- `CourseWaitlistService#usedSeats`: [L226](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L226)
- `CourseWaitlistService#hasEnrollmentOrHold`: [L230](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L230)
- `CourseWaitlistService#activeId`: [L234](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L234)
- `CourseWaitlistService#find`: [L238](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L238)
- `CourseWaitlistService#map`: [L242](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L242)
- `CourseWaitlistService#lockActiveMember`: [L249](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistService.java#L249)

### [CourseWithdrawalController](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L18)

Package `com.scms.backend.course.withdrawal`. API yêu cầu rút/hủy yêu cầu rút hộ

- `CourseWithdrawalController#memberCreate`: [L27](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L27)
- `CourseWithdrawalController#memberList`: [L32](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L32)
- `CourseWithdrawalController#memberDetail`: [L37](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L37)
- `CourseWithdrawalController#receptionCreate`: [L42](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L42)
- `CourseWithdrawalController#memberCancel`: [L47](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L47)
- `CourseWithdrawalController#receptionCancel`: [L52](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L52)
- `CourseWithdrawalController#managerQueue`: [L57](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L57)
- `CourseWithdrawalController#managerDetail`: [L62](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L62)
- `CourseWithdrawalController#approve`: [L67](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L67)
- `CourseWithdrawalController#reject`: [L72](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L72)
- `CourseWithdrawalController#managerCreate`: [L77](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L77)
- `CourseWithdrawalController#subject`: [L82](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L82)
- `CourseWithdrawalController#reason`: [L86](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L86)
- `CourseWithdrawalController#decisionReason`: [L90](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalController.java#L90)

### [CourseWithdrawalPolicy](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalPolicy.java#L6)

Package `com.scms.backend.course.withdrawal`. Điều kiện rút và công thức hoàn

- `CourseWithdrawalPolicy#canWithdraw`: [L11](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalPolicy.java#L11)
- `CourseWithdrawalPolicy#refund`: [L15](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalPolicy.java#L15), [L19](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalPolicy.java#L19)
- `CourseWithdrawalPolicy#evaluate`: [L29](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalPolicy.java#L29)

### [CourseWithdrawalRepository](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L17)

Package `com.scms.backend.course.withdrawal`. Component/repository tách từ CourseWithdrawalService, CourseWithdrawalSnapshot; facade giữ entrypoint và transaction.

- `CourseWithdrawalRepository#lockRun`: [L25](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L25)
- `CourseWithdrawalRepository#lockEnrollment`: [L29](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L29)
- `CourseWithdrawalRepository#lock`: [L33](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L33)
- `CourseWithdrawalRepository#find`: [L37](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L37)
- `CourseWithdrawalRepository#select`: [L41](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L41)
- `CourseWithdrawalRepository#map`: [L45](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L45)
- `CourseWithdrawalRepository#amount`: [L55](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L55), [L59](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L59)
- `CourseWithdrawalRepository#instant`: [L63](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L63), [L68](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L68)
- `CourseWithdrawalRepository#createForScheduleChangeFindCourseScheduleChangeResponses`: [L72](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L72)
- `CourseWithdrawalRepository#createForScheduleChangeFindCourseWithdrawalRequests`: [L83](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L83)
- `CourseWithdrawalRepository#findPendingWithdrawalForEnrollment`: [L87](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L87)
- `CourseWithdrawalRepository#createForScheduleChangeFindCourseTransferRequests`: [L91](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L91)
- `CourseWithdrawalRepository#listForMemberFindRows`: [L95](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L95)
- `CourseWithdrawalRepository#detailForMemberFindRows`: [L99](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L99)
- `CourseWithdrawalRepository#managerQueueFindRows`: [L103](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L103)
- `CourseWithdrawalRepository#approveUpdateCourseEnrollments`: [L107](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L107)
- `CourseWithdrawalRepository#approveUpdateCourseReservations`: [L111](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L111)
- `CourseWithdrawalRepository#approveUpdateCourseWithdrawalRequests`: [L115](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L115)
- `CourseWithdrawalRepository#approveInsertCourseRefundObligations`: [L122](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L122)
- `CourseWithdrawalRepository#rejectUpdateCourseEnrollments`: [L130](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L130)
- `CourseWithdrawalRepository#rejectUpdateCourseWithdrawalRequests`: [L134](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L134)
- `CourseWithdrawalRepository#cancelUpdateCourseWithdrawalRequests`: [L138](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L138)
- `CourseWithdrawalRepository#cancelUpdateCourseEnrollments`: [L142](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L142)
- `CourseWithdrawalRepository#createFindCourseEnrollments`: [L146](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L146)
- `CourseWithdrawalRepository#createFindCourseWithdrawalRequests`: [L150](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L150)
- `CourseWithdrawalRepository#createFindCourseTransferRequests`: [L154](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L154)
- `CourseWithdrawalRepository#insertRequestInsertCourseWithdrawalRequests`: [L158](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L158)
- `CourseWithdrawalRepository#insertRequestUpdateCourseEnrollments`: [L168](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L168)
- `CourseWithdrawalRepository#insertRequestFindAccounts`: [L172](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L172)
- `CourseWithdrawalRepository#refundSnapshotFindCourseEnrollmentSessions`: [L176](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalRepository.java#L176)

### [CourseWithdrawalService](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L34)

Package `com.scms.backend.course.withdrawal`. Tạo, xem và hủy request rút; approve thuộc Manager

- `CourseWithdrawalService#createForMember`: [L62](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L62)
- `CourseWithdrawalService#createForReceptionist`: [L67](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L67)
- `CourseWithdrawalService#createForManager`: [L72](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L72)
- `CourseWithdrawalService#createForScheduleChange`: [L77](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L77)
- `CourseWithdrawalService#listForReceptionist`: [L114](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L114)
- `CourseWithdrawalService#detailForReceptionist`: [L120](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L120)
- `CourseWithdrawalService#listForMember`: [L126](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L126)
- `CourseWithdrawalService#detailForMember`: [L132](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L132)
- `CourseWithdrawalService#managerQueue`: [L138](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L138)
- `CourseWithdrawalService#managerDetail`: [L144](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L144)
- `CourseWithdrawalService#approve`: [L150](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L150)
- `CourseWithdrawalService#reject`: [L175](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L175)
- `CourseWithdrawalService#cancelForMember`: [L193](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L193)
- `CourseWithdrawalService#cancelForReceptionist`: [L198](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L198)
- `CourseWithdrawalService#cancel`: [L203](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L203)
- `CourseWithdrawalService#create`: [L220](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L220)
- `CourseWithdrawalService#insertRequest`: [L242](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L242)
- `CourseWithdrawalService#refundSnapshot`: [L259](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L259)
- `CourseWithdrawalService#promoteAfterCommit`: [L263](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L263)
- `CourseWithdrawalService#active`: [L277](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L277)
- `CourseWithdrawalService#lockRun`: [L288](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L288)
- `CourseWithdrawalService#requireWithdrawableRun`: [L292](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L292)
- `CourseWithdrawalService#lockForDecision`: [L297](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L297)
- `CourseWithdrawalService#lockEnrollment`: [L306](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L306)
- `CourseWithdrawalService#reason`: [L310](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L310)
- `CourseWithdrawalService#invalidTransition`: [L316](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L316)
- `CourseWithdrawalService#lock`: [L320](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L320)
- `CourseWithdrawalService#find`: [L324](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L324)
- `CourseWithdrawalService#map`: [L328](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L328)
- `CourseWithdrawalService#uuid`: [L335](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L335)
- `CourseWithdrawalService#text`: [L339](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L339)
- `CourseWithdrawalService#number`: [L343](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L343)
- `CourseWithdrawalService#amount`: [L347](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L347), [L351](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L351)
- `CourseWithdrawalService#instant`: [L355](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L355), [L359](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalService.java#L359)

### [CourseWithdrawalSnapshot](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalSnapshot.java#L13)

Package `com.scms.backend.course.withdrawal`. Component/repository tách từ CourseWithdrawalService; facade giữ entrypoint và transaction.

- `CourseWithdrawalSnapshot#refundSnapshot`: [L22](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalSnapshot.java#L22)
- `CourseWithdrawalSnapshot#amount`: [L33](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalSnapshot.java#L33), [L37](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalSnapshot.java#L37)
- `CourseWithdrawalSnapshot#instant`: [L41](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalSnapshot.java#L41), [L45](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalSnapshot.java#L45)

### [CourseWithdrawalWaitlistPromotion](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalWaitlistPromotion.java#L10)

Package `com.scms.backend.course.withdrawal`. Lấp chỗ sau khi rút khóa

- `CourseWithdrawalWaitlistPromotion#promote`: [L19](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalWaitlistPromotion.java#L19)

### [EmailOutboxDeliveryService](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L19)

Package `com.scms.backend.emailverification.delivery`. Gửi và xử lý retry email

- `EmailOutboxDeliveryService#claim`: [L47](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L47)
- `EmailOutboxDeliveryService#deliver`: [L61](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L61)
- `EmailOutboxDeliveryService#metrics`: [L76](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L76)
- `EmailOutboxDeliveryService#alertLevels`: [L90](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L90)
- `EmailOutboxDeliveryService#sent`: [L102](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L102)
- `EmailOutboxDeliveryService#failed`: [L110](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L110)
- `EmailOutboxDeliveryService#retryDelay`: [L128](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L128)
- `EmailOutboxDeliveryService#terminal`: [L138](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L138)
- `EmailOutboxDeliveryService#expire`: [L147](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L147)
- `EmailOutboxDeliveryService#auditTerminal`: [L158](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L158)
- `EmailOutboxDeliveryService#map`: [L162](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDeliveryService.java#L162)

### [EmailOutboxDispatcher](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDispatcher.java#L12)

Package `com.scms.backend.emailverification.delivery`. Tác vụ nền gửi email OTP

- `EmailOutboxDispatcher#dispatch`: [L28](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDispatcher.java#L28)
- `EmailOutboxDispatcher#logHealth`: [L55](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxDispatcher.java#L55)

### [EmailOutboxEnqueuer](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxEnqueuer.java#L12)

Package `com.scms.backend.emailverification.delivery`. Đưa email OTP vào hàng đợi

- `EmailOutboxEnqueuer#enqueue`: [L40](../../src/main/java/com/scms/backend/emailverification/delivery/EmailOutboxEnqueuer.java#L40)

### [AccountEmailChangeController](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L23)

Package `com.scms.backend.emailverification.emailchange`. API đổi email chung

- `AccountEmailChangeController#status`: [L39](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L39)
- `AccountEmailChangeController#begin`: [L44](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L44)
- `AccountEmailChangeController#replace`: [L49](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L49)
- `AccountEmailChangeController#verify`: [L54](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L54)
- `AccountEmailChangeController#resend`: [L59](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L59)
- `AccountEmailChangeController#cancel`: [L64](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L64)
- `AccountEmailChangeController#id`: [L71](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeController.java#L71)

### [AccountEmailChangeRepository](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L15)

Package `com.scms.backend.emailverification.emailchange`. Component/repository tách từ AccountEmailChangeService; facade giữ entrypoint và transaction.

- `AccountEmailChangeRepository#createInsertStaffEmailChangeRequests`: [L17](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L17)
- `AccountEmailChangeRepository#verifyFindStaffEmailChangeRequests`: [L25](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L25)
- `AccountEmailChangeRepository#verifyUpdateStaffEmailChangeRequests`: [L32](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L32)
- `AccountEmailChangeRepository#markEmailChangeCompleted`: [L36](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L36)
- `AccountEmailChangeRepository#resendUpdateStaffEmailChangeRequests`: [L43](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L43)
- `AccountEmailChangeRepository#cancelUpdateStaffEmailChangeRequests`: [L50](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L50)
- `AccountEmailChangeRepository#statusFindStaffEmailChangeRequests`: [L54](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L54)
- `AccountEmailChangeRepository#expirePendingFindStaffEmailChangeRequests`: [L61](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L61)
- `AccountEmailChangeRepository#supersedeUpdateStaffEmailChangeRequests`: [L68](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L68)
- `AccountEmailChangeRepository#expireUpdateStaffEmailChangeRequests`: [L72](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L72)
- `AccountEmailChangeRepository#cancelOutboxUpdateEmailOutbox`: [L76](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L76)
- `AccountEmailChangeRepository#pendingForUpdateFindStaffEmailChangeRequests`: [L84](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L84)
- `AccountEmailChangeRepository#lockEmailClaimFindRows`: [L93](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L93)
- `AccountEmailChangeRepository#instant`: [L97](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L97)
- `AccountEmailChangeRepository#row`: [L101](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRepository.java#L101)

### [AccountEmailChangeService](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L34)

Package `com.scms.backend.emailverification.emailchange`. OTP đổi email và thu hồi phiên

- `AccountEmailChangeService#begin`: [L78](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L78)
- `AccountEmailChangeService#replace`: [L83](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L83)
- `AccountEmailChangeService#create`: [L88](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L88)
- `AccountEmailChangeService#verify`: [L120](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L120)
- `AccountEmailChangeService#resend`: [L164](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L164)
- `AccountEmailChangeService#cancel`: [L191](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L191)
- `AccountEmailChangeService#status`: [L203](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L203)
- `AccountEmailChangeService#expirePending`: [L211](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L211)
- `AccountEmailChangeService#supersede`: [L222](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L222)
- `AccountEmailChangeService#expire`: [L228](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L228)
- `AccountEmailChangeService#enqueue`: [L234](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L234)
- `AccountEmailChangeService#cancelOutbox`: [L238](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L238)
- `AccountEmailChangeService#pendingForUpdate`: [L242](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L242)
- `AccountEmailChangeService#activeForUpdate`: [L246](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L246)
- `AccountEmailChangeService#active`: [L250](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L250)
- `AccountEmailChangeService#lockEmailClaim`: [L254](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L254)
- `AccountEmailChangeService#digest`: [L258](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L258)
- `AccountEmailChangeService#bound`: [L262](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L262)
- `AccountEmailChangeService#response`: [L266](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L266)
- `AccountEmailChangeService#instant`: [L270](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L270)
- `AccountEmailChangeService#normalize`: [L274](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L274)
- `AccountEmailChangeService#ensureEnabled`: [L278](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L278)
- `AccountEmailChangeService#invalidOtp`: [L283](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L283)
- `AccountEmailChangeService#emailExists`: [L287](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L287)
- `AccountEmailChangeService#error`: [L291](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeService.java#L291)

### [StaffEmailChangeController](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L19)

Package `com.scms.backend.emailverification.emailchange`. API đổi email staff

- `StaffEmailChangeController#begin`: [L35](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L35)
- `StaffEmailChangeController#verify`: [L41](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L41)
- `StaffEmailChangeController#resend`: [L47](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L47)
- `StaffEmailChangeController#cancel`: [L52](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L52)
- `StaffEmailChangeController#response`: [L59](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L59)
- `StaffEmailChangeController#id`: [L63](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeController.java#L63)

### [ManualActivationRepository](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L21)

Package `com.scms.backend.emailverification.manualactivation`. Component/repository tách từ ManualActivationService; facade giữ entrypoint và transaction.

- `ManualActivationRepository#error`: [L29](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L29)
- `ManualActivationRepository#baseSelect`: [L33](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L33)
- `ManualActivationRepository#map`: [L42](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L42)
- `ManualActivationRepository#activationPage`: [L46](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L46)
- `ManualActivationRepository#activationDetail`: [L55](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L55)
- `ManualActivationRepository#searchFindAccounts`: [L64](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L64)
- `ManualActivationRepository#createFindAccounts`: [L73](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L73)
- `ManualActivationRepository#countEligibleInactiveMembers`: [L80](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L80)
- `ManualActivationRepository#createInsertEmailVerificationManualActivationRequests`: [L84](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L84)
- `ManualActivationRepository#approveUpdateEmailVerificationManualActivationRequests`: [L93](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L93)
- `ManualActivationRepository#rejectUpdateEmailVerificationManualActivationRequests`: [L97](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L97)
- `ManualActivationRepository#lockRequestFindEmailVerificationManualActivationRequests`: [L101](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L101)
- `ManualActivationRepository#terminateArtifactsUpdateEmailVerificationChallenges`: [L105](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L105)
- `ManualActivationRepository#terminateArtifactsUpdateEmailVerificationSessions`: [L109](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L109)
- `ManualActivationRepository#terminateArtifactsUpdateEmailOutbox`: [L113](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L113)
- `ManualActivationRepository#requesterSnapshotFindAccounts`: [L117](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationRepository.java#L117)

### [ManualActivationService](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L28)

Package `com.scms.backend.emailverification.manualactivation`. Đề nghị kích hoạt; approve/reject thuộc Manager

- `ManualActivationService#search`: [L53](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L53)
- `ManualActivationService#create`: [L66](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L66)
- `ManualActivationService#list`: [L90](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L90)
- `ManualActivationService#getForActor`: [L97](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L97)
- `ManualActivationService#approve`: [L103](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L103)
- `ManualActivationService#reject`: [L130](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L130)
- `ManualActivationService#lockRequest`: [L143](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L143)
- `ManualActivationService#terminateArtifacts`: [L150](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L150)
- `ManualActivationService#ensure`: [L156](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L156)
- `ManualActivationService#requesterSnapshot`: [L161](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L161)
- `ManualActivationService#validatePage`: [L165](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L165)
- `ManualActivationService#error`: [L170](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L170)
- `ManualActivationService#activationPage`: [L183](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L183)
- `ManualActivationService#activationDetail`: [L187](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationService.java#L187)

### [PasswordResetController](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L18)

Package `com.scms.backend.emailverification.passwordreset`. API tự quên mật khẩu qua email

- `PasswordResetController#request`: [L35](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L35)
- `PasswordResetController#verify`: [L41](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L41)
- `PasswordResetController#confirm`: [L47](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetController.java#L47)

### [PasswordResetRepository](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L13)

Package `com.scms.backend.emailverification.passwordreset`. Component/repository tách từ PasswordResetService; facade giữ entrypoint và transaction.

- `PasswordResetRepository#requestFindPasswordResetRequests`: [L15](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L15)
- `PasswordResetRepository#requestUpdatePasswordResetRequests`: [L22](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L22)
- `PasswordResetRepository#requestUpdatePasswordResetSessions`: [L26](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L26)
- `PasswordResetRepository#requestInsertPasswordResetRequests`: [L30](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L30)
- `PasswordResetRepository#regenerateResetChallenge`: [L37](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L37)
- `PasswordResetRepository#verifyFindPasswordResetRequests`: [L44](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L44)
- `PasswordResetRepository#verifyUpdatePasswordResetRequests`: [L53](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L53)
- `PasswordResetRepository#markResetRequestVerified`: [L57](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L57)
- `PasswordResetRepository#verifyUpdatePasswordResetSessions`: [L61](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L61)
- `PasswordResetRepository#verifyInsertPasswordResetSessions`: [L65](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L65)
- `PasswordResetRepository#confirmFindPasswordResetSessions`: [L69](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L69)
- `PasswordResetRepository#confirmUpdatePasswordResetSessions`: [L77](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L77)
- `PasswordResetRepository#confirmUpdatePasswordResetRequests`: [L81](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L81)
- `PasswordResetRepository#revokeOtherResetSessions`: [L85](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L85)
- `PasswordResetRepository#cancelOtpOutboxUpdateEmailOutbox`: [L89](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L89)
- `PasswordResetRepository#expireUpdatePasswordResetRequests`: [L96](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L96)
- `PasswordResetRepository#requestRow`: [L100](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L100)
- `PasswordResetRepository#sessionRow`: [L104](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L104)
- `PasswordResetRepository#instant`: [L108](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRepository.java#L108)

### [PasswordResetService](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L31)

Package `com.scms.backend.emailverification.passwordreset`. OTP và phiên khôi phục mật khẩu

- `PasswordResetService#request`: [L75](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L75)
- `PasswordResetService#verify`: [L123](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L123)
- `PasswordResetService#confirm`: [L159](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L159)
- `PasswordResetService#enqueueOtp`: [L183](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L183)
- `PasswordResetService#cancelOtpOutbox`: [L187](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L187)
- `PasswordResetService#expire`: [L191](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L191)
- `PasswordResetService#boundOtp`: [L197](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L197)
- `PasswordResetService#normalize`: [L201](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L201)
- `PasswordResetService#invalidOtp`: [L205](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L205)
- `PasswordResetService#invalidSession`: [L209](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L209)
- `PasswordResetService#invalidSessionException`: [L213](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L213)
- `PasswordResetService#instant`: [L217](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetService.java#L217)

### [ReceptionEmailVerificationController](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L25)

Package `com.scms.backend.emailverification.verification`. Tra cứu account chờ và tạo manual activation

- `ReceptionEmailVerificationController#search`: [L35](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L35)
- `ReceptionEmailVerificationController#create`: [L40](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L40)
- `ReceptionEmailVerificationController#list`: [L46](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L46)
- `ReceptionEmailVerificationController#get`: [L51](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L51)
- `ReceptionEmailVerificationController#id`: [L56](../../src/main/java/com/scms/backend/emailverification/verification/ReceptionEmailVerificationController.java#L56)

### [MemberProfileDetailsValidator](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L10)

Package `com.scms.backend.identity.memberprofile`. Kiểm tra dữ liệu hồ sơ dùng chung

- `MemberProfileDetailsValidator#normalizeRequired`: [L23](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L23)
- `MemberProfileDetailsValidator#normalizePersonName`: [L37](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L37)
- `MemberProfileDetailsValidator#normalizePhone`: [L45](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L45)
- `MemberProfileDetailsValidator#normalizeEmail`: [L53](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L53)
- `MemberProfileDetailsValidator#validateBirthDate`: [L61](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L61)
- `MemberProfileDetailsValidator#normalizeOptional`: [L71](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L71)
- `MemberProfileDetailsValidator#normalizeGender`: [L85](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L85)
- `MemberProfileDetailsValidator#normalizeOptionalPersonName`: [L93](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L93)
- `MemberProfileDetailsValidator#normalizeOptionalPhone`: [L99](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L99)
- `MemberProfileDetailsValidator#validateEmergencyContact`: [L106](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L106)
- `MemberProfileDetailsValidator#invalid`: [L112](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileDetailsValidator.java#L112)

### [ReceptionMemberController](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L22)

Package `com.scms.backend.identity.receptionmember`. API tạo, tìm, sửa hồ sơ và reset hội viên

- `ReceptionMemberController#resetPassword`: [L38](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L38)
- `ReceptionMemberController#createMember`: [L45](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L45)
- `ReceptionMemberController#searchMembers`: [L51](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L51)
- `ReceptionMemberController#getMemberProfile`: [L56](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L56)
- `ReceptionMemberController#updateMemberProfile`: [L61](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L61)
- `ReceptionMemberController#accountId`: [L69](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberController.java#L69)

### [ReceptionMemberProfileRepository](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileRepository.java#L14)

Package `com.scms.backend.identity.receptionmember`. Component/repository tách từ ReceptionMemberProfileService; facade giữ entrypoint và transaction.

- `ReceptionMemberProfileRepository#findActiveMembership`: [L16](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileRepository.java#L16)

### [ReceptionMemberProfileService](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L26)

Package `com.scms.backend.identity.receptionmember`. Tra cứu/sửa hồ sơ và tóm tắt gói

- `ReceptionMemberProfileService#search`: [L55](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L55)
- `ReceptionMemberProfileService#getProfile`: [L72](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L72)
- `ReceptionMemberProfileService#updateProfile`: [L78](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L78)
- `ReceptionMemberProfileService#replaceProfileImage`: [L109](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L109)
- `ReceptionMemberProfileService#normalizeMemberId`: [L119](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L119)
- `ReceptionMemberProfileService#ensureActiveReceptionist`: [L126](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L126)
- `ReceptionMemberProfileService#findByMemberId`: [L132](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L132)
- `ReceptionMemberProfileService#toSearchResponse`: [L136](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L136)
- `ReceptionMemberProfileService#activeMembership`: [L141](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L141)
- `ReceptionMemberProfileService#toProfileResponse`: [L147](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberProfileService.java#L147)

### [ReceptionMemberService](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberService.java#L24)

Package `com.scms.backend.identity.receptionmember`. Tạo hội viên tại quầy

- `ReceptionMemberService#createMember`: [L55](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberService.java#L55)
- `ReceptionMemberService#validateBirthDate`: [L77](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberService.java#L77)

### [StaffProfileController](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L13)

Package `com.scms.backend.identity.staffprofile`. Hồ sơ cá nhân lễ tân dùng chung với staff

- `StaffProfileController#get`: [L23](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L23)
- `StaffProfileController#update`: [L28](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L28)
- `StaffProfileController#id`: [L35](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileController.java#L35)

### [StaffProfileService](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L23)

Package `com.scms.backend.identity.staffprofile`. Đọc/sửa hồ sơ và ảnh staff

- `StaffProfileService#getProfile`: [L44](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L44)
- `StaffProfileService#replaceProfileImage`: [L49](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L49)
- `StaffProfileService#updateProfile`: [L57](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L57)
- `StaffProfileService#blankToNull`: [L90](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L90)
- `StaffProfileService#snapshot`: [L94](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L94)
- `StaffProfileService#loadStaff`: [L103](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L103)
- `StaffProfileService#toResponse`: [L111](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileService.java#L111)

### [ResetPasswordService](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L18)

Package `com.scms.backend.manager`. Lễ tân reset hội viên; manager reset staff qua method khác

- `ResetPasswordService#managerReset`: [L39](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L39)
- `ResetPasswordService#receptionistResetMember`: [L49](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L49)
- `ResetPasswordService#reset`: [L57](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L57)
- `ResetPasswordService#target`: [L64](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L64)
- `ResetPasswordService#ensureActive`: [L68](../../src/main/java/com/scms/backend/manager/ResetPasswordService.java#L68)

### [StaffAccountController](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L23)

Package `com.scms.backend.manager`. MANAGER dùng để quản lý tài khoản lễ tân

- `StaffAccountController#create`: [L42](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L42)
- `StaffAccountController#search`: [L48](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L48)
- `StaffAccountController#get`: [L57](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L57)
- `StaffAccountController#update`: [L62](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L62)
- `StaffAccountController#changeStatus`: [L67](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L67)
- `StaffAccountController#resetPassword`: [L72](../../src/main/java/com/scms/backend/manager/StaffAccountController.java#L72)

### [StaffAccountCreateService](../../src/main/java/com/scms/backend/manager/StaffAccountCreateService.java#L22)

Package `com.scms.backend.manager`. MANAGER tạo account RECEPTIONIST

- `StaffAccountCreateService#create`: [L46](../../src/main/java/com/scms/backend/manager/StaffAccountCreateService.java#L46)
- `StaffAccountCreateService#ensureActiveManager`: [L64](../../src/main/java/com/scms/backend/manager/StaffAccountCreateService.java#L64)
- `StaffAccountCreateService#validate`: [L70](../../src/main/java/com/scms/backend/manager/StaffAccountCreateService.java#L70)

### [StaffAccountService](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L20)

Package `com.scms.backend.manager`. MANAGER tìm/xem/sửa thông tin lễ tân

- `StaffAccountService#get`: [L35](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L35)
- `StaffAccountService#update`: [L41](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L41)
- `StaffAccountService#search`: [L62](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L62)
- `StaffAccountService#normalizeQuery`: [L86](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L86)
- `StaffAccountService#ensureActiveManager`: [L100](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L100)
- `StaffAccountService#findStaff`: [L106](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L106)
- `StaffAccountService#normalizeRequired`: [L110](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L110)
- `StaffAccountService#normalizePersonName`: [L117](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L117)
- `StaffAccountService#normalizePhone`: [L125](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L125)
- `StaffAccountService#normalizeEmail`: [L133](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L133)
- `StaffAccountService#validateBirthDate`: [L141](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L141)
- `StaffAccountService#toResponse`: [L148](../../src/main/java/com/scms/backend/manager/StaffAccountService.java#L148)

### [StaffStatusRepository](../../src/main/java/com/scms/backend/manager/StaffStatusRepository.java#L7)

Package `com.scms.backend.manager`. Component/repository tách từ StaffStatusService; facade giữ entrypoint và transaction.

- `StaffStatusRepository#endAssignmentsWithoutActivePlus`: [L9](../../src/main/java/com/scms/backend/manager/StaffStatusRepository.java#L9)
- `StaffStatusRepository#hasFutureSessions`: [L21](../../src/main/java/com/scms/backend/manager/StaffStatusRepository.java#L21)
- `StaffStatusRepository#hasActiveAssignments`: [L28](../../src/main/java/com/scms/backend/manager/StaffStatusRepository.java#L28)

### [StaffStatusService](../../src/main/java/com/scms/backend/manager/StaffStatusService.java#L18)

Package `com.scms.backend.manager`. MANAGER ngừng hoạt động tài khoản lễ tân

- `StaffStatusService#deactivate`: [L37](../../src/main/java/com/scms/backend/manager/StaffStatusService.java#L37)
- `StaffStatusService#ensureCoachHasNoOutstandingWork`: [L64](../../src/main/java/com/scms/backend/manager/StaffStatusService.java#L64)
- `StaffStatusService#validate`: [L73](../../src/main/java/com/scms/backend/manager/StaffStatusService.java#L73)

### [LocalProfileImageStorage](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L21)

Package `com.scms.backend.member`. Triển khai lưu/đọc/xóa ảnh

- `LocalProfileImageStorage#store`: [L40](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L40)
- `LocalProfileImageStorage#delete`: [L78](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L78)
- `LocalProfileImageStorage#addResourceHandlers`: [L98](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L98)
- `LocalProfileImageStorage#validateRequest`: [L103](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L103)
- `LocalProfileImageStorage#validateImage`: [L112](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L112)
- `LocalProfileImageStorage#invalid`: [L143](../../src/main/java/com/scms/backend/member/LocalProfileImageStorage.java#L143)

### [ProfileImageUploadController](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L17)

Package `com.scms.backend.member`. Upload ảnh hội viên và ảnh cá nhân staff

- `ProfileImageUploadController#uploadMemberProfileImage`: [L26](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L26)
- `ProfileImageUploadController#uploadReceptionMemberProfileImage`: [L31](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L31)
- `ProfileImageUploadController#uploadStaffProfileImage`: [L36](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L36)
- `ProfileImageUploadController#removeStaffProfileImage`: [L41](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L41)
- `ProfileImageUploadController#accountId`: [L46](../../src/main/java/com/scms/backend/member/ProfileImageUploadController.java#L46)

### [ProfileImageUploadService](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L17)

Package `com.scms.backend.member`. Lưu ảnh và dọn ảnh sau transaction

- `ProfileImageUploadService#uploadForMember`: [L37](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L37)
- `ProfileImageUploadService#uploadForReceptionist`: [L51](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L51)
- `ProfileImageUploadService#uploadForStaff`: [L65](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L65)
- `ProfileImageUploadService#removeForStaff`: [L79](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L79)
- `ProfileImageUploadService#cleanupAfterCommit`: [L87](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L87)
- `ProfileImageUploadService#deleteQuietly`: [L105](../../src/main/java/com/scms/backend/member/ProfileImageUploadService.java#L105)

### [MembershipLifecycleScheduler](../../src/main/java/com/scms/backend/membership/MembershipLifecycleScheduler.java#L13)

Package `com.scms.backend.membership`. Tác vụ nền vòng đời, freeze và termination

- `MembershipLifecycleScheduler#reconcile`: [L45](../../src/main/java/com/scms/backend/membership/MembershipLifecycleScheduler.java#L45)

### [MembershipLifecycleService](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L12)

Package `com.scms.backend.membership`. Hết hạn, kích hoạt gói RESERVED và chấm dứt

- `MembershipLifecycleService#reconcile`: [L30](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L30)
- `MembershipLifecycleService#activateReservedFromLatestExpired`: [L54](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L54)
- `MembershipLifecycleService#reconcileExpiredAssignments`: [L63](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L63)
- `MembershipLifecycleService#terminateActive`: [L68](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L68)
- `MembershipLifecycleService#terminateActiveAndActivateReserved`: [L74](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L74)
- `MembershipLifecycleService#activateReservedAfterRefundRejected`: [L84](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L84)
- `MembershipLifecycleService#extendActive`: [L95](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L95)
- `MembershipLifecycleService#activeForUpdate`: [L101](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L101)
- `MembershipLifecycleService#lockMember`: [L105](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L105)
- `MembershipLifecycleService#canActivateReserved`: [L109](../../src/main/java/com/scms/backend/membership/MembershipLifecycleService.java#L109)

### [MembershipFreezeController](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L18)

Package `com.scms.backend.membership.freeze`. API bảo lưu, thu phí, kết thúc sớm

- `MembershipFreezeController#memberList`: [L27](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L27)
- `MembershipFreezeController#memberCreate`: [L32](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L32)
- `MembershipFreezeController#memberWithdraw`: [L37](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L37)
- `MembershipFreezeController#memberEndEarly`: [L43](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L43)
- `MembershipFreezeController#receptionList`: [L48](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L48)
- `MembershipFreezeController#receptionCreate`: [L53](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L53)
- `MembershipFreezeController#managerList`: [L58](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L58)
- `MembershipFreezeController#approve`: [L63](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L63)
- `MembershipFreezeController#reject`: [L68](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L68)
- `MembershipFreezeController#cash`: [L73](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L73)
- `MembershipFreezeController#receptionEndEarly`: [L78](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L78)
- `MembershipFreezeController#id`: [L83](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L83)
- `MembershipFreezeController#size`: [L87](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeController.java#L87)

### [MembershipFreezeExecutionRepository](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L8)

Package `com.scms.backend.membership.freeze`. Component/repository tách từ MembershipFreezeExecutionService; facade giữ entrypoint và transaction.

- `MembershipFreezeExecutionRepository#markScheduledObsolete`: [L10](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L10)
- `MembershipFreezeExecutionRepository#completeScheduledPeriod`: [L14](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L14)
- `MembershipFreezeExecutionRepository#activateScheduledPeriod`: [L18](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L18)
- `MembershipFreezeExecutionRepository#completeFindMembershipFreezePeriods`: [L22](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L22)
- `MembershipFreezeExecutionRepository#completeUpdateMembershipFreezePeriods`: [L26](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L26)
- `MembershipFreezeExecutionRepository#endEarlyForTerminationFindMembershipFreezePeriods`: [L30](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L30)
- `MembershipFreezeExecutionRepository#endEarlyForTerminationUpdateMembershipFreezePeriods`: [L34](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionRepository.java#L34)

### [MembershipFreezeExecutionService](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L33)

Package `com.scms.backend.membership.freeze`. Kích hoạt/kết thúc bảo lưu theo ngày

- `MembershipFreezeExecutionService#reconcileDue`: [L77](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L77)
- `MembershipFreezeExecutionService#obsoleteUnpaid`: [L103](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L103)
- `MembershipFreezeExecutionService#activate`: [L111](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L111)
- `MembershipFreezeExecutionService#promoteAfterCommit`: [L153](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L153)
- `MembershipFreezeExecutionService#complete`: [L166](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L166)
- `MembershipFreezeExecutionService#endActiveForTermination`: [L191](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L191)
- `MembershipFreezeExecutionService#endEarlyForTermination`: [L198](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeExecutionService.java#L198)

### [MembershipFreezeRepository](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L11)

Package `com.scms.backend.membership.freeze`. Component/repository tách từ MembershipFreezeService; facade giữ entrypoint và transaction.

- `MembershipFreezeRepository#collectCashInsertMembershipFreezeFeePayments`: [L13](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L13)
- `MembershipFreezeRepository#collectCashInsertMembershipFreezePeriods`: [L17](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L17)
- `MembershipFreezeRepository#paidResponseFindMembershipFreezeFeePayments`: [L21](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L21)
- `MembershipFreezeRepository#endEarlyFindMembershipFreezePeriods`: [L25](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L25)
- `MembershipFreezeRepository#endEarlyUpdateMembershipFreezePeriods`: [L29](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRepository.java#L29)

### [MembershipFreezeService](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L39)

Package `com.scms.backend.membership.freeze`. Bảo lưu; approve/reject thuộc Manager

- `MembershipFreezeService#listForMember`: [L76](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L76)
- `MembershipFreezeService#createForMember`: [L82](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L82)
- `MembershipFreezeService#withdrawForMember`: [L88](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L88)
- `MembershipFreezeService#listForReceptionist`: [L100](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L100)
- `MembershipFreezeService#createForReceptionist`: [L106](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L106)
- `MembershipFreezeService#listForManager`: [L112](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L112)
- `MembershipFreezeService#approve`: [L118](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L118)
- `MembershipFreezeService#reject`: [L141](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L141)
- `MembershipFreezeService#collectCash`: [L155](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L155)
- `MembershipFreezeService#endEarlyForMember`: [L180](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L180)
- `MembershipFreezeService#endEarlyForReceptionist`: [L186](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L186)
- `MembershipFreezeService#fee`: [L192](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L192)
- `MembershipFreezeService#create`: [L197](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L197)
- `MembershipFreezeService#paidResponse`: [L212](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L212)
- `MembershipFreezeService#endEarly`: [L217](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L217)
- `MembershipFreezeService#validate`: [L249](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L249)
- `MembershipFreezeService#lock`: [L256](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L256)
- `MembershipFreezeService#lockForDecision`: [L260](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L260), [L264](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L264)
- `MembershipFreezeService#checkVersion`: [L273](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L273)
- `MembershipFreezeService#businessDate`: [L278](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L278)
- `MembershipFreezeService#findMember`: [L282](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L282)
- `MembershipFreezeService#ensure`: [L288](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L288)
- `MembershipFreezeService#audit`: [L293](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L293)
- `MembershipFreezeService#notify`: [L300](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L300)
- `MembershipFreezeService#notifyRole`: [L304](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L304)
- `MembershipFreezeService#pageable`: [L311](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L311)
- `MembershipFreezeService#page`: [L315](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L315)
- `MembershipFreezeService#managerPage`: [L319](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeService.java#L319)

### [MembershipOfferController](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferController.java#L13)

Package `com.scms.backend.membership.offer`. Danh mục gói dùng để tư vấn

- `MembershipOfferController#listActiveOffers`: [L23](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferController.java#L23)
- `MembershipOfferController#getActiveOffer`: [L29](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferController.java#L29)

### [MembershipOfferService](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferService.java#L13)

Package `com.scms.backend.membership.offer`. Đọc offer đang bán

- `MembershipOfferService#listActiveOffers`: [L24](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferService.java#L24)
- `MembershipOfferService#getActiveOffer`: [L35](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferService.java#L35)
- `MembershipOfferService#parsePlanCode`: [L40](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferService.java#L40)

### [MembershipOrderController](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L17)

Package `com.scms.backend.membership.order`. Tạo/tiếp tục đơn mua gói hộ hội viên

- `MembershipOrderController#createForMember`: [L26](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L26)
- `MembershipOrderController#resumeForMember`: [L32](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L32)
- `MembershipOrderController#createForReceptionist`: [L37](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L37)
- `MembershipOrderController#resumeForReceptionist`: [L43](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L43)
- `MembershipOrderController#accountId`: [L48](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L48)
- `MembershipOrderController#optionalResponse`: [L52](../../src/main/java/com/scms/backend/membership/order/MembershipOrderController.java#L52)

### [MembershipOrderService](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L33)

Package `com.scms.backend.membership.order`. Kiểm tra gói, tạo đơn và gia hạn

- `MembershipOrderService#createForMember`: [L59](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L59)
- `MembershipOrderService#createForReceptionist`: [L67](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L67)
- `MembershipOrderService#resumeForMember`: [L76](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L76)
- `MembershipOrderService#resumeForReceptionist`: [L82](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L82)
- `MembershipOrderService#create`: [L88](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L88)
- `MembershipOrderService#prepareMember`: [L115](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L115)
- `MembershipOrderService#findPending`: [L125](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L125)
- `MembershipOrderService#findActiveOffer`: [L130](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L130)
- `MembershipOrderService#findSelfMember`: [L135](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L135)
- `MembershipOrderService#findTargetMember`: [L144](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L144)
- `MembershipOrderService#ensureActiveActor`: [L151](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L151)
- `MembershipOrderService#ensureActiveMember`: [L157](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L157)
- `MembershipOrderService#toResponse`: [L163](../../src/main/java/com/scms/backend/membership/order/MembershipOrderService.java#L163)

### [CoursePaymentFulfillment](../../src/main/java/com/scms/backend/membership/payment/CoursePaymentFulfillment.java#L18)

Package `com.scms.backend.membership.payment`. Component/repository tách từ PaymentFulfillmentService; facade giữ entrypoint và transaction.

- `CoursePaymentFulfillment#fulfillCourse`: [L33](../../src/main/java/com/scms/backend/membership/payment/CoursePaymentFulfillment.java#L33)
- `CoursePaymentFulfillment#uuid`: [L63](../../src/main/java/com/scms/backend/membership/payment/CoursePaymentFulfillment.java#L63)
- `CoursePaymentFulfillment#string`: [L67](../../src/main/java/com/scms/backend/membership/payment/CoursePaymentFulfillment.java#L67)
- `CoursePaymentFulfillment#amount`: [L71](../../src/main/java/com/scms/backend/membership/payment/CoursePaymentFulfillment.java#L71)
- `CoursePaymentFulfillment#instant`: [L75](../../src/main/java/com/scms/backend/membership/payment/CoursePaymentFulfillment.java#L75)

### [CourseReconciliationController](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L20)

Package `com.scms.backend.membership.payment`. API đối soát thanh toán khóa

- `CourseReconciliationController#queue`: [L30](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L30)
- `CourseReconciliationController#detail`: [L36](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L36)
- `CourseReconciliationController#result`: [L42](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L42)
- `CourseReconciliationController#reconcile`: [L48](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationController.java#L48)

### [CourseReconciliationRepository](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationRepository.java#L15)

Package `com.scms.backend.membership.payment`. Component/repository tách từ CourseReconciliationService; facade giữ entrypoint và transaction.

- `CourseReconciliationRepository#queueRows`: [L26](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationRepository.java#L26)
- `CourseReconciliationRepository#reconcileFindCoursePaymentReconciliations`: [L45](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationRepository.java#L45)
- `CourseReconciliationRepository#reconcileInsertCoursePaymentReconciliations`: [L49](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationRepository.java#L49)

### [CourseReconciliationService](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L31)

Package `com.scms.backend.membership.payment`. Đối soát khóa, replay và kiểm tra hold

- `CourseReconciliationService#queue`: [L59](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L59)
- `CourseReconciliationService#detail`: [L67](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L67)
- `CourseReconciliationService#result`: [L76](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L76)
- `CourseReconciliationService#reconcile`: [L87](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L87)
- `CourseReconciliationService#view`: [L133](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L133)
- `CourseReconciliationService#paidBlockedReason`: [L137](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L137)
- `CourseReconciliationService#canFail`: [L158](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L158)
- `CourseReconciliationService#validate`: [L162](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L162)
- `CourseReconciliationService#blank`: [L171](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L171)
- `CourseReconciliationService#active`: [L175](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L175)
- `CourseReconciliationService#staff`: [L179](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L179)
- `CourseReconciliationService#queueRows`: [L185](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationService.java#L185)

### [MembershipPaymentFulfillment](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L16)

Package `com.scms.backend.membership.payment`. Component/repository tách từ PaymentFulfillmentService; facade giữ entrypoint và transaction.

- `MembershipPaymentFulfillment#fulfillLocked`: [L34](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L34)
- `MembershipPaymentFulfillment#createArtifacts`: [L44](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L44)
- `MembershipPaymentFulfillment#uuid`: [L59](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L59)
- `MembershipPaymentFulfillment#string`: [L63](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L63)
- `MembershipPaymentFulfillment#number`: [L68](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L68)
- `MembershipPaymentFulfillment#fulfillmentMode`: [L72](../../src/main/java/com/scms/backend/membership/payment/MembershipPaymentFulfillment.java#L72)

### [PaymentController](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L30)

Package `com.scms.backend.membership.payment`. Thu tiền, đối soát, biên lai, lịch sử gói

- `PaymentController#cash`: [L39](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L39)
- `PaymentController#deprecatedCash`: [L45](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L45)
- `PaymentController#reconcile`: [L51](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L51)
- `PaymentController#cancelOrder`: [L56](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L56)
- `PaymentController#reconciliationQueue`: [L61](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L61)
- `PaymentController#receipts`: [L69](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L69)
- `PaymentController#receiptsForReceptionist`: [L74](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L74)
- `PaymentController#receipt`: [L79](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L79)
- `PaymentController#result`: [L84](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L84)
- `PaymentController#history`: [L89](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L89)
- `PaymentController#receptionMembershipHistory`: [L94](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L94)
- `PaymentController#managerMembershipHistory`: [L100](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L100)
- `PaymentController#report`: [L106](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L106)
- `PaymentController#id`: [L112](../../src/main/java/com/scms/backend/membership/payment/PaymentController.java#L112)

### [PaymentEvidenceController](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L18)

Package `com.scms.backend.membership.payment`. API ảnh bằng chứng giao dịch

- `PaymentEvidenceController#list`: [L28](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L28)
- `PaymentEvidenceController#upload`: [L33](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L33)
- `PaymentEvidenceController#read`: [L38](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceController.java#L38)

### [PaymentEvidenceRepository](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L9)

Package `com.scms.backend.membership.payment`. Component/repository tách từ PaymentEvidenceService; facade giữ entrypoint và transaction.

- `PaymentEvidenceRepository#listFindPaymentEvidenceImages`: [L11](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L11)
- `PaymentEvidenceRepository#uploadFindPayments`: [L15](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L15)
- `PaymentEvidenceRepository#findDuplicateEvidence`: [L19](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L19)
- `PaymentEvidenceRepository#countEvidence`: [L23](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L23)
- `PaymentEvidenceRepository#insertEvidence`: [L27](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L27)
- `PaymentEvidenceRepository#readFindPaymentEvidenceImages`: [L34](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceRepository.java#L34)

### [PaymentEvidenceService](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L22)

Package `com.scms.backend.membership.payment`. Kiểm tra/lưu/đọc ảnh chứng từ

- `PaymentEvidenceService#authorize`: [L43](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L43)
- `PaymentEvidenceService#list`: [L49](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L49)
- `PaymentEvidenceService#upload`: [L55](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L55)
- `PaymentEvidenceService#read`: [L80](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L80)
- `PaymentEvidenceService#validate`: [L89](../../src/main/java/com/scms/backend/membership/payment/PaymentEvidenceService.java#L89)

### [PaymentFulfillmentRepository](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L13)

Package `com.scms.backend.membership.payment`. Component/repository tách từ CoursePaymentFulfillment, MembershipPaymentFulfillment, PaymentFulfillmentService; facade giữ entrypoint và transaction.

- `PaymentFulfillmentRepository#courseOrderForUpdate`: [L21](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L21)
- `PaymentFulfillmentRepository#memberIdForPayment`: [L35](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L35)
- `PaymentFulfillmentRepository#memberIdForOrder`: [L43](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L43)
- `PaymentFulfillmentRepository#lockCourseTransferForPayment`: [L51](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L51)
- `PaymentFulfillmentRepository#coursePaymentForUpdate`: [L59](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L59)
- `PaymentFulfillmentRepository#coursePayment`: [L78](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L78)
- `PaymentFulfillmentRepository#coursePaymentSql`: [L86](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L86)
- `PaymentFulfillmentRepository#paymentForUpdate`: [L107](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L107)
- `PaymentFulfillmentRepository#payment`: [L118](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L118)
- `PaymentFulfillmentRepository#uuid`: [L140](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L140)
- `PaymentFulfillmentRepository#fulfillCourseInsertCoursePayments`: [L144](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L144)
- `PaymentFulfillmentRepository#fulfillCourseUpdateCoursePayments`: [L153](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L153)
- `PaymentFulfillmentRepository#fulfillCourseUpdateCourseOrders`: [L160](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L160)
- `PaymentFulfillmentRepository#fulfillCourseUpdateCourseReservations`: [L164](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L164)
- `PaymentFulfillmentRepository#fulfillCourseInsertCourseEnrollments`: [L168](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L168)
- `PaymentFulfillmentRepository#fulfillCourseInsertCourseEnrollmentSessions`: [L177](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L177)
- `PaymentFulfillmentRepository#fulfillLockedUpdatePayments`: [L184](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L184)
- `PaymentFulfillmentRepository#fulfillLockedUpdateMembershipOrders`: [L192](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L192)
- `PaymentFulfillmentRepository#createArtifactsInsertMemberships`: [L196](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L196)
- `PaymentFulfillmentRepository#createArtifactsInsertReceipts`: [L205](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L205)
- `PaymentFulfillmentRepository#courseUnitsFindCourseSessions`: [L213](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L213)
- `PaymentFulfillmentRepository#courseEnrollmentFindCourseEnrollments`: [L226](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L226)
- `PaymentFulfillmentRepository#fulfillCashFindMembershipOrders`: [L233](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L233)
- `PaymentFulfillmentRepository#fulfillCashInsertPayments`: [L246](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L246)
- `PaymentFulfillmentRepository#fulfillCashUpdateMembershipOrders`: [L253](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L253)
- `PaymentFulfillmentRepository#ensureMembershipCanFulfillFindMemberships`: [L257](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L257)
- `PaymentFulfillmentRepository#resultFindMemberships`: [L261](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentRepository.java#L261)

### [PaymentFulfillmentService](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L30)

Package `com.scms.backend.membership.payment`. Cấp Membership/Enrollment và chứng từ sau thanh toán

- `PaymentFulfillmentService#notifyFailedReconciliation`: [L33](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L33)
- `PaymentFulfillmentService#fulfill`: [L64](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L64)
- `PaymentFulfillmentService#processSepayWebhook`: [L94](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L94)
- `PaymentFulfillmentService#processCourseSepayWebhook`: [L150](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L150)
- `PaymentFulfillmentService#fulfillCourseCash`: [L202](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L202)
- `PaymentFulfillmentService#fulfillCourseReconciliation`: [L214](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L214)
- `PaymentFulfillmentService#fulfillCourse`: [L218](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L218)
- `PaymentFulfillmentService#courseUnits`: [L222](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L222)
- `PaymentFulfillmentService#courseRunPayable`: [L235](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L235)
- `PaymentFulfillmentService#courseEnrollment`: [L239](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L239)
- `PaymentFulfillmentService#courseOrderForUpdate`: [L248](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L248)
- `PaymentFulfillmentService#fulfillCash`: [L252](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L252)
- `PaymentFulfillmentService#memberIdForPayment`: [L274](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L274)
- `PaymentFulfillmentService#memberIdForOrder`: [L278](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L278)
- `PaymentFulfillmentService#ensureMembershipCanFulfill`: [L282](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L282)
- `PaymentFulfillmentService#fulfillLocked`: [L289](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L289)
- `PaymentFulfillmentService#finishEvent`: [L293](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L293)
- `PaymentFulfillmentService#insertFinalEvent`: [L301](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L301), [L305](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L305)
- `PaymentFulfillmentService#finishCourseEvent`: [L309](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L309)
- `PaymentFulfillmentService#event`: [L317](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L317)
- `PaymentFulfillmentService#lockProviderTransaction`: [L321](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L321)
- `PaymentFulfillmentService#providerTransactionClaimed`: [L325](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L325)
- `PaymentFulfillmentService#replayEvent`: [L329](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L329), [L333](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L333)
- `PaymentFulfillmentService#coursePaymentForUpdate`: [L347](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L347)
- `PaymentFulfillmentService#coursePayment`: [L351](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L351)
- `PaymentFulfillmentService#webhookResponse`: [L355](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L355)
- `PaymentFulfillmentService#blank`: [L359](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L359)
- `PaymentFulfillmentService#normalizeReference`: [L363](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L363)
- `PaymentFulfillmentService#createArtifacts`: [L367](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L367)
- `PaymentFulfillmentService#paymentForUpdate`: [L371](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L371)
- `PaymentFulfillmentService#payment`: [L375](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L375)
- `PaymentFulfillmentService#result`: [L379](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L379)
- `PaymentFulfillmentService#uuid`: [L390](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L390)
- `PaymentFulfillmentService#string`: [L394](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L394)
- `PaymentFulfillmentService#number`: [L398](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L398)
- `PaymentFulfillmentService#fulfillmentMode`: [L402](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L402)
- `PaymentFulfillmentService#amount`: [L406](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L406)
- `PaymentFulfillmentService#instant`: [L410](../../src/main/java/com/scms/backend/membership/payment/PaymentFulfillmentService.java#L410)

### [PaymentProviderEventRepository](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L9)

Package `com.scms.backend.membership.payment`. Component/repository tách từ PaymentFulfillmentService; facade giữ entrypoint và transaction.

- `PaymentProviderEventRepository#insertFinalEvent`: [L17](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L17), [L21](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L21)
- `PaymentProviderEventRepository#event`: [L30](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L30)
- `PaymentProviderEventRepository#lockProviderTransaction`: [L38](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L38)
- `PaymentProviderEventRepository#providerTransactionClaimed`: [L44](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L44)
- `PaymentProviderEventRepository#last4`: [L60](../../src/main/java/com/scms/backend/membership/payment/PaymentProviderEventRepository.java#L60)

### [PaymentRepository](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L26)

Package `com.scms.backend.membership.payment`. Component/repository tách từ PaymentService; facade giữ entrypoint và transaction.

- `PaymentRepository#benefitsByPlanCode`: [L37](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L37)
- `PaymentRepository#range`: [L61](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L61)
- `PaymentRepository#addRangeArgs`: [L70](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L70)
- `PaymentRepository#number`: [L77](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L77)
- `PaymentRepository#big`: [L81](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L81)
- `PaymentRepository#receipt`: [L85](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L85), [L89](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L89)
- `PaymentRepository#reconciliationRows`: [L93](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L93)
- `PaymentRepository#receptionReceiptRows`: [L133](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L133)
- `PaymentRepository#reportRows`: [L146](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L146)
- `PaymentRepository#cashFindAccounts`: [L169](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L169)
- `PaymentRepository#cashFindMembershipOrders`: [L176](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L176)
- `PaymentRepository#cashFindMembershipOffers`: [L185](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L185)
- `PaymentRepository#countActiveMemberships`: [L194](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L194)
- `PaymentRepository#countReservedMemberships`: [L198](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L198)
- `PaymentRepository#countPendingOrders`: [L202](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L202)
- `PaymentRepository#cashInsertMembershipOrders`: [L206](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L206)
- `PaymentRepository#reconcileUpdatePayments`: [L215](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L215)
- `PaymentRepository#reconcileUpdateMembershipOrders`: [L219](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L219)
- `PaymentRepository#cancelOrderFindMembershipOrders`: [L223](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L223)
- `PaymentRepository#cancelOrderUpdatePayments`: [L227](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L227)
- `PaymentRepository#cancelOrderUpdateMembershipOrders`: [L234](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L234)
- `PaymentRepository#receiptsFindReceipts`: [L241](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L241)
- `PaymentRepository#receiptsForReceptionistFindMemberProfiles`: [L253](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L253)
- `PaymentRepository#receiptFindReceipts`: [L260](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L260)
- `PaymentRepository#memberHistoryFindMemberships`: [L272](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L272)
- `PaymentRepository#staffMembershipHistoryFindMemberships`: [L276](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L276)
- `PaymentRepository#count`: [L283](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L283)
- `PaymentRepository#instant`: [L288](../../src/main/java/com/scms/backend/membership/payment/PaymentRepository.java#L288)

### [PaymentService](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L38)

Package `com.scms.backend.membership.payment`. Nghiệp vụ tiền và lịch sử Membership

- `PaymentService#cash`: [L66](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L66), [L71](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L71)
- `PaymentService#normalizeIdempotencyKey`: [L131](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L131)
- `PaymentService#reconcile`: [L141](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L141)
- `PaymentService#cancelOrder`: [L172](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L172)
- `PaymentService#reconciliationQueue`: [L198](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L198)
- `PaymentService#receipts`: [L208](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L208)
- `PaymentService#receiptsForReceptionist`: [L214](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L214)
- `PaymentService#receipt`: [L231](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L231), [L339](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L339), [L343](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L343)
- `PaymentService#result`: [L248](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L248)
- `PaymentService#memberHistory`: [L261](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L261)
- `PaymentService#receptionMembershipHistory`: [L273](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L273)
- `PaymentService#managerMembershipHistory`: [L280](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L280)
- `PaymentService#staffMembershipHistory`: [L287](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L287)
- `PaymentService#benefitsByPlanCode`: [L297](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L297)
- `PaymentService#instant`: [L309](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L309)
- `PaymentService#report`: [L313](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L313)
- `PaymentService#blank`: [L321](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L321)
- `PaymentService#active`: [L325](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L325)
- `PaymentService#ensure`: [L329](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L329)
- `PaymentService#ensureAny`: [L334](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L334)
- `PaymentService#reconciliationRows`: [L347](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L347)
- `PaymentService#receptionReceiptRows`: [L351](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L351)
- `PaymentService#reportRows`: [L355](../../src/main/java/com/scms/backend/membership/payment/PaymentService.java#L355)

### [SepayController](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L24)

Package `com.scms.backend.membership.payment`. Tạo QR, thu tiền khóa và callback ngân hàng

- `SepayController#create`: [L33](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L33)
- `SepayController#createForReceptionist`: [L39](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L39)
- `SepayController#createCoursePayment`: [L45](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L45)
- `SepayController#createCoursePaymentForReceptionist`: [L52](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L52)
- `SepayController#fulfillCourseCash`: [L59](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L59)
- `SepayController#webhook`: [L65](../../src/main/java/com/scms/backend/membership/payment/SepayController.java#L65)

### [SepayRepository](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L9)

Package `com.scms.backend.membership.payment`. Component/repository tách từ SepayService; facade giữ entrypoint và transaction.

- `SepayRepository#createForReceptionistFindAccounts`: [L11](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L11)
- `SepayRepository#findCourseRunId`: [L18](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L18)
- `SepayRepository#lockCourseRun`: [L22](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L22)
- `SepayRepository#lockCourseOrder`: [L26](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L26)
- `SepayRepository#lockCourseReservation`: [L30](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L30)
- `SepayRepository#findCoursePaymentContext`: [L34](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L34)
- `SepayRepository#lockCoursePayments`: [L45](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L45)
- `SepayRepository#createCoursePaymentInsertCoursePayments`: [L49](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L49)
- `SepayRepository#findCoursePayment`: [L58](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L58)
- `SepayRepository#createPaymentFindMembershipOrders`: [L62](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L62)
- `SepayRepository#createPaymentUpdateMembershipOrders`: [L66](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L66)
- `SepayRepository#createPaymentInsertPayments`: [L70](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L70)
- `SepayRepository#createPaymentFindPayments`: [L79](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L79)
- `SepayRepository#findMembershipPaymentByReference`: [L83](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L83)
- `SepayRepository#findMembershipPaymentByNormalizedReference`: [L87](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L87)
- `SepayRepository#findCoursePaymentByReference`: [L91](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L91)
- `SepayRepository#findCoursePaymentByNormalizedReference`: [L95](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L95)
- `SepayRepository#pendingPaymentFindPayments`: [L99](../../src/main/java/com/scms/backend/membership/payment/SepayRepository.java#L99)

### [SepayService](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L33)

Package `com.scms.backend.membership.payment`. Tạo giao dịch SePay và xử lý callback

- `SepayService#create`: [L72](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L72)
- `SepayService#createForReceptionist`: [L80](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L80)
- `SepayService#createCoursePayment`: [L94](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L94), [L127](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L127)
- `SepayService#createCoursePaymentForReceptionist`: [L103](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L103)
- `SepayService#fulfillCourseCash`: [L115](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L115)
- `SepayService#coursePaymentResponse`: [L160](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L160)
- `SepayService#requireIdempotencyKey`: [L169](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L169)
- `SepayService#courseRunPayable`: [L175](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L175)
- `SepayService#createPayment`: [L179](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L179)
- `SepayService#webhook`: [L207](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L207)
- `SepayService#normalizeReference`: [L240](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L240)
- `SepayService#payloadSha256`: [L244](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L244)
- `SepayService#normalized`: [L253](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L253)
- `SepayService#missingSnapshot`: [L257](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L257)
- `SepayService#pendingPayment`: [L262](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L262)
- `SepayService#response`: [L267](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L267)
- `SepayService#reference`: [L277](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L277)
- `SepayService#requireBankConfiguration`: [L281](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L281)
- `SepayService#invalidBankDestinationConfiguration`: [L300](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L300)
- `SepayService#enc`: [L304](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L304)
- `SepayService#instant`: [L308](../../src/main/java/com/scms/backend/membership/payment/SepayService.java#L308)

### [MembershipRefundController](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L18)

Package `com.scms.backend.membership.refund`. Nguồn tiền hoàn, tạo yêu cầu và ghi chi

- `MembershipRefundController#receptionList`: [L27](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L27)
- `MembershipRefundController#refundEligibleSources`: [L32](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L32)
- `MembershipRefundController#create`: [L37](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L37)
- `MembershipRefundController#cashDisbursement`: [L43](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L43)
- `MembershipRefundController#managerList`: [L48](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L48)
- `MembershipRefundController#firstApproval`: [L53](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L53)
- `MembershipRefundController#finalApproval`: [L58](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L58)
- `MembershipRefundController#reject`: [L63](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L63)
- `MembershipRefundController#id`: [L68](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundController.java#L68)

### [MembershipRefundRepository](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L11)

Package `com.scms.backend.membership.refund`. Component/repository tách từ MembershipRefundService; facade giữ entrypoint và transaction.

- `MembershipRefundRepository#display`: [L19](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L19)
- `MembershipRefundRepository#refundEligibleSourcesFindPayments`: [L36](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L36)
- `MembershipRefundRepository#createFindPayments`: [L66](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L66)
- `MembershipRefundRepository#finalApprovalInsertCreditNotes`: [L78](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L78)
- `MembershipRefundRepository#refundSourceFindMemberships`: [L82](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundRepository.java#L82)

### [MembershipRefundService](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L39)

Package `com.scms.backend.membership.refund`. Hoàn tiền gói; first/final approval thuộc Manager

- `MembershipRefundService#listForReceptionist`: [L77](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L77)
- `MembershipRefundService#listForManager`: [L84](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L84)
- `MembershipRefundService#refundEligibleSources`: [L91](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L91)
- `MembershipRefundService#create`: [L99](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L99)
- `MembershipRefundService#firstApproval`: [L140](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L140)
- `MembershipRefundService#cashDisbursement`: [L168](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L168)
- `MembershipRefundService#finalApproval`: [L183](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L183)
- `MembershipRefundService#reject`: [L207](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L207)
- `MembershipRefundService#refundSource`: [L230](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L230)
- `MembershipRefundService#eligibleSource`: [L234](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L234)
- `MembershipRefundService#eligibleQuote`: [L241](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L241), [L258](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L258)
- `MembershipRefundService#quote`: [L248](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L248)
- `MembershipRefundService#eligibleQuoteValues`: [L265](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L265)
- `MembershipRefundService#big`: [L277](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L277)
- `MembershipRefundService#lockReservedMembership`: [L281](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L281)
- `MembershipRefundService#member`: [L286](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L286)
- `MembershipRefundService#lock`: [L293](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L293)
- `MembershipRefundService#checkVersion`: [L297](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L297)
- `MembershipRefundService#ensureRole`: [L302](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L302)
- `MembershipRefundService#validatePage`: [L307](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L307)
- `MembershipRefundService#page`: [L312](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L312)
- `MembershipRefundService#response`: [L317](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L317)
- `MembershipRefundService#display`: [L321](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L321)
- `MembershipRefundService#write`: [L325](../../src/main/java/com/scms/backend/membership/refund/MembershipRefundService.java#L325)

### [MembershipTerminationController](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L18)

Package `com.scms.backend.membership.termination`. API tiếp nhận chấm dứt

- `MembershipTerminationController#memberList`: [L27](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L27)
- `MembershipTerminationController#memberCreate`: [L32](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L32)
- `MembershipTerminationController#memberWithdraw`: [L37](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L37)
- `MembershipTerminationController#receptionList`: [L43](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L43)
- `MembershipTerminationController#receptionCreate`: [L48](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L48)
- `MembershipTerminationController#managerList`: [L53](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L53)
- `MembershipTerminationController#approve`: [L58](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L58)
- `MembershipTerminationController#reject`: [L63](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L63)
- `MembershipTerminationController#direct`: [L68](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L68)
- `MembershipTerminationController#id`: [L73](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L73)
- `MembershipTerminationController#validateSize`: [L77](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationController.java#L77)

### [MembershipTerminationExecutionService](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L28)

Package `com.scms.backend.membership.termination`. Thực thi chấm dứt đã duyệt

- `MembershipTerminationExecutionService#executeImmediate`: [L67](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L67)
- `MembershipTerminationExecutionService#promoteAfterCommit`: [L87](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L87)
- `MembershipTerminationExecutionService#reconcileDue`: [L100](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L100)
- `MembershipTerminationExecutionService#writeOutcome`: [L117](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationExecutionService.java#L117)

### [MembershipTerminationService](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L32)

Package `com.scms.backend.membership.termination`. Yêu cầu chấm dứt; approve/reject thuộc Manager

- `MembershipTerminationService#listForMember`: [L66](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L66)
- `MembershipTerminationService#createForMember`: [L72](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L72)
- `MembershipTerminationService#listForReceptionist`: [L78](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L78)
- `MembershipTerminationService#createForReceptionist`: [L84](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L84)
- `MembershipTerminationService#withdrawForMember`: [L90](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L90)
- `MembershipTerminationService#listForManager`: [L102](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L102)
- `MembershipTerminationService#approve`: [L108](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L108)
- `MembershipTerminationService#reject`: [L129](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L129)
- `MembershipTerminationService#createDirect`: [L143](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L143)
- `MembershipTerminationService#create`: [L150](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L150)
- `MembershipTerminationService#notifyRole`: [L166](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L166)
- `MembershipTerminationService#lockRequest`: [L173](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L173)
- `MembershipTerminationService#checkVersion`: [L177](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L177)
- `MembershipTerminationService#validate`: [L182](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L182)
- `MembershipTerminationService#findMember`: [L188](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L188)
- `MembershipTerminationService#ensureRole`: [L194](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L194)
- `MembershipTerminationService#audit`: [L199](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L199)
- `MembershipTerminationService#pageable`: [L203](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L203)
- `MembershipTerminationService#page`: [L207](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L207)
- `MembershipTerminationService#managerPage`: [L211](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationService.java#L211)

### [NotificationController](../../src/main/java/com/scms/backend/notification/NotificationController.java#L18)

Package `com.scms.backend.notification`. API thông báo cá nhân

- `NotificationController#list`: [L28](../../src/main/java/com/scms/backend/notification/NotificationController.java#L28)
- `NotificationController#markRead`: [L34](../../src/main/java/com/scms/backend/notification/NotificationController.java#L34)
- `NotificationController#markAllRead`: [L40](../../src/main/java/com/scms/backend/notification/NotificationController.java#L40)
- `NotificationController#pageable`: [L46](../../src/main/java/com/scms/backend/notification/NotificationController.java#L46)
- `NotificationController#accountId`: [L56](../../src/main/java/com/scms/backend/notification/NotificationController.java#L56)

### [NotificationOutboxDeliveryService](../../src/main/java/com/scms/backend/notification/NotificationOutboxDeliveryService.java#L11)

Package `com.scms.backend.notification`. Chuyển outbox thành notification

- `NotificationOutboxDeliveryService#claimNext`: [L26](../../src/main/java/com/scms/backend/notification/NotificationOutboxDeliveryService.java#L26)
- `NotificationOutboxDeliveryService#deliver`: [L31](../../src/main/java/com/scms/backend/notification/NotificationOutboxDeliveryService.java#L31)
- `NotificationOutboxDeliveryService#markFailed`: [L37](../../src/main/java/com/scms/backend/notification/NotificationOutboxDeliveryService.java#L37)
- `NotificationOutboxDeliveryService#markTerminalFailed`: [L42](../../src/main/java/com/scms/backend/notification/NotificationOutboxDeliveryService.java#L42)
- `NotificationOutboxDeliveryService#detail`: [L47](../../src/main/java/com/scms/backend/notification/NotificationOutboxDeliveryService.java#L47)

### [NotificationOutboxDispatcher](../../src/main/java/com/scms/backend/notification/NotificationOutboxDispatcher.java#L11)

Package `com.scms.backend.notification`. Lịch xử lý outbox

- `NotificationOutboxDispatcher#dispatchAvailable`: [L29](../../src/main/java/com/scms/backend/notification/NotificationOutboxDispatcher.java#L29)

### [NotificationService](../../src/main/java/com/scms/backend/notification/NotificationService.java#L14)

Package `com.scms.backend.notification`. Đọc/đánh dấu thông báo thuộc actor

- `NotificationService#list`: [L29](../../src/main/java/com/scms/backend/notification/NotificationService.java#L29)
- `NotificationService#markRead`: [L36](../../src/main/java/com/scms/backend/notification/NotificationService.java#L36)
- `NotificationService#markAllRead`: [L46](../../src/main/java/com/scms/backend/notification/NotificationService.java#L46)
- `NotificationService#ensureActive`: [L53](../../src/main/java/com/scms/backend/notification/NotificationService.java#L53)

### [NotificationWriter](../../src/main/java/com/scms/backend/notification/NotificationWriter.java#L10)

Package `com.scms.backend.notification`. Ghi sự kiện thông báo cùng transaction

- `NotificationWriter#write`: [L21](../../src/main/java/com/scms/backend/notification/NotificationWriter.java#L21)

### [BookingController](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L22)

Package `com.scms.backend.scheduling.booking`. Đặt/hủy/xem lịch thay hội viên

- `BookingController#book`: [L31](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L31)
- `BookingController#cancel`: [L37](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L37)
- `BookingController#listForMember`: [L42](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L42)
- `BookingController#bookForMember`: [L47](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L47)
- `BookingController#cancelForMember`: [L53](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L53)
- `BookingController#listForReceptionist`: [L58](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L58)
- `BookingController#pageable`: [L63](../../src/main/java/com/scms/backend/scheduling/booking/BookingController.java#L63)

### [BookingService](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L34)

Package `com.scms.backend.scheduling.booking`. Kiểm tra điều kiện và lưu booking

- `BookingService#book`: [L79](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L79), [L90](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L90)
- `BookingService#bookForMember`: [L84](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L84)
- `BookingService#cancel`: [L132](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L132), [L165](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L165)
- `BookingService#cancelForMember`: [L137](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L137)
- `BookingService#listForMember`: [L143](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L143)
- `BookingService#listForReceptionist`: [L151](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L151)
- `BookingService#toSchedulePage`: [L160](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L160)
- `BookingService#ensureActiveMember`: [L187](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L187)
- `BookingService#ensureActiveReceptionist`: [L194](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L194)
- `BookingService#ensureMemberExists`: [L201](../../src/main/java/com/scms/backend/scheduling/booking/BookingService.java#L201)

### [ClassSessionCatalog](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCatalog.java#L16)

Package `com.scms.backend.scheduling.session`. Component/repository tách từ ClassSessionService; facade giữ entrypoint và transaction.

- `ClassSessionCatalog#futureCatalogue`: [L30](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCatalog.java#L30)
- `ClassSessionCatalog#emptyCatalogue`: [L53](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCatalog.java#L53)
- `ClassSessionCatalog#advanceStatuses`: [L57](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCatalog.java#L57)

### [ClassSessionResources](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResources.java#L28)

Package `com.scms.backend.scheduling.session`. Component/repository tách từ ClassSessionService; facade giữ entrypoint và transaction.

- `ClassSessionResources#validateTimes`: [L57](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResources.java#L57)
- `ClassSessionResources#validateResources`: [L72](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResources.java#L72)
- `ClassSessionResources#ensureAvailable`: [L118](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResources.java#L118)
- `ClassSessionResources#ensureCoachAvailable`: [L144](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResources.java#L144)

### [ClassSessionService](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L48)

Package `com.scms.backend.scheduling.session`. Đọc lịch nhóm/PT; các method quản trị không thuộc lễ tân

- `ClassSessionService#create`: [L110](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L110)
- `ClassSessionService#list`: [L127](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L127)
- `ClassSessionService#memberSchedule`: [L154](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L154)
- `ClassSessionService#receptionSchedule`: [L162](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L162)
- `ClassSessionService#futureSchedule`: [L170](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L170)
- `ClassSessionService#memberPtSchedule`: [L174](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L174)
- `ClassSessionService#staffPtSchedule`: [L185](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L185)
- `ClassSessionService#futureCatalogue`: [L193](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L193)
- `ClassSessionService#emptyCatalogue`: [L197](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L197)
- `ClassSessionService#get`: [L201](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L201)
- `ClassSessionService#availableCoaches`: [L209](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L209), [L216](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L216)
- `ClassSessionService#updateAssignment`: [L250](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L250)
- `ClassSessionService#ensurePtMembersCompatible`: [L308](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L308)
- `ClassSessionService#memberIdentity`: [L334](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L334)
- `ClassSessionService#advanceStatuses`: [L338](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L338)
- `ClassSessionService#ensureActiveManager`: [L342](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L342)
- `ClassSessionService#validateTimes`: [L348](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L348)
- `ClassSessionService#validateResources`: [L352](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L352)
- `ClassSessionService#lockAssignmentRooms`: [L356](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L356)
- `ClassSessionService#ensureAvailable`: [L364](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L364)
- `ClassSessionService#reschedule`: [L368](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L368)
- `ClassSessionService#scheduleSnapshot`: [L422](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L422)
- `ClassSessionService#moveForRoomImpact`: [L431](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L431)
- `ClassSessionService#rescheduleForRoomImpact`: [L451](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L451)
- `ClassSessionService#newScheduled`: [L469](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionService.java#L469)

### [PtSessionController](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L16)

Package `com.scms.backend.scheduling.session`. Danh mục buổi PT cho lễ tân

- `PtSessionController#member`: [L25](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L25)
- `PtSessionController#reception`: [L30](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L30)
- `PtSessionController#manager`: [L35](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L35)
- `PtSessionController#coach`: [L40](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L40)
- `PtSessionController#page`: [L45](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L45)
- `PtSessionController#subject`: [L51](../../src/main/java/com/scms/backend/scheduling/session/PtSessionController.java#L51)

### [ReceptionClassSessionController](../../src/main/java/com/scms/backend/scheduling/session/ReceptionClassSessionController.java#L16)

Package `com.scms.backend.scheduling.session`. Danh mục buổi tập nhóm

- `ReceptionClassSessionController#list`: [L26](../../src/main/java/com/scms/backend/scheduling/session/ReceptionClassSessionController.java#L26)

### [SessionLifecycleScheduler](../../src/main/java/com/scms/backend/scheduling/session/SessionLifecycleScheduler.java#L9)

Package `com.scms.backend.scheduling.session`. Lịch chạy lifecycle

- `SessionLifecycleScheduler#catchUpAfterStartup`: [L21](../../src/main/java/com/scms/backend/scheduling/session/SessionLifecycleScheduler.java#L21)
- `SessionLifecycleScheduler#catchUpOnSchedule`: [L26](../../src/main/java/com/scms/backend/scheduling/session/SessionLifecycleScheduler.java#L26)

### [SessionLifecycleService](../../src/main/java/com/scms/backend/scheduling/session/SessionLifecycleService.java#L9)

Package `com.scms.backend.scheduling.session`. Cập nhật trạng thái buổi học theo thời gian

- `SessionLifecycleService#catchUp`: [L24](../../src/main/java/com/scms/backend/scheduling/session/SessionLifecycleService.java#L24)

### [WaitlistController](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L15)

Package `com.scms.backend.scheduling.waitlist`. API danh sách chờ buổi tập

- `WaitlistController#join`: [L24](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L24)
- `WaitlistController#leave`: [L29](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L29)
- `WaitlistController#mine`: [L34](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L34)
- `WaitlistController#joinFor`: [L39](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L39)
- `WaitlistController#leaveFor`: [L44](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L44)
- `WaitlistController#reception`: [L49](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L49)
- `WaitlistController#manager`: [L54](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L54)
- `WaitlistController#subject`: [L59](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistController.java#L59)

### [WaitlistPromotionCoordinator](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionCoordinator.java#L18)

Package `com.scms.backend.scheduling.waitlist`. Điều phối promotion và transaction

- `WaitlistPromotionCoordinator#promote`: [L42](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionCoordinator.java#L42)
- `WaitlistPromotionCoordinator#promoteInTransaction`: [L50](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionCoordinator.java#L50)

### [WaitlistPromotionScheduler](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionScheduler.java#L12)

Package `com.scms.backend.scheduling.waitlist`. Tác vụ nền lấp chỗ từ waitlist

- `WaitlistPromotionScheduler#promoteAvailable`: [L33](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionScheduler.java#L33)

### [WaitlistPromotionService](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionService.java#L23)

Package `com.scms.backend.scheduling.waitlist`. Tự tạo booking khi có chỗ

- `WaitlistPromotionService#promoteAvailableBySessionIds`: [L50](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionService.java#L50)
- `WaitlistPromotionService#promoteAvailable`: [L56](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionService.java#L56)
- `WaitlistPromotionService#cancelWaiting`: [L88](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistPromotionService.java#L88)

### [WaitlistService](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L35)

Package `com.scms.backend.scheduling.waitlist`. Xếp/rút hàng đợi và kiểm tra điều kiện

- `WaitlistService#join`: [L68](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L68), [L79](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L79)
- `WaitlistService#joinForMember`: [L73](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L73)
- `WaitlistService#leave`: [L102](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L102), [L113](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L113)
- `WaitlistService#leaveForMember`: [L107](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L107)
- `WaitlistService#mine`: [L124](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L124)
- `WaitlistService#sessionList`: [L131](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L131)
- `WaitlistService#expireStarted`: [L138](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L138)
- `WaitlistService#validateEligibility`: [L147](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L147)
- `WaitlistService#tryLockMember`: [L169](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L169)
- `WaitlistService#ensureActiveMember`: [L173](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L173)
- `WaitlistService#ensureRole`: [L180](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L180)
- `WaitlistService#response`: [L185](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistService.java#L185)

## Phụ thuộc mở rộng

Các kiểu bên dưới dùng để lần theo DTO, entity, repository và nghiệp vụ phối hợp; không phải danh sách quyền riêng của lễ tân.

### [Account](../../src/main/java/com/scms/backend/account/Account.java#L17)

Package `com.scms.backend.account`. Phụ lục đã đối chiếu trước đó

- `Account#getStaffEmergencyContactName`: [L60](../../src/main/java/com/scms/backend/account/Account.java#L60)
- `Account#getStaffEmergencyContactPhone`: [L64](../../src/main/java/com/scms/backend/account/Account.java#L64)
- `Account#getStaffEmergencyContactRelationship`: [L68](../../src/main/java/com/scms/backend/account/Account.java#L68)
- `Account#updateStaffEmergencyContact`: [L72](../../src/main/java/com/scms/backend/account/Account.java#L72)
- `Account#pendingSelfRegistration`: [L118](../../src/main/java/com/scms/backend/account/Account.java#L118)
- `Account#getId`: [L125](../../src/main/java/com/scms/backend/account/Account.java#L125)
- `Account#updateMemberProfileDetails`: [L129](../../src/main/java/com/scms/backend/account/Account.java#L129)
- `Account#changePassword`: [L136](../../src/main/java/com/scms/backend/account/Account.java#L136)
- `Account#replaceStaffProfileImage`: [L140](../../src/main/java/com/scms/backend/account/Account.java#L140)
- `Account#changeMemberStatus`: [L149](../../src/main/java/com/scms/backend/account/Account.java#L149)
- `Account#verifyPendingEmail`: [L160](../../src/main/java/com/scms/backend/account/Account.java#L160)
- `Account#approveManualEmailVerificationBypass`: [L167](../../src/main/java/com/scms/backend/account/Account.java#L167)
- `Account#expirePendingRegistration`: [L173](../../src/main/java/com/scms/backend/account/Account.java#L173)
- `Account#changePendingEmail`: [L180](../../src/main/java/com/scms/backend/account/Account.java#L180)
- `Account#changeVerifiedEmail`: [L185](../../src/main/java/com/scms/backend/account/Account.java#L185)
- `Account#changeVerifiedStaffEmail`: [L194](../../src/main/java/com/scms/backend/account/Account.java#L194)
- `Account#reviveSelfRegistration`: [L202](../../src/main/java/com/scms/backend/account/Account.java#L202)
- `Account#ensurePending`: [L215](../../src/main/java/com/scms/backend/account/Account.java#L215)
- `Account#deactivateStaff`: [L221](../../src/main/java/com/scms/backend/account/Account.java#L221)
- `Account#getRole`: [L228](../../src/main/java/com/scms/backend/account/Account.java#L228)
- `Account#getStatus`: [L232](../../src/main/java/com/scms/backend/account/Account.java#L232)
- `Account#getFullName`: [L236](../../src/main/java/com/scms/backend/account/Account.java#L236)
- `Account#getPhone`: [L240](../../src/main/java/com/scms/backend/account/Account.java#L240)
- `Account#getEmail`: [L244](../../src/main/java/com/scms/backend/account/Account.java#L244)
- `Account#getBirthDate`: [L248](../../src/main/java/com/scms/backend/account/Account.java#L248)
- `Account#getPasswordHash`: [L252](../../src/main/java/com/scms/backend/account/Account.java#L252)
- `Account#getProfileImageUrl`: [L256](../../src/main/java/com/scms/backend/account/Account.java#L256)
- `Account#getEmailVerifiedAt`: [L260](../../src/main/java/com/scms/backend/account/Account.java#L260)
- `Account#isEmailVerificationRequired`: [L264](../../src/main/java/com/scms/backend/account/Account.java#L264)
- `Account#getRegistrationExpiresAt`: [L268](../../src/main/java/com/scms/backend/account/Account.java#L268)
- `Account#getAuthVersion`: [L272](../../src/main/java/com/scms/backend/account/Account.java#L272)
- `Account#getCreatedAt`: [L276](../../src/main/java/com/scms/backend/account/Account.java#L276)
- `Account#getUpdatedAt`: [L280](../../src/main/java/com/scms/backend/account/Account.java#L280)
- `Account#getVersion`: [L284](../../src/main/java/com/scms/backend/account/Account.java#L284)

### [AccountRepository](../../src/main/java/com/scms/backend/account/AccountRepository.java#L13)

Package `com.scms.backend.account`. Phụ lục đã đối chiếu trước đó

- `AccountRepository#findByEmailIgnoreCaseAndStatus`: [L15](../../src/main/java/com/scms/backend/account/AccountRepository.java#L15)
- `AccountRepository#findByPhoneAndStatus`: [L17](../../src/main/java/com/scms/backend/account/AccountRepository.java#L17)
- `AccountRepository#existsByEmailIgnoreCaseAndStatusNot`: [L19](../../src/main/java/com/scms/backend/account/AccountRepository.java#L19)
- `AccountRepository#existsByPhoneAndStatusNot`: [L21](../../src/main/java/com/scms/backend/account/AccountRepository.java#L21)
- `AccountRepository#existsByEmailIgnoreCaseAndStatusNotAndIdNot`: [L23](../../src/main/java/com/scms/backend/account/AccountRepository.java#L23)
- `AccountRepository#existsByPhoneAndStatusNotAndIdNot`: [L25](../../src/main/java/com/scms/backend/account/AccountRepository.java#L25)
- `AccountRepository#existsByIdAndStatus`: [L27](../../src/main/java/com/scms/backend/account/AccountRepository.java#L27)
- `AccountRepository#existsByIdAndStatusAndAuthVersion`: [L29](../../src/main/java/com/scms/backend/account/AccountRepository.java#L29)
- `AccountRepository#existsByIdAndRoleAndStatus`: [L31](../../src/main/java/com/scms/backend/account/AccountRepository.java#L31)
- `AccountRepository#existsByRole`: [L33](../../src/main/java/com/scms/backend/account/AccountRepository.java#L33)
- `AccountRepository#findByRoleAndStatusOrderById`: [L35](../../src/main/java/com/scms/backend/account/AccountRepository.java#L35)
- `AccountRepository#findMemberIdentity`: [L37](../../src/main/java/com/scms/backend/account/AccountRepository.java#L37)
- `AccountRepository#existsByOccupiedEmail`: [L53](../../src/main/java/com/scms/backend/account/AccountRepository.java#L53)
- `AccountRepository#existsByOccupiedPhone`: [L56](../../src/main/java/com/scms/backend/account/AccountRepository.java#L56)
- `AccountRepository#findByEmailForUpdate`: [L59](../../src/main/java/com/scms/backend/account/AccountRepository.java#L59)
- `AccountRepository#findExpiredSelfRegistrationForUpdate`: [L63](../../src/main/java/com/scms/backend/account/AccountRepository.java#L63)
- `AccountRepository#findActiveManagersForUpdate`: [L67](../../src/main/java/com/scms/backend/account/AccountRepository.java#L67)
- `AccountRepository#findAllByRoleAndStatusOrderByIdAsc`: [L71](../../src/main/java/com/scms/backend/account/AccountRepository.java#L71)
- `AccountRepository#findByIdForUpdate`: [L73](../../src/main/java/com/scms/backend/account/AccountRepository.java#L73)
- `AccountRepository#tryLockById`: [L77](../../src/main/java/com/scms/backend/account/AccountRepository.java#L77)

### [AccountRole](../../src/main/java/com/scms/backend/account/AccountRole.java#L3)

Package `com.scms.backend.account`. Phụ lục đã đối chiếu trước đó


### [AccountStatus](../../src/main/java/com/scms/backend/account/AccountStatus.java#L3)

Package `com.scms.backend.account`. Phụ lục đã đối chiếu trước đó


### [MemberProfile](../../src/main/java/com/scms/backend/account/MemberProfile.java#L18)

Package `com.scms.backend.account`. Phụ lục đã đối chiếu trước đó

- `MemberProfile#getAccountId`: [L88](../../src/main/java/com/scms/backend/account/MemberProfile.java#L88)
- `MemberProfile#updateDetails`: [L92](../../src/main/java/com/scms/backend/account/MemberProfile.java#L92)
- `MemberProfile#getAccount`: [L99](../../src/main/java/com/scms/backend/account/MemberProfile.java#L99)
- `MemberProfile#getMemberCode`: [L103](../../src/main/java/com/scms/backend/account/MemberProfile.java#L103)
- `MemberProfile#getProfileImageUrl`: [L107](../../src/main/java/com/scms/backend/account/MemberProfile.java#L107)
- `MemberProfile#getFitnessGoal`: [L111](../../src/main/java/com/scms/backend/account/MemberProfile.java#L111)
- `MemberProfile#getEmergencyContactName`: [L115](../../src/main/java/com/scms/backend/account/MemberProfile.java#L115)
- `MemberProfile#getEmergencyContactPhone`: [L119](../../src/main/java/com/scms/backend/account/MemberProfile.java#L119)
- `MemberProfile#updateAdditionalDetails`: [L123](../../src/main/java/com/scms/backend/account/MemberProfile.java#L123)
- `MemberProfile#getGender`: [L131](../../src/main/java/com/scms/backend/account/MemberProfile.java#L131)
- `MemberProfile#getAddress`: [L135](../../src/main/java/com/scms/backend/account/MemberProfile.java#L135)
- `MemberProfile#getPhysicalConditionNotes`: [L139](../../src/main/java/com/scms/backend/account/MemberProfile.java#L139)
- `MemberProfile#getEmergencyContactRelationship`: [L143](../../src/main/java/com/scms/backend/account/MemberProfile.java#L143)
- `MemberProfile#getEmergencyContactTimeNotes`: [L147](../../src/main/java/com/scms/backend/account/MemberProfile.java#L147)
- `MemberProfile#getCreatedAt`: [L151](../../src/main/java/com/scms/backend/account/MemberProfile.java#L151)
- `MemberProfile#getUpdatedAt`: [L155](../../src/main/java/com/scms/backend/account/MemberProfile.java#L155)
- `MemberProfile#getVersion`: [L159](../../src/main/java/com/scms/backend/account/MemberProfile.java#L159)

### [MemberProfileRepository](../../src/main/java/com/scms/backend/account/MemberProfileRepository.java#L11)

Package `com.scms.backend.account`. Phụ lục đã đối chiếu trước đó

- `MemberProfileRepository#findActivePlans`: [L20](../../src/main/java/com/scms/backend/account/MemberProfileRepository.java#L20)
- `MemberProfileRepository#findVisibleMemberByCode`: [L27](../../src/main/java/com/scms/backend/account/MemberProfileRepository.java#L27)
- `MemberProfileRepository#findVisibleMemberByPhone`: [L36](../../src/main/java/com/scms/backend/account/MemberProfileRepository.java#L36)

### [AuditEvent](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L21)

Package `com.scms.backend.audit`. Phụ lục đã đối chiếu trước đó

- `AuditEvent#getId`: [L87](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L87)
- `AuditEvent#getActorAccountId`: [L91](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L91)
- `AuditEvent#getActorNameSnapshot`: [L95](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L95)
- `AuditEvent#getActorRoleSnapshot`: [L99](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L99)
- `AuditEvent#getAction`: [L103](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L103)
- `AuditEvent#getTargetType`: [L107](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L107)
- `AuditEvent#getTargetId`: [L111](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L111)
- `AuditEvent#getReason`: [L115](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L115)
- `AuditEvent#getBeforeData`: [L119](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L119)
- `AuditEvent#getAfterData`: [L123](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L123)
- `AuditEvent#getCreatedAt`: [L127](../../src/main/java/com/scms/backend/audit/AuditEvent.java#L127)

### [AuditEventRepository](../../src/main/java/com/scms/backend/audit/AuditEventRepository.java#L7)

Package `com.scms.backend.audit`. Phụ lục đã đối chiếu trước đó


### [AuditHistoryController](../../src/main/java/com/scms/backend/audit/AuditHistoryController.java#L17)

Package `com.scms.backend.audit`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AuditHistoryController#list`: [L27](../../src/main/java/com/scms/backend/audit/AuditHistoryController.java#L27)

### [AuditHistoryPageResponse](../../src/main/java/com/scms/backend/audit/AuditHistoryPageResponse.java#L5)

Package `com.scms.backend.audit`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [AuditHistoryResponse](../../src/main/java/com/scms/backend/audit/AuditHistoryResponse.java#L10)

Package `com.scms.backend.audit`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AuditHistoryResponse#from`: [L12](../../src/main/java/com/scms/backend/audit/AuditHistoryResponse.java#L12)

### [AuditHistoryService](../../src/main/java/com/scms/backend/audit/AuditHistoryService.java#L21)

Package `com.scms.backend.audit`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AuditHistoryService#list`: [L33](../../src/main/java/com/scms/backend/audit/AuditHistoryService.java#L33)
- `AuditHistoryService#ensureActiveManager`: [L51](../../src/main/java/com/scms/backend/audit/AuditHistoryService.java#L51)
- `AuditHistoryService#normalize`: [L57](../../src/main/java/com/scms/backend/audit/AuditHistoryService.java#L57)

### [AuditHistorySpecifications](../../src/main/java/com/scms/backend/audit/AuditHistorySpecifications.java#L7)

Package `com.scms.backend.audit`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AuditHistorySpecifications#filtered`: [L12](../../src/main/java/com/scms/backend/audit/AuditHistorySpecifications.java#L12)

### [AuditHistoryValidationException](../../src/main/java/com/scms/backend/audit/AuditHistoryValidationException.java#L3)

Package `com.scms.backend.audit`. Phụ lục đã đối chiếu trước đó

- `AuditHistoryValidationException#getField`: [L12](../../src/main/java/com/scms/backend/audit/AuditHistoryValidationException.java#L12)

### [AuthProperties](../../src/main/java/com/scms/backend/auth/AuthProperties.java#L8)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [AuthResponse](../../src/main/java/com/scms/backend/auth/AuthResponse.java#L6)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [AuthSession](../../src/main/java/com/scms/backend/auth/AuthSession.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [ChangePasswordRequest](../../src/main/java/com/scms/backend/auth/ChangePasswordRequest.java#L5)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `ChangePasswordRequest#validate`: [L7](../../src/main/java/com/scms/backend/auth/ChangePasswordRequest.java#L7)
- `ChangePasswordRequest#rejectUnknownField`: [L17](../../src/main/java/com/scms/backend/auth/ChangePasswordRequest.java#L17)
- `ChangePasswordRequest#toString`: [L22](../../src/main/java/com/scms/backend/auth/ChangePasswordRequest.java#L22)

### [ChangePasswordValidationException](../../src/main/java/com/scms/backend/auth/ChangePasswordValidationException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `ChangePasswordValidationException#getField`: [L12](../../src/main/java/com/scms/backend/auth/ChangePasswordValidationException.java#L12)

### [CurrentPasswordIncorrectException](../../src/main/java/com/scms/backend/auth/CurrentPasswordIncorrectException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [DuplicateAccountException](../../src/main/java/com/scms/backend/auth/DuplicateAccountException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `DuplicateAccountException#email`: [L12](../../src/main/java/com/scms/backend/auth/DuplicateAccountException.java#L12)
- `DuplicateAccountException#phone`: [L16](../../src/main/java/com/scms/backend/auth/DuplicateAccountException.java#L16)
- `DuplicateAccountException#identifier`: [L20](../../src/main/java/com/scms/backend/auth/DuplicateAccountException.java#L20)
- `DuplicateAccountException#getCode`: [L24](../../src/main/java/com/scms/backend/auth/DuplicateAccountException.java#L24)

### [InvalidAuthenticatedAccountException](../../src/main/java/com/scms/backend/auth/InvalidAuthenticatedAccountException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [InvalidCredentialsException](../../src/main/java/com/scms/backend/auth/InvalidCredentialsException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [InvalidRefreshTokenException](../../src/main/java/com/scms/backend/auth/InvalidRefreshTokenException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [LoginRequest](../../src/main/java/com/scms/backend/auth/LoginRequest.java#L6)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `LoginRequest#toString`: [L14](../../src/main/java/com/scms/backend/auth/LoginRequest.java#L14)

### [NewPasswordSameAsCurrentException](../../src/main/java/com/scms/backend/auth/NewPasswordSameAsCurrentException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó


### [PasswordConstraints](../../src/main/java/com/scms/backend/auth/PasswordConstraints.java#L5)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `PasswordConstraints#validate`: [L14](../../src/main/java/com/scms/backend/auth/PasswordConstraints.java#L14)
- `PasswordConstraints#fitsBcrypt`: [L26](../../src/main/java/com/scms/backend/auth/PasswordConstraints.java#L26)

### [RefreshToken](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L18)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `RefreshToken#isUsableAt`: [L60](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L60)
- `RefreshToken#revoke`: [L64](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L64)
- `RefreshToken#getId`: [L71](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L71)
- `RefreshToken#getAccount`: [L75](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L75)
- `RefreshToken#getTokenHash`: [L79](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L79)
- `RefreshToken#getCreatedAt`: [L83](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L83)
- `RefreshToken#getExpiresAt`: [L87](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L87)
- `RefreshToken#getRevokedAt`: [L91](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L91)
- `RefreshToken#getReplacedByTokenId`: [L95](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L95)
- `RefreshToken#getVersion`: [L99](../../src/main/java/com/scms/backend/auth/RefreshToken.java#L99)

### [RefreshTokenRepository](../../src/main/java/com/scms/backend/auth/RefreshTokenRepository.java#L12)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `RefreshTokenRepository#findByTokenHash`: [L14](../../src/main/java/com/scms/backend/auth/RefreshTokenRepository.java#L14)
- `RefreshTokenRepository#findByTokenHashForUpdate`: [L17](../../src/main/java/com/scms/backend/auth/RefreshTokenRepository.java#L17)
- `RefreshTokenRepository#findAllByAccountId`: [L21](../../src/main/java/com/scms/backend/auth/RefreshTokenRepository.java#L21)

### [RegistrationController](../../src/main/java/com/scms/backend/auth/RegistrationController.java#L18)

Package `com.scms.backend.auth`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RegistrationController#register`: [L37](../../src/main/java/com/scms/backend/auth/RegistrationController.java#L37)

### [RegistrationOutcome](../../src/main/java/com/scms/backend/auth/RegistrationOutcome.java#L3)

Package `com.scms.backend.auth`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RegistrationRequest](../../src/main/java/com/scms/backend/auth/RegistrationRequest.java#L13)

Package `com.scms.backend.auth`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RegistrationRequest#rejectUnknownField`: [L27](../../src/main/java/com/scms/backend/auth/RegistrationRequest.java#L27)
- `RegistrationRequest#toString`: [L32](../../src/main/java/com/scms/backend/auth/RegistrationRequest.java#L32)

### [RegistrationResponse](../../src/main/java/com/scms/backend/auth/RegistrationResponse.java#L6)

Package `com.scms.backend.auth`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RegistrationService](../../src/main/java/com/scms/backend/auth/RegistrationService.java#L25)

Package `com.scms.backend.auth`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RegistrationService#register`: [L59](../../src/main/java/com/scms/backend/auth/RegistrationService.java#L59), [L64](../../src/main/java/com/scms/backend/auth/RegistrationService.java#L64)
- `RegistrationService#validatePassword`: [L119](../../src/main/java/com/scms/backend/auth/RegistrationService.java#L119)
- `RegistrationService#validateBirthDate`: [L123](../../src/main/java/com/scms/backend/auth/RegistrationService.java#L123)

### [RegistrationValidationException](../../src/main/java/com/scms/backend/auth/RegistrationValidationException.java#L3)

Package `com.scms.backend.auth`. Phụ lục đã đối chiếu trước đó

- `RegistrationValidationException#getField`: [L12](../../src/main/java/com/scms/backend/auth/RegistrationValidationException.java#L12)

### [CenterVisit](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L12)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó

- `CenterVisit#forCourse`: [L71](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L71)
- `CenterVisit#checkout`: [L79](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L79)
- `CenterVisit#getId`: [L87](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L87)
- `CenterVisit#getMemberAccountId`: [L91](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L91)
- `CenterVisit#getMembershipId`: [L95](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L95)
- `CenterVisit#getSource`: [L99](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L99)
- `CenterVisit#getCourseEnrollmentId`: [L103](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L103)
- `CenterVisit#getCourseSessionId`: [L107](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L107)
- `CenterVisit#getCheckedInByAccountId`: [L111](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L111)
- `CenterVisit#getCheckedInAt`: [L115](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L115)
- `CenterVisit#getCheckedOutAt`: [L119](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L119)
- `CenterVisit#getCheckedOutByAccountId`: [L123](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L123)
- `CenterVisit#getCheckoutSource`: [L127](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L127)
- `CenterVisit#getIdempotencyKey`: [L131](../../src/main/java/com/scms/backend/centervisit/CenterVisit.java#L131)

### [CenterVisitException](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L5)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó

- `CenterVisitException#identityVerificationRequired`: [L21](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L21)
- `CenterVisitException#memberNotActive`: [L25](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L25)
- `CenterVisitException#profileImageRequired`: [L29](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L29)
- `CenterVisitException#membershipRequired`: [L33](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L33)
- `CenterVisitException#membershipFrozen`: [L37](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L37)
- `CenterVisitException#courseCheckInNotEligible`: [L41](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L41)
- `CenterVisitException#invalidIdempotencyKey`: [L45](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L45)
- `CenterVisitException#idempotencyKeyReused`: [L49](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L49)
- `CenterVisitException#openVisitNotFound`: [L53](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L53)
- `CenterVisitException#memberNotFound`: [L57](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L57)
- `CenterVisitException#invalidPage`: [L61](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L61)
- `CenterVisitException#getStatus`: [L65](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L65)
- `CenterVisitException#getCode`: [L69](../../src/main/java/com/scms/backend/centervisit/CenterVisitException.java#L69)

### [CenterVisitPageResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitPageResponse.java#L5)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó


### [CenterVisitRepository](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L13)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó

- `CenterVisitRepository#findCurrentMembership`: [L15](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L15)
- `CenterVisitRepository#findEligibleCourseEnrollmentId`: [L27](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L27)
- `CenterVisitRepository#hasActiveMembershipFreeze`: [L50](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L50)
- `CenterVisitRepository#findByCheckedInByAccountIdAndIdempotencyKey`: [L58](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L58)
- `CenterVisitRepository#findFirstByMemberAccountIdAndCheckedOutAtIsNullOrderByCheckedInAtDescIdDesc`: [L60](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L60)
- `CenterVisitRepository#findFirstByMemberAccountIdAndCheckedOutAtIsNotNullOrderByCheckedOutAtDescIdDesc`: [L62](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L62)
- `CenterVisitRepository#findByCheckedOutAtIsNull`: [L64](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L64)
- `CenterVisitRepository#checkoutOpenForMembershipTermination`: [L66](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L66)
- `CenterVisitRepository#checkoutOpenForMembershipFreeze`: [L76](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L76)
- `CenterVisitRepository#findOverlappingVisitId`: [L84](../../src/main/java/com/scms/backend/centervisit/CenterVisitRepository.java#L84)

### [CenterVisitRequest](../../src/main/java/com/scms/backend/centervisit/CenterVisitRequest.java#L5)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó


### [CenterVisitResponse](../../src/main/java/com/scms/backend/centervisit/CenterVisitResponse.java#L7)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó

- `CenterVisitResponse#from`: [L9](../../src/main/java/com/scms/backend/centervisit/CenterVisitResponse.java#L9)

### [CenterVisitSource](../../src/main/java/com/scms/backend/centervisit/CenterVisitSource.java#L3)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó


### [CheckoutSource](../../src/main/java/com/scms/backend/centervisit/CheckoutSource.java#L3)

Package `com.scms.backend.centervisit`. Phụ lục đã đối chiếu trước đó


### [AccountProblemResponses](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L14)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AccountProblemResponses#handleMemberProfileValidation`: [L16](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L16)
- `AccountProblemResponses#handleReceptionMemberValidation`: [L24](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L24)
- `AccountProblemResponses#handleStaffAccountValidation`: [L32](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L32)
- `AccountProblemResponses#handleStaffAccountNotFound`: [L40](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L40)
- `AccountProblemResponses#handleMemberAccountNotFound`: [L47](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L47)
- `AccountProblemResponses#handleMemberStatusConflict`: [L54](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L54)
- `AccountProblemResponses#handleStaffStatusConflict`: [L61](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L61)
- `AccountProblemResponses#handleReceptionMemberNotFound`: [L68](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L68)
- `AccountProblemResponses#handleCenterVisit`: [L75](../../src/main/java/com/scms/backend/common/error/AccountProblemResponses.java#L75)

### [AuthenticationProblemResponses](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L17)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AuthenticationProblemResponses#handleInvalidCredentials`: [L19](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L19)
- `AuthenticationProblemResponses#handleInvalidRefreshToken`: [L26](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L26)
- `AuthenticationProblemResponses#handleInvalidAuthenticatedAccount`: [L33](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L33)
- `AuthenticationProblemResponses#handleCurrentPasswordIncorrect`: [L40](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L40)
- `AuthenticationProblemResponses#handleChangePasswordValidation`: [L47](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L47)
- `AuthenticationProblemResponses#handleNewPasswordSameAsCurrent`: [L55](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L55)
- `AuthenticationProblemResponses#handleRegistrationValidation`: [L62](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L62)
- `AuthenticationProblemResponses#handleDuplicateAccount`: [L70](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L70)
- `AuthenticationProblemResponses#handlePendingRegistration`: [L77](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L77)
- `AuthenticationProblemResponses#handleEmailVerification`: [L88](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L88)
- `AuthenticationProblemResponses#handlePasswordReset`: [L98](../../src/main/java/com/scms/backend/common/error/AuthenticationProblemResponses.java#L98)

### [CourseProblemResponses](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L14)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseProblemResponses#handleCourse`: [L16](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L16)
- `CourseProblemResponses#handleCourseOrder`: [L26](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L26)
- `CourseProblemResponses#handleCourseWaitlist`: [L33](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L33)
- `CourseProblemResponses#handleCourseTransfer`: [L40](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L40)
- `CourseProblemResponses#handleCourseWithdrawal`: [L47](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L47)
- `CourseProblemResponses#handleCourseRefund`: [L54](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L54)
- `CourseProblemResponses#handleCourseAttendance`: [L61](../../src/main/java/com/scms/backend/common/error/CourseProblemResponses.java#L61)

### [MembershipProblemResponses](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L18)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipProblemResponses#handleInvalidPlanCode`: [L20](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L20)
- `MembershipProblemResponses#handleMembershipOfferNotFound`: [L27](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L27)
- `MembershipProblemResponses#handleMembershipOfferUnavailable`: [L34](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L34)
- `MembershipProblemResponses#handleMembershipOrderMemberNotFound`: [L41](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L41)
- `MembershipProblemResponses#handleMemberNotActive`: [L48](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L48)
- `MembershipProblemResponses#handleActiveMembershipExists`: [L55](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L55)
- `MembershipProblemResponses#handlePendingMembershipOrderExists`: [L62](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L62)
- `MembershipProblemResponses#handleMembershipRenewalAlreadyExists`: [L69](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L69)
- `MembershipProblemResponses#handleSepayValidation`: [L76](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L76)
- `MembershipProblemResponses#handleMembershipOfferValidation`: [L83](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L83)
- `MembershipProblemResponses#handleMembershipPlanValidation`: [L90](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L90)
- `MembershipProblemResponses#handleMembershipPlanConflict`: [L98](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L98)
- `MembershipProblemResponses#handleMembershipPlanNotFound`: [L105](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L105)
- `MembershipProblemResponses#handleMembershipTermination`: [L112](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L112)
- `MembershipProblemResponses#handleMembershipFreeze`: [L119](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L119)
- `MembershipProblemResponses#handleRefund`: [L126](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L126)
- `MembershipProblemResponses#handlePayment`: [L133](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L133)
- `MembershipProblemResponses#handleReportingValidation`: [L140](../../src/main/java/com/scms/backend/common/error/MembershipProblemResponses.java#L140)

### [OperationsProblemResponses](../../src/main/java/com/scms/backend/common/error/OperationsProblemResponses.java#L10)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `OperationsProblemResponses#handleNotificationNotFound`: [L12](../../src/main/java/com/scms/backend/common/error/OperationsProblemResponses.java#L12)
- `OperationsProblemResponses#handleNotificationValidation`: [L19](../../src/main/java/com/scms/backend/common/error/OperationsProblemResponses.java#L19)
- `OperationsProblemResponses#handleAuditHistoryValidation`: [L27](../../src/main/java/com/scms/backend/common/error/OperationsProblemResponses.java#L27)

### [ProblemDetails](../../src/main/java/com/scms/backend/common/error/ProblemDetails.java#L9)

Package `com.scms.backend.common.error`. Phụ lục đã đối chiếu trước đó

- `ProblemDetails#create`: [L14](../../src/main/java/com/scms/backend/common/error/ProblemDetails.java#L14)
- `ProblemDetails#complete`: [L21](../../src/main/java/com/scms/backend/common/error/ProblemDetails.java#L21)
- `ProblemDetails#stableCode`: [L35](../../src/main/java/com/scms/backend/common/error/ProblemDetails.java#L35)

### [SchedulingProblemResponses](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L18)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `SchedulingProblemResponses#handleDisciplineValidation`: [L20](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L20)
- `SchedulingProblemResponses#handleDisciplineNotFound`: [L32](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L32)
- `SchedulingProblemResponses#handleDuplicateDiscipline`: [L39](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L39)
- `SchedulingProblemResponses#handleSportClass`: [L46](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L46)
- `SchedulingProblemResponses#handleRoom`: [L60](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L60)
- `SchedulingProblemResponses#handleRecurringSchedule`: [L73](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L73)
- `SchedulingProblemResponses#handlePtSessionAssignment`: [L92](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L92)
- `SchedulingProblemResponses#handleBooking`: [L101](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L101)
- `SchedulingProblemResponses#handleCoachLeave`: [L112](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L112)
- `SchedulingProblemResponses#handleWaitlist`: [L125](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L125)
- `SchedulingProblemResponses#handleAttendance`: [L132](../../src/main/java/com/scms/backend/common/error/SchedulingProblemResponses.java#L132)

### [TrainingProblemResponses](../../src/main/java/com/scms/backend/common/error/TrainingProblemResponses.java#L11)

Package `com.scms.backend.common.error`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `TrainingProblemResponses#handleCoachAssignment`: [L13](../../src/main/java/com/scms/backend/common/error/TrainingProblemResponses.java#L13)
- `TrainingProblemResponses#handleTrainingPlan`: [L26](../../src/main/java/com/scms/backend/common/error/TrainingProblemResponses.java#L26)
- `TrainingProblemResponses#handleTrainingResult`: [L43](../../src/main/java/com/scms/backend/common/error/TrainingProblemResponses.java#L43)
- `TrainingProblemResponses#handleTrainingFeedback`: [L50](../../src/main/java/com/scms/backend/common/error/TrainingProblemResponses.java#L50)

### [PersonNameRules](../../src/main/java/com/scms/backend/common/validation/PersonNameRules.java#L5)

Package `com.scms.backend.common.validation`. Phụ lục đã đối chiếu trước đó

- `PersonNameRules#isValid`: [L16](../../src/main/java/com/scms/backend/common/validation/PersonNameRules.java#L16)

### [OpenApiConfiguration](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L24)

Package `com.scms.backend.config`. Phụ lục đã đối chiếu trước đó

- `OpenApiConfiguration#apiContractCustomizer`: [L35](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L35)
- `OpenApiConfiguration#addDefaultResponse`: [L79](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L79)
- `OpenApiConfiguration#applyKnownResponseVariants`: [L85](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L85)
- `OpenApiConfiguration#problemSchema`: [L144](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L144)
- `OpenApiConfiguration#membershipBenefitRequiredProblemSchema`: [L148](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L148)
- `OpenApiConfiguration#unauthorizedProblemSchema`: [L155](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L155)
- `OpenApiConfiguration#ordinaryBookingConflictProblemSchema`: [L161](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L161)
- `OpenApiConfiguration#addBookingConflictResponse`: [L167](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L167)
- `OpenApiConfiguration#addErrorResponse`: [L172](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L172), [L176](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L176)
- `OpenApiConfiguration#reference`: [L180](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L180)
- `OpenApiConfiguration#constraint`: [L184](../../src/main/java/com/scms/backend/config/OpenApiConfiguration.java#L184)

### [TimeConfiguration](../../src/main/java/com/scms/backend/config/TimeConfiguration.java#L7)

Package `com.scms.backend.config`. Phụ lục đã đối chiếu trước đó

- `TimeConfiguration#clock`: [L10](../../src/main/java/com/scms/backend/config/TimeConfiguration.java#L10)

### [Course](../../src/main/java/com/scms/backend/course/Course.java#L19)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `Course#submittedForApproval`: [L97](../../src/main/java/com/scms/backend/course/Course.java#L97)
- `Course#update`: [L101](../../src/main/java/com/scms/backend/course/Course.java#L101)
- `Course#publish`: [L109](../../src/main/java/com/scms/backend/course/Course.java#L109)
- `Course#approve`: [L117](../../src/main/java/com/scms/backend/course/Course.java#L117)
- `Course#reject`: [L127](../../src/main/java/com/scms/backend/course/Course.java#L127)
- `Course#deactivate`: [L137](../../src/main/java/com/scms/backend/course/Course.java#L137)
- `Course#archive`: [L144](../../src/main/java/com/scms/backend/course/Course.java#L144)
- `Course#getId`: [L152](../../src/main/java/com/scms/backend/course/Course.java#L152)
- `Course#getCode`: [L156](../../src/main/java/com/scms/backend/course/Course.java#L156)
- `Course#getName`: [L160](../../src/main/java/com/scms/backend/course/Course.java#L160)
- `Course#getDescription`: [L164](../../src/main/java/com/scms/backend/course/Course.java#L164)
- `Course#getDiscipline`: [L168](../../src/main/java/com/scms/backend/course/Course.java#L168)
- `Course#getImageUrl`: [L172](../../src/main/java/com/scms/backend/course/Course.java#L172)
- `Course#getDefaultMinimumEnrollment`: [L176](../../src/main/java/com/scms/backend/course/Course.java#L176)
- `Course#getSubmittedByAccountId`: [L180](../../src/main/java/com/scms/backend/course/Course.java#L180)
- `Course#getSubmittedAt`: [L184](../../src/main/java/com/scms/backend/course/Course.java#L184)
- `Course#getReviewedByAccountId`: [L188](../../src/main/java/com/scms/backend/course/Course.java#L188)
- `Course#getReviewedAt`: [L192](../../src/main/java/com/scms/backend/course/Course.java#L192)
- `Course#getReviewReason`: [L196](../../src/main/java/com/scms/backend/course/Course.java#L196)
- `Course#getStatus`: [L200](../../src/main/java/com/scms/backend/course/Course.java#L200)
- `Course#getCreatedAt`: [L204](../../src/main/java/com/scms/backend/course/Course.java#L204)
- `Course#getUpdatedAt`: [L208](../../src/main/java/com/scms/backend/course/Course.java#L208)
- `Course#getVersion`: [L212](../../src/main/java/com/scms/backend/course/Course.java#L212)

### [CourseAttendanceException](../../src/main/java/com/scms/backend/course/CourseAttendanceException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseAttendanceException#getStatus`: [L17](../../src/main/java/com/scms/backend/course/CourseAttendanceException.java#L17)
- `CourseAttendanceException#getCode`: [L21](../../src/main/java/com/scms/backend/course/CourseAttendanceException.java#L21)
- `CourseAttendanceException#notFound`: [L25](../../src/main/java/com/scms/backend/course/CourseAttendanceException.java#L25)
- `CourseAttendanceException#closed`: [L29](../../src/main/java/com/scms/backend/course/CourseAttendanceException.java#L29)
- `CourseAttendanceException#invalid`: [L33](../../src/main/java/com/scms/backend/course/CourseAttendanceException.java#L33)

### [CourseAttendanceStatus](../../src/main/java/com/scms/backend/course/CourseAttendanceStatus.java#L5)

Package `com.scms.backend.course`. Tham chiếu kiểu từ CourseEnrollmentApiModels, CourseEnrollmentService

- `CourseAttendanceStatus#writableValues`: [L9](../../src/main/java/com/scms/backend/course/CourseAttendanceStatus.java#L9)

### [CourseException](../../src/main/java/com/scms/backend/course/CourseException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseException#sessionHasMembers`: [L24](../../src/main/java/com/scms/backend/course/CourseException.java#L24)
- `CourseException#validation`: [L28](../../src/main/java/com/scms/backend/course/CourseException.java#L28)
- `CourseException#notFound`: [L32](../../src/main/java/com/scms/backend/course/CourseException.java#L32)
- `CourseException#duplicateCode`: [L36](../../src/main/java/com/scms/backend/course/CourseException.java#L36)
- `CourseException#invalidCourseTransition`: [L40](../../src/main/java/com/scms/backend/course/CourseException.java#L40)
- `CourseException#invalidApprovalTransition`: [L44](../../src/main/java/com/scms/backend/course/CourseException.java#L44)
- `CourseException#changed`: [L48](../../src/main/java/com/scms/backend/course/CourseException.java#L48)
- `CourseException#runNotFound`: [L52](../../src/main/java/com/scms/backend/course/CourseException.java#L52)
- `CourseException#duplicateRunCode`: [L56](../../src/main/java/com/scms/backend/course/CourseException.java#L56)
- `CourseException#runChanged`: [L60](../../src/main/java/com/scms/backend/course/CourseException.java#L60)
- `CourseException#rescheduleIdempotencyConflict`: [L64](../../src/main/java/com/scms/backend/course/CourseException.java#L64)
- `CourseException#sessionNotFound`: [L68](../../src/main/java/com/scms/backend/course/CourseException.java#L68)
- `CourseException#sessionChanged`: [L72](../../src/main/java/com/scms/backend/course/CourseException.java#L72)
- `CourseException#invalidTransition`: [L76](../../src/main/java/com/scms/backend/course/CourseException.java#L76)
- `CourseException#invalidOpening`: [L80](../../src/main/java/com/scms/backend/course/CourseException.java#L80)
- `CourseException#immutableRun`: [L84](../../src/main/java/com/scms/backend/course/CourseException.java#L84)
- `CourseException#invalidSession`: [L88](../../src/main/java/com/scms/backend/course/CourseException.java#L88)
- `CourseException#resourceConflict`: [L92](../../src/main/java/com/scms/backend/course/CourseException.java#L92)
- `CourseException#roomCapacityConflict`: [L96](../../src/main/java/com/scms/backend/course/CourseException.java#L96)
- `CourseException#responseDeadline`: [L100](../../src/main/java/com/scms/backend/course/CourseException.java#L100)
- `CourseException#invalidResponse`: [L104](../../src/main/java/com/scms/backend/course/CourseException.java#L104)
- `CourseException#responseNotFound`: [L108](../../src/main/java/com/scms/backend/course/CourseException.java#L108)
- `CourseException#responseConflict`: [L112](../../src/main/java/com/scms/backend/course/CourseException.java#L112)
- `CourseException#responseClosed`: [L116](../../src/main/java/com/scms/backend/course/CourseException.java#L116)
- `CourseException#sessionDisruptionConflict`: [L120](../../src/main/java/com/scms/backend/course/CourseException.java#L120)
- `CourseException#getStatus`: [L124](../../src/main/java/com/scms/backend/course/CourseException.java#L124)
- `CourseException#getCode`: [L128](../../src/main/java/com/scms/backend/course/CourseException.java#L128)
- `CourseException#getField`: [L132](../../src/main/java/com/scms/backend/course/CourseException.java#L132)

### [CourseFieldLimits](../../src/main/java/com/scms/backend/course/CourseFieldLimits.java#L3)

Package `com.scms.backend.course`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseOrderException](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseOrderException#keyRequired`: [L17](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L17)
- `CourseOrderException#keyReused`: [L21](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L21)
- `CourseOrderException#runUnavailable`: [L25](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L25)
- `CourseOrderException#memberNotFound`: [L29](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L29)
- `CourseOrderException#memberNotActive`: [L33](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L33)
- `CourseOrderException#registrationNotOpen`: [L37](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L37)
- `CourseOrderException#registrationClosed`: [L41](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L41)
- `CourseOrderException#reservationExists`: [L45](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L45)
- `CourseOrderException#alreadyEnrolled`: [L49](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L49)
- `CourseOrderException#capacityFull`: [L53](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L53)
- `CourseOrderException#scheduleConflict`: [L57](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L57)
- `CourseOrderException#orderNotFound`: [L61](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L61)
- `CourseOrderException#getStatus`: [L65](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L65)
- `CourseOrderException#getCode`: [L69](../../src/main/java/com/scms/backend/course/CourseOrderException.java#L69)

### [CourseRefundException](../../src/main/java/com/scms/backend/course/CourseRefundException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseRefundException#status`: [L17](../../src/main/java/com/scms/backend/course/CourseRefundException.java#L17)
- `CourseRefundException#code`: [L21](../../src/main/java/com/scms/backend/course/CourseRefundException.java#L21)
- `CourseRefundException#invalid`: [L25](../../src/main/java/com/scms/backend/course/CourseRefundException.java#L25)
- `CourseRefundException#notFound`: [L29](../../src/main/java/com/scms/backend/course/CourseRefundException.java#L29)
- `CourseRefundException#conflict`: [L33](../../src/main/java/com/scms/backend/course/CourseRefundException.java#L33)

### [CourseRefundSource](../../src/main/java/com/scms/backend/course/CourseRefundSource.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseRefundStatus](../../src/main/java/com/scms/backend/course/CourseRefundStatus.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseRun](../../src/main/java/com/scms/backend/course/CourseRun.java#L20)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseRun#openForRegistration`: [L105](../../src/main/java/com/scms/backend/course/CourseRun.java#L105)
- `CourseRun#confirm`: [L115](../../src/main/java/com/scms/backend/course/CourseRun.java#L115)
- `CourseRun#start`: [L119](../../src/main/java/com/scms/backend/course/CourseRun.java#L119)
- `CourseRun#markSettlementPending`: [L123](../../src/main/java/com/scms/backend/course/CourseRun.java#L123)
- `CourseRun#complete`: [L127](../../src/main/java/com/scms/backend/course/CourseRun.java#L127)
- `CourseRun#cancel`: [L131](../../src/main/java/com/scms/backend/course/CourseRun.java#L131)
- `CourseRun#changeMinimumEnrollment`: [L139](../../src/main/java/com/scms/backend/course/CourseRun.java#L139)
- `CourseRun#changeSchedule`: [L148](../../src/main/java/com/scms/backend/course/CourseRun.java#L148)
- `CourseRun#getStatus`: [L155](../../src/main/java/com/scms/backend/course/CourseRun.java#L155)
- `CourseRun#getPolicyVersionSnapshot`: [L159](../../src/main/java/com/scms/backend/course/CourseRun.java#L159)
- `CourseRun#getMinimumEnrollment`: [L163](../../src/main/java/com/scms/backend/course/CourseRun.java#L163)
- `CourseRun#getCapacity`: [L167](../../src/main/java/com/scms/backend/course/CourseRun.java#L167)
- `CourseRun#hasValidOpeningDetails`: [L171](../../src/main/java/com/scms/backend/course/CourseRun.java#L171)
- `CourseRun#transition`: [L175](../../src/main/java/com/scms/backend/course/CourseRun.java#L175)
- `CourseRun#getId`: [L182](../../src/main/java/com/scms/backend/course/CourseRun.java#L182)
- `CourseRun#getRunCode`: [L186](../../src/main/java/com/scms/backend/course/CourseRun.java#L186)
- `CourseRun#getCourse`: [L190](../../src/main/java/com/scms/backend/course/CourseRun.java#L190)
- `CourseRun#getPrimaryCoach`: [L194](../../src/main/java/com/scms/backend/course/CourseRun.java#L194)
- `CourseRun#getTuitionVnd`: [L198](../../src/main/java/com/scms/backend/course/CourseRun.java#L198)
- `CourseRun#getRegistrationOpensAt`: [L202](../../src/main/java/com/scms/backend/course/CourseRun.java#L202)
- `CourseRun#getRegistrationClosesAt`: [L206](../../src/main/java/com/scms/backend/course/CourseRun.java#L206)
- `CourseRun#getPlannedStartAt`: [L210](../../src/main/java/com/scms/backend/course/CourseRun.java#L210)
- `CourseRun#getPlannedEndAt`: [L214](../../src/main/java/com/scms/backend/course/CourseRun.java#L214)
- `CourseRun#getTotalUnits`: [L218](../../src/main/java/com/scms/backend/course/CourseRun.java#L218)
- `CourseRun#isNamedCoachCommitment`: [L222](../../src/main/java/com/scms/backend/course/CourseRun.java#L222)
- `CourseRun#getCreatedAt`: [L226](../../src/main/java/com/scms/backend/course/CourseRun.java#L226)
- `CourseRun#getUpdatedAt`: [L230](../../src/main/java/com/scms/backend/course/CourseRun.java#L230)
- `CourseRun#getVersion`: [L234](../../src/main/java/com/scms/backend/course/CourseRun.java#L234)

### [CourseRunStatus](../../src/main/java/com/scms/backend/course/CourseRunStatus.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseSession](../../src/main/java/com/scms/backend/course/CourseSession.java#L21)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseSession#getId`: [L99](../../src/main/java/com/scms/backend/course/CourseSession.java#L99)
- `CourseSession#getCourseRun`: [L103](../../src/main/java/com/scms/backend/course/CourseSession.java#L103)
- `CourseSession#getUnitNumber`: [L107](../../src/main/java/com/scms/backend/course/CourseSession.java#L107)
- `CourseSession#getRoom`: [L111](../../src/main/java/com/scms/backend/course/CourseSession.java#L111)
- `CourseSession#getCoach`: [L115](../../src/main/java/com/scms/backend/course/CourseSession.java#L115)
- `CourseSession#getStartAt`: [L119](../../src/main/java/com/scms/backend/course/CourseSession.java#L119)
- `CourseSession#getEndAt`: [L123](../../src/main/java/com/scms/backend/course/CourseSession.java#L123)
- `CourseSession#getOriginalStartAt`: [L127](../../src/main/java/com/scms/backend/course/CourseSession.java#L127)
- `CourseSession#getOriginalEndAt`: [L131](../../src/main/java/com/scms/backend/course/CourseSession.java#L131)
- `CourseSession#getAllocationVnd`: [L135](../../src/main/java/com/scms/backend/course/CourseSession.java#L135)
- `CourseSession#getStatus`: [L139](../../src/main/java/com/scms/backend/course/CourseSession.java#L139)
- `CourseSession#getCreatedAt`: [L143](../../src/main/java/com/scms/backend/course/CourseSession.java#L143)
- `CourseSession#getUpdatedAt`: [L147](../../src/main/java/com/scms/backend/course/CourseSession.java#L147)
- `CourseSession#getVersion`: [L151](../../src/main/java/com/scms/backend/course/CourseSession.java#L151)
- `CourseSession#reschedule`: [L155](../../src/main/java/com/scms/backend/course/CourseSession.java#L155)
- `CourseSession#updateAssignment`: [L161](../../src/main/java/com/scms/backend/course/CourseSession.java#L161)
- `CourseSession#cancelPendingMakeup`: [L166](../../src/main/java/com/scms/backend/course/CourseSession.java#L166)
- `CourseSession#scheduleMakeup`: [L171](../../src/main/java/com/scms/backend/course/CourseSession.java#L171)
- `CourseSession#removeByRunChange`: [L176](../../src/main/java/com/scms/backend/course/CourseSession.java#L176)
- `CourseSession#linkReplacement`: [L181](../../src/main/java/com/scms/backend/course/CourseSession.java#L181)
- `CourseSession#getReplacementSession`: [L186](../../src/main/java/com/scms/backend/course/CourseSession.java#L186)
- `CourseSession#isMakeup`: [L190](../../src/main/java/com/scms/backend/course/CourseSession.java#L190)

### [CourseSessionStatus](../../src/main/java/com/scms/backend/course/CourseSessionStatus.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseStatus](../../src/main/java/com/scms/backend/course/CourseStatus.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseTransferException](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseTransferException#status`: [L17](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L17)
- `CourseTransferException#code`: [L21](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L21)
- `CourseTransferException#invalid`: [L25](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L25)
- `CourseTransferException#notFound`: [L29](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L29)
- `CourseTransferException#conflict`: [L33](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L33)
- `CourseTransferException#keyRequired`: [L37](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L37)
- `CourseTransferException#expiredOffer`: [L41](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L41)
- `CourseTransferException#value`: [L53](../../src/main/java/com/scms/backend/course/CourseTransferException.java#L53)

### [CourseWaitlistException](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseWaitlistException#unavailable`: [L17](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L17)
- `CourseWaitlistException#capacityAvailable`: [L21](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L21)
- `CourseWaitlistException#duplicate`: [L25](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L25)
- `CourseWaitlistException#notFound`: [L29](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L29)
- `CourseWaitlistException#invitationExpired`: [L33](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L33)
- `CourseWaitlistException#getStatus`: [L37](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L37)
- `CourseWaitlistException#getCode`: [L41](../../src/main/java/com/scms/backend/course/CourseWaitlistException.java#L41)

### [CourseWithdrawalCause](../../src/main/java/com/scms/backend/course/CourseWithdrawalCause.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseWithdrawalEligibilityReason](../../src/main/java/com/scms/backend/course/CourseWithdrawalEligibilityReason.java#L3)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó


### [CourseWithdrawalException](../../src/main/java/com/scms/backend/course/CourseWithdrawalException.java#L5)

Package `com.scms.backend.course`. Phụ lục đã đối chiếu trước đó

- `CourseWithdrawalException#status`: [L17](../../src/main/java/com/scms/backend/course/CourseWithdrawalException.java#L17)
- `CourseWithdrawalException#code`: [L21](../../src/main/java/com/scms/backend/course/CourseWithdrawalException.java#L21)
- `CourseWithdrawalException#invalid`: [L25](../../src/main/java/com/scms/backend/course/CourseWithdrawalException.java#L25)
- `CourseWithdrawalException#notFound`: [L29](../../src/main/java/com/scms/backend/course/CourseWithdrawalException.java#L29)
- `CourseWithdrawalException#conflict`: [L33](../../src/main/java/com/scms/backend/course/CourseWithdrawalException.java#L33)

### [RegistrationStatus](../../src/main/java/com/scms/backend/course/RegistrationStatus.java#L5)

Package `com.scms.backend.course`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService

- `RegistrationStatus#at`: [L9](../../src/main/java/com/scms/backend/course/RegistrationStatus.java#L9)

### [VersionRequest](../../src/main/java/com/scms/backend/course/VersionRequest.java#L7)

Package `com.scms.backend.course`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CatalogCoursePageResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCoursePageResponse.java#L7)

Package `com.scms.backend.course.catalog`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService

- `CatalogCoursePageResponse#from`: [L9](../../src/main/java/com/scms/backend/course/catalog/CatalogCoursePageResponse.java#L9)

### [CatalogCourseResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseResponse.java#L7)

Package `com.scms.backend.course.catalog`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService

- `CatalogCourseResponse#from`: [L9](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseResponse.java#L9)

### [CatalogCourseRunDetailResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunDetailResponse.java#L11)

Package `com.scms.backend.course.catalog`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService


### [CatalogCourseRunPageResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunPageResponse.java#L8)

Package `com.scms.backend.course.catalog`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService

- `CatalogCourseRunPageResponse#from`: [L10](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunPageResponse.java#L10)

### [CatalogCourseRunResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunResponse.java#L11)

Package `com.scms.backend.course.catalog`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService

- `CatalogCourseRunResponse#from`: [L13](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseRunResponse.java#L13)

### [CatalogCourseSessionResponse](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseSessionResponse.java#L10)

Package `com.scms.backend.course.catalog`. Tham chiếu kiểu từ CourseCatalogController, CourseCatalogService

- `CatalogCourseSessionResponse#from`: [L12](../../src/main/java/com/scms/backend/course/catalog/CatalogCourseSessionResponse.java#L12)

### [CourseImageController](../../src/main/java/com/scms/backend/course/catalog/CourseImageController.java#L18)

Package `com.scms.backend.course.catalog`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseImageController#upload`: [L30](../../src/main/java/com/scms/backend/course/catalog/CourseImageController.java#L30)

### [CourseEnrollmentApiModels](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentApiModels.java#L13)

Package `com.scms.backend.course.enrollment`. Phụ lục đã đối chiếu trước đó


### [CourseEnrollmentRepository](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L11)

Package `com.scms.backend.course.enrollment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseEnrollmentRepository#memberEnrollmentsFindCourseEnrollments`: [L13](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L13)
- `CourseEnrollmentRepository#rosterFindCourseEnrollmentSessions`: [L31](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L31)
- `CourseEnrollmentRepository#rosterFindCourseSessions`: [L44](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L44)
- `CourseEnrollmentRepository#updateAttendanceFindCourseRuns`: [L48](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L48)
- `CourseEnrollmentRepository#updateAttendanceFindCourseEnrollmentSessions`: [L59](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L59)
- `CourseEnrollmentRepository#updateAttendanceUpdateCourseEnrollmentSessions`: [L70](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentRepository.java#L70)

### [CourseEnrollmentResponse](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentResponse.java#L8)

Package `com.scms.backend.course.enrollment`. Phụ lục đã đối chiếu trước đó


### [CourseEnrollmentService](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L36)

Package `com.scms.backend.course.enrollment`. Phụ lục đã đối chiếu trước đó

- `CourseEnrollmentService#scheduleChanges`: [L58](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L58)
- `CourseEnrollmentService#respondToScheduleChange`: [L62](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L62)
- `CourseEnrollmentService#memberEnrollments`: [L66](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L66)
- `CourseEnrollmentService#roster`: [L78](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L78)
- `CourseEnrollmentService#updateAttendance`: [L87](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L87)
- `CourseEnrollmentService#active`: [L104](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L104)
- `CourseEnrollmentService#uuid`: [L109](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L109)
- `CourseEnrollmentService#text`: [L113](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L113)
- `CourseEnrollmentService#number`: [L117](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L117)
- `CourseEnrollmentService#amount`: [L121](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L121)
- `CourseEnrollmentService#instant`: [L126](../../src/main/java/com/scms/backend/course/enrollment/CourseEnrollmentService.java#L126)

### [CourseAdministration](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L31)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseAdministration#listCourses`: [L57](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L57)
- `CourseAdministration#getCourse`: [L62](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L62)
- `CourseAdministration#createCourse`: [L67](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L67)
- `CourseAdministration#listCourseApprovals`: [L72](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L72)
- `CourseAdministration#getCourseApproval`: [L77](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L77)
- `CourseAdministration#approveCourse`: [L82](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L82)
- `CourseAdministration#rejectCourse`: [L86](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L86)
- `CourseAdministration#createDraftCourse`: [L90](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L90)
- `CourseAdministration#decideCourse`: [L116](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L116)
- `CourseAdministration#patchCourse`: [L134](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L134)
- `CourseAdministration#publishCourse`: [L162](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L162)
- `CourseAdministration#deactivateCourse`: [L166](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L166)
- `CourseAdministration#archiveCourse`: [L170](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L170)
- `CourseAdministration#transitionCourse`: [L174](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L174)
- `CourseAdministration#cancelUnstartedRuns`: [L192](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L192)
- `CourseAdministration#lockCourse`: [L213](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L213)
- `CourseAdministration#lockRun`: [L217](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L217)
- `CourseAdministration#ensureActiveManager`: [L221](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L221)
- `CourseAdministration#required`: [L227](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L227)
- `CourseAdministration#optional`: [L234](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L234)
- `CourseAdministration#checkVersion`: [L243](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L243)
- `CourseAdministration#audit`: [L248](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L248)
- `CourseAdministration#snapshot`: [L252](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L252), [L256](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L256), [L260](../../src/main/java/com/scms/backend/course/management/CourseAdministration.java#L260)

### [CourseApprovalController](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L18)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseApprovalController#queue`: [L27](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L27)
- `CourseApprovalController#detail`: [L32](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L32)
- `CourseApprovalController#approve`: [L37](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L37)
- `CourseApprovalController#reject`: [L42](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L42)
- `CourseApprovalController#page`: [L47](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L47)
- `CourseApprovalController#accountId`: [L55](../../src/main/java/com/scms/backend/course/management/CourseApprovalController.java#L55)

### [CourseApprovalDecision](../../src/main/java/com/scms/backend/course/management/CourseApprovalDecision.java#L8)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseApprovalPageResponse](../../src/main/java/com/scms/backend/course/management/CourseApprovalPageResponse.java#L6)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseApprovalPageResponse#from`: [L8](../../src/main/java/com/scms/backend/course/management/CourseApprovalPageResponse.java#L8)

### [CourseApprovalResponse](../../src/main/java/com/scms/backend/course/management/CourseApprovalResponse.java#L8)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseApprovalResponse#from`: [L10](../../src/main/java/com/scms/backend/course/management/CourseApprovalResponse.java#L10)

### [CourseController](../../src/main/java/com/scms/backend/course/management/CourseController.java#L40)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseController#listCourses`: [L56](../../src/main/java/com/scms/backend/course/management/CourseController.java#L56)
- `CourseController#createCourse`: [L61](../../src/main/java/com/scms/backend/course/management/CourseController.java#L61)
- `CourseController#getCourse`: [L66](../../src/main/java/com/scms/backend/course/management/CourseController.java#L66)
- `CourseController#patchCourse`: [L71](../../src/main/java/com/scms/backend/course/management/CourseController.java#L71)
- `CourseController#publishCourse`: [L76](../../src/main/java/com/scms/backend/course/management/CourseController.java#L76)
- `CourseController#deactivateCourse`: [L81](../../src/main/java/com/scms/backend/course/management/CourseController.java#L81)
- `CourseController#archiveCourse`: [L86](../../src/main/java/com/scms/backend/course/management/CourseController.java#L86)
- `CourseController#listRuns`: [L91](../../src/main/java/com/scms/backend/course/management/CourseController.java#L91)
- `CourseController#createRun`: [L96](../../src/main/java/com/scms/backend/course/management/CourseController.java#L96)
- `CourseController#getRun`: [L101](../../src/main/java/com/scms/backend/course/management/CourseController.java#L101)
- `CourseController#patchRun`: [L106](../../src/main/java/com/scms/backend/course/management/CourseController.java#L106)
- `CourseController#openRun`: [L111](../../src/main/java/com/scms/backend/course/management/CourseController.java#L111)
- `CourseController#patchRunSchedule`: [L116](../../src/main/java/com/scms/backend/course/management/CourseController.java#L116)
- `CourseController#cancelRun`: [L121](../../src/main/java/com/scms/backend/course/management/CourseController.java#L121)
- `CourseController#cancellationImpact`: [L126](../../src/main/java/com/scms/backend/course/management/CourseController.java#L126)
- `CourseController#rescheduleRun`: [L131](../../src/main/java/com/scms/backend/course/management/CourseController.java#L131)
- `CourseController#listSessions`: [L137](../../src/main/java/com/scms/backend/course/management/CourseController.java#L137)
- `CourseController#createSession`: [L142](../../src/main/java/com/scms/backend/course/management/CourseController.java#L142)
- `CourseController#getSession`: [L147](../../src/main/java/com/scms/backend/course/management/CourseController.java#L147)
- `CourseController#patchSession`: [L152](../../src/main/java/com/scms/backend/course/management/CourseController.java#L152)
- `CourseController#cancelSession`: [L157](../../src/main/java/com/scms/backend/course/management/CourseController.java#L157)
- `CourseController#cancelWithMakeup`: [L162](../../src/main/java/com/scms/backend/course/management/CourseController.java#L162)
- `CourseController#scheduleMakeup`: [L167](../../src/main/java/com/scms/backend/course/management/CourseController.java#L167)
- `CourseController#page`: [L172](../../src/main/java/com/scms/backend/course/management/CourseController.java#L172)
- `CourseController#accountId`: [L180](../../src/main/java/com/scms/backend/course/management/CourseController.java#L180)

### [CourseCreateRequest](../../src/main/java/com/scms/backend/course/management/CourseCreateRequest.java#L11)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseManagementRepository](../../src/main/java/com/scms/backend/course/management/CourseManagementRepository.java#L8)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseManagementRepository#cancellationImpactFindCourseEnrollments`: [L10](../../src/main/java/com/scms/backend/course/management/CourseManagementRepository.java#L10)
- `CourseManagementRepository#patchRunScheduleInsertCourseScheduleChangeEvents`: [L23](../../src/main/java/com/scms/backend/course/management/CourseManagementRepository.java#L23)
- `CourseManagementRepository#patchRunScheduleUpdateCourseEnrollmentSessions`: [L31](../../src/main/java/com/scms/backend/course/management/CourseManagementRepository.java#L31)

### [CourseManagementService](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L35)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseManagementService#listCourses`: [L82](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L82)
- `CourseManagementService#getCourse`: [L87](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L87)
- `CourseManagementService#createCourse`: [L92](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L92)
- `CourseManagementService#listCourseApprovals`: [L97](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L97)
- `CourseManagementService#getCourseApproval`: [L102](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L102)
- `CourseManagementService#approveCourse`: [L107](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L107)
- `CourseManagementService#rejectCourse`: [L112](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L112)
- `CourseManagementService#patchCourse`: [L117](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L117)
- `CourseManagementService#publishCourse`: [L122](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L122)
- `CourseManagementService#deactivateCourse`: [L127](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L127)
- `CourseManagementService#archiveCourse`: [L132](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L132)
- `CourseManagementService#listRuns`: [L137](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L137)
- `CourseManagementService#getRun`: [L142](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L142)
- `CourseManagementService#createRun`: [L147](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L147)
- `CourseManagementService#patchRun`: [L152](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L152)
- `CourseManagementService#openRun`: [L157](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L157)
- `CourseManagementService#patchRunSchedule`: [L162](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L162)
- `CourseManagementService#cancelRun`: [L167](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L167)
- `CourseManagementService#cancellationImpact`: [L172](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L172)
- `CourseManagementService#listSessions`: [L180](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L180)
- `CourseManagementService#getSession`: [L190](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L190)
- `CourseManagementService#createSession`: [L197](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L197)
- `CourseManagementService#patchSession`: [L207](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L207)
- `CourseManagementService#originalSessionId`: [L220](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L220)
- `CourseManagementService#originalSessionIds`: [L224](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L224)
- `CourseManagementService#ensureActiveManager`: [L233](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L233)
- `CourseManagementService#required`: [L237](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L237)
- `CourseManagementService#audit`: [L241](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L241)
- `CourseManagementService#snapshot`: [L245](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L245), [L249](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L249), [L253](../../src/main/java/com/scms/backend/course/management/CourseManagementService.java#L253)

### [CourseManagementSnapshots](../../src/main/java/com/scms/backend/course/management/CourseManagementSnapshots.java#L9)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseManagementSnapshots#snapshot`: [L11](../../src/main/java/com/scms/backend/course/management/CourseManagementSnapshots.java#L11), [L28](../../src/main/java/com/scms/backend/course/management/CourseManagementSnapshots.java#L28), [L47](../../src/main/java/com/scms/backend/course/management/CourseManagementSnapshots.java#L47)

### [CoursePageResponse](../../src/main/java/com/scms/backend/course/management/CoursePageResponse.java#L6)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CoursePageResponse#from`: [L8](../../src/main/java/com/scms/backend/course/management/CoursePageResponse.java#L8)

### [CoursePatchRequest](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L13)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CoursePatchRequest#setExpectedVersion`: [L44](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L44)
- `CoursePatchRequest#setName`: [L49](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L49)
- `CoursePatchRequest#setDescription`: [L55](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L55)
- `CoursePatchRequest#setImageUrl`: [L61](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L61)
- `CoursePatchRequest#setDefaultMinimumEnrollment`: [L67](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L67)
- `CoursePatchRequest#rejectUnknownField`: [L73](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L73)
- `CoursePatchRequest#expectedVersion`: [L78](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L78)
- `CoursePatchRequest#hasName`: [L82](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L82)
- `CoursePatchRequest#name`: [L86](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L86)
- `CoursePatchRequest#hasDescription`: [L90](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L90)
- `CoursePatchRequest#description`: [L94](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L94)
- `CoursePatchRequest#hasImageUrl`: [L98](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L98)
- `CoursePatchRequest#imageUrl`: [L102](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L102)
- `CoursePatchRequest#hasDefaultMinimumEnrollment`: [L106](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L106)
- `CoursePatchRequest#defaultMinimumEnrollment`: [L110](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L110)
- `CoursePatchRequest#getDescription`: [L114](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L114)
- `CoursePatchRequest#getImageUrl`: [L119](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L119)
- `CoursePatchRequest#hasMutableField`: [L124](../../src/main/java/com/scms/backend/course/management/CoursePatchRequest.java#L124)

### [CourseRepository](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L16)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseCatalogService

- `CourseRepository#existsByCode`: [L18](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L18)
- `CourseRepository#findAll`: [L20](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L20)
- `CourseRepository#findByStatus`: [L23](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L23)
- `CourseRepository#findCatalogByStatus`: [L26](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L26)
- `CourseRepository#findCatalogByIdAndStatus`: [L30](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L30)
- `CourseRepository#findDetailedById`: [L34](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L34)
- `CourseRepository#findSubmittedById`: [L38](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L38)
- `CourseRepository#findByIdForUpdate`: [L42](../../src/main/java/com/scms/backend/course/management/CourseRepository.java#L42)

### [CourseResponse](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L8)

Package `com.scms.backend.course.management`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseResponse#from`: [L10](../../src/main/java/com/scms/backend/course/management/CourseResponse.java#L10)

### [CourseRunAdministration](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L39)

Package `com.scms.backend.course.management`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseRunAdministration#listRuns`: [L69](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L69)
- `CourseRunAdministration#getRun`: [L74](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L74)
- `CourseRunAdministration#createRun`: [L79](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L79)
- `CourseRunAdministration#patchRun`: [L106](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L106)
- `CourseRunAdministration#openRun`: [L120](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L120)
- `CourseRunAdministration#validateOpeningSchedule`: [L135](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L135)
- `CourseRunAdministration#patchRunSchedule`: [L152](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L152)
- `CourseRunAdministration#scheduleCause`: [L209](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L209)
- `CourseRunAdministration#cancelRun`: [L217](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L217)
- `CourseRunAdministration#lockCourse`: [L238](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L238)
- `CourseRunAdministration#lockRun`: [L242](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L242)
- `CourseRunAdministration#ensureActiveManager`: [L246](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L246)
- `CourseRunAdministration#validateRun`: [L250](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L250)
- `CourseRunAdministration#required`: [L267](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L267)
- `CourseRunAdministration#checkVersion`: [L271](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L271)
- `CourseRunAdministration#audit`: [L275](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L275)
- `CourseRunAdministration#snapshot`: [L279](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L279), [L283](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L283), [L287](../../src/main/java/com/scms/backend/course/management/CourseRunAdministration.java#L287)

### [CourseOrderCreateRequest](../../src/main/java/com/scms/backend/course/order/CourseOrderCreateRequest.java#L8)

Package `com.scms.backend.course.order`. Phụ lục đã đối chiếu trước đó

- `CourseOrderCreateRequest#rejectUnknownField`: [L11](../../src/main/java/com/scms/backend/course/order/CourseOrderCreateRequest.java#L11)

### [CourseOrderCreationRendezvous](../../src/main/java/com/scms/backend/course/order/CourseOrderCreationRendezvous.java#L5)

Package `com.scms.backend.course.order`. Phụ lục đã đối chiếu trước đó

- `CourseOrderCreationRendezvous#beforeRunLock`: [L8](../../src/main/java/com/scms/backend/course/order/CourseOrderCreationRendezvous.java#L8)
- `CourseOrderCreationRendezvous#afterRunLock`: [L11](../../src/main/java/com/scms/backend/course/order/CourseOrderCreationRendezvous.java#L11)

### [CourseOrderResponse](../../src/main/java/com/scms/backend/course/order/CourseOrderResponse.java#L8)

Package `com.scms.backend.course.order`. Phụ lục đã đối chiếu trước đó


### [CoursePaymentResponse](../../src/main/java/com/scms/backend/course/order/CoursePaymentResponse.java#L10)

Package `com.scms.backend.course.order`. Phụ lục đã đối chiếu trước đó

- `CoursePaymentResponse#qrUrl`: [L13](../../src/main/java/com/scms/backend/course/order/CoursePaymentResponse.java#L13)
- `CoursePaymentResponse#enc`: [L19](../../src/main/java/com/scms/backend/course/order/CoursePaymentResponse.java#L19)

### [CourseRefundApiModels](../../src/main/java/com/scms/backend/course/refund/CourseRefundApiModels.java#L9)

Package `com.scms.backend.course.refund`. Phụ lục đã đối chiếu trước đó


### [CourseRefundObligationWriter](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationWriter.java#L13)

Package `com.scms.backend.course.refund`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseRefundObligationWriter#create`: [L24](../../src/main/java/com/scms/backend/course/refund/CourseRefundObligationWriter.java#L24)

### [CourseUnitRefunds](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L14)

Package `com.scms.backend.course.refund`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseUnitRefunds#unserved`: [L22](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L22)
- `CourseUnitRefunds#eligible`: [L29](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L29)
- `CourseUnitRefunds#reduced`: [L40](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L40)
- `CourseUnitRefunds#cancelled`: [L44](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L44)
- `CourseUnitRefunds#settled`: [L50](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L50)
- `CourseUnitRefunds#create`: [L55](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L55)
- `CourseUnitRefunds#cancelStaleWithdrawal`: [L83](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefunds.java#L83)

### [CourseUnitRefundsRepository](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L10)

Package `com.scms.backend.course.refund`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseUnitRefundsRepository#createFindCourseEnrollments`: [L12](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L12)
- `CourseUnitRefundsRepository#createFindCourseEnrollmentSessions`: [L20](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L20)
- `CourseUnitRefundsRepository#createInsertCourseRefundObligations`: [L33](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L33)
- `CourseUnitRefundsRepository#createInsertCourseRefundUnitClaims`: [L41](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L41)
- `CourseUnitRefundsRepository#createInsertAuditEvents`: [L48](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L48)
- `CourseUnitRefundsRepository#cancelStaleWithdrawalFindCourseWithdrawalRequests`: [L56](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L56)
- `CourseUnitRefundsRepository#cancelStaleWithdrawalUpdateCourseEnrollments`: [L64](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L64)
- `CourseUnitRefundsRepository#cancelStaleWithdrawalInsertAuditEvents`: [L68](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L68)
- `CourseUnitRefundsRepository#cancelStaleWithdrawalInsertNotificationOutbox`: [L76](../../src/main/java/com/scms/backend/course/refund/CourseUnitRefundsRepository.java#L76)

### [CourseRunClosureRepository](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L9)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseRunClosureRepository#findDueRunIdsFindCourseRuns`: [L11](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L11)
- `CourseRunClosureRepository#closeAtRegistrationFindCourseEnrollments`: [L20](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L20)
- `CourseRunClosureRepository#cancelEffectsFindCourseOrders`: [L24](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L24)
- `CourseRunClosureRepository#lockOrderForClosure`: [L28](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L28)
- `CourseRunClosureRepository#cancelEffectsFindCourseReservations`: [L32](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L32)
- `CourseRunClosureRepository#lockReservationForClosure`: [L36](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L36)
- `CourseRunClosureRepository#cancelEffectsFindCoursePayments`: [L40](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L40)
- `CourseRunClosureRepository#lockPaymentForClosure`: [L47](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L47)
- `CourseRunClosureRepository#cancelEffectsUpdateCourseReservations`: [L51](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L51)
- `CourseRunClosureRepository#cancelEffectsUpdateCourseOrders`: [L55](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L55)
- `CourseRunClosureRepository#cancelEffectsUpdateCoursePayments`: [L59](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L59)
- `CourseRunClosureRepository#cancelEffectsUpdateCourseTransferHolds`: [L63](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L63)
- `CourseRunClosureRepository#cancelEffectsUpdateCourseTransferRequests`: [L67](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L67)
- `CourseRunClosureRepository#cancelEffectsUpdateCourseTransferOffers`: [L71](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L71)
- `CourseRunClosureRepository#cancelEffectsUpdateCourseSessions`: [L75](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L75)
- `CourseRunClosureRepository#cancelPendingWithdrawalsFindCourseWithdrawalRequests`: [L82](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L82)
- `CourseRunClosureRepository#cancelPendingWithdrawalsUpdateCourseEnrollments`: [L90](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L90)
- `CourseRunClosureRepository#affectedMembersFindCourseOrders`: [L94](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L94)
- `CourseRunClosureRepository#notifyEnrolledMembersFindCourseEnrollments`: [L98](../../src/main/java/com/scms/backend/course/run/CourseRunClosureRepository.java#L98)

### [CourseRunClosureScheduler](../../src/main/java/com/scms/backend/course/run/CourseRunClosureScheduler.java#L10)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseRunClosureScheduler#closeDueRuns`: [L28](../../src/main/java/com/scms/backend/course/run/CourseRunClosureScheduler.java#L28)

### [CourseRunClosureService](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L22)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseRunClosureService#findDueRunIds`: [L50](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L50)
- `CourseRunClosureService#closeAtRegistration`: [L54](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L54)
- `CourseRunClosureService#cancelEffects`: [L84](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L84), [L88](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L88), [L122](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L122)
- `CourseRunClosureService#cancelPendingWithdrawals`: [L113](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L113)
- `CourseRunClosureService#affectedMembers`: [L126](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L126)
- `CourseRunClosureService#notifyEnrolledMembers`: [L130](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L130)
- `CourseRunClosureService#snapshot`: [L136](../../src/main/java/com/scms/backend/course/run/CourseRunClosureService.java#L136)

### [CourseRunCreateRequest](../../src/main/java/com/scms/backend/course/run/CourseRunCreateRequest.java#L12)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseRunOpenRequest](../../src/main/java/com/scms/backend/course/run/CourseRunOpenRequest.java#L8)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseRunPageResponse](../../src/main/java/com/scms/backend/course/run/CourseRunPageResponse.java#L6)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseRunPageResponse#from`: [L8](../../src/main/java/com/scms/backend/course/run/CourseRunPageResponse.java#L8)

### [CourseRunPatchRequest](../../src/main/java/com/scms/backend/course/run/CourseRunPatchRequest.java#L7)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseRunRepository](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L16)

Package `com.scms.backend.course.run`. Phụ lục đã đối chiếu trước đó

- `CourseRunRepository#existsByRunCode`: [L18](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L18)
- `CourseRunRepository#findIdsByCourseId`: [L20](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L20)
- `CourseRunRepository#findAll`: [L23](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L23)
- `CourseRunRepository#findCatalogByStatusAndCourseStatus`: [L26](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L26)
- `CourseRunRepository#findCatalogByIdAndStatusAndCourseStatus`: [L33](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L33)
- `CourseRunRepository#findDetailedById`: [L40](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L40)
- `CourseRunRepository#findByIdForUpdate`: [L44](../../src/main/java/com/scms/backend/course/run/CourseRunRepository.java#L44)

### [CourseRunRescheduleRepository](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L9)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseRunRescheduleRepository#rescheduleFindCourseRuns`: [L11](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L11)
- `CourseRunRescheduleRepository#rescheduleFindCourseRunReschedules`: [L15](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L15)
- `CourseRunRescheduleRepository#rescheduleFindCourseSessions`: [L19](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L19)
- `CourseRunRescheduleRepository#hasConflictingCourseSessions`: [L23](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L23)
- `CourseRunRescheduleRepository#rescheduleInsertCourseScheduleChangeEvents`: [L30](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L30)
- `CourseRunRescheduleRepository#rescheduleUpdateCourseEnrollmentSessions`: [L37](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L37)
- `CourseRunRescheduleRepository#rescheduleInsertCourseRunReschedules`: [L41](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L41)
- `CourseRunRescheduleRepository#resourcesFindCourseSessions`: [L45](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRepository.java#L45)

### [CourseRunRescheduleRequest](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleRequest.java#L11)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseRunRescheduleResponse](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleResponse.java#L8)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseRunRescheduleService](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L44)

Package `com.scms.backend.course.run`. Phụ lục đã đối chiếu trước đó

- `CourseRunRescheduleService#reschedule`: [L84](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L84)
- `CourseRunRescheduleService#validate`: [L194](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L194)
- `CourseRunRescheduleService#resources`: [L209](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L209)
- `CourseRunRescheduleService#fingerprint`: [L213](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L213)
- `CourseRunRescheduleService#auditData`: [L221](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L221)
- `CourseRunRescheduleService#active`: [L226](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L226)
- `CourseRunRescheduleService#overlaps`: [L230](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L230)
- `CourseRunRescheduleService#timestamp`: [L234](../../src/main/java/com/scms/backend/course/run/CourseRunRescheduleService.java#L234)

### [CourseRunResponse](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L9)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseRunResponse#from`: [L11](../../src/main/java/com/scms/backend/course/run/CourseRunResponse.java#L11)

### [CourseRunScheduleRequest](../../src/main/java/com/scms/backend/course/run/CourseRunScheduleRequest.java#L9)

Package `com.scms.backend.course.run`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseWeeklyPreview](../../src/main/java/com/scms/backend/course/run/CourseWeeklyPreview.java#L6)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseWeeklyRunController](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunController.java#L15)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseWeeklyRunController#preview`: [L25](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunController.java#L25)
- `CourseWeeklyRunController#create`: [L31](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunController.java#L31)

### [CourseWeeklyRunCreateRequest](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunCreateRequest.java#L13)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseWeeklyRunResponse](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunResponse.java#L6)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseWeeklyRunService](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunService.java#L18)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseWeeklyRunService#preview`: [L36](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunService.java#L36)
- `CourseWeeklyRunService#create`: [L42](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunService.java#L42)
- `CourseWeeklyRunService#ensureManager`: [L63](../../src/main/java/com/scms/backend/course/run/CourseWeeklyRunService.java#L63)

### [CourseWeeklySchedule](../../src/main/java/com/scms/backend/course/run/CourseWeeklySchedule.java#L13)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseWeeklySchedule#generate`: [L20](../../src/main/java/com/scms/backend/course/run/CourseWeeklySchedule.java#L20)

### [CourseWeeklyScheduleRequest](../../src/main/java/com/scms/backend/course/run/CourseWeeklyScheduleRequest.java#L12)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseWeeklySlot](../../src/main/java/com/scms/backend/course/run/CourseWeeklySlot.java#L5)

Package `com.scms.backend.course.run`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseScheduleChangeApiModels](../../src/main/java/com/scms/backend/course/schedulechange/CourseScheduleChangeApiModels.java#L9)

Package `com.scms.backend.course.schedulechange`. Phụ lục đã đối chiếu trước đó


### [CourseAttendancePolicy](../../src/main/java/com/scms/backend/course/session/CourseAttendancePolicy.java#L6)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseEnrollmentService

- `CourseAttendancePolicy#evaluate`: [L13](../../src/main/java/com/scms/backend/course/session/CourseAttendancePolicy.java#L13)

### [CourseCoachSessionApiModels](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionApiModels.java#L10)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseCoachSessionController](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionController.java#L16)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseCoachSessionController#list`: [L25](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionController.java#L25)
- `CourseCoachSessionController#detail`: [L30](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionController.java#L30)
- `CourseCoachSessionController#subject`: [L35](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionController.java#L35)

### [CourseCoachSessionRepository](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L18)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseCoachSessionRepository#query`: [L54](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L54)
- `CourseCoachSessionRepository#orderBy`: [L72](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L72)
- `CourseCoachSessionRepository#map`: [L104](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L104)
- `CourseCoachSessionRepository#sessionPage`: [L111](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L111)
- `CourseCoachSessionRepository#sessionDetail`: [L124](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionRepository.java#L124)

### [CourseCoachSessionService](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L19)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseCoachSessionService#list`: [L35](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L35)
- `CourseCoachSessionService#detail`: [L41](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L41)
- `CourseCoachSessionService#query`: [L47](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L47)
- `CourseCoachSessionService#validate`: [L51](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L51)
- `CourseCoachSessionService#active`: [L67](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L67)
- `CourseCoachSessionService#sessionPage`: [L75](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L75)
- `CourseCoachSessionService#sessionDetail`: [L79](../../src/main/java/com/scms/backend/course/session/CourseCoachSessionService.java#L79)

### [CourseLeaveBatchActions](../../src/main/java/com/scms/backend/course/session/CourseLeaveBatchActions.java#L9)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseLeaveBatchActions#reassign`: [L21](../../src/main/java/com/scms/backend/course/session/CourseLeaveBatchActions.java#L21)
- `CourseLeaveBatchActions#cancel`: [L26](../../src/main/java/com/scms/backend/course/session/CourseLeaveBatchActions.java#L26)

### [CourseMakeupRequest](../../src/main/java/com/scms/backend/course/session/CourseMakeupRequest.java#L8)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseResourceAvailabilityController](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityController.java#L31)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseResourceAvailabilityController#available`: [L69](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityController.java#L69)

### [CourseResourceAvailabilityRepository](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityRepository.java#L9)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseResourceAvailabilityRepository#availableFindClassSessions`: [L11](../../src/main/java/com/scms/backend/course/session/CourseResourceAvailabilityRepository.java#L11)

### [CourseSessionCancelWithMakeupRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionCancelWithMakeupRequest.java#L8)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseSessionCreateRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionCreateRequest.java#L10)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseSessionDisruptionRepository](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L8)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseSessionDisruptionRepository#cancelInternalUpdateCourseEnrollmentSessions`: [L10](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L10)
- `CourseSessionDisruptionRepository#cancelWithMakeupUpdateCourseEnrollmentSessions`: [L14](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L14)
- `CourseSessionDisruptionRepository#scheduleMakeupUpdateCourseEnrollmentSessions`: [L18](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L18)
- `CourseSessionDisruptionRepository#advanceFindCourseRuns`: [L22](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L22)
- `CourseSessionDisruptionRepository#advanceFindCourseSessions`: [L36](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L36)
- `CourseSessionDisruptionRepository#markMakeupWarnings`: [L40](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L40)
- `CourseSessionDisruptionRepository#advanceUpdateCourseRuns`: [L48](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L48)
- `CourseSessionDisruptionRepository#advanceUpdateCourseSessions`: [L52](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L52)
- `CourseSessionDisruptionRepository#isRunCompletable`: [L56](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L56)
- `CourseSessionDisruptionRepository#settleUpdateCourseSessions`: [L60](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L60)
- `CourseSessionDisruptionRepository#settleUpdateCourseEnrollmentSessions`: [L64](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L64)
- `CourseSessionDisruptionRepository#settleFindCourseRefundObligations`: [L68](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L68)
- `CourseSessionDisruptionRepository#settleUpdateCourseRuns`: [L72](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L72)
- `CourseSessionDisruptionRepository#completeSettledRun`: [L76](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L76)
- `CourseSessionDisruptionRepository#settleFindCourseEnrollments`: [L80](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L80)
- `CourseSessionDisruptionRepository#enrolledMembersFindCourseEnrollmentSessions`: [L84](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRepository.java#L84)

### [CourseSessionDisruptionRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionRequest.java#L6)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseSessionDisruptionScheduler](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionScheduler.java#L10)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseSessionDisruptionScheduler#advanceDueSessions`: [L24](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionScheduler.java#L24)

### [CourseSessionDisruptionService](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L29)

Package `com.scms.backend.course.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseSessionDisruptionService#cancel`: [L60](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L60)
- `CourseSessionDisruptionService#cancelFromEndpoint`: [L65](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L65)
- `CourseSessionDisruptionService#cancelInternal`: [L70](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L70)
- `CourseSessionDisruptionService#cancelWithMakeup`: [L101](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L101)
- `CourseSessionDisruptionService#scheduleMakeup`: [L130](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L130)
- `CourseSessionDisruptionService#advance`: [L156](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L156)
- `CourseSessionDisruptionService#settle`: [L173](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L173)
- `CourseSessionDisruptionService#enrolledMembers`: [L186](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L186)
- `CourseSessionDisruptionService#notify`: [L190](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L190)
- `CourseSessionDisruptionService#audit`: [L196](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L196)
- `CourseSessionDisruptionService#snapshot`: [L200](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L200)
- `CourseSessionDisruptionService#checkSessionVersion`: [L210](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L210)
- `CourseSessionDisruptionService#ensureManager`: [L215](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L215)
- `CourseSessionDisruptionService#ensureDisruptible`: [L221](../../src/main/java/com/scms/backend/course/session/CourseSessionDisruptionService.java#L221)

### [CourseSessionPatchRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionPatchRequest.java#L8)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseSessionRepository](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L14)

Package `com.scms.backend.course.session`. Phụ lục đã đối chiếu trước đó

- `CourseSessionRepository#findByCourseRunIdOrderByStartAtAscIdAsc`: [L16](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L16)
- `CourseSessionRepository#findByIdAndCourseRunId`: [L19](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L19)
- `CourseSessionRepository#findByIdForUpdate`: [L22](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L22)
- `CourseSessionRepository#findRoomIdById`: [L26](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L26)
- `CourseSessionRepository#findCoachIdById`: [L29](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L29)
- `CourseSessionRepository#findCourseRunIdById`: [L32](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L32)
- `CourseSessionRepository#findOriginalIdByReplacementId`: [L35](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L35)
- `CourseSessionRepository#findOriginalIdsByReplacementIds`: [L38](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L38)
- `CourseSessionRepository#existsResourceOverlap`: [L48](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L48)
- `CourseSessionRepository#existsResourceOverlapExcluding`: [L57](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L57)
- `CourseSessionRepository#findActiveOverlapIdsByCoach`: [L67](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L67)
- `CourseSessionRepository#findRoomImpactIds`: [L77](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L77)
- `CourseSessionRepository#findBlockingRoomSessionIds`: [L87](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L87)
- `CourseSessionRepository#existsRunOverRoomCapacity`: [L96](../../src/main/java/com/scms/backend/course/session/CourseSessionRepository.java#L96)

### [CourseSessionRescheduleRequest](../../src/main/java/com/scms/backend/course/session/CourseSessionRescheduleRequest.java#L8)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService


### [CourseSessionResponse](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L10)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseSessionResponse#from`: [L12](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L12), [L16](../../src/main/java/com/scms/backend/course/session/CourseSessionResponse.java#L16)

### [CourseSessionService](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L27)

Package `com.scms.backend.course.session`. Tham chiếu kiểu từ CourseRunRescheduleService

- `CourseSessionService#create`: [L57](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L57), [L66](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L66)
- `CourseSessionService#createMakeup`: [L62](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L62)
- `CourseSessionService#reschedule`: [L87](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L87), [L92](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L92)
- `CourseSessionService#validateResources`: [L120](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L120)
- `CourseSessionService#validateAssignment`: [L133](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L133)
- `CourseSessionService#roomImpactIds`: [L143](../../src/main/java/com/scms/backend/course/session/CourseSessionService.java#L143)

### [CourseTransferApiModels](../../src/main/java/com/scms/backend/course/transfer/CourseTransferApiModels.java#L8)

Package `com.scms.backend.course.transfer`. Phụ lục đã đối chiếu trước đó


### [CourseTransferController](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L20)

Package `com.scms.backend.course.transfer`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseTransferController#queue`: [L29](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L29)
- `CourseTransferController#targets`: [L34](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L34)
- `CourseTransferController#approve`: [L39](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L39)
- `CourseTransferController#reject`: [L44](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L44)
- `CourseTransferController#offers`: [L49](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L49)
- `CourseTransferController#accept`: [L54](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L54)
- `CourseTransferController#subject`: [L59](../../src/main/java/com/scms/backend/course/transfer/CourseTransferController.java#L59)

### [CourseWaitlistController](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistController.java#L20)

Package `com.scms.backend.course.waitlist`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CourseWaitlistController#join`: [L29](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistController.java#L29)
- `CourseWaitlistController#leave`: [L34](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistController.java#L34)
- `CourseWaitlistController#mine`: [L39](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistController.java#L39)
- `CourseWaitlistController#accept`: [L44](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistController.java#L44)
- `CourseWaitlistController#subject`: [L50](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistController.java#L50)

### [CourseWaitlistResponse](../../src/main/java/com/scms/backend/course/waitlist/CourseWaitlistResponse.java#L6)

Package `com.scms.backend.course.waitlist`. Phụ lục đã đối chiếu trước đó


### [CourseWithdrawalApiModels](../../src/main/java/com/scms/backend/course/withdrawal/CourseWithdrawalApiModels.java#L9)

Package `com.scms.backend.course.withdrawal`. Phụ lục đã đối chiếu trước đó


### [AccountEmailChangeStatus](../../src/main/java/com/scms/backend/emailverification/AccountEmailChangeStatus.java#L5)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `AccountEmailChangeStatus#none`: [L7](../../src/main/java/com/scms/backend/emailverification/AccountEmailChangeStatus.java#L7)

### [DatabaseRateLimiter](../../src/main/java/com/scms/backend/emailverification/DatabaseRateLimiter.java#L11)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `DatabaseRateLimiter#check`: [L26](../../src/main/java/com/scms/backend/emailverification/DatabaseRateLimiter.java#L26)

### [EmailDeliveryException](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L5)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `EmailDeliveryException#retryable`: [L20](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L20), [L24](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L24), [L32](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L32)
- `EmailDeliveryException#terminal`: [L28](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L28)
- `EmailDeliveryException#errorName`: [L36](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L36)
- `EmailDeliveryException#retryAfter`: [L40](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L40)
- `EmailDeliveryException#sanitize`: [L44](../../src/main/java/com/scms/backend/emailverification/EmailDeliveryException.java#L44)

### [EmailVerificationConfiguration](../../src/main/java/com/scms/backend/emailverification/EmailVerificationConfiguration.java#L7)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó


### [EmailVerificationCrypto](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L13)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `EmailVerificationCrypto#otp`: [L24](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L24)
- `EmailVerificationCrypto#token`: [L28](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L28)
- `EmailVerificationCrypto#sha256`: [L34](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L34)
- `EmailVerificationCrypto#secureHashMatch`: [L38](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L38)
- `EmailVerificationCrypto#otpDigest`: [L42](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L42)
- `EmailVerificationCrypto#matchesOtp`: [L46](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L46)
- `EmailVerificationCrypto#rateKey`: [L50](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L50)
- `EmailVerificationCrypto#encryptOtp`: [L54](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L54)
- `EmailVerificationCrypto#encryptPayload`: [L58](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L58)
- `EmailVerificationCrypto#decryptOtp`: [L70](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L70)
- `EmailVerificationCrypto#decryptPayload`: [L74](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L74)
- `EmailVerificationCrypto#hmac`: [L84](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L84)
- `EmailVerificationCrypto#digest`: [L94](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L94)
- `EmailVerificationCrypto#hex`: [L102](../../src/main/java/com/scms/backend/emailverification/EmailVerificationCrypto.java#L102)

### [EmailVerificationException](../../src/main/java/com/scms/backend/emailverification/EmailVerificationException.java#L5)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `EmailVerificationException#getStatus`: [L24](../../src/main/java/com/scms/backend/emailverification/EmailVerificationException.java#L24)
- `EmailVerificationException#getCode`: [L28](../../src/main/java/com/scms/backend/emailverification/EmailVerificationException.java#L28)
- `EmailVerificationException#getRetryAfter`: [L32](../../src/main/java/com/scms/backend/emailverification/EmailVerificationException.java#L32)

### [EmailVerificationProperties](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L10)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `EmailVerificationProperties#validate`: [L37](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L37)
- `EmailVerificationProperties#hmacKeyMap`: [L51](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L51)
- `EmailVerificationProperties#encryptionKeyMap`: [L55](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L55)
- `EmailVerificationProperties#rateLimitKeyBytes`: [L59](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L59)
- `EmailVerificationProperties#keyMap`: [L63](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L63)
- `EmailVerificationProperties#decodeKey`: [L76](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L76)
- `EmailVerificationProperties#isEnabled`: [L87](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L87)
- `EmailVerificationProperties#setEnabled`: [L91](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L91)
- `EmailVerificationProperties#getRegistrationTtl`: [L95](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L95)
- `EmailVerificationProperties#setRegistrationTtl`: [L99](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L99)
- `EmailVerificationProperties#getOtpTtl`: [L103](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L103)
- `EmailVerificationProperties#setOtpTtl`: [L107](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L107)
- `EmailVerificationProperties#getResendCooldown`: [L111](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L111)
- `EmailVerificationProperties#setResendCooldown`: [L115](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L115)
- `EmailVerificationProperties#getMaxAttempts`: [L119](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L119)
- `EmailVerificationProperties#setMaxAttempts`: [L123](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L123)
- `EmailVerificationProperties#getHmacActiveVersion`: [L127](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L127)
- `EmailVerificationProperties#setHmacActiveVersion`: [L131](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L131)
- `EmailVerificationProperties#getEncryptionActiveVersion`: [L135](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L135)
- `EmailVerificationProperties#setEncryptionActiveVersion`: [L139](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L139)
- `EmailVerificationProperties#setHmacKeys`: [L143](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L143)
- `EmailVerificationProperties#setEncryptionKeys`: [L147](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L147)
- `EmailVerificationProperties#setRateLimitKey`: [L151](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L151)
- `EmailVerificationProperties#isCookieSecure`: [L155](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L155)
- `EmailVerificationProperties#setCookieSecure`: [L159](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L159)
- `EmailVerificationProperties#getCookieSameSite`: [L163](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L163)
- `EmailVerificationProperties#setCookieSameSite`: [L167](../../src/main/java/com/scms/backend/emailverification/EmailVerificationProperties.java#L167)

### [ManualActivationReasonCode](../../src/main/java/com/scms/backend/emailverification/ManualActivationReasonCode.java#L3)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó


### [ManualActivationStatus](../../src/main/java/com/scms/backend/emailverification/ManualActivationStatus.java#L3)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó


### [ManualVerificationMethod](../../src/main/java/com/scms/backend/emailverification/ManualVerificationMethod.java#L3)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó


### [PasswordResetException](../../src/main/java/com/scms/backend/emailverification/PasswordResetException.java#L5)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `PasswordResetException#getStatus`: [L27](../../src/main/java/com/scms/backend/emailverification/PasswordResetException.java#L27)
- `PasswordResetException#getCode`: [L31](../../src/main/java/com/scms/backend/emailverification/PasswordResetException.java#L31)
- `PasswordResetException#getField`: [L35](../../src/main/java/com/scms/backend/emailverification/PasswordResetException.java#L35)
- `PasswordResetException#getFieldMessage`: [L39](../../src/main/java/com/scms/backend/emailverification/PasswordResetException.java#L39)

### [PendingOriginPolicy](../../src/main/java/com/scms/backend/emailverification/PendingOriginPolicy.java#L9)

Package `com.scms.backend.emailverification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `PendingOriginPolicy#allows`: [L18](../../src/main/java/com/scms/backend/emailverification/PendingOriginPolicy.java#L18)

### [PendingRegistrationException](../../src/main/java/com/scms/backend/emailverification/PendingRegistrationException.java#L5)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `PendingRegistrationException#getOtpExpiresAt`: [L23](../../src/main/java/com/scms/backend/emailverification/PendingRegistrationException.java#L23)
- `PendingRegistrationException#getResendAvailableAt`: [L27](../../src/main/java/com/scms/backend/emailverification/PendingRegistrationException.java#L27)
- `PendingRegistrationException#getRegistrationExpiresAt`: [L31](../../src/main/java/com/scms/backend/emailverification/PendingRegistrationException.java#L31)
- `PendingRegistrationException#getServerTime`: [L35](../../src/main/java/com/scms/backend/emailverification/PendingRegistrationException.java#L35)

### [RailwayClientIpResolver](../../src/main/java/com/scms/backend/emailverification/RailwayClientIpResolver.java#L8)

Package `com.scms.backend.emailverification`. Phụ lục đã đối chiếu trước đó

- `RailwayClientIpResolver#resolve`: [L17](../../src/main/java/com/scms/backend/emailverification/RailwayClientIpResolver.java#L17)
- `RailwayClientIpResolver#isIpLiteral`: [L29](../../src/main/java/com/scms/backend/emailverification/RailwayClientIpResolver.java#L29)

### [ReferencedEmailKeyValidator](../../src/main/java/com/scms/backend/emailverification/ReferencedEmailKeyValidator.java#L9)

Package `com.scms.backend.emailverification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ReferencedEmailKeyValidator#run`: [L21](../../src/main/java/com/scms/backend/emailverification/ReferencedEmailKeyValidator.java#L21)

### [StaffEmailChangeStatus](../../src/main/java/com/scms/backend/emailverification/StaffEmailChangeStatus.java#L5)

Package `com.scms.backend.emailverification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `StaffEmailChangeStatus#none`: [L7](../../src/main/java/com/scms/backend/emailverification/StaffEmailChangeStatus.java#L7)

### [EmailDeliveryProperties](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L7)

Package `com.scms.backend.emailverification.delivery`. Tham chiếu kiểu từ EmailMessageRenderer, EmailOutboxEnqueuer, EmailVerificationConfiguration

- `EmailDeliveryProperties#validate`: [L26](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L26)
- `EmailDeliveryProperties#getProvider`: [L41](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L41)
- `EmailDeliveryProperties#setProvider`: [L45](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L45)
- `EmailDeliveryProperties#getFrom`: [L49](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L49)
- `EmailDeliveryProperties#setFrom`: [L53](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L53)
- `EmailDeliveryProperties#getSmtpUsername`: [L57](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L57)
- `EmailDeliveryProperties#setSmtpUsername`: [L61](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L61)
- `EmailDeliveryProperties#isAllowCustomFrom`: [L65](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L65)
- `EmailDeliveryProperties#setAllowCustomFrom`: [L69](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L69)
- `EmailDeliveryProperties#getRecipientOverride`: [L73](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L73)
- `EmailDeliveryProperties#setRecipientOverride`: [L77](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L77)
- `EmailDeliveryProperties#setStaging`: [L81](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L81)
- `EmailDeliveryProperties#setProduction`: [L85](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L85)
- `EmailDeliveryProperties#recipient`: [L89](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L89)
- `EmailDeliveryProperties#getSupportContact`: [L93](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L93)
- `EmailDeliveryProperties#setSupportContact`: [L97](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryProperties.java#L97)

### [EmailDeliveryRequest](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryRequest.java#L5)

Package `com.scms.backend.emailverification.delivery`. Phụ lục đã đối chiếu trước đó


### [EmailDeliveryResult](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryResult.java#L3)

Package `com.scms.backend.emailverification.delivery`. Phụ lục đã đối chiếu trước đó

- `EmailDeliveryResult#accepted`: [L5](../../src/main/java/com/scms/backend/emailverification/delivery/EmailDeliveryResult.java#L5)

### [EmailMessage](../../src/main/java/com/scms/backend/emailverification/delivery/EmailMessage.java#L5)

Package `com.scms.backend.emailverification.delivery`. Phụ lục đã đối chiếu trước đó


### [EmailMessageRenderer](../../src/main/java/com/scms/backend/emailverification/delivery/EmailMessageRenderer.java#L11)

Package `com.scms.backend.emailverification.delivery`. Phụ lục đã đối chiếu trước đó

- `EmailMessageRenderer#render`: [L32](../../src/main/java/com/scms/backend/emailverification/delivery/EmailMessageRenderer.java#L32)
- `EmailMessageRenderer#load`: [L57](../../src/main/java/com/scms/backend/emailverification/delivery/EmailMessageRenderer.java#L57)
- `EmailMessageRenderer#escape`: [L65](../../src/main/java/com/scms/backend/emailverification/delivery/EmailMessageRenderer.java#L65)

### [EmailPayloadCodec](../../src/main/java/com/scms/backend/emailverification/delivery/EmailPayloadCodec.java#L9)

Package `com.scms.backend.emailverification.delivery`. Phụ lục đã đối chiếu trước đó

- `EmailPayloadCodec#serialize`: [L14](../../src/main/java/com/scms/backend/emailverification/delivery/EmailPayloadCodec.java#L14)
- `EmailPayloadCodec#deserialize`: [L22](../../src/main/java/com/scms/backend/emailverification/delivery/EmailPayloadCodec.java#L22)
- `EmailPayloadCodec#textField`: [L30](../../src/main/java/com/scms/backend/emailverification/delivery/EmailPayloadCodec.java#L30)

### [EmailProvider](../../src/main/java/com/scms/backend/emailverification/delivery/EmailProvider.java#L3)

Package `com.scms.backend.emailverification.delivery`. Tham chiếu kiểu từ EmailProviderRouter

- `EmailProvider#providerName`: [L5](../../src/main/java/com/scms/backend/emailverification/delivery/EmailProvider.java#L5)
- `EmailProvider#send`: [L7](../../src/main/java/com/scms/backend/emailverification/delivery/EmailProvider.java#L7)

### [EmailProviderRouter](../../src/main/java/com/scms/backend/emailverification/delivery/EmailProviderRouter.java#L10)

Package `com.scms.backend.emailverification.delivery`. Phụ lục đã đối chiếu trước đó

- `EmailProviderRouter#send`: [L19](../../src/main/java/com/scms/backend/emailverification/delivery/EmailProviderRouter.java#L19)

### [SmtpEmailProvider](../../src/main/java/com/scms/backend/emailverification/delivery/SmtpEmailProvider.java#L14)

Package `com.scms.backend.emailverification.delivery`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `SmtpEmailProvider#validate`: [L32](../../src/main/java/com/scms/backend/emailverification/delivery/SmtpEmailProvider.java#L32)
- `SmtpEmailProvider#providerName`: [L42](../../src/main/java/com/scms/backend/emailverification/delivery/SmtpEmailProvider.java#L42)
- `SmtpEmailProvider#send`: [L47](../../src/main/java/com/scms/backend/emailverification/delivery/SmtpEmailProvider.java#L47)

### [AccountEmailChangeRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeRequest.java#L7)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [AccountEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeResponse.java#L5)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [AccountEmailChangeVerifyRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeVerifyRequest.java#L6)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [AccountEmailChangeVerifyResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/AccountEmailChangeVerifyResponse.java#L3)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [StaffEmailChangeRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeRequest.java#L7)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [StaffEmailChangeResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeResponse.java#L5)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [StaffEmailChangeVerifyRequest](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeVerifyRequest.java#L5)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [StaffEmailChangeVerifyResponse](../../src/main/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeVerifyResponse.java#L3)

Package `com.scms.backend.emailverification.emailchange`. Phụ lục đã đối chiếu trước đó


### [ManagerEmailVerificationController](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManagerEmailVerificationController.java#L19)

Package `com.scms.backend.emailverification.manualactivation`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerEmailVerificationController#list`: [L29](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManagerEmailVerificationController.java#L29)
- `ManagerEmailVerificationController#get`: [L34](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManagerEmailVerificationController.java#L34)
- `ManagerEmailVerificationController#approve`: [L39](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManagerEmailVerificationController.java#L39)
- `ManagerEmailVerificationController#reject`: [L44](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManagerEmailVerificationController.java#L44)
- `ManagerEmailVerificationController#id`: [L49](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManagerEmailVerificationController.java#L49)

### [ManualActivationCreateRequest](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationCreateRequest.java#L10)

Package `com.scms.backend.emailverification.manualactivation`. Phụ lục đã đối chiếu trước đó


### [ManualActivationDecisionRequest](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationDecisionRequest.java#L6)

Package `com.scms.backend.emailverification.manualactivation`. Phụ lục đã đối chiếu trước đó


### [ManualActivationPageResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationPageResponse.java#L5)

Package `com.scms.backend.emailverification.manualactivation`. Phụ lục đã đối chiếu trước đó


### [ManualActivationResponse](../../src/main/java/com/scms/backend/emailverification/manualactivation/ManualActivationResponse.java#L10)

Package `com.scms.backend.emailverification.manualactivation`. Phụ lục đã đối chiếu trước đó


### [PasswordResetAcceptedResponse](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetAcceptedResponse.java#L3)

Package `com.scms.backend.emailverification.passwordreset`. Phụ lục đã đối chiếu trước đó


### [PasswordResetConfirmRequest](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetConfirmRequest.java#L7)

Package `com.scms.backend.emailverification.passwordreset`. Phụ lục đã đối chiếu trước đó

- `PasswordResetConfirmRequest#validate`: [L9](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetConfirmRequest.java#L9)
- `PasswordResetConfirmRequest#invalid`: [L22](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetConfirmRequest.java#L22)
- `PasswordResetConfirmRequest#rejectUnknownField`: [L26](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetConfirmRequest.java#L26)
- `PasswordResetConfirmRequest#toString`: [L31](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetConfirmRequest.java#L31)

### [PasswordResetCookieService](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetCookieService.java#L8)

Package `com.scms.backend.emailverification.passwordreset`. Phụ lục đã đối chiếu trước đó

- `PasswordResetCookieService#create`: [L23](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetCookieService.java#L23)
- `PasswordResetCookieService#clear`: [L27](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetCookieService.java#L27)
- `PasswordResetCookieService#base`: [L31](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetCookieService.java#L31)

### [PasswordResetRequest](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetRequest.java#L7)

Package `com.scms.backend.emailverification.passwordreset`. Phụ lục đã đối chiếu trước đó


### [PasswordResetVerifiedResponse](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetVerifiedResponse.java#L5)

Package `com.scms.backend.emailverification.passwordreset`. Phụ lục đã đối chiếu trước đó


### [PasswordResetVerifyRequest](../../src/main/java/com/scms/backend/emailverification/passwordreset/PasswordResetVerifyRequest.java#L8)

Package `com.scms.backend.emailverification.passwordreset`. Phụ lục đã đối chiếu trước đó


### [ChangePendingEmailRequest](../../src/main/java/com/scms/backend/emailverification/verification/ChangePendingEmailRequest.java#L6)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [ChangePendingEmailResponse](../../src/main/java/com/scms/backend/emailverification/verification/ChangePendingEmailResponse.java#L5)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [EmailVerificationController](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationController.java#L24)

Package `com.scms.backend.emailverification.verification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `EmailVerificationController#verify`: [L46](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationController.java#L46)
- `EmailVerificationController#resend`: [L55](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationController.java#L55)
- `EmailVerificationController#change`: [L61](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationController.java#L61)
- `EmailVerificationController#pendingCookie`: [L70](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationController.java#L70)
- `EmailVerificationController#clearCookie`: [L74](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationController.java#L74)

### [EmailVerificationRegistration](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRegistration.java#L5)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [EmailVerificationRepository](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L15)

Package `com.scms.backend.emailverification.verification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `EmailVerificationRepository#beginInsertEmailVerificationSessions`: [L17](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L17)
- `EmailVerificationRepository#pendingConflictFindAccounts`: [L24](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L24)
- `EmailVerificationRepository#verifyFindAccounts`: [L32](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L32)
- `EmailVerificationRepository#verifyUpdateEmailVerificationChallenges`: [L42](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L42)
- `EmailVerificationRepository#consumeChallenge`: [L46](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L46)
- `EmailVerificationRepository#resendFindAccounts`: [L50](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L50)
- `EmailVerificationRepository#resendUpdateEmailVerificationChallenges`: [L59](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L59)
- `EmailVerificationRepository#changeEmailFindEmailVerificationSessions`: [L63](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L63)
- `EmailVerificationRepository#changeEmailUpdateAccounts`: [L71](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L71)
- `EmailVerificationRepository#changeEmailFindEmailVerificationChallenges`: [L78](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L78)
- `EmailVerificationRepository#changeEmailUpdateEmailVerificationChallenges`: [L82](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L82)
- `EmailVerificationRepository#changeEmailUpdateEmailVerificationSessions`: [L86](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L86)
- `EmailVerificationRepository#changeEmailInsertEmailVerificationSessions`: [L90](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L90)
- `EmailVerificationRepository#generateInsertEmailVerificationChallenges`: [L94](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L94)
- `EmailVerificationRepository#revokeArtifactsUpdateEmailVerificationSessions`: [L101](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L101)
- `EmailVerificationRepository#revokeArtifactsUpdateEmailOutbox`: [L105](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L105)
- `EmailVerificationRepository#cancelOutboxUpdateEmailOutbox`: [L109](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L109)
- `EmailVerificationRepository#cancelManualRequestsFindEmailVerificationManualActivationRequests`: [L113](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L113)
- `EmailVerificationRepository#cancelManualRequestsUpdateEmailVerificationManualActivationRequests`: [L117](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L117)
- `EmailVerificationRepository#cookieMatchesFindEmailVerificationSessions`: [L121](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L121)
- `EmailVerificationRepository#challengeRow`: [L125](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L125)
- `EmailVerificationRepository#instant`: [L129](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationRepository.java#L129)

### [EmailVerificationService](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L28)

Package `com.scms.backend.emailverification.verification`. Phụ lục đã đối chiếu trước đó

- `EmailVerificationService#enabled`: [L62](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L62)
- `EmailVerificationService#registrationExpiry`: [L66](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L66)
- `EmailVerificationService#rateLimitRegister`: [L70](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L70)
- `EmailVerificationService#begin`: [L78](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L78)
- `EmailVerificationService#pendingConflict`: [L87](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L87)
- `EmailVerificationService#verify`: [L93](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L93)
- `EmailVerificationService#resend`: [L133](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L133)
- `EmailVerificationService#changeEmail`: [L155](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L155)
- `EmailVerificationService#generate`: [L190](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L190)
- `EmailVerificationService#revokeArtifacts`: [L202](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L202)
- `EmailVerificationService#cancelOutbox`: [L207](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L207)
- `EmailVerificationService#cancelManualRequests`: [L211](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L211)
- `EmailVerificationService#cookieMatches`: [L217](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L217)
- `EmailVerificationService#invalidOtp`: [L224](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L224)
- `EmailVerificationService#invalidSession`: [L228](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L228)
- `EmailVerificationService#normalizeEmail`: [L232](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L232)
- `EmailVerificationService#mask`: [L236](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L236)
- `EmailVerificationService#min`: [L241](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L241)
- `EmailVerificationService#instant`: [L245](../../src/main/java/com/scms/backend/emailverification/verification/EmailVerificationService.java#L245)

### [PendingRegistrationCleanup](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanup.java#L18)

Package `com.scms.backend.emailverification.verification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `PendingRegistrationCleanup#cleanup`: [L37](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanup.java#L37)

### [PendingRegistrationCleanupRepository](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L9)

Package `com.scms.backend.emailverification.verification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `PendingRegistrationCleanupRepository#cleanupFindAccounts`: [L11](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L11)
- `PendingRegistrationCleanupRepository#cleanupFindEmailVerificationManualActivationRequests`: [L15](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L15)
- `PendingRegistrationCleanupRepository#cleanupUpdateEmailVerificationChallenges`: [L19](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L19)
- `PendingRegistrationCleanupRepository#cleanupUpdateEmailVerificationSessions`: [L23](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L23)
- `PendingRegistrationCleanupRepository#cleanupUpdateEmailOutbox`: [L27](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L27)
- `PendingRegistrationCleanupRepository#cleanupUpdateRefreshTokens`: [L31](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L31)
- `PendingRegistrationCleanupRepository#cleanupUpdateEmailVerificationManualActivationRequests`: [L35](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L35)
- `PendingRegistrationCleanupRepository#cleanupDeleteEmailVerificationRateLimits`: [L39](../../src/main/java/com/scms/backend/emailverification/verification/PendingRegistrationCleanupRepository.java#L39)

### [PendingVerificationAccountResponse](../../src/main/java/com/scms/backend/emailverification/verification/PendingVerificationAccountResponse.java#L6)

Package `com.scms.backend.emailverification.verification`. Phụ lục đã đối chiếu trước đó


### [ResendEmailRequest](../../src/main/java/com/scms/backend/emailverification/verification/ResendEmailRequest.java#L6)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [ResendEmailResponse](../../src/main/java/com/scms/backend/emailverification/verification/ResendEmailResponse.java#L5)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [VerifyEmailRequest](../../src/main/java/com/scms/backend/emailverification/verification/VerifyEmailRequest.java#L7)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [VerifyEmailResponse](../../src/main/java/com/scms/backend/emailverification/verification/VerifyEmailResponse.java#L5)

Package `com.scms.backend.emailverification.verification`. Tham chiếu kiểu từ EmailVerificationService


### [MemberProfileController](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileController.java#L13)

Package `com.scms.backend.identity.memberprofile`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberProfileController#getProfile`: [L23](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileController.java#L23)
- `MemberProfileController#updateProfile`: [L28](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileController.java#L28)
- `MemberProfileController#accountId`: [L36](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileController.java#L36)

### [MemberProfilePatchRequest](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L8)

Package `com.scms.backend.identity.memberprofile`. Phụ lục đã đối chiếu trước đó

- `MemberProfilePatchRequest#setFullName`: [L43](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L43)
- `MemberProfilePatchRequest#setPhone`: [L49](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L49)
- `MemberProfilePatchRequest#setEmail`: [L55](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L55)
- `MemberProfilePatchRequest#setBirthDate`: [L61](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L61)
- `MemberProfilePatchRequest#setProfileImageUrl`: [L67](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L67)
- `MemberProfilePatchRequest#setFitnessGoal`: [L73](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L73)
- `MemberProfilePatchRequest#setEmergencyContactName`: [L79](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L79)
- `MemberProfilePatchRequest#setEmergencyContactPhone`: [L85](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L85)
- `MemberProfilePatchRequest#setGender`: [L95](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L95)
- `MemberProfilePatchRequest#hasGender`: [L101](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L101)
- `MemberProfilePatchRequest#gender`: [L105](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L105)
- `MemberProfilePatchRequest#setAddress`: [L113](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L113)
- `MemberProfilePatchRequest#hasAddress`: [L119](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L119)
- `MemberProfilePatchRequest#address`: [L123](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L123)
- `MemberProfilePatchRequest#setPhysicalConditionNotes`: [L131](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L131)
- `MemberProfilePatchRequest#hasPhysicalConditionNotes`: [L137](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L137)
- `MemberProfilePatchRequest#physicalConditionNotes`: [L141](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L141)
- `MemberProfilePatchRequest#setEmergencyContactRelationship`: [L149](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L149)
- `MemberProfilePatchRequest#hasEmergencyContactRelationship`: [L155](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L155)
- `MemberProfilePatchRequest#emergencyContactRelationship`: [L159](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L159)
- `MemberProfilePatchRequest#setEmergencyContactTimeNotes`: [L167](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L167)
- `MemberProfilePatchRequest#hasEmergencyContactTimeNotes`: [L173](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L173)
- `MemberProfilePatchRequest#emergencyContactTimeNotes`: [L177](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L177)
- `MemberProfilePatchRequest#rejectUnknownField`: [L181](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L181)
- `MemberProfilePatchRequest#hasFullName`: [L186](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L186)
- `MemberProfilePatchRequest#fullName`: [L190](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L190)
- `MemberProfilePatchRequest#hasPhone`: [L194](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L194)
- `MemberProfilePatchRequest#phone`: [L198](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L198)
- `MemberProfilePatchRequest#hasEmail`: [L202](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L202)
- `MemberProfilePatchRequest#email`: [L206](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L206)
- `MemberProfilePatchRequest#hasBirthDate`: [L210](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L210)
- `MemberProfilePatchRequest#birthDate`: [L214](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L214)
- `MemberProfilePatchRequest#hasProfileImageUrl`: [L218](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L218)
- `MemberProfilePatchRequest#profileImageUrl`: [L222](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L222)
- `MemberProfilePatchRequest#hasFitnessGoal`: [L226](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L226)
- `MemberProfilePatchRequest#fitnessGoal`: [L230](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L230)
- `MemberProfilePatchRequest#hasEmergencyContactName`: [L234](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L234)
- `MemberProfilePatchRequest#emergencyContactName`: [L238](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L238)
- `MemberProfilePatchRequest#hasEmergencyContactPhone`: [L242](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L242)
- `MemberProfilePatchRequest#emergencyContactPhone`: [L246](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfilePatchRequest.java#L246)

### [MemberProfileResponse](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileResponse.java#L9)

Package `com.scms.backend.identity.memberprofile`. Phụ lục đã đối chiếu trước đó


### [MemberProfileService](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L17)

Package `com.scms.backend.identity.memberprofile`. Phụ lục đã đối chiếu trước đó

- `MemberProfileService#getProfile`: [L35](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L35)
- `MemberProfileService#updateProfile`: [L40](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L40)
- `MemberProfileService#replaceProfileImage`: [L73](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L73)
- `MemberProfileService#loadMemberAccount`: [L83](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L83)
- `MemberProfileService#loadProfile`: [L91](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L91)
- `MemberProfileService#toResponse`: [L95](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileService.java#L95)

### [MemberProfileValidationException](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileValidationException.java#L3)

Package `com.scms.backend.identity.memberprofile`. Phụ lục đã đối chiếu trước đó

- `MemberProfileValidationException#getField`: [L12](../../src/main/java/com/scms/backend/identity/memberprofile/MemberProfileValidationException.java#L12)

### [ReceptionActiveMembershipResponse](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionActiveMembershipResponse.java#L10)

Package `com.scms.backend.identity.receptionmember`. Phụ lục đã đối chiếu trước đó


### [ReceptionMemberCreateRequest](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateRequest.java#L13)

Package `com.scms.backend.identity.receptionmember`. Phụ lục đã đối chiếu trước đó

- `ReceptionMemberCreateRequest#rejectUnknownField`: [L27](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateRequest.java#L27)
- `ReceptionMemberCreateRequest#toString`: [L32](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateRequest.java#L32)

### [ReceptionMemberCreateResponse](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberCreateResponse.java#L8)

Package `com.scms.backend.identity.receptionmember`. Phụ lục đã đối chiếu trước đó


### [ReceptionMemberNotFoundException](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberNotFoundException.java#L3)

Package `com.scms.backend.identity.receptionmember`. Phụ lục đã đối chiếu trước đó


### [ReceptionMemberSearchResponse](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberSearchResponse.java#L8)

Package `com.scms.backend.identity.receptionmember`. Phụ lục đã đối chiếu trước đó


### [ReceptionMemberValidationException](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberValidationException.java#L3)

Package `com.scms.backend.identity.receptionmember`. Phụ lục đã đối chiếu trước đó

- `ReceptionMemberValidationException#getField`: [L12](../../src/main/java/com/scms/backend/identity/receptionmember/ReceptionMemberValidationException.java#L12)

### [StaffProfilePatchRequest](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfilePatchRequest.java#L7)

Package `com.scms.backend.identity.staffprofile`. Phụ lục đã đối chiếu trước đó

- `StaffProfilePatchRequest#rejectUnknownField`: [L14](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfilePatchRequest.java#L14)

### [StaffProfileResponse](../../src/main/java/com/scms/backend/identity/staffprofile/StaffProfileResponse.java#L10)

Package `com.scms.backend.identity.staffprofile`. Phụ lục đã đối chiếu trước đó


### [ManagerMemberController](../../src/main/java/com/scms/backend/manager/ManagerMemberController.java#L20)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerMemberController#search`: [L30](../../src/main/java/com/scms/backend/manager/ManagerMemberController.java#L30)
- `ManagerMemberController#get`: [L39](../../src/main/java/com/scms/backend/manager/ManagerMemberController.java#L39)
- `ManagerMemberController#update`: [L44](../../src/main/java/com/scms/backend/manager/ManagerMemberController.java#L44)
- `ManagerMemberController#accountId`: [L49](../../src/main/java/com/scms/backend/manager/ManagerMemberController.java#L49)

### [ManagerMemberService](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L23)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerMemberService#search`: [L41](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L41), [L46](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L46)
- `ManagerMemberService#get`: [L76](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L76)
- `ManagerMemberService#update`: [L82](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L82)
- `ManagerMemberService#ensureActiveManager`: [L111](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L111)
- `ManagerMemberService#findMember`: [L117](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L117)
- `ManagerMemberService#normalizeQuery`: [L121](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L121)
- `ManagerMemberService#toResponse`: [L132](../../src/main/java/com/scms/backend/manager/ManagerMemberService.java#L132)

### [MemberAccountNotFoundException](../../src/main/java/com/scms/backend/manager/MemberAccountNotFoundException.java#L3)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [MemberAccountPageResponse](../../src/main/java/com/scms/backend/manager/MemberAccountPageResponse.java#L5)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MemberAccountResponse](../../src/main/java/com/scms/backend/manager/MemberAccountResponse.java#L8)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberAccountResponse#withActivePlan`: [L14](../../src/main/java/com/scms/backend/manager/MemberAccountResponse.java#L14)

### [MemberStatusChangeRequest](../../src/main/java/com/scms/backend/manager/MemberStatusChangeRequest.java#L5)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MemberStatusChangeResponse](../../src/main/java/com/scms/backend/manager/MemberStatusChangeResponse.java#L6)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MemberStatusConflictException](../../src/main/java/com/scms/backend/manager/MemberStatusConflictException.java#L3)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [MemberStatusController](../../src/main/java/com/scms/backend/manager/MemberStatusController.java#L13)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberStatusController#changeStatus`: [L23](../../src/main/java/com/scms/backend/manager/MemberStatusController.java#L23)

### [MemberStatusRepository](../../src/main/java/com/scms/backend/manager/MemberStatusRepository.java#L9)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberStatusRepository#lockFutureBookings`: [L11](../../src/main/java/com/scms/backend/manager/MemberStatusRepository.java#L11)
- `MemberStatusRepository#cancelFutureBookings`: [L20](../../src/main/java/com/scms/backend/manager/MemberStatusRepository.java#L20)

### [MemberStatusService](../../src/main/java/com/scms/backend/manager/MemberStatusService.java#L23)

Package `com.scms.backend.manager`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberStatusService#changeStatus`: [L51](../../src/main/java/com/scms/backend/manager/MemberStatusService.java#L51)
- `MemberStatusService#cancelFutureBookings`: [L80](../../src/main/java/com/scms/backend/manager/MemberStatusService.java#L80)
- `MemberStatusService#ensureActiveManager`: [L90](../../src/main/java/com/scms/backend/manager/MemberStatusService.java#L90)
- `MemberStatusService#validate`: [L96](../../src/main/java/com/scms/backend/manager/MemberStatusService.java#L96)

### [StaffAccountCreateRequest](../../src/main/java/com/scms/backend/manager/StaffAccountCreateRequest.java#L8)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó

- `StaffAccountCreateRequest#rejectUnknownField`: [L19](../../src/main/java/com/scms/backend/manager/StaffAccountCreateRequest.java#L19)

### [StaffAccountNotFoundException](../../src/main/java/com/scms/backend/manager/StaffAccountNotFoundException.java#L3)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [StaffAccountPageResponse](../../src/main/java/com/scms/backend/manager/StaffAccountPageResponse.java#L5)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [StaffAccountPatchRequest](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L7)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó

- `StaffAccountPatchRequest#setFullName`: [L25](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L25)
- `StaffAccountPatchRequest#setPhone`: [L31](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L31)
- `StaffAccountPatchRequest#setEmail`: [L37](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L37)
- `StaffAccountPatchRequest#setBirthDate`: [L43](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L43)
- `StaffAccountPatchRequest#rejectUnknownField`: [L49](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L49)
- `StaffAccountPatchRequest#hasFullName`: [L54](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L54)
- `StaffAccountPatchRequest#fullName`: [L58](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L58)
- `StaffAccountPatchRequest#hasPhone`: [L62](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L62)
- `StaffAccountPatchRequest#phone`: [L66](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L66)
- `StaffAccountPatchRequest#hasEmail`: [L70](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L70)
- `StaffAccountPatchRequest#email`: [L74](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L74)
- `StaffAccountPatchRequest#hasBirthDate`: [L78](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L78)
- `StaffAccountPatchRequest#birthDate`: [L82](../../src/main/java/com/scms/backend/manager/StaffAccountPatchRequest.java#L82)

### [StaffAccountResponse](../../src/main/java/com/scms/backend/manager/StaffAccountResponse.java#L8)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [StaffAccountValidationException](../../src/main/java/com/scms/backend/manager/StaffAccountValidationException.java#L3)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó

- `StaffAccountValidationException#getField`: [L12](../../src/main/java/com/scms/backend/manager/StaffAccountValidationException.java#L12)

### [StaffStatusChangeRequest](../../src/main/java/com/scms/backend/manager/StaffStatusChangeRequest.java#L5)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [StaffStatusConflictException](../../src/main/java/com/scms/backend/manager/StaffStatusConflictException.java#L3)

Package `com.scms.backend.manager`. Phụ lục đã đối chiếu trước đó


### [ProfileImageStorage](../../src/main/java/com/scms/backend/member/ProfileImageStorage.java#L5)

Package `com.scms.backend.member`. Phụ lục đã đối chiếu trước đó

- `ProfileImageStorage#store`: [L7](../../src/main/java/com/scms/backend/member/ProfileImageStorage.java#L7)
- `ProfileImageStorage#delete`: [L9](../../src/main/java/com/scms/backend/member/ProfileImageStorage.java#L9)

### [ActiveMembershipExistsException](../../src/main/java/com/scms/backend/membership/ActiveMembershipExistsException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [FulfillmentMode](../../src/main/java/com/scms/backend/membership/FulfillmentMode.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [InvalidPlanCodeException](../../src/main/java/com/scms/backend/membership/InvalidPlanCodeException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MemberNotActiveException](../../src/main/java/com/scms/backend/membership/MemberNotActiveException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [Membership](../../src/main/java/com/scms/backend/membership/Membership.java#L18)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `Membership#expire`: [L91](../../src/main/java/com/scms/backend/membership/Membership.java#L91)
- `Membership#activate`: [L97](../../src/main/java/com/scms/backend/membership/Membership.java#L97)
- `Membership#terminate`: [L105](../../src/main/java/com/scms/backend/membership/Membership.java#L105)
- `Membership#cancelReserved`: [L116](../../src/main/java/com/scms/backend/membership/Membership.java#L116)
- `Membership#extendTo`: [L122](../../src/main/java/com/scms/backend/membership/Membership.java#L122)
- `Membership#getStatus`: [L129](../../src/main/java/com/scms/backend/membership/Membership.java#L129)
- `Membership#getId`: [L133](../../src/main/java/com/scms/backend/membership/Membership.java#L133)
- `Membership#getMemberAccountId`: [L137](../../src/main/java/com/scms/backend/membership/Membership.java#L137)
- `Membership#getStartsAt`: [L141](../../src/main/java/com/scms/backend/membership/Membership.java#L141)
- `Membership#getEndsAt`: [L145](../../src/main/java/com/scms/backend/membership/Membership.java#L145)
- `Membership#getPriceAmountSnapshot`: [L149](../../src/main/java/com/scms/backend/membership/Membership.java#L149)
- `Membership#getDurationDaysSnapshot`: [L153](../../src/main/java/com/scms/backend/membership/Membership.java#L153)

### [MembershipBenefit](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L14)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipBenefit#getCode`: [L52](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L52)
- `MembershipBenefit#getDisplayName`: [L56](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L56)
- `MembershipBenefit#getDescription`: [L60](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L60)
- `MembershipBenefit#getDisplayOrder`: [L64](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L64)
- `MembershipBenefit#getGroup`: [L68](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L68)
- `MembershipBenefit#getStatus`: [L72](../../src/main/java/com/scms/backend/membership/MembershipBenefit.java#L72)

### [MembershipBenefitGroup](../../src/main/java/com/scms/backend/membership/MembershipBenefitGroup.java#L9)

Package `com.scms.backend.membership`. Tham chiếu kiểu từ MembershipBenefit, MembershipBenefitResponse

- `MembershipBenefitGroup#getCode`: [L35](../../src/main/java/com/scms/backend/membership/MembershipBenefitGroup.java#L35)
- `MembershipBenefitGroup#getDisplayName`: [L39](../../src/main/java/com/scms/backend/membership/MembershipBenefitGroup.java#L39)
- `MembershipBenefitGroup#getDescription`: [L43](../../src/main/java/com/scms/backend/membership/MembershipBenefitGroup.java#L43)
- `MembershipBenefitGroup#getDisplayOrder`: [L47](../../src/main/java/com/scms/backend/membership/MembershipBenefitGroup.java#L47)

### [MembershipBenefitStatus](../../src/main/java/com/scms/backend/membership/MembershipBenefitStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipFreezeException](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L5)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipFreezeException#notFound`: [L17](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L17)
- `MembershipFreezeException#alreadyPending`: [L21](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L21)
- `MembershipFreezeException#invalidPeriod`: [L25](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L25)
- `MembershipFreezeException#validation`: [L29](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L29)
- `MembershipFreezeException#paymentRequired`: [L33](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L33)
- `MembershipFreezeException#invalidTransition`: [L37](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L37)
- `MembershipFreezeException#changed`: [L41](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L41)
- `MembershipFreezeException#notActive`: [L45](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L45)
- `MembershipFreezeException#conflict`: [L49](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L49)
- `MembershipFreezeException#value`: [L53](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L53)
- `MembershipFreezeException#getStatus`: [L57](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L57)
- `MembershipFreezeException#getCode`: [L61](../../src/main/java/com/scms/backend/membership/MembershipFreezeException.java#L61)

### [MembershipFreezeRequest](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L18)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipFreezeRequest#approve`: [L86](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L86)
- `MembershipFreezeRequest#reject`: [L95](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L95)
- `MembershipFreezeRequest#withdraw`: [L103](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L103)
- `MembershipFreezeRequest#schedule`: [L109](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L109)
- `MembershipFreezeRequest#activate`: [L115](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L115)
- `MembershipFreezeRequest#complete`: [L121](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L121)
- `MembershipFreezeRequest#obsolete`: [L127](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L127)
- `MembershipFreezeRequest#getId`: [L133](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L133)
- `MembershipFreezeRequest#getMembershipId`: [L137](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L137)
- `MembershipFreezeRequest#getMemberAccountId`: [L141](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L141)
- `MembershipFreezeRequest#getRequesterAccountId`: [L145](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L145)
- `MembershipFreezeRequest#getApproverAccountId`: [L149](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L149)
- `MembershipFreezeRequest#getStartDate`: [L153](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L153)
- `MembershipFreezeRequest#getEndDate`: [L157](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L157)
- `MembershipFreezeRequest#getReason`: [L161](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L161)
- `MembershipFreezeRequest#getFeeAmount`: [L165](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L165)
- `MembershipFreezeRequest#getStatus`: [L169](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L169)
- `MembershipFreezeRequest#getApprovedAt`: [L173](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L173)
- `MembershipFreezeRequest#getRejectedAt`: [L177](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L177)
- `MembershipFreezeRequest#getCreatedAt`: [L181](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L181)
- `MembershipFreezeRequest#getUpdatedAt`: [L185](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L185)
- `MembershipFreezeRequest#getVersion`: [L189](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequest.java#L189)

### [MembershipFreezeRequestStatus](../../src/main/java/com/scms/backend/membership/MembershipFreezeRequestStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipHistoryResponse](../../src/main/java/com/scms/backend/membership/MembershipHistoryResponse.java#L10)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipOffer](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L19)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipOffer#update`: [L79](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L79)
- `MembershipOffer#patch`: [L86](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L86)
- `MembershipOffer#changeStatus`: [L97](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L97)
- `MembershipOffer#getId`: [L101](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L101)
- `MembershipOffer#getPlan`: [L105](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L105)
- `MembershipOffer#getName`: [L109](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L109)
- `MembershipOffer#getDescription`: [L113](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L113)
- `MembershipOffer#getPriceAmount`: [L117](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L117)
- `MembershipOffer#getCurrencyCode`: [L121](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L121)
- `MembershipOffer#getCreatedByAccountId`: [L125](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L125)
- `MembershipOffer#getDurationDays`: [L129](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L129)
- `MembershipOffer#getStatus`: [L133](../../src/main/java/com/scms/backend/membership/MembershipOffer.java#L133)

### [MembershipOfferNotFoundException](../../src/main/java/com/scms/backend/membership/MembershipOfferNotFoundException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipOfferStatus](../../src/main/java/com/scms/backend/membership/MembershipOfferStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipOfferUnavailableException](../../src/main/java/com/scms/backend/membership/MembershipOfferUnavailableException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipOfferValidationException](../../src/main/java/com/scms/backend/membership/MembershipOfferValidationException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipOrder](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L19)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipOrder#getId`: [L108](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L108)
- `MembershipOrder#getOrderNumber`: [L112](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L112)
- `MembershipOrder#getMemberAccountId`: [L116](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L116)
- `MembershipOrder#getCreatedByAccountId`: [L120](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L120)
- `MembershipOrder#getOfferId`: [L124](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L124)
- `MembershipOrder#getOfferNameSnapshot`: [L128](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L128)
- `MembershipOrder#getPlanCodeSnapshot`: [L132](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L132)
- `MembershipOrder#getPriceAmountSnapshot`: [L136](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L136)
- `MembershipOrder#getCurrencyCodeSnapshot`: [L140](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L140)
- `MembershipOrder#getDurationDaysSnapshot`: [L144](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L144)
- `MembershipOrder#getPaymentMethod`: [L148](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L148)
- `MembershipOrder#getStatus`: [L152](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L152)
- `MembershipOrder#getFulfillmentMode`: [L156](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L156)
- `MembershipOrder#getExpiresAt`: [L160](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L160)
- `MembershipOrder#getPaidAt`: [L164](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L164)
- `MembershipOrder#getCreatedAt`: [L168](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L168)
- `MembershipOrder#getUpdatedAt`: [L172](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L172)
- `MembershipOrder#getVersion`: [L176](../../src/main/java/com/scms/backend/membership/MembershipOrder.java#L176)

### [MembershipOrderMemberNotFoundException](../../src/main/java/com/scms/backend/membership/MembershipOrderMemberNotFoundException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipOrderStatus](../../src/main/java/com/scms/backend/membership/MembershipOrderStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipPlan](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L18)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipPlan#getPlanCode`: [L65](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L65)
- `MembershipPlan#getDisplayName`: [L69](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L69)
- `MembershipPlan#supportsBooking`: [L73](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L73)
- `MembershipPlan#supportsPersonalCoaching`: [L77](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L77)
- `MembershipPlan#getBenefits`: [L81](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L81)
- `MembershipPlan#getStatus`: [L85](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L85)
- `MembershipPlan#getCreatedAt`: [L89](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L89)
- `MembershipPlan#getUpdatedAt`: [L93](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L93)
- `MembershipPlan#getVersion`: [L97](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L97)
- `MembershipPlan#update`: [L101](../../src/main/java/com/scms/backend/membership/MembershipPlan.java#L101)

### [MembershipPlanCode](../../src/main/java/com/scms/backend/membership/MembershipPlanCode.java#L3)

Package `com.scms.backend.membership`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MembershipPlanConflictException](../../src/main/java/com/scms/backend/membership/MembershipPlanConflictException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipPlanNotFoundException](../../src/main/java/com/scms/backend/membership/MembershipPlanNotFoundException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipPlanStatus](../../src/main/java/com/scms/backend/membership/MembershipPlanStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipPlanValidationException](../../src/main/java/com/scms/backend/membership/MembershipPlanValidationException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipPlanValidationException#getField`: [L16](../../src/main/java/com/scms/backend/membership/MembershipPlanValidationException.java#L16)

### [MembershipRenewalAlreadyExistsException](../../src/main/java/com/scms/backend/membership/MembershipRenewalAlreadyExistsException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipRepository](../../src/main/java/com/scms/backend/membership/MembershipRepository.java#L13)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipRepository#findByIdForUpdate`: [L15](../../src/main/java/com/scms/backend/membership/MembershipRepository.java#L15)
- `MembershipRepository#existsByMemberAccountIdAndStatus`: [L19](../../src/main/java/com/scms/backend/membership/MembershipRepository.java#L19)
- `MembershipRepository#findByMemberAccountIdAndStatusForUpdate`: [L21](../../src/main/java/com/scms/backend/membership/MembershipRepository.java#L21)
- `MembershipRepository#findLatestExpiredByMemberAccountIdForUpdate`: [L25](../../src/main/java/com/scms/backend/membership/MembershipRepository.java#L25)
- `MembershipRepository#findDueMemberAccountIds`: [L28](../../src/main/java/com/scms/backend/membership/MembershipRepository.java#L28)

### [MembershipStatus](../../src/main/java/com/scms/backend/membership/MembershipStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipTerminationException](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L5)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipTerminationException#notActive`: [L17](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L17)
- `MembershipTerminationException#alreadyPending`: [L21](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L21)
- `MembershipTerminationException#notFound`: [L25](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L25)
- `MembershipTerminationException#invalidTransition`: [L29](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L29)
- `MembershipTerminationException#changed`: [L33](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L33)
- `MembershipTerminationException#conflict`: [L37](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L37)
- `MembershipTerminationException#getStatus`: [L41](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L41)
- `MembershipTerminationException#getCode`: [L45](../../src/main/java/com/scms/backend/membership/MembershipTerminationException.java#L45)

### [MembershipTerminationMode](../../src/main/java/com/scms/backend/membership/MembershipTerminationMode.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipTerminationRequest](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L16)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `MembershipTerminationRequest#approve`: [L84](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L84)
- `MembershipTerminationRequest#reject`: [L92](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L92)
- `MembershipTerminationRequest#withdraw`: [L99](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L99)
- `MembershipTerminationRequest#execute`: [L105](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L105)
- `MembershipTerminationRequest#obsolete`: [L112](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L112)
- `MembershipTerminationRequest#ensurePending`: [L119](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L119)
- `MembershipTerminationRequest#getId`: [L125](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L125)
- `MembershipTerminationRequest#getMembershipId`: [L129](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L129)
- `MembershipTerminationRequest#getMemberAccountId`: [L133](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L133)
- `MembershipTerminationRequest#getRequesterAccountId`: [L137](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L137)
- `MembershipTerminationRequest#getApproverAccountId`: [L141](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L141)
- `MembershipTerminationRequest#getMode`: [L145](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L145)
- `MembershipTerminationRequest#getStatus`: [L149](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L149)
- `MembershipTerminationRequest#getReason`: [L153](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L153)
- `MembershipTerminationRequest#getEffectiveAt`: [L157](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L157)
- `MembershipTerminationRequest#getApprovedAt`: [L161](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L161)
- `MembershipTerminationRequest#getRejectedAt`: [L165](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L165)
- `MembershipTerminationRequest#getWithdrawnAt`: [L169](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L169)
- `MembershipTerminationRequest#getCreatedAt`: [L173](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L173)
- `MembershipTerminationRequest#getUpdatedAt`: [L177](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L177)
- `MembershipTerminationRequest#getVersion`: [L181](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequest.java#L181)

### [MembershipTerminationRequestStatus](../../src/main/java/com/scms/backend/membership/MembershipTerminationRequestStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [PaymentException](../../src/main/java/com/scms/backend/membership/PaymentException.java#L5)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `PaymentException#status`: [L17](../../src/main/java/com/scms/backend/membership/PaymentException.java#L17)
- `PaymentException#code`: [L21](../../src/main/java/com/scms/backend/membership/PaymentException.java#L21)
- `PaymentException#validation`: [L25](../../src/main/java/com/scms/backend/membership/PaymentException.java#L25)
- `PaymentException#unauthorized`: [L29](../../src/main/java/com/scms/backend/membership/PaymentException.java#L29)
- `PaymentException#notFound`: [L33](../../src/main/java/com/scms/backend/membership/PaymentException.java#L33)
- `PaymentException#conflict`: [L37](../../src/main/java/com/scms/backend/membership/PaymentException.java#L37)
- `PaymentException#forbidden`: [L41](../../src/main/java/com/scms/backend/membership/PaymentException.java#L41)

### [PaymentMethod](../../src/main/java/com/scms/backend/membership/PaymentMethod.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [PendingMembershipOrderExistsException](../../src/main/java/com/scms/backend/membership/PendingMembershipOrderExistsException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [ReconciliationQueueStatus](../../src/main/java/com/scms/backend/membership/ReconciliationQueueStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [RefundException](../../src/main/java/com/scms/backend/membership/RefundException.java#L5)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `RefundException#validation`: [L17](../../src/main/java/com/scms/backend/membership/RefundException.java#L17)
- `RefundException#notFound`: [L21](../../src/main/java/com/scms/backend/membership/RefundException.java#L21)
- `RefundException#notEligible`: [L25](../../src/main/java/com/scms/backend/membership/RefundException.java#L25)
- `RefundException#exists`: [L29](../../src/main/java/com/scms/backend/membership/RefundException.java#L29)
- `RefundException#amountLimit`: [L33](../../src/main/java/com/scms/backend/membership/RefundException.java#L33)
- `RefundException#transition`: [L37](../../src/main/java/com/scms/backend/membership/RefundException.java#L37)
- `RefundException#changed`: [L41](../../src/main/java/com/scms/backend/membership/RefundException.java#L41)
- `RefundException#unsupportedPayment`: [L45](../../src/main/java/com/scms/backend/membership/RefundException.java#L45)
- `RefundException#threshold`: [L49](../../src/main/java/com/scms/backend/membership/RefundException.java#L49)
- `RefundException#value`: [L53](../../src/main/java/com/scms/backend/membership/RefundException.java#L53)
- `RefundException#getStatus`: [L57](../../src/main/java/com/scms/backend/membership/RefundException.java#L57)
- `RefundException#getCode`: [L61](../../src/main/java/com/scms/backend/membership/RefundException.java#L61)

### [RefundRequest](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L18)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `RefundRequest#firstApprove`: [L142](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L142)
- `RefundRequest#cashDisburse`: [L156](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L156)
- `RefundRequest#complete`: [L167](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L167)
- `RefundRequest#reject`: [L175](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L175)
- `RefundRequest#getId`: [L185](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L185)
- `RefundRequest#getPaymentId`: [L189](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L189)
- `RefundRequest#getOrderId`: [L193](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L193)
- `RefundRequest#getMembershipId`: [L197](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L197)
- `RefundRequest#getReceiptId`: [L201](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L201)
- `RefundRequest#getMemberAccountId`: [L205](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L205)
- `RefundRequest#getRequesterAccountId`: [L209](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L209)
- `RefundRequest#getFirstApproverAccountId`: [L213](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L213)
- `RefundRequest#getFinalApproverAccountId`: [L217](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L217)
- `RefundRequest#getDisbursedByAccountId`: [L221](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L221)
- `RefundRequest#getType`: [L225](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L225)
- `RefundRequest#getStatus`: [L229](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L229)
- `RefundRequest#getRequestedAmount`: [L233](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L233)
- `RefundRequest#getSuggestedAmount`: [L237](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L237)
- `RefundRequest#getApprovedAmount`: [L241](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L241)
- `RefundRequest#getRemainingDaysSnapshot`: [L245](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L245)
- `RefundRequest#getCalculationDate`: [L249](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L249)
- `RefundRequest#getReason`: [L253](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L253)
- `RefundRequest#getDisbursementEvidence`: [L257](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L257)
- `RefundRequest#getRejectionReason`: [L261](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L261)
- `RefundRequest#getIdempotencyKey`: [L265](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L265)
- `RefundRequest#getRequestedAt`: [L269](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L269)
- `RefundRequest#getFirstApprovedAt`: [L273](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L273)
- `RefundRequest#getCashDisbursedAt`: [L277](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L277)
- `RefundRequest#getCompletedAt`: [L281](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L281)
- `RefundRequest#getRejectedAt`: [L285](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L285)
- `RefundRequest#getVersion`: [L289](../../src/main/java/com/scms/backend/membership/RefundRequest.java#L289)

### [RefundRequestStatus](../../src/main/java/com/scms/backend/membership/RefundRequestStatus.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [RefundType](../../src/main/java/com/scms/backend/membership/RefundType.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [ReportingValidationException](../../src/main/java/com/scms/backend/membership/ReportingValidationException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó

- `ReportingValidationException#getField`: [L12](../../src/main/java/com/scms/backend/membership/ReportingValidationException.java#L12)

### [SepayValidationException](../../src/main/java/com/scms/backend/membership/SepayValidationException.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [SepayWebhookOutcome](../../src/main/java/com/scms/backend/membership/SepayWebhookOutcome.java#L3)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [StaffMembershipResponse](../../src/main/java/com/scms/backend/membership/StaffMembershipResponse.java#L9)

Package `com.scms.backend.membership`. Phụ lục đã đối chiếu trước đó


### [MembershipFreezeCashPaymentResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeCashPaymentResponse.java#L8)

Package `com.scms.backend.membership.freeze`. Phụ lục đã đối chiếu trước đó


### [MembershipFreezeCreateRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeCreateRequest.java#L5)

Package `com.scms.backend.membership.freeze`. Phụ lục đã đối chiếu trước đó


### [MembershipFreezeDecisionRequest](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeDecisionRequest.java#L3)

Package `com.scms.backend.membership.freeze`. Phụ lục đã đối chiếu trước đó


### [MembershipFreezeRequestPageResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L6)

Package `com.scms.backend.membership.freeze`. Phụ lục đã đối chiếu trước đó

- `MembershipFreezeRequestPageResponse#from`: [L8](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L8)
- `MembershipFreezeRequestPageResponse#fromResponses`: [L12](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L12)
- `MembershipFreezeRequestPageResponse#empty`: [L16](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestPageResponse.java#L16)

### [MembershipFreezeRequestRepository](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L18)

Package `com.scms.backend.membership.freeze`. Phụ lục đã đối chiếu trước đó

- `MembershipFreezeRequestRepository#findByIdForUpdate`: [L20](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L20)
- `MembershipFreezeRequestRepository#findByMemberAccountIdOrderByCreatedAtDesc`: [L24](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L24)
- `MembershipFreezeRequestRepository#findAllByOrderByCreatedAtDesc`: [L26](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L26)
- `MembershipFreezeRequestRepository#existsByMembershipIdAndStatusIn`: [L28](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L28)
- `MembershipFreezeRequestRepository#findDueUnpaidIds`: [L30](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L30)
- `MembershipFreezeRequestRepository#findDueScheduledIds`: [L37](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L37)
- `MembershipFreezeRequestRepository#findDueActiveIds`: [L46](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L46)
- `MembershipFreezeRequestRepository#findActiveIdsByMemberAccountId`: [L55](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestRepository.java#L55)

### [MembershipFreezeRequestResponse](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L10)

Package `com.scms.backend.membership.freeze`. Phụ lục đã đối chiếu trước đó

- `MembershipFreezeRequestResponse#from`: [L12](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L12), [L16](../../src/main/java/com/scms/backend/membership/freeze/MembershipFreezeRequestResponse.java#L16)

### [MembershipOfferAdminController](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminController.java#L18)

Package `com.scms.backend.membership.offer`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipOfferAdminController#all`: [L28](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminController.java#L28)
- `MembershipOfferAdminController#get`: [L33](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminController.java#L33)
- `MembershipOfferAdminController#create`: [L38](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminController.java#L38)
- `MembershipOfferAdminController#update`: [L44](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminController.java#L44)

### [MembershipOfferAdminDetailResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminDetailResponse.java#L10)

Package `com.scms.backend.membership.offer`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipOfferAdminDetailResponse#from`: [L12](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminDetailResponse.java#L12)

### [MembershipOfferAdminRequest](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminRequest.java#L6)

Package `com.scms.backend.membership.offer`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MembershipOfferAdminResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminResponse.java#L8)

Package `com.scms.backend.membership.offer`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipOfferAdminResponse#from`: [L10](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminResponse.java#L10)

### [MembershipOfferAdminService](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L26)

Package `com.scms.backend.membership.offer`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipOfferAdminService#create`: [L48](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L48)
- `MembershipOfferAdminService#all`: [L60](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L60)
- `MembershipOfferAdminService#get`: [L66](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L66)
- `MembershipOfferAdminService#update`: [L72](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L72)
- `MembershipOfferAdminService#snapshot`: [L88](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L88)
- `MembershipOfferAdminService#ensureManager`: [L101](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L101)
- `MembershipOfferAdminService#validate`: [L106](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L106)
- `MembershipOfferAdminService#validatePatch`: [L111](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L111)
- `MembershipOfferAdminService#trim`: [L126](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L126)
- `MembershipOfferAdminService#normalizePlanCode`: [L130](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L130)
- `MembershipOfferAdminService#validAmount`: [L134](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferAdminService.java#L134)

### [MembershipOfferPatchRequest](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferPatchRequest.java#L6)

Package `com.scms.backend.membership.offer`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MembershipOfferRepository](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L12)

Package `com.scms.backend.membership.offer`. Phụ lục đã đối chiếu trước đó

- `MembershipOfferRepository#findAllWithPlanAndBenefits`: [L14](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L14)
- `MembershipOfferRepository#findByIdWithPlanAndBenefits`: [L23](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L23)
- `MembershipOfferRepository#findAllByStatus`: [L32](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L32)
- `MembershipOfferRepository#findAllByStatusAndPlanCode`: [L42](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L42)
- `MembershipOfferRepository#findByIdAndStatus`: [L52](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L52)
- `MembershipOfferRepository#findByIdAndStatusAndActivePlan`: [L59](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L59)
- `MembershipOfferRepository#findSellablePlanCodeForUpdate`: [L68](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferRepository.java#L68)

### [MembershipOfferResponse](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferResponse.java#L11)

Package `com.scms.backend.membership.offer`. Phụ lục đã đối chiếu trước đó

- `MembershipOfferResponse#from`: [L13](../../src/main/java/com/scms/backend/membership/offer/MembershipOfferResponse.java#L13)

### [MembershipOrderCancellationRequest](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCancellationRequest.java#L3)

Package `com.scms.backend.membership.order`. Phụ lục đã đối chiếu trước đó


### [MembershipOrderCancellationResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCancellationResponse.java#L6)

Package `com.scms.backend.membership.order`. Phụ lục đã đối chiếu trước đó


### [MembershipOrderConstraintDetector](../../src/main/java/com/scms/backend/membership/order/MembershipOrderConstraintDetector.java#L5)

Package `com.scms.backend.membership.order`. Phụ lục đã đối chiếu trước đó

- `MembershipOrderConstraintDetector#isPendingMemberConstraint`: [L12](../../src/main/java/com/scms/backend/membership/order/MembershipOrderConstraintDetector.java#L12)

### [MembershipOrderCreateRequest](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCreateRequest.java#L7)

Package `com.scms.backend.membership.order`. Phụ lục đã đối chiếu trước đó

- `MembershipOrderCreateRequest#rejectUnknownField`: [L9](../../src/main/java/com/scms/backend/membership/order/MembershipOrderCreateRequest.java#L9)

### [MembershipOrderRepository](../../src/main/java/com/scms/backend/membership/order/MembershipOrderRepository.java#L12)

Package `com.scms.backend.membership.order`. Phụ lục đã đối chiếu trước đó

- `MembershipOrderRepository#existsByMemberAccountIdAndStatus`: [L14](../../src/main/java/com/scms/backend/membership/order/MembershipOrderRepository.java#L14)
- `MembershipOrderRepository#findByMemberAccountIdAndStatus`: [L16](../../src/main/java/com/scms/backend/membership/order/MembershipOrderRepository.java#L16)
- `MembershipOrderRepository#existsByPlanCodeSnapshot`: [L18](../../src/main/java/com/scms/backend/membership/order/MembershipOrderRepository.java#L18)
- `MembershipOrderRepository#findUsedPlanCodes`: [L21](../../src/main/java/com/scms/backend/membership/order/MembershipOrderRepository.java#L21)

### [MembershipOrderResponse](../../src/main/java/com/scms/backend/membership/order/MembershipOrderResponse.java#L11)

Package `com.scms.backend.membership.order`. Phụ lục đã đối chiếu trước đó


### [CashPaymentRequest](../../src/main/java/com/scms/backend/membership/payment/CashPaymentRequest.java#L6)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [CourseReconciliationModels](../../src/main/java/com/scms/backend/membership/payment/CourseReconciliationModels.java#L10)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [PaymentActionRequest](../../src/main/java/com/scms/backend/membership/payment/PaymentActionRequest.java#L5)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [PaymentResultResponse](../../src/main/java/com/scms/backend/membership/payment/PaymentResultResponse.java#L8)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [ReceiptResponse](../../src/main/java/com/scms/backend/membership/payment/ReceiptResponse.java#L8)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [ReconciliationQueueItem](../../src/main/java/com/scms/backend/membership/payment/ReconciliationQueueItem.java#L8)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [ReconciliationQueuePageResponse](../../src/main/java/com/scms/backend/membership/payment/ReconciliationQueuePageResponse.java#L5)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [SepayPaymentResponse](../../src/main/java/com/scms/backend/membership/payment/SepayPaymentResponse.java#L7)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [SepayWebhookRequest](../../src/main/java/com/scms/backend/membership/payment/SepayWebhookRequest.java#L7)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [SepayWebhookResponse](../../src/main/java/com/scms/backend/membership/payment/SepayWebhookResponse.java#L7)

Package `com.scms.backend.membership.payment`. Phụ lục đã đối chiếu trước đó


### [MembershipBenefitRepository](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitRepository.java#L10)

Package `com.scms.backend.membership.plan`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipBenefitRepository#findAllByStatusOrderByCode`: [L12](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitRepository.java#L12)
- `MembershipBenefitRepository#findAllByOrderByCode`: [L14](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitRepository.java#L14)
- `MembershipBenefitRepository#findAllByCodeIn`: [L21](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitRepository.java#L21)

### [MembershipBenefitResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitResponse.java#L9)

Package `com.scms.backend.membership.plan`. Phụ lục đã đối chiếu trước đó

- `MembershipBenefitResponse#from`: [L15](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitResponse.java#L15)
- `MembershipBenefitResponse#displayOrdering`: [L20](../../src/main/java/com/scms/backend/membership/plan/MembershipBenefitResponse.java#L20)

### [MembershipPlanAdminController](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L17)

Package `com.scms.backend.membership.plan`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipPlanAdminController#benefits`: [L27](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L27)
- `MembershipPlanAdminController#list`: [L32](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L32)
- `MembershipPlanAdminController#get`: [L37](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L37)
- `MembershipPlanAdminController#create`: [L42](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L42)
- `MembershipPlanAdminController#update`: [L47](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L47)
- `MembershipPlanAdminController#accountId`: [L52](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminController.java#L52)

### [MembershipPlanAdminService](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L29)

Package `com.scms.backend.membership.plan`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipPlanAdminService#catalog`: [L52](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L52)
- `MembershipPlanAdminService#list`: [L58](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L58)
- `MembershipPlanAdminService#get`: [L67](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L67)
- `MembershipPlanAdminService#create`: [L74](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L74)
- `MembershipPlanAdminService#update`: [L93](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L93)
- `MembershipPlanAdminService#find`: [L123](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L123)
- `MembershipPlanAdminService#resolveBenefits`: [L127](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L127)
- `MembershipPlanAdminService#normalizeBenefitCodes`: [L140](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L140)
- `MembershipPlanAdminService#normalizeCode`: [L149](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L149)
- `MembershipPlanAdminService#normalizeBenefitCode`: [L155](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L155)
- `MembershipPlanAdminService#normalizeDisplayName`: [L161](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L161)
- `MembershipPlanAdminService#invalid`: [L167](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L167)
- `MembershipPlanAdminService#ensureActiveManager`: [L171](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L171)
- `MembershipPlanAdminService#snapshot`: [L177](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L177)
- `MembershipPlanAdminService#codes`: [L186](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L186)
- `MembershipPlanAdminService#auditId`: [L190](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanAdminService.java#L190)

### [MembershipPlanCreateRequest](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanCreateRequest.java#L6)

Package `com.scms.backend.membership.plan`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MembershipPlanPatchRequest](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanPatchRequest.java#L6)

Package `com.scms.backend.membership.plan`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [MembershipPlanRepository](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanRepository.java#L12)

Package `com.scms.backend.membership.plan`. Phụ lục đã đối chiếu trước đó

- `MembershipPlanRepository#findAllWithBenefits`: [L14](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanRepository.java#L14)
- `MembershipPlanRepository#findByIdWithBenefits`: [L17](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanRepository.java#L17)
- `MembershipPlanRepository#findByPlanCodeForUpdate`: [L20](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanRepository.java#L20)
- `MembershipPlanRepository#findStatusByPlanCodeForUpdate`: [L24](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanRepository.java#L24)

### [MembershipPlanResponse](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanResponse.java#L10)

Package `com.scms.backend.membership.plan`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MembershipPlanResponse#from`: [L12](../../src/main/java/com/scms/backend/membership/plan/MembershipPlanResponse.java#L12)

### [RefundActionRequest](../../src/main/java/com/scms/backend/membership/refund/RefundActionRequest.java#L3)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó


### [RefundCashDisbursementRequest](../../src/main/java/com/scms/backend/membership/refund/RefundCashDisbursementRequest.java#L3)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó


### [RefundEligibleSourceResponse](../../src/main/java/com/scms/backend/membership/refund/RefundEligibleSourceResponse.java#L9)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó


### [RefundFirstApprovalRequest](../../src/main/java/com/scms/backend/membership/refund/RefundFirstApprovalRequest.java#L5)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó


### [RefundRequestCreateRequest](../../src/main/java/com/scms/backend/membership/refund/RefundRequestCreateRequest.java#L6)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó


### [RefundRequestPageResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestPageResponse.java#L5)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó

- `RefundRequestPageResponse#from`: [L7](../../src/main/java/com/scms/backend/membership/refund/RefundRequestPageResponse.java#L7)

### [RefundRequestRepository](../../src/main/java/com/scms/backend/membership/refund/RefundRequestRepository.java#L17)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó

- `RefundRequestRepository#findByIdForUpdate`: [L19](../../src/main/java/com/scms/backend/membership/refund/RefundRequestRepository.java#L19)
- `RefundRequestRepository#findByRequesterAccountIdAndIdempotencyKey`: [L23](../../src/main/java/com/scms/backend/membership/refund/RefundRequestRepository.java#L23)
- `RefundRequestRepository#existsByMembershipIdAndTypeAndStatusIn`: [L25](../../src/main/java/com/scms/backend/membership/refund/RefundRequestRepository.java#L25)
- `RefundRequestRepository#findAllByOrderByRequestedAtDescIdDesc`: [L27](../../src/main/java/com/scms/backend/membership/refund/RefundRequestRepository.java#L27)

### [RefundRequestResponse](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L13)

Package `com.scms.backend.membership.refund`. Phụ lục đã đối chiếu trước đó

- `RefundRequestResponse#from`: [L15](../../src/main/java/com/scms/backend/membership/refund/RefundRequestResponse.java#L15)

### [AttendanceReportResponse](../../src/main/java/com/scms/backend/membership/reporting/AttendanceReportResponse.java#L6)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [BookingReportResponse](../../src/main/java/com/scms/backend/membership/reporting/BookingReportResponse.java#L5)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CenterVisitReportResponse](../../src/main/java/com/scms/backend/membership/reporting/CenterVisitReportResponse.java#L5)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ClassUtilizationReportResponse](../../src/main/java/com/scms/backend/membership/reporting/ClassUtilizationReportResponse.java#L6)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseRunReportItem](../../src/main/java/com/scms/backend/membership/reporting/CourseRunReportItem.java#L7)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CourseRunReportPageResponse](../../src/main/java/com/scms/backend/membership/reporting/CourseRunReportPageResponse.java#L5)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ManagerDashboardResponse](../../src/main/java/com/scms/backend/membership/reporting/ManagerDashboardResponse.java#L5)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ManagerReportingController](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L17)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerReportingController#dashboard`: [L27](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L27)
- `ManagerReportingController#members`: [L33](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L33)
- `ManagerReportingController#bookings`: [L38](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L38)
- `ManagerReportingController#attendance`: [L43](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L43)
- `ManagerReportingController#centerVisits`: [L48](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L48)
- `ManagerReportingController#classUtilization`: [L53](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L53)
- `ManagerReportingController#courseRuns`: [L58](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L58)
- `ManagerReportingController#id`: [L64](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingController.java#L64)

### [ManagerReportingRepository](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L16)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerReportingRepository#courseRunWhere`: [L27](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L27)
- `ManagerReportingRepository#courseRunReportSql`: [L48](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L48)
- `ManagerReportingRepository#integer`: [L136](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L136)
- `ManagerReportingRepository#membersData`: [L140](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L140)
- `ManagerReportingRepository#bookingsData`: [L150](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L150)
- `ManagerReportingRepository#attendanceData`: [L156](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L156)
- `ManagerReportingRepository#visitsData`: [L166](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L166)
- `ManagerReportingRepository#utilizationData`: [L172](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L172)
- `ManagerReportingRepository#range`: [L202](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L202)
- `ManagerReportingRepository#args`: [L206](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L206)
- `ManagerReportingRepository#number`: [L215](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L215)
- `ManagerReportingRepository#rate`: [L219](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L219)
- `ManagerReportingRepository#courseRunRows`: [L223](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingRepository.java#L223)

### [ManagerReportingService](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L21)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerReportingService#dashboard`: [L46](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L46)
- `ManagerReportingService#members`: [L52](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L52)
- `ManagerReportingService#bookings`: [L58](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L58)
- `ManagerReportingService#attendance`: [L64](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L64)
- `ManagerReportingService#centerVisits`: [L70](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L70)
- `ManagerReportingService#classUtilization`: [L76](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L76)
- `ManagerReportingService#courseRuns`: [L82](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L82)
- `ManagerReportingService#validateCourseRunReport`: [L90](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L90)
- `ManagerReportingService#integer`: [L104](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L104)
- `ManagerReportingService#membersData`: [L108](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L108)
- `ManagerReportingService#bookingsData`: [L112](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L112)
- `ManagerReportingService#attendanceData`: [L116](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L116)
- `ManagerReportingService#visitsData`: [L120](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L120)
- `ManagerReportingService#utilizationData`: [L124](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L124)
- `ManagerReportingService#validate`: [L128](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L128)
- `ManagerReportingService#courseRunRows`: [L137](../../src/main/java/com/scms/backend/membership/reporting/ManagerReportingService.java#L137)

### [MemberReportResponse](../../src/main/java/com/scms/backend/membership/reporting/MemberReportResponse.java#L5)

Package `com.scms.backend.membership.reporting`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RevenueReportResponse](../../src/main/java/com/scms/backend/membership/reporting/RevenueReportResponse.java#L6)

Package `com.scms.backend.membership.reporting`. Phụ lục đã đối chiếu trước đó


### [MembershipTerminationCreateRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationCreateRequest.java#L5)

Package `com.scms.backend.membership.termination`. Phụ lục đã đối chiếu trước đó


### [MembershipTerminationDecisionRequest](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationDecisionRequest.java#L3)

Package `com.scms.backend.membership.termination`. Phụ lục đã đối chiếu trước đó


### [MembershipTerminationRequestPageResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L6)

Package `com.scms.backend.membership.termination`. Phụ lục đã đối chiếu trước đó

- `MembershipTerminationRequestPageResponse#from`: [L8](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L8), [L16](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L16)
- `MembershipTerminationRequestPageResponse#fromResponses`: [L12](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L12)
- `MembershipTerminationRequestPageResponse#empty`: [L20](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestPageResponse.java#L20)

### [MembershipTerminationRequestRepository](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestRepository.java#L14)

Package `com.scms.backend.membership.termination`. Phụ lục đã đối chiếu trước đó

- `MembershipTerminationRequestRepository#findByIdForUpdate`: [L16](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestRepository.java#L16)
- `MembershipTerminationRequestRepository#findByMemberAccountIdOrderByCreatedAtDesc`: [L20](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestRepository.java#L20)
- `MembershipTerminationRequestRepository#findAllByOrderByCreatedAtDesc`: [L22](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestRepository.java#L22)
- `MembershipTerminationRequestRepository#existsByMembershipIdAndStatusIn`: [L24](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestRepository.java#L24)
- `MembershipTerminationRequestRepository#findApprovedDueForUpdate`: [L26](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestRepository.java#L26)

### [MembershipTerminationRequestResponse](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L9)

Package `com.scms.backend.membership.termination`. Phụ lục đã đối chiếu trước đó

- `MembershipTerminationRequestResponse#from`: [L11](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L11), [L15](../../src/main/java/com/scms/backend/membership/termination/MembershipTerminationRequestResponse.java#L15)

### [DatabaseNotificationProvider](../../src/main/java/com/scms/backend/notification/DatabaseNotificationProvider.java#L6)

Package `com.scms.backend.notification`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `DatabaseNotificationProvider#deliver`: [L15](../../src/main/java/com/scms/backend/notification/DatabaseNotificationProvider.java#L15)

### [Notification](../../src/main/java/com/scms/backend/notification/Notification.java#L18)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó

- `Notification#getId`: [L79](../../src/main/java/com/scms/backend/notification/Notification.java#L79)
- `Notification#getRecipientAccountId`: [L83](../../src/main/java/com/scms/backend/notification/Notification.java#L83)
- `Notification#getEventKey`: [L87](../../src/main/java/com/scms/backend/notification/Notification.java#L87)
- `Notification#getNotificationType`: [L91](../../src/main/java/com/scms/backend/notification/Notification.java#L91)
- `Notification#getTargetType`: [L95](../../src/main/java/com/scms/backend/notification/Notification.java#L95)
- `Notification#getTargetId`: [L99](../../src/main/java/com/scms/backend/notification/Notification.java#L99)
- `Notification#getPayload`: [L103](../../src/main/java/com/scms/backend/notification/Notification.java#L103)
- `Notification#getDeliveryStatus`: [L107](../../src/main/java/com/scms/backend/notification/Notification.java#L107)
- `Notification#getDeliveryAttemptCount`: [L111](../../src/main/java/com/scms/backend/notification/Notification.java#L111)
- `Notification#getLastDeliveryAttemptAt`: [L115](../../src/main/java/com/scms/backend/notification/Notification.java#L115)
- `Notification#getDeliveredAt`: [L119](../../src/main/java/com/scms/backend/notification/Notification.java#L119)
- `Notification#getReadAt`: [L123](../../src/main/java/com/scms/backend/notification/Notification.java#L123)
- `Notification#getCreatedAt`: [L127](../../src/main/java/com/scms/backend/notification/Notification.java#L127)

### [NotificationDeliveryStatus](../../src/main/java/com/scms/backend/notification/NotificationDeliveryStatus.java#L3)

Package `com.scms.backend.notification`. Tham chiếu kiểu từ Notification


### [NotificationNotFoundException](../../src/main/java/com/scms/backend/notification/NotificationNotFoundException.java#L3)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó


### [NotificationOutboxEvent](../../src/main/java/com/scms/backend/notification/NotificationOutboxEvent.java#L5)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó


### [NotificationOutboxRepository](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L11)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó

- `NotificationOutboxRepository#enqueue`: [L20](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L20)
- `NotificationOutboxRepository#claimNext`: [L28](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L28)
- `NotificationOutboxRepository#markSent`: [L47](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L47)
- `NotificationOutboxRepository#markFailed`: [L55](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L55)
- `NotificationOutboxRepository#markTerminalFailed`: [L63](../../src/main/java/com/scms/backend/notification/NotificationOutboxRepository.java#L63)

### [NotificationPageResponse](../../src/main/java/com/scms/backend/notification/NotificationPageResponse.java#L5)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó


### [NotificationProvider](../../src/main/java/com/scms/backend/notification/NotificationProvider.java#L3)

Package `com.scms.backend.notification`. Tham chiếu kiểu từ NotificationOutboxDeliveryService

- `NotificationProvider#deliver`: [L6](../../src/main/java/com/scms/backend/notification/NotificationProvider.java#L6)

### [NotificationReadResponse](../../src/main/java/com/scms/backend/notification/NotificationReadResponse.java#L3)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó


### [NotificationRepository](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L12)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó

- `NotificationRepository#findByRecipientAccountId`: [L14](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L14)
- `NotificationRepository#findByRecipientAccountIdAndReadAtIsNull`: [L16](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L16)
- `NotificationRepository#countByRecipientAccountIdAndReadAtIsNull`: [L18](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L18)
- `NotificationRepository#findByIdAndRecipientAccountId`: [L20](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L20)
- `NotificationRepository#markRead`: [L22](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L22)
- `NotificationRepository#markAllRead`: [L26](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L26)
- `NotificationRepository#insertPending`: [L30](../../src/main/java/com/scms/backend/notification/NotificationRepository.java#L30)

### [NotificationResponse](../../src/main/java/com/scms/backend/notification/NotificationResponse.java#L7)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó

- `NotificationResponse#from`: [L9](../../src/main/java/com/scms/backend/notification/NotificationResponse.java#L9)

### [NotificationValidationException](../../src/main/java/com/scms/backend/notification/NotificationValidationException.java#L3)

Package `com.scms.backend.notification`. Phụ lục đã đối chiếu trước đó

- `NotificationValidationException#getField`: [L12](../../src/main/java/com/scms/backend/notification/NotificationValidationException.java#L12)

### [Attendance](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L13)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `Attendance#getId`: [L58](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L58)
- `Attendance#getBookingId`: [L62](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L62)
- `Attendance#getStatus`: [L66](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L66)
- `Attendance#getRecordedAt`: [L70](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L70)
- `Attendance#getCenterVisitId`: [L74](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L74)
- `Attendance#mark`: [L78](../../src/main/java/com/scms/backend/scheduling/Attendance.java#L78)

### [AttendanceException](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L5)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `AttendanceException#getStatus`: [L17](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L17)
- `AttendanceException#getCode`: [L21](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L21)
- `AttendanceException#missing`: [L25](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L25)
- `AttendanceException#visit`: [L29](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L29)
- `AttendanceException#closed`: [L33](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L33)
- `AttendanceException#invalid`: [L37](../../src/main/java/com/scms/backend/scheduling/AttendanceException.java#L37)

### [AttendanceStatus](../../src/main/java/com/scms/backend/scheduling/AttendanceStatus.java#L3)

Package `com.scms.backend.scheduling`. Tham chiếu kiểu từ Attendance


### [Booking](../../src/main/java/com/scms/backend/scheduling/Booking.java#L13)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `Booking#getId`: [L70](../../src/main/java/com/scms/backend/scheduling/Booking.java#L70)
- `Booking#getClassSessionId`: [L74](../../src/main/java/com/scms/backend/scheduling/Booking.java#L74)
- `Booking#getMemberAccountId`: [L78](../../src/main/java/com/scms/backend/scheduling/Booking.java#L78)
- `Booking#getMembershipId`: [L82](../../src/main/java/com/scms/backend/scheduling/Booking.java#L82)
- `Booking#getStatus`: [L86](../../src/main/java/com/scms/backend/scheduling/Booking.java#L86)
- `Booking#getBookedByAccountId`: [L90](../../src/main/java/com/scms/backend/scheduling/Booking.java#L90)
- `Booking#getCreatedAt`: [L94](../../src/main/java/com/scms/backend/scheduling/Booking.java#L94)
- `Booking#getCancelledByAccountId`: [L98](../../src/main/java/com/scms/backend/scheduling/Booking.java#L98)
- `Booking#getCancellationSource`: [L102](../../src/main/java/com/scms/backend/scheduling/Booking.java#L102)
- `Booking#getCancelledAt`: [L106](../../src/main/java/com/scms/backend/scheduling/Booking.java#L106)
- `Booking#cancel`: [L110](../../src/main/java/com/scms/backend/scheduling/Booking.java#L110)

### [BookingException](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L6)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `BookingException#sessionNotFound`: [L28](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L28)
- `BookingException#memberNotActive`: [L32](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L32)
- `BookingException#membershipFrozen`: [L36](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L36)
- `BookingException#actorNotActiveReceptionist`: [L40](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L40)
- `BookingException#sessionUnavailable`: [L44](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L44)
- `BookingException#benefitRequired`: [L48](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L48)
- `BookingException#duplicate`: [L52](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L52)
- `BookingException#timeConflict`: [L56](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L56)
- `BookingException#full`: [L60](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L60)
- `BookingException#ptCoachAssignmentMismatch`: [L64](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L64)
- `BookingException#concurrentConflict`: [L68](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L68)
- `BookingException#notFound`: [L72](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L72)
- `BookingException#notBooked`: [L76](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L76)
- `BookingException#cancellationWindowClosed`: [L80](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L80)
- `BookingException#getStatus`: [L84](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L84)
- `BookingException#getCode`: [L88](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L88)
- `BookingException#getRequiredBenefitCodes`: [L92](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L92)
- `BookingException#getMissingBenefitCodes`: [L96](../../src/main/java/com/scms/backend/scheduling/BookingException.java#L96)

### [BookingStatus](../../src/main/java/com/scms/backend/scheduling/BookingStatus.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [ClassSession](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L17)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `ClassSession#getId`: [L89](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L89)
- `ClassSession#getSportClass`: [L93](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L93)
- `ClassSession#getRecurringSchedule`: [L97](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L97)
- `ClassSession#getTeachingCoach`: [L101](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L101)
- `ClassSession#getRoom`: [L105](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L105)
- `ClassSession#getStartTime`: [L109](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L109)
- `ClassSession#getEndTime`: [L113](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L113)
- `ClassSession#getCapacity`: [L117](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L117)
- `ClassSession#getStatus`: [L121](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L121)
- `ClassSession#getCancelledAt`: [L125](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L125)
- `ClassSession#getCancellationReason`: [L129](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L129)
- `ClassSession#getCreatedBy`: [L133](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L133)
- `ClassSession#getVersion`: [L137](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L137)
- `ClassSession#updateAssignment`: [L141](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L141)
- `ClassSession#updateSchedule`: [L146](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L146)
- `ClassSession#cancel`: [L151](../../src/main/java/com/scms/backend/scheduling/ClassSession.java#L151)

### [ClassSessionStatus](../../src/main/java/com/scms/backend/scheduling/ClassSessionStatus.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [CoachLeaveException](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L7)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `CoachLeaveException#batchFailure`: [L29](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L29)
- `CoachLeaveException#validation`: [L35](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L35)
- `CoachLeaveException#notFound`: [L39](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L39)
- `CoachLeaveException#conflict`: [L43](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L43)
- `CoachLeaveException#affectedSessions`: [L47](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L47)
- `CoachLeaveException#alreadyStarted`: [L51](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L51)
- `CoachLeaveException#cancellationAlreadyStarted`: [L55](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L55)
- `CoachLeaveException#notApproved`: [L59](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L59)
- `CoachLeaveException#getStatus`: [L63](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L63)
- `CoachLeaveException#getCode`: [L67](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L67)
- `CoachLeaveException#getField`: [L71](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L71)
- `CoachLeaveException#getAffectedSessionIds`: [L75](../../src/main/java/com/scms/backend/scheduling/CoachLeaveException.java#L75)

### [CoachLeaveRequest](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L17)

Package `com.scms.backend.scheduling`. Tham chiếu kiểu từ CoachLeaveRepository

- `CoachLeaveRequest#getId`: [L73](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L73)
- `CoachLeaveRequest#getCoach`: [L77](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L77)
- `CoachLeaveRequest#getStartTime`: [L81](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L81)
- `CoachLeaveRequest#getEndTime`: [L85](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L85)
- `CoachLeaveRequest#getReason`: [L89](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L89)
- `CoachLeaveRequest#getStatus`: [L93](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L93)
- `CoachLeaveRequest#getReviewedBy`: [L97](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L97)
- `CoachLeaveRequest#getReviewNote`: [L101](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L101)
- `CoachLeaveRequest#getReviewedAt`: [L105](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L105)
- `CoachLeaveRequest#getCreatedAt`: [L109](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L109)
- `CoachLeaveRequest#approve`: [L113](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L113)
- `CoachLeaveRequest#reject`: [L117](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L117)
- `CoachLeaveRequest#withdraw`: [L121](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L121)
- `CoachLeaveRequest#cancel`: [L125](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L125)
- `CoachLeaveRequest#expire`: [L132](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L132)
- `CoachLeaveRequest#review`: [L139](../../src/main/java/com/scms/backend/scheduling/CoachLeaveRequest.java#L139)

### [CoachLeaveStatus](../../src/main/java/com/scms/backend/scheduling/CoachLeaveStatus.java#L3)

Package `com.scms.backend.scheduling`. Tham chiếu kiểu từ CoachLeaveRepository


### [Discipline](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L20)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `Discipline#update`: [L63](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L63)
- `Discipline#getId`: [L70](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L70)
- `Discipline#getName`: [L74](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L74)
- `Discipline#getDescription`: [L78](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L78)
- `Discipline#getStatus`: [L82](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L82)
- `Discipline#getRequiredBenefitCodes`: [L86](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L86)
- `Discipline#getEffectiveRequiredBenefitCodes`: [L90](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L90)
- `Discipline#replaceRequiredBenefitCodes`: [L94](../../src/main/java/com/scms/backend/scheduling/Discipline.java#L94)

### [DisciplineNotFoundException](../../src/main/java/com/scms/backend/scheduling/DisciplineNotFoundException.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [DisciplineStatus](../../src/main/java/com/scms/backend/scheduling/DisciplineStatus.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [DisciplineValidationException](../../src/main/java/com/scms/backend/scheduling/DisciplineValidationException.java#L6)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `DisciplineValidationException#getField`: [L22](../../src/main/java/com/scms/backend/scheduling/DisciplineValidationException.java#L22)
- `DisciplineValidationException#getAffectedSessions`: [L26](../../src/main/java/com/scms/backend/scheduling/DisciplineValidationException.java#L26)

### [DuplicateDisciplineException](../../src/main/java/com/scms/backend/scheduling/DuplicateDisciplineException.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [PtSessionAssignmentException](../../src/main/java/com/scms/backend/scheduling/PtSessionAssignmentException.java#L8)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `PtSessionAssignmentException#incompatibleMembers`: [L20](../../src/main/java/com/scms/backend/scheduling/PtSessionAssignmentException.java#L20)
- `PtSessionAssignmentException#getStatus`: [L24](../../src/main/java/com/scms/backend/scheduling/PtSessionAssignmentException.java#L24)
- `PtSessionAssignmentException#getCode`: [L28](../../src/main/java/com/scms/backend/scheduling/PtSessionAssignmentException.java#L28)
- `PtSessionAssignmentException#getAffectedBookings`: [L32](../../src/main/java/com/scms/backend/scheduling/PtSessionAssignmentException.java#L32)
- `PtSessionAssignmentException#getAffectedWaitlists`: [L36](../../src/main/java/com/scms/backend/scheduling/PtSessionAssignmentException.java#L36)

### [RecurringSchedule](../../src/main/java/com/scms/backend/scheduling/RecurringSchedule.java#L17)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `RecurringSchedule#getId`: [L79](../../src/main/java/com/scms/backend/scheduling/RecurringSchedule.java#L79)
- `RecurringSchedule#getSessionCount`: [L83](../../src/main/java/com/scms/backend/scheduling/RecurringSchedule.java#L83)

### [RecurringScheduleException](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L7)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `RecurringScheduleException#sessionHasMembers`: [L45](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L45)
- `RecurringScheduleException#validation`: [L49](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L49)
- `RecurringScheduleException#notFound`: [L53](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L53)
- `RecurringScheduleException#inactive`: [L58](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L58)
- `RecurringScheduleException#legacyYoga`: [L62](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L62)
- `RecurringScheduleException#coachInvalid`: [L66](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L66)
- `RecurringScheduleException#coachOnLeave`: [L70](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L70)
- `RecurringScheduleException#capacityConflict`: [L74](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L74)
- `RecurringScheduleException#roomTimeConflict`: [L78](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L78)
- `RecurringScheduleException#sharedRoomCapacityExceeded`: [L82](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L82)
- `RecurringScheduleException#ptCapacity`: [L86](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L86)
- `RecurringScheduleException#sessionConflict`: [L90](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L90)
- `RecurringScheduleException#sessionNotScheduled`: [L94](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L94)
- `RecurringScheduleException#rescheduleWindowClosed`: [L98](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L98)
- `RecurringScheduleException#versionConflict`: [L102](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L102)
- `RecurringScheduleException#memberTimeConflict`: [L106](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L106)
- `RecurringScheduleException#bulkSelectionChanged`: [L110](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L110)
- `RecurringScheduleException#scheduleConflict`: [L114](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L114)
- `RecurringScheduleException#getStatus`: [L118](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L118)
- `RecurringScheduleException#getCode`: [L122](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L122)
- `RecurringScheduleException#getField`: [L126](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L126)
- `RecurringScheduleException#getEligibleSessionIds`: [L130](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L130)
- `RecurringScheduleException#getIneligibleSessionIds`: [L134](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L134)
- `RecurringScheduleException#getRoomCapacity`: [L138](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L138)
- `RecurringScheduleException#getOccupiedCapacity`: [L142](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L142)
- `RecurringScheduleException#getRequestedCapacity`: [L146](../../src/main/java/com/scms/backend/scheduling/RecurringScheduleException.java#L146)

### [Room](../../src/main/java/com/scms/backend/scheduling/Room.java#L13)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `Room#update`: [L60](../../src/main/java/com/scms/backend/scheduling/Room.java#L60), [L64](../../src/main/java/com/scms/backend/scheduling/Room.java#L64)
- `Room#getId`: [L71](../../src/main/java/com/scms/backend/scheduling/Room.java#L71)
- `Room#getName`: [L75](../../src/main/java/com/scms/backend/scheduling/Room.java#L75)
- `Room#getCapacity`: [L79](../../src/main/java/com/scms/backend/scheduling/Room.java#L79)
- `Room#getStatus`: [L83](../../src/main/java/com/scms/backend/scheduling/Room.java#L83)
- `Room#getUsageMode`: [L87](../../src/main/java/com/scms/backend/scheduling/Room.java#L87)

### [RoomException](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L8)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `RoomException#validation`: [L49](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L49)
- `RoomException#notFound`: [L53](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L53)
- `RoomException#duplicateName`: [L57](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L57)
- `RoomException#capacityConflict`: [L61](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L61)
- `RoomException#activeSessionsConflict`: [L65](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L65)
- `RoomException#inProgressSessionsConflict`: [L69](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L69)
- `RoomException#suspensionValidation`: [L73](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L73)
- `RoomException#suspensionConflict`: [L77](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L77)
- `RoomException#suspensionChanged`: [L81](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L81)
- `RoomException#suspensionExpired`: [L85](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L85)
- `RoomException#inactiveRoom`: [L89](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L89)
- `RoomException#moveCapacityConflict`: [L93](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L93)
- `RoomException#moveSuspensionConflict`: [L97](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L97)
- `RoomException#moveSessionConflict`: [L101](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L101)
- `RoomException#moveConflict`: [L105](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L105)
- `RoomException#cancelNotAllowed`: [L109](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L109)
- `RoomException#sessionNotScheduled`: [L113](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L113)
- `RoomException#getStatus`: [L117](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L117)
- `RoomException#getCode`: [L121](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L121)
- `RoomException#getField`: [L125](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L125)
- `RoomException#getAffectedSessionIds`: [L129](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L129)
- `RoomException#getSessionId`: [L133](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L133)
- `RoomException#getTargetRoomId`: [L137](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L137)
- `RoomException#getStartTime`: [L141](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L141)
- `RoomException#getEndTime`: [L145](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L145)
- `RoomException#getConflictingSessionId`: [L149](../../src/main/java/com/scms/backend/scheduling/RoomException.java#L149)

### [RoomStatus](../../src/main/java/com/scms/backend/scheduling/RoomStatus.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [RoomSuspension](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L13)

Package `com.scms.backend.scheduling`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomSuspension#getId`: [L52](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L52)
- `RoomSuspension#getRoom`: [L56](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L56)
- `RoomSuspension#getStartTime`: [L60](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L60)
- `RoomSuspension#getEndTime`: [L64](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L64)
- `RoomSuspension#getReason`: [L68](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L68)
- `RoomSuspension#getCreatedBy`: [L72](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L72)
- `RoomSuspension#extend`: [L76](../../src/main/java/com/scms/backend/scheduling/RoomSuspension.java#L76)

### [RoomUsageMode](../../src/main/java/com/scms/backend/scheduling/RoomUsageMode.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [SessionWaitlist](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L12)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `SessionWaitlist#getId`: [L62](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L62)
- `SessionWaitlist#getSessionId`: [L66](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L66)
- `SessionWaitlist#getMemberId`: [L70](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L70)
- `SessionWaitlist#getStatus`: [L74](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L74)
- `SessionWaitlist#getJoinedBy`: [L78](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L78)
- `SessionWaitlist#getSource`: [L82](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L82)
- `SessionWaitlist#getTerminalReasonCode`: [L86](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L86)
- `SessionWaitlist#getTerminalAt`: [L90](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L90)
- `SessionWaitlist#getCreatedAt`: [L94](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L94)
- `SessionWaitlist#promote`: [L98](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L98)
- `SessionWaitlist#cancel`: [L102](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L102)
- `SessionWaitlist#skip`: [L106](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L106)
- `SessionWaitlist#expire`: [L110](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L110)
- `SessionWaitlist#finish`: [L114](../../src/main/java/com/scms/backend/scheduling/SessionWaitlist.java#L114)

### [SportClass](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L16)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `SportClass#update`: [L64](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L64)
- `SportClass#getId`: [L71](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L71)
- `SportClass#getDiscipline`: [L75](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L75)
- `SportClass#getName`: [L79](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L79)
- `SportClass#getClassType`: [L83](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L83)
- `SportClass#getDescription`: [L87](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L87)
- `SportClass#getStatus`: [L91](../../src/main/java/com/scms/backend/scheduling/SportClass.java#L91)

### [SportClassException](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L7)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `SportClassException#validation`: [L29](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L29)
- `SportClassException#notFound`: [L33](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L33)
- `SportClassException#disciplineNotFound`: [L37](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L37)
- `SportClassException#inactiveDiscipline`: [L41](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L41)
- `SportClassException#unsupportedType`: [L45](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L45)
- `SportClassException#duplicateName`: [L49](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L49)
- `SportClassException#activeSessions`: [L53](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L53)
- `SportClassException#getStatus`: [L57](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L57)
- `SportClassException#getCode`: [L61](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L61)
- `SportClassException#getField`: [L65](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L65)
- `SportClassException#getAffectedSessions`: [L69](../../src/main/java/com/scms/backend/scheduling/SportClassException.java#L69)

### [SportClassStatus](../../src/main/java/com/scms/backend/scheduling/SportClassStatus.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [SportClassType](../../src/main/java/com/scms/backend/scheduling/SportClassType.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [WaitlistException](../../src/main/java/com/scms/backend/scheduling/WaitlistException.java#L5)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó

- `WaitlistException#sessionNotFull`: [L17](../../src/main/java/com/scms/backend/scheduling/WaitlistException.java#L17)
- `WaitlistException#duplicate`: [L21](../../src/main/java/com/scms/backend/scheduling/WaitlistException.java#L21)
- `WaitlistException#notFound`: [L25](../../src/main/java/com/scms/backend/scheduling/WaitlistException.java#L25)
- `WaitlistException#getStatus`: [L29](../../src/main/java/com/scms/backend/scheduling/WaitlistException.java#L29)
- `WaitlistException#getCode`: [L33](../../src/main/java/com/scms/backend/scheduling/WaitlistException.java#L33)

### [WaitlistStatus](../../src/main/java/com/scms/backend/scheduling/WaitlistStatus.java#L3)

Package `com.scms.backend.scheduling`. Phụ lục đã đối chiếu trước đó


### [AttendanceController](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceController.java#L15)

Package `com.scms.backend.scheduling.attendance`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AttendanceController#list`: [L25](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceController.java#L25)
- `AttendanceController#update`: [L30](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceController.java#L30)

### [AttendanceRepository](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceRepository.java#L12)

Package `com.scms.backend.scheduling.attendance`. Tham chiếu kiểu từ SessionLifecycleService

- `AttendanceRepository#findByBookingId`: [L14](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceRepository.java#L14)
- `AttendanceRepository#insertAbsentForStartedBookings`: [L16](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceRepository.java#L16)

### [AttendanceResponse](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceResponse.java#L9)

Package `com.scms.backend.scheduling.attendance`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AttendanceResponse#from`: [L11](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceResponse.java#L11)

### [AttendanceService](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceService.java#L25)

Package `com.scms.backend.scheduling.attendance`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AttendanceService#list`: [L55](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceService.java#L55)
- `AttendanceService#update`: [L64](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceService.java#L64)
- `AttendanceService#owned`: [L86](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceService.java#L86)

### [AttendanceUpdateRequest](../../src/main/java/com/scms/backend/scheduling/attendance/AttendanceUpdateRequest.java#L5)

Package `com.scms.backend.scheduling.attendance`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [BookingCancellationRepository](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepository.java#L7)

Package `com.scms.backend.scheduling.booking`. Phụ lục đã đối chiếu trước đó

- `BookingCancellationRepository#cancelFutureBookingsForMembershipTermination`: [L9](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepository.java#L9)
- `BookingCancellationRepository#cancelFutureBookingsForMembershipFreeze`: [L11](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepository.java#L11)

### [BookingCancellationRepositoryImpl](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepositoryImpl.java#L8)

Package `com.scms.backend.scheduling.booking`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `BookingCancellationRepositoryImpl#cancelFutureBookingsForMembershipTermination`: [L16](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepositoryImpl.java#L16)
- `BookingCancellationRepositoryImpl#cancelFutureBookingsForMembershipFreeze`: [L38](../../src/main/java/com/scms/backend/scheduling/booking/BookingCancellationRepositoryImpl.java#L38)

### [BookingRepository](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L16)

Package `com.scms.backend.scheduling.booking`. Phụ lục đã đối chiếu trước đó

- `BookingRepository#findBookedMembersBySessionId`: [L18](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L18)
- `BookingRepository#findBookedAttendanceMembers`: [L30](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L30)
- `BookingRepository#findByIdForUpdate`: [L33](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L33)
- `BookingRepository#findBookedBySessionForUpdate`: [L37](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L37)
- `BookingRepository#findFuturePtImpact`: [L46](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L46)
- `BookingRepository#findBookedByIdsForUpdate`: [L61](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L61)
- `BookingRepository#findBookingBenefitEligibility`: [L65](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L65)
- `BookingRepository#findByMemberAccountId`: [L96](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L96)
- `BookingRepository#findByMemberAccountIdAndStatus`: [L98](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L98)
- `BookingRepository#existsByMemberAccountIdAndClassSessionIdAndStatus`: [L100](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L100)
- `BookingRepository#countByClassSessionIdAndStatus`: [L102](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L102)
- `BookingRepository#existsBookedOverlap`: [L104](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L104)
- `BookingRepository#hasActiveMembershipFreeze`: [L114](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L114)
- `BookingRepository#findFutureSessionIdsForMembershipTermination`: [L125](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L125)
- `BookingRepository#findFutureSessionIdsForMembershipFreeze`: [L135](../../src/main/java/com/scms/backend/scheduling/booking/BookingRepository.java#L135)

### [BookingResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingResponse.java#L8)

Package `com.scms.backend.scheduling.booking`. Phụ lục đã đối chiếu trước đó

- `BookingResponse#from`: [L10](../../src/main/java/com/scms/backend/scheduling/booking/BookingResponse.java#L10)

### [BookingSchedulePageResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingSchedulePageResponse.java#L5)

Package `com.scms.backend.scheduling.booking`. Phụ lục đã đối chiếu trước đó


### [BookingScheduleResponse](../../src/main/java/com/scms/backend/scheduling/booking/BookingScheduleResponse.java#L13)

Package `com.scms.backend.scheduling.booking`. Phụ lục đã đối chiếu trước đó

- `BookingScheduleResponse#from`: [L15](../../src/main/java/com/scms/backend/scheduling/booking/BookingScheduleResponse.java#L15)

### [ReceptionBookingRequest](../../src/main/java/com/scms/backend/scheduling/booking/ReceptionBookingRequest.java#L6)

Package `com.scms.backend.scheduling.booking`. Phụ lục đã đối chiếu trước đó


### [CoachLeaveBatchController](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchController.java#L14)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveBatchController#available`: [L24](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchController.java#L24)
- `CoachLeaveBatchController#resolve`: [L29](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchController.java#L29)

### [CoachLeaveBatchService](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L24)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveBatchService#selected`: [L60](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L60)
- `CoachLeaveBatchService#requireNonOverlapping`: [L81](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L81)
- `CoachLeaveBatchService#available`: [L90](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L90)
- `CoachLeaveBatchService#resolve`: [L112](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchService.java#L112)

### [CoachLeaveController](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L18)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveController#create`: [L27](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L27)
- `CoachLeaveController#mine`: [L32](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L32)
- `CoachLeaveController#withdraw`: [L37](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L37)
- `CoachLeaveController#cancel`: [L42](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L42)
- `CoachLeaveController#list`: [L47](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L47)
- `CoachLeaveController#page`: [L52](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L52)
- `CoachLeaveController#approve`: [L57](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L57)
- `CoachLeaveController#reject`: [L62](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L62)
- `CoachLeaveController#subject`: [L67](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveController.java#L67)

### [CoachLeaveCreateRequest](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveCreateRequest.java#L5)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachLeaveImpactController](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactController.java#L13)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveImpactController#detail`: [L23](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactController.java#L23)
- `CoachLeaveImpactController#available`: [L29](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactController.java#L29)

### [CoachLeaveImpactRepository](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactRepository.java#L16)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveImpactRepository#map`: [L27](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactRepository.java#L27)
- `CoachLeaveImpactRepository#affectedSessionRows`: [L37](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactRepository.java#L37)
- `CoachLeaveImpactRepository#availableCoachRows`: [L68](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactRepository.java#L68)

### [CoachLeaveImpactService](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L14)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveImpactService#detail`: [L30](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L30)
- `CoachLeaveImpactService#available`: [L39](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L39), [L48](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L48)
- `CoachLeaveImpactService#affectedSessionRows`: [L58](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L58)
- `CoachLeaveImpactService#availableCoachRows`: [L62](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveImpactService.java#L62)

### [CoachLeaveLifecycleScheduler](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleScheduler.java#L9)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveLifecycleScheduler#catchUpAfterStartup`: [L21](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleScheduler.java#L21)
- `CoachLeaveLifecycleScheduler#catchUpOnSchedule`: [L26](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleScheduler.java#L26)

### [CoachLeaveLifecycleService](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleService.java#L14)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveLifecycleService#catchUp`: [L26](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleService.java#L26)
- `CoachLeaveLifecycleService#expireIfStarted`: [L33](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleService.java#L33)
- `CoachLeaveLifecycleService#expire`: [L41](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveLifecycleService.java#L41)

### [CoachLeaveRepository](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L14)

Package `com.scms.backend.scheduling.coachleave`. Phụ lục đã đối chiếu trước đó

- `CoachLeaveRepository#findAll`: [L16](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L16), [L22](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L22)
- `CoachLeaveRepository#findByStatus`: [L19](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L19)
- `CoachLeaveRepository#findByCoach_IdOrderByCreatedAtDesc`: [L26](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L26)
- `CoachLeaveRepository#findByStatusOrderByCreatedAtAsc`: [L29](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L29)
- `CoachLeaveRepository#findByIdForUpdate`: [L32](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L32)
- `CoachLeaveRepository#findPendingStartedForUpdate`: [L36](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L36)
- `CoachLeaveRepository#existsOpenOverlap`: [L45](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L45)
- `CoachLeaveRepository#existsApprovedOverlap`: [L54](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveRepository.java#L54)

### [CoachLeaveResponse](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L9)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveResponse#from`: [L11](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveResponse.java#L11)

### [CoachLeaveReviewRequest](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveReviewRequest.java#L3)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachLeaveService](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L26)

Package `com.scms.backend.scheduling.coachleave`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachLeaveService#create`: [L65](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L65)
- `CoachLeaveService#mine`: [L89](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L89)
- `CoachLeaveService#withdraw`: [L96](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L96)
- `CoachLeaveService#cancel`: [L110](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L110)
- `CoachLeaveService#managerList`: [L126](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L126)
- `CoachLeaveService#managerPage`: [L134](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L134)
- `CoachLeaveService#approve`: [L142](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L142)
- `CoachLeaveService#managerDetail`: [L147](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L147)
- `CoachLeaveService#reject`: [L154](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L154)
- `CoachLeaveService#review`: [L159](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L159)
- `CoachLeaveService#catchUp`: [L184](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L184)
- `CoachLeaveService#expireIfStarted`: [L189](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L189)
- `CoachLeaveService#notifyManagers`: [L194](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L194)
- `CoachLeaveService#notifyCancellationManagers`: [L200](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L200)
- `CoachLeaveService#notifyManager`: [L207](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L207)
- `CoachLeaveService#activeAccount`: [L212](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L212)
- `CoachLeaveService#normalize`: [L219](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L219)
- `CoachLeaveService#response`: [L230](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L230)
- `CoachLeaveService#affectedSessionIds`: [L234](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L234)
- `CoachLeaveService#snapshot`: [L241](../../src/main/java/com/scms/backend/scheduling/coachleave/CoachLeaveService.java#L241)

### [DisciplineController](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineController.java#L19)

Package `com.scms.backend.scheduling.discipline`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `DisciplineController#list`: [L29](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineController.java#L29)
- `DisciplineController#create`: [L34](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineController.java#L34)
- `DisciplineController#get`: [L40](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineController.java#L40)
- `DisciplineController#update`: [L45](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineController.java#L45)
- `DisciplineController#accountId`: [L50](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineController.java#L50)

### [DisciplineCreateRequest](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineCreateRequest.java#L7)

Package `com.scms.backend.scheduling.discipline`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [DisciplinePatchRequest](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplinePatchRequest.java#L6)

Package `com.scms.backend.scheduling.discipline`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [DisciplineRepository](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L13)

Package `com.scms.backend.scheduling.discipline`. Phụ lục đã đối chiếu trước đó

- `DisciplineRepository#existsByName`: [L15](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L15)
- `DisciplineRepository#existsByNameAndIdNot`: [L17](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L17)
- `DisciplineRepository#findByIdForUpdate`: [L19](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L19)
- `DisciplineRepository#findBySessionIdForUpdate`: [L23](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L23)
- `DisciplineRepository#findAllWithRequiredBenefitCodes`: [L33](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L33)
- `DisciplineRepository#findBlockingSessions`: [L41](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineRepository.java#L41)

### [DisciplineResponse](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineResponse.java#L8)

Package `com.scms.backend.scheduling.discipline`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `DisciplineResponse#from`: [L10](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineResponse.java#L10)

### [DisciplineService](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L28)

Package `com.scms.backend.scheduling.discipline`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `DisciplineService#list`: [L49](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L49)
- `DisciplineService#get`: [L55](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L55)
- `DisciplineService#create`: [L61](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L61)
- `DisciplineService#update`: [L85](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L85)
- `DisciplineService#snapshot`: [L130](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L130)
- `DisciplineService#normalizeRequiredBenefitCodes`: [L139](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L139)
- `DisciplineService#invalidRequiredBenefits`: [L158](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L158)
- `DisciplineService#find`: [L162](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L162)
- `DisciplineService#findForUpdate`: [L166](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L166)
- `DisciplineService#ensureActiveManager`: [L173](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L173)
- `DisciplineService#normalizeName`: [L179](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L179)
- `DisciplineService#normalizeDescription`: [L186](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L186)
- `DisciplineService#isLegacyYoga`: [L193](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineService.java#L193)

### [DisciplineSessionConflict](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineSessionConflict.java#L6)

Package `com.scms.backend.scheduling.discipline`. Tham chiếu kiểu từ DisciplineValidationException, SportClassException

- `DisciplineSessionConflict#from`: [L8](../../src/main/java/com/scms/backend/scheduling/discipline/DisciplineSessionConflict.java#L8)

### [SharedResourceLockRendezvous](../../src/main/java/com/scms/backend/scheduling/resource/SharedResourceLockRendezvous.java#L5)

Package `com.scms.backend.scheduling.resource`. Tham chiếu kiểu từ SharedResourceLockService

- `SharedResourceLockRendezvous#beforeLock`: [L8](../../src/main/java/com/scms/backend/scheduling/resource/SharedResourceLockRendezvous.java#L8)
- `SharedResourceLockRendezvous#afterLock`: [L11](../../src/main/java/com/scms/backend/scheduling/resource/SharedResourceLockRendezvous.java#L11)

### [SharedResourceLockService](../../src/main/java/com/scms/backend/scheduling/resource/SharedResourceLockService.java#L13)

Package `com.scms.backend.scheduling.resource`. Phụ lục đã đối chiếu trước đó

- `SharedResourceLockService#lock`: [L28](../../src/main/java/com/scms/backend/scheduling/resource/SharedResourceLockService.java#L28)

### [RoomAvailabilityAnalysis](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L17)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomAvailabilityAnalysis#targetRoomAvailability`: [L31](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L31)
- `RoomAvailabilityAnalysis#supportsBulkMove`: [L58](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L58)
- `RoomAvailabilityAnalysis#overlappingCapacity`: [L70](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L70)
- `RoomAvailabilityAnalysis#capacityPrefix`: [L84](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L84)
- `RoomAvailabilityAnalysis#lowerBound`: [L92](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L92)
- `RoomAvailabilityAnalysis#upperBound`: [L105](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L105)
- `RoomAvailabilityAnalysis#hasOverlappingSessions`: [L118](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityAnalysis.java#L118)

### [RoomAvailabilityResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityResponse.java#L6)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomAvailabilityView](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L5)

Package `com.scms.backend.scheduling.room`. Tham chiếu kiểu từ RoomRepository

- `RoomAvailabilityView#getRoomId`: [L7](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L7)
- `RoomAvailabilityView#getName`: [L9](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L9)
- `RoomAvailabilityView#getUsageMode`: [L11](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L11)
- `RoomAvailabilityView#getCapacity`: [L13](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L13)
- `RoomAvailabilityView#getOccupiedCapacity`: [L15](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L15)
- `RoomAvailabilityView#getSuspended`: [L17](../../src/main/java/com/scms/backend/scheduling/room/RoomAvailabilityView.java#L17)

### [RoomBulkResolutionRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomBulkResolutionRequest.java#L7)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomController](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L21)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomController#list`: [L34](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L34)
- `RoomController#availability`: [L39](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L39)
- `RoomController#create`: [L44](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L44)
- `RoomController#get`: [L50](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L50)
- `RoomController#update`: [L55](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L55)
- `RoomController#listSuspensions`: [L60](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L60)
- `RoomController#suspend`: [L65](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L65)
- `RoomController#extendSuspension`: [L71](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L71)
- `RoomController#impactPreview`: [L76](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L76)
- `RoomController#impactApply`: [L81](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L81)
- `RoomController#accountId`: [L86](../../src/main/java/com/scms/backend/scheduling/room/RoomController.java#L86)

### [RoomCreateRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomCreateRequest.java#L5)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomImpactLockRendezvous](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactLockRendezvous.java#L5)

Package `com.scms.backend.scheduling.room`. Phụ lục đã đối chiếu trước đó

- `RoomImpactLockRendezvous#await`: [L8](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactLockRendezvous.java#L8)

### [RoomImpactPolicy](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L22)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomImpactPolicy#validateImpact`: [L36](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L36)
- `RoomImpactPolicy#resolutionInputs`: [L54](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L54)
- `RoomImpactPolicy#normalizeBulkResolution`: [L69](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L69)
- `RoomImpactPolicy#resolutionMap`: [L85](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L85)
- `RoomImpactPolicy#effectiveResolutions`: [L113](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L113)
- `RoomImpactPolicy#validateOperation`: [L129](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L129)
- `RoomImpactPolicy#validateMove`: [L150](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L150)
- `RoomImpactPolicy#validateReschedule`: [L167](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L167)
- `RoomImpactPolicy#validateRescheduleTimes`: [L180](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L180)
- `RoomImpactPolicy#validateCancel`: [L193](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L193)
- `RoomImpactPolicy#overlaps`: [L199](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L199)
- `RoomImpactPolicy#validateResolutionSet`: [L203](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L203)
- `RoomImpactPolicy#normalizeReason`: [L209](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L209)
- `RoomImpactPolicy#validateExtensionRequest`: [L218](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPolicy.java#L218)

### [RoomImpactPreviewRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewRequest.java#L6)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomImpactPreviewResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewResponse.java#L9)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomImpactPreviewResponse#from`: [L15](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewResponse.java#L15), [L20](../../src/main/java/com/scms/backend/scheduling/room/RoomImpactPreviewResponse.java#L20)

### [RoomPatchRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomPatchRequest.java#L6)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomRepository](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L11)

Package `com.scms.backend.scheduling.room`. Phụ lục đã đối chiếu trước đó

- `RoomRepository#findAvailability`: [L13](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L13)
- `RoomRepository#findBatchAvailability`: [L38](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L38)
- `RoomRepository#findByIdForUpdate`: [L64](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L64)
- `RoomRepository#existsByName`: [L68](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L68)
- `RoomRepository#existsByNameAndIdNot`: [L70](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L70)
- `RoomRepository#existsActiveSessionOverCapacity`: [L72](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L72)
- `RoomRepository#existsActiveLoadOverCapacity`: [L82](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L82)
- `RoomRepository#existsActiveSessionOverlapPair`: [L95](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L95)
- `RoomRepository#findBlockingSessionIds`: [L109](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L109)
- `RoomRepository#existsSuspensionOverlap`: [L118](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L118)
- `RoomRepository#existsActiveSessionOverlap`: [L127](../../src/main/java/com/scms/backend/scheduling/room/RoomRepository.java#L127)

### [RoomResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L8)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomResponse#from`: [L10](../../src/main/java/com/scms/backend/scheduling/room/RoomResponse.java#L10)

### [RoomService](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L24)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomService#list`: [L47](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L47)
- `RoomService#availability`: [L54](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L54)
- `RoomService#get`: [L68](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L68)
- `RoomService#create`: [L74](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L74)
- `RoomService#update`: [L90](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L90)
- `RoomService#disableAfterImpactResolution`: [L127](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L127)
- `RoomService#snapshot`: [L137](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L137)
- `RoomService#availabilityResponse`: [L141](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L141), [L145](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L145), [L149](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L149)
- `RoomService#findRoom`: [L156](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L156)
- `RoomService#findRoomForUpdate`: [L160](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L160)
- `RoomService#flush`: [L167](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L167)
- `RoomService#ensureActiveManager`: [L175](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L175)
- `RoomService#normalizeName`: [L181](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L181)
- `RoomService#validateCapacity`: [L188](../../src/main/java/com/scms/backend/scheduling/room/RoomService.java#L188)

### [RoomSessionAvailabilityView](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L5)

Package `com.scms.backend.scheduling.room`. Tham chiếu kiểu từ RoomRepository

- `RoomSessionAvailabilityView#getSessionId`: [L7](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L7)
- `RoomSessionAvailabilityView#getRoomId`: [L9](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L9)
- `RoomSessionAvailabilityView#getName`: [L11](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L11)
- `RoomSessionAvailabilityView#getUsageMode`: [L13](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L13)
- `RoomSessionAvailabilityView#getCapacity`: [L15](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L15)
- `RoomSessionAvailabilityView#getOccupiedCapacity`: [L17](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L17)
- `RoomSessionAvailabilityView#getSuspended`: [L19](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionAvailabilityView.java#L19)

### [RoomSessionResolutionRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomSessionResolutionRequest.java#L6)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomSuspensionCreateRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionCreateRequest.java#L5)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomSuspensionExtensionRequest](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionExtensionRequest.java#L8)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RoomSuspensionJdbcRepository](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionJdbcRepository.java#L7)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomSuspensionJdbcRepository#notifyChangedSessionFindBookings`: [L9](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionJdbcRepository.java#L9)

### [RoomSuspensionRepository](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionRepository.java#L13)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomSuspensionRepository#existsOverlap`: [L15](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionRepository.java#L15)
- `RoomSuspensionRepository#existsOverlapExcluding`: [L22](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionRepository.java#L22)
- `RoomSuspensionRepository#findByIdForUpdate`: [L29](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionRepository.java#L29)
- `RoomSuspensionRepository#findByRoom_IdOrderByStartTimeAsc`: [L33](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionRepository.java#L33)

### [RoomSuspensionResponse](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionResponse.java#L7)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomSuspensionResponse#from`: [L9](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionResponse.java#L9)

### [RoomSuspensionService](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L37)

Package `com.scms.backend.scheduling.room`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RoomSuspensionService#list`: [L100](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L100)
- `RoomSuspensionService#create`: [L107](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L107)
- `RoomSuspensionService#extend`: [L116](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L116)
- `RoomSuspensionService#previewImpact`: [L159](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L159)
- `RoomSuspensionService#targetRoomAvailability`: [L171](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L171)
- `RoomSuspensionService#applyImpact`: [L175](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L175)
- `RoomSuspensionService#applyResolutions`: [L201](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L201)
- `RoomSuspensionService#createLocked`: [L223](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L223)
- `RoomSuspensionService#courseImpactIds`: [L253](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L253)
- `RoomSuspensionService#ensureNoCourseImpacts`: [L257](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L257)
- `RoomSuspensionService#validateImpact`: [L263](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L263)
- `RoomSuspensionService#resolutionInputs`: [L267](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L267)
- `RoomSuspensionService#effectiveResolutions`: [L271](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L271)
- `RoomSuspensionService#lockRooms`: [L275](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L275)
- `RoomSuspensionService#lockImpactResources`: [L284](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L284)
- `RoomSuspensionService#ensureLockedCoaches`: [L292](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L292)
- `RoomSuspensionService#impactRoomIds`: [L299](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L299)
- `RoomSuspensionService#validateOperation`: [L308](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L308)
- `RoomSuspensionService#validateResolutionSet`: [L312](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L312)
- `RoomSuspensionService#bookingCounts`: [L316](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L316)
- `RoomSuspensionService#suspensionSnapshot`: [L322](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L322)
- `RoomSuspensionService#notifyChangedSession`: [L331](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L331)
- `RoomSuspensionService#normalizeReason`: [L340](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L340)
- `RoomSuspensionService#validateExtensionRequest`: [L344](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L344)
- `RoomSuspensionService#findRoom`: [L367](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L367)
- `RoomSuspensionService#ensureActiveManager`: [L371](../../src/main/java/com/scms/backend/scheduling/room/RoomSuspensionService.java#L371)

### [AvailableCoachResponse](../../src/main/java/com/scms/backend/scheduling/session/AvailableCoachResponse.java#L5)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó

- `AvailableCoachResponse#from`: [L7](../../src/main/java/com/scms/backend/scheduling/session/AvailableCoachResponse.java#L7)

### [ClassSessionAssignmentRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionAssignmentRequest.java#L5)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [ClassSessionCancellationBatchRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationBatchRequest.java#L7)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ClassSessionCancellationBatchResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationBatchResponse.java#L6)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ClassSessionCancellationPreviewResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationPreviewResponse.java#L6)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ClassSessionCancellationRepository](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationRepository.java#L8)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ClassSessionCancellationRepository#cancelLockedFindBookings`: [L10](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationRepository.java#L10)
- `ClassSessionCancellationRepository#cancelLockedUpdateBookings`: [L18](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationRepository.java#L18)

### [ClassSessionCancellationRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationRequest.java#L3)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [ClassSessionCancellationService](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L28)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ClassSessionCancellationService#cancel`: [L69](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L69)
- `ClassSessionCancellationService#cancelLocked`: [L82](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L82)
- `ClassSessionCancellationService#preview`: [L97](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L97)
- `ClassSessionCancellationService#cancelBulk`: [L104](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L104)
- `ClassSessionCancellationService#isEligible`: [L136](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L136)
- `ClassSessionCancellationService#findEligible`: [L140](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L140)
- `ClassSessionCancellationService#ensureActiveManager`: [L159](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L159)
- `ClassSessionCancellationService#validate`: [L165](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L165)
- `ClassSessionCancellationService#validateSelector`: [L172](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCancellationService.java#L172)

### [ClassSessionController](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L23)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ClassSessionController#create`: [L36](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L36)
- `ClassSessionController#list`: [L42](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L42)
- `ClassSessionController#get`: [L51](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L51)
- `ClassSessionController#availableCoaches`: [L56](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L56)
- `ClassSessionController#updateAssignment`: [L61](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L61)
- `ClassSessionController#reschedule`: [L66](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L66)
- `ClassSessionController#cancel`: [L71](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L71)
- `ClassSessionController#cancellationPreview`: [L76](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L76)
- `ClassSessionController#bulkCancel`: [L81](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionController.java#L81)

### [ClassSessionCreateRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionCreateRequest.java#L6)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [ClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionDetailResponse.java#L12)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó

- `ClassSessionDetailResponse#from`: [L14](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionDetailResponse.java#L14)

### [ClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionPageResponse.java#L5)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [ClassSessionRepository](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L17)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó

- `ClassSessionRepository#findByIdIn`: [L19](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L19)
- `ClassSessionRepository#findByIdAndTeachingCoach_Id`: [L22](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L22)
- `ClassSessionRepository#findOwnedGroupOrYogaByIdForRead`: [L24](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L24)
- `ClassSessionRepository#findOwnedGroupOrYogaById`: [L35](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L35)
- `ClassSessionRepository#findByIdForUpdate`: [L45](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L45)
- `ClassSessionRepository#findWaitlistPromotionCandidateIds`: [L49](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L49)
- `ClassSessionRepository#findRoomIdById`: [L64](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L64)
- `ClassSessionRepository#findCoachIdById`: [L67](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L67)
- `ClassSessionRepository#findAllByIdForUpdate`: [L70](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L70)
- `ClassSessionRepository#findFutureScheduledForUpdate`: [L78](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L78)
- `ClassSessionRepository#advanceScheduledToInProgress`: [L90](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L90)
- `ClassSessionRepository#advanceInProgressToCompleted`: [L100](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L100)
- `ClassSessionRepository#existsOverlap`: [L110](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L110)
- `ClassSessionRepository#existsOverlapExcluding`: [L119](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L119)
- `ClassSessionRepository#existsCoachOverlap`: [L129](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L129)
- `ClassSessionRepository#existsCoachOverlapExcluding`: [L138](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L138)
- `ClassSessionRepository#existsRoomOverlap`: [L148](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L148)
- `ClassSessionRepository#existsRoomOverlapExcluding`: [L157](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L157)
- `ClassSessionRepository#sumOverlappingCapacity`: [L167](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L167)
- `ClassSessionRepository#sumOverlappingCapacityExcluding`: [L176](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L176)
- `ClassSessionRepository#findConflictIdExcluding`: [L186](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L186)
- `ClassSessionRepository#countBookedBySessionIds`: [L197](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L197)
- `ClassSessionRepository#findRequiredBenefitsBySessionIds`: [L205](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L205)
- `ClassSessionRepository#findFutureScheduledByIds`: [L216](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L216)
- `ClassSessionRepository#findActiveOverlapIdsByCoach`: [L225](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L225)
- `ClassSessionRepository#findAvailableCoaches`: [L235](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L235)
- `ClassSessionRepository#findAvailableCoachesForWindow`: [L265](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L265)
- `ClassSessionRepository#findBookedMemberIds`: [L294](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L294)
- `ClassSessionRepository#findBookedMemberConflictIds`: [L302](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L302)
- `ClassSessionRepository#findRoomImpacts`: [L319](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L319)
- `ClassSessionRepository#findRoomImpactCoachIds`: [L331](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L331)
- `ClassSessionRepository#findRoomImpactsForUpdate`: [L342](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionRepository.java#L342)

### [ClassSessionResponse](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResponse.java#L8)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó

- `ClassSessionResponse#withBookedCount`: [L14](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResponse.java#L14)
- `ClassSessionResponse#from`: [L18](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionResponse.java#L18)

### [ClassSessionScheduleRequest](../../src/main/java/com/scms/backend/scheduling/session/ClassSessionScheduleRequest.java#L6)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [CoachAvailabilityController](../../src/main/java/com/scms/backend/scheduling/session/CoachAvailabilityController.java#L14)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAvailabilityController#available`: [L24](../../src/main/java/com/scms/backend/scheduling/session/CoachAvailabilityController.java#L24)

### [CoachClassSessionDetailResponse](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionDetailResponse.java#L11)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachClassSessionDetailResponse#from`: [L13](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionDetailResponse.java#L13)

### [CoachClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionPageResponse.java#L5)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachClassSessionResponse](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionResponse.java#L10)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachClassSessionResponse#from`: [L12](../../src/main/java/com/scms/backend/scheduling/session/CoachClassSessionResponse.java#L12)

### [CoachSessionMemberResponse](../../src/main/java/com/scms/backend/scheduling/session/CoachSessionMemberResponse.java#L6)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachSessionMemberResponse#from`: [L8](../../src/main/java/com/scms/backend/scheduling/session/CoachSessionMemberResponse.java#L8)

### [CoachTeachingScheduleController](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleController.java#L18)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachTeachingScheduleController#list`: [L28](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleController.java#L28)
- `CoachTeachingScheduleController#get`: [L37](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleController.java#L37)

### [CoachTeachingScheduleService](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleService.java#L21)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachTeachingScheduleService#list`: [L42](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleService.java#L42)
- `CoachTeachingScheduleService#get`: [L62](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleService.java#L62)
- `CoachTeachingScheduleService#ensureActiveCoach`: [L71](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleService.java#L71)
- `CoachTeachingScheduleService#validateRange`: [L77](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleService.java#L77)
- `CoachTeachingScheduleService#advanceStatuses`: [L83](../../src/main/java/com/scms/backend/scheduling/session/CoachTeachingScheduleService.java#L83)

### [ExplicitBulkCancellationLockRendezvous](../../src/main/java/com/scms/backend/scheduling/session/ExplicitBulkCancellationLockRendezvous.java#L5)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ExplicitBulkCancellationLockRendezvous#await`: [L8](../../src/main/java/com/scms/backend/scheduling/session/ExplicitBulkCancellationLockRendezvous.java#L8)

### [MemberClassScheduleController](../../src/main/java/com/scms/backend/scheduling/session/MemberClassScheduleController.java#L16)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberClassScheduleController#list`: [L26](../../src/main/java/com/scms/backend/scheduling/session/MemberClassScheduleController.java#L26)

### [MemberClassSessionPageResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionPageResponse.java#L5)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [MemberClassSessionResponse](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionResponse.java#L11)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó

- `MemberClassSessionResponse#from`: [L13](../../src/main/java/com/scms/backend/scheduling/session/MemberClassSessionResponse.java#L13)

### [PtAssignmentImpactConfirmation](../../src/main/java/com/scms/backend/scheduling/session/PtAssignmentImpactConfirmation.java#L6)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [PtAssignmentImpactItem](../../src/main/java/com/scms/backend/scheduling/session/PtAssignmentImpactItem.java#L6)

Package `com.scms.backend.scheduling.session`. Tham chiếu kiểu từ PtAssignmentImpactResponse


### [PtAssignmentImpactResponse](../../src/main/java/com/scms/backend/scheduling/session/PtAssignmentImpactResponse.java#L5)

Package `com.scms.backend.scheduling.session`. Tham chiếu kiểu từ CoachAssignmentException

- `PtAssignmentImpactResponse#empty`: [L12](../../src/main/java/com/scms/backend/scheduling/session/PtAssignmentImpactResponse.java#L12)

### [PtCoachAssignmentImpactService](../../src/main/java/com/scms/backend/scheduling/session/PtCoachAssignmentImpactService.java#L24)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `PtCoachAssignmentImpactService#preview`: [L50](../../src/main/java/com/scms/backend/scheduling/session/PtCoachAssignmentImpactService.java#L50)
- `PtCoachAssignmentImpactService#resolve`: [L56](../../src/main/java/com/scms/backend/scheduling/session/PtCoachAssignmentImpactService.java#L56)
- `PtCoachAssignmentImpactService#matches`: [L101](../../src/main/java/com/scms/backend/scheduling/session/PtCoachAssignmentImpactService.java#L101)

### [PtSessionAffectedBooking](../../src/main/java/com/scms/backend/scheduling/session/PtSessionAffectedBooking.java#L5)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [PtSessionAffectedWaitlist](../../src/main/java/com/scms/backend/scheduling/session/PtSessionAffectedWaitlist.java#L5)

Package `com.scms.backend.scheduling.session`. Phụ lục đã đối chiếu trước đó


### [RecurringScheduleController](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleController.java#L14)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RecurringScheduleController#create`: [L24](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleController.java#L24)

### [RecurringScheduleCreateRequest](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleCreateRequest.java#L9)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RecurringScheduleRepository](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleRepository.java#L7)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RecurringScheduleResponse](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleResponse.java#L6)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [RecurringScheduleService](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleService.java#L19)

Package `com.scms.backend.scheduling.session`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `RecurringScheduleService#create`: [L43](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleService.java#L43)
- `RecurringScheduleService#occurrences`: [L68](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleService.java#L68)
- `RecurringScheduleService#validateRequest`: [L82](../../src/main/java/com/scms/backend/scheduling/session/RecurringScheduleService.java#L82)

### [SportClassController](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L19)

Package `com.scms.backend.scheduling.sportclass`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `SportClassController#list`: [L29](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L29)
- `SportClassController#supportedTypes`: [L34](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L34)
- `SportClassController#create`: [L39](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L39)
- `SportClassController#get`: [L45](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L45)
- `SportClassController#update`: [L50](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L50)
- `SportClassController#accountId`: [L55](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassController.java#L55)

### [SportClassCreateRequest](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassCreateRequest.java#L6)

Package `com.scms.backend.scheduling.sportclass`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [SportClassPatchRequest](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassPatchRequest.java#L6)

Package `com.scms.backend.scheduling.sportclass`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [SportClassRepository](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassRepository.java#L13)

Package `com.scms.backend.scheduling.sportclass`. Phụ lục đã đối chiếu trước đó

- `SportClassRepository#existsByDisciplineIdAndName`: [L15](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassRepository.java#L15)
- `SportClassRepository#existsByDisciplineIdAndNameAndIdNot`: [L17](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassRepository.java#L17)
- `SportClassRepository#findByDisciplineIdForUpdate`: [L19](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassRepository.java#L19)
- `SportClassRepository#findByIdForUpdate`: [L23](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassRepository.java#L23)
- `SportClassRepository#findBlockingSessions`: [L27](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassRepository.java#L27)

### [SportClassResponse](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassResponse.java#L8)

Package `com.scms.backend.scheduling.sportclass`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `SportClassResponse#from`: [L10](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassResponse.java#L10)

### [SportClassService](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L26)

Package `com.scms.backend.scheduling.sportclass`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `SportClassService#list`: [L44](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L44)
- `SportClassService#supportedTypes`: [L51](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L51)
- `SportClassService#get`: [L57](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L57)
- `SportClassService#create`: [L63](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L63)
- `SportClassService#update`: [L83](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L83)
- `SportClassService#snapshot`: [L114](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L114)
- `SportClassService#validateCreate`: [L124](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L124)
- `SportClassService#findClass`: [L135](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L135)
- `SportClassService#findClassForUpdate`: [L139](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L139)
- `SportClassService#flush`: [L143](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L143)
- `SportClassService#ensureActiveManager`: [L151](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L151)
- `SportClassService#normalizeName`: [L157](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L157)
- `SportClassService#normalizeDescription`: [L164](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L164)
- `SportClassService#isLegacyYoga`: [L170](../../src/main/java/com/scms/backend/scheduling/sportclass/SportClassService.java#L170)

### [SessionWaitlistRepository](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L14)

Package `com.scms.backend.scheduling.waitlist`. Phụ lục đã đối chiếu trước đó

- `SessionWaitlistRepository#findBySessionIdAndMemberIdAndStatus`: [L16](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L16)
- `SessionWaitlistRepository#findByMemberIdOrderByCreatedAtDesc`: [L18](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L18)
- `SessionWaitlistRepository#findBySessionIdOrderByCreatedAtAsc`: [L20](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L20)
- `SessionWaitlistRepository#findWaitingForUpdate`: [L22](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L22)
- `SessionWaitlistRepository#findWaitingBySessionForUpdate`: [L26](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L26)
- `SessionWaitlistRepository#findFuturePtImpact`: [L30](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L30)
- `SessionWaitlistRepository#findWaitingByIdsForUpdate`: [L45](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L45)
- `SessionWaitlistRepository#countAhead`: [L49](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L49)
- `SessionWaitlistRepository#findExpiredForUpdate`: [L56](../../src/main/java/com/scms/backend/scheduling/waitlist/SessionWaitlistRepository.java#L56)

### [WaitlistResponse](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L8)

Package `com.scms.backend.scheduling.waitlist`. Phụ lục đã đối chiếu trước đó

- `WaitlistResponse#from`: [L10](../../src/main/java/com/scms/backend/scheduling/waitlist/WaitlistResponse.java#L10)

### [AssignmentCandidateStatus](../../src/main/java/com/scms/backend/training/AssignmentCandidateStatus.java#L3)

Package `com.scms.backend.training`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachAssignment](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L14)

Package `com.scms.backend.training`. Phụ lục đã đối chiếu trước đó

- `CoachAssignment#end`: [L68](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L68)
- `CoachAssignment#getId`: [L76](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L76)
- `CoachAssignment#getMemberAccountId`: [L80](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L80)
- `CoachAssignment#getCoachAccountId`: [L84](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L84)
- `CoachAssignment#getAssignedByAccountId`: [L88](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L88)
- `CoachAssignment#getStatus`: [L92](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L92)
- `CoachAssignment#getStartsAt`: [L96](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L96)
- `CoachAssignment#getEndsAt`: [L100](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L100)
- `CoachAssignment#getEndReason`: [L104](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L104)
- `CoachAssignment#getCreatedAt`: [L108](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L108)
- `CoachAssignment#getUpdatedAt`: [L112](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L112)
- `CoachAssignment#getVersion`: [L116](../../src/main/java/com/scms/backend/training/CoachAssignment.java#L116)

### [CoachAssignmentException](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L6)

Package `com.scms.backend.training`. Phụ lục đã đối chiếu trước đó

- `CoachAssignmentException#memberNotFound`: [L32](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L32)
- `CoachAssignmentException#coachNotFound`: [L36](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L36)
- `CoachAssignmentException#memberNotActive`: [L40](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L40)
- `CoachAssignmentException#coachNotActive`: [L44](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L44)
- `CoachAssignmentException#plusMembershipRequired`: [L48](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L48)
- `CoachAssignmentException#membershipFrozen`: [L52](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L52)
- `CoachAssignmentException#alreadyAssignedToThisCoach`: [L56](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L56)
- `CoachAssignmentException#activeAssignmentNotFound`: [L60](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L60)
- `CoachAssignmentException#actorNotActiveManager`: [L64](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L64)
- `CoachAssignmentException#concurrentConflict`: [L68](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L68)
- `CoachAssignmentException#impactConfirmationRequired`: [L72](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L72)
- `CoachAssignmentException#impactChanged`: [L76](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L76)
- `CoachAssignmentException#coachCapacityExceeded`: [L80](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L80)
- `CoachAssignmentException#validationError`: [L84](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L84)
- `CoachAssignmentException#getStatus`: [L88](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L88)
- `CoachAssignmentException#getCode`: [L92](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L92)
- `CoachAssignmentException#getField`: [L96](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L96)
- `CoachAssignmentException#getImpact`: [L100](../../src/main/java/com/scms/backend/training/CoachAssignmentException.java#L100)

### [CoachAssignmentStatus](../../src/main/java/com/scms/backend/training/CoachAssignmentStatus.java#L3)

Package `com.scms.backend.training`. Phụ lục đã đối chiếu trước đó


### [TrainingFeedback](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L12)

Package `com.scms.backend.training`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `TrainingFeedback#forClassSessionResult`: [L62](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L62)
- `TrainingFeedback#update`: [L74](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L74)
- `TrainingFeedback#updateClassSessionResult`: [L80](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L80)
- `TrainingFeedback#setContext`: [L87](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L87)
- `TrainingFeedback#requireContent`: [L95](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L95)
- `TrainingFeedback#getId`: [L102](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L102)
- `TrainingFeedback#getMemberAccountId`: [L106](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L106)
- `TrainingFeedback#getCoachAssignmentId`: [L110](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L110)
- `TrainingFeedback#getTrainingPlanVersionId`: [L114](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L114)
- `TrainingFeedback#getTrainingResultId`: [L118](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L118)
- `TrainingFeedback#getContent`: [L122](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L122)
- `TrainingFeedback#getCreatedByAccountId`: [L126](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L126)
- `TrainingFeedback#getCreatedAt`: [L130](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L130)
- `TrainingFeedback#getUpdatedAt`: [L134](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L134)
- `TrainingFeedback#getVersion`: [L138](../../src/main/java/com/scms/backend/training/TrainingFeedback.java#L138)

### [TrainingFeedbackException](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L5)

Package `com.scms.backend.training`. Phụ lục đã đối chiếu trước đó

- `TrainingFeedbackException#actorNotActiveCoach`: [L17](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L17)
- `TrainingFeedbackException#plusMembershipRequired`: [L21](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L21)
- `TrainingFeedbackException#activeAssignmentNotFound`: [L25](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L25)
- `TrainingFeedbackException#planVersionNotFound`: [L29](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L29)
- `TrainingFeedbackException#resultNotFound`: [L33](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L33)
- `TrainingFeedbackException#feedbackNotFound`: [L37](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L37)
- `TrainingFeedbackException#sessionNotFound`: [L41](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L41)
- `TrainingFeedbackException#sessionResultNotFound`: [L45](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L45)
- `TrainingFeedbackException#sessionFeedbackNotFound`: [L49](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L49)
- `TrainingFeedbackException#sessionNotEnded`: [L53](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L53)
- `TrainingFeedbackException#getStatus`: [L57](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L57)
- `TrainingFeedbackException#getCode`: [L61](../../src/main/java/com/scms/backend/training/TrainingFeedbackException.java#L61)

### [TrainingPlan](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L14)

Package `com.scms.backend.training`. Tham chiếu kiểu từ TrainingPlanException

- `TrainingPlan#update`: [L62](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L62)
- `TrainingPlan#archive`: [L67](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L67)
- `TrainingPlan#getId`: [L72](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L72)
- `TrainingPlan#getMemberAccountId`: [L76](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L76)
- `TrainingPlan#getCoachAssignmentId`: [L80](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L80)
- `TrainingPlan#getTitle`: [L84](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L84)
- `TrainingPlan#getStatus`: [L88](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L88)
- `TrainingPlan#getCreatedByAccountId`: [L92](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L92)
- `TrainingPlan#getCreatedAt`: [L96](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L96)
- `TrainingPlan#getUpdatedAt`: [L100](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L100)
- `TrainingPlan#getVersion`: [L104](../../src/main/java/com/scms/backend/training/TrainingPlan.java#L104)

### [TrainingPlanException](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L5)

Package `com.scms.backend.training`. Phụ lục đã đối chiếu trước đó

- `TrainingPlanException#actorNotActiveCoach`: [L37](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L37)
- `TrainingPlanException#plusMembershipRequired`: [L41](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L41)
- `TrainingPlanException#activeAssignmentNotFound`: [L45](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L45)
- `TrainingPlanException#planNotFound`: [L49](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L49)
- `TrainingPlanException#planNotActive`: [L53](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L53)
- `TrainingPlanException#versionHistoryNotFound`: [L57](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L57)
- `TrainingPlanException#versionConflict`: [L61](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L61)
- `TrainingPlanException#activePlanExists`: [L65](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L65)
- `TrainingPlanException#activePlanChanged`: [L69](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L69)
- `TrainingPlanException#validationError`: [L73](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L73)
- `TrainingPlanException#getStatus`: [L77](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L77)
- `TrainingPlanException#getCode`: [L81](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L81)
- `TrainingPlanException#getField`: [L85](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L85)
- `TrainingPlanException#getCurrentVersionNumber`: [L89](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L89)
- `TrainingPlanException#getActivePlanId`: [L93](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L93)
- `TrainingPlanException#getActivePlanTitle`: [L97](../../src/main/java/com/scms/backend/training/TrainingPlanException.java#L97)

### [TrainingPlanStatus](../../src/main/java/com/scms/backend/training/TrainingPlanStatus.java#L3)

Package `com.scms.backend.training`. Tham chiếu kiểu từ TrainingPlan


### [TrainingPlanVersion](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L16)

Package `com.scms.backend.training`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `TrainingPlanVersion#getId`: [L52](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L52)
- `TrainingPlanVersion#getTrainingPlanId`: [L56](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L56)
- `TrainingPlanVersion#getVersionNumber`: [L60](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L60)
- `TrainingPlanVersion#getContent`: [L64](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L64)
- `TrainingPlanVersion#getCreatedByAccountId`: [L68](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L68)
- `TrainingPlanVersion#getCreatedAt`: [L72](../../src/main/java/com/scms/backend/training/TrainingPlanVersion.java#L72)

### [TrainingResult](../../src/main/java/com/scms/backend/training/TrainingResult.java#L17)

Package `com.scms.backend.training`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `TrainingResult#forClassSession`: [L75](../../src/main/java/com/scms/backend/training/TrainingResult.java#L75)
- `TrainingResult#update`: [L88](../../src/main/java/com/scms/backend/training/TrainingResult.java#L88)
- `TrainingResult#forCourseSession`: [L95](../../src/main/java/com/scms/backend/training/TrainingResult.java#L95)
- `TrainingResult#setSessionPlanVersion`: [L102](../../src/main/java/com/scms/backend/training/TrainingResult.java#L102)
- `TrainingResult#updateClassSessionResult`: [L106](../../src/main/java/com/scms/backend/training/TrainingResult.java#L106)
- `TrainingResult#getId`: [L112](../../src/main/java/com/scms/backend/training/TrainingResult.java#L112)
- `TrainingResult#getMemberAccountId`: [L116](../../src/main/java/com/scms/backend/training/TrainingResult.java#L116)
- `TrainingResult#getCoachAssignmentId`: [L120](../../src/main/java/com/scms/backend/training/TrainingResult.java#L120)
- `TrainingResult#getTrainingPlanVersionId`: [L124](../../src/main/java/com/scms/backend/training/TrainingResult.java#L124)
- `TrainingResult#getClassSessionId`: [L128](../../src/main/java/com/scms/backend/training/TrainingResult.java#L128)
- `TrainingResult#getCourseSessionId`: [L132](../../src/main/java/com/scms/backend/training/TrainingResult.java#L132)
- `TrainingResult#getActivityAt`: [L136](../../src/main/java/com/scms/backend/training/TrainingResult.java#L136)
- `TrainingResult#getContent`: [L140](../../src/main/java/com/scms/backend/training/TrainingResult.java#L140)
- `TrainingResult#getCreatedByAccountId`: [L144](../../src/main/java/com/scms/backend/training/TrainingResult.java#L144)
- `TrainingResult#getCreatedAt`: [L148](../../src/main/java/com/scms/backend/training/TrainingResult.java#L148)
- `TrainingResult#getUpdatedAt`: [L152](../../src/main/java/com/scms/backend/training/TrainingResult.java#L152)
- `TrainingResult#getVersion`: [L156](../../src/main/java/com/scms/backend/training/TrainingResult.java#L156)

### [TrainingResultException](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L5)

Package `com.scms.backend.training`. Phụ lục đã đối chiếu trước đó

- `TrainingResultException#actorNotActiveCoach`: [L17](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L17)
- `TrainingResultException#plusMembershipRequired`: [L21](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L21)
- `TrainingResultException#activeAssignmentNotFound`: [L25](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L25)
- `TrainingResultException#planVersionNotFound`: [L29](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L29)
- `TrainingResultException#resultNotFound`: [L33](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L33)
- `TrainingResultException#sessionNotFound`: [L37](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L37)
- `TrainingResultException#sessionMemberNotFound`: [L41](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L41)
- `TrainingResultException#bookedSessionNotFound`: [L45](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L45)
- `TrainingResultException#sessionResultNotFound`: [L49](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L49)
- `TrainingResultException#sessionNotEnded`: [L53](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L53)
- `TrainingResultException#getStatus`: [L57](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L57)
- `TrainingResultException#getCode`: [L61](../../src/main/java/com/scms/backend/training/TrainingResultException.java#L61)

### [AssignmentCandidatePageResponse](../../src/main/java/com/scms/backend/training/assignment/AssignmentCandidatePageResponse.java#L5)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [AssignmentCandidateResponse](../../src/main/java/com/scms/backend/training/assignment/AssignmentCandidateResponse.java#L8)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AssignmentCandidateResponse#from`: [L10](../../src/main/java/com/scms/backend/training/assignment/AssignmentCandidateResponse.java#L10)

### [AssignmentDiscoveryController](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryController.java#L20)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AssignmentDiscoveryController#candidates`: [L30](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryController.java#L30)
- `AssignmentDiscoveryController#workloads`: [L36](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryController.java#L36)
- `AssignmentDiscoveryController#validatePagination`: [L41](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryController.java#L41)
- `AssignmentDiscoveryController#parseAssignmentStatus`: [L50](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryController.java#L50)
- `AssignmentDiscoveryController#accountId`: [L60](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryController.java#L60)

### [AssignmentDiscoveryService](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryService.java#L19)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `AssignmentDiscoveryService#candidates`: [L39](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryService.java#L39)
- `AssignmentDiscoveryService#workloads`: [L48](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryService.java#L48)
- `AssignmentDiscoveryService#reconcile`: [L55](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryService.java#L55)
- `AssignmentDiscoveryService#ensureActiveManager`: [L61](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryService.java#L61)
- `AssignmentDiscoveryService#normalizeQuery`: [L67](../../src/main/java/com/scms/backend/training/assignment/AssignmentDiscoveryService.java#L67)

### [CoachAssignedMemberController](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberController.java#L17)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignedMemberController#list`: [L30](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberController.java#L30)
- `CoachAssignedMemberController#getProfile`: [L42](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberController.java#L42)
- `CoachAssignedMemberController#coachId`: [L47](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberController.java#L47)

### [CoachAssignedMemberPageResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberPageResponse.java#L8)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachAssignedMemberService](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberService.java#L34)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignedMemberService#listAssignedMembers`: [L56](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberService.java#L56)
- `CoachAssignedMemberService#getMemberTrainingProfile`: [L73](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberService.java#L73)
- `CoachAssignedMemberService#ensureActiveCoach`: [L86](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberService.java#L86)
- `CoachAssignedMemberService#normalizeQuery`: [L92](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberService.java#L92)

### [CoachAssignedMemberSummaryResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberSummaryResponse.java#L13)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignedMemberSummaryResponse#from`: [L15](../../src/main/java/com/scms/backend/training/assignment/CoachAssignedMemberSummaryResponse.java#L15)

### [CoachAssignmentAssignRequest](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentAssignRequest.java#L8)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachAssignmentEndRequest](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentEndRequest.java#L7)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachAssignmentPageResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentPageResponse.java#L5)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại


### [CoachAssignmentReminder](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentReminder.java#L13)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignmentReminder#notifyManagers`: [L26](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentReminder.java#L26)

### [CoachAssignmentReminderRepository](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentReminderRepository.java#L6)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignmentReminderRepository#notifyManagersInsertNotificationOutbox`: [L8](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentReminderRepository.java#L8)

### [CoachAssignmentRepository](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L18)

Package `com.scms.backend.training.assignment`. Phụ lục đã đối chiếu trước đó

- `CoachAssignmentRepository#findByMemberAccountIdAndStatus`: [L20](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L20)
- `CoachAssignmentRepository#findByMemberAccountIdAndCoachAccountIdAndStatus`: [L22](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L22)
- `CoachAssignmentRepository#findActiveForMembersAndCoachForUpdate`: [L24](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L24)
- `CoachAssignmentRepository#findByMemberAccountIdAndStatusForUpdate`: [L34](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L34)
- `CoachAssignmentRepository#findByMemberAccountIdOrderByCreatedAtDesc`: [L38](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L38)
- `CoachAssignmentRepository#existsByCoachAccountIdAndStatus`: [L41](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L41)
- `CoachAssignmentRepository#findActiveAssignedMembers`: [L43](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L43)
- `CoachAssignmentRepository#countByCoachAccountIdAndStatus`: [L69](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L69)
- `CoachAssignmentRepository#endActiveAssignmentsForMembershipTermination`: [L71](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L71)
- `CoachAssignmentRepository#hasActiveMembershipBenefit`: [L83](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L83)
- `CoachAssignmentRepository#hasActiveMembershipFreeze`: [L104](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L104)
- `CoachAssignmentRepository#endActiveAssignmentIfPersonalCoachingExpired`: [L112](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L112)
- `CoachAssignmentRepository#endAllActiveAssignmentsWithExpiredPersonalCoaching`: [L147](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L147)
- `CoachAssignmentRepository#findAssignmentCandidates`: [L181](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L181)
- `CoachAssignmentRepository#findAssignmentWorkloads`: [L241](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentRepository.java#L241)

### [CoachAssignmentResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentResponse.java#L10)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignmentResponse#from`: [L16](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentResponse.java#L16), [L20](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentResponse.java#L20)

### [CoachAssignmentService](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L31)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignmentService#assign`: [L65](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L65)
- `CoachAssignmentService#enforceCapacity`: [L137](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L137)
- `CoachAssignmentService#getActive`: [L143](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L143)
- `CoachAssignmentService#listHistory`: [L157](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L157)
- `CoachAssignmentService#endAssignment`: [L170](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L170)
- `CoachAssignmentService#previewImpact`: [L195](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L195)
- `CoachAssignmentService#ensureActiveManager`: [L205](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L205)
- `CoachAssignmentService#ensureMemberExists`: [L212](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L212)
- `CoachAssignmentService#ensureMemberExistsForUpdate`: [L219](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentService.java#L219)

### [CoachAssignmentWorkloadResponse](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentWorkloadResponse.java#L5)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachAssignmentWorkloadResponse#from`: [L7](../../src/main/java/com/scms/backend/training/assignment/CoachAssignmentWorkloadResponse.java#L7)

### [CoachMemberTrainingProfileResponse](../../src/main/java/com/scms/backend/training/assignment/CoachMemberTrainingProfileResponse.java#L11)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `CoachMemberTrainingProfileResponse#from`: [L13](../../src/main/java/com/scms/backend/training/assignment/CoachMemberTrainingProfileResponse.java#L13)

### [ManagerCoachAssignmentController](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L25)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `ManagerCoachAssignmentController#assign`: [L35](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L35)
- `ManagerCoachAssignmentController#getActive`: [L42](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L42)
- `ManagerCoachAssignmentController#previewImpact`: [L48](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L48)
- `ManagerCoachAssignmentController#listHistory`: [L53](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L53)
- `ManagerCoachAssignmentController#end`: [L62](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L62)
- `ManagerCoachAssignmentController#accountId`: [L67](../../src/main/java/com/scms/backend/training/assignment/ManagerCoachAssignmentController.java#L67)

### [MemberCoachAssignmentController](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentController.java#L11)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberCoachAssignmentController#getActive`: [L21](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentController.java#L21)

### [MemberCoachAssignmentResponse](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentResponse.java#L9)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberCoachAssignmentResponse#from`: [L11](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentResponse.java#L11)

### [MemberCoachAssignmentService](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentService.java#L15)

Package `com.scms.backend.training.assignment`. Component/DTO cùng nghiệp vụ trong source hiện tại

- `MemberCoachAssignmentService#getActive`: [L30](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentService.java#L30)
- `MemberCoachAssignmentService#coachResponse`: [L39](../../src/main/java/com/scms/backend/training/assignment/MemberCoachAssignmentService.java#L39)


## File gom DTO đã tách

- `CourseApiModels.java`: tra các type `CourseCreateRequest`, `CoursePatchRequest`, `CourseFieldLimits`, `VersionRequest`, `CourseApprovalDecision`, `CourseApprovalResponse`, `CourseApprovalPageResponse`, `CourseRunCreateRequest`, `CourseRunPatchRequest`, `CourseRunScheduleRequest`, `CourseRunOpenRequest`, `CourseRunRescheduleRequest`, `CourseSessionRescheduleRequest`, `CourseRunRescheduleResponse`, `CourseSessionCreateRequest`, `CourseSessionPatchRequest`, `CourseResponse`, `CoursePageResponse`, `CourseRunResponse`, `CourseRunPageResponse`, `CourseSessionResponse`, `CourseSessionDisruptionRequest`, `CourseMakeupRequest`, `CourseSessionCancelWithMakeupRequest` trong chỉ mục ở trên.
- `CourseCatalogApiModels.java`: tra các type `RegistrationStatus`, `CatalogCourseResponse`, `CatalogCoursePageResponse`, `CatalogCourseRunResponse`, `CatalogCourseRunPageResponse`, `CatalogCourseSessionResponse`, `CatalogCourseRunDetailResponse` trong chỉ mục ở trên.
