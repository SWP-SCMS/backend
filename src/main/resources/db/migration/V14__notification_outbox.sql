create table notification_outbox (
    id uuid primary key,
    event_key varchar(200) not null unique,
    recipient_account_id uuid not null references accounts(id),
    notification_type varchar(80) not null,
    target_type varchar(80) not null,
    target_id uuid,
    payload jsonb not null default '{}'::jsonb,
    status varchar(20) not null default 'PENDING',
    attempt_count integer not null default 0,
    next_attempt_at timestamptz default current_timestamp,
    last_attempt_at timestamptz,
    delivered_at timestamptz,
    last_error varchar(1000),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint chk_notification_outbox_status check (status in ('PENDING','PROCESSING','SENT','FAILED','TERMINAL_FAILED')),
    constraint chk_notification_outbox_attempt_count check (attempt_count >= 0),
    constraint chk_notification_outbox_payload check (jsonb_typeof(payload) = 'object')
);

create index idx_notification_outbox_retry
    on notification_outbox (next_attempt_at, created_at)
    where status in ('PENDING','FAILED');
