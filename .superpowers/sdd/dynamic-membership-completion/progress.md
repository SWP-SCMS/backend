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

## Progress 7: in progress

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
