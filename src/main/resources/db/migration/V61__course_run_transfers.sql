alter table course_enrollments
    add column lifecycle_status varchar(24) not null default 'ACTIVE',
    add column source_transfer_offer_id uuid,
    add column credit_applied_vnd numeric(19, 0) not null default 0,
    add column cash_paid_vnd numeric(19, 0) not null default 0;

update course_enrollments set cash_paid_vnd=amount_paid_vnd_snapshot;

alter table course_enrollments
    alter column course_order_id drop not null,
    alter column course_reservation_id drop not null,
    alter column course_payment_id drop not null,
    drop constraint fk_course_enrollments_payment_tuple;

alter table course_payments add constraint uq_course_payments_enrollment_identity
    unique (id, course_order_id, course_reservation_id, member_account_id, actor_account_id);
alter table course_enrollments add constraint fk_course_enrollments_payment_identity
    foreign key (course_payment_id,course_order_id,course_reservation_id,member_account_id,actor_account_id)
    references course_payments(id,course_order_id,course_reservation_id,member_account_id,actor_account_id);
alter table course_enrollments add constraint chk_course_enrollments_lifecycle
    check (lifecycle_status in ('ACTIVE','TRANSFERRED','COMPLETED','WITHDRAWAL_PENDING','WITHDRAWN'));
alter table course_enrollments add constraint chk_course_enrollments_transfer_amounts
    check (credit_applied_vnd>=0 and cash_paid_vnd>=0 and credit_applied_vnd+cash_paid_vnd<=amount_paid_vnd_snapshot);

drop index uq_course_enrollments_member_run;
create unique index uq_course_enrollments_member_run_active
    on course_enrollments(member_account_id,course_run_id) where lifecycle_status='ACTIVE';

create table course_transfer_requests (
    id uuid primary key,
    schedule_change_response_id uuid not null references course_schedule_change_responses(id),
    source_enrollment_id uuid not null references course_enrollments(id),
    source_run_id uuid not null references course_runs(id),
    member_account_id uuid not null references accounts(id),
    requester_account_id uuid not null references accounts(id),
    status varchar(20) not null default 'PENDING',
    target_run_id uuid references course_runs(id),
    reason varchar(500),
    decided_by_account_id uuid references accounts(id),
    requested_at timestamptz not null,
    decided_at timestamptz,
    completed_at timestamptz,
    constraint uq_course_transfer_request_schedule_change unique(schedule_change_response_id),
    constraint chk_course_transfer_request_status check(status in ('PENDING','OFFERED','REJECTED','COMPLETED','EXPIRED')),
    constraint fk_course_transfer_request_source foreign key(source_enrollment_id,source_run_id)
        references course_enrollments(id,course_run_id)
);
create index idx_course_transfer_requests_queue on course_transfer_requests(status,requested_at,id);

create table course_transfer_offers (
    id uuid primary key,
    transfer_request_id uuid not null unique references course_transfer_requests(id),
    source_enrollment_id uuid not null references course_enrollments(id),
    source_run_id uuid not null references course_runs(id),
    target_run_id uuid not null references course_runs(id),
    member_account_id uuid not null references accounts(id),
    credit_vnd_snapshot numeric(19,0) not null,
    target_tuition_vnd_snapshot numeric(19,0) not null,
    difference_vnd_snapshot numeric(19,0) not null,
    expires_at timestamptz not null,
    status varchar(24) not null default 'OFFERED',
    acceptance_idempotency_key varchar(100),
    difference_order_id uuid unique references course_orders(id),
    issued_at timestamptz not null,
    accepted_at timestamptz,
    completed_at timestamptz,
    constraint chk_course_transfer_offer_status check(status in ('OFFERED','PENDING_PAYMENT','COMPLETED','EXPIRED','CANCELLED')),
    constraint chk_course_transfer_offer_values check(credit_vnd_snapshot>=0 and target_tuition_vnd_snapshot>0
        and difference_vnd_snapshot=target_tuition_vnd_snapshot-credit_vnd_snapshot),
    constraint chk_course_transfer_offer_runs check(source_run_id<>target_run_id)
);
create unique index uq_course_transfer_offer_acceptance_key
    on course_transfer_offers(member_account_id,acceptance_idempotency_key)
    where acceptance_idempotency_key is not null;
create index idx_course_transfer_offers_expiry on course_transfer_offers(status,expires_at,id);

create table course_transfer_holds (
    id uuid primary key,
    transfer_offer_id uuid not null,
    target_run_id uuid not null references course_runs(id),
    member_account_id uuid not null references accounts(id),
    status varchar(16) not null default 'HELD',
    expires_at timestamptz not null,
    created_at timestamptz not null,
    released_at timestamptz,
    constraint uq_course_transfer_hold_offer unique(transfer_offer_id),
    constraint fk_course_transfer_hold_offer foreign key(transfer_offer_id) references course_transfer_offers(id),
    constraint chk_course_transfer_hold_status check(status in ('HELD','CONSUMED','RELEASED'))
);
create index idx_course_transfer_holds_capacity on course_transfer_holds(target_run_id,expires_at) where status='HELD';

alter table course_enrollments add constraint fk_course_enrollments_transfer_offer
    foreign key(source_transfer_offer_id) references course_transfer_offers(id);

alter table course_reservations drop constraint chk_course_reservations_status;
alter table course_reservations add constraint chk_course_reservations_status
    check(status in ('HELD','EXPIRED','ENROLLED','TRANSFERRED'));

alter table course_refund_obligations drop constraint chk_course_refund_obligations_source;
alter table course_refund_obligations add constraint chk_course_refund_obligations_source
    check(source_type in ('UNRESOLVED_SESSIONS','COURSE_RUN_CANCELLED','TRANSFER_CREDIT_EXCESS'));
