alter table memberships drop constraint chk_memberships_status;
alter table memberships add constraint chk_memberships_status
    check (status in ('RESERVED', 'ACTIVE', 'EXPIRED', 'TERMINATED', 'CANCELLED'));

alter table memberships drop constraint chk_memberships_time;
alter table memberships add constraint chk_memberships_time check (
    (status in ('RESERVED', 'CANCELLED') and starts_at is null and ends_at is null)
    or (status not in ('RESERVED', 'CANCELLED') and starts_at is not null and ends_at is not null and ends_at > starts_at)
);
