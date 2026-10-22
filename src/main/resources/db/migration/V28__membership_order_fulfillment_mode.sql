alter table membership_orders
    add column fulfillment_mode varchar(20) not null default 'IMMEDIATE';

alter table membership_orders
    add column cash_idempotency_key varchar(100);

update membership_orders
set fulfillment_mode = 'IMMEDIATE'
where fulfillment_mode is null;

alter table membership_orders
    add constraint chk_membership_orders_fulfillment_mode
    check (fulfillment_mode in ('IMMEDIATE', 'RESERVED'));

create unique index uq_membership_orders_cash_idempotency
    on membership_orders (created_by_account_id, cash_idempotency_key)
    where cash_idempotency_key is not null;
