create table courses (
    id uuid primary key,
    code varchar(50) not null,
    name varchar(200) not null,
    description text,
    discipline_id uuid not null references disciplines(id),
    image_url text,
    default_minimum_enrollment integer,
    status varchar(20) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint uq_courses_code unique (code),
    constraint chk_courses_code_not_blank check (btrim(code) <> ''),
    constraint chk_courses_name_not_blank check (btrim(name) <> ''),
    constraint chk_courses_default_minimum_enrollment
        check (default_minimum_enrollment is null or default_minimum_enrollment > 0),
    constraint chk_courses_status check (status in ('DRAFT', 'PUBLISHED', 'INACTIVE', 'ARCHIVED'))
);

create index idx_courses_discipline_status on courses (discipline_id, status);

create table course_runs (
    id uuid primary key,
    run_code varchar(50) not null,
    course_id uuid not null references courses(id),
    primary_coach_account_id uuid not null references accounts(id),
    tuition_vnd numeric(19, 0) not null,
    capacity integer not null,
    minimum_enrollment integer not null,
    registration_opens_at timestamptz not null,
    registration_closes_at timestamptz not null,
    planned_start_at timestamptz not null,
    planned_end_at timestamptz not null,
    total_units integer not null,
    named_coach_commitment boolean not null,
    policy_version_snapshot varchar(100),
    status varchar(32) not null,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint uq_course_runs_run_code unique (run_code),
    constraint chk_course_runs_run_code_not_blank check (btrim(run_code) <> ''),
    constraint chk_course_runs_tuition check (tuition_vnd > 0),
    constraint chk_course_runs_capacity check (capacity > 0),
    constraint chk_course_runs_minimum_enrollment
        check (minimum_enrollment > 0 and minimum_enrollment <= capacity),
    constraint chk_course_runs_registration_window check (
        registration_opens_at < registration_closes_at
        and registration_closes_at <= planned_start_at
        and planned_start_at < planned_end_at
    ),
    constraint chk_course_runs_total_units check (total_units > 0),
    constraint chk_course_runs_policy_snapshot check (
        (status = 'DRAFT' and (policy_version_snapshot is null or btrim(policy_version_snapshot) <> ''))
        or (status <> 'DRAFT' and policy_version_snapshot is not null and btrim(policy_version_snapshot) <> '')
    ),
    constraint chk_course_runs_status check (status in (
        'DRAFT', 'OPEN_FOR_REGISTRATION', 'CONFIRMED', 'IN_PROGRESS',
        'SETTLEMENT_PENDING', 'COMPLETED', 'CANCELLED'
    ))
);

create index idx_course_runs_course_status on course_runs (course_id, status);
create index idx_course_runs_primary_coach_start
    on course_runs (primary_coach_account_id, planned_start_at);

create table course_sessions (
    id uuid primary key,
    course_run_id uuid not null references course_runs(id),
    unit_number integer not null,
    room_id uuid not null references rooms(id),
    coach_account_id uuid not null references accounts(id),
    start_at timestamptz not null,
    end_at timestamptz not null,
    original_start_at timestamptz not null,
    original_end_at timestamptz not null,
    allocation_vnd numeric(19, 0) not null,
    status varchar(32) not null,
    replacement_session_id uuid references course_sessions(id),
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint uq_course_sessions_run_unit unique (course_run_id, unit_number),
    constraint chk_course_sessions_unit_number check (unit_number > 0),
    constraint chk_course_sessions_time check (start_at < end_at),
    constraint chk_course_sessions_original_time check (original_start_at < original_end_at),
    constraint chk_course_sessions_allocation check (allocation_vnd > 0),
    constraint chk_course_sessions_replacement check (replacement_session_id is null or replacement_session_id <> id),
    constraint chk_course_sessions_status check (status in (
        'SCHEDULED', 'CANCELLED_PENDING_MAKEUP', 'MAKEUP_SCHEDULED',
        'COMPLETED', 'UNRESOLVED', 'RESOLVED_BY_REFUND'
    ))
);

create index idx_course_sessions_run_start on course_sessions (course_run_id, start_at);
create index idx_course_sessions_room_start on course_sessions (room_id, start_at);
create index idx_course_sessions_coach_start on course_sessions (coach_account_id, start_at);
create index idx_course_sessions_replacement on course_sessions (replacement_session_id)
    where replacement_session_id is not null;
