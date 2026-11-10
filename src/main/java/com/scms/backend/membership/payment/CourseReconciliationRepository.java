package com.scms.backend.membership.payment;

import static com.scms.backend.membership.payment.PaymentFulfillmentService.instant;
import com.scms.backend.membership.ReconciliationQueueStatus;
import com.scms.backend.membership.payment.CourseReconciliationModels.Page;
import com.scms.backend.membership.payment.CourseReconciliationModels.QueueItem;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

final class CourseReconciliationRepository {

	private final JdbcTemplate db;

	private final Clock clock;

	CourseReconciliationRepository(JdbcTemplate db, Clock clock) {
		this.db = db;
		this.clock = clock;
	}

	Page queueRows(ReconciliationQueueStatus status, String query, int page, int size) {
		String from = " from course_payments p join course_orders o on o.id=p.course_order_id " + "join accounts a on a.id=p.member_account_id left join member_profiles m on m.account_id=a.id ";
		String where = " where p.status='PENDING' and p.method='SEPAY' and o.status='PENDING_PAYMENT'";
		List<Object> args = new ArrayList<>();
		if (status == ReconciliationQueueStatus.PENDING || status == ReconciliationQueueStatus.EXPIRED_WINDOW) {
			where += status == ReconciliationQueueStatus.PENDING ? " and p.deadline_at>?" : " and p.deadline_at<=?";
			args.add(Timestamp.from(clock.instant()));
		}
		if (query != null && !query.isBlank()) {
			where += " and (p.id::text ilike ? or o.order_reference ilike ? or p.payment_reference ilike ? or a.full_name ilike ? or m.member_code ilike ?)";
			for (int i = 0; i < 5; i++) args.add("%" + query.trim() + "%");
		}
		long count = db.queryForObject("select count(*)" + from + where, Long.class, args.toArray());
		args.add(size);
		args.add((long) page * size);
		List<QueueItem> items = db.query("select p.*,o.course_run_id,o.order_reference,m.member_code,a.full_name" + from + where + " order by p.created_at desc,p.id desc limit ? offset ?", (rs, n) -> new QueueItem(rs.getObject("id", UUID.class), rs.getObject("course_order_id", UUID.class), rs.getObject("course_reservation_id", UUID.class), rs.getObject("course_run_id", UUID.class), rs.getObject("member_account_id", UUID.class), rs.getString("member_code"), rs.getString("full_name"), rs.getString("order_reference"), rs.getString("payment_reference"), rs.getBigDecimal("amount").toBigIntegerExact(), rs.getString("currency_code").trim(), rs.getString("status"), rs.getTimestamp("deadline_at").toInstant(), rs.getTimestamp("created_at").toInstant()), args.toArray());
		return new Page(items, page, size, count, (int) Math.min(Integer.MAX_VALUE, Math.ceilDiv(count, (long) size)));
	}

	java.util.List<java.util.Map<java.lang.String, java.lang.Object>> reconcileFindCoursePaymentReconciliations(java.util.UUID actor, java.util.UUID paymentId, java.lang.String key) {
		return db.queryForList("select request_json::text,response_json::text from course_payment_reconciliations " + "where actor_account_id=? and course_payment_id=? and idempotency_key=?", actor, paymentId, key);
	}

	int reconcileInsertCoursePaymentReconciliations(java.util.UUID randomUUID, java.util.UUID actor, java.util.UUID paymentId, java.lang.String key, java.lang.String payload, java.lang.String value6, boolean changed, java.sql.Timestamp nowTimestamp) {
		return db.update("""
			insert into course_payment_reconciliations(id,actor_account_id,course_payment_id,idempotency_key,
			  request_json,response_json,changed_payment,created_at) values(?,?,?,?,?::jsonb,?::jsonb,?,?)
			""", randomUUID, actor, paymentId, key, payload, value6, changed, nowTimestamp);
	}
}
