package com.scms.backend.membership.freeze;

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class MembershipFreezeExecutionRepository {

	int markScheduledObsolete(UUID requestId) {
		return db.update("update membership_freeze_periods set status='OBSOLETE', updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", requestId);
	}

	int completeScheduledPeriod(Date value1, Date value2, int creditedDays, UUID requestId) {
		return db.update("update membership_freeze_periods set status='COMPLETED', actual_start_date=?, actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", value1, value2, creditedDays, requestId);
	}

	int activateScheduledPeriod(Date value1, UUID requestId) {
		return db.update("update membership_freeze_periods set status='ACTIVE', actual_start_date=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='SCHEDULED'", value1, requestId);
	}

	LocalDate completeFindMembershipFreezePeriods(UUID requestId) {
		return db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update", LocalDate.class, requestId);
	}

	int completeUpdateMembershipFreezePeriods(Date value1, int creditedDays, UUID requestId) {
		return db.update("update membership_freeze_periods set status='COMPLETED', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'", value1, creditedDays, requestId);
	}

	LocalDate endEarlyForTerminationFindMembershipFreezePeriods(UUID requestId) {
		return db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update", LocalDate.class, requestId);
	}

	int endEarlyForTerminationUpdateMembershipFreezePeriods(Date value1, int creditedDays, UUID requestId) {
		return db.update("update membership_freeze_periods set status='ENDED_EARLY', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'", value1, creditedDays, requestId);
	}

	private final JdbcTemplate db;

	MembershipFreezeExecutionRepository(JdbcTemplate db) {
		this.db = db;
	}
}
