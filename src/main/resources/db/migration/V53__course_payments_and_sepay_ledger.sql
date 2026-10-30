alter table course_orders
    add constraint uq_course_orders_payment_tuple
    unique (id, member_account_id, actor_account_id, tuition_vnd_snapshot, currency_code, order_reference);

alter table course_reservations
    add constraint uq_course_reservations_payment_tuple
    unique (id, course_order_id, member_account_id, expires_at);

create table course_payments (
    id uuid primary key,
    course_order_id uuid not null,
    course_reservation_id uuid not null,
    member_account_id uuid not null,
    actor_account_id uuid not null,
    payment_reference varchar(36) not null,
    amount numeric(19, 0) not null,
    currency_code char(3) not null,
    status varchar(20) not null,
    deadline_at timestamptz not null,
    bank_code_snapshot text not null,
    bank_account_number_snapshot text not null,
    bank_account_name_snapshot text not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,

    constraint uq_course_payments_order unique (course_order_id),
    constraint uq_course_payments_reservation unique (course_reservation_id),
    constraint uq_course_payments_reference unique (payment_reference),
    constraint fk_course_payments_order_tuple
        foreign key (course_order_id, member_account_id, actor_account_id, amount, currency_code, payment_reference)
        references course_orders
            (id, member_account_id, actor_account_id, tuition_vnd_snapshot, currency_code, order_reference),
    constraint fk_course_payments_reservation_tuple
        foreign key (course_reservation_id, course_order_id, member_account_id, deadline_at)
        references course_reservations (id, course_order_id, member_account_id, expires_at),
    constraint chk_course_payments_reference check (payment_reference ~ '^COR-[0-9A-F]{32}$'),
    constraint chk_course_payments_amount check (amount > 0),
    constraint chk_course_payments_currency check (currency_code = 'VND'),
    constraint chk_course_payments_status check (status in ('PENDING', 'PAID', 'FAILED')),
    constraint chk_course_payments_deadline check (deadline_at > created_at),
    constraint chk_course_payments_destination check (
        btrim(bank_code_snapshot) <> '' and
        btrim(bank_account_number_snapshot) <> '' and
        btrim(bank_account_name_snapshot) <> ''
    )
);

create index idx_course_payments_order_created_at
    on course_payments (course_order_id, created_at desc);

create or replace function protect_course_payment_destination_snapshot()
returns trigger language plpgsql as $$
begin
    if new.bank_code_snapshot is distinct from old.bank_code_snapshot or
       new.bank_account_number_snapshot is distinct from old.bank_account_number_snapshot or
       new.bank_account_name_snapshot is distinct from old.bank_account_name_snapshot then
        raise exception 'Course Payment destination snapshot is immutable';
    end if;
    return new;
end;
$$;

create trigger trg_course_payments_destination_immutable
before update on course_payments
for each row execute function protect_course_payment_destination_snapshot();

alter table payment_provider_events
    add column course_payment_id uuid references course_payments(id);

alter table payment_provider_events
    add constraint chk_payment_provider_events_single_target check (
        payment_id is null or course_payment_id is null
    );

alter table payment_provider_events
    drop constraint chk_payment_provider_events_outcome;

alter table payment_provider_events
    add constraint chk_payment_provider_events_outcome check (outcome in (
        'FULFILLED',
        'ALREADY_PROCESSED',
        'PAYMENT_WINDOW_EXPIRED',
        'DESTINATION_SNAPSHOT_MISSING',
        'REFERENCE_MISMATCH',
        'AMOUNT_MISMATCH',
        'ACCOUNT_MISMATCH',
        'PAYMENT_NOT_PAYABLE',
        'COURSE_PAYMENT_RECORDED'
    ));

create index idx_payment_provider_events_course_payment_received_at
    on payment_provider_events (course_payment_id, received_at desc);
