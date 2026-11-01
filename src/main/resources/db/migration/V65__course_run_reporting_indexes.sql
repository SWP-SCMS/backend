create index idx_course_runs_reporting_start
    on course_runs (planned_start_at desc, id desc);

create index idx_course_runs_reporting_status_start
    on course_runs (status, planned_start_at desc, id desc);
