package com.scms.backend.manager;

import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class StaffStatusRepository {

	int endAssignmentsWithoutActivePlus(UUID coachId) {
		return jdbc.update("""
			update coach_assignments ca set status='ENDED', end_reason='PLUS membership expired',
				ends_at=greatest(ca.starts_at, coalesce((select max(m.ends_at) from memberships m
				where m.member_account_id=ca.member_account_id and m.plan_code_snapshot='PLUS'), current_timestamp)),
				updated_at=current_timestamp, version=ca.version+1
			where ca.coach_account_id=? and ca.status='ACTIVE' and not exists (
				select 1 from memberships m where m.member_account_id=ca.member_account_id
				and m.status='ACTIVE' and m.plan_code_snapshot='PLUS' and m.starts_at <= current_timestamp and m.ends_at > current_timestamp)
			""", coachId);
	}

	java.lang.Boolean hasFutureSessions(UUID coachId, Timestamp value3) {
		return jdbc.queryForObject("""
			select exists(select 1 from class_sessions where teaching_coach_account_id = ?
				and end_time > ? and status in ('SCHEDULED', 'IN_PROGRESS'))
			""", Boolean.class, coachId, value3);
	}

	java.lang.Boolean hasActiveAssignments(UUID coachId) {
		return jdbc.queryForObject("""
			select exists(select 1 from coach_assignments where coach_account_id = ? and status = 'ACTIVE')
			""", Boolean.class, coachId);
	}

	private final JdbcTemplate jdbc;

	StaffStatusRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}
}
