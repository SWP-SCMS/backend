alter table rooms
    add column usage_mode varchar(20) not null default 'EXCLUSIVE',
    add constraint chk_rooms_usage_mode check (usage_mode in ('EXCLUSIVE', 'SHARED'));

alter table class_sessions drop constraint ex_class_sessions_room_time;

create index idx_class_sessions_room_active_time
    on class_sessions (room_id, start_time, end_time)
    where status in ('SCHEDULED', 'IN_PROGRESS');

create or replace function validate_session_capacity()
returns trigger
language plpgsql
as $$
declare
    room_row rooms%rowtype;
    occupied_capacity bigint;
begin
    if new.room_id is null then
        return new;
    end if;

    select * into room_row from rooms where id = new.room_id for update;
    if new.capacity > room_row.capacity then
        raise exception 'Session capacity cannot exceed Room capacity';
    end if;

    if new.status in ('SCHEDULED', 'IN_PROGRESS') then
        select coalesce(sum(capacity), 0) into occupied_capacity
        from class_sessions
        where room_id = new.room_id
          and id <> new.id
          and status in ('SCHEDULED', 'IN_PROGRESS')
          and start_time < new.end_time and end_time > new.start_time;

        if room_row.usage_mode = 'EXCLUSIVE' and occupied_capacity > 0 then
            raise exception 'Exclusive Room already has an overlapping Session';
        end if;
        if room_row.usage_mode = 'SHARED' and occupied_capacity + new.capacity > room_row.capacity then
            raise exception 'Shared Room capacity would be exceeded';
        end if;
    end if;
    return new;
end;
$$;

drop trigger trg_class_sessions_capacity on class_sessions;
create trigger trg_class_sessions_capacity
before insert or update of room_id, capacity, start_time, end_time, status on class_sessions
for each row execute function validate_session_capacity();
