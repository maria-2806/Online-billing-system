package com.example.billingservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long billId,
        Long customerId,
        String customerName,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
