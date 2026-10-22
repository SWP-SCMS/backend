alter table memberships drop constraint chk_memberships_status;
alter table memberships drop constraint chk_memberships_time;
alter table memberships alter column starts_at drop not null;
alter table memberships alter column ends_at drop not null;

-- V24 allowed non-overlapping ACTIVE history; reconcile already-ended rows before tightening cardinality.
update memberships
set status = 'EXPIRED', updated_at = current_timestamp, version = version + 1
where status = 'ACTIVE' and ends_at <= current_timestamp;

do $$
begin
    if exists (
        select 1 from memberships where status = 'ACTIVE'
        group by member_account_id having count(*) > 1
    ) then
        raise exception 'V27 cannot choose between multiple future ACTIVE Memberships for one Member';
    end if;
end $$;

alter table memberships add constraint chk_memberships_status
    check (status in ('RESERVED', 'ACTIVE', 'EXPIRED', 'TERMINATED'));

alter table memberships add constraint chk_memberships_time check (
    (status = 'RESERVED' and starts_at is null and ends_at is null)
    or (status <> 'RESERVED' and starts_at is not null and ends_at is not null and ends_at > starts_at)
);

alter table memberships drop constraint ex_memberships_no_active_overlap;

create unique index uq_memberships_one_active_per_member
    on memberships(member_account_id) where status = 'ACTIVE';

create unique index uq_memberships_one_reserved_per_member
    on memberships(member_account_id) where status = 'RESERVED';
