alter table course_orders drop constraint chk_course_orders_status;
alter table course_orders add constraint chk_course_orders_status
    check (status in ('PENDING_PAYMENT', 'EXPIRED', 'PAID'));

alter table course_reservations drop constraint chk_course_reservations_status;
alter table course_reservations add constraint chk_course_reservations_status
    check (status in ('HELD', 'EXPIRED', 'ENROLLED'));

alter table course_payments
    alter column bank_code_snapshot drop not null,
    alter column bank_account_number_snapshot drop not null,
    alter column bank_account_name_snapshot drop not null,
    add column method varchar(16) not null default 'SEPAY',
    add column processed_by_account_id uuid references accounts(id),
    add column provider varchar(20),
    add column provider_transaction_id varchar(100),
    add column paid_at timestamptz;

alter table course_payments drop constraint chk_course_payments_destination;
alter table course_payments add constraint chk_course_payments_method check (method in ('SEPAY', 'CASH'));
alter table course_payments add constraint chk_course_payments_destination check (
    (method = 'CASH') or (
        bank_code_snapshot is not null and btrim(bank_code_snapshot) <> '' and
        bank_account_number_snapshot is not null and btrim(bank_account_number_snapshot) <> '' and
        bank_account_name_snapshot is not null and btrim(bank_account_name_snapshot) <> ''
    )
);
alter table course_payments add constraint uq_course_payments_provider_transaction
    unique (provider, provider_transaction_id);
alter table course_payments add constraint uq_course_payments_enrollment_tuple
    unique (id, course_order_id, course_reservation_id, member_account_id, actor_account_id, amount);

alter table course_reservations add constraint uq_course_reservations_enrollment_tuple
    unique (id, course_order_id, member_account_id, course_run_id);
alter table course_sessions add constraint uq_course_sessions_enrollment_tuple
    unique (id, course_run_id, unit_number);

create table course_enrollments (
    id uuid primary key,
    course_order_id uuid not null unique,
    course_reservation_id uuid not null unique,
    course_payment_id uuid not null unique,
    member_account_id uuid not null references accounts(id),
    actor_account_id uuid not null references accounts(id),
    processed_by_account_id uuid references accounts(id),
    course_run_id uuid not null references course_runs(id),
    amount_paid_vnd_snapshot numeric(19, 0) not null,
    total_units_snapshot integer not null,
    enrolled_at timestamptz not null,
    created_at timestamptz not null default current_timestamp,

    constraint fk_course_enrollments_order_tuple foreign key (course_order_id, member_account_id, course_run_id)
        references course_orders (id, member_account_id, course_run_id),
    constraint fk_course_enrollments_reservation_tuple foreign key
        (course_reservation_id, course_order_id, member_account_id, course_run_id)
        references course_reservations (id, course_order_id, member_account_id, course_run_id),
    constraint fk_course_enrollments_payment_tuple foreign key
        (course_payment_id, course_order_id, course_reservation_id, member_account_id, actor_account_id,
            amount_paid_vnd_snapshot)
        references course_payments
            (id, course_order_id, course_reservation_id, member_account_id, actor_account_id, amount),
    constraint chk_course_enrollments_amount check (amount_paid_vnd_snapshot > 0),
    constraint chk_course_enrollments_units check (total_units_snapshot > 0),
    constraint uq_course_enrollments_run_tuple unique (id, course_run_id)
);

create unique index uq_course_enrollments_member_run
    on course_enrollments (member_account_id, course_run_id);

create table course_enrollment_sessions (
    id uuid primary key,
    course_enrollment_id uuid not null references course_enrollments(id),
    course_session_id uuid not null references course_sessions(id),
    course_run_id uuid not null,
    unit_number integer not null,
    allocation_vnd numeric(19, 0) not null,
    disposition varchar(20) not null,
    created_at timestamptz not null default current_timestamp,

    constraint uq_course_enrollment_sessions_session unique (course_enrollment_id, course_session_id),
    constraint uq_course_enrollment_sessions_unit unique (course_enrollment_id, unit_number),
    constraint fk_course_enrollment_sessions_enrollment_run foreign key (course_enrollment_id, course_run_id)
        references course_enrollments (id, course_run_id),
    constraint fk_course_enrollment_sessions_session_run_unit foreign key
        (course_session_id, course_run_id, unit_number)
        references course_sessions (id, course_run_id, unit_number),
    constraint chk_course_enrollment_sessions_unit check (unit_number > 0),
    constraint chk_course_enrollment_sessions_allocation check (allocation_vnd >= 0),
    constraint chk_course_enrollment_sessions_disposition check (disposition in ('ENROLLED', 'NOT_ENROLLED'))
);

create index idx_course_enrollments_member_run on course_enrollments (member_account_id, course_run_id);
