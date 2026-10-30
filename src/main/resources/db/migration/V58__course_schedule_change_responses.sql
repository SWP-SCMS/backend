create table course_schedule_change_responses (
    id uuid primary key,
    enrollment_session_id uuid not null references course_enrollment_sessions(id),
    old_start_at timestamptz not null,
    old_end_at timestamptz not null,
    new_start_at timestamptz not null,
    new_end_at timestamptz not null,
    notification_sent_at timestamptz not null,
    response_deadline timestamptz not null,
    response varchar(32),
    responded_at timestamptz,
    status varchar(16) not null default 'PENDING',
    created_at timestamptz not null default current_timestamp,
    constraint chk_course_schedule_change_response_choice check
        (response is null or response in ('CONTINUE', 'REQUEST_WITHDRAWAL', 'REQUEST_TRANSFER')),
    constraint chk_course_schedule_change_response_status check (status in ('PENDING', 'RESPONDED', 'SUPERSEDED')),
    constraint chk_course_schedule_change_response_state check
        ((status = 'PENDING' and response is null and responded_at is null) or
         (status = 'RESPONDED' and response is not null and responded_at is not null) or status = 'SUPERSEDED')
);

create index idx_course_schedule_change_response_member_pending
    on course_schedule_change_responses (enrollment_session_id, response_deadline)
    where status = 'PENDING';
