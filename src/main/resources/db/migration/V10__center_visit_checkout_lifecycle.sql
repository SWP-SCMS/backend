alter table center_visits
    add column checked_out_at timestamptz,
    add column checked_out_by_account_id uuid references accounts(id),
    add column checkout_source varchar(30),
    add column idempotency_key varchar(100);

with ordered_visits as (
    select id,
           lead(checked_in_at) over (
               partition by member_account_id
               order by checked_in_at, id
           ) as next_checked_in_at
    from center_visits
)
update center_visits visit
set checked_out_at = coalesce(
        ordered.next_checked_in_at,
        (((visit.checked_in_at at time zone 'Asia/Ho_Chi_Minh')::date + 1)::timestamp
            at time zone 'Asia/Ho_Chi_Minh')
    ),
    checked_out_by_account_id = visit.checked_in_by_account_id,
    checkout_source = 'MIGRATION'
from ordered_visits ordered
where ordered.id = visit.id;

alter table center_visits
    add constraint chk_center_visits_checkout_coherent check (
        (checked_out_at is null and checked_out_by_account_id is null and checkout_source is null)
        or (checked_out_at is not null and checked_out_by_account_id is not null and checkout_source is not null)
    ),
    add constraint chk_center_visits_checkout_time check (
        checked_out_at is null or checked_out_at >= checked_in_at
    ),
    add constraint chk_center_visits_checkout_source check (
        checkout_source is null
        or checkout_source in ('MEMBER', 'RECEPTIONIST', 'AUTO_REENTRY', 'MIGRATION')
    );

create unique index uq_center_visits_receptionist_idempotency
    on center_visits (checked_in_by_account_id, idempotency_key)
    where idempotency_key is not null;

create unique index uq_center_visits_open_member
    on center_visits (member_account_id)
    where checked_out_at is null;
