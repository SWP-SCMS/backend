alter table course_enrollments
    add constraint uq_course_enrollments_visit_owner unique (id, member_account_id);

alter table center_visits
    alter column membership_id drop not null,
    add column visit_source varchar(20) not null default 'MEMBERSHIP',
    add column course_enrollment_id uuid references course_enrollments(id),
    add column course_session_id uuid references course_sessions(id),
    add constraint fk_center_visits_course_enrollment_session
        foreign key (course_enrollment_id, course_session_id)
        references course_enrollment_sessions(course_enrollment_id, course_session_id),
    add constraint fk_center_visits_course_enrollment_owner
        foreign key (course_enrollment_id, member_account_id)
        references course_enrollments(id, member_account_id),
    add constraint chk_center_visits_source check (
        (visit_source = 'MEMBERSHIP' and membership_id is not null
            and course_enrollment_id is null and course_session_id is null)
        or
        (visit_source = 'COURSE' and membership_id is null
            and course_enrollment_id is not null and course_session_id is not null)
    );

create index idx_center_visits_course_session
    on center_visits (course_session_id, checked_in_at desc)
    where visit_source = 'COURSE';
