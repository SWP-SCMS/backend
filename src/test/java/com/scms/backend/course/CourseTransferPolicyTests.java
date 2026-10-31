package com.scms.backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class CourseTransferPolicyTests {
	@Test
	void expiryUsesTheEarliestBusinessBoundary() {
		Instant issued = Instant.parse("2035-01-01T00:00:00Z");
		assertThat(CourseTransferPolicy.offerExpiresAt(issued,
			Instant.parse("2035-01-03T00:00:00Z"), Instant.parse("2035-01-01T12:00:00Z")))
			.isEqualTo(Instant.parse("2035-01-01T12:00:00Z"));
	}

	@Test
	void creditOnlyIncludesUnitsThatHaveNotStarted() {
		Instant now = Instant.parse("2035-01-01T00:00:00Z");
		assertThat(CourseTransferPolicy.unusedCredit(List.of(
			new CourseTransferPolicy.UnitAllocation(now.minusSeconds(1), BigInteger.valueOf(31)),
			new CourseTransferPolicy.UnitAllocation(now, BigInteger.valueOf(32)),
			new CourseTransferPolicy.UnitAllocation(now.plusSeconds(1), BigInteger.valueOf(33))), now))
			.isEqualTo(BigInteger.valueOf(65));
	}
}
