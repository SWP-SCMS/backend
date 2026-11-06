# Backend pre-push verification — 2026-11-06

Scope: weekly course-run scheduling, course-image upload, cancellation cascading, and refunds based on original purchased session allocations.

- Remote main verified at 12fa1df before publication; local branch already contains it.
- PASS: 76 selected tests, zero failures/errors/skips; Maven BUILD SUCCESS.
- Test classes: CourseWeeklyScheduleTests, CourseWeeklyRunServiceTests, CourseWeeklyRunControllerTests, CourseManagementServiceTests, CourseControllerTests, CourseSessionServiceTests, CourseRunTests, CourseImageControllerTests, CourseUnitRefundsTests, CourseWithdrawalPolicyTests, CourseTransferPolicyTests, CourseRefundStateMachineTests.
- PASS: git diff --check.
- This run does not re-run PostgreSQL/Testcontainers integration tests or Chrome UI tests. Historical QA and its limitations are recorded in the course handoff/refund documents.
- CourseManagementServiceTests now pass in this run; this supersedes the earlier report of a stalled service-test process.
- Migration V74 cancels sessions of cancelled runs. V75 adds original-unit refund claims and reduction-event tracking. Existing approved/paid historic obligations are not recalculated.
- Runtime uploads under data/ and the generated target/ report deletion are excluded from this commit.
- Publication target: feature branch fix/t13-t24-qa only; no merge into main.
