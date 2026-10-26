create or replace function reject_booking_during_active_membership_freeze()
returns trigger language plpgsql as $$
begin
    if exists (
        select 1 from membership_freeze_periods freeze_period
        join memberships membership on membership.id = freeze_period.membership_id
        where membership.member_account_id = new.member_account_id
          and freeze_period.status in ('SCHEDULED', 'ACTIVE')
          and freeze_period.scheduled_start_date <= (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date
          and freeze_period.scheduled_end_date > (current_timestamp at time zone 'Asia/Ho_Chi_Minh')::date
    ) then
        raise exception 'MEMBERSHIP_FROZEN' using errcode = '23514';
    end if;
    return new;
end;
$$;
