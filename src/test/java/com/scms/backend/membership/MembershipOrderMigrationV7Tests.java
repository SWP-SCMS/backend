package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MembershipOrderMigrationV7Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void v7PreservesOldDeadlineAllowsNullDeadlineAndKeepsV5UniqueIndex() throws Exception {
		Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("6")
			.load()
			.migrate();

		UUID actorId = UUID.randomUUID();
		UUID oldMemberId = UUID.randomUUID();
		UUID newMemberId = UUID.randomUUID();
		UUID offerId = UUID.randomUUID();
		UUID oldOrderId = UUID.randomUUID();
		Instant oldDeadline = Instant.parse("2026-10-02T01:23:45Z");
		try (Connection connection = connection()) {
			insertAccount(connection, actorId, "RECEPTIONIST", "reception@example.test", "0900000001");
			insertAccount(connection, oldMemberId, "MEMBER", "old@example.test", "0900000002");
			insertAccount(connection, newMemberId, "MEMBER", "new@example.test", "0900000003");
			insertOffer(connection, offerId, actorId);
			insertOrder(connection, oldOrderId, oldMemberId, actorId, offerId, oldDeadline);
		}

		Flyway flyway = Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load();
		flyway.migrate();
		flyway.validate();

		try (Connection connection = connection()) {
			assertThat(versions(connection)).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13");
			assertThat(queryTimestamp(connection,
				"select expires_at from membership_orders where id = ?", oldOrderId)).isEqualTo(oldDeadline);
			assertThat(queryString(connection, """
				select is_nullable from information_schema.columns
				where table_schema = 'public' and table_name = 'membership_orders' and column_name = 'expires_at'
				""")).isEqualTo("YES");

			UUID newOrderId = UUID.randomUUID();
			insertOrder(connection, newOrderId, newMemberId, actorId, offerId, null);
			assertThat(queryTimestamp(connection,
				"select expires_at from membership_orders where id = ?", newOrderId)).isNull();

			assertThatThrownBy(() -> insertOrder(connection, UUID.randomUUID(), newMemberId, actorId, offerId, null))
				.isInstanceOf(SQLException.class)
				.hasMessageContaining(MembershipOrderConstraintDetector.PENDING_MEMBER_CONSTRAINT);
		}
	}

	private Connection connection() throws SQLException {
		return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

	private void insertAccount(Connection connection, UUID id, String role, String email, String phone)
			throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			insert into accounts (id, role, status, full_name, phone, email, birth_date, password_hash)
			values (?, ?, 'ACTIVE', 'Migration Test', ?, ?, date '1990-01-01', 'unused-hash')
			""")) {
			statement.setObject(1, id);
			statement.setString(2, role);
			statement.setString(3, phone);
			statement.setString(4, email);
			statement.executeUpdate();
		}
	}

	private void insertOffer(Connection connection, UUID offerId, UUID actorId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			insert into membership_offers
			    (id, plan_code, name, description, price_amount, currency_code, duration_days, status,
			     created_by_account_id)
			values (?, 'BASIC', 'Migration Offer', 'Migration proof', 500000, 'VND', 30, 'ACTIVE', ?)
			""")) {
			statement.setObject(1, offerId);
			statement.setObject(2, actorId);
			statement.executeUpdate();
		}
	}

	private void insertOrder(Connection connection, UUID orderId, UUID memberId, UUID actorId, UUID offerId,
			Instant expiresAt) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			insert into membership_orders
			    (id, order_number, member_account_id, created_by_account_id, offer_id, offer_name_snapshot,
			     plan_code_snapshot, price_amount_snapshot, currency_code_snapshot, duration_days_snapshot,
			     payment_method, status, expires_at, paid_at)
			values (?, ?, ?, ?, ?, 'Migration Offer', 'BASIC', 500000, 'VND', 30,
			        'BANK_TRANSFER', 'PENDING_PAYMENT', ?, null)
			""")) {
			statement.setObject(1, orderId);
			statement.setString(2, "ORD-" + orderId.toString().replace("-", "").toUpperCase());
			statement.setObject(3, memberId);
			statement.setObject(4, actorId);
			statement.setObject(5, offerId);
			if (expiresAt == null) {
				statement.setTimestamp(6, null);
			}
			else {
				statement.setTimestamp(6, Timestamp.from(expiresAt));
			}
			statement.executeUpdate();
		}
	}

	private List<String> versions(Connection connection) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
				"select version from flyway_schema_history where success order by installed_rank");
				var resultSet = statement.executeQuery()) {
			var versions = new java.util.ArrayList<String>();
			while (resultSet.next()) {
				versions.add(resultSet.getString(1));
			}
			return versions;
		}
	}

	private Instant queryTimestamp(Connection connection, String sql, UUID id) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setObject(1, id);
			try (var resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).isTrue();
				Timestamp value = resultSet.getTimestamp(1);
				return value == null ? null : value.toInstant();
			}
		}
	}

	private String queryString(Connection connection, String sql) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(sql);
				var resultSet = statement.executeQuery()) {
			assertThat(resultSet.next()).isTrue();
			return resultSet.getString(1);
		}
	}
}
