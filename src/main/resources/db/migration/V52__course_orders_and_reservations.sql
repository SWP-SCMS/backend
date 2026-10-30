create table course_orders (
    id uuid primary key,
    order_reference varchar(36) not null,
    member_account_id uuid not null references accounts(id),
    actor_account_id uuid not null references accounts(id),
    course_run_id uuid not null references course_runs(id),
    tuition_vnd_snapshot numeric(19, 0) not null,
    currency_code char(3) not null,
    status varchar(24) not null,
    idempotency_key varchar(100) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,

    constraint uq_course_orders_reference unique (order_reference),
    constraint uq_course_orders_actor_idempotency unique (actor_account_id, idempotency_key),
    constraint uq_course_orders_reservation_tuple unique (id, member_account_id, course_run_id),
    constraint chk_course_orders_reference check (order_reference ~ '^COR-[0-9A-F]{32}$'),
    constraint chk_course_orders_tuition check (tuition_vnd_snapshot > 0),
    constraint chk_course_orders_currency check (currency_code = 'VND'),
    constraint chk_course_orders_status check (status in ('PENDING_PAYMENT', 'EXPIRED')),
    constraint chk_course_orders_idempotency_key check (btrim(idempotency_key) <> '')
);

create index idx_course_orders_member_created on course_orders (member_account_id, created_at desc, id);
create index idx_course_orders_run_status on course_orders (course_run_id, status);

create table course_reservations (
    id uuid primary key,
    course_order_id uuid not null references course_orders(id),
    member_account_id uuid not null references accounts(id),
    course_run_id uuid not null references course_runs(id),
    status varchar(16) not null,
    expires_at timestamptz not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,

    constraint uq_course_reservations_order unique (course_order_id),
	constraint fk_course_reservations_order_tuple
		foreign key (course_order_id, member_account_id, course_run_id)
		references course_orders (id, member_account_id, course_run_id),
	constraint chk_course_reservations_status check (status in ('HELD', 'EXPIRED')),
	constraint chk_course_reservations_expiry check (expires_at > created_at)
);

create unique index uq_course_reservations_member_run_held
    on course_reservations (member_account_id, course_run_id)
    where status = 'HELD';
create index idx_course_reservations_run_capacity
    on course_reservations (course_run_id, expires_at)
    where status = 'HELD';
create index idx_course_reservations_expiry_cursor
    on course_reservations (expires_at, id)
    where status = 'HELD';
