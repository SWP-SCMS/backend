package com.scms.backend.membership.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.math.BigInteger;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/** All fixtures live in connection-private temporary tables; no application rows are changed. */
@Testcontainers
class CenterRevenueRepositoryTests {
    static Connection connection;
    static PostgreSQLContainer postgres;
    JdbcTemplate db;
    CenterRevenueRepository repository;
    UUID member = UUID.randomUUID(), testMember = UUID.randomUUID();
    Instant from = Instant.parse("2026-11-19T17:00:00Z"), to = from.plusSeconds(86400);

    @BeforeAll static void connect() throws Exception {
        String url = System.getenv("REPORT_TEST_DB_URL");
        if (url == null) {
            postgres = new PostgreSQLContainer("postgres:17");
            postgres.start();
            connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        } else {
            connection = DriverManager.getConnection(url, System.getenv("REPORT_TEST_DB_USERNAME"), System.getenv("REPORT_TEST_DB_PASSWORD"));
        }
        connection.setAutoCommit(false);
    }

    @AfterAll static void close() throws Exception {
        if (connection != null) connection.close();
        if (postgres != null) postgres.stop();
    }

    @BeforeEach void setup() {
        db = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
        String[] tables = {"accounts", "membership_orders", "refund_requests", "course_payments", "course_refund_obligations"};
        if (System.getenv("REPORT_TEST_DB_URL") != null) {
            // Validates column names/types against the actual schema, with no constraints or copied rows.
            for (String table : tables) db.execute("create temporary table " + table + " as select * from public." + table + " with no data");
        } else {
            db.execute("create temporary table accounts(id uuid, is_test_data boolean)");
            db.execute("create temporary table membership_orders(id uuid, is_test_data boolean)");
            db.execute("create temporary table refund_requests(order_id uuid, status text, requested_amount numeric, approved_amount numeric)");
            db.execute("create temporary table course_payments(member_account_id uuid, status text, amount numeric, paid_at timestamptz, created_at timestamptz, updated_at timestamptz)");
            db.execute("create temporary table course_refund_obligations(member_account_id uuid, status text, amount_vnd numeric, approved_amount_vnd numeric, verified_amount_vnd numeric, verified_at timestamptz)");
        }
        db.execute("set local search_path to pg_temp");
        db.update("insert into accounts(id,is_test_data) values (?,false),(?,true)", member, testMember);
        repository = new CenterRevenueRepository(db);
    }

    @AfterEach void rollback() throws Exception { connection.rollback(); }

    void payment(UUID account, String status, int amount, Instant paid, Instant created, Instant updated) {
        db.update("insert into course_payments(member_account_id,status,amount,paid_at,created_at,updated_at) values (?,?,?,?,?,?)",
            account, status, amount, stamp(paid), stamp(created), stamp(updated));
    }
    Timestamp stamp(Instant value) { return value == null ? null : Timestamp.from(value); }
    void refund(UUID account, String status, int amount, Integer approved, int verified, Instant at) {
        db.update("insert into course_refund_obligations(member_account_id,status,amount_vnd,approved_amount_vnd,verified_amount_vnd,verified_at) values (?,?,?,?,?,?)",
            account, status, amount, approved, verified, stamp(at));
    }

    @Test void usesPaymentEventDatesHalfOpenRangeAndTestFilter() {
        payment(member, "PAID", 100, from, from.minusSeconds(100), from);
        payment(member, "PAID", 900, to, from, to);
        payment(member, "PENDING", 70, null, from, to);
        payment(member, "FAILED", 80, null, from.minusSeconds(100), from);
        payment(testMember, "PAID", 200, from, from, from);
        refund(member, "COMPLETED", 150, 150, 150, from);
        refund(member, "COMPLETED", 999, 999, 999, to);
        refund(testMember, "COMPLETED", 30, 30, 30, from);
        RevenueSourceResponse report = repository.course(from, to, false);
        assertThat(report.paidPayments()).isOne();
        assertThat(report.paidRevenue()).isEqualTo(BigInteger.valueOf(100));
        assertThat(report.completedRefundAmount()).isEqualTo(BigInteger.valueOf(150));
        assertThat(report.netRevenue()).isEqualTo(BigInteger.valueOf(-50));
        assertThat(report.pendingPayments()).isOne();
        assertThat(report.failedPayments()).isOne();
        assertThat(repository.course(from, to, true).netRevenue()).isEqualTo(BigInteger.valueOf(120));
        assertThat(repository.course(null, null, false).paidRevenue()).isEqualTo(BigInteger.valueOf(1000));
    }

    @Test void pendingRefundsAreCurrentOutstandingAndExcludeTerminalStates() {
        refund(member, "PENDING_REVIEW", 100, null, 0, null);
        refund(member, "APPROVED", 100, 80, 0, null);
        refund(member, "DISBURSEMENT_RECORDED", 100, 70, 0, null);
        refund(member, "PENDING_VERIFICATION", 100, 60, 20, null);
        refund(member, "REJECTED", 999, null, 0, null);
        refund(member, "FAILED", 999, 999, 0, null);
        refund(testMember, "APPROVED", 90, 90, 0, null);
        assertThat(repository.course(to, to.plusSeconds(86400), false).pendingRefundAmount()).isEqualTo(BigInteger.valueOf(290));
        assertThat(repository.course(null, null, true).pendingRefundAmount()).isEqualTo(BigInteger.valueOf(380));
        assertThat(repository.course(null, null, false).completedRefundAmount()).isZero();
    }

    @Test void membershipPendingRefundUsesApprovedAmountAndOrderTestFlag() {
        db.update("insert into membership_orders(id,is_test_data) values (?,false),(?,true)", member, testMember);
        for (String status : new String[]{"REQUESTED","FIRST_APPROVED","CASH_DISBURSED","COMPLETED","REJECTED","CANCELLED"})
            db.update("insert into refund_requests(order_id,status,requested_amount,approved_amount) values (?,?,100,80)", member, status);
        db.update("insert into refund_requests(order_id,status,requested_amount) values (?,'REQUESTED',50)", testMember);
        assertThat(repository.pendingMembershipRefunds(false)).isEqualTo(BigInteger.valueOf(240));
        assertThat(repository.pendingMembershipRefunds(true)).isEqualTo(BigInteger.valueOf(290));
    }

    @Test void emptySourcesAndTotalsRemainExactIncludingNegativeNet() {
        RevenueSourceResponse empty = repository.course(null, null, false);
        assertThat(empty.paidRevenue()).isZero();
        assertThat(empty.netRevenue()).isZero();
        assertThat(repository.pendingMembershipRefunds(false)).isZero();
        var membership = new RevenueSourceResponse(2, BigInteger.valueOf(500), BigInteger.valueOf(600), BigInteger.valueOf(-100), 1, 2, BigInteger.TEN);
        var course = new RevenueSourceResponse(1, BigInteger.valueOf(200), BigInteger.valueOf(50), BigInteger.valueOf(150), 3, 4, BigInteger.ONE);
        assertThat(membership.plus(course)).isEqualTo(new RevenueSourceResponse(3, BigInteger.valueOf(700), BigInteger.valueOf(650), BigInteger.valueOf(50), 4, 6, BigInteger.valueOf(11)));
        assertThat(empty.plus(membership)).isEqualTo(membership);
    }
}
