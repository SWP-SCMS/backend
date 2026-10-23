package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MembershipRefundCancellationMigrationContractTests {
	@Test
	void addsCancelledMembershipStatusWithoutEditingEarlierMigrations() throws Exception {
		String migration = Files.readString(Path.of("src/main/resources/db/migration/V39__reserved_membership_cancellation.sql"));
		assertThat(migration).contains("drop constraint chk_memberships_status", "'CANCELLED'", "add constraint chk_memberships_status",
			"drop constraint chk_memberships_time", "status in ('RESERVED', 'CANCELLED')", "starts_at is null and ends_at is null");
	}
}
