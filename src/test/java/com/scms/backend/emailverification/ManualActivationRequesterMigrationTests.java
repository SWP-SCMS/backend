package com.scms.backend.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class ManualActivationRequesterMigrationTests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void v46BackfillsImmutableRequesterIdentityForExistingRequests() throws Exception {
		Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("45")
			.load()
			.migrate();

		UUID requesterId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		try (Connection connection = connection()) {
			insertAccount(connection, requesterId, "RECEPTIONIST", "Original Receptionist",
				"receptionist@example.test", "0900000101");
			insertAccount(connection, memberId, "MEMBER", "Pending Member",
				"member@example.test", "0900000102");
			insertRequest(connection, requestId, memberId, requesterId);
		}

		Flyway flyway = Flyway.configure()
			.dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load();
		flyway.migrate();
		flyway.validate();

		try (Connection connection = connection()) {
			assertThat(snapshot(connection, requestId))
				.containsExactly("Original Receptionist", "RECEPTIONIST");
			assertThat(nullable(connection, "requested_by_name_snapshot")).isEqualTo("NO");
			assertThat(nullable(connection, "requested_by_role_snapshot")).isEqualTo("NO");
			try (PreparedStatement statement = connection.prepareStatement(
					"update accounts set full_name='Renamed Receptionist',status='INACTIVE' where id=?")) {
				statement.setObject(1, requesterId);
				statement.executeUpdate();
			}
			assertThat(snapshot(connection, requestId))
				.containsExactly("Original Receptionist", "RECEPTIONIST");
		}
	}

	private Connection connection() throws SQLException {
		return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
	}

	private void insertAccount(Connection connection, UUID id, String role, String name, String email,
			String phone) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			insert into accounts (id,role,status,full_name,phone,email,birth_date,password_hash)
			values (?,?,'ACTIVE',?,?,?,date '1990-01-01','unused-hash')
			""")) {
			statement.setObject(1, id);
			statement.setString(2, role);
			statement.setString(3, name);
			statement.setString(4, phone);
			statement.setString(5, email);
			statement.executeUpdate();
		}
	}

	private void insertRequest(Connection connection, UUID requestId, UUID memberId, UUID requesterId)
			throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			insert into email_verification_manual_activation_requests
				(id,account_id,requested_by_account_id,reason_code,reason_detail,verification_method,
				 evidence_note,status,requested_at,expires_at)
			values (?,?,?,'OTHER','Migration proof','OTHER_APPROVED_PROCEDURE','Verified','PENDING',?,?)
			""")) {
			Instant now = Instant.parse("2026-10-25T00:00:00Z");
			statement.setObject(1, requestId);
			statement.setObject(2, memberId);
			statement.setObject(3, requesterId);
			statement.setTimestamp(4, Timestamp.from(now));
			statement.setTimestamp(5, Timestamp.from(now.plusSeconds(3600)));
			statement.executeUpdate();
		}
	}

	private java.util.List<String> snapshot(Connection connection, UUID requestId) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			select requested_by_name_snapshot,requested_by_role_snapshot
			from email_verification_manual_activation_requests where id=?
			""")) {
			statement.setObject(1, requestId);
			try (var resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).isTrue();
				return java.util.List.of(resultSet.getString(1), resultSet.getString(2));
			}
		}
	}

	private String nullable(Connection connection, String column) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("""
			select is_nullable from information_schema.columns
			where table_schema='public' and table_name='email_verification_manual_activation_requests'
			  and column_name=?
			""")) {
			statement.setString(1, column);
			try (var resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).isTrue();
				return resultSet.getString(1);
			}
		}
	}
}
