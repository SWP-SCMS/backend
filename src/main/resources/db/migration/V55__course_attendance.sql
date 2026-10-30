alter table course_enrollment_sessions
    add column attendance_status varchar(20) not null default 'NOT_RECORDED',
    add column attendance_recorded_at timestamptz,
    add column attendance_recorded_by uuid references accounts(id),
    add column attendance_updated_at timestamptz,
    add column version bigint not null default 0,
    add constraint chk_course_enrollment_sessions_attendance_status
        check (attendance_status in ('NOT_RECORDED', 'PRESENT', 'ABSENT')),
    add constraint chk_course_enrollment_sessions_attendance_recording check (
        (attendance_status = 'NOT_RECORDED' and attendance_recorded_at is null
            and attendance_recorded_by is null and attendance_updated_at is null)
        or (attendance_status in ('PRESENT', 'ABSENT') and attendance_recorded_at is not null
            and attendance_recorded_by is not null and attendance_updated_at is not null)
    );

create index idx_course_enrollment_sessions_roster
    on course_enrollment_sessions (course_session_id, disposition, course_enrollment_id);
