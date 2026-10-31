alter table course_refund_obligations
    add column disbursement_idempotency_key varchar(100);

alter table course_refund_obligations
    add constraint uq_course_refund_disbursement_actor_key
        unique(disbursed_by_account_id, disbursement_idempotency_key),
    add constraint chk_course_refund_disbursement_metadata check(
        disbursement_idempotency_key is null
        or (btrim(disbursement_idempotency_key) <> ''
            and disbursed_by_account_id is not null
            and disbursed_at is not null
            and disbursement_method is not null
            and disbursement_reference is not null
            and evidence_reference is not null)
    );

create index idx_course_refund_obligations_verification_queue
    on course_refund_obligations(status, disbursed_at, id)
    where status in ('DISBURSEMENT_RECORDED', 'PENDING_VERIFICATION');
