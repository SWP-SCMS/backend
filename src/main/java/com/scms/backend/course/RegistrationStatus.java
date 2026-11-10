package com.scms.backend.course;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public enum RegistrationStatus {

	UPCOMING, OPEN, CLOSED;

	public static RegistrationStatus at(Instant now, Instant opensAt, Instant closesAt) {
		if (now.isBefore(opensAt))
			return UPCOMING;
		return now.isBefore(closesAt) ? OPEN : CLOSED;
	}
}
