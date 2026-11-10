package com.scms.backend.training.assignment;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciles assignment needs, including memberships activated by the lifecycle job.
 */
@Service
public class CoachAssignmentReminder {

	private final JdbcTemplate db;

	private final Clock clock;

	CoachAssignmentReminder(JdbcTemplate db, Clock clock) {
		this.db = db;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${scms.assignment-reminder-delay-ms:30000}", initialDelayString = "${scms.assignment-reminder-initial-delay-ms:5000}")
	@Transactional
	public void notifyManagers() {
		var now = clock.instant();
		var today = now.atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
		// The membership and last ended assignment identify one assignment need.
		// A retry or another application instance cannot enqueue it twice.
		db.update("""
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
			""", Timestamp.from(now), Timestamp.from(now), today, today);
	}
}
