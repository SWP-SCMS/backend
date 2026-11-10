package com.scms.backend.scheduling.booking;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record ReceptionBookingRequest(@NotNull UUID sessionId) {
}
