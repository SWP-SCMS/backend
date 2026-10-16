alter table recurring_schedules
    add constraint chk_recurring_schedules_max_duration
    check (end_time - start_time <= interval '90 minutes') not valid;

alter table class_sessions
    add constraint chk_class_sessions_max_duration
    check (end_time - start_time <= interval '90 minutes') not valid;

create table room_suspensions (
    id uuid primary key,
    room_id uuid not null references rooms(id),
    start_time timestamptz not null,
    end_time timestamptz not null,
    reason varchar(1000),
    created_by_account_id uuid not null references accounts(id),
    created_at timestamptz not null default current_timestamp,

    constraint chk_room_suspensions_time check (end_time > start_time),
    constraint chk_room_suspensions_reason check (reason is null or btrim(reason) <> '')
);

alter table room_suspensions
    add constraint ex_room_suspensions_time exclude using gist (
        room_id with =,
        tstzrange(start_time, end_time, '[)') with &&
    );

create index idx_room_suspensions_room_start_time
    on room_suspensions (room_id, start_time);
