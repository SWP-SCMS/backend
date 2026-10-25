package com.scms.backend.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface BookingCancellationRepository {
	List<UUID> cancelFutureBookingsForMembershipTermination(UUID memberId, UUID actorId, Instant now);

	List<UUID> cancelFutureBookingsForMembershipFreeze(UUID memberId, UUID actorId, Instant startAt,
		Instant endAt, Instant now);
}
