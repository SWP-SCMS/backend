# Course Booking remediation and FE handoff

Scope: backend and API contracts. The five sequential changes are access policy, withdrawal snapshots/cancellation, automatic Run completion, atomic rescheduling, and Reception actions.

## Access policy
- Only Manager creates Course/Run. `POST /reception/courses` is forbidden (403) and removed from the creation contract.
- Existing Manager approval endpoints remain available for historical submissions; existing records are preserved.
- Course check-in requires an ACTIVE enrollment and a Run that is not CANCELLED, in addition to the existing session status and half-open time window. Pending withdrawals, withdrawn/transferred/completed enrollments cannot grant Course entry.
- Membership entry remains independent. FE hiding an action does not replace API authorization.

## Verification and rollout
- Regression first: Reception creation returned 201 instead of 403; the Course entry query lacked enrollment/Run guards.
- PostgreSQL tests cover ACTIVE versus pending/withdrawn/transferred/completed enrollment and cancelled Run eligibility.
- Static and generated OpenAPI omit the retired submission operation. Legacy approval tests seed historical submissions directly.
- Access policy has no schema change. No manual deployment.
- Atomic reschedule and Reception proxy actions follow in separate sequential PRs. Full smoke remains unverified: the user has no isolated test environment.

## Withdrawal snapshots and cancellation (G12-G13)
- Every new withdrawal stores its cause, capture time, total/remaining units, refund basis, eligibility/reason, and refund amount in the submission transaction. Member, Reception, Manager, and schedule-change responses share the same writer. Existing 70%/80% and G10 exceptions remain unchanged.
- Example: submit with 7/10 units remaining and 700,000 VND unused allocation: saved refund 560,000 VND. Review with 6/10 remaining, or after Run completion, still uses 560,000 VND. The last unit starting prevents new requests only.
- `snapshotSource=REQUEST_TIME` identifies new immutable values. `LEGACY_APPROVAL` identifies historical approval values; `UNAVAILABLE` means no snapshot exists. Never replace unavailable values with today's estimate. `snapshotWithdrawalEligible`/`snapshotWithdrawalReason` describe the captured policy; `withdrawalEligible` and `canCancel` describe current request actions. The existing `estimatedRefundAmount` mirrors the saved amount while pending.
- A pending request keeps the seat but immediately suspends Course check-in, roster participation and attendance. Enrollment timelines and Reception discovery expose `learningSuspended`. Preserve historical sessions and attendance in the UI.
- Cancel with a nonblank `reason` (maximum 1,000 characters):
  - `POST /members/me/course-withdrawal-requests/{requestId}/cancel`
  - `POST /reception/members/{memberId}/course-withdrawal-requests/{requestId}/cancel`
- Both return the updated withdrawal DTO (200). Repeat cancellation returns the original cancellation metadata; cancelling APPROVED/REJECTED returns `409 COURSE_WITHDRAWAL_INVALID_TRANSITION`. Wrong owner is 404; wrong role/inactive actor is denied.
- Cancellation metadata: `cancelledAt`, `cancelledByAccountId`, `cancellationReason`. Cancellation/rejection restores learning from processing time and keeps the seat. No makeup is created and expired attendance windows stay closed. Approval alone releases the seat.
- Decision transactions lock Member, Run, then request/enrollment and conditionally update PENDING. Audit, refund obligation and notification outbox commit together. Retrying cannot add another terminal effect.
- Run cancellation closes pending requests with `COURSE_RUN_CANCELLED` and creates the existing Run-cancellation refund; it cannot also approve the pending withdrawal refund. The existing total-refund cap and payment workflow remain in force.

## Migration V68 and rollout
- `V68__course_withdrawal_snapshot_and_cancellation.sql` expands the schema, permits CANCELLED, requires complete snapshots for new writes, and prevents later snapshot mutation.
- Existing PENDING requests missing a request-time snapshot become CANCELLED with `LEGACY_SNAPSHOT_MISSING`; restore the corresponding pending enrollment, preserve history, and write one audit/outbox message. Already approved/rejected requests, payments, attendance, and paid refunds are preserved. No historical snapshot is fabricated.
- A retry of an old schedule response returns its closed request. Resubmission is a new withdrawal operation, subject to the current deadline and policy; FE must explain that the amount may differ.
- Upgrade tests cover a fresh DB and V67 to latest with legacy rows and paid refunds. Local PostgreSQL integration: `NOT_RUN_LOCAL_DOCKER`; both CI events must pass all four PostgreSQL shards before merge.
- Apply migration with the compatible binary; old writers do not supply snapshots and will fail the new integrity checks. Do not overlap old/new writers or roll back to an old binary that does not understand CANCELLED. Forward-fix with a new migration; never modify an applied migration.
- No manual deployment was performed. `SMOKE_UNVERIFIED` and `E2E_UNVERIFIED` remain explicit until an isolated environment and matching FE are available.

## Automatic Run completion
- The existing session-disruption scheduler processes at most 100 actionable Runs per transaction, locks Runs before sessions, and skips Runs already locked by another worker. Missing units and effective sessions that have not ended prevent completion, including legacy SETTLEMENT_PENDING Runs.
- A replacement session supplies the effective end time, even if the makeup is earlier than its cancelled original. Removed tail units do not count toward the final schedule. Orphan active future sessions also prevent completion.
- Unserved units are resolved under the existing policy and their consolidated UNRESOLVED_SESSIONS obligations are recorded before the Run becomes COMPLETED. Existing obligations, approval/payment state and enrollment lifecycle are preserved.
- COMPLETED describes delivery of the Run, not payment of refunds. Manager approval and Reception disbursement remain mandatory; ordinary approved refunds can now proceed after automatic completion. G01 immediate sources remain unchanged.
- Pending withdrawal snapshots do not block completion and remain reviewable afterwards. Do not hide historical attendance or disable its existing final-session time window merely because the Run is COMPLETED.
- Completion emits one audit and one transactional outbox event per enrolled Member. Retry/concurrent execution does not duplicate completion effects. New sessions cannot be inserted into completed or cancelled Runs.
- No schema migration or new endpoint is needed. Existing CourseRunStatus contracts now explain automatic completion. Operational rollback is a forward fix on the V68-compatible binary; completed Runs must not be reset just to restore the previous stuck state.
