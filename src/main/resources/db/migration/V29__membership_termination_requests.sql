create table membership_termination_requests (
    id uuid primary key,
    membership_id uuid not null references memberships(id),
    member_account_id uuid not null references accounts(id),
    requester_account_id uuid not null references accounts(id),
    approver_account_id uuid references accounts(id),
    mode varchar(20) not null,
    status varchar(20) not null,
    reason text not null,
    effective_at timestamptz,
    approved_at timestamptz,
    rejected_at timestamptz,
    withdrawn_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint chk_membership_termination_mode check (mode in ('IMMEDIATE','END_OF_CYCLE')),
    constraint chk_membership_termination_status check (status in ('PENDING','APPROVED','EXECUTED','REJECTED','WITHDRAWN','OBSOLETE')),
    constraint chk_membership_termination_reason check (btrim(reason) <> '')
);

create index idx_membership_termination_member_created
    on membership_termination_requests(member_account_id, created_at desc);

create index idx_membership_termination_status_created
    on membership_termination_requests(status, created_at desc);

create unique index uq_membership_termination_open_membership
    on membership_termination_requests(membership_id)
    where status in ('PENDING','APPROVED');
