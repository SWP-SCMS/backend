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
- [ ] Run this supplemental read-only sweep for blank or whitespace-only snapshots and store its result with the first query.

```sql
select p.id, p.order_id, o.order_number, o.expires_at,
       p.bank_code_snapshot, p.bank_account_number_snapshot, p.bank_account_name_snapshot
from payments p
join membership_orders o on o.id = p.order_id
where p.provider = 'SEPAY' and p.status = 'PENDING'
  and (nullif(btrim(p.bank_code_snapshot), '') is null
    or nullif(btrim(p.bank_account_number_snapshot), '') is null
    or nullif(btrim(p.bank_account_name_snapshot), '') is null);
```

- Supplemental query executor: __________  Run time (UTC): __________
- Supplemental row count: __________  Evidence reference/checksum: __________
- [ ] Run this read-only complete inventory and store its result with the preceding query evidence. It must return every pending SePay payment, including rows whose snapshots are complete and nonblank.

```sql
select p.id, p.order_id, o.order_number, o.expires_at,
       p.bank_code_snapshot, p.bank_account_number_snapshot, p.bank_account_name_snapshot
from payments p
join membership_orders o on o.id = p.order_id
where p.provider = 'SEPAY' and p.status = 'PENDING';
```

- Complete inventory executor: __________  Run time (UTC): __________
- Complete pending count: __________  Evidence reference/checksum: __________
- [ ] Group the complete inventory by the exact bank code, account number, and account name snapshots, including null/blank tuples. Classify every destination tuple against trusted immutable QR, provider, or approved deployment evidence for the destination presented when those payments were created; a complete nonblank tuple is not implicitly trusted.
- [ ] Classify every payment below using its tuple classification and expiry/payment evidence. The classified payment count must equal the complete pending count, and every tuple must have an evidence-backed classification and reviewer.
- Destination tuple count: __________  Classified tuple count: __________  Counts match: YES / NO
- Classified payment count: __________  Complete pending count matches: YES / NO

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
- [ ] In approved secret tooling, reproduce Java exactly: apply `String.trim()` to each value (removing leading/trailing characters less than or equal to U+0020), join the three results with a literal `|`, apply `toUpperCase(Locale.ROOT)`, encode as UTF-8 with no newline, and calculate SHA-256:

  `String.join("|", bankCode.trim(), bankAccount.trim(), bankAccountName.trim()).toUpperCase(Locale.ROOT)`

- [ ] Verify the non-sensitive known-answer vector used by the application test: inputs `" mb "`, `" 123456789 "`, `" scms gym "` produce canonical text `MB|123456789|SCMS GYM` and SHA-256 `7128318f4501a30f6c5648627306786fb6e7a26a15da6850a959c5b35a0a6859`.
- Known-answer verifier: __________  Tool/test evidence: __________  Timestamp: __________

- [ ] Record the resulting 64-character hexadecimal digest as `SEPAY_BANK_DESTINATION_SHA256` without exposing the source values in tickets, logs, or command history.
- Fingerprint secret version/reference: __________  Digest verifier: __________  Timestamp: __________
- [ ] Store `SEPAY_BANK_DESTINATION_SHA256` under deployment permissions separate from `SEPAY_BANK_CODE`, `SEPAY_BANK_ACCOUNT`, and `SEPAY_BANK_ACCOUNT_NAME`. A principal able to change the destination values must not be able to change the fingerprint secret; an independently authorized operator supplies/approves it.
- [ ] Enable deployment-platform audit alerts for any change to the three destination values or `SEPAY_BANK_DESTINATION_SHA256`, including actor, environment, old/new secret version (not value), and time.
- Alert route: __________  Test alert evidence: __________  On-call acknowledgement: __________

## 3. Staging-first verification

- [ ] Deploy the exact immutable release artifact to staging first with the approved destination and independently stored fingerprint.
- Staging release commit: __________  Artifact SHA-256: __________
- [ ] Configure SePay to retry webhook deliveries automatically after every non-2xx response, and enable an incident alert for exhausted/delayed retries or repeated non-2xx responses before testing delivery.
- Provider retry configuration evidence: __________  Incident alert test evidence: __________  On-call acknowledgement: __________
- [ ] Complete every check below using non-production test data. Preserve request/response, database, and audit evidence without credentials or full bank details.

