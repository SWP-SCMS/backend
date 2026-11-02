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
- No schema change or data cleanup in this change. No manual deployment.
- The remaining four changes are not implemented by this PR. The complete workflow is not accepted until all applicable CI and isolated smoke gates pass.
