package com.example.billingservice.dto.response;

import java.math.BigDecimal;

public record PaymentResponse(
        Long paymentId,
        Long billId,
        BigDecimal amountPaid,
        BigDecimal remainingBalance,
        String status
) {
}
