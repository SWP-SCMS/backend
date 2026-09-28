-- BR-COA-03: a Member may have at most one ACTIVE Personal Coach assignment.
drop index uq_coach_assignments_active_pair;

create unique index uq_coach_assignments_active_member
    on coach_assignments (member_account_id)
    where status = 'ACTIVE';

-- BR-VAL-COM-06: a Member may have at most one pending Membership Order.
create unique index uq_membership_orders_pending_member
    on membership_orders (member_account_id)
    where status = 'PENDING_PAYMENT';

-- Group/Yoga Result and Feedback ownership follows the Session Teaching Coach
-- and is independent from Personal Coach Assignment (BR-RES-04, BR-FDB-03).
alter table training_results
    alter column coach_assignment_id drop not null,
    add constraint chk_training_results_assignment_or_session
        check (coach_assignment_id is not null or class_session_id is not null);

alter table feedback
    alter column coach_assignment_id drop not null,
    add constraint chk_feedback_assignment_or_result
        check (coach_assignment_id is not null or training_result_id is not null);

create function validate_session_capacity()
returns trigger
language plpgsql
as $$
declare
    room_capacity integer;
begin
    select capacity into room_capacity
    from rooms
    where id = new.room_id;

    if new.capacity > room_capacity then
        raise exception 'Session capacity cannot exceed Room capacity';
    end if;
    return new;
end;
$$;

create trigger trg_class_sessions_capacity
before insert or update of room_id, capacity on class_sessions
for each row execute function validate_session_capacity();

-- Locking the Session row serializes competing bookings and prevents overbooking.
create function validate_booked_booking()
returns trigger
language plpgsql
as $$
declare
    session_row class_sessions%rowtype;
    member_status varchar(20);
    membership_row memberships%rowtype;
    booked_count bigint;
begin
    if new.status <> 'BOOKED' then
        return new;
    end if;

    select * into session_row
    from class_sessions
    where id = new.class_session_id
    for update;

    if session_row.status <> 'SCHEDULED' or session_row.start_time <= current_timestamp then
        raise exception 'Booking requires a future SCHEDULED Session';
    end if;

    select status into member_status
    from accounts
    where id = new.member_account_id and role = 'MEMBER';

    if member_status is distinct from 'ACTIVE' then
        raise exception 'Booking requires an ACTIVE Member Account';
    end if;

    select * into membership_row
    from memberships
    where id = new.membership_id and member_account_id = new.member_account_id;

    if membership_row.status is distinct from 'ACTIVE'
        or membership_row.plan_code_snapshot <> 'PLUS'
        or membership_row.starts_at > session_row.start_time
        or membership_row.ends_at <= session_row.start_time then
        raise exception 'Booking requires an ACTIVE PLUS Membership covering Session start';
    end if;

    select count(*) into booked_count
    from bookings
    where class_session_id = new.class_session_id
      and status = 'BOOKED'
      and id <> new.id;

    if booked_count >= session_row.capacity then
        raise exception 'Session capacity is full';
    end if;
    return new;
end;
$$;

create trigger trg_bookings_eligibility_and_capacity
before insert or update of class_session_id, member_account_id, membership_id, status on bookings
for each row execute function validate_booked_booking();

create function validate_payment_order_match()
returns trigger
language plpgsql
as $$
declare
    order_row membership_orders%rowtype;
begin
    select * into order_row from membership_orders where id = new.order_id;

    if new.method <> order_row.payment_method
        or new.amount <> order_row.price_amount_snapshot
        or new.currency_code <> order_row.currency_code_snapshot then
        raise exception 'Payment method, amount and currency must match Order';
    end if;

    if new.method = 'CASH' and new.status <> 'PAID' then
        raise exception 'Cash Payment must be persisted directly as PAID';
    end if;

    if new.method = 'BANK_TRANSFER' and btrim(coalesce(new.bank_transfer_content, '')) = '' then
        raise exception 'Bank Transfer Payment requires transfer content';
    end if;
    return new;
end;
$$;

create trigger trg_payments_match_order
before insert or update of order_id, method, status, amount, currency_code, bank_transfer_content on payments
for each row execute function validate_payment_order_match();

create function validate_membership_fulfillment()
returns trigger
language plpgsql
as $$
declare
    order_row membership_orders%rowtype;
begin
    select * into order_row from membership_orders where id = new.order_id;

    if order_row.status <> 'PAID'
        or order_row.member_account_id <> new.member_account_id
        or order_row.offer_id <> new.offer_id
        or order_row.plan_code_snapshot <> new.plan_code_snapshot then
        raise exception 'Membership must match a PAID Order';
    end if;

    if not exists (
        select 1 from payments
        where order_id = new.order_id and status = 'PAID'
    ) then
        raise exception 'Membership requires a PAID Payment';
    end if;
    return new;
end;
$$;

create trigger trg_memberships_paid_fulfillment
before insert on memberships
for each row execute function validate_membership_fulfillment();

create function validate_receipt_fulfillment()
returns trigger
language plpgsql
as $$
declare
    payment_row payments%rowtype;
begin
    select * into payment_row from payments where id = new.payment_id;

    if payment_row.status <> 'PAID'
        or payment_row.order_id <> new.order_id
        or payment_row.amount <> new.amount_snapshot
        or payment_row.currency_code <> new.currency_code_snapshot
        or payment_row.method <> new.payment_method_snapshot then
        raise exception 'Receipt must match a PAID Payment';
    end if;
    return new;
end;
$$;

create trigger trg_receipts_paid_fulfillment
before insert on receipts
for each row execute function validate_receipt_fulfillment();

-- BR-required historical records use lifecycle state rather than hard deletion.
create trigger trg_membership_offers_no_delete
before delete on membership_offers
for each row execute function prevent_history_mutation();

create trigger trg_membership_orders_no_delete
before delete on membership_orders
for each row execute function prevent_history_mutation();

create trigger trg_payments_no_delete
before delete on payments
for each row execute function prevent_history_mutation();

create trigger trg_memberships_no_delete
before delete on memberships
for each row execute function prevent_history_mutation();

create trigger trg_receipts_no_delete
before delete on receipts
for each row execute function prevent_history_mutation();

create trigger trg_bookings_no_delete
before delete on bookings
for each row execute function prevent_history_mutation();

create trigger trg_attendance_no_delete
before delete on attendance
for each row execute function prevent_history_mutation();

create trigger trg_center_visits_no_delete
before delete on center_visits
for each row execute function prevent_history_mutation();

create trigger trg_training_results_no_delete
before delete on training_results
for each row execute function prevent_history_mutation();

create trigger trg_feedback_no_delete
before delete on feedback
for each row execute function prevent_history_mutation();
