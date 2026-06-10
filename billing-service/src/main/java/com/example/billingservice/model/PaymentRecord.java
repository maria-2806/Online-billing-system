package com.example.billingservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentRecord(
        Long id,
        Long billId,
        BigDecimal amountPaid,
        String method,
        PaymentStatus status,
        LocalDateTime paymentDate
) {
}
