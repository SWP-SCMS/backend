-- REVIEW DRAFT ONLY: not a Flyway migration and not executed.
-- Requires separate approval because it changes existing payment/order/hold states.
-- PostgreSQL integration validation is pending (Docker is unavailable).
-- If approved, execute transactionally after reviewing affected open transfers.

-- Retire open transfers without rewriting completed enrollments or financial history.
create temporary table retired_course_transfer_responses on commit drop as
select response.id
from course_schedule_change_responses response
join course_transfer_requests request on request.schedule_change_response_id=response.id
where request.status in ('PENDING','OFFERED');

update course_payments payment set status='FAILED',updated_at=current_timestamp
where payment.status='PENDING' and payment.course_order_id in (
    select difference_order_id from course_transfer_offers where status in ('OFFERED','PENDING_PAYMENT')
);

update course_reservations reservation set status='EXPIRED',updated_at=current_timestamp
where reservation.status='HELD' and reservation.course_order_id in (
    select difference_order_id from course_transfer_offers where status in ('OFFERED','PENDING_PAYMENT')
);

update course_orders orders set status='EXPIRED',updated_at=current_timestamp
where orders.status='PENDING_PAYMENT' and orders.id in (
    select difference_order_id from course_transfer_offers where status in ('OFFERED','PENDING_PAYMENT')
);

update course_transfer_holds set status='RELEASED',released_at=current_timestamp
where status='HELD';

update course_transfer_offers set status='CANCELLED'
where status in ('OFFERED','PENDING_PAYMENT');

update course_transfer_requests set status='EXPIRED',reason='COURSE_TRANSFERS_RETIRED'
where status in ('PENDING','OFFERED');

-- Keep the original response as history. Reopen only decisions still within their
-- original deadline, with no newer response, so members can continue or withdraw.
insert into course_schedule_change_responses(
    id,enrollment_session_id,old_start_at,old_end_at,new_start_at,new_end_at,
    notification_sent_at,response_deadline,cause_code,change_event_id
)
select gen_random_uuid(),response.enrollment_session_id,response.old_start_at,response.old_end_at,
    response.new_start_at,response.new_end_at,current_timestamp,response.response_deadline,
    response.cause_code,response.change_event_id
from course_schedule_change_responses response
join retired_course_transfer_responses retired on retired.id=response.id
join course_enrollment_sessions unit on unit.id=response.enrollment_session_id
join course_enrollments enrollment on enrollment.id=unit.course_enrollment_id
join course_runs run on run.id=enrollment.course_run_id
where response.status='RESPONDED' and response.response='REQUEST_TRANSFER'
    and response.response_deadline>current_timestamp
    and enrollment.lifecycle_status='ACTIVE'
    and run.status in ('OPEN_FOR_REGISTRATION','CONFIRMED','IN_PROGRESS')
    and not exists (
        select 1 from course_schedule_change_responses newer
        where newer.enrollment_session_id=response.enrollment_session_id and newer.id<>response.id
            and newer.status in ('PENDING','RESPONDED') and newer.created_at>response.created_at
    );

update course_schedule_change_responses response set status='SUPERSEDED'
where response.id in (select id from retired_course_transfer_responses)
    and response.response='REQUEST_TRANSFER';
