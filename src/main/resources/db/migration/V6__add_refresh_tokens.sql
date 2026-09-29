create table refresh_tokens (
    id uuid primary key,
    account_id uuid not null references accounts(id),
    token_hash varchar(64) not null,
    created_at timestamptz not null default current_timestamp,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    replaced_by_token_id uuid references refresh_tokens(id),
    version bigint not null default 0,

    constraint uq_refresh_tokens_token_hash unique (token_hash),
    constraint chk_refresh_tokens_hash_format check (token_hash ~ '^[0-9a-f]{64}$'),
    constraint chk_refresh_tokens_expiry check (expires_at > created_at),
    constraint chk_refresh_tokens_revocation check (revoked_at is null or revoked_at >= created_at),
    constraint chk_refresh_tokens_replacement_revoked
        check (replaced_by_token_id is null or revoked_at is not null),
    constraint chk_refresh_tokens_no_self_replacement
        check (replaced_by_token_id is null or replaced_by_token_id <> id)
);

create index ix_refresh_tokens_account_id on refresh_tokens (account_id);

create index ix_refresh_tokens_active_expiry
    on refresh_tokens (expires_at)
    where revoked_at is null;
