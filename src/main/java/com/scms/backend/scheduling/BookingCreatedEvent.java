package com.scms.backend.scheduling;

import java.util.UUID;

record BookingCreatedEvent(UUID bookingId, UUID sessionId, UUID memberId) {
}