| Check | Required result | Evidence reference | Executor / UTC time | Pass |
| --- | --- | --- | --- | --- |
| QR destination | Decoded QR bank code, account, and account name match the two-person-approved destination; amount and transfer content match the order. |  |  | [ ] |
| Creation disabled | With the cutover control enabled, a SePay payment/QR cannot be created and no payment row is written. |  |  | [ ] |
| Webhook ingress disabled | With webhook ingress disabled, a request cannot reach payment processing and no provider event or fulfilment is created. |  |  | [ ] |
| Drain barrier | Hold one already-admitted SePay request and its database transaction open, disable creation and webhook ingress, and prove inventory/cutover remains blocked until that request finishes or aborts and both admitted-request and related-transaction metrics reach zero. |  |  | [ ] |
| Valid webhook | A valid in-window webhook fulfils the intended payment/order once and creates one `FULFILLED` provider event. |  |  | [ ] |
| Expired webhook | A webhook at or after expiry does not fulfil; payment/order remain pending for manual reconciliation and a `PAYMENT_WINDOW_EXPIRED` event is appended. |  |  | [ ] |
| Snapshot missing/blank webhook | A webhook for a payment with a null, empty, or whitespace-only destination snapshot does not fulfil and appends `DESTINATION_SNAPSHOT_MISSING`. |  |  | [ ] |
| Duplicate replay | Replaying the same provider transaction does not duplicate fulfilment, membership, receipt, revenue, or rights; the response is idempotent and the original ledger event remains unchanged. |  |  | [ ] |
| Manual reconciliation after expiry | An authorized operator can resolve an expired pending payment from actual transaction evidence; actor, timestamp, reason, and evidence/reference are audited, with no duplicate fulfilment. |  |  | [ ] |
| Whitespace inventory fixture | A pending staging payment with a whitespace-only snapshot is returned by the supplemental query, appears once in the complete inventory, and is classified. |  |  | [ ] |
| Complete unapproved snapshot fixture | A pending staging payment with a complete nonblank but historically unapproved destination tuple appears in the complete inventory; rollout remains blocked until it receives an evidence-backed classification and its required disposition is closed. |  |  | [ ] |
| Provider retry after non-2xx | Force the first delivery to return 500, then prove SePay automatically retries the same provider transaction ID and the retry fulfils the intended payment exactly once without duplicate membership, receipt, revenue, or rights. |  |  | [ ] |

- Staging verifier: __________  Timestamp: __________
- Staging disable evidence: __________  Re-enable evidence: __________
- Staging drain metrics/evidence: __________  Zero observed at (UTC): __________

## 4. Production rollout, monitoring, and rollback

- [ ] All legacy rows are dispositioned, staging checks pass, destination/fingerprint permissions are separated, and change alerts are active.
- [ ] Disable SePay payment creation and SePay webhook ingress before the final production inventory. Keep both disabled through inventory, deployment, verification, and cutover.
- Disable approver/operator: __________  Disabled at (UTC): __________  Evidence: __________
- [ ] Drain every SePay payment-create/webhook request admitted before disable and its related database transaction. Do not start final inventory or cutover until monitoring shows both counts at zero.
- Admitted requests at disable: __________  Related DB transactions at disable: __________
- Drain metrics/query evidence: __________  Both zero at (UTC): __________  Verifier: __________
- [ ] While both paths are disabled, re-run all three read-only queries from Section 1. Classify every complete-inventory destination tuple against trusted historical evidence, disposition every payment, and confirm both classified counts equal their complete-inventory counts.
- Final plan-query count: __________  Supplemental count: __________  Complete pending count: __________
- Final tuple count: __________  Classified tuple count: __________  Final classified payment count: __________
- Complete unapproved fixture blocked/dispositioned evidence: __________  Inventory evidence: __________  Timestamp: __________
- [ ] Deploy the immutable release artifact while public payment creation and webhook ingress remain disabled. Do not create an operator-only production test payment; use staging QR evidence produced by this exact artifact.
- Release commit: __________  Expected artifact SHA-256: __________
- Staging QR/expiry/snapshot rejection evidence for this artifact: __________
- Production artifact SHA-256: __________  Digest matches: YES / NO  Verifier: __________
- [ ] Keep webhook ingress disabled through cutover. Before re-enabling either path, confirm the artifact digest matches and the tied staging evidence proves the QR destination plus expired-webhook and null/empty/whitespace-snapshot rejection checks.
- [ ] Re-enable payment creation and webhook ingress only after the preceding checks pass and two-person approval is recorded.
- Re-enable approver 1: __________  Approver 2: __________  Re-enabled at (UTC): __________  Evidence: __________
- [ ] Monitor destination/fingerprint change alerts, invalid-destination errors, webhook outcomes, and duplicate/expired event rates during the deployment window.
- Monitoring owner: __________  Dashboard/log reference: __________  End time (UTC): __________

If rollback is required:

- [ ] Disable SePay payment creation and webhook ingress immediately and record evidence; keep both disabled during rollback.
- Rollback disable operator: __________  Disabled at (UTC): __________  Evidence: __________
- [ ] Drain every request admitted before rollback disable and its related database transaction. Do not start rollback until both monitored counts reach zero.
- Rollback admitted requests at disable: __________  Related DB transactions at disable: __________
- Rollback drain metrics/query evidence: __________  Both zero at (UTC): __________  Verifier: __________
- [ ] Roll back the application artifact/configuration only to the approved prior version.
- [ ] Keep additive migration V48 applied. Preserve the `payment_provider_events` table, its append-only trigger, and every existing provider event.
- [ ] Do not drop/truncate the table or update/delete provider events. Continue manual reconciliation for unresolved payments and retain all evidence.
- [ ] Do not re-enable payment creation or webhook ingress unless the selected rollback artifact itself has verified rejection of expired webhooks and null/empty/whitespace destination snapshots. Otherwise leave both disabled and use manual reconciliation.
- Rollback artifact: __________  Expiry/snapshot rejection evidence: __________
- Rollback re-enable approver 1: __________  Approver 2: __________  Re-enabled at (UTC): __________  Evidence: __________ / NOT RE-ENABLED
- Rollback decision/incident: __________  Decider: __________  Timestamp: __________

## Final sign-off

- Operations: __________  Decision: GO / NO-GO  Timestamp: __________
- Security: __________  Decision: GO / NO-GO  Timestamp: __________
- Product/payment owner: __________  Decision: GO / NO-GO  Timestamp: __________
- Post-deploy evidence location: __________
