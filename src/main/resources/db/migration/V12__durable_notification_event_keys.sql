alter table notifications add column event_key varchar(200);

update notifications set event_key = 'legacy:' || id;

alter table notifications alter column event_key set not null;

alter table notifications
    add constraint uq_notifications_event_key unique (event_key);
