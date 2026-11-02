create table course_run_reschedules (
    course_run_id uuid not null references course_runs(id),
    actor_account_id uuid not null references accounts(id),
    idempotency_key varchar(100) not null,
    request_fingerprint varchar(64) not null,
    change_event_id uuid not null unique references course_schedule_change_events(id),
    response_json jsonb not null,
    created_at timestamptz not null default current_timestamp,
    primary key (course_run_id, actor_account_id, idempotency_key),
    constraint chk_course_reschedule_key check (length(btrim(idempotency_key)) > 0),
    constraint chk_course_reschedule_fingerprint check (request_fingerprint ~ '^[0-9a-f]{64}$'),
    constraint chk_course_reschedule_response check (jsonb_typeof(response_json) = 'object')
);
