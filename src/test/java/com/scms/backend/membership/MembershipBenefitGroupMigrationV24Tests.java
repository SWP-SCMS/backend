package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MembershipBenefitGroupMigrationV24Tests {

	@Container
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

	@Test
	void upgradesExistingCatalogAndEnforcesGroupAndOrderConstraints() {
		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.target("23").load().migrate();
		JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),
			postgres.getUsername(), postgres.getPassword()));

		Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
			.load().migrate();

		assertThat(db.queryForMap("""
			select group_code, display_order from membership_benefits where code='TRAINING_PLAN'
			"""))
			.containsEntry("group_code", "PERSONAL_TRAINING")
			.containsEntry("display_order", 20);
		assertThat(db.queryForMap("""
			select display_name, display_order from membership_benefit_groups where code='TRAINING_TRACKING'
			"""))
			.containsEntry("display_name", "Theo dõi luyện tập")
			.containsEntry("display_order", 70);
		assertThat(db.queryForMap("""
			select group_code, display_order from membership_benefits where code='CENTER_ACCESS'
			"""))
			.containsEntry("group_code", null)
			.containsEntry("display_order", 10);

		assertThatThrownBy(() -> db.update("""
			insert into membership_benefit_groups(code,display_name,display_order)
			values('INVALID','Invalid',-1)
			"""))
			.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> db.update("""
			update membership_benefits set group_code='MISSING' where code='CENTER_ACCESS'
			"""))
			.isInstanceOf(DataIntegrityViolationException.class);
	}
}
