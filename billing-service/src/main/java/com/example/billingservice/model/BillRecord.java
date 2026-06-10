package com.example.billingservice.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BillRecord(
        Long id,
        Long customerId,
        String customerName,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        BillStatus status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
