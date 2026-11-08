alter table training_results
    add column course_session_id uuid references course_sessions(id),
    drop constraint chk_training_results_reference,
    drop constraint chk_training_results_assignment_or_session;

alter table training_results
    add constraint chk_training_results_reference check (
        training_plan_version_id is not null or class_session_id is not null or course_session_id is not null
    ),
    add constraint chk_training_results_assignment_or_session check (
        coach_assignment_id is not null or class_session_id is not null or course_session_id is not null
    ),
    add constraint chk_training_results_one_session check (
        class_session_id is null or course_session_id is null
    );

create unique index uq_training_results_course_session_member
    on training_results(course_session_id, member_account_id)
    where course_session_id is not null and not legacy_duplicate;
