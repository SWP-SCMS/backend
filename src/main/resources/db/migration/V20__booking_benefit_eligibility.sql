create or replace function validate_booked_booking()
returns trigger
language plpgsql
as $$
declare
    session_row class_sessions%rowtype;
    session_discipline_id uuid;
    member_status varchar(20);
    membership_row memberships%rowtype;
    booked_count bigint;
begin
    if new.status <> 'BOOKED' then
        return new;
    end if;

    select cs.* into session_row
    from class_sessions cs
    where cs.id = new.class_session_id
    for update;

    select sc.discipline_id into session_discipline_id
    from sport_classes sc
    where sc.id = session_row.sport_class_id;

    if session_row.status <> 'SCHEDULED' or session_row.start_time <= current_timestamp then
        raise exception 'Booking requires a future SCHEDULED Session' using errcode = '23514';
    end if;

    select status into member_status
    from accounts
    where id = new.member_account_id and role = 'MEMBER';

    if member_status is distinct from 'ACTIVE' then
        raise exception 'Booking requires an ACTIVE Member Account' using errcode = '23514';
    end if;

    select * into membership_row
    from memberships
    where id = new.membership_id and member_account_id = new.member_account_id;

    if membership_row.status is distinct from 'ACTIVE'
        or membership_row.starts_at > session_row.start_time
        or membership_row.ends_at <= session_row.start_time
        or not exists (
            select 1
            from membership_plan_benefits plan_benefit
            where plan_benefit.plan_code = membership_row.plan_code_snapshot
              and plan_benefit.benefit_code = 'CLASS_BOOKING'
        )
        or exists (
            select 1
            from discipline_required_benefits required_benefit
            where required_benefit.discipline_id = session_discipline_id
              and not exists (
                  select 1
                  from membership_plan_benefits plan_benefit
                  where plan_benefit.plan_code = membership_row.plan_code_snapshot
                    and plan_benefit.benefit_code = required_benefit.benefit_code
              )
        ) then
        raise exception 'Booking requires an ACTIVE Membership with required benefits covering Session start'
            using errcode = '23514';
    end if;

    select count(*) into booked_count
    from bookings
    where class_session_id = new.class_session_id
      and status = 'BOOKED'
      and id <> new.id;

    if booked_count >= session_row.capacity then
        raise exception 'Session capacity is full' using errcode = '23514';
    end if;
    return new;
end;
$$;
