package com.scms.backend.membership.reporting;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CenterRevenueRepository {
	private final NamedParameterJdbcTemplate db;

	CenterRevenueRepository(JdbcTemplate db) {
		this.db = new NamedParameterJdbcTemplate(db);
	}

	BigInteger pendingMembershipRefunds(boolean includeTestData) {
		return db.queryForObject("""
			select coalesce(sum(coalesce(r.approved_amount,r.requested_amount)),0)
			from refund_requests r join membership_orders o on o.id=r.order_id
			where r.status in ('REQUESTED','FIRST_APPROVED','CASH_DISBURSED')
			  and (:includeTestData or not o.is_test_data)
			""", Map.of("includeTestData", includeTestData), java.math.BigDecimal.class).toBigIntegerExact();
	}

	RevenueSourceResponse course(Instant from, Instant to, boolean includeTestData) {
		Map<String, Object> params = new HashMap<>();
		params.put("from", from == null ? null : Timestamp.from(from));
		params.put("to", to == null ? null : Timestamp.from(to));
		params.put("includeTestData", includeTestData);
		return db.queryForObject("""
			with scoped_payments as (
				select p.*,case p.status when 'PAID' then p.paid_at
					when 'FAILED' then p.updated_at else p.created_at end event_at
				from course_payments p join accounts a on a.id=p.member_account_id
				where (:includeTestData or not a.is_test_data)
			), payments as (
				select count(*) filter(where status='PAID') paid,
					coalesce(sum(amount) filter(where status='PAID'),0) received,
					count(*) filter(where status='PENDING') pending,
					count(*) filter(where status='FAILED') failed
				from scoped_payments
				where (cast(:from as timestamptz) is null or event_at>=:from)
				  and (cast(:to as timestamptz) is null or event_at<:to)
			), refunds as (
				select coalesce(sum(r.verified_amount_vnd) filter(where r.status='COMPLETED'
					and (cast(:from as timestamptz) is null or r.verified_at>=:from)
					and (cast(:to as timestamptz) is null or r.verified_at<:to)),0) refunded,
					coalesce(sum(coalesce(r.approved_amount_vnd,r.amount_vnd)-r.verified_amount_vnd)
						filter(where r.status in ('PENDING_REVIEW','APPROVED','DISBURSEMENT_RECORDED','PENDING_VERIFICATION')),0) pending_refund
				from course_refund_obligations r join accounts a on a.id=r.member_account_id
				where (:includeTestData or not a.is_test_data)
			)
			select * from payments cross join refunds
			""", params, (rs, row) -> {
				BigInteger received = rs.getBigDecimal("received").toBigIntegerExact();
				BigInteger refunded = rs.getBigDecimal("refunded").toBigIntegerExact();
				return new RevenueSourceResponse(rs.getLong("paid"), received, refunded, received.subtract(refunded),
					rs.getLong("pending"), rs.getLong("failed"), rs.getBigDecimal("pending_refund").toBigIntegerExact());
			});
	}
}
