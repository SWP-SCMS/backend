create table course_waitlists (
    id uuid primary key,
    course_run_id uuid not null references course_runs(id),
    member_account_id uuid not null references accounts(id),
    status varchar(20) not null,
    invited_at timestamptz,
    invitation_expires_at timestamptz,
    accepted_order_id uuid,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint chk_course_waitlists_status check (status in ('WAITING','INVITED','ACCEPTED','LEFT','EXPIRED','CANCELLED')),
    constraint chk_course_waitlists_invitation check (
        (status = 'WAITING' and invited_at is null and invitation_expires_at is null and accepted_order_id is null)
        or (status = 'INVITED' and invited_at is not null and invitation_expires_at is not null
            and invitation_expires_at > invited_at and accepted_order_id is null)
        or (status = 'ACCEPTED' and invited_at is not null and invitation_expires_at is not null
            and accepted_order_id is not null)
        or (status in ('LEFT','EXPIRED','CANCELLED') and accepted_order_id is null)
    )
);

alter table course_waitlists
    add constraint fk_course_waitlists_accepted_order_tuple
    foreign key (accepted_order_id, member_account_id, course_run_id)
    references course_orders (id, member_account_id, course_run_id);

create unique index uq_course_waitlists_active_member_run
    on course_waitlists(course_run_id, member_account_id)
    where status in ('WAITING','INVITED');
create index idx_course_waitlists_promotion
    on course_waitlists(course_run_id, status, created_at, id);
create index idx_course_waitlists_invitation_expiry
    on course_waitlists(invitation_expires_at, id)
    where status = 'INVITED';
