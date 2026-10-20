alter table accounts
    add column email_verified_at timestamptz,
    add column email_verification_required boolean not null default false,
    add column registration_expires_at timestamptz;

alter table accounts drop constraint chk_accounts_role_lifecycle;
alter table accounts add constraint chk_accounts_role_lifecycle check (
    (role = 'MEMBER' and status in ('ACTIVE', 'SUSPENDED', 'INACTIVE'))
    or (role in ('COACH', 'RECEPTIONIST', 'MANAGER') and status in ('ACTIVE', 'INACTIVE'))
);
alter table accounts add constraint chk_accounts_email_verification_lifecycle check (
    email_verification_required = false
    or (role = 'MEMBER' and status = 'INACTIVE' and email_verified_at is null
        and registration_expires_at is not null)
);

drop index if exists uq_accounts_current_email;
create unique index uq_accounts_current_email on accounts (lower(email))
    where status <> 'INACTIVE' or email_verification_required = true;
drop index if exists uq_accounts_current_phone;
create unique index uq_accounts_current_phone on accounts (phone)
    where status <> 'INACTIVE' or email_verification_required = true;

create table email_verification_challenges (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    generation integer not null,
    otp_digest varchar(128) not null,
    hmac_key_version varchar(40) not null,
    expires_at timestamptz not null,
    attempt_count integer not null default 0,
    max_attempts integer not null default 5,
    resend_available_at timestamptz not null,
    consumed_at timestamptz,
    superseded_at timestamptz,
    locked_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint chk_email_verification_challenge_counts check (
        generation > 0 and attempt_count >= 0 and max_attempts > 0 and attempt_count <= max_attempts)
);
create unique index uq_email_verification_active_challenge
    on email_verification_challenges (account_id)
    where consumed_at is null and superseded_at is null;
create index ix_email_verification_challenge_account on email_verification_challenges(account_id, generation desc);

create table email_verification_sessions (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    token_hash varchar(64) not null unique,
    csrf_token_hash varchar(64) not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    change_email_count integer not null default 0,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint chk_email_verification_change_count check (change_email_count between 0 and 3)
);
create unique index uq_email_verification_active_session
    on email_verification_sessions (account_id) where revoked_at is null;

create table email_outbox (
    id uuid primary key,
    event_key varchar(160) not null unique,
    account_id uuid not null references accounts(id),
    generation integer not null,
    recipient_email varchar(320) not null,
    template_key varchar(80) not null,
    deliver_before timestamptz not null,
    payload_ciphertext bytea,
    payload_nonce bytea,
    encryption_key_version varchar(40) not null,
    status varchar(24) not null,
    retry_count integer not null default 0,
    next_retry_at timestamptz not null,
    locked_at timestamptz,
    locked_by varchar(120),
    sent_at timestamptz,
    last_error varchar(500),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint chk_email_outbox_status check (status in
        ('PENDING','PROCESSING','SENT','FAILED','TERMINAL_FAILED','CANCELLED')),
    constraint chk_email_outbox_payload check (
        (status in ('PENDING','PROCESSING','FAILED') and payload_ciphertext is not null and payload_nonce is not null)
        or (status in ('SENT','TERMINAL_FAILED','CANCELLED') and payload_ciphertext is null and payload_nonce is null))
);
create index ix_email_outbox_available on email_outbox(status, next_retry_at, created_at);

create table email_verification_rate_limits (
    bucket_type varchar(60) not null,
    rate_key_hash varchar(64) not null,
    window_started_at timestamptz not null,
    request_count integer not null,
    expires_at timestamptz not null,
    updated_at timestamptz not null,
    primary key (bucket_type, rate_key_hash, window_started_at)
);
create index ix_email_verification_rate_limit_expiry on email_verification_rate_limits(expires_at);

create table email_verification_manual_activation_requests (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    requested_by_account_id uuid not null references accounts(id),
    reason_code varchar(40) not null,
    reason_detail varchar(1000) not null,
    verification_method varchar(60) not null,
    evidence_note varchar(2000) not null,
    status varchar(20) not null,
    reviewed_by_account_id uuid references accounts(id),
    decision_note varchar(1000),
    terminal_reason varchar(80),
    requested_at timestamptz not null,
    reviewed_at timestamptz,
    expires_at timestamptz not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint chk_manual_activation_status check (status in
        ('PENDING','APPROVED','REJECTED','CANCELLED','EXPIRED'))
);
create unique index uq_manual_activation_pending_account
    on email_verification_manual_activation_requests(account_id) where status = 'PENDING';
create index ix_manual_activation_queue
    on email_verification_manual_activation_requests(status, requested_at desc, id desc);
