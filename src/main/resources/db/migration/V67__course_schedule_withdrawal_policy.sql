create table course_schedule_change_events (
    id uuid primary key,
    course_run_id uuid not null references course_runs(id),
    actor_account_id uuid references accounts(id),
    cause_code varchar(40) not null,
    old_planned_start_at timestamptz not null,
    old_planned_end_at timestamptz not null,
    new_planned_start_at timestamptz not null,
    new_planned_end_at timestamptz not null,
    old_total_units integer not null,
    new_total_units integer not null,
    created_at timestamptz not null default current_timestamp,
    constraint chk_course_schedule_change_event_cause check
        (cause_code in ('VOLUNTARY', 'START_DELAY_OVER_7_DAYS', 'MEMBER_SCHEDULE_CONFLICT', 'REDUCED_UNIT_COUNT')),
    constraint chk_course_schedule_change_event_units check (old_total_units > 0 and new_total_units > 0)
);

create index idx_course_schedule_change_event_run on course_schedule_change_events(course_run_id, created_at desc, id desc);

alter table course_schedule_change_responses
    add column cause_code varchar(40) not null default 'VOLUNTARY',
    add column change_event_id uuid references course_schedule_change_events(id);

alter table course_schedule_change_responses
    add constraint chk_course_schedule_change_response_cause check
        (cause_code in ('VOLUNTARY', 'START_DELAY_OVER_7_DAYS', 'MEMBER_SCHEDULE_CONFLICT', 'REDUCED_UNIT_COUNT'));

create index idx_course_schedule_change_response_event
    on course_schedule_change_responses(change_event_id, created_at desc, id desc);

alter table course_withdrawal_requests
    add column cause_code varchar(40) not null default 'VOLUNTARY',
    add column schedule_change_response_id uuid references course_schedule_change_responses(id),
    add column change_event_id uuid references course_schedule_change_events(id);

alter table course_withdrawal_requests
    add constraint chk_course_withdrawal_cause check
        (cause_code in ('VOLUNTARY', 'START_DELAY_OVER_7_DAYS', 'MEMBER_SCHEDULE_CONFLICT', 'REDUCED_UNIT_COUNT'));

create unique index uq_course_withdrawal_schedule_response
    on course_withdrawal_requests(schedule_change_response_id)
    where schedule_change_response_id is not null;

create index idx_course_withdrawal_change_event
    on course_withdrawal_requests(change_event_id, requested_at desc, id desc);

alter table course_sessions
    drop constraint chk_course_sessions_status,
    add constraint chk_course_sessions_status check (status in (
        'SCHEDULED', 'CANCELLED_PENDING_MAKEUP', 'MAKEUP_SCHEDULED',
        'COMPLETED', 'UNRESOLVED', 'RESOLVED_BY_REFUND', 'REMOVED_BY_RUN_CHANGE'
    ));

alter table course_enrollment_sessions
    alter column disposition type varchar(32),
    drop constraint chk_course_enrollment_sessions_disposition,
    add constraint chk_course_enrollment_sessions_disposition
        check (disposition in ('ENROLLED', 'NOT_ENROLLED', 'UNRESOLVED', 'REMOVED_BY_RUN_CHANGE'));
