# SePay destination security deployment gate

Deployment is blocked until every item below is complete and the evidence is signed. Store evidence in the restricted deployment record; do not put full bank details, webhook credentials, or customer data in this repository, logs, or screenshots.

## Deployment record

- Environment: __________
- Release/commit: __________
- Change ticket: __________
- Deployment window (UTC): __________
- Evidence location (restricted): __________
- Operator: __________  Timestamp: __________

## 1. Read-only legacy inventory

- [ ] Run the following query with a read-only database identity and export the result to the restricted evidence location.

```sql
select p.id, p.order_id, o.order_number, o.expires_at,
       p.bank_code_snapshot, p.bank_account_number_snapshot, p.bank_account_name_snapshot
from payments p
join membership_orders o on o.id = p.order_id
where p.provider = 'SEPAY' and p.status = 'PENDING'
  and (o.expires_at is null
    or p.bank_code_snapshot is null
    or p.bank_account_number_snapshot is null
    or p.bank_account_name_snapshot is null);
```

- Query executor: __________  Run time (UTC): __________
- Result row count: __________  Evidence reference/checksum: __________
- [ ] Classify every returned row in the table below. No row may be left unclassified.

| Payment ID | Order number | Classification | Evidence/reference | Required action | Owner | Reviewer | Closed at (UTC) |
| --- | --- | --- | --- | --- | --- | --- | --- |
|  |  |  |  |  |  |  |  |

Use exactly one classification per row:

- **Trusted backfill with evidence:** immutable QR, provider, or approved deployment evidence proves the exact bank code, account number, account name, and expiry presented when that payment was created. Apply only a separately reviewed, row-specific correction using those historical values, then attach before/after evidence.
- **Cancel/recreate:** evidence confirms no money was received and policy permits closing the legacy attempt and creating a new payment with the approved destination and expiry. Preserve the old payment and its audit history.
- **Manual-reconciliation-only:** funds may have moved, evidence is missing or conflicting, the window is missing/expired, or cancel/recreate is unsafe. Do not auto-fulfil or infer snapshots; an authorized operator must reconcile the actual transaction and record actor, time, reason, and provider/evidence reference.

**Prohibited:** generic or bulk `UPDATE`, copying current runtime bank configuration into legacy rows, or stamping one destination/expiry across multiple payments. This gate performs no automatic production mutation.

- [ ] All row actions are closed or explicitly assigned before rollout.
- Classification approver: __________  Timestamp: __________

## 2. Destination approval and fingerprint

- [ ] Approver 1 and Approver 2 independently verify the bank code, account number, and account name against the authorized bank/account-owner source. They must be different people.
- Approved destination secure reference/version: __________
- Approver 1: __________  Evidence checked: __________  Timestamp: __________
- Approver 2: __________  Evidence checked: __________  Timestamp: __________
- [ ] In approved secret tooling, trim each approved value, uppercase it, join the UTF-8 values with literal `|` separators and no spaces/newline, then calculate SHA-256:

  `UPPERCASE(TRIM(bankCode))|UPPERCASE(TRIM(bankAccount))|UPPERCASE(TRIM(bankAccountName))`

- [ ] Record the resulting 64-character hexadecimal digest as `SEPAY_BANK_DESTINATION_SHA256` without exposing the source values in tickets, logs, or command history.
- Fingerprint secret version/reference: __________  Digest verifier: __________  Timestamp: __________
- [ ] Store `SEPAY_BANK_DESTINATION_SHA256` under deployment permissions separate from `SEPAY_BANK_CODE`, `SEPAY_BANK_ACCOUNT`, and `SEPAY_BANK_ACCOUNT_NAME`. A principal able to change the destination values must not be able to change the fingerprint secret; an independently authorized operator supplies/approves it.
- [ ] Enable deployment-platform audit alerts for any change to the three destination values or `SEPAY_BANK_DESTINATION_SHA256`, including actor, environment, old/new secret version (not value), and time.
- Alert route: __________  Test alert evidence: __________  On-call acknowledgement: __________

## 3. Staging-first verification

- [ ] Deploy to staging first with the approved destination and independently stored fingerprint.
- [ ] Complete every check below using non-production test data. Preserve request/response, database, and audit evidence without credentials or full bank details.

| Check | Required result | Evidence reference | Executor / UTC time | Pass |
| --- | --- | --- | --- | --- |
| QR destination | Decoded QR bank code, account, and account name match the two-person-approved destination; amount and transfer content match the order. |  |  | [ ] |
| Valid webhook | A valid in-window webhook fulfils the intended payment/order once and creates one `FULFILLED` provider event. |  |  | [ ] |
| Expired webhook | A webhook at or after expiry does not fulfil; payment/order remain pending for manual reconciliation and a `PAYMENT_WINDOW_EXPIRED` event is appended. |  |  | [ ] |
| Duplicate replay | Replaying the same provider transaction does not duplicate fulfilment, membership, receipt, revenue, or rights; the response is idempotent and the original ledger event remains unchanged. |  |  | [ ] |
| Manual reconciliation after expiry | An authorized operator can resolve an expired pending payment from actual transaction evidence; actor, timestamp, reason, and evidence/reference are audited, with no duplicate fulfilment. |  |  | [ ] |

- Staging verifier: __________  Timestamp: __________
- [ ] Re-run the read-only inventory for production immediately before deployment; attach the result and reconcile any delta through Section 1.
- Pre-production inventory evidence: __________  Row count: __________  Timestamp: __________

## 4. Production rollout, monitoring, and rollback

- [ ] All legacy rows are dispositioned, staging checks pass, destination/fingerprint permissions are separated, and change alerts are active.
- [ ] Deploy the application and confirm QR generation succeeds only with the approved destination/fingerprint pair.
- [ ] Monitor destination/fingerprint change alerts, invalid-destination errors, webhook outcomes, and duplicate/expired event rates during the deployment window.
- Monitoring owner: __________  Dashboard/log reference: __________  End time (UTC): __________

If rollback is required:

- [ ] Roll back the application artifact/configuration only to the approved prior version.
- [ ] Keep additive migration V48 applied. Preserve the `payment_provider_events` table, its append-only trigger, and every existing provider event.
- [ ] Do not drop/truncate the table or update/delete provider events. Continue manual reconciliation for unresolved payments and retain all evidence.
- Rollback decision/incident: __________  Decider: __________  Timestamp: __________

## Final sign-off

- Operations: __________  Decision: GO / NO-GO  Timestamp: __________
- Security: __________  Decision: GO / NO-GO  Timestamp: __________
- Product/payment owner: __________  Decision: GO / NO-GO  Timestamp: __________
- Post-deploy evidence location: __________
