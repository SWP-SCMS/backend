alter table coach_leave_requests drop constraint chk_coach_leave_review;
alter table coach_leave_requests drop constraint chk_coach_leave_status;

alter table coach_leave_requests
    add constraint chk_coach_leave_status
    check (status in ('PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN', 'EXPIRED', 'CANCELLED'));

alter table coach_leave_requests
    add constraint chk_coach_leave_review check (
        (status in ('PENDING', 'WITHDRAWN', 'EXPIRED') and reviewed_by_account_id is null and reviewed_at is null)
        or (status in ('APPROVED', 'REJECTED', 'CANCELLED') and reviewed_by_account_id is not null and reviewed_at is not null)
    );
