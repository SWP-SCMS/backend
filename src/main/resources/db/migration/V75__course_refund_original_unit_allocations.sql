alter table course_refund_obligations
    add column source_event_id uuid references course_schedule_change_events(id),
    drop constraint uq_course_refund_obligations_source,
    add constraint uq_course_refund_obligations_source
        unique nulls not distinct (course_enrollment_id, source_type, source_event_id),
    add constraint uq_course_refund_obligation_enrollment unique (id, course_enrollment_id),
    drop constraint chk_course_refund_obligations_source,
    add constraint chk_course_refund_obligations_source check (source_type in (
        'MEMBER_WITHDRAWAL','COURSE_RUN_CANCELLED','MATERIAL_CHANGE','UNRESOLVED_SESSIONS',
        'TRANSFER_CREDIT_EXCESS','PAYMENT_EXCEPTION','REDUCED_SESSION_COUNT'));

alter table course_enrollment_sessions
    add constraint uq_course_enrollment_session_owner unique (id, course_enrollment_id);

-- One original purchased unit can fund only one centre-initiated refund.
create table course_refund_unit_claims (
    enrollment_session_id uuid primary key,
    course_enrollment_id uuid not null,
    refund_obligation_id uuid not null,
    allocation_vnd numeric(19,0) not null check (allocation_vnd > 0),
    created_at timestamptz not null default current_timestamp,
    foreign key (enrollment_session_id,course_enrollment_id)
        references course_enrollment_sessions(id,course_enrollment_id),
    foreign key (refund_obligation_id,course_enrollment_id)
        references course_refund_obligations(id,course_enrollment_id)
);
create index idx_course_refund_unit_claims_obligation on course_refund_unit_claims(refund_obligation_id);
create trigger trg_course_refund_unit_claims_immutable
    before update or delete on course_refund_unit_claims
    for each row execute function prevent_history_mutation();
