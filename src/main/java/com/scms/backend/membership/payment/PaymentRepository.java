package com.scms.backend.membership.payment;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRole;
import com.scms.backend.membership.MembershipBenefitStatus;
import com.scms.backend.membership.MembershipHistoryResponse;
import com.scms.backend.membership.ReconciliationQueueStatus;
import com.scms.backend.membership.StaffMembershipResponse;
import com.scms.backend.membership.payment.PaymentService.MembershipBenefits;
import com.scms.backend.membership.payment.PaymentService.PlanBenefitRow;
import com.scms.backend.membership.plan.MembershipBenefitResponse;
import com.scms.backend.membership.reporting.RevenueReportResponse;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class PaymentRepository {

	private final JdbcTemplate db;

	private final Clock clock;

	PaymentRepository(JdbcTemplate db, Clock clock) {
		this.db = db;
		this.clock = clock;
	}

	Map<String, MembershipBenefits> benefitsByPlanCode(List<String> planCodes) {
		List<String> codes = new ArrayList<>(new TreeSet<>(planCodes));
		if (codes.isEmpty())
			return Map.of();
		String placeholders = String.join(",", java.util.Collections.nCopies(codes.size(), "?"));
		List<PlanBenefitRow> rows = db.query("""
			select link.plan_code,benefit.code,benefit.display_name,benefit.description,benefit.status,
			       benefit.display_order,benefit.group_code,benefit_group.display_name as group_display_name,
			       benefit_group.display_order as group_display_order
			from membership_plan_benefits link
			join membership_benefits benefit on benefit.code=link.benefit_code
			left join membership_benefit_groups benefit_group on benefit_group.code=benefit.group_code
			where link.plan_code in (%s)
			order by link.plan_code,coalesce(benefit_group.display_order,benefit.display_order),benefit.display_order,benefit.code
			""".formatted(placeholders), (rs, row) -> new PlanBenefitRow(rs.getString("plan_code"), rs.getString("code"), rs.getString("display_name"), rs.getString("description"), MembershipBenefitStatus.valueOf(rs.getString("status")), rs.getInt("display_order"), rs.getString("group_code"), rs.getString("group_display_name"), rs.getObject("group_display_order", Integer.class)), codes.toArray());
		Map<String, List<MembershipBenefitResponse>> grouped = new LinkedHashMap<>();
		for (PlanBenefitRow row : rows) {
			grouped.computeIfAbsent(row.planCode(), ignored -> new ArrayList<>()).add(new MembershipBenefitResponse(row.code(), row.displayName(), row.description(), row.status(), row.displayOrder(), row.groupCode(), row.groupDisplayName(), row.groupDisplayOrder()));
		}
		Map<String, MembershipBenefits> result = new LinkedHashMap<>();
		grouped.forEach((code, planBenefits) -> result.put(code, new MembershipBenefits(planBenefits.stream().map(MembershipBenefitResponse::code).sorted().toList(), List.copyOf(planBenefits))));
		return result;
	}

	String range(String base, String column, Instant from, Instant to) {
		StringBuilder p = new StringBuilder(base);
		if (from != null)
			p.append(" and ").append(column).append(">=?");
		if (to != null)
			p.append(" and ").append(column).append("<?");
		return p.toString();
	}

	void addRangeArgs(List<Object> args, Instant from, Instant to) {
		if (from != null)
			args.add(Timestamp.from(from));
		if (to != null)
			args.add(Timestamp.from(to));
	}

	long number(Map<String, Object> row, String key) {
		return ((Number) row.get(key)).longValue();
	}

	BigInteger big(Map<String, Object> row, String key) {
		return new BigDecimal(row.get(key).toString()).toBigIntegerExact();
	}

	ReceiptResponse receipt(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new ReceiptResponse(rs.getObject("id", UUID.class), rs.getString("receipt_number"), rs.getObject("payment_id", UUID.class), rs.getObject("order_id", UUID.class), rs.getString("plan_code_snapshot"), rs.getString("offer_name_snapshot"), rs.getObject("member_account_id", UUID.class), rs.getString("member_name"), rs.getString("member_code"), rs.getBigDecimal("amount_snapshot").toBigIntegerExact(), rs.getString("currency_code_snapshot").trim(), rs.getString("payment_method_snapshot"), rs.getTimestamp("issued_at").toInstant(), rs.getString("order_number"), rs.getString("payment_reference"));
	}

	ReceiptResponse receipt(Map<String, Object> r) {
		return new ReceiptResponse((UUID) r.get("id"), r.get("receipt_number").toString(), (UUID) r.get("payment_id"), (UUID) r.get("order_id"), r.get("plan_code_snapshot").toString(), r.get("offer_name_snapshot").toString(), (UUID) r.get("member_account_id"), r.get("member_name").toString(), r.get("member_code").toString(), new BigDecimal(r.get("amount_snapshot").toString()).toBigIntegerExact(), r.get("currency_code_snapshot").toString().trim(), r.get("payment_method_snapshot").toString(), ((Timestamp) r.get("issued_at")).toInstant(), (String) r.get("order_number"), (String) r.get("payment_reference"));
	}

	ReconciliationQueuePageResponse reconciliationRows(int page, int size, ReconciliationQueueStatus effectiveStatus, String normalizedQuery) {
		StringBuilder where = new StringBuilder(" where p.status='PENDING' and o.status='PENDING_PAYMENT'");
		List<Object> filterArgs = new ArrayList<>();
		if (effectiveStatus == ReconciliationQueueStatus.PENDING) {
			where.append(" and (o.expires_at is null or o.expires_at > ?)");
			filterArgs.add(Timestamp.from(clock.instant()));
		} else if (effectiveStatus == ReconciliationQueueStatus.EXPIRED_WINDOW) {
			where.append(" and o.expires_at is not null and o.expires_at <= ?");
			filterArgs.add(Timestamp.from(clock.instant()));
		}
		if (normalizedQuery != null) {
			where.append(" and (o.order_number ilike ? or p.id::text ilike ? or a.full_name ilike ?").append(" or coalesce(p.bank_transfer_content,'') ilike ?)");
			String pattern = "%" + normalizedQuery + "%";
			filterArgs.add(pattern);
			filterArgs.add(pattern);
			filterArgs.add(pattern);
			filterArgs.add(pattern);
		}
		Long count = db.queryForObject("select count(*) from payments p join membership_orders o on o.id=p.order_id " + "join accounts a on a.id=o.member_account_id" + where, Long.class, filterArgs.toArray());
		long totalElements = count == null ? 0 : count;
		List<Object> contentArgs = new ArrayList<>(filterArgs);
		contentArgs.add(size);
		contentArgs.add(Math.multiplyFull(page, size));
		String sql = """
			select p.id payment_id,p.order_id,o.order_number,o.member_account_id,a.full_name member_name,
				m.member_code member_code,p.provider_reference payment_reference,p.amount,p.currency_code,
				p.bank_transfer_content transfer_content,p.created_at,o.expires_at
			from payments p
			join membership_orders o on o.id=p.order_id
			join accounts a on a.id=o.member_account_id
			left join member_profiles m on m.account_id=a.id
			""" + where + " order by p.created_at desc, p.id desc limit ? offset ?";
		List<ReconciliationQueueItem> content = db.query(sql, (rs, rowNum) -> {
			Timestamp expiresAt = rs.getTimestamp("expires_at");
			return new ReconciliationQueueItem(rs.getObject("payment_id", UUID.class), rs.getObject("order_id", UUID.class), rs.getString("order_number"), rs.getObject("member_account_id", UUID.class), rs.getString("member_name"), rs.getBigDecimal("amount").toBigIntegerExact(), rs.getString("currency_code").trim(), rs.getString("transfer_content"), rs.getTimestamp("created_at").toInstant(), expiresAt == null ? null : expiresAt.toInstant(), rs.getString("member_code"), rs.getString("payment_reference"));
		}, contentArgs.toArray());
		int totalPages = totalElements == 0 ? 0 : (int) Math.min(Integer.MAX_VALUE, Math.ceilDiv(totalElements, (long) size));
		return new ReconciliationQueuePageResponse(content, page, size, totalElements, totalPages);
	}

	List<ReceiptResponse> receptionReceiptRows(UUID actor, Account account, UUID memberId) {
		String ownership = account.getRole() == AccountRole.RECEPTIONIST ? " and p.processed_by_account_id=?" : "";
		Object[] args = account.getRole() == AccountRole.RECEPTIONIST ? new Object[] { memberId, actor } : new Object[] { memberId };
		return db.query("""
			select r.*,o.plan_code_snapshot,o.offer_name_snapshot,o.order_number,a.full_name member_name,
				m.member_code,p.provider_reference payment_reference from receipts r
			join membership_orders o on o.id=r.order_id
			join accounts a on a.id=r.member_account_id
			join member_profiles m on m.account_id=a.id
			join payments p on p.id=r.payment_id
			where r.member_account_id=?""" + ownership + " order by r.issued_at desc,r.id desc", (rs, row) -> receipt(rs), args);
	}

	RevenueReportResponse reportRows(Instant from, Instant to, boolean includeTestData) {
		String test = includeTestData ? "" : " and not o.is_test_data";
		String paid = range("p.status='PAID'", "p.paid_at", from, to);
		String pending = range("p.status='PENDING'", "p.created_at", from, to);
		String failed = range("p.status='FAILED'", "p.updated_at", from, to);
		String sql = "select count(*) filter(where " + paid + ") paid_payments,coalesce(sum(p.amount) filter(where " + paid + "),0) paid_revenue," + "count(*) filter(where " + pending + ") pending_payments,count(*) filter(where " + failed + ") failed_payments from payments p join membership_orders o on o.id=p.order_id where true" + test;
		List<Object> args = new ArrayList<>();
		addRangeArgs(args, from, to);
		addRangeArgs(args, from, to);
		addRangeArgs(args, from, to);
		addRangeArgs(args, from, to);
		Map<String, Object> row = db.queryForMap(sql, args.toArray());
		String completedRefunds = range("r.status='COMPLETED'", "r.completed_at", from, to);
		String refundSql = "select coalesce(sum(cn.amount),0) completed_refund_amount from credit_notes cn " + "join refund_requests r on r.id=cn.refund_request_id join membership_orders o on o.id=r.order_id where " + completedRefunds + test;
		List<Object> refundArgs = new ArrayList<>();
		addRangeArgs(refundArgs, from, to);
		Map<String, Object> refundRow = db.queryForMap(refundSql, refundArgs.toArray());
		Map<String, Object> memberships = db.queryForMap("select count(*) filter(where m.status='ACTIVE' and m.starts_at<=current_timestamp and m.ends_at>current_timestamp) active,count(*) filter(where m.status='EXPIRED' or (m.status='ACTIVE' and m.ends_at<=current_timestamp)) expired from memberships m join membership_orders o on o.id=m.order_id where true" + test);
		BigInteger paidRevenue = big(row, "paid_revenue");
		BigInteger completedRefundAmount = big(refundRow, "completed_refund_amount");
		return new RevenueReportResponse(from, to, number(row, "paid_payments"), paidRevenue, completedRefundAmount, paidRevenue.subtract(completedRefundAmount), number(row, "pending_payments"), number(row, "failed_payments"), number(memberships, "active"), number(memberships, "expired"), includeTestData);
	}

	Map<java.lang.String, java.lang.Object> cashFindAccounts(java.lang.String memberCode) {
		return db.queryForMap("""
			select a.id account_id from accounts a join member_profiles m on m.account_id=a.id
			where m.member_code=? and a.role='MEMBER' and a.status='ACTIVE'
			""", memberCode);
	}

	Map<java.lang.String, java.lang.Object> cashFindMembershipOrders(UUID actor, java.lang.String normalizedIdempotencyKey) {
		return db.queryForMap("""
			select o.member_account_id,p.id payment_id
			from membership_orders o join payments p on p.order_id=o.id
			where o.created_by_account_id=? and o.cash_idempotency_key=?
			for update
			""", actor, normalizedIdempotencyKey);
	}

	Map<java.lang.String, java.lang.Object> cashFindMembershipOffers(@jakarta.validation.constraints.NotNull UUID offerId) {
		return db.queryForMap("""
			select offer.id,offer.name,offer.plan_code,offer.price_amount,offer.currency_code,offer.duration_days
			from membership_offers offer join membership_plans plan on plan.plan_code=offer.plan_code
			where offer.id=? and offer.status='ACTIVE' and plan.status='ACTIVE'
			for update of offer, plan
			""", offerId);
	}

	long countActiveMemberships(UUID memberId) {
		return count("select count(*) from memberships where member_account_id=? and status='ACTIVE'", memberId);
	}

	long countReservedMemberships(UUID memberId) {
		return count("select count(*) from memberships where member_account_id=? and status='RESERVED'", memberId);
	}

	long countPendingOrders(UUID memberId) {
		return count("select count(*) from membership_orders where member_account_id=? and status='PENDING_PAYMENT'", memberId);
	}

	int cashInsertMembershipOrders(UUID orderId, java.lang.String orderNumber, UUID memberId, UUID actor, @jakarta.validation.constraints.NotNull UUID offerId, java.lang.Object name, java.lang.Object planCode, java.lang.Object priceAmount, java.lang.Object currencyCode, java.lang.Object durationDays, java.lang.String mode, java.lang.String normalizedIdempotencyKey) {
		return db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
				duration_days_snapshot,payment_method,status,fulfillment_mode,cash_idempotency_key)
			values(?,?,?,?,?,?,?,?,?,?,'CASH','PENDING_PAYMENT',?,?)
			""", orderId, orderNumber, memberId, actor, offerId, name, planCode, priceAmount, currencyCode, durationDays, mode, normalizedIdempotencyKey);
	}

	int reconcileUpdatePayments(java.lang.String value1, java.lang.String value2, UUID actor, UUID paymentId) {
		return db.update("update payments set status='FAILED',failure_reason=?,evidence=?,processed_by_account_id=?,updated_at=current_timestamp where id=? and status='PENDING'", value1, value2, actor, paymentId);
	}

	int reconcileUpdateMembershipOrders(java.lang.Object orderId) {
		return db.update("update membership_orders set status='EXPIRED',updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'", orderId);
	}

	Map<java.lang.String, java.lang.Object> cancelOrderFindMembershipOrders(UUID orderId) {
		return db.queryForMap("select id,status from membership_orders where id=? for update", orderId);
	}

	int cancelOrderUpdatePayments(java.lang.String value1, java.lang.String value2, UUID actor, Timestamp nowTimestamp, UUID orderId) {
		return db.update("""
			update payments set status='FAILED',failure_reason=?,evidence=?,processed_by_account_id=?,
				updated_at=? where order_id=? and status='PENDING'
			""", value1, value2, actor, nowTimestamp, orderId);
	}

	int cancelOrderUpdateMembershipOrders(Timestamp nowTimestamp, UUID orderId) {
		return db.update("""
			update membership_orders set status='EXPIRED',updated_at=?,version=version+1
			where id=? and status='PENDING_PAYMENT'
			""", nowTimestamp, orderId);
	}

	List<com.scms.backend.membership.payment.ReceiptResponse> receiptsFindReceipts(UUID actor) {
		return db.query("""
			select r.*,o.plan_code_snapshot,o.offer_name_snapshot,o.order_number,a.full_name member_name,
				m.member_code,p.provider_reference payment_reference from receipts r
			join membership_orders o on o.id=r.order_id
			join accounts a on a.id=r.member_account_id
			join member_profiles m on m.account_id=a.id
			join payments p on p.id=r.payment_id
			where r.member_account_id=? order by r.issued_at desc,r.id desc
			""", (rs, row) -> receipt(rs), actor);
	}

	UUID receiptsForReceptionistFindMemberProfiles(java.lang.String trim) {
		return db.queryForObject("""
			select a.id from member_profiles m join accounts a on a.id=m.account_id
			where m.member_code=? and a.role='MEMBER'
			""", UUID.class, trim);
	}

	Map<java.lang.String, java.lang.Object> receiptFindReceipts(UUID receiptId) {
		return db.queryForMap("""
			select r.*,o.plan_code_snapshot,o.offer_name_snapshot,o.order_number,a.full_name member_name,
			m.member_code,p.provider_reference payment_reference,p.processed_by_account_id from receipts r
			join membership_orders o on o.id=r.order_id
			join accounts a on a.id=r.member_account_id
			join member_profiles m on m.account_id=a.id
			join payments p on p.id=r.payment_id
			where r.id=?
			""", receiptId);
	}

	List<MembershipHistoryResponse> memberHistoryFindMemberships(UUID actor) {
		return db.query("select id,order_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at from memberships where member_account_id=? order by starts_at desc nulls first,id desc", (rs, row) -> new MembershipHistoryResponse(rs.getObject("id", UUID.class), rs.getObject("order_id", UUID.class), rs.getString("plan_code_snapshot"), rs.getString("offer_name_snapshot"), rs.getBigDecimal("price_amount_snapshot").toBigIntegerExact(), rs.getString("currency_code_snapshot").trim(), rs.getInt("duration_days_snapshot"), rs.getString("status"), instant(rs.getTimestamp("starts_at")), instant(rs.getTimestamp("ends_at"))), actor);
	}

	List<StaffMembershipResponse> staffMembershipHistoryFindMemberships(UUID memberId) {
		return db.query("""
			select id,plan_code_snapshot,offer_name_snapshot,status,starts_at,ends_at
			from memberships where member_account_id=? order by starts_at desc,id desc
			""", (rs, row) -> new StaffMembershipResponse(rs.getObject("id", UUID.class), rs.getString("plan_code_snapshot"), rs.getString("offer_name_snapshot"), rs.getString("status"), instant(rs.getTimestamp("starts_at")), instant(rs.getTimestamp("ends_at"))), memberId);
	}

	private long count(String sql, Object... args) {
		Long value = db.queryForObject(sql, Long.class, args);
		return value == null ? 0 : value;
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}
}
