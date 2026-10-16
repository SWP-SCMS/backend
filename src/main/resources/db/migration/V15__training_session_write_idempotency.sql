-- A session has one result per booked Member and one feedback per session result.
alter table training_results
    add column legacy_duplicate boolean not null default false;

with ranked as (
    select id, row_number() over (
        partition by class_session_id, member_account_id
        order by created_at, id
    ) as position
    from training_results
    where class_session_id is not null
)
update training_results r
set legacy_duplicate = true
from ranked
where r.id = ranked.id and ranked.position > 1;

create unique index uq_training_results_session_member
    on training_results (class_session_id, member_account_id)
    where class_session_id is not null and not legacy_duplicate;

alter table feedback
    add column legacy_duplicate boolean not null default false;

with ranked as (
    select id, row_number() over (
        partition by training_result_id
        order by created_at, id
    ) as position
    from feedback
    where training_result_id is not null
      and coach_assignment_id is null
      and training_plan_version_id is null
)
update feedback f
set legacy_duplicate = true
from ranked
where f.id = ranked.id and ranked.position > 1;

create unique index uq_feedback_session_result
    on feedback (training_result_id)
    where training_result_id is not null
      and coach_assignment_id is null
      and training_plan_version_id is null
      and not legacy_duplicate
;
