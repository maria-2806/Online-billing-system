package com.example.billingservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateInvoiceRequest(
        @NotNull(message = "customerId is required")
        @Positive(message = "customerId must be positive")
        Long customerId,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount
) {
}
