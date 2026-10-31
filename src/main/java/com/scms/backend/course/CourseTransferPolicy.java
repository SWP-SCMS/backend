package com.scms.backend.course;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;

final class CourseTransferPolicy {
	private CourseTransferPolicy() { }

	static Instant offerExpiresAt(Instant issuedAt, Instant registrationClosesAt, Instant nextSessionAt) {
		Instant expiry = issuedAt.plusSeconds(24 * 60 * 60);
		if (registrationClosesAt.isBefore(expiry)) expiry = registrationClosesAt;
		if (nextSessionAt != null && nextSessionAt.isBefore(expiry)) expiry = nextSessionAt;
		return expiry;
	}

	static BigInteger unusedCredit(List<UnitAllocation> units, Instant now) {
		return units.stream().filter(unit -> !unit.startAt().isBefore(now)).map(UnitAllocation::allocationVnd)
			.reduce(BigInteger.ZERO, BigInteger::add);
	}

	record UnitAllocation(Instant startAt, BigInteger allocationVnd) { }
}
