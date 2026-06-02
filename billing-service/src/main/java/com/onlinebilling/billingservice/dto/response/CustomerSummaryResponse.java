package com.onlinebilling.billingservice.dto.response;

import java.math.BigDecimal;

public record CustomerSummaryResponse(
        Long customerId,
        String customerName,
        long totalBills,
        long paidBills,
        long pendingBills,
        BigDecimal totalBilled
) {
}