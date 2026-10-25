alter table recurring_schedules
    drop constraint chk_recurring_schedules_session_count;

alter table recurring_schedules
    add constraint chk_recurring_schedules_session_count
    check (session_count between 1 and 100);
