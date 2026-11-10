package com.scms.backend.identity.receptionmember;

import com.scms.backend.membership.MembershipStatus;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class ReceptionMemberProfileRepository {

	com.scms.backend.identity.receptionmember.ReceptionActiveMembershipResponse findActiveMembership(UUID memberAccountId, Timestamp nowTimestamp, Timestamp nowTimestamp2, Date today, Date today2) {
		return db.query("""
			with current_membership as (
			    select * from memberships
			    where member_account_id=? and status='ACTIVE' and starts_at <= ? and ends_at > ?
			    order by ends_at desc, id limit 1
			)
			select membership.*, plan.display_name as plan_name, benefit.benefit_code,
			       exists (select 1 from membership_freeze_periods period
			               where period.membership_id=membership.id and period.status in ('SCHEDULED', 'ACTIVE')
			                 and coalesce(period.actual_start_date, period.scheduled_start_date) <= ?
			                 and coalesce(period.actual_end_date, period.scheduled_end_date) > ?) as frozen
			from current_membership membership
			left join membership_plans plan on plan.plan_code=membership.plan_code_snapshot
			left join membership_plan_benefits benefit on benefit.plan_code=plan.plan_code
			""", rows -> {
			if (!rows.next())
				return null;
			UUID membershipId = rows.getObject("id", UUID.class);
			String offerName = rows.getString("offer_name_snapshot");
			String planName = rows.getString("plan_name");
			if (planName == null)
				planName = rows.getString("plan_code_snapshot");
			LocalDate start = rows.getTimestamp("starts_at").toInstant().atZone(VIETNAM).toLocalDate();
			LocalDate end = rows.getTimestamp("ends_at").toInstant().atZone(VIETNAM).toLocalDate();
			MembershipStatus status = MembershipStatus.valueOf(rows.getString("status"));
			boolean frozen = rows.getBoolean("frozen");
			Set<String> benefits = new TreeSet<>();
			do {
				String code = rows.getString("benefit_code");
				if (code != null)
					benefits.add(code);
			} while (rows.next());
			return new ReceptionActiveMembershipResponse(membershipId, offerName, planName, start, end, status, List.copyOf(benefits), frozen);
		}, memberAccountId, nowTimestamp, nowTimestamp2, today, today2);
	}

	private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

	private final JdbcTemplate db;

	ReceptionMemberProfileRepository(JdbcTemplate db) {
		this.db = db;
	}
}
