# SDD ledger — plan: user message "Kế hoạch tự động hoàn tất tiến trình còn lại"

## Fixed workflow

- Execute Progresses 3–9 sequentially from merged `origin/main`.
- Each progress requires TDD, focused tests, full local test, `git diff --check`, rebase, repeat tests, Push-CI, PR-CI, squash merge, Main CI, and post-merge smoke.
- Docker/Testcontainers may be unavailable locally; every GitHub integration shard remains mandatory.
- Preserve untracked `docs/SCMS_BACKEND_CHANGE_REPORT_2026-10-17.md` and `docs/superpowers/plans/2026-10-17-swp391-sheet-follow-up.md`.

## Completed before this checkpoint

- Progress 3: complete — PR #84, merge `1804ab27ec27c8f9a5b10145a1a54e744e0a3dcc`.
- Progress 4: complete — PR #85, merge `990969ac174d012d11b8687730639d9a41f4c2ac`.
- Progress 5: complete — PR #86, merge `acc7a016de8180fc16d47e2f61e48855e546f861`.

## Progress 6: complete

- Final branch commit: `4b005e4913027d6d6d3d3deaf170c0902d6935f4`.
- PR: https://github.com/SWP-SCMS/backend/pull/87
- Squash merge: `10f574481c349b5f66206d59823c86e30b34e740`.
- Push-CI `39163184257`: success, unit plus four integration shards.
- PR-CI `39163187282`: success, unit plus four integration shards.
- Main CI `39163723773`: success, unit plus four integration shards.
- Pre-merge focused: 17/17 passed.
- Full local: 519 tests, 0 failures, 49 Docker-only errors.
- Post-merge focused smoke: 17/17 passed.
- Independent final review: no Critical or Important findings.

## Progress 7: complete

- Base: `origin/main@10f574481c349b5f66206d59823c86e30b34e740`.
- Branch: `codex/task-7-coach-training-benefits`.
- Scope: Coach Assignment requires `PERSONAL_COACHING`; Training Plan/Feedback/Result mutations require their matching Benefits while historical reads remain readable.
- Read-only scope mapping dispatched separately for assignment and training paths.
- Focused baseline: 50/50 passed across assignment discovery/assignment and personal/session training service tests.
- Implementation worker: `01a150bf-a0ae-73c2-bf8f-1d6392cd2ff3` (Linnaeus), exclusive Progress 7 code scope.
- Ruling: keep existing public `*_PLUS_MEMBERSHIP_REQUIRED` codes for FE compatibility, but make eligibility Benefit-based and update descriptions — cost if wrong: new clients see a legacy-named error code.
- Ruling: apply `TRAINING_FEEDBACK` and `TRAINING_RESULT` to both personal and session write paths because the plan names the domain capability, not a specific endpoint — cost if wrong: custom Plans may need these Benefits for group-session coaching records.
- Ruling: do not broaden Coach historical-read authorization; Member history already remains readable after Membership expiry/Plan inactivity — cost if wrong: a former Coach cannot use Coach endpoints after assignment expiry, avoiding an unrequested data-access expansion.

### Progress 7 implementation/verification checkpoint

- Implementation complete on `codex/task-7-coach-training-benefits`.
- Added Benefit-based assignment reconciliation/discovery and training mutation guards; preserved legacy public error codes and historical member reads.
- Added `TrainingBenefitEligibilityIntegrationTests` plus focused unit/OpenAPI coverage.
- Focused OpenAPI assertions: 20/20 passed; selected service/unit tests passed.
- Full local test: 523 tests, 0 failures, 50 Docker/Testcontainers-only errors because Docker is unavailable locally.
- `git diff --check`: passed.
- Report: `.superpowers/sdd/dynamic-membership-completion/task-7-report.md`.
- Remaining gates: fetch/rebase, repeat tests, commit/push, Push-CI, PR-CI, merge, Main CI, post-merge smoke, then Progress 8.

### Progress 7 complete

- Fix commit: `83c779e`.
- PR: https://github.com/SWP-SCMS/backend/pull/88.
- Squash merge: `71a783a`.
- Push-CI after fix: `39174732630` — success, unit plus four integration shards.
- PR-CI after fix: `39174735668` — success, unit plus four integration shards.
- Main CI after merge: `39175219818` — success, unit plus four integration shards.
- Post-merge smoke on `main`: focused assignment/training service and OpenAPI suite passed; 16 selected classes, 78 tests, 0 failures/errors.
- Local full test after fix: 523 tests, 0 failures, 50 Docker/Testcontainers-only errors because Docker is unavailable locally.
- Progress 8 may now start from `origin/main@71a783a`.

## Progress 8: complete

- Base: `origin/main@3017b7180f1fbea5b792e53a5ad4aef3277893d2`.
- Commit before merge: `4f2f02f`.
- PR: https://github.com/SWP-SCMS/backend/pull/89.
- Squash merge: `03df03418b401285743a65504a0f2484aa27eac9`.
- Scope: reject new Yoga Discipline/Class/Session/Recurring Schedule mutations while preserving legacy Yoga reads/history; document legacy `YOGA` OpenAPI response semantics.
- Push-CI: runs `39180446790` and `39180449760`, both success (unit plus four integration shards).
- PR-CI: same required CI runs above, both success.
- Main CI after merge: `39180902366`, success (unit plus four integration shards).
- Focused pre-merge: 51/51 passed after rebase and fixture correction.
- Full local after rebase: 526 tests, 0 failures, 50 Docker/Testcontainers-only errors because Docker is unavailable locally.
- Post-merge smoke on `main`: 51/51 passed.
- CI initially exposed mutation fixtures incorrectly using legacy Yoga; corrected them to Pilates/GROUP on the same PR before merge. Historical Yoga fixtures were retained.
- Report: `.superpowers/sdd/dynamic-membership-completion/task-8-report.md`.

## Progress 9: complete

- Final regression completed on merged `main` at `bdfe8df`.
- OpenAPI assertions cover Staff profile, Reception SePay, Membership Benefit/Plan, Audit actor identity, Center Visit `memberName`, legacy Yoga semantics, Coach/Training reads and Membership eligibility contracts.
- Direct code inspection confirmed the Excel/MD items are implemented, including session min/max duration, room impact/cancellation flows, class/discipline deactivation guards, profile image upload, dynamic Plan/Benefit, Staff profile, SePay, Audit actor fields and Center Visit member name.
- CI evidence: PR #89 Push-CI and PR-CI green; Main CI after feature merge `39180902366` green; final ledger Main CI `39181508091` green. The preceding ledger-only CI `39181491649` also completed green.
- Final focused smoke: 51/51 scheduling/Yoga tests passed on `main`.
- Full local suite evidence: 526 tests, 0 failures, 50 Docker/Testcontainers-only errors because Docker is unavailable locally; GitHub integration shards provide the required container coverage.
- Workbook discrepancy recorded: the Pictures workbook marks Room Impact as `fail`, while the PRN212 workbook marks it `done`; merged code and Main CI integration tests pass the room-impact/cancellation flows. No production change was made solely to reconcile stale sheet status.
- Preserved known untracked files without staging or deletion.
