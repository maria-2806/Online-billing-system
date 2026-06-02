package com.onlinebilling.billingservice.model;

import java.math.BigDecimal;

public record PaymentRecord(
        Long id,
        Long billId,
        BigDecimal amountPaid,
        String method,
        PaymentStatus status
) {
}