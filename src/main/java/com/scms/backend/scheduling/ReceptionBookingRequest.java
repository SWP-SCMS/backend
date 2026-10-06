package com.scms.backend.scheduling;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

record ReceptionBookingRequest(@NotNull UUID sessionId) {
}
