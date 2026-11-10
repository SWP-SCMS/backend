package com.scms.backend.membership.refund;

import com.scms.backend.membership.RefundRequest;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class MembershipRefundRepository {

	private final JdbcTemplate db;

	MembershipRefundRepository(JdbcTemplate db) {
		this.db = db;
	}

	Map<UUID, Map<String, Object>> display(List<RefundRequest> requests) {
		if (requests.isEmpty())
			return Map.of();
		String placeholders = String.join(",", java.util.Collections.nCopies(requests.size(), "?"));
		return db.queryForList("""
			select refund.id, profile.member_code, account.full_name member_full_name,
				o.order_number, p.provider_reference payment_reference, r.receipt_number
			from refund_requests refund
			left join accounts account on account.id=refund.member_account_id
			left join member_profiles profile on profile.account_id=refund.member_account_id
			left join membership_orders o on o.id=refund.order_id
			left join payments p on p.id=refund.payment_id
			left join receipts r on r.id=refund.receipt_id
			where refund.id in (
			""" + placeholders + ")", requests.stream().map(RefundRequest::getId).toArray()).stream().collect(java.util.stream.Collectors.toMap(row -> (UUID) row.get("id"), row -> row));
	}

	List<Map<java.lang.String, java.lang.Object>> refundEligibleSourcesFindPayments(UUID memberId) {
		return db.queryForList("""
			select p.id payment_id, m.id membership_id, r.id receipt_id, r.receipt_number,
				o.order_number, m.offer_name_snapshot offer_name, profile.member_code,
				account.full_name member_full_name, p.provider_reference payment_reference,
				m.status membership_status, p.amount paid_amount, p.currency_code currency,
				m.starts_at, m.duration_days_snapshot, m.price_amount_snapshot,
				coalesce(freeze_totals.credited_days, 0) credited_days
			from payments p
			join membership_orders o on o.id=p.order_id
			join memberships m on m.order_id=o.id and m.member_account_id=o.member_account_id
			join receipts r on r.payment_id=p.id and r.order_id=o.id and r.member_account_id=o.member_account_id
			join accounts account on account.id=o.member_account_id
			join member_profiles profile on profile.account_id=account.id
			left join (
				select membership_id, sum(credited_days) credited_days
				from membership_freeze_periods
				where status in ('COMPLETED','ENDED_EARLY')
				group by membership_id
			) freeze_totals on freeze_totals.membership_id=m.id
			where o.member_account_id=? and p.method='CASH' and p.status='PAID'
				and m.status in ('TERMINATED','RESERVED')
				and not exists (
					select 1 from refund_requests refund
					where refund.payment_id=p.id and refund.status not in ('REJECTED','CANCELLED')
				)
			order by p.paid_at desc, p.id desc
			""", memberId);
	}

	Map<java.lang.String, java.lang.Object> createFindPayments(UUID paymentId, UUID membershipId, UUID memberId) {
		return db.queryForMap("""
			select p.id payment_id, p.order_id, p.method, p.status payment_status,
				m.id membership_id, m.member_account_id, m.status membership_status, m.price_amount_snapshot, r.id receipt_id
			from payments p join membership_orders o on o.id=p.order_id
			join memberships m on m.order_id=o.id join receipts r on r.payment_id=p.id
			where p.id=? and m.id=? and m.member_account_id=?
			order by r.issued_at desc, r.id limit 1
			for update of p, m
			""", paymentId, membershipId, memberId);
	}

	int finalApprovalInsertCreditNotes(UUID randomUUID, UUID requestId, UUID receiptId, UUID paymentId, UUID memberAccountId, BigInteger approvedAmount, UUID actor, Timestamp nowTimestamp) {
		return db.update("insert into credit_notes(id,refund_request_id,receipt_id,payment_id,member_account_id,amount,issued_by_account_id,issued_at) values(?,?,?,?,?,?,?,?)", randomUUID, requestId, receiptId, paymentId, memberAccountId, approvedAmount, actor, nowTimestamp);
	}

	Map<java.lang.String, java.lang.Object> refundSourceFindMemberships(UUID membershipId) {
		return db.queryForMap("""
			select m.starts_at, m.status membership_status, m.duration_days_snapshot, m.price_amount_snapshot,
				coalesce(sum(period.credited_days), 0) credited_days
			from memberships m left join membership_freeze_periods period
				on period.membership_id=m.id and period.status in ('COMPLETED','ENDED_EARLY')
			where m.id=? group by m.id
			""", membershipId);
	}
}
