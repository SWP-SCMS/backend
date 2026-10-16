alter table payments
    add column legacy_provider_transaction_id varchar(120);

alter table payments disable trigger trg_payments_terminal_immutable;

with ranked as (
    select id, row_number() over (
        partition by coalesce(provider, 'SEPAY'), provider_transaction_id
        order by created_at, id
    ) as position
    from payments
    where provider_transaction_id is not null
)
update payments p
set legacy_provider_transaction_id = p.provider_transaction_id,
    provider_transaction_id = null
from ranked
where p.id = ranked.id and ranked.position > 1;

update payments
set provider = 'SEPAY'
where provider is null
  and (provider_reference is not null
    or provider_transaction_id is not null
    or legacy_provider_transaction_id is not null);

alter table payments enable trigger trg_payments_terminal_immutable;
