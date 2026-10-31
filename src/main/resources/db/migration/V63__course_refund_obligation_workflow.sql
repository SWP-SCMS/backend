alter table course_refund_obligations
    add column member_account_id uuid,
    add column course_order_id uuid,
    add column course_payment_id uuid,
    add column currency_code char(3) not null default 'VND',
    add column approved_amount_vnd numeric(19, 0),
    add column disbursed_amount_vnd numeric(19, 0) not null default 0,
    add column verified_amount_vnd numeric(19, 0) not null default 0,
    add column requester_account_id uuid,
    add column request_reason varchar(1000),
    add column approved_by_account_id uuid,
    add column approval_reason varchar(1000),
    add column approved_at timestamptz,
    add column rejected_by_account_id uuid,
    add column rejection_reason varchar(1000),
    add column rejected_at timestamptz,
    add column disbursed_by_account_id uuid,
    add column disbursed_at timestamptz,
    add column disbursement_method varchar(32),
    add column disbursement_reference varchar(200),
    add column evidence_reference varchar(500),
    add column verified_by_account_id uuid,
    add column verification_reason varchar(1000),
    add column verified_at timestamptz,
    add column terminal_by_account_id uuid,
    add column terminal_reason varchar(1000),
    add column terminal_at timestamptz,
    add column version bigint not null default 0,
    add column updated_at timestamptz not null default current_timestamp;

update course_refund_obligations obligation
set member_account_id=enrollment.member_account_id,
    course_order_id=enrollment.course_order_id,
    course_payment_id=enrollment.course_payment_id,
    request_reason=case obligation.source_type
        when 'MEMBER_WITHDRAWAL' then 'Member withdrawal refund'
        when 'COURSE_RUN_CANCELLED' then 'Course Run cancellation refund'
        when 'UNRESOLVED_SESSIONS' then 'Unresolved Course sessions refund'
        when 'TRANSFER_CREDIT_EXCESS' then 'Unused Course transfer credit refund'
        else 'Course refund obligation'
    end
from course_enrollments enrollment
where enrollment.id=obligation.course_enrollment_id;

update course_refund_obligations obligation
set requester_account_id=request.requester_account_id,
    request_reason=request.request_reason
from course_withdrawal_requests request
where obligation.source_type='MEMBER_WITHDRAWAL'
  and request.course_enrollment_id=obligation.course_enrollment_id
  and request.status='APPROVED';

update course_refund_obligations obligation
set requester_account_id=request.requester_account_id,
    request_reason=coalesce(request.reason, 'Unused Course transfer credit refund')
from course_transfer_requests request
where obligation.source_type='TRANSFER_CREDIT_EXCESS'
  and request.source_enrollment_id=obligation.course_enrollment_id
  and request.status='COMPLETED';

alter table course_refund_obligations
    alter column member_account_id set not null,
    alter column request_reason set not null,
    add constraint fk_course_refund_obligations_member
        foreign key(member_account_id) references accounts(id),
    add constraint fk_course_refund_obligations_order
        foreign key(course_order_id) references course_orders(id),
    add constraint fk_course_refund_obligations_payment
        foreign key(course_payment_id) references course_payments(id),
    add constraint fk_course_refund_obligations_requester
        foreign key(requester_account_id) references accounts(id),
    add constraint fk_course_refund_obligations_approver
        foreign key(approved_by_account_id) references accounts(id),
    add constraint fk_course_refund_obligations_rejector
        foreign key(rejected_by_account_id) references accounts(id),
    add constraint fk_course_refund_obligations_disburser
        foreign key(disbursed_by_account_id) references accounts(id),
    add constraint fk_course_refund_obligations_verifier
        foreign key(verified_by_account_id) references accounts(id),
    add constraint fk_course_refund_obligations_terminal_actor
        foreign key(terminal_by_account_id) references accounts(id);

