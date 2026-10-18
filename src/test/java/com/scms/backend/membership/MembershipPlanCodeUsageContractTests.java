package com.scms.backend.membership;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class MembershipPlanCodeUsageContractTests {

	@Test
	void offerAndOrderProductionSourcesDoNotCallLegacyEnumConversion() throws Exception {
		for (String source : new String[] {
			"src/main/java/com/scms/backend/membership/MembershipOfferResponse.java",
			"src/main/java/com/scms/backend/membership/MembershipOrder.java"
		}) {
			assertThat(Files.readString(Path.of(source))).doesNotContain("MembershipPlanCode.valueOf");
		}
	}
}
