package com.example.billingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record ProcessPaymentRequest(
        @NotNull(message = "billId is required")
        @Positive(message = "billId must be positive")
        Long billId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "method is required")
        @Pattern(regexp = "^(CREDIT_CARD|BANK_TRANSFER|CASH|PAYPAL)$", message = "method must be one of: CREDIT_CARD, BANK_TRANSFER, CASH, PAYPAL")
        String method
) {
}
