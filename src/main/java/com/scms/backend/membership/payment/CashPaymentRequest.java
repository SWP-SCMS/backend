package com.scms.backend.membership.payment;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CashPaymentRequest(@NotNull UUID offerId) {
}
