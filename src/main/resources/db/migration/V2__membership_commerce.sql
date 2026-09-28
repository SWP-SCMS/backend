create table membership_plans (
    plan_code varchar(10) primary key,
    display_name varchar(100) not null,
    supports_booking boolean not null,
    supports_personal_coaching boolean not null,
    created_at timestamptz not null default current_timestamp,

    constraint chk_membership_plans_code check (plan_code in ('BASIC', 'PLUS'))
);

insert into membership_plans (plan_code, display_name, supports_booking, supports_personal_coaching)
values
    ('BASIC', 'Basic', false, false),
    ('PLUS', 'Plus', true, true)
on conflict (plan_code) do nothing;

create table membership_offers (
    id uuid primary key,
    plan_code varchar(10) not null references membership_plans(plan_code),
    name varchar(200) not null,
    description text not null,
    price_amount numeric(19, 0) not null,
    currency_code char(3) not null default 'VND',
    duration_days integer not null,
    status varchar(20) not null,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_membership_offers_status check (status in ('ACTIVE', 'INACTIVE')),
    constraint chk_membership_offers_price check (price_amount > 0),
    constraint chk_membership_offers_duration check (duration_days > 0),
    constraint chk_membership_offers_name_not_blank check (btrim(name) <> ''),
    constraint chk_membership_offers_currency check (currency_code = 'VND')
);

create index idx_membership_offers_active_plan
    on membership_offers (plan_code, created_at desc)
    where status = 'ACTIVE';

create table membership_orders (
    id uuid primary key,
    order_number varchar(40) not null unique,
    member_account_id uuid not null references accounts(id),
    created_by_account_id uuid not null references accounts(id),
    offer_id uuid not null references membership_offers(id),
    offer_name_snapshot varchar(200) not null,
    plan_code_snapshot varchar(10) not null references membership_plans(plan_code),
    price_amount_snapshot numeric(19, 0) not null,
    currency_code_snapshot char(3) not null,
    duration_days_snapshot integer not null,
    payment_method varchar(20) not null,
    status varchar(20) not null,
    expires_at timestamptz not null,
    paid_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_membership_orders_payment_method check (payment_method in ('CASH', 'BANK_TRANSFER')),
    constraint chk_membership_orders_status check (status in ('PENDING_PAYMENT', 'PAID', 'EXPIRED')),
    constraint chk_membership_orders_amount check (price_amount_snapshot > 0),
    constraint chk_membership_orders_duration check (duration_days_snapshot > 0),
    constraint chk_membership_orders_currency check (currency_code_snapshot = 'VND'),
    constraint chk_membership_orders_paid_at check (
        (status = 'PAID' and paid_at is not null) or (status <> 'PAID' and paid_at is null)
    ),
    constraint uq_membership_orders_id_member unique (id, member_account_id)
);

create index idx_membership_orders_member_created_at
    on membership_orders (member_account_id, created_at desc);

create index idx_membership_orders_pending_expiry
    on membership_orders (expires_at)
    where status = 'PENDING_PAYMENT';

create table payments (
    id uuid primary key,
    order_id uuid not null references membership_orders(id),
    method varchar(20) not null,
    status varchar(20) not null,
    amount numeric(19, 0) not null,
    currency_code char(3) not null,
    bank_transfer_content varchar(255),
    provider_reference varchar(255),
    processed_by_account_id uuid references accounts(id),
    failure_reason text,
    paid_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_payments_method check (method in ('CASH', 'BANK_TRANSFER')),
    constraint chk_payments_status check (status in ('PENDING', 'PAID', 'FAILED')),
    constraint chk_payments_amount check (amount > 0),
    constraint chk_payments_currency check (currency_code = 'VND'),
    constraint chk_payments_paid_at check (
        (status = 'PAID' and paid_at is not null) or (status <> 'PAID' and paid_at is null)
    ),
    constraint uq_payments_id_order unique (id, order_id)
);

create index idx_payments_order_created_at on payments (order_id, created_at desc);

create unique index uq_payments_provider_reference
    on payments (provider_reference)
    where provider_reference is not null;

create table memberships (
    id uuid primary key,
    member_account_id uuid not null references accounts(id),
    order_id uuid not null unique references membership_orders(id),
    offer_id uuid not null references membership_offers(id),
    plan_code_snapshot varchar(10) not null references membership_plans(plan_code),
    offer_name_snapshot varchar(200) not null,
    price_amount_snapshot numeric(19, 0) not null,
    currency_code_snapshot char(3) not null,
    duration_days_snapshot integer not null,
    status varchar(20) not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_memberships_status check (status in ('ACTIVE', 'EXPIRED')),
    constraint chk_memberships_time check (ends_at > starts_at),
    constraint chk_memberships_price check (price_amount_snapshot > 0),
    constraint chk_memberships_duration check (duration_days_snapshot > 0),
    constraint chk_memberships_currency check (currency_code_snapshot = 'VND'),
    constraint uq_memberships_id_member unique (id, member_account_id),
    constraint ex_memberships_no_active_overlap exclude using gist (
        member_account_id with =,
        tstzrange(starts_at, ends_at, '[)') with &&
    ) where (status = 'ACTIVE')
);

create index idx_memberships_member_status_ends_at
    on memberships (member_account_id, status, ends_at desc);

create table receipts (
    id uuid primary key,
    receipt_number varchar(40) not null unique,
    payment_id uuid not null,
    order_id uuid not null,
    member_account_id uuid not null references accounts(id),
    amount_snapshot numeric(19, 0) not null,
    currency_code_snapshot char(3) not null,
    payment_method_snapshot varchar(20) not null,
    issued_at timestamptz not null default current_timestamp,

    constraint chk_receipts_amount check (amount_snapshot > 0),
    constraint chk_receipts_currency check (currency_code_snapshot = 'VND'),
    constraint chk_receipts_method check (payment_method_snapshot in ('CASH', 'BANK_TRANSFER')),
    constraint uq_receipts_payment unique (payment_id),
    constraint fk_receipts_payment_order
        foreign key (payment_id, order_id) references payments (id, order_id),
    constraint fk_receipts_order_member
        foreign key (order_id, member_account_id) references membership_orders (id, member_account_id)
);

create index idx_receipts_member_issued_at on receipts (member_account_id, issued_at desc);
