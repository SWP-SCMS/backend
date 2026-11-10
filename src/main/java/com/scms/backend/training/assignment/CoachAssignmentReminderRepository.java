package com.scms.backend.training.assignment;

import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;

final class CoachAssignmentReminderRepository {

	int notifyManagersInsertNotificationOutbox(java.sql.Timestamp nowTimestamp, java.sql.Timestamp nowTimestamp2, java.time.LocalDate today, java.time.LocalDate today2) {
		return db.update("""
			with eligible as (
			    select m.id membership_id, a.id member_id, a.full_name, p.member_code,
			        coalesce((select ca.id::text from coach_assignments ca
			            where ca.member_account_id=a.id and ca.status='ENDED'
			            order by ca.ends_at desc, ca.id desc limit 1), 'initial') episode
			    from memberships m
			    join accounts a on a.id=m.member_account_id
			    join member_profiles p on p.account_id=a.id
			    where a.role='MEMBER' and a.status='ACTIVE' and m.status='ACTIVE'
			      and m.starts_at<=? and m.ends_at>?
			      and exists (select 1 from membership_plan_benefits b
			          where b.plan_code=m.plan_code_snapshot and b.benefit_code='PERSONAL_COACHING')
			      and not exists (select 1 from coach_assignments ca
			          where ca.member_account_id=a.id and ca.status='ACTIVE')
			      and not exists (select 1 from membership_freeze_periods f
			          where f.membership_id=m.id and f.status in ('SCHEDULED','ACTIVE')
			            and coalesce(f.actual_start_date,f.scheduled_start_date)<=cast(? as date)
			            and coalesce(f.actual_end_date,f.scheduled_end_date)>cast(? as date))
			)
			insert into notification_outbox
			    (id,event_key,recipient_account_id,notification_type,target_type,target_id,payload)
			select gen_random_uuid(),
			    'PT_ASSIGNMENT_REQUIRED:'||e.membership_id||':'||e.episode||':'||manager.id,
			    manager.id,'PT_ASSIGNMENT_REQUIRED','MEMBER',e.member_id,
			    jsonb_build_object('memberAccountId',e.member_id,'memberFullName',e.full_name,
			        'memberCode',e.member_code,'membershipId',e.membership_id)
			from eligible e cross join accounts manager
			where manager.role='MANAGER' and manager.status='ACTIVE'
			on conflict(event_key) do nothing
			""", nowTimestamp, nowTimestamp2, today, today2);
	}

	private final JdbcTemplate db;

	CoachAssignmentReminderRepository(JdbcTemplate db) {
		this.db = db;
	}
}
