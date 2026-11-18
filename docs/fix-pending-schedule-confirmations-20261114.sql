-- Repair only the six pending confirmations incorrectly superseded by the
-- resource-only update 1200ebe7-82a0-42f1-8f54-3a5c63e64485 in the reported run.
-- Keep the original schedule, deadline, response, and refund data unchanged.
-- Abort atomically if the run or any candidate has changed since diagnosis.
begin;
do $repair$
declare restored_count integer;
begin
  perform id from course_runs where id='e36b79df-a800-48a7-8a9e-a26195966074' for update;
  if not exists (
    select 1 from course_schedule_change_events ev
    where ev.id='1200ebe7-82a0-42f1-8f54-3a5c63e64485'
      and ev.course_run_id='e36b79df-a800-48a7-8a9e-a26195966074'
      and ev.old_planned_start_at=ev.new_planned_start_at and ev.old_planned_end_at=ev.new_planned_end_at
      and ev.old_total_units=ev.new_total_units
      and not exists (select 1 from course_schedule_change_events newer where newer.course_run_id=ev.course_run_id and newer.created_at>ev.created_at)
      and not exists (select 1 from course_schedule_change_responses r where r.change_event_id=ev.id)
      and exists (select 1 from notification_outbox n where n.payload->>'changeEventId'=ev.id::text)
      and not exists (select 1 from notification_outbox n cross join lateral jsonb_array_elements(n.payload->'changes') c
        where n.payload->>'changeEventId'=ev.id::text and
          (c->>'oldStartAt' is distinct from c->>'newStartAt' or c->>'oldEndAt' is distinct from c->>'newEndAt' or c->>'recordId' is not null))
  ) then raise exception 'Schedule changed since diagnosis; repair aborted'; end if;

  with restored as (
    update course_schedule_change_responses r set status='PENDING'
    from course_enrollment_sessions es join course_sessions s on s.id=es.course_session_id
    where r.enrollment_session_id=es.id and s.course_run_id='e36b79df-a800-48a7-8a9e-a26195966074'
      and r.id in ('4ef7dbad-6969-4dcf-8b3a-a74dcb59310e','5ba24334-3687-4b34-9e28-f24f4e234b5e','bcf020f0-5c37-4e05-8131-f8ebe5ec17af',
        'a7442706-e401-439b-96aa-ca9b7c0bb75d','dd60448a-6faf-4342-90e3-fc0329a3b02b','ead60c61-8328-4c7f-80d5-b71f6403e1b3')
      and r.status='SUPERSEDED' and r.response is null and r.responded_at is null and r.response_deadline>current_timestamp
      and r.new_start_at=s.start_at and r.new_end_at=s.end_at
      and not exists (select 1 from course_schedule_change_responses newer where newer.enrollment_session_id=r.enrollment_session_id and newer.created_at>r.created_at)
    returning r.id
  )
  insert into audit_events(id,action,target_type,target_id,reason,before_data,after_data)
  select gen_random_uuid(),'COURSE_SCHEDULE_CONFIRMATION_RESTORED','COURSE_SCHEDULE_CHANGE_RESPONSE',id,
    'Repair pending confirmation incorrectly superseded by resource-only update 1200ebe7-82a0-42f1-8f54-3a5c63e64485',
    '{"status":"SUPERSEDED"}'::jsonb,'{"status":"PENDING"}'::jsonb from restored;
  get diagnostics restored_count = row_count;
  if restored_count<>6 then raise exception 'Expected six confirmations, found %; repair aborted',restored_count; end if;
end;
$repair$;
commit;
