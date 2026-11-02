alter table course_withdrawal_requests
    drop constraint chk_course_withdrawal_status,
    drop constraint chk_course_withdrawal_decision,
    add column snapshot_captured_at timestamptz,
    add column withdrawal_eligible_snapshot boolean,
    add column withdrawal_reason_snapshot varchar(64),
    add column cancelled_at timestamptz,
    add column cancelled_by_account_id uuid references accounts(id),
    add column cancellation_reason varchar(1000);

-- Historical request-time values cannot be reconstructed from today's schedule.
with closed as (
    update course_withdrawal_requests set status='CANCELLED',cancelled_at=current_timestamp,
        cancellation_reason='LEGACY_SNAPSHOT_MISSING'
    where status='PENDING' and (snapshot_captured_at is null or total_units_snapshot is null
        or remaining_units_snapshot is null or refund_basis_vnd is null or refund_amount_vnd is null
        or withdrawal_eligible_snapshot is null or withdrawal_reason_snapshot is null)
    returning id,course_enrollment_id,member_account_id
), restored as (
    update course_enrollments set lifecycle_status='ACTIVE'
    where lifecycle_status='WITHDRAWAL_PENDING' and id in (select course_enrollment_id from closed)
), audited as (
    insert into audit_events(id,actor_account_id,action,target_type,target_id,reason,before_data,after_data)
    select gen_random_uuid(),null,'COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_REQUEST',id,
        'LEGACY_SNAPSHOT_MISSING','{"status":"PENDING"}'::jsonb,'{"status":"CANCELLED"}'::jsonb from closed
)
insert into notification_outbox(id,event_key,recipient_account_id,notification_type,target_type,target_id,payload)
select gen_random_uuid(),'course-withdrawal-cancelled:' || id,member_account_id,
    'COURSE_WITHDRAWAL_CANCELLED','COURSE_WITHDRAWAL_REQUEST',id,
    '{"reason":"LEGACY_SNAPSHOT_MISSING","resubmissionRequired":true}'::jsonb from closed
on conflict(event_key) do nothing;

alter table course_withdrawal_requests
    add constraint chk_course_withdrawal_status check(status in ('PENDING','APPROVED','REJECTED','CANCELLED')),
    add constraint chk_course_withdrawal_decision check(
        (status in ('PENDING','CANCELLED') and decision_reason is null and decided_by_account_id is null and decided_at is null)
        or (status in ('APPROVED','REJECTED') and decision_reason is not null and decided_by_account_id is not null and decided_at is not null)),
    add constraint chk_course_withdrawal_cancellation check(
        (status='CANCELLED' and cancelled_at is not null and cancellation_reason is not null)
        or (status<>'CANCELLED' and cancelled_at is null and cancelled_by_account_id is null and cancellation_reason is null)),
    add constraint chk_course_withdrawal_snapshot check(
        (snapshot_captured_at is null and status<>'PENDING')
        or (snapshot_captured_at is not null and snapshot_captured_at=requested_at
            and total_units_snapshot is not null and total_units_snapshot>0
            and remaining_units_snapshot is not null and remaining_units_snapshot>0 and remaining_units_snapshot<=total_units_snapshot
            and refund_basis_vnd is not null and refund_basis_vnd>=0
            and refund_amount_vnd is not null and refund_amount_vnd>=0 and refund_amount_vnd<=refund_basis_vnd
            and withdrawal_eligible_snapshot is true and withdrawal_reason_snapshot is not null));

create function enforce_course_withdrawal_snapshot() returns trigger language plpgsql as $$
begin
    if TG_OP='INSERT' and NEW.snapshot_captured_at is null then
        raise exception 'New withdrawals require a request-time snapshot' using errcode='23514';
    end if;
    if TG_OP='UPDATE' and OLD.snapshot_captured_at is not null and
        row(NEW.snapshot_captured_at,NEW.requested_at,NEW.cause_code,NEW.total_units_snapshot,NEW.remaining_units_snapshot,
            NEW.refund_basis_vnd,NEW.refund_amount_vnd,NEW.withdrawal_eligible_snapshot,NEW.withdrawal_reason_snapshot)
        is distinct from
        row(OLD.snapshot_captured_at,OLD.requested_at,OLD.cause_code,OLD.total_units_snapshot,OLD.remaining_units_snapshot,
            OLD.refund_basis_vnd,OLD.refund_amount_vnd,OLD.withdrawal_eligible_snapshot,OLD.withdrawal_reason_snapshot) then
        raise exception 'Withdrawal snapshots are immutable' using errcode='23514';
    end if;
    return NEW;
end;
$$;
create trigger course_withdrawal_snapshot before insert or update on course_withdrawal_requests
    for each row execute function enforce_course_withdrawal_snapshot();
