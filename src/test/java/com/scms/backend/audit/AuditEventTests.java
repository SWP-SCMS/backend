package com.scms.backend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuditEventTests {

	@Test
	void snapshotCopiesPreserveNullValuesAndRemainImmutable() {
		Map<String, Object> before = new LinkedHashMap<>();
		before.put("description", null);
		Map<String, Object> after = new LinkedHashMap<>();
		after.put("description", null);
		AuditEvent event = new AuditEvent(UUID.randomUUID(), UUID.randomUUID(), "UPDATED", "RESOURCE",
			UUID.randomUUID(), null, before, after);

		before.put("description", "changed");
		after.put("description", "changed");

		assertThat(event.getBeforeData()).containsEntry("description", null);
		assertThat(event.getAfterData()).containsEntry("description", null);
		assertThatThrownBy(() -> event.getAfterData().put("description", "changed"))
			.isInstanceOf(UnsupportedOperationException.class);
	}
}
