alter table membership_freeze_requests drop constraint chk_membership_freeze_request_status;
alter table membership_freeze_requests add constraint chk_membership_freeze_request_status
    check (status in ('PENDING','APPROVED_PENDING_PAYMENT','SCHEDULED','ACTIVE','COMPLETED','OBSOLETE','REJECTED'));

drop index uq_membership_freeze_request_open_membership;
create unique index uq_membership_freeze_request_open_membership
    on membership_freeze_requests(membership_id)
    where status in ('PENDING','APPROVED_PENDING_PAYMENT','SCHEDULED','ACTIVE');

alter table bookings drop constraint chk_bookings_cancellation_source;
alter table bookings add constraint chk_bookings_cancellation_source check (
    cancellation_source is null or cancellation_source in ('MEMBER','RECEPTIONIST','MEMBER_SUSPENDED','SESSION_CANCELLED','MEMBERSHIP_TERMINATED','MEMBERSHIP_FREEZE')
);

alter table center_visits drop constraint chk_center_visits_checkout_source;
alter table center_visits add constraint chk_center_visits_checkout_source check (
    checkout_source is null
    or checkout_source in ('MEMBER', 'RECEPTIONIST', 'AUTO_REENTRY', 'MIGRATION', 'MEMBERSHIP_TERMINATION', 'MEMBERSHIP_FREEZE')
);

create or replace function reject_booking_during_active_membership_freeze()
returns trigger language plpgsql as $$
begin
    if exists (
        select 1 from membership_freeze_periods freeze_period
        join memberships membership on membership.id = freeze_period.membership_id
        where membership.member_account_id = new.member_account_id and freeze_period.status = 'ACTIVE'
          and freeze_period.scheduled_start_date <= (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date
          and freeze_period.scheduled_end_date > (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date
    ) then
        raise exception 'MEMBERSHIP_FROZEN' using errcode = '23514';
    end if;
    return new;
end;
$$;

create trigger trg_bookings_active_membership_freeze
before insert on bookings
for each row execute function reject_booking_during_active_membership_freeze();
