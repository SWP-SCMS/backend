alter table courses drop constraint chk_courses_status;

alter table courses
    add column submitted_by_account_id uuid references accounts(id),
    add column submitted_at timestamptz,
    add column reviewed_by_account_id uuid references accounts(id),
    add column reviewed_at timestamptz,
    add column review_reason varchar(1000),
    add constraint chk_courses_status check (status in (
        'DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'PUBLISHED', 'INACTIVE', 'ARCHIVED'
    )),
    add constraint chk_courses_approval_metadata check (
        (status = 'PENDING_APPROVAL'
            and submitted_by_account_id is not null and submitted_at is not null
            and reviewed_by_account_id is null and reviewed_at is null and review_reason is null)
        or (status = 'REJECTED'
            and submitted_by_account_id is not null and submitted_at is not null
            and reviewed_by_account_id is not null and reviewed_at is not null
            and review_reason is not null and btrim(review_reason) <> '')
        or (status in ('DRAFT', 'PUBLISHED', 'INACTIVE', 'ARCHIVED') and (
            (submitted_by_account_id is null and submitted_at is null
                and reviewed_by_account_id is null and reviewed_at is null and review_reason is null)
            or (submitted_by_account_id is not null and submitted_at is not null
                and reviewed_by_account_id is not null and reviewed_at is not null
                and review_reason is not null and btrim(review_reason) <> '')
        ))
    );

create index idx_courses_pending_approval
    on courses (submitted_at, id)
    where status = 'PENDING_APPROVAL';
