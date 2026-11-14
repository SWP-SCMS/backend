# Course withdrawal refund selection

## Center cancellation after classes have started

A manager may cancel an `IN_PROGRESS` run. Each active or withdrawal-pending
enrollment receives a `COURSE_RUN_CANCELLED` obligation for 100% of the original
paid allocation of its unprovided, unclaimed sessions. Delivered sessions are
excluded even if the member was absent or the scheduler has not marked them
completed yet. Existing unit claims are excluded to prevent duplicate refunds.

For example, ten sessions with 100,000 VND allocated to each, with one already
delivered, yield a 900,000 VND refund obligation for the other nine. Uneven
allocations retain their original amounts; remaining units are never repriced.
The obligation starts in `PENDING_REVIEW` and follows approval, disbursement and
verification. Creating it does not mean money has been transferred.

## Withdrawal following a schedule change

New withdrawal requests receive 100% of the unused, unclaimed unit allocation for
`CENTER_SESSION_RESCHEDULE` and `MEMBER_SCHEDULE_CONFLICT`, without the voluntary
70% remaining-unit threshold. A regular session move uses the existing material
change definition (more than 30 minutes, or a different Vietnam calendar date).

Opening delays over seven days and reduced-unit withdrawals retain 80%.
Run-window-only changes, resource-only changes, manual withdrawal, and makeup
schedule changes retain their existing classification and thresholds. Existing
special Run causes take precedence when a batch also changes individual sessions.
Unit refunds for removed or unprovided sessions remain separate at 100%.

The new rate applies when a withdrawal request is submitted. Existing withdrawal
snapshots, including pending decisions and approved/disbursed refunds, remain
unchanged. Migration V78 classifies only unanswered material regular-session
notices that do not already have a withdrawal request, so their future submission
and displayed estimate use the new policy.

In a grouped event the full-refund cause is selected before other causes, except
an existing linked withdrawal always wins to preserve replay and its snapshot.
