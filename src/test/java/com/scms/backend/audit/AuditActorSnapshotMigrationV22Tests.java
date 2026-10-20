package com.scms.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class AuditActorSnapshotMigrationV22Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void backfillsExistingEventsAndSnapshotsNewEventsWhileKeepingHistoryAppendOnly() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("21").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));
		UUID actorId = UUID.randomUUID();
		UUID existingEventId = UUID.randomUUID();
		db.update("""
			insert into accounts(id,role,status,full_name,phone,email,birth_date,password_hash)
			values(?,'RECEPTIONIST','ACTIVE','Original Actor','0900000022',?,date '1990-01-01','unused')
			""", actorId, actorId + "@example.test");
		db.update("""
			insert into audit_events(id,actor_account_id,action,target_type,target_id,after_data)
			values(?,?,'ACCOUNT_UPDATED','ACCOUNT',?,'{}')
			""", existingEventId, actorId, UUID.randomUUID());

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForMap("""
			select actor_name_snapshot,actor_role_snapshot from audit_events where id=?
			""", existingEventId))
			.containsEntry("actor_name_snapshot", "Original Actor")
			.containsEntry("actor_role_snapshot", "RECEPTIONIST");

		db.update("update accounts set full_name='Renamed Actor' where id=?", actorId);
		UUID newEventId = UUID.randomUUID();
		db.update("""
			insert into audit_events(id,actor_account_id,action,target_type,target_id,after_data)
			values(?,?,'ACCOUNT_UPDATED','ACCOUNT',?,'{}')
			""", newEventId, actorId, UUID.randomUUID());
		assertThat(db.queryForMap("""
			select actor_name_snapshot,actor_role_snapshot from audit_events where id=?
			""", newEventId))
			.containsEntry("actor_name_snapshot", "Renamed Actor")
			.containsEntry("actor_role_snapshot", "RECEPTIONIST");

		UUID systemEventId = UUID.randomUUID();
		db.update("""
			insert into audit_events(id,action,target_type,target_id,after_data)
			values(?,'SYSTEM_ACTION','ACCOUNT',?,'{}')
			""", systemEventId, UUID.randomUUID());
		assertThat(db.queryForObject("select actor_name_snapshot is null and actor_role_snapshot is null "
			+ "from audit_events where id=?", Boolean.class, systemEventId)).isTrue();

		assertThatThrownBy(() -> db.update("delete from audit_events where id=?", newEventId))
			.isInstanceOf(UncategorizedSQLException.class)
			.hasMessageContaining("audit_events is append-only");
	}
}
