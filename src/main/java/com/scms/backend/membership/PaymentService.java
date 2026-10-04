package com.scms.backend.membership;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.scms.backend.account.Account;
import com.scms.backend.account.AccountRepository;
import com.scms.backend.account.AccountRole;
import com.scms.backend.account.AccountStatus;
import com.scms.backend.audit.AuditEvent;
import com.scms.backend.audit.AuditEventRepository;
import com.scms.backend.auth.InvalidAuthenticatedAccountException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PaymentService {
	private final JdbcTemplate db;
	private final AccountRepository accounts;
	private final AuditEventRepository audits;
	private final PaymentFulfillmentService fulfillment;
	private final Clock clock;

	PaymentService(JdbcTemplate db, AccountRepository accounts, AuditEventRepository audits,
			PaymentFulfillmentService fulfillment, Clock clock) {
		this.db=db; this.accounts=accounts; this.audits=audits; this.fulfillment=fulfillment; this.clock=clock;
	}

	@Transactional
	PaymentResultResponse cash(UUID actor, String memberCode, CashPaymentRequest request) {
		ensure(actor, AccountRole.RECEPTIONIST);
		if (request == null || request.offerId() == null) throw PaymentException.validation("offerId is required");
		Map<String,Object> member;
		Map<String,Object> offer;
		try {
			member = db.queryForMap("""
				select a.id account_id from accounts a join member_profiles m on m.account_id=a.id
				where m.member_code=? and a.role='MEMBER' and a.status='ACTIVE'
				""", memberCode);
			offer = db.queryForMap("select * from membership_offers where id=? and status='ACTIVE'", request.offerId());
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Active Member or Membership Offer was not found");
		}
		UUID memberId=(UUID)member.get("account_id");
		if (count("select count(*) from memberships where member_account_id=? and status='ACTIVE'", memberId)>0)
			throw PaymentException.conflict("Member already has an active membership");
		if (count("select count(*) from membership_orders where member_account_id=? and status='PENDING_PAYMENT'", memberId)>0)
			throw PaymentException.conflict("Member already has a pending membership order");
		UUID orderId=UUID.randomUUID(); UUID paymentId=UUID.randomUUID();
		String orderNumber="ORD-"+orderId.toString().replace("-","").toUpperCase();
		db.update("""
			insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,
				offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,
				duration_days_snapshot,payment_method,status)
			values(?,?,?,?,?,?,?,?,?,?,'CASH','PENDING_PAYMENT')
			""", orderId,orderNumber,memberId,actor,request.offerId(),offer.get("name"),offer.get("plan_code"),
			offer.get("price_amount"),offer.get("currency_code"),offer.get("duration_days"));
		return fulfillment.fulfillCash(paymentId, orderId, actor, "Cash received by receptionist",
			"Cash payment confirmed");
	}

	@Transactional
	PaymentResultResponse reconcile(UUID actor, UUID paymentId, PaymentActionRequest request) {
		ensureAny(actor);
		if (request == null || request.status() == null || blank(request.reason()) || blank(request.evidence()))
			throw PaymentException.validation("status, evidence and reason are required");
		Map<String,Object> payment=fulfillment.paymentForUpdate(paymentId);
		if (request.status() == PaymentActionRequest.ReconciliationStatus.PAID) {
			if (request.receivedAmount()==null || blank(request.transferContent()) || blank(request.providerTransactionId()))
				throw PaymentException.validation("receivedAmount, transferContent and providerTransactionId are required for PAID");
			if (!PaymentFulfillmentService.amount(payment,"payment_amount").equals(request.receivedAmount()))
				throw PaymentException.validation("receivedAmount does not match Payment amount");
			String expectedContent=PaymentFulfillmentService.string(payment,"bank_transfer_content");
			if (expectedContent==null || !expectedContent.equals(request.transferContent().trim()))
				throw PaymentException.validation("transferContent does not match Payment");
			return fulfillment.fulfill(paymentId,actor,request.providerTransactionId().trim(),request.evidence().trim(),request.reason().trim());
		}
		String status=PaymentFulfillmentService.string(payment,"payment_status");
		if (!"PENDING".equals(status)) throw PaymentException.conflict("Payment is terminal");
		Object expires=payment.get("expires_at");
		Instant expiry=expires instanceof Timestamp value ? value.toInstant() : (Instant)expires;
		if (expiry==null || clock.instant().isBefore(expiry)) throw PaymentException.conflict("Payment window has not expired");
		db.update("update payments set status='FAILED',failure_reason=?,evidence=?,processed_by_account_id=?,updated_at=current_timestamp where id=? and status='PENDING'",
			request.reason().trim(),request.evidence().trim(),actor,paymentId);
		db.update("update membership_orders set status='EXPIRED',updated_at=current_timestamp where id=? and status='PENDING_PAYMENT'",
			payment.get("order_id"));
		audits.save(new AuditEvent(UUID.randomUUID(),actor,"PAYMENT_RECONCILED","PAYMENT",paymentId,
			request.reason().trim(),Map.of("status","PENDING"),Map.of("status","FAILED","evidence",request.evidence().trim())));
		return new PaymentResultResponse(paymentId,(UUID)payment.get("order_id"),"FAILED",
			PaymentFulfillmentService.string(payment,"payment_method"),null,null,null);
	}

	@Transactional
	MembershipOrderCancellationResponse cancelOrder(UUID actor, UUID orderId,
			MembershipOrderCancellationRequest request) {
		ensureAny(actor);
		if (request == null || blank(request.reason())) {
			throw PaymentException.validation("reason is required");
		}
		String reason = request.reason().trim();
		Map<String, Object> order;
		try {
			order = db.queryForMap("select id,status from membership_orders where id=? for update", orderId);
		} catch (EmptyResultDataAccessException exception) {
			throw PaymentException.notFound("Membership Order was not found");
		}
		if (!"PENDING_PAYMENT".equals(PaymentFulfillmentService.string(order, "status"))) {
			throw PaymentException.conflict("Only a pending Membership Order can be cancelled");
		}
		Instant now = clock.instant();
		int cancelledPayments = db.update("""
			update payments set status='FAILED',failure_reason=?,evidence=?,processed_by_account_id=?,
				updated_at=? where order_id=? and status='PENDING'
			""", "Order cancelled: " + reason, "Cancelled by authorized staff", actor,
			Timestamp.from(now), orderId);
		int cancelledOrder = db.update("""
			update membership_orders set status='EXPIRED',updated_at=?,version=version+1
			where id=? and status='PENDING_PAYMENT'
			""", Timestamp.from(now), orderId);
		if (cancelledOrder != 1) {
			throw PaymentException.conflict("Membership Order is no longer pending");
		}
		audits.save(new AuditEvent(UUID.randomUUID(), actor, "MEMBERSHIP_ORDER_CANCELLED",
			"MEMBERSHIP_ORDER", orderId, reason, Map.of("status", "PENDING_PAYMENT"),
			Map.of("status", "EXPIRED", "cancelledPayments", cancelledPayments)));
		return new MembershipOrderCancellationResponse(orderId, "EXPIRED", cancelledPayments, now, reason);
	}

	@Transactional(readOnly = true)
	ReconciliationQueuePageResponse reconciliationQueue(UUID actor, ReconciliationQueueStatus status,
			String query, int page, int size) {
		ensureAny(actor);
		ReconciliationQueueStatus effectiveStatus = status == null ? ReconciliationQueueStatus.ALL : status;
		String normalizedQuery = query == null ? null : query.trim();
		if (normalizedQuery != null && normalizedQuery.isEmpty()) normalizedQuery = null;

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
			where.append(" and (o.order_number ilike ? or p.id::text ilike ? or a.full_name ilike ?")
				.append(" or coalesce(p.bank_transfer_content,'') ilike ?)");
			String pattern = "%" + normalizedQuery + "%";
			filterArgs.add(pattern);
			filterArgs.add(pattern);
			filterArgs.add(pattern);
			filterArgs.add(pattern);
		}

		Long count = db.queryForObject("select count(*) from payments p join membership_orders o on o.id=p.order_id "
			+ "join accounts a on a.id=o.member_account_id" + where, Long.class, filterArgs.toArray());
		long totalElements = count == null ? 0 : count;
		List<Object> contentArgs = new ArrayList<>(filterArgs);
		contentArgs.add(size);
		contentArgs.add(Math.multiplyFull(page, size));
		String sql = """
			select p.id payment_id,p.order_id,o.order_number,o.member_account_id,a.full_name member_name,
				p.amount,p.currency_code,p.bank_transfer_content transfer_content,p.created_at,o.expires_at
			from payments p
			join membership_orders o on o.id=p.order_id
			join accounts a on a.id=o.member_account_id
			""" + where + " order by p.created_at desc, p.id desc limit ? offset ?";
		List<ReconciliationQueueItem> content = db.query(sql, (rs, rowNum) -> {
			Timestamp expiresAt = rs.getTimestamp("expires_at");
			return new ReconciliationQueueItem(rs.getObject("payment_id", UUID.class),
				rs.getObject("order_id", UUID.class), rs.getString("order_number"),
				rs.getObject("member_account_id", UUID.class), rs.getString("member_name"),
				rs.getBigDecimal("amount").toBigIntegerExact(), rs.getString("currency_code").trim(),
				rs.getString("transfer_content"), rs.getTimestamp("created_at").toInstant(),
				expiresAt == null ? null : expiresAt.toInstant());
		}, contentArgs.toArray());
		int totalPages = totalElements == 0 ? 0 : (int)Math.min(Integer.MAX_VALUE,
			Math.ceilDiv(totalElements, (long)size));
		return new ReconciliationQueuePageResponse(content, page, size, totalElements, totalPages);
	}

	@Transactional(readOnly=true)
	List<ReceiptResponse> receipts(UUID actor) {
		ensure(actor,AccountRole.MEMBER);
		return db.query("select * from receipts where member_account_id=? order by issued_at desc",
			(rs,row)->receipt(rs),actor);
	}

	@Transactional(readOnly=true)
	ReceiptResponse receipt(UUID actor, UUID receiptId) {
		Account account=active(actor);
		try {
			Map<String,Object> row=db.queryForMap("select * from receipts where id=?",receiptId);
			if (account.getRole()==AccountRole.MEMBER && !actor.equals(row.get("member_account_id"))) throw PaymentException.forbidden();
			if (account.getRole()==AccountRole.COACH)
				throw PaymentException.forbidden();
			return receipt(row);
		} catch (EmptyResultDataAccessException exception) { throw PaymentException.notFound("Receipt was not found"); }
	}

	@Transactional(readOnly=true)
	PaymentResultResponse result(UUID actor, UUID paymentId) {
		Account account=active(actor); Map<String,Object> row=fulfillment.payment(paymentId);
		if (account.getRole()==AccountRole.MEMBER && !actor.equals(row.get("member_account_id"))) throw PaymentException.forbidden();
		if (account.getRole()==AccountRole.COACH)
			throw PaymentException.forbidden();
		if (!"PAID".equals(row.get("payment_status"))) return new PaymentResultResponse(paymentId,(UUID)row.get("order_id"),
			row.get("payment_status").toString(),row.get("payment_method").toString(),null,null,null);
		return fulfillment.result(row);
	}

	@Transactional(readOnly=true)
	List<Map<String,Object>> memberHistory(UUID actor) {
		ensure(actor,AccountRole.MEMBER);
		return db.queryForList("select id,order_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at from memberships where member_account_id=? order by starts_at desc",actor);
	}

	@Transactional(readOnly=true)
	RevenueReportResponse report(UUID actor, Instant from, Instant to, boolean includeTestData) {
		ensure(actor,AccountRole.MANAGER);
		if (from!=null && to!=null && !from.isBefore(to)) throw PaymentException.validation("from must be earlier than to");
		String test=includeTestData ? "" : " and not o.is_test_data";
		String paid=range("p.status='PAID'","p.paid_at",from,to);
		String pending=range("p.status='PENDING'","p.created_at",from,to);
		String failed=range("p.status='FAILED'","p.updated_at",from,to);
		String sql="select count(*) filter(where "+paid+") paid_payments,coalesce(sum(p.amount) filter(where "+paid+"),0) paid_revenue,"+
			"count(*) filter(where "+pending+") pending_payments,count(*) filter(where "+failed+") failed_payments from payments p join membership_orders o on o.id=p.order_id where true"+test;
		List<Object> args=new ArrayList<>(); addRangeArgs(args,from,to); addRangeArgs(args,from,to); addRangeArgs(args,from,to); addRangeArgs(args,from,to);
		Map<String,Object> row=db.queryForMap(sql,args.toArray());
		Map<String,Object> memberships=db.queryForMap("select count(*) filter(where m.status='ACTIVE') active,count(*) filter(where m.status='EXPIRED') expired from memberships m join membership_orders o on o.id=m.order_id where true"+test);
		return new RevenueReportResponse(from,to,number(row,"paid_payments"),big(row,"paid_revenue"),number(row,"pending_payments"),
			number(row,"failed_payments"),number(memberships,"active"),number(memberships,"expired"),includeTestData);
	}

	private String range(String base,String column,Instant from,Instant to){StringBuilder p=new StringBuilder(base);if(from!=null)p.append(" and ").append(column).append(">=?");if(to!=null)p.append(" and ").append(column).append("<?");return p.toString();}
	private void addRangeArgs(List<Object> args,Instant from,Instant to){if(from!=null)args.add(Timestamp.from(from));if(to!=null)args.add(Timestamp.from(to));}
	private long count(String sql,Object...args){Long value=db.queryForObject(sql,Long.class,args);return value==null?0:value;}
	private long number(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
	private BigInteger big(Map<String,Object> row,String key){return new BigDecimal(row.get(key).toString()).toBigIntegerExact();}
	private boolean blank(String value){return value==null||value.isBlank();}
	private Account active(UUID id){return accounts.findById(id).filter(a->a.getStatus()==AccountStatus.ACTIVE).orElseThrow(InvalidAuthenticatedAccountException::new);}
	private void ensure(UUID id,AccountRole role){if(!accounts.existsByIdAndRoleAndStatus(id,role,AccountStatus.ACTIVE))throw new InvalidAuthenticatedAccountException();}
	private void ensureAny(UUID id){if(!accounts.existsByIdAndRoleAndStatus(id,AccountRole.MANAGER,AccountStatus.ACTIVE)&&!accounts.existsByIdAndRoleAndStatus(id,AccountRole.RECEPTIONIST,AccountStatus.ACTIVE))throw new InvalidAuthenticatedAccountException();}
	private ReceiptResponse receipt(java.sql.ResultSet rs)throws java.sql.SQLException{return new ReceiptResponse(rs.getObject("id",UUID.class),rs.getString("receipt_number"),rs.getObject("payment_id",UUID.class),rs.getObject("order_id",UUID.class),rs.getObject("member_account_id",UUID.class),rs.getBigDecimal("amount_snapshot").toBigIntegerExact(),rs.getString("currency_code_snapshot").trim(),rs.getString("payment_method_snapshot"),rs.getTimestamp("issued_at").toInstant());}
	private ReceiptResponse receipt(Map<String,Object> r){return new ReceiptResponse((UUID)r.get("id"),r.get("receipt_number").toString(),(UUID)r.get("payment_id"),(UUID)r.get("order_id"),(UUID)r.get("member_account_id"),new BigDecimal(r.get("amount_snapshot").toString()).toBigIntegerExact(),r.get("currency_code_snapshot").toString().trim(),r.get("payment_method_snapshot").toString(),((Timestamp)r.get("issued_at")).toInstant());}
}
