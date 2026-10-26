create table payment_provider_events (
    id uuid primary key,
    provider varchar(20) not null,
    provider_transaction_id varchar(100) not null,
    payment_id uuid references payments(id),
    transfer_amount numeric(19,0) not null,
    payment_reference varchar(255),
    account_number_last4 varchar(4),
    payload_sha256 char(64) not null,
    outcome varchar(50) not null,
    detail varchar(500) not null,
    received_at timestamptz not null default current_timestamp,

    constraint uq_payment_provider_events_provider_transaction
        unique (provider, provider_transaction_id),
    constraint chk_payment_provider_events_outcome check (outcome in (
        'FULFILLED',
        'ALREADY_PROCESSED',
        'PAYMENT_WINDOW_EXPIRED',
        'DESTINATION_SNAPSHOT_MISSING',
        'REFERENCE_MISMATCH',
        'AMOUNT_MISMATCH',
        'ACCOUNT_MISMATCH',
        'PAYMENT_NOT_PAYABLE'
    ))
);

create index idx_payment_provider_events_payment_received_at
    on payment_provider_events (payment_id, received_at desc);

create trigger trg_payment_provider_events_append_only
before update or delete on payment_provider_events
for each row execute function prevent_history_mutation();
