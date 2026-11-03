# Course Booking: PostgreSQL and process recovery coverage

Baseline 09215bd. No production API, migration, or business rule changes. FE cash fix merged in frontend #74 (43d1a63); runtime UI evidence is a separate follow-up.

| QA | Evidence and limits |
| --- | --- |
| QA-01 | PaymentFlowIntegrationTests: manualCourseTransactionCannotBeReusedByMembershipOrAnotherCourse, courseWebhookTransactionCannotBeReusedByManualMembershipReconciliation, manualMembershipReconciliationTransactionCannotFulfillCourse, concurrentMembershipReconciliationAndCourseWebhookShareProviderClaim. Shared provider claim, both domain directions; not a proof of every possible interleaving. |
| QA-02 | Existing concurrent reconciliation/webhook/cash/expiry tests plus new ordered first-winner parameterized tests for both outcomes. Verify enrollment and expiry audit cardinality; no random sleep to choose a winner. |
| QA-03 | manualCourseAllocationOrOutboxFailureRollsBackThenRetries plus audit/webhook rollback tests. Shared assertions cover payment/order/hold, allocation, outbox and provider claim rollback; same-key retry commits one fulfillment. |
| QA-04 | CourseCrashRecoveryIntegrationTests: force-kill owned JVM before transaction commit (PostgreSQL advisory barrier on audit insert); restart and replay. Also force-kill after independent DB verification of commit while an HTTP test filter withholds response; restart and replay identical result, changed payload 409. Counts: enrollment, allocations, reconciliation audit, outbox, decision ledger. |
| QA-05 | CourseWithdrawalIntegrationTests: courseOnlyCheckInAndAttendancePersistThroughApiWithTimeAndAssignmentGuards, attendanceApiCannotWriteForInactiveRunEvenInsideSessionWindow, pendingWithdrawalSuspendsRosterAndAttendance; CourseCoachSessionIntegrationTests. |
| QA-06 | Withdrawal tests for exact 70%, below 70%, immutable request snapshot, delayed approval after another unit and completed Run, cancellation/approval/rejection race. |
| QA-07 | CourseScheduleChangeIntegrationTests: completion/makeup/legacy/concurrency and automaticallyCompletedRunStillRequiresManagerApprovalBeforeReceptionDisbursement through verify. Additional child-JVM test uses real Spring scheduler and SYSTEM_CLOCK, observes CONFIRMED to COMPLETED and additional cycles without another completion audit. |

## Reproduce

JDK 21, Maven, Docker Linux containers. Use only Testcontainers-created PostgreSQL 17. Never point fixture/test datasource at a business database.

```powershell
mvn '-Dtest=CourseCrashRecoveryIntegrationTests,PaymentFlowIntegrationTests,CourseWithdrawalIntegrationTests,CourseRefundObligationIntegrationTests,CourseRunClosureIntegrationTests,CourseCoachSessionIntegrationTests,CourseScheduleChangeIntegrationTests' test
```

Crash tests run automatically in the existing integration-shard selector (`@Testcontainers`, `*Tests.java`). Logs are under target/course-qa; XML/TXT under target/surefire-reports, uploaded on CI failure. No `mvn clean` (repository tracks a target artifact).

## Runtime launcher for FE

CourseQaEnvironment (test classpath only) owns a new PostgreSQL container and CourseQaApplication JVM. Argument: an empty per-run output directory. It emits environment.json with backend URL, PID, source SHA and synthetic fixture IDs. Set SCMS_QA_BACKEND_SHA to the verified checkout SHA. The owner writes stop to request teardown. It never searches for or kills another server.

Without clock.txt, uses SYSTEM_CLOCK. For temporal tests, atomically replace clock.txt with an ISO UTC instant and label evidence CONTROLLED_CLOCK. JWT timestamp validation uses the same Clock in this test launcher; account status checks remain enabled. No control endpoint or test clock is shipped in the production binary.

The child response barrier requires a test-only X-QA-Barrier header. No production filter is added. PostgreSQL trigger/constraints used for faults exist only inside disposable tests. Initial data may be seeded; tested transitions use normal HTTP/services and real transactions. Email is directed to loopback port 1; bank details and checksum are synthetic. No real transfer or webhook service is contacted.

Runtime FE/BE E2E is not certified by this report. Main CI and final frontend/backend SHA evidence are recorded by the execution ledger after merge.
