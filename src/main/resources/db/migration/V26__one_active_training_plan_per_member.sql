-- Preserve the newest ACTIVE plan when legacy data contains duplicates.
with ranked as (
    select id, row_number() over (
        partition by member_account_id order by updated_at desc, created_at desc, id desc
    ) as position
    from training_plans
    where status = 'ACTIVE'
)
update training_plans p
set status = 'ARCHIVED', updated_at = current_timestamp, version = version + 1
from ranked r
where p.id = r.id and r.position > 1;

create unique index uq_training_plans_one_active_member
    on training_plans (member_account_id)
    where status = 'ACTIVE';
