alter table course_schedule_change_events drop constraint chk_course_schedule_change_event_cause;
alter table course_schedule_change_events add constraint chk_course_schedule_change_event_cause check
  (cause_code in ('VOLUNTARY','START_DELAY_OVER_7_DAYS','MEMBER_SCHEDULE_CONFLICT','REDUCED_UNIT_COUNT','CENTER_SESSION_RESCHEDULE'));

alter table course_schedule_change_responses drop constraint chk_course_schedule_change_response_cause;
alter table course_schedule_change_responses add constraint chk_course_schedule_change_response_cause check
  (cause_code in ('VOLUNTARY','START_DELAY_OVER_7_DAYS','MEMBER_SCHEDULE_CONFLICT','REDUCED_UNIT_COUNT','CENTER_SESSION_RESCHEDULE'));

alter table course_withdrawal_requests drop constraint chk_course_withdrawal_cause;
alter table course_withdrawal_requests add constraint chk_course_withdrawal_cause check
  (cause_code in ('VOLUNTARY','START_DELAY_OVER_7_DAYS','MEMBER_SCHEDULE_CONFLICT','REDUCED_UNIT_COUNT','CENTER_SESSION_RESCHEDULE'));

-- Existing withdrawal requests retain their captured cause and amount at every status.
-- Only unanswered notices with no withdrawal yet are classified for a future new request.
update course_schedule_change_responses r set cause_code='CENTER_SESSION_RESCHEDULE'
from course_enrollment_sessions es join course_sessions s on s.id=es.course_session_id
where r.enrollment_session_id=es.id and r.status='PENDING' and r.response is null
  and r.cause_code='VOLUNTARY' and not s.is_makeup
  and (abs(extract(epoch from (r.new_start_at-r.old_start_at)))>1800
    or abs(extract(epoch from (r.new_end_at-r.old_end_at)))>1800
    or (r.new_start_at at time zone 'Asia/Ho_Chi_Minh')::date<>(r.old_start_at at time zone 'Asia/Ho_Chi_Minh')::date)
  and not exists (select 1 from course_withdrawal_requests w where w.schedule_change_response_id=r.id
    or (w.change_event_id=r.change_event_id and w.course_enrollment_id=es.course_enrollment_id));
