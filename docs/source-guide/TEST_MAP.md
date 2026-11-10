# Tra bằng chứng test theo nghiệp vụ

Bảng này dẫn tới test thật, không thay cho kết quả chạy. Kết quả gate cuối ở [VALIDATION.md](VALIDATION.md). Các method bên dưới được lấy từ source cuối; đọc assertion để biết chính xác phạm vi mỗi test.

## API/Swagger

Operation, schema, response và baseline đầy đủ.

### [GeneratedOpenApiTests](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L42)

- [GeneratedOpenApiTests#courseReconciliationContractExistsIndependentlyOfMembership](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L91)
- [GeneratedOpenApiTests#receptionProxyContractsMatchGeneratedResponsesAndMemberSchemas](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L110)
- [GeneratedOpenApiTests#controllerSuccessResponsesHaveConcreteJavaTypes](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L140)
- [GeneratedOpenApiTests#generatedContractContainsEveryRouteAndConcreteSuccessSchema](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L155)
- [GeneratedOpenApiTests#generatedCourseContractDocumentsManagerSecurityValidationEnumsAndProblems](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L186)
- [GeneratedOpenApiTests#generatedReceptionCourseRefundQueueDocumentsFilterAndRedactedResponse](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L206)
- [GeneratedOpenApiTests#generatedCourseMemberTimelineAndManagerMakeupMetadata](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L222)
- [GeneratedOpenApiTests#generatedReceptionEnrollmentDiscoveryDocumentsFiltersAndRedactedFields](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L239)
- [GeneratedOpenApiTests#generatedManagerScheduleChangesExposeVersionedRequestsAndRemovedUnitState](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L256)
- [GeneratedOpenApiTests#generatedCourseCatalogIsPublicAndDocumentsOnlyVisibleRunState](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L275)
- [GeneratedOpenApiTests#generatedCoachCourseSessionDiscoveryDocumentsFiltersAndAttendanceState](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L292)
- [GeneratedOpenApiTests#staticAndGeneratedCourseCatalogContractsHaveNormalizedParity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L305)
- [GeneratedOpenApiTests#staticAndGeneratedCourseOrderContractsHaveParity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L319)
- [GeneratedOpenApiTests#staticAndGeneratedCourseWaitlistContractsExposeOwnershipAndIdempotency](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L339)
- [GeneratedOpenApiTests#staticAndGeneratedCoursePaymentOperationsRequireIdempotencyAndMatchResponses](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L366)
- [GeneratedOpenApiTests#staticAndGeneratedCourseContractsHaveTableDrivenResponseAndProblemParity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L394)
- [GeneratedOpenApiTests#generatedUnauthorizedComponentEnumeratesEveryActualStable401Code](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L415)
- [GeneratedOpenApiTests#sepayWebhookDocumentsAnonymousApiKeyHeaderAuthentication](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L424)
- [GeneratedOpenApiTests#sepayWebhookAcknowledgementSchemaRequiresSuccessProcessedAndOutcome](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L448)
- [GeneratedOpenApiTests#sepayCreateOperationsDocumentConfigurationInvalidServiceUnavailable](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L461)
- [GeneratedOpenApiTests#generatedContractDocumentsSecuritySchemasAndDynamicStatuses](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L471)
- [GeneratedOpenApiTests#generatedContractDocumentsDisciplineBenefitsAndBothBookingConflictVariants](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L517)
- [GeneratedOpenApiTests#generatedContractDocumentsStaffMembershipHistory](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L546)
- [GeneratedOpenApiTests#generatedAndStaticContractsDescribeRefundEligibleSources](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L570)
- [GeneratedOpenApiTests#generatedAndStaticRefundContractsIncludeNullableDisplayOnEveryResponse](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L588)
- [GeneratedOpenApiTests#generatedContractDocumentsEmailVerificationAndManualActivation](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L616)
- [GeneratedOpenApiTests#generatedContractDocumentsAuditActorIdentity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L643)
- [GeneratedOpenApiTests#generatedContractDocumentsMembershipPlanManagement](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L651)
- [GeneratedOpenApiTests#generatedMembershipRequestContractsIncludeManagerMemberIdentity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L700)
- [GeneratedOpenApiTests#staticContractDocumentsAuditActorIdentity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L709)
- [GeneratedOpenApiTests#generatedContractDocumentsCenterVisitMemberName](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L720)
- [GeneratedOpenApiTests#staticContractDocumentsCenterVisitMemberName](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L728)
- [GeneratedOpenApiTests#staticOfferContractsDocumentPublicActiveCatalogue](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L739)
- [GeneratedOpenApiTests#generatedContractDocumentsAssignmentDiscovery](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L755)
- [GeneratedOpenApiTests#generatedContractDocumentsTrainingPlanGapContracts](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L770)
- [GeneratedOpenApiTests#generatedContractDocumentsTrainingBenefitEligibility](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L785)
- [GeneratedOpenApiTests#recurringScheduleContractDocumentsOptionalBoundedSessionCount](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L796)
- [GeneratedOpenApiTests#generatedAndStaticContractsDescribeRoomSuspensionExtension](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L814)
- [GeneratedOpenApiTests#receptionSearchDocumentsNullableActiveMembershipInGeneratedAndStaticContracts](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L841)
- [GeneratedOpenApiTests#staffProfileContractsKeepImageUploadOutOfPatchAndRequireNullableImageInResponses](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L851)
- [GeneratedOpenApiTests#generatedCoachLeaveResponseIncludesCoachIdentity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L860)
- [GeneratedOpenApiTests#generatedTransferResponsesIncludeDisplayIdentity](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L868)
- [GeneratedOpenApiTests#receptionSearchRequiresExplicitNullableMembershipInGeneratedAndStaticContracts](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L876)
- [GeneratedOpenApiTests#cleanupPreservesGeneratedOpenApiContract](../../src/test/java/com/scms/backend/config/GeneratedOpenApiTests.java#L913)

## Security

Matcher theo role, 401/403, tài khoản/JWT không hợp lệ.

### [AuthorizationSnapshotTests](../../src/test/java/com/scms/backend/config/AuthorizationSnapshotTests.java#L26)

- [AuthorizationSnapshotTests#endpointRoleDecisionsMatchBaseline](../../src/test/java/com/scms/backend/config/AuthorizationSnapshotTests.java#L37)

### [SecurityProblemDetailTests](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L22)

- [SecurityProblemDetailTests#unauthenticatedRequestsUseTheApiProblemContract](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L33)
- [SecurityProblemDetailTests#forbiddenRequestsUseTheApiProblemContract](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L38)
- [SecurityProblemDetailTests#membershipOffersArePublic](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L43)
- [SecurityProblemDetailTests#courseCatalogIsPublic](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L49)
- [SecurityProblemDetailTests#bookingViewsAreRoleScoped](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L56)
- [SecurityProblemDetailTests#receptionClassSessionCatalogueIsReceptionistOnly](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L62)
- [SecurityProblemDetailTests#auditHistoryIsManagerOnly](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L70)
- [SecurityProblemDetailTests#managerDashboardIsManagerOnly](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L75)
- [SecurityProblemDetailTests#staffMembershipHistoryRoutesAreRoleScoped](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L81)
- [SecurityProblemDetailTests#assignmentDiscoveryRoutesAreManagerOnly](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L93)
- [SecurityProblemDetailTests#courseManagementRoutesAreManagerOnly](../../src/test/java/com/scms/backend/config/SecurityProblemDetailTests.java#L104)

### [AuthenticationIntegrationTests](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L48)

- [AuthenticationIntegrationTests#loginByEmailNormalizesIdentifierAndReturnsJwtRoleAndLocalCookie](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L93)
- [AuthenticationIntegrationTests#loginByPhoneTrimsIdentifierAndReturnsSession](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L108)
- [AuthenticationIntegrationTests#loginRejectsUnknownIdentifierWrongPasswordAndNonActiveAccountsWithoutDisclosure](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L114)
- [AuthenticationIntegrationTests#loginUsesTheExistingValidationEnvelope](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L128)
- [AuthenticationIntegrationTests#refreshRotatesTokenAndOldTokenCannotBeReused](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L133)
- [AuthenticationIntegrationTests#refreshRejectsExpiredRevokedAndNowInactiveSessions](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L149)
- [AuthenticationIntegrationTests#logoutRevokesCurrentTokenClearsCookieAndIsIdempotent](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L173)
- [AuthenticationIntegrationTests#protectedRequestsRejectMissingAndInvalidBearerTokens](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L183)
- [AuthenticationIntegrationTests#activeAccessTokenIsRejectedAfterAccountBecomesSuspendedOrInactive](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L189)
- [AuthenticationIntegrationTests#credentialedCorsPreflightWorksForLogin](../../src/test/java/com/scms/backend/auth/AuthenticationIntegrationTests.java#L205)

### [ActiveAccountJwtValidatorTests](../../src/test/java/com/scms/backend/auth/ActiveAccountJwtValidatorTests.java#L14)

- [ActiveAccountJwtValidatorTests#rejectsAccessTokenIssuedBeforeAnAuthenticationVersionChange](../../src/test/java/com/scms/backend/auth/ActiveAccountJwtValidatorTests.java#L16)
- [ActiveAccountJwtValidatorTests#acceptsCurrentAuthenticationVersion](../../src/test/java/com/scms/backend/auth/ActiveAccountJwtValidatorTests.java#L27)

## Hủy buổi

Chặn hủy buổi còn hội viên; giữ guard và giao dịch.

### [ClassSessionCancellationIntegrationTests](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L49)

- [ClassSessionCancellationIntegrationTests#memberBookingIsPersistedAuditedNotifiedAndCannotOverbook](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L89)
- [ClassSessionCancellationIntegrationTests#receptionistBooksAndCancelsForMemberWithCorrectOwnershipAndActor](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L145)
- [ClassSessionCancellationIntegrationTests#bookedSessionCannotBeCancelledAndKeepsBookingsAuditAndOutboxUnchanged](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L176)
- [ClassSessionCancellationIntegrationTests#bulkCancellationRejectsBookedSessionsThenCancelsEmptySessionsIdempotently](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L211)
- [ClassSessionCancellationIntegrationTests#mixedExplicitSelectionRollsBackWithoutCancellingTheEligibleSession](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L261)
- [ClassSessionCancellationIntegrationTests#concurrentOppositeOrderBulkCancellationCancelsEachEffectExactlyOnceWithoutDeadlock](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationIntegrationTests.java#L286)

### [ClassSessionCancellationServiceTests](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L43)

- [ClassSessionCancellationServiceTests#rejectsCancellationWhenMembersAreBooked](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L76)
- [ClassSessionCancellationServiceTests#blankReasonIsRejectedBeforeReadingSession](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L94)
- [ClassSessionCancellationServiceTests#nonScheduledSessionCannotBeCancelled](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L101)
- [ClassSessionCancellationServiceTests#previewSupportsClassAndTimeFiltersWithoutRequiringAReason](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L111)
- [ClassSessionCancellationServiceTests#previewRequiresAtLeastOneFilter](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L121)
- [ClassSessionCancellationServiceTests#bulkCancellationRetryIsIdempotentWhenAllRequestedSessionsAreAlreadyHandled](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L128)
- [ClassSessionCancellationServiceTests#bulkCancellationRejectsMixedExplicitSelectionWithEligibleAndIneligibleIds](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L141)
- [ClassSessionCancellationServiceTests#bulkCancellationCancelsEveryExplicitSessionReturnedByTheOrderedLockQuery](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L152)
- [ClassSessionCancellationServiceTests#explicitBulkCancellationReachesRendezvousImmediatelyBeforeOrderedLockQuery](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L169)
- [ClassSessionCancellationServiceTests#bulkCancellationLocksFilteredSessionsBeforeCancellingThem](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L181)
- [ClassSessionCancellationServiceTests#bulkCancellationDoesNotTreatInProgressSessionsAsEligible](../../src/test/java/com/scms/backend/scheduling/session/ClassSessionCancellationServiceTests.java#L195)

### [CourseLeaveCancellationTests](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java#L44)

- [CourseLeaveCancellationTests#rejectsEnrolledSessionAndAllowsEmptySession](../../src/test/java/com/scms/backend/course/management/CourseLeaveCancellationTests.java#L46)

## Coach leave batch

Version cũ, lựa chọn không hợp lệ, reassign/cancel và rollback PostgreSQL.

### [CoachLeaveBatchServiceTests](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L40)

- [CoachLeaveBatchServiceTests#reassignsAllInOneCallWithoutApprovingLeave](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L78)
- [CoachLeaveBatchServiceTests#transactionRollsBackWhenSecondMutationFails](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L87)
- [CoachLeaveBatchServiceTests#rejectsEntireCancellationIfAnySessionHasMembers](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L104)
- [CoachLeaveBatchServiceTests#rejectsStaleOrDuplicateSelectionBeforeMutating](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L112)
- [CoachLeaveBatchServiceTests#candidatesIntersectAcrossTheWholeSelectionWithOneImpactRead](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L122)
- [CoachLeaveBatchServiceTests#courseBatchUsesExistingCourseRulesAndVersions](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L134)
- [CoachLeaveBatchServiceTests#cancellationChecksLockedVersionBeforeCallingExistingGuard](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchServiceTests.java#L145)

### [CoachLeaveBatchControllerTests](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchControllerTests.java#L25)

- [CoachLeaveBatchControllerTests#managerCanSubmitBatchAndOtherRolesCannot](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchControllerTests.java#L45)

### [CoachLeaveBatchRollbackIntegrationTests](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchRollbackIntegrationTests.java#L37)

- [CoachLeaveBatchRollbackIntegrationTests#secondSessionConflictRollsBackFirstAssignmentAndAuditInPostgres](../../src/test/java/com/scms/backend/scheduling/coachleave/CoachLeaveBatchRollbackIntegrationTests.java#L70)

## Payment

Thu tiền, webhook/idempotency, provider claim, cạnh tranh và rollback.

### [PaymentFlowIntegrationTests](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L78)

- [PaymentFlowIntegrationTests#courseReconciliationQueuePaymentAndPrivateResultHaveCorrectOwnership](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L127)
- [PaymentFlowIntegrationTests#courseReconciliationRejectsInvalidMoneyRolesKeysAndTerminalChanges](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L147)
- [PaymentFlowIntegrationTests#courseReconciliationNeverRevivesExpiredReservationsAndFailureReplays](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L164)
- [PaymentFlowIntegrationTests#courseReconciliationAuditFailureRollsBackTheEntireDecision](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L178)
- [PaymentFlowIntegrationTests#concurrentCourseReconciliationAndWebhookOnlyFulfillOnce](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L195)
- [PaymentFlowIntegrationTests#concurrentStaffCourseReconciliationSerializesKeysAndProviderClaims](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L216)
- [PaymentFlowIntegrationTests#manualCourseTransactionCannotBeReusedByMembershipOrAnotherCourse](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L238)
- [PaymentFlowIntegrationTests#concurrentCourseCashAndReconciliationDoNotDuplicateEnrollment](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L257)
- [PaymentFlowIntegrationTests#concurrentCourseExpiryAndFailedReconciliationExpireOnlyOnce](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L278)
- [PaymentFlowIntegrationTests#offerPatchIsPartialRejectsEmptyAndCannotChangePlan](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L377)
- [PaymentFlowIntegrationTests#managerCreatesOfferForActiveDynamicPlanAfterNormalizingCode](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L387)
- [PaymentFlowIntegrationTests#managerRejectsOfferForUnknownPlanWithOfferValidationContract](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L394)
- [PaymentFlowIntegrationTests#managerRejectsOfferForInactivePlanWithOfferValidationContract](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L400)
- [PaymentFlowIntegrationTests#managerRejectsOfferForBenefitlessPlanWithOfferValidationContract](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L407)
- [PaymentFlowIntegrationTests#cashConfirmationIsAtomicAndDoubleClickCannotFulfillTwice](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L414)
- [PaymentFlowIntegrationTests#cashRenewalReplaysByIdempotencyKeyWithoutCreatingDuplicateArtifacts](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L432)
- [PaymentFlowIntegrationTests#lateSepayRenewalActivatesFromTheExpiredMembershipEnd](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L446)
- [PaymentFlowIntegrationTests#cashRejectsInactiveOfferAndInactivePlanWithOrderAvailabilityContract](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L469)
- [PaymentFlowIntegrationTests#cashWaitsForPlanDeactivationAndThenReturnsUnavailable](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L483)
- [PaymentFlowIntegrationTests#cashWaitsForOfferDeactivationAndThenReturnsUnavailable](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L513)
- [PaymentFlowIntegrationTests#notificationDeliveryFailureDoesNotRollBackPaidPaymentFulfillment](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L542)
- [PaymentFlowIntegrationTests#sepayCreationReturnsManualFallbackWithoutLoadingQr](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L567)
- [PaymentFlowIntegrationTests#sepayRetryReturnsTheSameFallbackSnapshotAndOnePayment](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L590)
- [PaymentFlowIntegrationTests#sepayQrParametersMatchReturnedFallback](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L602)
- [PaymentFlowIntegrationTests#sepayDestinationSurvivesRestartAndConfigurationRotation](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L613)
- [PaymentFlowIntegrationTests#sepayRetryPreservesExactPersistedTransferContent](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L630)
- [PaymentFlowIntegrationTests#sepayWebhookUsesPersistedAccountAfterConfigurationRotation](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L648)
- [PaymentFlowIntegrationTests#missingDestinationSnapshotIsNeverAutomaticallyFulfilled](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L662)
- [PaymentFlowIntegrationTests#expiredAutomaticWebhooksLeaveNoPaidArtifacts](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L687)
- [PaymentFlowIntegrationTests#automaticWebhookCrossingDeadlineWhileWaitingForOrderLockDoesNotFulfill](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L716)
- [PaymentFlowIntegrationTests#expiredPaymentCanStillBeManuallyReconciledWithEvidence](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L758)
- [PaymentFlowIntegrationTests#courseWebhookTransactionCannotBeReusedByManualMembershipReconciliation](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L775)
- [PaymentFlowIntegrationTests#manualMembershipReconciliationTransactionCannotFulfillCourse](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L807)
- [PaymentFlowIntegrationTests#concurrentMembershipReconciliationAndCourseWebhookShareProviderClaim](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L834)
- [PaymentFlowIntegrationTests#courseQrCreationAndOrderReadReturnTheSamePaymentSnapshot](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L878)
- [PaymentFlowIntegrationTests#coursePaymentAndCashMutationsRejectMissingIdempotencyKey](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L893)
- [PaymentFlowIntegrationTests#receptionistCashRetryCreatesOneEnrollmentWithExactVndAllocations](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L904)
- [PaymentFlowIntegrationTests#courseLateEnrollmentRequiresMoreThanSeventyPercentUnitsRemaining](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L925)
- [PaymentFlowIntegrationTests#courseCashAllocationFailureRollsBackPaymentOrderReservationAndArtifacts](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L944)
- [PaymentFlowIntegrationTests#courseWebhookAllocationFailureRollsBackEventAndEveryFulfillmentArtifact](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L962)
- [PaymentFlowIntegrationTests#courseWebhookAuditFailureRollsBackEventAndEveryFulfillmentArtifact](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L981)
- [PaymentFlowIntegrationTests#courseWebhookOutboxFailureRollsBackEventAndEveryFulfillmentArtifact](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L995)
- [PaymentFlowIntegrationTests#readTriggeredExpiryAndWebhookBothWaitForCourseRunBeforeOrderOrReservation](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1009)
- [PaymentFlowIntegrationTests#courseWebhookReceivedBeforeExpiryFulfillsAfterWaitingPastExpiry](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1048)
- [PaymentFlowIntegrationTests#expiredCourseRejectionIsCommittedBeforeAcknowledgementAndStorageFailureIsNotSuccess](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1086)
- [PaymentFlowIntegrationTests#sepayWebhookValidatesEveryTrustedFieldAndIsIdempotent](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1105)
- [PaymentFlowIntegrationTests#rejectedWebhookIsAcknowledgedWithoutProcessing](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1127)
- [PaymentFlowIntegrationTests#successfulWebhookRollsBackLedgerAndArtifactsWhenArtifactCreationFails](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1142)
- [PaymentFlowIntegrationTests#concurrentDuplicateWebhookCompletesWithoutDeadlockOrDuplicateArtifacts](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1164)
- [PaymentFlowIntegrationTests#providerTransactionCannotFulfillTwoPayments](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1229)
- [PaymentFlowIntegrationTests#changedOversizedReplayIsAcknowledgedWithoutReinsertingEvent](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1244)
- [PaymentFlowIntegrationTests#reconciliationHasStrictValidationAndTerminalStates](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1264)
- [PaymentFlowIntegrationTests#reconciliationQueueFiltersSearchesPaginatesAndEnforcesRoles](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1285)
- [PaymentFlowIntegrationTests#managerAndReceptionistCanCancelOnlyPendingOrdersWithAudit](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1312)
- [PaymentFlowIntegrationTests#receiptAndPaymentResultEnforceOwnershipAndRoleScope](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1335)
- [PaymentFlowIntegrationTests#webhookReceiptIsHiddenFromReceptionistButVisibleToManager](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1356)
- [PaymentFlowIntegrationTests#receptionistCanListCashAndBankTransferReceiptsForMember](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1373)
- [PaymentFlowIntegrationTests#paymentEndpointsRejectAnonymousAndWrongRoles](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1400)
- [PaymentFlowIntegrationTests#staffMembershipHistoryUsesOwnershipSnapshotsOrderingAndReconciliation](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1412)
- [PaymentFlowIntegrationTests#staffMembershipHistoryRollsBackScopedExpiryWhenHistoryQueryFails](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1436)
- [PaymentFlowIntegrationTests#staffMembershipHistoryEnforcesSecurityEmptyAndNotFoundContracts](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1454)
- [PaymentFlowIntegrationTests#reportUsesHalfOpenBoundariesAndExcludesTestDataByDefault](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1480)
- [PaymentFlowIntegrationTests#migrationEnforcesSinglePendingPaymentTerminalImmutabilityAndTestFlagInheritance](../../src/test/java/com/scms/backend/membership/payment/PaymentFlowIntegrationTests.java#L1498)

## Withdrawal/transfer/refund

State, ownership, thời hạn, hoàn tiền và không cấp quyền lợi lặp.

### [CourseWithdrawalIntegrationTests](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L59)

- [CourseWithdrawalIntegrationTests#courseOnlyCheckInAndAttendancePersistThroughApiWithTimeAndAssignmentGuards](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L148)
- [CourseWithdrawalIntegrationTests#receptionHistoryRejectsInactiveBeneficiariesAndKeepsRefundPaginationValidation](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L200)
- [CourseWithdrawalIntegrationTests#receptionHistoryMatchesMemberDtoAndEnforcesBeneficiaryOwnership](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L210)
- [CourseWithdrawalIntegrationTests#pendingKeepsCapacityAndExactSeventyPercentApprovalCreatesFlooredRefund](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L227)
- [CourseWithdrawalIntegrationTests#belowSeventyPercentStillWithdrawsWithZeroRefundObligation](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L250)
- [CourseWithdrawalIntegrationTests#rejectionRestoresActiveEnrollmentAndManagerCanSelfApproveOwnRequest](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L261)
- [CourseWithdrawalIntegrationTests#finalUnitBoundaryClosesWindowAndMemberDetailIsIdorSafe](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L272)
- [CourseWithdrawalIntegrationTests#receptionistEnrollmentDiscoveryIsOwnedAndNotAvailableToOtherRoles](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L283)
- [CourseWithdrawalIntegrationTests#concurrentManagerDecisionsCommitExactlyOneOutcome](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L301)
- [CourseWithdrawalIntegrationTests#courseEntryRequiresActiveEnrollmentAndUncancelledRun](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L318)
- [CourseWithdrawalIntegrationTests#courseEntryWaitsForConcurrentRunCancellationAndRechecksEligibility](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L332)
- [CourseWithdrawalIntegrationTests#approvalUsesRequestTimeSnapshotEvenAfterAnotherUnitStarts](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L367)
- [CourseWithdrawalIntegrationTests#validRequestCanBeApprovedAfterRunCompletes](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L376)
- [CourseWithdrawalIntegrationTests#pendingWithdrawalSuspendsRosterAndAttendance](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L384)
- [CourseWithdrawalIntegrationTests#memberCanCancelPendingRequestAndRetryWithoutDuplicatingEffects](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L394)
- [CourseWithdrawalIntegrationTests#cancellationRestoresRightsWithoutChangingHistoryOrGrantingMakeup](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L403)
- [CourseWithdrawalIntegrationTests#cancelOwnershipRolesInactiveAccountsAndTerminalConflictsAreEnforced](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L422)
- [CourseWithdrawalIntegrationTests#cancellationApprovalAndRejectionCompeteInRealTransactions](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L438)
- [CourseWithdrawalIntegrationTests#runCancellationClosesPendingRequestAndOnlyCreatesRunCancellationRefund](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L457)
- [CourseWithdrawalIntegrationTests#snapshotsAreImmutableAndRollbackIncludesAuditOutboxAndEnrollment](../../src/test/java/com/scms/backend/course/withdrawal/CourseWithdrawalIntegrationTests.java#L471)

### [CourseTransferIntegrationTests](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L52)

- [CourseTransferIntegrationTests#manualReconciliationCompletesHigherPriceTransferAndPreservesBothActors](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L134)
- [CourseTransferIntegrationTests#completedTransferKeepsScheduleResponseRetryIdempotentForReception](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L157)
- [CourseTransferIntegrationTests#proxyDifferencePaymentUsesExistingSepayWebhookAndKeepsInitiatorOnReplay](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L187)
- [CourseTransferIntegrationTests#proxyAcceptanceReplaysZeroDifferenceAndPreservesActorAndNotifications](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L211)
- [CourseTransferIntegrationTests#proxyResponsePreservesOwnershipDeadlineAndContinueReplay](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L227)
- [CourseTransferIntegrationTests#proxyOfferCannotCrossOwnersOmitKeysOrAcceptAtExpiry](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L243)
- [CourseTransferIntegrationTests#receptionistRequestsAndAcceptsTransferWithCorrectActorThroughExistingCashPayment](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L262)
- [CourseTransferIntegrationTests#receptionistScheduleWithdrawalCapturesSnapshotAndActorOnce](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L285)
- [CourseTransferIntegrationTests#equalPriceOfferMovesEnrollmentOnceAndPreservesSourceHistory](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L297)
- [CourseTransferIntegrationTests#lowerPriceCompletesAtomicallyAndCreatesOneCreditExcessObligation](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L332)
- [CourseTransferIntegrationTests#higherPriceUsesOnlyDifferenceOrderThenCashFulfillmentCommitsTransfer](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L344)
- [CourseTransferIntegrationTests#expiryReleasesSeatAndKeepsSourceEnrollmentActive](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L360)
- [CourseTransferIntegrationTests#securityKeepsManagerAndMemberSurfacesSeparated](../../src/test/java/com/scms/backend/course/transfer/CourseTransferIntegrationTests.java#L372)

### [CourseRefundObligationIntegrationTests](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L46)

- [CourseRefundObligationIntegrationTests#managerApprovesZeroAmountAndSameDecisionReplayIsIdempotent](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L125)
- [CourseRefundObligationIntegrationTests#duplicateSourceIsIdempotentButChangedReplayConflicts](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L138)
- [CourseRefundObligationIntegrationTests#vndNineteenDigitBoundaryIsStoredAndOverflowIsRejected](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L146)
- [CourseRefundObligationIntegrationTests#completedRefundReducesAvailablePaidAmountAndRejectIsTerminal](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L154)
- [CourseRefundObligationIntegrationTests#concurrentConflictingManagerDecisionsCommitExactlyOneOutcome](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L168)
- [CourseRefundObligationIntegrationTests#managerApiIsRoleProtectedAndQueueIsPaginated](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L184)
- [CourseRefundObligationIntegrationTests#disbursementRequiresCompletionExceptForImmediateSourcesWithoutMutatingRejectedRefunds](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L193)
- [CourseRefundObligationIntegrationTests#ineligibleDisbursementHttpConflictDoesNotMutateRefund](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L224)
- [CourseRefundObligationIntegrationTests#receptionistQueueIsApprovedFilteredEligibleSortedRedactedAndRoleProtected](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L239)
- [CourseRefundObligationIntegrationTests#receptionistRecordsExactlyOneDisbursementAndManagerVerifiesMatchingEvidence](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L262)
- [CourseRefundObligationIntegrationTests#disbursementRejectsConflictingReplayAndVerificationMismatch](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L281)
- [CourseRefundObligationIntegrationTests#memberHistoryIsOwnedAndRedactedWhileRolesRemainSeparated](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L291)
- [CourseRefundObligationIntegrationTests#twoReceptionistsCompetingToDisburseCommitExactlyOneRecord](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L305)
- [CourseRefundObligationIntegrationTests#managerCanFailApprovedObligationOrMarkRecordedDisbursementReturned](../../src/test/java/com/scms/backend/course/refund/CourseRefundObligationIntegrationTests.java#L321)

## Training summary

PT/course, quyền coach/member, thời gian, V77 và rollback kết quả/feedback/audit.

### [BookedSessionTrainingIntegrationTests](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java#L26)

- [BookedSessionTrainingIntegrationTests#migrationAndHistorySupportCurrentCourseSessionsAndRetries](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java#L77)
- [BookedSessionTrainingIntegrationTests#feedbackFailureRollsBackResultAndAuditTogether](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingIntegrationTests.java#L96)

### [BookedSessionTrainingServiceTests](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L44)

- [BookedSessionTrainingServiceTests#ptUsesBookedSessionTimeAndLinksFeedbackToTheSameResult](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L101)
- [BookedSessionTrainingServiceTests#courseUsesEnrollmentWithoutRequiringPersonalCoachingBenefits](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L116)
- [BookedSessionTrainingServiceTests#retryUpdatesExistingPairAndPreservesOtherMeasurements](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L127)
- [BookedSessionTrainingServiceTests#rejectsUnfinishedSession](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L142)
- [BookedSessionTrainingServiceTests#rejectsCancelledSession](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L149)
- [BookedSessionTrainingServiceTests#rejectsMemberOutsideRoster](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L156)
- [BookedSessionTrainingServiceTests#rejectsAnotherCoachsSession](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L163)
- [BookedSessionTrainingServiceTests#rejectsPlanFromAnotherMember](../../src/test/java/com/scms/backend/training/session/BookedSessionTrainingServiceTests.java#L170)

### [SessionTrainingSummaryServiceTests](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java#L28)

- [SessionTrainingSummaryServiceTests#createsBothAndLinksFeedbackToSavedResultInOneTransaction](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java#L55)
- [SessionTrainingSummaryServiceTests#retryUpdatesCanonicalRecordsAndPreservesExistingMetrics](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java#L67)
- [SessionTrainingSummaryServiceTests#feedbackFailureRollsBackCombinedTransaction](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryServiceTests.java#L81)

### [SessionTrainingSummaryControllerTests](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryControllerTests.java#L21)

- [SessionTrainingSummaryControllerTests#coachCanSaveBothFields](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryControllerTests.java#L46)
- [SessionTrainingSummaryControllerTests#rejectsBlankInputWithoutCallingService](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryControllerTests.java#L52)
- [SessionTrainingSummaryControllerTests#memberCannotWriteCoachSummary](../../src/test/java/com/scms/backend/training/session/SessionTrainingSummaryControllerTests.java#L58)

## Email và reset mật khẩu

OTP, manual activation, đổi email, reset và rollback thông báo.

### [EmailVerificationIntegrationTests](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java#L54)

- [EmailVerificationIntegrationTests#registerCreatesEncryptedPendingArtifactsAndOtpActivatesExactlyOnce](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java#L96)
- [EmailVerificationIntegrationTests#changeEmailRequiresCookieCsrfAndOriginAndRotatesSession](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java#L119)
- [EmailVerificationIntegrationTests#receptionistRequestAndManagerApprovalActivatesWithoutMarkingEmailVerified](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java#L130)
- [EmailVerificationIntegrationTests#manualActivationRequestNotifiesOnlyActiveManagersWithoutSensitivePayload](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java#L151)
- [EmailVerificationIntegrationTests#rollingBackManualActivationRequestAlsoRollsBackManagerNotifications](../../src/test/java/com/scms/backend/emailverification/verification/EmailVerificationIntegrationTests.java#L181)

### [PasswordResetIntegrationTests](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java#L35)

- [PasswordResetIntegrationTests#resetUsesOneTimeOtpSessionRevokesRefreshTokensAndDoesNotLogin](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java#L66)
- [PasswordResetIntegrationTests#requestDoesNotRevealWhetherAnAccountExists](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java#L89)
- [PasswordResetIntegrationTests#failedOtpAttemptsPersistAndCurrentPasswordIsRejected](../../src/test/java/com/scms/backend/emailverification/passwordreset/PasswordResetIntegrationTests.java#L95)

### [StaffEmailChangeIntegrationTests](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java#L31)

- [StaffEmailChangeIntegrationTests#currentEmailRemainsUntilOtpVerificationThenRefreshTokensAreRevoked](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java#L64)
- [StaffEmailChangeIntegrationTests#wrongOtpCountsAttemptsAndCancelKeepsCurrentEmail](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java#L89)
- [StaffEmailChangeIntegrationTests#resendSupersedesOldOtpAndKeepsOnlyLatestValid](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java#L103)
- [StaffEmailChangeIntegrationTests#pendingEmailDoesNotReserveAddressAndReplaceSupersedesOldRequest](../../src/test/java/com/scms/backend/emailverification/emailchange/StaffEmailChangeIntegrationTests.java#L115)

## Regex email trước merge

[EmailValidationCompatibilityTests](../../src/test/java/com/scms/backend/manager/EmailValidationCompatibilityTests.java) đối chiếu acceptance cũ/mới ở cả hai validator, normalization, giới hạn độ dài và thông báo lỗi. Xem [phạm vi bổ sung](EMAIL_VALIDATION_FIX.md).
