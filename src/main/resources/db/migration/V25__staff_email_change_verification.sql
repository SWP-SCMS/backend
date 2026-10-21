alter table accounts add column auth_version bigint not null default 0;

create table staff_email_change_requests (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    pending_email varchar(320) not null,
    generation integer not null default 1,
    otp_digest varchar(128) not null,
    hmac_key_version varchar(40) not null,
    expires_at timestamptz not null,
    resend_available_at timestamptz not null,
    attempt_count integer not null default 0,
    max_attempts integer not null,
    status varchar(20) not null,
    verified_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    version bigint not null default 0,
    constraint chk_staff_email_change_status check (status in ('PENDING','VERIFIED','CANCELLED','EXPIRED')),
    constraint chk_staff_email_change_attempts check (generation > 0 and attempt_count >= 0
        and max_attempts > 0 and attempt_count <= max_attempts)
);

create unique index uq_staff_email_change_pending_account
    on staff_email_change_requests(account_id) where status = 'PENDING';
create unique index uq_staff_email_change_pending_email
    on staff_email_change_requests(lower(pending_email)) where status = 'PENDING';
create index ix_staff_email_change_expiry on staff_email_change_requests(status, expires_at);
