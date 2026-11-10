package com.scms.backend.membership.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigInteger;

public record SepayWebhookRequest(@NotNull Long id, String code, @NotBlank String content, @NotNull BigInteger transferAmount, @NotBlank String transferType, String referenceCode, String transactionDate, String accountNumber) {
}
