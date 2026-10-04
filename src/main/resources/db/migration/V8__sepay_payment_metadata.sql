alter table payments add column if not exists provider varchar(30);
alter table payments add column if not exists provider_transaction_id varchar(120);
alter table payments add column if not exists evidence text;

create unique index if not exists uq_payments_provider_transaction
    on payments (provider, provider_transaction_id)
    where provider is not null and provider_transaction_id is not null;

create index if not exists idx_payments_status_created_at on payments (status, created_at desc);
