package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class MembershipBenefitMigrationTests {
	@Test
	void migrationCreatesCatalogRelationshipsAndPreservesLegacyPlans() throws Exception {
		try (var stream = getClass().getResourceAsStream("/db/migration/V19__membership_benefit_catalog.sql")) {
			assertThat(stream).isNotNull();
			String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(sql).contains("create table membership_benefits")
				.contains("create table membership_plan_benefits")
				.contains("create table discipline_required_benefits")
				.contains("drop constraint if exists chk_membership_plans_code")
				.contains("CENTER_ACCESS")
				.contains("CLASS_BOOKING")
				.contains("insert into membership_plan_benefits")
				.contains("on conflict do nothing");
		}
	}

	@Test
	void bookingEligibilityMigrationUsesBenefitCatalogAndKeepsExistingTrigger() throws Exception {
		try (var stream = getClass().getResourceAsStream("/db/migration/V20__booking_benefit_eligibility.sql")) {
			assertThat(stream).isNotNull();
			String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
			assertThat(sql).contains("create or replace function validate_booked_booking()")
				.contains("membership_plan_benefits")
				.contains("discipline_required_benefits")
				.contains("class_booking")
				.doesNotContain("plan_code_snapshot <> 'plus'")
				.doesNotContain("drop trigger");
		}
	}
}
