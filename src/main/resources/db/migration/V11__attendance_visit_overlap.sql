create or replace function validate_attendance_visit_owner()
returns trigger
language plpgsql
as $$
declare
    booking_member_id uuid;
    visit_member_id uuid;
    visit_checked_in_at timestamptz;
    visit_checked_out_at timestamptz;
    session_start_time timestamptz;
    session_end_time timestamptz;
begin
    if new.status = 'PRESENT' and new.center_visit_id is not null then
        select bookings.member_account_id, class_sessions.start_time, class_sessions.end_time
        into booking_member_id, session_start_time, session_end_time
        from bookings
        join class_sessions on class_sessions.id = bookings.class_session_id
        where bookings.id = new.booking_id;

        select member_account_id, checked_in_at, checked_out_at
        into visit_member_id, visit_checked_in_at, visit_checked_out_at
        from center_visits
        where id = new.center_visit_id;

        if booking_member_id is distinct from visit_member_id then
            raise exception 'Attendance Center Visit must belong to the booked Member';
        end if;

        if visit_checked_in_at > current_timestamp then
            raise exception 'Attendance Center Visit cannot be in the future';
        end if;

        if visit_checked_in_at >= session_end_time
            or (visit_checked_out_at is not null and visit_checked_out_at <= session_start_time) then
            raise exception 'Attendance Center Visit must overlap the Session';
        end if;
    end if;
    return new;
end;
$$;
