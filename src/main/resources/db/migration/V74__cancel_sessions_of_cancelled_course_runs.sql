alter table course_sessions
    drop constraint chk_course_sessions_status,
    add constraint chk_course_sessions_status check (status in (
        'SCHEDULED', 'CANCELLED', 'CANCELLED_PENDING_MAKEUP', 'MAKEUP_SCHEDULED',
        'COMPLETED', 'UNRESOLVED', 'RESOLVED_BY_REFUND', 'REMOVED_BY_RUN_CHANGE'
    ));

-- Repair older cancelled runs without changing completed learning or refund history.
update course_sessions session
set status = 'CANCELLED', version = session.version + 1, updated_at = current_timestamp
from course_runs run
where session.course_run_id = run.id and run.status = 'CANCELLED'
  and session.status in ('SCHEDULED', 'MAKEUP_SCHEDULED', 'CANCELLED_PENDING_MAKEUP');
