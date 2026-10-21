package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;

class MembershipBenefitGroupFoundationTests {

	@Test
	void migrationCreatesGroupsAndBackfillsDisplayMetadata() throws Exception {
		try (var stream = getClass().getResourceAsStream("/db/migration/V24__membership_benefit_groups.sql")) {
			assertThat(stream).isNotNull();
			String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			assertThat(sql).contains("create table membership_benefit_groups")
				.contains("group_code varchar(50)")
				.contains("display_order integer")
				.contains("PERSONAL_TRAINING")
				.contains("TRAINING_TRACKING")
				.contains("PERSONAL_COACHING")
				.contains("TRAINING_PLAN")
				.contains("TRAINING_RESULT")
				.contains("TRAINING_FEEDBACK")
				.contains("check (display_order >= 0)");
		}
	}

	@Test
	void benefitGroupAndBenefitExposeReadOnlyDisplayMetadata() throws Exception {
		Class<?> groupType;
		try {
			groupType = Class.forName("com.scms.backend.membership.MembershipBenefitGroup");
		}
		catch (ClassNotFoundException exception) {
			throw new AssertionError("MembershipBenefitGroup must be mapped", exception);
		}
		assertThat(groupType.getAnnotation(Table.class).name()).isEqualTo("membership_benefit_groups");
		assertThat(Arrays.stream(groupType.getDeclaredMethods()).map(method -> method.getName()))
			.contains("getCode", "getDisplayName", "getDescription", "getDisplayOrder");

		assertThat(MembershipBenefit.class.getDeclaredField("group").getAnnotation(JoinColumn.class).name())
			.isEqualTo("group_code");
		assertThat(MembershipBenefit.class.getDeclaredMethod("getGroup")).isNotNull();
		assertThat(MembershipBenefit.class.getDeclaredMethod("getDisplayOrder")).isNotNull();
	}
}
