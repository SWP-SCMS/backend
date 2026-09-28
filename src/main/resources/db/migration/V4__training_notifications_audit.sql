create table coach_assignments (
    id uuid primary key,
    member_account_id uuid not null references accounts(id),
    coach_account_id uuid not null references accounts(id),
    assigned_by_account_id uuid not null references accounts(id),
    status varchar(20) not null,
    starts_at timestamptz not null default current_timestamp,
    ends_at timestamptz,
    end_reason text,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_coach_assignments_status check (status in ('ACTIVE', 'ENDED')),
    constraint chk_coach_assignments_ends_at check (
        (status = 'ACTIVE' and ends_at is null)
        or (status = 'ENDED' and ends_at is not null and ends_at >= starts_at)
    )
);

create unique index uq_coach_assignments_active_pair
    on coach_assignments (member_account_id, coach_account_id)
    where status = 'ACTIVE';

create index idx_coach_assignments_coach_status
    on coach_assignments (coach_account_id, status);

create table training_plans (
    id uuid primary key,
    member_account_id uuid not null references accounts(id),
    coach_assignment_id uuid not null references coach_assignments(id),
    title varchar(200) not null,
    status varchar(20) not null,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_training_plans_status check (status in ('ACTIVE', 'ARCHIVED')),
    constraint chk_training_plans_title_not_blank check (btrim(title) <> '')
);

create index idx_training_plans_member_created_at
    on training_plans (member_account_id, created_at desc);

create table training_plan_versions (
    id uuid primary key,
    training_plan_id uuid not null references training_plans(id),
    version_number integer not null,
    content jsonb not null,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,

    constraint uq_training_plan_versions_number unique (training_plan_id, version_number),
    constraint chk_training_plan_versions_number check (version_number > 0),
    constraint chk_training_plan_versions_content check (jsonb_typeof(content) = 'object')
);

create table training_results (
    id uuid primary key,
    member_account_id uuid not null references accounts(id),
    coach_assignment_id uuid not null references coach_assignments(id),
    training_plan_version_id uuid references training_plan_versions(id),
    class_session_id uuid references class_sessions(id),
    activity_at timestamptz not null,
    content jsonb not null,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_training_results_reference check (
        training_plan_version_id is not null or class_session_id is not null
    ),
    constraint chk_training_results_content check (jsonb_typeof(content) = 'object')
);

create index idx_training_results_member_activity_at
    on training_results (member_account_id, activity_at desc);

create table feedback (
    id uuid primary key,
    member_account_id uuid not null references accounts(id),
    coach_assignment_id uuid not null references coach_assignments(id),
    training_plan_version_id uuid references training_plan_versions(id),
    training_result_id uuid references training_results(id),
    content text not null,
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_feedback_context check (
        training_plan_version_id is not null or training_result_id is not null
    ),
    constraint chk_feedback_content_not_blank check (btrim(content) <> '')
);

create index idx_feedback_member_created_at
    on feedback (member_account_id, created_at desc);

create table notifications (
    id uuid primary key,
    recipient_account_id uuid not null references accounts(id),
    notification_type varchar(80) not null,
    target_type varchar(80) not null,
    target_id uuid,
    payload jsonb not null default '{}'::jsonb,
    delivery_status varchar(20) not null,
    delivery_attempt_count integer not null default 0,
    last_delivery_attempt_at timestamptz,
    delivered_at timestamptz,
    read_at timestamptz,
    created_at timestamptz not null default current_timestamp,

    constraint chk_notifications_delivery_status check (delivery_status in ('PENDING', 'SENT', 'FAILED')),
    constraint chk_notifications_attempt_count check (delivery_attempt_count >= 0),
    constraint chk_notifications_payload check (jsonb_typeof(payload) = 'object')
);

create index idx_notifications_recipient_unread
    on notifications (recipient_account_id, created_at desc)
    where read_at is null;

create index idx_notifications_retry
    on notifications (created_at)
    where delivery_status in ('PENDING', 'FAILED');

create table audit_events (
    id uuid primary key,
    actor_account_id uuid references accounts(id),
    action varchar(120) not null,
    target_type varchar(80) not null,
    target_id uuid,
    reason text,
    before_data jsonb,
    after_data jsonb,
    created_at timestamptz not null default current_timestamp,

    constraint chk_audit_events_action_not_blank check (btrim(action) <> ''),
    constraint chk_audit_events_target_type_not_blank check (btrim(target_type) <> '')
);

create index idx_audit_events_target_created_at
    on audit_events (target_type, target_id, created_at desc);

create index idx_audit_events_actor_created_at
    on audit_events (actor_account_id, created_at desc);

create function prevent_history_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception '% is append-only', tg_table_name;
end;
$$;

create trigger trg_training_plan_versions_append_only
before update or delete on training_plan_versions
for each row execute function prevent_history_mutation();

create trigger trg_audit_events_append_only
before update or delete on audit_events
for each row execute function prevent_history_mutation();
