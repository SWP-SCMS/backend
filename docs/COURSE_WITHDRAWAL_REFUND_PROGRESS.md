# Withdrawal refund progress

Withdrawal decisions and refund settlement remain separate state machines. An approved withdrawal stays APPROVED after disbursement and verification.

All existing withdrawal list/detail/mutation responses now include nullable `refundObligationId` and `refundStatus`. The shared read query joins the approved withdrawal to its enrollment and refund source (VOLUNTARY -> MEMBER_WITHDRAWAL, other causes -> MATERIAL_CHANGE), with source_event_id IS NULL. The existing unique constraint prevents duplicate queue rows. Pending/rejected/cancelled withdrawals never inherit another request's refund. Unlinked historical requests return null.

No schema migration, historical rewrite, payment-policy change or new endpoint is required. Only refund identity and status are exposed; bank details and disbursement evidence are not added to withdrawal responses.

Frontend display:

| Refund state of an APPROVED withdrawal | Meaning |
| --- | --- |
| PENDING_REVIEW | Withdrawal approved, refund awaiting approval |
| APPROVED | Refund approved, awaiting disbursement |
| DISBURSEMENT_RECORDED / PENDING_VERIFICATION | Disbursement recorded, awaiting verification |
| COMPLETED | Refund completed successfully |
| REJECTED / FAILED / RETURNED | Show the corresponding unsuccessful outcome |
| null / unknown | Withdrawal approved, refund progress unavailable |

Roll out backend before frontend when these branches are merged/deployed separately. Old frontend ignores the added fields; new frontend handles an old backend without claiming payment success. Pushes in this task do not deploy either application.

Verification covers the full refund transition through real services/PostgreSQL and reads from all three roles, source/event isolation, cancelled requests, missing legacy links, zero-value obligations, terminal outcomes, ownership, and static/generated OpenAPI. The frontend branch supplies unit, browser and real-runtime coverage.
