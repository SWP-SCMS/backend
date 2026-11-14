package com.scms.backend.course.refund;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseUnitRefundsTests {

	private static final Instant NOW = Instant.parse("2035-01-01T00:00:00Z");

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "COMPLETED", "SCHEDULED" })
	void centerCancellationAfterOneHeldSessionRefundsAllNineRemainingUnitsAtFullAllocation(String heldStatus) {
		var units = new ArrayList<CourseUnitRefunds.Unit>();
		var held = unit("ENROLLED", heldStatus, NOW.minusSeconds(86400), 100000);
		units.add(held);
		for (int i = 1; i <= 9; i++)
			units.add(unit("ENROLLED", "SCHEDULED", NOW.plusSeconds(i * 86400L), 100000));
		var refund = CourseUnitRefunds.eligible(units, NOW, "COURSE_RUN_CANCELLED");
		assertThat(refund).hasSize(9).doesNotContain(held);
		assertThat(total(refund)).isEqualTo(BigInteger.valueOf(900000));
	}

	@Test
	void centerCancellationPreservesOriginalUnevenAllocationsInsteadOfRepricingRemainingUnits() {
		var held = unit("ENROLLED", "COMPLETED", NOW.minusSeconds(86400), 333334);
		var units = List.of(held,
			unit("ENROLLED", "SCHEDULED", NOW.plusSeconds(86400), 333333),
			unit("ENROLLED", "SCHEDULED", NOW.plusSeconds(2 * 86400), 333333));
		assertThat(total(CourseUnitRefunds.eligible(units, NOW, "COURSE_RUN_CANCELLED")))
			.isEqualTo(BigInteger.valueOf(666666));
	}

	@Test
	void tenUnitsReducedToEightThenOneDeliveredRefundsTwoPlusSevenOriginalAllocations() {
		var units = new ArrayList<CourseUnitRefunds.Unit>();
		units.add(unit("ENROLLED", "COMPLETED", NOW.minusSeconds(3600), 100000));
		for (int i = 0; i < 7; i++) units.add(unit("ENROLLED", "SCHEDULED", NOW.plusSeconds(3600), 100000));
		for (int i = 0; i < 2; i++) units.add(unit("REMOVED_BY_RUN_CHANGE", "REMOVED_BY_RUN_CHANGE", NOW.plusSeconds(3600), 100000));
		var reduced = CourseUnitRefunds.eligible(units, NOW, "REDUCED_SESSION_COUNT");
		var cancelled = CourseUnitRefunds.eligible(units, NOW, "COURSE_RUN_CANCELLED");
		assertThat(total(reduced)).isEqualTo(BigInteger.valueOf(200000));
		assertThat(total(cancelled)).isEqualTo(BigInteger.valueOf(700000));
		assertThat(cancelled).doesNotContainAnyElementsOf(reduced);
		assertThat(total(reduced).add(total(cancelled))).isEqualTo(BigInteger.valueOf(900000));
	}

	@Test
	void pastCancelledSessionIsRefundableButHeldSessionAndAlreadyRefundedSessionAreNot() {
		var missed = unit("UNRESOLVED", "CANCELLED_PENDING_MAKEUP", NOW.minusSeconds(3600), 100001);
		var units = List.of(missed, unit("ENROLLED", "SCHEDULED", NOW.minusSeconds(3600), 100000), unit("ENROLLED", "MAKEUP_SCHEDULED", NOW, 100000), unit("ENROLLED", "RESOLVED_BY_REFUND", NOW.plusSeconds(3600), 100000), unit("NOT_ENROLLED", "SCHEDULED", NOW.plusSeconds(3600), 100000));
		assertThat(CourseUnitRefunds.eligible(units, NOW, "COURSE_RUN_CANCELLED")).containsExactly(missed);
		assertThat(total(CourseUnitRefunds.eligible(units, NOW, "UNRESOLVED_SESSIONS"))).isEqualTo(BigInteger.valueOf(100001));
	}

	@Test
	void removedUnitsRemainRefundableAfterTheirFormerDateWithoutRepricingRemainders() {
		var units = List.of(unit("REMOVED_BY_RUN_CHANGE", "REMOVED_BY_RUN_CHANGE", NOW.minusSeconds(3600), 100001), unit("REMOVED_BY_RUN_CHANGE", "REMOVED_BY_RUN_CHANGE", NOW.minusSeconds(3600), 100000));
		assertThat(total(CourseUnitRefunds.eligible(units, NOW, "REDUCED_SESSION_COUNT"))).isEqualTo(BigInteger.valueOf(200001));
	}

	private static BigInteger total(List<CourseUnitRefunds.Unit> units) {
		return units.stream().map(CourseUnitRefunds.Unit::allocation).reduce(BigInteger.ZERO, BigInteger::add);
	}

	private static CourseUnitRefunds.Unit unit(String disposition, String status, Instant start, long allocation) {
		return new CourseUnitRefunds.Unit(UUID.randomUUID(), BigInteger.valueOf(allocation), disposition, status, start);
	}
}
