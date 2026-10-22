create table password_reset_requests (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    status varchar(20) not null,
    generation integer not null,
    otp_digest varchar(128) not null,
    hmac_key_version varchar(40) not null,
    expires_at timestamptz not null,
    attempt_count integer not null default 0,
    max_attempts integer not null default 5,
    resend_available_at timestamptz not null,
    verified_at timestamptz,
    completed_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint chk_password_reset_status check (status in
        ('PENDING','VERIFIED','COMPLETED','EXPIRED','SUPERSEDED')),
    constraint chk_password_reset_attempts check (
        generation > 0 and attempt_count >= 0 and max_attempts > 0 and attempt_count <= max_attempts)
);
create unique index uq_password_reset_active_account on password_reset_requests(account_id)
    where status in ('PENDING','VERIFIED');
create index ix_password_reset_account_created on password_reset_requests(account_id, created_at desc);

create table password_reset_sessions (
    id uuid primary key,
    request_id uuid not null references password_reset_requests(id),
    account_id uuid not null references accounts(id),
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp
);
create unique index uq_password_reset_active_session on password_reset_sessions(account_id)
    where used_at is null and revoked_at is null;
