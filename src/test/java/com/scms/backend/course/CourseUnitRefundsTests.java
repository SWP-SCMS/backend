package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CourseUnitRefundsTests {
    private static final Instant NOW = Instant.parse("2035-01-01T00:00:00Z");

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
        var units = List.of(missed,
            unit("ENROLLED", "SCHEDULED", NOW.minusSeconds(3600), 100000),
            unit("ENROLLED", "MAKEUP_SCHEDULED", NOW, 100000),
            unit("ENROLLED", "RESOLVED_BY_REFUND", NOW.plusSeconds(3600), 100000),
            unit("NOT_ENROLLED", "SCHEDULED", NOW.plusSeconds(3600), 100000));
        assertThat(CourseUnitRefunds.eligible(units, NOW, "COURSE_RUN_CANCELLED")).containsExactly(missed);
        assertThat(total(CourseUnitRefunds.eligible(units, NOW, "UNRESOLVED_SESSIONS"))).isEqualTo(BigInteger.valueOf(100001));
    }

    @Test
    void removedUnitsRemainRefundableAfterTheirFormerDateWithoutRepricingRemainders() {
        var units = List.of(unit("REMOVED_BY_RUN_CHANGE", "REMOVED_BY_RUN_CHANGE", NOW.minusSeconds(3600), 100001),
            unit("REMOVED_BY_RUN_CHANGE", "REMOVED_BY_RUN_CHANGE", NOW.minusSeconds(3600), 100000));
        assertThat(total(CourseUnitRefunds.eligible(units, NOW, "REDUCED_SESSION_COUNT"))).isEqualTo(BigInteger.valueOf(200001));
    }

    private static BigInteger total(List<CourseUnitRefunds.Unit> units) {
        return units.stream().map(CourseUnitRefunds.Unit::allocation).reduce(BigInteger.ZERO, BigInteger::add);
    }
    private static CourseUnitRefunds.Unit unit(String disposition, String status, Instant start, long allocation) {
        return new CourseUnitRefunds.Unit(UUID.randomUUID(), BigInteger.valueOf(allocation), disposition, status, start);
    }
}
