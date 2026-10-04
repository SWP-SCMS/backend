package com.scms.backend.membership;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record CashPaymentRequest(@NotNull UUID offerId) { }
