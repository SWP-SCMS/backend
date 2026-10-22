drop index if exists uq_staff_email_change_pending_email;

alter table staff_email_change_requests
    add column purpose_bound boolean not null default false,
    add column completed_at timestamptz,
    add column superseded_at timestamptz;

update staff_email_change_requests
set completed_at = verified_at
where status = 'VERIFIED' and completed_at is null;

alter table staff_email_change_requests drop constraint chk_staff_email_change_status;
alter table staff_email_change_requests add constraint chk_account_email_change_status check (
    status in ('PENDING','VERIFIED','COMPLETED','CANCELLED','EXPIRED','SUPERSEDED'));

comment on table staff_email_change_requests is
    'Account-wide verified email changes; legacy name retained for rolling-deploy and rollback compatibility.';
