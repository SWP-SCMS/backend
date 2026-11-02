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
- Reception proxy actions are described below. Full smoke remains unverified: the user has no isolated test environment.

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

## Atomic Run rescheduling
- Use `POST /manager/course-runs/{runId}/reschedule` with `Idempotency-Key` (1–100 nonblank characters), the current Run `expectedVersion`, final `plannedStartAt`, `plannedEndAt`, `totalUnits`, and `sessions[]`. Each listed session requires `sessionId`, `expectedVersion`, `startAt`, `endAt`; omit `roomId`/`coachId` to retain assignments. Unlisted sessions retain their schedule. Existing PATCH endpoints remain supported.
- Response: `{run, sessions, changeEventId}`. The session list contains the final Run timetable, including historical/removed entries and original/makeup links. Render status and links rather than dropping history.
- The backend locks all old/new rooms and Coaches in the shared stable order, then the Run and sessions; it reads versions again under the locks. It validates the final timetable before writing. Swapping session times is supported. Started sessions cannot move; reduction can only remove unstarted effective tail units. Missing/foreign sessions return 404; stale Run/session versions, resource conflicts, or an unavailable response deadline return 409. Invalid windows, duplicate session IDs and invalid unit counts return 400.
- Resource conflicts (Course/gym, Coach leave, room suspension, capacity) reject the entire operation. Member conflicts use the existing G10 response/withdrawal policy, calculated against the final timetable; old positions of moving sessions are ignored. Reduction and Run-start delay over 7 days retain their existing cause precedence. Material changes and deadlines retain the existing Vietnam timezone/72-hour and new-start-minus-24-hour rules.
- One operation creates one change event. Responses retain real before/after session times and share that event; pending affected responses are superseded. Members receive one summary outbox notification each after commit. Submitted withdrawal snapshots never change.
- Keep the same key and **identical payload** after a timeout or lost response. The saved response is returned even though its expected versions are now stale. The key is scoped to the acting Manager and Run; changing the payload under that key returns `COURSE_RESCHEDULE_IDEMPOTENCY_CONFLICT`. A new intended edit needs a new key and freshly read versions. Failed transactions save no key or partial schedule.
- Example (replace IDs and versions with current API values):
```json
{"expectedVersion":3,"plannedStartAt":"2035-01-07T09:00:00+07:00","plannedEndAt":"2035-01-07T10:00:00+07:00","totalUnits":1,"sessions":[{"sessionId":"<session UUID>","expectedVersion":1,"startAt":"2035-01-07T09:00:00+07:00","endAt":"2035-01-07T10:00:00+07:00"}]}
```
- V69 adds only `course_run_reschedules` (fingerprint, event link, saved result). Existing data and PATCH callers remain compatible. Keep this table and replay keys on rollback; do not roll back earlier than the V68-compatible binary. Forward-fix schema issues with a new migration. Migration tests cover fresh creation and upgrade from V68; the existing historical payment/attendance/refund preservation test also upgrades through V69.
- Full backend smoke and UI/E2E remain unverified until an isolated deployment with the merged source SHA and updated FE is available. FE must replace the temporary expand–move–shrink sequence with this endpoint.

## Reception actions (G14)

