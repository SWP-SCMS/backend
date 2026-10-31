alter table course_sessions
    drop constraint uq_course_sessions_run_unit,
    add column makeup_warning_at timestamptz,
    add column is_makeup boolean not null default false;

create unique index uq_course_sessions_run_original_unit
    on course_sessions (course_run_id, unit_number)
    where is_makeup = false;

create unique index uq_course_sessions_replacement
    on course_sessions (replacement_session_id)
    where replacement_session_id is not null;

alter table course_sessions
    add constraint fk_course_sessions_replacement_unit
        foreign key (replacement_session_id, course_run_id, unit_number)
        references course_sessions (id, course_run_id, unit_number);

alter table course_enrollment_sessions
    drop constraint chk_course_enrollment_sessions_disposition,
    add constraint chk_course_enrollment_sessions_disposition
        check (disposition in ('ENROLLED', 'NOT_ENROLLED', 'UNRESOLVED'));

create table course_refund_obligations (
    id uuid primary key,
    course_enrollment_id uuid not null,
    course_run_id uuid not null,
    source_type varchar(32) not null,
    amount_vnd numeric(19, 0) not null,
    status varchar(24) not null default 'PENDING_REVIEW',
    created_at timestamptz not null default current_timestamp,

    constraint fk_course_refund_obligations_enrollment_run
        foreign key (course_enrollment_id, course_run_id)
        references course_enrollments (id, course_run_id),
    constraint uq_course_refund_obligations_source
        unique (course_enrollment_id, source_type),
    constraint chk_course_refund_obligations_source
        check (source_type in ('UNRESOLVED_SESSIONS')),
    constraint chk_course_refund_obligations_amount check (amount_vnd > 0),
    constraint chk_course_refund_obligations_status
        check (status in ('PENDING_REVIEW', 'APPROVED', 'DISBURSEMENT_RECORDED',
            'PENDING_VERIFICATION', 'COMPLETED', 'REJECTED', 'FAILED', 'RETURNED'))
);

create index idx_course_refund_obligations_review
    on course_refund_obligations (status, created_at, id);
