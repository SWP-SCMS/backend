create table coach_leave_requests (
    id uuid primary key,
    coach_account_id uuid not null references accounts(id),
    start_time timestamptz not null,
    end_time timestamptz not null,
    reason varchar(1000) not null,
    status varchar(20) not null,
    reviewed_by_account_id uuid references accounts(id),
    review_note varchar(1000),
    reviewed_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,

    constraint chk_coach_leave_interval check (end_time > start_time),
    constraint chk_coach_leave_status check (status in ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN')),
    constraint chk_coach_leave_review check (
        (status in ('PENDING', 'WITHDRAWN') and reviewed_by_account_id is null and reviewed_at is null)
        or (status in ('APPROVED', 'REJECTED') and reviewed_by_account_id is not null and reviewed_at is not null)
    )
);

create index idx_coach_leave_requests_coach_status_time
    on coach_leave_requests (coach_account_id, status, start_time, end_time);

create unique index uq_coach_leave_open_interval
    on coach_leave_requests (coach_account_id, start_time, end_time)
    where status in ('PENDING', 'APPROVED');

