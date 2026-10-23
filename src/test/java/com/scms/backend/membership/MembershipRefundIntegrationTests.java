package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import com.scms.backend.ScmsBackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest(classes = ScmsBackendApplication.class)
@ActiveProfiles("test")
class MembershipRefundIntegrationTests {
	@Container @ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");
	@Autowired JdbcTemplate jdbc;
	@Autowired MembershipRefundService refunds;

	@Test
	void refundLedgerMigrationInstallsExpectedTablesIndexesAndImmutableCreditNoteGuard() {
		assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='public' and table_name in ('refund_requests','credit_notes')", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select pg_get_expr(i.indpred, i.indrelid) from pg_index i join pg_class c on c.oid=i.indexrelid where c.relname='uq_refund_requests_nonrejected_payment'", String.class))
			.contains("REJECTED", "CANCELLED").doesNotContain("COMPLETED");
		assertThat(jdbc.queryForObject("select exists(select 1 from pg_trigger where tgname='trg_credit_notes_immutable' and not tgisinternal)", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("select version from flyway_schema_history where version='38' and success", String.class)).isEqualTo("38");
	}

	@Test
	void finalApprovalWritesOneCreditNoteAndOneEventAndRetryCannotDuplicateThem() {
		UUID manager = account("MANAGER");
		UUID receptionist = account("RECEPTIONIST");
		UUID member = account("MEMBER");
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID membership = UUID.randomUUID();
		UUID receipt = UUID.randomUUID();
		UUID refund = UUID.randomUUID();
		Instant now = Instant.now();
		jdbc.update("insert into member_profiles(account_id) values(?)", member);
		jdbc.update("insert into membership_offers(id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id) values(?,'BASIC','Refund test','Refund test',100000,'VND',30,'ACTIVE',?)",
			offer, manager);
		jdbc.update("insert into membership_orders(id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,payment_method,status,paid_at,fulfillment_mode) values(?,?,?, ?,?,'Refund test','BASIC',100000,'VND',30,'CASH','PAID',?,'IMMEDIATE')",
			order, "RF-" + order, member, manager, offer, Timestamp.from(now));
		jdbc.update("insert into payments(id,order_id,method,status,amount,currency_code,paid_at) values(?,?,'CASH','PAID',100000,'VND',?)",
			payment, order, Timestamp.from(now));
		jdbc.update("insert into memberships(id,member_account_id,order_id,offer_id,plan_code_snapshot,offer_name_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,status,starts_at,ends_at) values(?,?,?,?,'BASIC','Refund test',100000,'VND',30,'TERMINATED',?,?)",
			membership, member, order, offer, Timestamp.from(now.minusSeconds(30L * 86400)), Timestamp.from(now.minusSeconds(86400)));
		jdbc.update("insert into receipts(id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,currency_code_snapshot,payment_method_snapshot) values(?,?,?,?,?,100000,'VND','CASH')",
			receipt, "RC-" + receipt, payment, order, member);
		jdbc.update("insert into refund_requests(id,payment_id,order_id,membership_id,receipt_id,member_account_id,requester_account_id,refund_type,status,requested_amount,suggested_amount,approved_amount,remaining_days_snapshot,calculation_date,reason,disbursement_evidence,idempotency_key,cash_disbursed_at) values(?,?,?,?,?,?,?,'TERMINATED_MEMBERSHIP','CASH_DISBURSED',1000,1000,1000,1,current_date,'test refund','cash receipt','integration-key',?)",
			refund, payment, order, membership, receipt, member, receptionist, Timestamp.from(now));

		refunds.finalApproval(manager, refund, new RefundActionRequest(0L, null));

		assertThat(jdbc.queryForObject("select status from refund_requests where id=?", String.class, refund)).isEqualTo("COMPLETED");
		assertThat(jdbc.queryForObject("select count(*) from credit_notes where refund_request_id=?", Long.class, refund)).isOne();
		assertThat(jdbc.queryForObject("select count(*) from audit_events where target_id=? and action='MEMBERSHIP_REFUND_COMPLETED'", Long.class, refund)).isOne();
		assertThat(jdbc.queryForObject("select count(*) from notification_outbox where event_key=?", Long.class,
			"MEMBERSHIP_REFUND_COMPLETED:" + refund)).isOne();
		assertThatThrownBy(() -> refunds.finalApproval(manager, refund, new RefundActionRequest(0L, null)))
			.isInstanceOf(RefundException.class).extracting("code").isEqualTo("REFUND_REQUEST_CHANGED");
		assertThat(jdbc.queryForObject("select count(*) from credit_notes where refund_request_id=?", Long.class, refund)).isOne();
		assertThatThrownBy(() -> jdbc.update("insert into refund_requests(id,payment_id,order_id,membership_id,receipt_id,member_account_id,requester_account_id,refund_type,status,requested_amount,reason,idempotency_key) values(?,?,?,?,?,?,?,'TERMINATED_MEMBERSHIP','REQUESTED',1,'duplicate','second-key')",
			UUID.randomUUID(), payment, order, membership, receipt, member, receptionist))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private UUID account(String role) {
		UUID id = UUID.randomUUID();
		jdbc.update("insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash) values(?,?,'ACTIVE',?,?,?,date '1990-01-01','test')",
			id, role, "Refund " + role, "09" + id.toString().replace("-", "").substring(0, 9), id + "@refund.test");
		return id;
	}
}
