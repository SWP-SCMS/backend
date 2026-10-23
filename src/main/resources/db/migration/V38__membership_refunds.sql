create unique index uq_receipts_refund_request_link
    on receipts(id, payment_id, order_id, member_account_id);

create unique index uq_receipts_refund_link
    on receipts(id, payment_id, member_account_id);

create table refund_requests (
    id uuid primary key,
    payment_id uuid not null,
    order_id uuid not null,
    membership_id uuid not null,
    receipt_id uuid not null,
    member_account_id uuid not null references accounts(id),
    requester_account_id uuid not null references accounts(id),
    first_approver_account_id uuid references accounts(id),
    final_approver_account_id uuid references accounts(id),
    disbursed_by_account_id uuid references accounts(id),
    refund_type varchar(30) not null,
    status varchar(20) not null,
    requested_amount numeric(19,0) not null,
    suggested_amount numeric(19,0),
    approved_amount numeric(19,0),
    remaining_days_snapshot integer,
    calculation_date date,
    reason text not null,
    rejection_reason text,
    disbursement_evidence text,
    idempotency_key varchar(100) not null,
    requested_at timestamptz not null default current_timestamp,
    first_approved_at timestamptz,
    cash_disbursed_at timestamptz,
    completed_at timestamptz,
    rejected_at timestamptz,
    cancelled_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    version bigint not null default 0,
    constraint fk_refund_payment_order foreign key (payment_id, order_id) references payments(id, order_id),
    constraint fk_refund_order_member foreign key (order_id, member_account_id) references membership_orders(id, member_account_id),
    constraint fk_refund_membership_member foreign key (membership_id, member_account_id) references memberships(id, member_account_id),
    constraint fk_refund_receipt_context foreign key (receipt_id, payment_id, order_id, member_account_id)
        references receipts(id, payment_id, order_id, member_account_id),
    constraint chk_refund_type check (refund_type in ('TERMINATED_MEMBERSHIP','RESERVED_CANCELLATION')),
    constraint chk_refund_status check (status in ('REQUESTED','FIRST_APPROVED','CASH_DISBURSED','COMPLETED','REJECTED','CANCELLED')),
    constraint chk_refund_amounts check (
        requested_amount > 0 and (suggested_amount is null or suggested_amount >= 0)
        and (approved_amount is null or (approved_amount > 0 and approved_amount <= requested_amount
            and suggested_amount is not null and approved_amount <= suggested_amount))
    ),
    constraint chk_refund_reason check (btrim(reason) <> ''),
    constraint chk_refund_evidence check (disbursement_evidence is null or btrim(disbursement_evidence) <> ''),
    constraint chk_refund_rejection_reason check (rejection_reason is null or btrim(rejection_reason) <> '')
);

create unique index uq_refund_requests_idempotency
    on refund_requests(requester_account_id, idempotency_key);

create unique index uq_refund_requests_nonrejected_payment
    on refund_requests(payment_id)
    where status not in ('REJECTED','CANCELLED');

create index idx_refund_requests_status_requested
    on refund_requests(status, requested_at desc, id desc);

create index idx_refund_requests_member_requested
    on refund_requests(member_account_id, requested_at desc, id desc);

create table credit_notes (
    id uuid primary key,
    refund_request_id uuid not null unique references refund_requests(id),
    receipt_id uuid not null,
    payment_id uuid not null,
    member_account_id uuid not null references accounts(id),
    amount numeric(19,0) not null,
    currency_code char(3) not null default 'VND',
    issued_by_account_id uuid not null references accounts(id),
    issued_at timestamptz not null default current_timestamp,
    constraint fk_credit_note_receipt_context foreign key (receipt_id, payment_id, member_account_id)
        references receipts(id, payment_id, member_account_id),
    constraint chk_credit_note_amount check (amount > 0),
    constraint chk_credit_note_currency check (currency_code = 'VND')
);

create function reject_credit_note_mutation() returns trigger
language plpgsql as $$
begin
    raise exception 'Credit notes are immutable';
end;
$$;

create trigger trg_credit_notes_immutable
before update or delete on credit_notes
for each row execute function reject_credit_note_mutation();
