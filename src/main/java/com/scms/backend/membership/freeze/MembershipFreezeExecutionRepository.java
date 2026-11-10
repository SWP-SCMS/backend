package com.scms.backend.membership.freeze;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class MembershipFreezeExecutionRepository {

	int markScheduledObsolete(java.util.UUID requestId) {
		return db.update("update membership_freeze_periods set status='OBSOLETE', updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", requestId);
	}

	int completeScheduledPeriod(java.sql.Date value1, java.sql.Date value2, int creditedDays, java.util.UUID requestId) {
		return db.update("update membership_freeze_periods set status='COMPLETED', actual_start_date=?, actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", value1, value2, creditedDays, requestId);
	}

	int activateScheduledPeriod(java.sql.Date value1, java.util.UUID requestId) {
		return db.update("update membership_freeze_periods set status='ACTIVE', actual_start_date=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", value1, requestId);
	}

	java.time.LocalDate completeFindMembershipFreezePeriods(java.util.UUID requestId) {
		return db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update", LocalDate.class, requestId);
	}

	int completeUpdateMembershipFreezePeriods(java.sql.Date value1, int creditedDays, java.util.UUID requestId) {
		return db.update("update membership_freeze_periods set status='COMPLETED', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'", value1, creditedDays, requestId);
	}

	java.time.LocalDate endEarlyForTerminationFindMembershipFreezePeriods(java.util.UUID requestId) {
		return db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update", LocalDate.class, requestId);
	}

	int endEarlyForTerminationUpdateMembershipFreezePeriods(java.sql.Date value1, int creditedDays, java.util.UUID requestId) {
		return db.update("update membership_freeze_periods set status='ENDED_EARLY', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'", value1, creditedDays, requestId);
	}

	private final JdbcTemplate db;

	MembershipFreezeExecutionRepository(JdbcTemplate db) {
		this.db = db;
	}
}
