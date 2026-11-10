package com.scms.backend.course;

import java.time.Instant;

public enum RegistrationStatus {

	UPCOMING, OPEN, CLOSED;

	public static RegistrationStatus at(Instant now, Instant opensAt, Instant closesAt) {
		if (now.isBefore(opensAt))
			return UPCOMING;
		return now.isBefore(closesAt) ? OPEN : CLOSED;
	}
}
