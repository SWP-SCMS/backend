# Course withdrawal refund selection

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
