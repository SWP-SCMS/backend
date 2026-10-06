package com.scms.backend.scheduling;

import java.util.UUID;

record BookingCancelledEvent(UUID bookingId, UUID sessionId, UUID memberId) {
}
