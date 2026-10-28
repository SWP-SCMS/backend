# SCMS Manager Deep Audit - 28/10/2026

## Kết luận

Đợt audit này **không kết luận Manager regression sạch**. Không có P0 được xác nhận,
nhưng có một P1 về chuỗi cung ứng frontend và hai P2 cần xử lý trước khi coi nhánh
Manager sẵn sàng cho tải thực tế. Các luồng browser, quyền thực tế, tải lớn và race
PostgreSQL chưa thể xác nhận vì không có staging cô lập/credential và local không có
Docker.

Lỗi nghiệp vụ đã biết trước audit đã được sửa và phát hành:

- Backend: Manager ACTIVE nhận một notification
  `MANUAL_ACTIVATION_REVIEW_REQUIRED` cho mỗi manual activation request.
  PR [#163](https://github.com/SWP-SCMS/backend/pull/163), squash commit
  `0ee26b892e62313f9b94b4e19665f8016aa73274`.
- Frontend: notification này hiện có nhãn tiếng Việt và Manager được chuyển tới
  `/manager/email-verification`. PR [#66](https://github.com/SWP-SCMS/frontend/pull/66),
  squash commit `147c895f4387222fc884f0dca5bd36908ff8ade0`.

Không có dữ liệu hay mutation nào được thực hiện trên production.

## Baseline va pham vi

| Muc | Gia tri |
| --- | --- |
| Backend worktree | `C:\Users\ngoct\.codex\worktrees\manager-deep-audit\SWP391` |
| Branch audit | `codex/manager-deep-audit-2026-10-28` |
| Backend baseline | `correct/main` at `0ee26b892e62313f9b94b4e19665f8016aa73274` |
| Frontend baseline | `origin/main` at `147c895f4387222fc884f0dca5bd36908ff8ade0` |
| Working tree before report | clean |
| Requirements | `docs/requirements/BUSINESS_RULES_V4_2_FIXED.md`, `docs/requirements/TASK_LIST_CURRENT.md` |
| Production mutation | none |

Phân loại dùng trong báo cáo: `PASS`, `FAIL`, `MISSING`, `RULE-CONFLICT`,
`BLOCKED`, `UNVERIFIED`. `PASS` chỉ nói phần có evidence; không suy ra E2E hoặc tải
đã đạt khi chưa chạy được.

## Evidence da thu thap

### Gates va CI

| Kiem tra | Ket qua | Evidence |
| --- | --- | --- |
| Backend non-container suite | PASS: 797 tests, 0 failures, 0 errors | Chạy local theo danh sách class không dùng Testcontainers |
| Backend full `mvnw -ntp test` | BLOCKED | Testcontainers không tìm được Docker; failure đầu tiên ở `AuditActorSnapshotMigrationV22Tests`, sau đó các suite container không thể khởi tạo |
| Backend Main CI | PASS | [CI #40712791147](https://github.com/SWP-SCMS/backend/actions/runs/40712791147): unit và integration shard 0-3 xanh tại `0ee26b8` |
| Backend Main CodeQL | PASS | [CodeQL #40712788539](https://github.com/SWP-SCMS/backend/actions/runs/40712788539) xanh tại `0ee26b8` |
| Frontend test / lint / production build | PASS with warnings | `npm ci`, `npm test`, `npm run lint`, `npm run build`; lint có 5 warning dependency/React hiện hữu |
| Frontend Main CI | PASS | [CI #40714547824](https://github.com/SWP-SCMS/frontend/actions/runs/40714547824) xanh tại `147c895` |
| Preview browser smoke | BLOCKED | Vercel Preview redirect tới đăng nhập Vercel; không dùng production để thay thế |
| Staging manager E2E / load / accessibility | BLOCKED | Không có staging cô lập, account test và seed dữ liệu được cung cấp |

Các warning frontend đáng theo dõi: `useMemo` dependencies ở trang staff/member,
dynamic import không có hiệu lực ở `authService.js`, và các warning incompatible
library ở form lịch/lớp. Chúng không được xác nhận là lỗi nghiệp vụ trong audit này.

### Activation notification regression

`ManualActivationService.create` ghi request, audit, và outbox notification trong một
transaction. Nó dùng `findAllByRoleAndStatusOrderByIdAsc(MANAGER, ACTIVE)`; event key
gồm request ID và manager ID. Payload chỉ chứa `requestId`, `memberCode`,
`requestedByName`, `status`, không có email/evidence. `NotificationService.markRead`
tra cứu bằng `(notificationId, recipientAccountId)`, ngăn một Manager đọc notification
của Manager khác.

`EmailVerificationIntegrationTests` kiểm tra fan-out Manager ACTIVE, loại trừ account
INACTIVE/role khác, idempotency của request pending, rollback, payload và ownership.
Browser flow Receptionist -> Manager vẫn `BLOCKED` do Preview được bảo vệ.

## Ma tran truy vet Manager

| Nhom | BR/US chinh | UI/API va implementation | Evidence | Trang thai |
| --- | --- | --- | --- | --- |
| Dashboard | BR-RPT-01, US22 | `/manager/dashboard`, `ManagerReportingController` | controller/service tests; Main CI | UNVERIFIED E2E/load |
| Staff va Manager lifecycle | BR-ACC-03..06, 18..22 | staff pages, `/manager/staff-accounts`, `StaffAccount*Service`, `StaffStatusService` | unit + `ManagerAccountIntegrationTests`, `OperationalIntegrityIntegrationTests` | PASS code; UNVERIFIED local PostgreSQL race |
| Member list/detail/edit/suspend/reactivate | BR-ACC-13, 25 | member pages, `/manager/members`, `ManagerMemberService`, `MemberStatusService` | controller/service tests; common name validation | PASS code; UNVERIFIED E2E |
| Vietnamese name validation | BR-VAL-ACC-01, 03, 09 | `PersonNameRules`, staff create/patch, member manager patch | regex accepts Unicode letters/marks, spaces, hyphen/apostrophe/period; rejects digits/emoji | PASS static |
| Manual activation va notification | BR-ACC-28, BR-AUD-01 | `/manager/email-verification/manual-activation-requests`, notification center | PR #163/#66, integration tests, Main CI | PASS backend; BLOCKED browser E2E |
| Discipline, class, room | BR-CLS-01, 08, 09 | Manager pages; `DisciplineController`, `SportClassController`, `RoomController` | unit/controller/integration suites present | UNVERIFIED scenario E2E |
| Room suspension, preview, apply | room-impact rules | `/manager/rooms/{id}/impact-*`, `RoomSuspensionService` | `RoomImpactIntegrationTests`, `RoomSuspension*Tests` | FAIL performance: F-02 |
| Recurring schedule, session, calendar | BR-REC-01, BR-SES-03, 05, 06, 08 | recurring/session pages, `RecurringScheduleController`, `ClassSessionController` | service/controller/integration suites present | UNVERIFIED E2E/concurrency |
| Coach availability va leave | coach leave rules | `/manager/coaches`, leave services/controllers | CoachLeave service/controller/integration suites | UNVERIFIED E2E/race |
| Coach assignment | BR-COA-02 | coach assignment pages, `ManagerCoachAssignmentController`, `CoachAssignmentService` | service/controller/integration suites | UNVERIFIED PostgreSQL capacity race; coach account row is locked before capacity check |
| Plan, benefit, offer | BR-OFF-01..04 | plan/offer pages, `MembershipPlanAdminController`, `MembershipOfferAdminController` | Main CI and contract tests | UNVERIFIED E2E |
| Freeze va termination | membership lifecycle rules | Manager lifecycle APIs and pages | lifecycle focused suites/Main CI | UNVERIFIED current staging flow |
| Refund workflow | refund rules | refund page, manager/reception endpoints, `MembershipRefund*` | refund service/contract/integration suites | UNVERIFIED E2E/race |
| Payment reconciliation | BR-PAY-12 | reconciliation page, `/payments/**` | authorization mapping/Main CI | UNVERIFIED E2E/manual evidence |
| Reports va revenue | BR-RPT-01 | reports pages, `ManagerReportingService` | unit/controller/integration suites | UNVERIFIED 10k-row performance |
| Audit | BR-AUD-01, 03 | `/manager/audit-events`, `AuditHistoryService` | Main CI and explicit Manager-only security mapping | PASS static; UNVERIFIED pagination/load |
| Notification va profile | notification/profile rules | `/notifications`, manager profile pages | recipient-scoped repository query and frontend mapping tests | PASS code; BLOCKED E2E |
| Authorization and API contract | BR-SEC-04, 05 | `SecurityConfiguration`, service-level active Manager checks, OpenAPI tests | non-container suite and Main CI | UNVERIFIED live role/IDOR matrix |

## Authorization, transaction va data-integrity review

| Kiem tra | Ket qua | Evidence |
| --- | --- | --- |
| Manager endpoints role-gated | PASS static | `SecurityConfiguration` maps Manager paths to `hasRole("MANAGER")`; services additionally call active-manager checks |
| Notification ownership | PASS static/tested | repository uses `findByIdAndRecipientAccountId`; no generic by-ID mutation route found |
| Activation decision concurrency | UNVERIFIED local | request lock uses `SELECT ... FOR UPDATE`; integration race requires PostgreSQL/Docker or staging |
| Last active Manager safeguard | PASS code; UNVERIFIED race | integrity tests exist, but local Testcontainers unavailable |
| Coach capacity concurrent assignment | UNVERIFIED | `CoachAssignmentService` locks coach Account before count, which should serialize same-coach assignment; missing direct PostgreSQL proof test |
| Audit/outbox atomicity | PASS code/tested for activation | manual activation method is transactional; writer uses transactional outbox |
| Direct API malformed/stale/IDOR matrix | BLOCKED | requires isolated target and role tokens |
| Fresh/upgrade Flyway database | BLOCKED local | Testcontainers cannot start |

## Findings

### F-01 - P1 - Known critical/high frontend dependency advisories

- **Type:** security / supply-chain
- **Status:** FAIL (remediation priority); exploitability of each advisory in SCMS remains `UNVERIFIED`.
- **Evidence:** GitHub Dependabot reports 13 open alerts on `SWP-SCMS/frontend` main. Alert #12 is critical for `dompurify` (`SAFE_FOR_TEMPLATES` bypass); additional high alerts affect `dompurify`, `zod`, `follow-redirects` (two), `source-map-js`, and Vite's `launch-editor`.
- **Business impact:** a known vulnerable browser or build dependency can expose users if the affected code path is reachable. The current audit does not claim a working XSS or credential-exposure exploit.
- **Smallest safe remediation:** create a dependency-only frontend PR; upgrade each direct dependency to the GitHub-patched version, regenerate the lockfile, run tests/lint/build, then exercise any DOMPurify-backed content/PDF rendering route. Do not bulk-upgrade unrelated packages.

### F-02 - P2 - Room suspension preview has N-plus-one queries and quadratic overlap work

- **Type:** performance / scalability
- **Location:** `src/main/java/com/scms/backend/scheduling/RoomSuspensionService.java:135-194`.
- **Expected:** preview remains bounded for a large room impact, including the audit target of thousands of sessions.
- **Actual:** `previewImpact` obtains all affected sessions, then calls `roomService.availability` separately for each session (lines 152-160). It subsequently compares every pair for exclusive rooms (179-184) and re-scans every session for each candidate shared room (187-193).
- **Impact:** a long room suspension can generate many repeated availability queries and quadratic CPU work, causing slow pages, timeouts or database pressure.
- **Smallest safe remediation:** introduce a single batch availability query for all affected intervals, then use a sorted interval sweep per candidate room. Add a query-count and 5,000-session integration performance regression test plus an explicit preview-size limit/pagination if the UX needs an upper bound.

### F-03 - P2 - Production API documentation is publicly enabled by default

- **Type:** security hardening / information exposure
- **Location:** `src/main/resources/application.properties:17-20`, `src/main/java/com/scms/backend/config/SecurityConfiguration.java:78`; `application-prod.properties` does not disable Springdoc.
- **Expected:** production API documentation is deliberately controlled by an environment-specific policy.
- **Actual:** `/docs`, `/swagger-ui/**`, and `/v3/api-docs/**` are permitted to anonymous callers in every profile.
- **Impact:** this does not bypass authorization, but exposes endpoint and schema inventory to unauthenticated users and increases reconnaissance value.
- **Smallest safe remediation:** disable Springdoc in production by default and permit it only behind an explicit ops-controlled flag or authenticated internal route. Add a production-profile security test.

### F-04 - P3 - Birth-date validation uses UTC while business dates use Vietnam time

- **Type:** business-rule consistency
- **Location:** `TimeConfiguration.java:12-14`; `StaffAccountService.java:151-156`; `MemberProfileDetailsValidator.java:61-69`; similar uses occur in registration and reception member services.
- **Expected:** a birth date equal to the current business date in `Asia/Ho_Chi_Minh` is accepted consistently.
- **Actual:** the application Clock is `UTC` and these validators call `LocalDate.now(clock)`, whereas membership/freeze/scheduling explicitly use `Asia/Ho_Chi_Minh`.
- **Impact:** during the Vietnam morning before UTC midnight, today's Vietnam birth date can be treated as a future date.
- **Smallest safe remediation:** use a shared business-date clock/`ZoneId.of("Asia/Ho_Chi_Minh")` for date-only validation and add boundary tests around midnight Vietnam time.

### F-05 - P2 - Registration bot protection planned for OTP hardening is not present

- **Type:** missing security hardening
- **Status:** MISSING relative to the agreed OTP hardening plan; not claimed as a regression against BR v4.2.
- **Evidence:** no Turnstile implementation/configuration was found and CORS has no `X-Turnstile-Token` support.
- **Impact:** registration relies on the existing rate limits and can remain a target for automated account/OTP abuse.
- **Smallest safe remediation:** implement the separately approved Turnstile backend/frontend plan, including server-side Siteverify, timeouts, hostname/action validation, CORS and staging test coverage.

## Usability and user-day simulation

The complete Manager day simulation was not run because no isolated staging account/data set was available and Vercel Preview requires authentication. The static review confirms all requested Manager routes exist, including the new `/manager/email-verification` route. The following remain `BLOCKED`, not failures:

- Receptionist creates activation request -> Manager sees unread notification -> approves/rejects in browser.
- Responsive and keyboard/focus behavior at 1440x900, 1280x720, 390x844 and Firefox smoke.
- Vietnamese `dd/MM/yyyy`, VND formatting, loading/error/empty/retry states in browser.
- Manager actions under double-click, retry-after-timeout, stale version and simultaneous Manager use.
- 10,000-member/request and 5,000-session performance target, p95/p99 and `EXPLAIN ANALYZE` evidence.

## Required follow-up order

1. Create a P1 dependency-remediation PR for F-01 and re-run frontend CI plus focused content-sanitization checks.
2. Create a P2 Room preview performance PR for F-02 with a PostgreSQL query-count/load regression test.
3. Create a P2 production-docs exposure policy PR for F-03.
4. Create a P3 business-date consistency PR for F-04.
5. Finish the already-approved Turnstile hardening work (F-05) as separate backend/frontend PRs.
6. Provision isolated staging with synthetic accounts and data, then run the blocked authorization, E2E, accessibility, concurrency and load matrix before release sign-off.

## Audit limits

This report is a code-and-test audit, not a production certification or penetration test. Docker/Testcontainers, staging E2E, Firefox checks, load tests, SQL plans and live role/IDOR calls were not run. No secrets, OTPs, full emails, evidence notes or production records are included.
