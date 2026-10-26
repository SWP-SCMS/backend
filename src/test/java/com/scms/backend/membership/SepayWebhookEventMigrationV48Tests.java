package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class SepayWebhookEventMigrationV48Tests {

	private static final List<String> OUTCOMES = List.of(
		"FULFILLED", "ALREADY_PROCESSED", "PAYMENT_WINDOW_EXPIRED", "DESTINATION_SNAPSHOT_MISSING",
		"REFERENCE_MISMATCH", "AMOUNT_MISMATCH", "ACCOUNT_MISMATCH", "PAYMENT_NOT_PAYABLE");

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@BeforeEach
	void cleanDatabase() {
		flyway().clean();
	}

	@Test
	void freshDatabaseCreatesConstrainedImmutableProviderEventLedger() {
		flyway().migrate();
		JdbcTemplate db = db();

		assertThat(db.query("""
			select attribute.attname,
			       format_type(attribute.atttypid, attribute.atttypmod) as data_type,
			       attribute.attnotnull,
			       definition.adbin is not null as has_default
			from pg_attribute attribute
			left join pg_attrdef definition
			  on definition.adrelid = attribute.attrelid and definition.adnum = attribute.attnum
			where attribute.attrelid = 'payment_provider_events'::regclass
			  and attribute.attnum > 0 and not attribute.attisdropped
			order by attribute.attnum
			""", (result, row) -> new Column(
			result.getString("attname"), result.getString("data_type"),
			result.getBoolean("attnotnull"), result.getBoolean("has_default"))))
			.containsExactly(
				new Column("id", "uuid", true, false),
				new Column("provider", "character varying(20)", true, false),
				new Column("provider_transaction_id", "character varying(100)", true, false),
				new Column("payment_id", "uuid", false, false),
				new Column("transfer_amount", "numeric(19,0)", true, false),
				new Column("payment_reference", "character varying(255)", false, false),
				new Column("account_number_last4", "character varying(4)", false, false),
				new Column("payload_sha256", "character(64)", true, false),
				new Column("outcome", "character varying(50)", true, false),
				new Column("detail", "character varying(500)", true, false),
				new Column("received_at", "timestamp with time zone", true, true));

		assertThat(db.queryForObject("""
			select indexdef from pg_indexes
			where schemaname='public' and tablename='payment_provider_events'
			  and indexdef ilike '%(payment_id, received_at desc)%'
			""", String.class)).contains("payment_id", "received_at DESC");
		assertThat(db.queryForObject("""
			select proc.proname
			from pg_trigger event_trigger
			join pg_proc proc on proc.oid = event_trigger.tgfoid
			where event_trigger.tgrelid='payment_provider_events'::regclass
			  and not event_trigger.tgisinternal
			""", String.class)).isEqualTo("prevent_history_mutation");

		UUID eventId = UUID.randomUUID();
		insertEvent(db, eventId, "9001", null, OUTCOMES.get(0));
		for (String outcome : OUTCOMES.subList(1, OUTCOMES.size())) {
			insertEvent(db, UUID.randomUUID(), outcome, null, outcome);
		}

		assertThatThrownBy(() -> insertEvent(db, eventId, "other-id", null, OUTCOMES.get(0)))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertEvent(db, UUID.randomUUID(), "9001", null, OUTCOMES.get(0)))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertEvent(db, UUID.randomUUID(), "invalid-outcome", null, "INVALID"))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> insertEvent(db, UUID.randomUUID(), "missing-payment", UUID.randomUUID(),
			OUTCOMES.get(0))).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("update payment_provider_events set detail='changed' where id=?", eventId))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("payment_provider_events is append-only");
		assertThatThrownBy(() -> db.update("delete from payment_provider_events where id=?", eventId))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("payment_provider_events is append-only");
	}

	@Test
	void v48UpgradePreservesLegacyPaymentOrderAndReceiptRows() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("47").load().migrate();
		JdbcTemplate db = db();
		LegacyRows legacy = seedLegacyCommerce(db);
		List<String> before = snapshots(db, legacy);

		flyway().migrate();

		assertThat(snapshots(db, legacy)).containsExactlyElementsOf(before);
		assertThat(db.queryForObject("select count(*) from payment_provider_events", Long.class)).isZero();
		assertThat(db.queryForObject(
			"select count(*) from flyway_schema_history where version='48' and success", Long.class)).isOne();
	}

	private Flyway flyway() {
		return Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.cleanDisabled(false)
			.load();
	}

	private JdbcTemplate db() {
		return new JdbcTemplate(new DriverManagerDataSource(
			postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
	}

	private void insertEvent(JdbcTemplate db, UUID id, String transactionId, UUID paymentId, String outcome) {
		db.update("""
			insert into payment_provider_events
			    (id, provider, provider_transaction_id, payment_id, transfer_amount, payment_reference,
			     account_number_last4, payload_sha256, outcome, detail)
			values (?, 'SEPAY', ?, ?, 500000, 'SCMS-ORDER', '6789', ?, ?, 'Migration test')
			""", id, transactionId, paymentId, "a".repeat(64), outcome);
	}

	private LegacyRows seedLegacyCommerce(JdbcTemplate db) {
		UUID manager = UUID.randomUUID();
		UUID member = UUID.randomUUID();
		UUID offer = UUID.randomUUID();
		UUID order = UUID.randomUUID();
		UUID payment = UUID.randomUUID();
		UUID receipt = UUID.randomUUID();
		Timestamp paidAt = Timestamp.from(Instant.parse("2026-10-01T02:03:04Z"));
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?, 'MANAGER', 'ACTIVE', 'V48 Manager', '0900000048', 'manager-v48@example.test',
			       date '1990-01-01', 'unused')
			""", manager);
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?, 'MEMBER', 'ACTIVE', 'V48 Member', '0900000049', 'member-v48@example.test',
			       date '1990-01-01', 'unused')
			""", member);
		db.update("""
			insert into membership_offers
			    (id,plan_code,name,description,price_amount,currency_code,duration_days,status,created_by_account_id)
			values(?,'BASIC','V48 Offer','Migration test',500000,'VND',30,'ACTIVE',?)
			""", offer, manager);
		db.update("""
			insert into membership_orders
			    (id,order_number,member_account_id,created_by_account_id,offer_id,offer_name_snapshot,
			     plan_code_snapshot,price_amount_snapshot,currency_code_snapshot,duration_days_snapshot,
			     payment_method,status,expires_at,paid_at)
			values(?,?,?, ?,?,'V48 Offer','BASIC',500000,'VND',30,'BANK_TRANSFER','PAID',?,?)
			""", order, "ORD-" + order, member, manager, offer, paidAt, paidAt);
		db.update("""
			insert into payments
			    (id,order_id,method,status,amount,currency_code,bank_transfer_content,provider_reference,
			     provider,provider_transaction_id,bank_code_snapshot,bank_account_number_snapshot,
			     bank_account_name_snapshot,paid_at)
			values(?,?,'BANK_TRANSFER','PAID',500000,'VND','SCMS-V48','SCMS-V48','SEPAY','legacy-v48',
			       'MB','123456789','SCMS GYM',?)
			""", payment, order, paidAt);
		db.update("""
			insert into receipts
			    (id,receipt_number,payment_id,order_id,member_account_id,amount_snapshot,
			     currency_code_snapshot,payment_method_snapshot,issued_at)
			values(?,?,?,?,?,500000,'VND','BANK_TRANSFER',?)
			""", receipt, "RC-" + receipt, payment, order, member, paidAt);
		return new LegacyRows(order, payment, receipt);
	}

	private List<String> snapshots(JdbcTemplate db, LegacyRows rows) {
		return List.of(
			snapshot(db, "membership_orders", rows.orderId()),
			snapshot(db, "payments", rows.paymentId()),
			snapshot(db, "receipts", rows.receiptId()));
	}

	private String snapshot(JdbcTemplate db, String table, UUID id) {
		return db.queryForObject("select to_jsonb(snapshot)::text from (select * from " + table
			+ " where id=?) snapshot", String.class, id);
	}

	private record Column(String name, String type, boolean notNull, boolean hasDefault) { }

	private record LegacyRows(UUID orderId, UUID paymentId, UUID receiptId) { }
}
