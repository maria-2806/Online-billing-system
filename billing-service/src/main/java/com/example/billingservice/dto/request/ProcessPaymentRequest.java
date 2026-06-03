package com.example.billingservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProcessPaymentRequest(
        @NotNull(message = "billId is required")
        @Positive(message = "billId must be positive")
        Long billId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "method is required")
        String method
) {
}
