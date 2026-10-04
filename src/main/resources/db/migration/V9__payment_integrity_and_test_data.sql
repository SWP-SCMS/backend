alter table accounts add column if not exists is_test_data boolean not null default false;
alter table membership_orders add column if not exists is_test_data boolean not null default false;

update accounts
set is_test_data = true,
    status = case when role = 'MEMBER' then 'SUSPENDED' else 'INACTIVE' end,
    updated_at = current_timestamp
where lower(email) like 'qa.%@example.test';

update membership_orders o
set is_test_data = true,
    updated_at = current_timestamp
where exists (select 1 from accounts a where a.id = o.member_account_id and a.is_test_data);

update membership_offers
set status = 'INACTIVE', updated_at = current_timestamp
where name like 'QA Offer %';

create or replace function set_order_test_data_from_member()
returns trigger language plpgsql as $$
begin
    select is_test_data into new.is_test_data from accounts where id = new.member_account_id;
    return new;
end;
$$;

create trigger trg_membership_orders_test_data
before insert or update of member_account_id on membership_orders
for each row execute function set_order_test_data_from_member();

with ranked as (
    select id, row_number() over (partition by order_id order by created_at, id) as position
    from payments
    where status = 'PENDING'
)
update payments p
set status = 'FAILED',
    failure_reason = 'Superseded duplicate pending attempt during V9 migration',
    updated_at = current_timestamp
from ranked r
where p.id = r.id and r.position > 1;

create unique index uq_payments_pending_order
    on payments (order_id)
    where status = 'PENDING';

create or replace function protect_terminal_payment()
returns trigger language plpgsql as $$
begin
    if old.status in ('PAID', 'FAILED') and (
        new.status is distinct from old.status or
        new.amount is distinct from old.amount or
        new.currency_code is distinct from old.currency_code or
        new.provider_reference is distinct from old.provider_reference or
        new.provider_transaction_id is distinct from old.provider_transaction_id or
        new.paid_at is distinct from old.paid_at
    ) then
        raise exception 'Terminal Payment cannot be modified';
    end if;
    return new;
end;
$$;

create trigger trg_payments_terminal_immutable
before update on payments
for each row execute function protect_terminal_payment();
