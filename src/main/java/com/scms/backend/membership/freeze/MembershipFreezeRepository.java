package com.scms.backend.membership.freeze;

import java.math.BigInteger;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class MembershipFreezeRepository {

	int collectCashInsertMembershipFreezeFeePayments(java.util.UUID paymentId, java.util.UUID requestId, java.math.BigInteger feeAmount, java.util.UUID receptionistId, java.sql.Timestamp nowTimestamp) {
		return db.update("insert into membership_freeze_fee_payments(id,request_id,amount,currency_code,collected_by_account_id,paid_at) values(?,?,?,'VND',?,?)", paymentId, requestId, feeAmount, receptionistId, nowTimestamp);
	}

	int collectCashInsertMembershipFreezePeriods(java.util.UUID randomUUID, java.util.UUID requestId, java.util.UUID membershipId, java.sql.Date value4, java.sql.Date value5) {
		return db.update("insert into membership_freeze_periods(id,request_id,membership_id,scheduled_start_date,scheduled_end_date,status) values(?,?,?,?,?,'SCHEDULED')", randomUUID, requestId, membershipId, value4, value5);
	}

	java.util.Map<java.lang.String, java.lang.Object> paidResponseFindMembershipFreezeFeePayments(java.util.UUID requestId) {
		return db.queryForMap("select id,amount,paid_at from membership_freeze_fee_payments where request_id=?", requestId);
	}

	java.time.LocalDate endEarlyFindMembershipFreezePeriods(java.util.UUID requestId) {
		return db.queryForObject("select actual_start_date from membership_freeze_periods where request_id=? and status='ACTIVE' for update", LocalDate.class, requestId);
	}

	int endEarlyUpdateMembershipFreezePeriods(java.sql.Date value1, int creditedDays, java.util.UUID requestId) {
		return db.update("update membership_freeze_periods set status='ENDED_EARLY', actual_end_date=?, credited_days=?, updated_at=current_timestamp, version=version+1 where request_id=? and status='ACTIVE'", value1, creditedDays, requestId);
	}

	private final JdbcTemplate db;

	MembershipFreezeRepository(JdbcTemplate db) {
		this.db = db;
	}
}
