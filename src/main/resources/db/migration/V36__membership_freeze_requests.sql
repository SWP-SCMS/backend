create table membership_freeze_requests (
    id uuid primary key,
    membership_id uuid not null references memberships(id),
    member_account_id uuid not null references accounts(id),
    requester_account_id uuid not null references accounts(id),
    approver_account_id uuid references accounts(id),
    start_date date not null,
    end_date date not null,
    reason text not null,
    fee_amount numeric(19,0),
    status varchar(30) not null,
    approved_at timestamptz,
    rejected_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint chk_membership_freeze_request_dates check (end_date > start_date),
    constraint chk_membership_freeze_request_reason check (btrim(reason) <> ''),
    constraint chk_membership_freeze_request_fee check (fee_amount is null or fee_amount >= 0),
    constraint chk_membership_freeze_request_status check (status in ('PENDING','APPROVED_PENDING_PAYMENT','SCHEDULED','REJECTED'))
);

create unique index uq_membership_freeze_request_open_membership
    on membership_freeze_requests(membership_id)
    where status in ('PENDING','APPROVED_PENDING_PAYMENT','SCHEDULED');

create index idx_membership_freeze_request_member_created
    on membership_freeze_requests(member_account_id, created_at desc);

create index idx_membership_freeze_request_status_start
    on membership_freeze_requests(status, start_date);

create table membership_freeze_periods (
    id uuid primary key,
    request_id uuid not null unique references membership_freeze_requests(id),
    membership_id uuid not null references memberships(id),
    scheduled_start_date date not null,
    scheduled_end_date date not null,
    actual_start_date date,
    actual_end_date date,
    credited_days integer not null default 0,
    status varchar(20) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint chk_membership_freeze_period_dates check (scheduled_end_date > scheduled_start_date),
    constraint chk_membership_freeze_period_credit check (credited_days >= 0),
    constraint chk_membership_freeze_period_status check (status in ('SCHEDULED','ACTIVE','COMPLETED','ENDED_EARLY','OBSOLETE'))
);

create table membership_freeze_fee_payments (
    id uuid primary key,
    request_id uuid not null unique references membership_freeze_requests(id),
    amount numeric(19,0) not null,
    currency_code char(3) not null,
    collected_by_account_id uuid not null references accounts(id),
    paid_at timestamptz not null,
    created_at timestamptz not null default current_timestamp,
    constraint chk_membership_freeze_fee_payment_amount check (amount >= 0),
    constraint chk_membership_freeze_fee_payment_currency check (currency_code = 'VND')
);
