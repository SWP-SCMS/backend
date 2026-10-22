create table session_waitlists (
    id uuid primary key,
    class_session_id uuid not null references class_sessions(id),
    member_account_id uuid not null references accounts(id),
    status varchar(20) not null,
    joined_by_account_id uuid not null references accounts(id),
    source varchar(20) not null,
    terminal_reason_code varchar(80),
    terminal_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,

    constraint chk_session_waitlists_status check (status in ('WAITING', 'PROMOTED', 'CANCELLED', 'SKIPPED', 'EXPIRED')),
    constraint chk_session_waitlists_source check (source in ('MEMBER', 'RECEPTIONIST')),
    constraint chk_session_waitlists_terminal check (
        (status = 'WAITING' and terminal_at is null and terminal_reason_code is null)
        or (status <> 'WAITING' and terminal_at is not null)
    )
);

create unique index uq_session_waitlists_waiting_member
    on session_waitlists (class_session_id, member_account_id)
    where status = 'WAITING';

create index idx_session_waitlists_fifo
    on session_waitlists (class_session_id, status, created_at, id);

create index idx_session_waitlists_member_created
    on session_waitlists (member_account_id, created_at desc);
