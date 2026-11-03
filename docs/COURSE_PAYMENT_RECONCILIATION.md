# Course payment reconciliation (G15)

Backend-only addition after PR #195–199. FE work remains FE_PENDING. No runtime/E2E acceptance is implied by CI.

## API

Paths below use `/api/v1`. Contract: `src/main/resources/openapi/course-payment-reconciliation.yaml`.

- GET `/course-payments/reconciliation-queue?status=ALL&query=COR-...&page=0&size=20`: ACTIVE Manager/Reception; pending SEPAY Course payments only. `PENDING` means still within deadline; `EXPIRED_WINDOW` means deadline reached but expiry has not yet transitioned payment. Terminal payments are available by ID, not in the pending queue. Search payment ID/reference, order reference, member name/code.
- GET `/course-payments/{paymentId}`: staff detail, immutable bank destination, `canConfirmPaid`, `canConfirmFailed`, `blockedReason`. Eligibility is advisory and rechecked under transaction locks.
- POST `/course-payments/{paymentId}/reconcile`: ACTIVE Manager/Reception; required `Idempotency-Key` (1–128 characters). Request fields match PaymentActionRequest. Example:

```json
{"status":"PAID","receivedAmount":500000,"transferContent":"COR-0123456789ABCDEF0123456789ABCDEF","providerTransactionId":"123456789","evidence":"QA statement reference; simulated money","reason":"Manual reconciliation after checking bank evidence"}
```

Use `FAILED` with evidence/reason only for an expired, still-pending payment. Never use PAID to override expiry. Expiry already performed by the scheduler returns 409 for a new decision; the same successful key still replays.

- GET `/course-payments/{paymentId}/result`: ACTIVE staff or owning Member. Contains payment/order/reservation/run/member IDs, amount/status/deadline, and enrollment when paid. No bank destination, evidence or internal decision ledger.

400 PAYMENT_VALIDATION_ERROR: invalid input/mismatched amount or reference. 403 PAYMENT_FORBIDDEN: role/ownership denial (JWT layer can return 401 for inactive accounts). 404 PAYMENT_NOT_FOUND: unknown payment. 409 PAYMENT_CONFLICT: expired/unavailable/terminal payment, reused transaction or changed retry payload. `blockedReason` values: PAYMENT_NOT_PENDING, PAYMENT_METHOD_NOT_TRANSFER, PAYMENT_CURRENCY_MISMATCH, PAYMENT_WINDOW_EXPIRED, COURSE_RUN_NOT_PAYABLE, DESTINATION_SNAPSHOT_MISSING, COURSE_ENROLLMENT_NOT_AVAILABLE.

Persist the original key and exact payload through timeouts. Same actor/payment/key replays the stored result; changing payload returns 409. Different keys/actors cannot duplicate fulfillment. A matching already-paid SEPAY transaction returns the existing result without another business audit or notification. A CASH payment cannot be confirmed as a bank transfer. Existing Membership API behavior is unchanged.

## Data and concurrency

V70 creates a separate decision/retry ledger with evidence and reason; it does not backfill, rewrite, or delete existing payments. Course fulfillment remains shared with webhook/cash, including higher-price transfers. Transfer payments first lock the offer and both Run IDs in stable order; ordinary payments lock Run, order, reservation, payment. The provider advisory lock and cross-domain claim check prevent reusing SEPAY transaction IDs across Membership and Course.

Audit, enrollment/allocation, notification outbox and saved retry response commit together. No direct network notification is sent before commit. Failed transactions leave no saved success. Amount/fees, immutable withdrawal snapshots and refund workflows are unchanged.

## SePay configuration

New QR creation requires SEPAY_BANK_CODE, SEPAY_BANK_ACCOUNT, SEPAY_BANK_ACCOUNT_NAME and SEPAY_BANK_DESTINATION_SHA256. The checksum must match the service's canonical bank destination (trimmed, upper-case `bankCode|bankAccount|bankAccountName`, SHA-256 hex). Missing/mismatched configuration returns 503 PAYMENT_DESTINATION_CONFIGURATION_INVALID. Do not disable this guard or paste bank secrets into PR/logs. Existing payments reconcile against their own destination snapshot, not the current configuration.

## Rollout / limitations

Apply forward migration through the normal deployment process. Prefer a forward fix; retain the ledger and all evidence. No manual deployment, production configuration change, real-money smoke or business-DB test is authorized here. Local Docker unavailable: NOT_RUN_LOCAL_DOCKER. Exact-head PostgreSQL CI is required before merge. API smoke remains SMOKE_UNVERIFIED until an isolated deployment/source SHA and fixtures are available; FE_PENDING is separate from backend completion.
