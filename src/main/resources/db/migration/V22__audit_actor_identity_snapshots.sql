alter table audit_events
    add column actor_name_snapshot varchar(200),
    add column actor_role_snapshot varchar(20);

alter table audit_events disable trigger trg_audit_events_append_only;

update audit_events event
set actor_name_snapshot = account.full_name,
    actor_role_snapshot = account.role
from accounts account
where account.id = event.actor_account_id;

alter table audit_events enable trigger trg_audit_events_append_only;

create function snapshot_audit_actor_identity()
returns trigger
language plpgsql
as $$
begin
    new.actor_name_snapshot := null;
    new.actor_role_snapshot := null;
    if new.actor_account_id is not null then
        select full_name, role
        into new.actor_name_snapshot, new.actor_role_snapshot
        from accounts
        where id = new.actor_account_id;
    end if;
    return new;
end;
$$;

create trigger trg_audit_events_actor_identity
before insert on audit_events
for each row execute function snapshot_audit_actor_identity();