alter table course_refund_obligations drop constraint chk_course_refund_obligations_source;
alter table course_refund_obligations add constraint chk_course_refund_obligations_source
    check(source_type in ('MEMBER_WITHDRAWAL', 'COURSE_RUN_CANCELLED', 'MATERIAL_CHANGE',
        'UNRESOLVED_SESSIONS', 'TRANSFER_CREDIT_EXCESS', 'PAYMENT_EXCEPTION'));

alter table course_refund_obligations add constraint chk_course_refund_obligations_amounts check(
    amount_vnd >= 0
    and (approved_amount_vnd is null or (approved_amount_vnd >= 0 and approved_amount_vnd <= amount_vnd))
    and disbursed_amount_vnd >= 0
    and verified_amount_vnd >= 0
    and disbursed_amount_vnd <= coalesce(approved_amount_vnd, 0)
    and verified_amount_vnd <= disbursed_amount_vnd
    and currency_code='VND'
);

alter table course_refund_obligations add constraint chk_course_refund_obligations_review_state check(
    (status='PENDING_REVIEW' and approved_amount_vnd is null and approved_by_account_id is null
        and approval_reason is null and approved_at is null and rejected_by_account_id is null
        and rejection_reason is null and rejected_at is null)
    or (status='REJECTED' and approved_amount_vnd is null and approved_by_account_id is null
        and approval_reason is null and approved_at is null and rejected_by_account_id is not null
        and rejection_reason is not null and rejected_at is not null)
    or (status in ('APPROVED', 'DISBURSEMENT_RECORDED', 'PENDING_VERIFICATION', 'COMPLETED', 'FAILED', 'RETURNED')
        and approved_amount_vnd is not null and approved_by_account_id is not null
        and approval_reason is not null and approved_at is not null and rejected_by_account_id is null
        and rejection_reason is null and rejected_at is null)
);

create or replace function populate_course_refund_obligation_snapshot()
returns trigger language plpgsql as $$
begin
    select enrollment.member_account_id, enrollment.course_order_id, enrollment.course_payment_id
    into new.member_account_id, new.course_order_id, new.course_payment_id
    from course_enrollments enrollment where enrollment.id=new.course_enrollment_id;
    if new.request_reason is null or btrim(new.request_reason)='' then
        new.request_reason=case new.source_type
            when 'MEMBER_WITHDRAWAL' then 'Member withdrawal refund'
            when 'COURSE_RUN_CANCELLED' then 'Course Run cancellation refund'
            when 'MATERIAL_CHANGE' then 'Material Course change refund'
            when 'UNRESOLVED_SESSIONS' then 'Unresolved Course sessions refund'
            when 'TRANSFER_CREDIT_EXCESS' then 'Unused Course transfer credit refund'
            when 'PAYMENT_EXCEPTION' then 'Course payment exception refund'
            else 'Course refund obligation'
        end;
    end if;
    return new;
end $$;

create trigger trg_course_refund_obligation_snapshot
before insert on course_refund_obligations
for each row execute function populate_course_refund_obligation_snapshot();

create or replace function enforce_course_refund_obligation_transition()
returns trigger language plpgsql as $$
begin
    if new.status=old.status then return new; end if;
    if (old.status='PENDING_REVIEW' and new.status in ('APPROVED', 'REJECTED'))
        or (old.status='APPROVED' and new.status in ('DISBURSEMENT_RECORDED', 'FAILED'))
        or (old.status='DISBURSEMENT_RECORDED' and new.status in ('PENDING_VERIFICATION', 'FAILED', 'RETURNED'))
        or (old.status='PENDING_VERIFICATION' and new.status in ('COMPLETED', 'FAILED', 'RETURNED')) then
        return new;
    end if;
    raise exception 'invalid Course refund transition: % -> %', old.status, new.status
        using errcode='23514';
end $$;

create trigger trg_course_refund_obligation_transition
before update of status on course_refund_obligations
for each row execute function enforce_course_refund_obligation_transition();

create index idx_course_refund_obligations_member_history
    on course_refund_obligations(member_account_id, created_at desc, id desc);
create index idx_course_refund_obligations_enrollment_accounting
    on course_refund_obligations(course_enrollment_id, status, id);
