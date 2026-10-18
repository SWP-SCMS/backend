alter table recurring_schedules
    add constraint chk_recurring_schedules_min_duration
    check (end_time - start_time >= interval '30 minutes') not valid;

alter table class_sessions
    add constraint chk_class_sessions_min_duration
    check (end_time - start_time >= interval '30 minutes') not valid;
