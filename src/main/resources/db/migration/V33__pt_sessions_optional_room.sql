alter table class_sessions alter column room_id drop not null;

create or replace function validate_pt_session_shape()
returns trigger
language plpgsql
as $$
declare
    session_type varchar(20);
begin
    select class_type into session_type from sport_classes where id = new.sport_class_id;
    if session_type = 'PT_1_1' and new.capacity <> 1 then
        raise exception 'PT_1_1 Session capacity must equal one';
    end if;
    if session_type <> 'PT_1_1' and new.room_id is null then
        raise exception 'Non-PT Session requires a Room';
    end if;
    return new;
end;
$$;

create trigger trg_validate_pt_session_shape
before insert or update of sport_class_id, room_id, capacity on class_sessions
for each row execute function validate_pt_session_shape();

