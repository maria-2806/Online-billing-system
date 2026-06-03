package com.example.billingservice.dto.response;

import java.math.BigDecimal;

public record InvoiceResponse(
        Long billId,
        Long customerId,
        String customerName,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String status
) {
}