All paths below start with `/reception/members/{memberId}` (under the deployment's `/api/v1` prefix). The authenticated account must be an active RECEPTIONIST; the beneficiary must be an active MEMBER. Use the beneficiary UUID from existing Member discovery. Never substitute the Reception UUID into a Member self-service URL.

| Method and suffix | Result / behavior |
| --- | --- |
| `GET /course-waitlists` | Same list of `CourseWaitlistResponse` as the Member history |
| `POST /course-runs/{runId}/waitlist` | 201; join only when the Run is full and waitlisting is open |
| `DELETE /course-runs/{runId}/waitlist` | 200; leave the beneficiary's WAITING/INVITED entry |
| `POST /course-waitlists/{waitlistId}/accept` | `Idempotency-Key`; 201 for a new Course order, 200 for its replay |
| `GET /course-schedule-changes` | Same `ScheduleChange[]` as Member self-service |
| `POST /course-schedule-changes/{responseId}/response` | 200; body `{"response":"CONTINUE"}`, `REQUEST_WITHDRAWAL`, or `REQUEST_TRANSFER` |
| `GET /course-transfer-offers` | Same `TransferOffer[]` as Member self-service |
| `POST /course-transfer-offers/{offerId}/accept` | `Idempotency-Key`; 200 with completed offer or difference order |
| `GET /course-withdrawal-requests` | Same immutable snapshot/history DTOs as Member self-service |
| `GET /course-withdrawal-requests/{requestId}` | Same `WithdrawalRequest`; wrong beneficiary is 404 |
| `GET /course-refund-obligations?page=0&size=20` | Same redacted `MemberRefundObligationPage` (size 1–100, page >= 0) |
| `GET /course-refund-obligations/{obligationId}` | Same redacted `MemberRefundObligation`; wrong beneficiary is 404 |

Existing Reception withdrawal create/cancel endpoints remain the shared G12/G13 workflow. The refund history includes all workflow states, while `/reception/course-refunds` remains the approved disbursement queue. History does not expose bank/evidence data or staff verification details. None of these actions grants Manager review/approval privileges or access to training plans/results.

- Reception and Member actions use the same ownership, deadline, capacity and enrollment checks. Wrong role: 403. Inactive actor or beneficiary: 401 `INVALID_TOKEN` under the existing active-account policy. An ID owned by a different beneficiary: 404. Invalid pagination/body/key: 400. State/deadline/capacity/key conflict: 409. Render the returned `code`; do not infer success from a timeout.
- Waitlist join/leave do not use a key. A duplicate join remains `COURSE_WAITLIST_DUPLICATE`; repeating a completed leave has no active entry (404). An accepted invitation always returns its existing order; retries never create another order. The order key is scoped to the acting account, and reuse for another Member/Run is rejected by existing Course order validation.
- Schedule responses use the response ID and choice for retry. The same choice returns the recorded result, another choice returns `COURSE_SCHEDULE_CHANGE_RESPONSE_CONFLICT`. New responses must precede `responseDeadline`. A withdrawal response records the Reception requester and the immutable submission snapshot in the same transaction. Its PENDING enrollment retains a seat and immediately suspends Course learning.
- Transfer acceptance requires a nonblank key of 1–100 characters. Retry with the same key returns the saved offer/order; another key after acceptance returns `COURSE_TRANSFER_IDEMPOTENCY_CONFLICT`. A zero/lower price target completes without a new payment. Any excess credit uses the existing refund obligation workflow.
- If `status=PENDING_PAYMENT`, use the returned `differenceOrderId`. Reuse `GET /reception/members/{memberId}/course-orders/{orderId}` and `POST .../payments/sepay` or `POST .../payments/cash` with their existing keys. Charge only `differenceVnd`; do not create another full tuition order. Cash/SePay deadline, replay and fulfillment rules remain unchanged.
- Audits and new orders identify the actual acting Reception account. Target enrollments retain that initiating actor even when a different cashier completes payment. `processed_by_account_id` is the cashier, or null for the automatic SePay webhook. Member IDs/notification recipients remain the beneficiary. Transfer completion payloads add `actorAccountId` (nullable for the webhook), `initiatorAccountId`, and `memberAccountId`; existing target IDs remain.
- PR5 adds no migration and uses the existing actor/audit/outbox columns. Legacy records are preserved, without assigning invented historical actors. Disable the new UI actions before a binary rollback; keep V68/V69 and their compatible readers/writers.

## Atomic schedule notification payload for FE

The atomic endpoint sends one `COURSE_SCHEDULE_CHANGED` notification per affected Member, targeting `COURSE_RUN`. It carries `changeEventId`, `courseCode`, `runCode`, and `changes[]`. Each item has `courseSessionId`, nullable `recordId`, `oldStartAt`, `oldEndAt`, `newStartAt`, `newEndAt`, `cause`, and nullable `responseDeadline`. A null `recordId` means no response is required for that item. Use each non-null record to load/respond through the Member or Reception schedule-change API. Existing single-session PATCH notifications continue to use their original payload. Render instants in `Asia/Ho_Chi_Minh` and never show an intermediate expand/move/shrink timetable.

## Final smoke checklist and acceptance boundary

**Current environment result: `SMOKE_UNVERIFIED` / `E2E_UNVERIFIED`.** The user has no isolated smoke deployment. Local PostgreSQL execution is `NOT_RUN_LOCAL_DOCKER`; PostgreSQL migration/concurrency tests run in the required CI shards. Unit/contract/integration CI is evidence for backend behavior, not a claim that deployment, browser screens or real bank transactions have been accepted.

Before running this checklist, verify the deployed source SHA equals the final merged squash SHA, record the deployment ID, and use an isolated database with fake data plus Manager, Reception, Coach and two distinct Member accounts. Never target the business/production database. Use synthetic cash and a mock/sandbox SePay callback; no live payment is required.

| Check | Expected evidence | Current result |
| --- | --- | --- |
| Identity/version | Source SHA, deployment ID, schema through V69 and account roles recorded | NOT_RUN — no environment |
| Course creation/access | Reception direct create 403/no side effects; Manager creates; Membership entry independent | NOT_RUN |
| Withdrawal snapshot | Submit at 7/10, approve at 6/10 or after Run completion: saved 560,000 on 700,000 basis; new late request blocked | NOT_RUN |
| Pending/cancel/reject | Seat held, check-in/attendance blocked; cancel/reject restores learning without makeup; concurrent decision produces one terminal result | NOT_RUN |
| Run cancellation/legacy | Pending requests close; no duplicate refund; V68 legacy cancellation retains history and requires a new eligible request | NOT_RUN |
| Completion | Effective makeup/end times honored; obligations precede COMPLETED; scheduler retry leaves one effect; Manager approval still required for disbursement | NOT_RUN |
| Atomic reschedule | Multi-session time swap, final resource conflicts, Member conflict cause, stale version, deadline and injected failure; rollback leaves no partial schedule/event/outbox | NOT_RUN |
| Retry after lost response | Same key/payload returns saved Run/event; another payload conflicts; one summary notification per Member | NOT_RUN |
| Reception equivalence | Proxy waitlist, response, transfer and history match Member rules; wrong Member/role and inactive accounts denied; actual actor retained | NOT_RUN |
| Difference payment | Only difference charged; mock callback/cash retry creates one enrollment; initiator/cashier/automatic processor distinguished | NOT_RUN |
| UI/E2E | Updated FE handles all fields, errors, suspension, notification summary and atomic endpoint; four-role flows verified in browser | NOT_RUN — FE outside this implementation |

The Git completion state may be recorded as `MERGED + MAIN_CI_PASS + SMOKE_UNVERIFIED` only after all five PRs and their exact-squash Main CI/security gates succeed. Backend acceptance additionally needs the API smoke above; full product acceptance additionally needs the FE/UI checks. No manual deploy, production configuration change or database reset is part of this work.
