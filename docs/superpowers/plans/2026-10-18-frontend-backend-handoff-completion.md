# Frontend–Backend Handoff Completion Plan

## Context and constraints

- Source handoff: `C:/Users/ngoct/Downloads/frontend-required-backend-api-gaps.md`.
- Baseline: `origin/main@d3ead30`.
- Deliver five independent pull requests, developed sequentially with TDD and subagent review.
- Preserve backward compatibility, use stable lock ordering, avoid N+1 queries, add no dependency, and keep static/generated OpenAPI aligned.
- Docker is unavailable locally; run all non-Docker checks locally and require GitHub CI/Testcontainers acceptance before declaring a PR merge-ready.
- Do not merge automatically.

## PR 1 — Scheduling and Room Impact Integrity

Branch: `codex/fix-scheduling-impact-integrity`

### Task 1 — Bulk cancellation consistency

- Lock eligible sessions in stable ID order for filter requests.
- Lock every requested ID in stable order for explicit selections.
- Return idempotent `200` with `cancelledCount=0` when all requested sessions are already cancelled.
- Return atomic `409 SESSION_BULK_SELECTION_CHANGED` with eligible/ineligible IDs for a mixed stale selection.
- Keep booking release, audit, and notification outbox through the shared cancellation service.
- Test bookings/audit/notifications, mixed selection, retry, and concurrent cancellation.

### Task 2 — Room Impact transaction and locking

- Normalize `MOVE|RESCHEDULE|CANCEL` once before validation and locking.
- Collect source and target room IDs and lock all rooms in UUID order before locking sessions.
- Reject RESCHEDULE into the pending suspension interval with `ROOM_SUSPENSION_CONFLICT`.
- Block permanent disable when any affected session is `IN_PROGRESS`, returning `ROOM_HAS_IN_PROGRESS_SESSION` and IDs.
- Keep CANCEL limited to `SCHEDULED` sessions.
- Validate MOVE target status, capacity, suspension, and schedule overlap while excluding the moved session itself.
- Test valid move, self-conflict, overlap, capacity, whitespace/case action, opposing room moves, and rollback on a late failure.

### Task 3 — Error contract and OpenAPI

- Return structured MOVE conflict details: session, target room, start/end, and conflicting session when known.
- Update scheduling endpoints and ProblemDetail schemas in static OpenAPI and route/schema parity tests.

## PR 2 — Room Suspension Extension

Branch: `codex/feat-room-suspension-extension` after PR 1.

- Add `POST /api/v1/manager/rooms/{roomId}/suspensions/{suspensionId}/extension-preview`.
- Add `PATCH /api/v1/manager/rooms/{roomId}/suspensions/{suspensionId}` with MOVE/RESCHEDULE/CANCEL resolutions.
- Allow only ACTIVE Managers; verify suspension ownership and that it has not ended.
- Require the new end time to exceed the current end time; impact interval is `[oldEndTime,newEndTime)`.
- Preview impacted sessions and booking counts; apply requires exactly one resolution per impacted session.
- Block extension for `IN_PROGRESS` sessions and reject reschedules inside the full extended suspension.
- Lock suspension, all source/target rooms, and sessions in stable order; update and resolve atomically.
- Audit `ROOM_SUSPENSION_EXTENDED`; notify affected Coaches and Members.
- Map exclusion violations to `ROOM_SUSPENSION_CONFLICT`.
- Test preview, no-impact, each resolution, conflicting/expired suspensions, concurrency, stale preview, and rollback.

## PR 3 — Staff Membership and Receipt Contracts

Branch: `codex/feat-staff-membership-receipt-contracts`.

- Add Receptionist membership history by member code and Manager membership history by account UUID.
- Permit Receptionist access to ACTIVE/SUSPENDED Members and Manager access through account identity; return 404 for unknown Member and `[]` for no history.
- Reconcile expired memberships and assignments before reads.
- Return immutable membership snapshots newest first (`startsAt DESC, id DESC`).
- Add `planCode` to `ReceiptResponse` from `membership_orders.plan_code_snapshot`; retain `offerName` and existing receipt authorization.
- Test snapshot lifetime, retry, role/ownership matrix, unknown/empty history, and just-expired membership.

## PR 4 — Assignment Operations, Candidate Queue, and Capacity

Branch: `codex/feat-assignment-operations`.

- Add Flyway V18: nullable positive `accounts.maximum_member_count` and partial index supporting active PLUS membership candidate/workload reads; no backfill.
- Extend Manager staff-account PATCH with Coach-only nullable `maximumMemberCount`, rejecting limits below active assignment count and auditing before/after.
- Add paginated Manager candidate queue with search, assignment status, ACTIVE account and ACTIVE PLUS eligibility, deterministic ordering, and one projection query.
- Add Manager Coach workloads with grouped counts, capacity, availability, search, and deterministic ordering.
- Enforce capacity by locking Coach before counting during assignment mutation.
- Add `GET /api/v1/members/me/coach-assignment`; reconcile expiry and return 204 when absent.
- Test nullable/zero/full capacity, last-slot concurrency, expiry, pagination/search/order, role/IDOR, and audit.

## PR 5 — Coach Training Reads and Contract Hardening

Branch: `codex/feat-coach-training-reads`.

- Add paginated Coach reads for a personally assigned Member's plans, results, and feedback.
- Authorize an ACTIVE Coach with an ACTIVE assignment and unexpired PLUS membership; return scope-style 404 otherwise.
- Let the current Coach read complete Member history, including prior Coaches and other teaching Coaches.
- Add Session-scoped result/feedback GETs using the existing write authorization: teaching Coach owns Session and Member belongs to Session; return 204 when absent.
- Add nullable `classSessionId` to result/feedback responses, including POST/PUT responses.
- Reuse Member read DTOs/projections; order pages by `createdAt DESC,id DESC`, max size 100.
- Add explicit Coach GET security matchers and update static/generated OpenAPI parity.
- Test current/other/inactive Coach, ended assignment, expired PLUS, old history, Session owner/member scope, pagination, and nullable Session identity.

## Per-task execution protocol

1. Create a bounded task brief and dispatch one implementation worker in the isolated worktree.
2. The worker writes a focused test, demonstrates the expected RED failure, implements the minimum complete change, demonstrates GREEN, self-reviews, commits, and writes a report.
3. Dispatch an independent spec/correctness reviewer using a generated review package.
4. Return findings to the same worker for up to five scoped fix/re-review rounds; record every round and ruling in the SDD ledger.
5. Dispatch a test analyst for focused integration, migration, security/IDOR, and concurrency coverage.
6. Run `git diff --check`, `mvnw -DskipTests package`, focused tests, and the full locally available suite.
7. Push, create and attach the PR, then use the babysit workflow until CI is green, the PR is mergeable/clean, and review comments are resolved.
8. Start the next PR only from the latest merged `main` (or explicitly document a necessary stacked dependency).

## Final acceptance

- No expected validation path in bulk cancellation or Room Impact returns `INTERNAL_ERROR`.
- Concurrent bulk cancellation and opposite-direction room moves do not deadlock.
- Sessions cannot be rescheduled into a pending or extended suspension.
- Candidate/workload reads reconcile expiry and do not cause N+1 queries.
- Coach capacity cannot be exceeded under concurrency.
- Coach, Member, Receptionist, and Manager reads enforce role and ownership boundaries.
- Receipts expose immutable `planCode` and `offerName` snapshots.
- Static OpenAPI, generated OpenAPI, controller routes, and schemas match.
- Every PR is green in CI, has no unresolved review comments, and is merge-ready.
