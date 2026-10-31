drop index uq_course_enrollments_member_run_active;
create unique index uq_course_enrollments_member_run_active
    on course_enrollments(member_account_id, course_run_id)
    where lifecycle_status in ('ACTIVE', 'WITHDRAWAL_PENDING');

alter table course_reservations drop constraint chk_course_reservations_status;
alter table course_reservations add constraint chk_course_reservations_status
    check(status in ('HELD', 'EXPIRED', 'ENROLLED', 'TRANSFERRED', 'WITHDRAWN'));

alter table course_refund_obligations drop constraint chk_course_refund_obligations_source;
alter table course_refund_obligations add constraint chk_course_refund_obligations_source
    check(source_type in ('UNRESOLVED_SESSIONS', 'COURSE_RUN_CANCELLED', 'TRANSFER_CREDIT_EXCESS',
        'MEMBER_WITHDRAWAL'));
alter table course_refund_obligations drop constraint chk_course_refund_obligations_amount;
alter table course_refund_obligations add constraint chk_course_refund_obligations_amount
    check(amount_vnd >= 0);

create table course_withdrawal_requests (
    id uuid primary key,
    course_enrollment_id uuid not null,
    course_run_id uuid not null,
    member_account_id uuid not null references accounts(id),
    requester_account_id uuid not null references accounts(id),
    status varchar(16) not null default 'PENDING',
    request_reason varchar(1000) not null,
    decision_reason varchar(1000),
    requested_at timestamptz not null,
    decided_by_account_id uuid references accounts(id),
    decided_at timestamptz,
    total_units_snapshot integer,
    remaining_units_snapshot integer,
    refund_basis_vnd numeric(19, 0),
    refund_amount_vnd numeric(19, 0),
    constraint fk_course_withdrawal_enrollment_run
        foreign key(course_enrollment_id, course_run_id) references course_enrollments(id, course_run_id),
    constraint chk_course_withdrawal_status check(status in ('PENDING', 'APPROVED', 'REJECTED')),
    constraint chk_course_withdrawal_decision check(
        (status='PENDING' and decision_reason is null and decided_by_account_id is null and decided_at is null
            and total_units_snapshot is null and remaining_units_snapshot is null
            and refund_basis_vnd is null and refund_amount_vnd is null)
        or (status='REJECTED' and decision_reason is not null and decided_by_account_id is not null
            and decided_at is not null and total_units_snapshot is null and remaining_units_snapshot is null
            and refund_basis_vnd is null and refund_amount_vnd is null)
        or (status='APPROVED' and decision_reason is not null and decided_by_account_id is not null
            and decided_at is not null and total_units_snapshot > 0 and remaining_units_snapshot > 0
            and remaining_units_snapshot <= total_units_snapshot and refund_basis_vnd >= 0
            and refund_amount_vnd >= 0 and refund_amount_vnd <= refund_basis_vnd)
    )
);

create unique index uq_course_withdrawal_pending_enrollment
    on course_withdrawal_requests(course_enrollment_id) where status='PENDING';
create index idx_course_withdrawal_manager_queue
    on course_withdrawal_requests(status, requested_at, id);
create index idx_course_withdrawal_member_history
    on course_withdrawal_requests(member_account_id, requested_at desc, id desc);
